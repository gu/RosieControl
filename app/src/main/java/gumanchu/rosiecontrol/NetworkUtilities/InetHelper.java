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
 * Class with helper functions for internet connection.
 */
public class InetHelper extends AsyncTask<Void, InetHelper.Wrapper, Void > implements NetworkHelper {

    private static final String TAG = "InetHelper";

    Handler controlHandle;
    DataInputStream dataInputStream;
    DataOutputStream dataOutputStream;

    InetAddress serverAddress;
    Socket serverSocket;

    Context context;
    private ProgressDialog dialog;

    boolean connected = false;

    /*
     * Streaming stuff
     */
    int bytes, size;
    byte[] data;
    Mat buff, rev, ret;
    Bitmap bmp;

    @Override
    public void connect(Context context) {
        Log.i(TAG, "Attempting to connect via Inet");

        //TODO: try to put connection stuff in connect()

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

    public void read() {
    }

    public void write() {

    }

    @Override
    protected void onPreExecute() {
        this.dialog.setMessage("Attempting to connect via Wifi");
        this.dialog.show();

        rev = new Mat(480, 640, CvType.CV_8UC3);
        ret = new Mat(480, 640, CvType.CV_8UC3);
        bmp = Bitmap.createBitmap(640, 480, Bitmap.Config.ARGB_8888);

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

                        controlHandle.postDelayed(this, 75);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, 1000);
    }

    @Override
    protected Void doInBackground(final Void ... unused) {

        Wrapper wrapper = new Wrapper();

        try {
            serverAddress = InetAddress.getByName(Constants.SERVER_IP);
            serverSocket = new Socket();
            serverSocket.connect(new InetSocketAddress(Constants.SERVER_IP, Constants.SERVER_PORT), 5000);
        } catch (Exception e ) {
            e.printStackTrace();
        }

        wrapper.type = 0;
        wrapper.status = serverSocket.isConnected();
        publishProgress(wrapper);

        try {
            Thread.sleep(500);

            dataInputStream = new DataInputStream(serverSocket.getInputStream());
            dataOutputStream = new DataOutputStream(serverSocket.getOutputStream());

            wrapper.type = 1;

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
                publishProgress(wrapper);
                Thread.sleep(75);
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
        Log.i(TAG, "In onProgressUpdate");
        if (wrap[0].type == 0) {
            this.dialog.dismiss();
            connected = wrap[0].status;
            if (connected) {
                Log.i(TAG, "Successful connection");
                MainActivity.initView();
            } else {
                Log.i(TAG, "Failed connection");
                MainActivity.failedConnection();
            }
        } else if (wrap[0].type == 1) {
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
