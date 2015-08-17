package gumanchu.rosiecontrol.NetworkUtilities;


import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

import gumanchu.rosiecontrol.CardboardUtilities.TextureHelper;
import gumanchu.rosiecontrol.Constants;
import gumanchu.rosiecontrol.Controller;
import gumanchu.rosiecontrol.MainActivity;

/**
 * Class with helper functions for WiFi connection.
 */
public class InetHelper extends AsyncTask<Void, InetHelper.Wrapper, Void > implements NetworkHelper {
    private static final String TAG = "InetHelper";

    Handler controlHandle;

    /*
     * Networking objects.
     */
    InetAddress serverAddress;
    Socket serverSocket;
    DataInputStream dataInputStream;
    DataOutputStream dataOutputStream;

    Context context;
    private ProgressDialog dialog;

    boolean connected = false;

    /*
     * Objects used for streaming.
     */
    int bytes, size;
    byte[] data;
    Mat buff, rev, ret;
    Bitmap bmp;

    private String serverIP;

    /*
     * Function meant to establish the connection between the device and Rosie's server.
     */
    @Override
    public void connect(Context context, String url) {
        Log.i(TAG, "Attempting to connect via Inet");

        //TODO: try to put connection stuff (Sockets, etc) in connect()

        serverIP = url;

        this.context = context;
        dialog = new ProgressDialog(this.context);

        this.execute();
    }

    @Override
    public void disconnect() {
        Log.i(TAG, "Attempting to disconnect via Inet");

        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void read() {
        //TODO: Implement.
    }

    @Override
    public void write() {
        //TODO: Implement.
    }

    @Override
    protected void onPreExecute() {
        this.dialog.setMessage("Attempting to connect via Wifi");
        this.dialog.show();

        rev = new Mat(480, 640, CvType.CV_8UC3);
        ret = new Mat(480, 640, CvType.CV_8UC3);
        bmp = Bitmap.createBitmap(640, 480, Bitmap.Config.ARGB_8888);

    }

    @Override
    protected Void doInBackground(final Void ... unused) {

        /*
         * Sets up wrapper class for this AsyncTask.
         */
        Wrapper wrapper = new Wrapper();

        try {
            serverAddress = InetAddress.getByName(serverIP);
            serverSocket = new Socket();
            serverSocket.connect(new InetSocketAddress(Constants.SERVER_IP, Constants.SERVER_PORT), 5000);
        } catch (Exception e ) {
            e.printStackTrace();
        }

        /*
         * Sends a signal to stop the progress dialog
         */
        wrapper.type = 0;
        wrapper.status = serverSocket.isConnected();
        publishProgress(wrapper);

        try {
            Thread.sleep(250);

            dataInputStream = new DataInputStream(serverSocket.getInputStream());
            dataOutputStream = new DataOutputStream(serverSocket.getOutputStream());

            wrapper.type = 1;

            /*
             * Loop to continually read image data.
             */
            while (serverSocket.isConnected()) {
                bytes = 0;

                size = dataInputStream.readInt();
                data = new byte[size];

                for (int i = 0; i < size; i+= bytes) {
                    bytes = dataInputStream.read(data, i, size - i);
                }

                buff = new Mat(1, size, CvType.CV_8UC1);
                buff.put(0, 0, data);

                rev = Highgui.imdecode(buff, Highgui.CV_LOAD_IMAGE_UNCHANGED);

                Imgproc.cvtColor(rev, ret, Imgproc.COLOR_RGB2BGR);

                wrapper.img = ret;
                // Display image to appropriate view.
                publishProgress(wrapper);
            }

        } catch (Exception e ) {
            e.printStackTrace();
        }

        return null;
    }

    class Wrapper {
        int type;
        Boolean status;
        Mat img;
    }

    @Override
    protected void onProgressUpdate(InetHelper.Wrapper... wrap) {
//        Log.i(TAG, "In onProgressUpdate");
        if (wrap[0].type == 0) {
            // Ran only after connection is established.
            this.dialog.dismiss();
            connected = wrap[0].status;
            if (connected) {
                Log.i(TAG, "Successful connection");

                // Block to write control data to the server.
                controlHandle = new Handler();
                controlHandle.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (serverSocket.isConnected()) {
                                dataOutputStream.writeInt(Controller.keyDirection);
                                dataOutputStream.writeInt(Controller.keyRotation);
                                dataOutputStream.writeInt(Controller.orientationX);
                                dataOutputStream.writeInt(Controller.orientationY);
                                dataOutputStream.flush();
//                                Log.i(TAG, "stuff: " + Controller.keyDirection + " " + Controller.keyRotation
//                                        + " " + Controller.orientationX + " " + Controller.orientationY);

                                controlHandle.postDelayed(this, 75);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }, 1000);


                MainActivity.initView();
            } else {
                Log.i(TAG, "Failed connection");
                MainActivity.failedConnection();
            }
        } else if (wrap[0].type == 1) {
            // Ran when a new image is read.
            switch (MainActivity.rosieView) {
                case Constants.CARDBOARD_VIEW:
                    TextureHelper.setMat(wrap[0].img);
                    break;
                case Constants.DEFAULT_VIEW:
                    Utils.matToBitmap(wrap[0].img, bmp);
                    MainActivity.imageView.setImageBitmap(bmp);
                    break;
            }
        }

    }

    @Override
    protected void onPostExecute(Void unused) {
        if (this.dialog.isShowing()) {
            Log.i(TAG, "Force closing dialog.");
            this.dialog.dismiss();
        }
    }

    public boolean isConnected() {
        return connected;
    }



}
