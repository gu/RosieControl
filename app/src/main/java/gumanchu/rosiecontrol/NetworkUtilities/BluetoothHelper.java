package gumanchu.rosiecontrol.NetworkUtilities;

import android.app.Activity;
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
 * Class holding functions for network connections
 */
public class BluetoothHelper extends AsyncTask<Void, Boolean, Void> implements NetworkHelper {
    private static final String TAG = "BluetoothHelper";

    boolean connected = false;

    @Override
    public void connect(Context context) {
    }

    @Override
    public void disconnect() {
    }

    public void read() {

    }

    public void write() {

    }

    @Override
    protected void onPreExecute() {
    }

    @Override
    protected Void doInBackground(final Void ... unused) {
        return null;
    }

    @Override
    protected void onProgressUpdate(Boolean ... status) {

    }

    @Override
    protected void onPostExecute(Void unused) {
    }

    public boolean isConnected() {
        return connected;
    }

}
