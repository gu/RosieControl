package gumanchu.rosiecontrol.NetworkUtilities;


import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;

import gumanchu.rosiecontrol.Constants;

/**
 * Class with helper functions for internet connection.
 */
public class InetHelper extends AsyncTask<Void, Boolean, Void > implements NetworkHelper {

    private static final String TAG = "InetHelper";

    InetAddress serverAddress;
    Socket serverSocket;

    Context context;
    private ProgressDialog dialog;

    boolean connected = false;

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
    }

    @Override
    protected Void doInBackground(final Void ... unused) {

        try {
            serverAddress = InetAddress.getByName(Constants.SERVER_IP);
            serverSocket = new Socket();
            serverSocket.connect(new InetSocketAddress(Constants.SERVER_IP, Constants.SERVER_PORT), 5000);

            publishProgress(serverSocket.isConnected());

        } catch (IOException e) {
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
