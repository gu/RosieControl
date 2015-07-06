package gumanchu.rosiecontrol.NetworkUtilities;


import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

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
public class InetHelper extends AsyncTask<Void, Boolean, Void > implements NetworkHelper {

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
    long imgSize;
    byte[] data;
    Mat buff, rev, ret;

    @Override
    public void connect(Context context) {
        Log.i(TAG, "Attempting to connect via Inet");

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

//        controlHandle = new Handler();
//        controlHandle.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    if (serverSocket.isConnected()) {
//                        dataOutputStream.writeInt(Controller.keyDirection);
//                        dataOutputStream.writeInt(Controller.keyRotation);
//                        dataOutputStream.writeInt(Controller.orientationX);
//                        dataOutputStream.writeInt(Controller.orientationY);
//                        dataOutputStream.flush();
//
//                        controlHandle.postDelayed(this, 75);
//                    }
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }
//        }, 1000);
    }

    @Override
    protected Void doInBackground(final Void ... unused) {

        try {
            serverAddress = InetAddress.getByName(Constants.SERVER_IP);
            serverSocket = new Socket();
            serverSocket.connect(new InetSocketAddress(Constants.SERVER_IP, Constants.SERVER_PORT), 5000);

            publishProgress(serverSocket.isConnected());

            dataInputStream = new DataInputStream(serverSocket.getInputStream());
            dataOutputStream = new DataOutputStream(serverSocket.getOutputStream());


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

                TextureHelper.setMat(ret);
                Log.i(TAG, "Got frame");
                Thread.sleep(75);
            }



        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    protected void onProgressUpdate(Boolean ... status) {
        if (status[0])
            Log.i(TAG, "Successful connection");
        else
            Log.i(TAG, "Failed connection");
        this.dialog.dismiss();

        connected = status[0];
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
