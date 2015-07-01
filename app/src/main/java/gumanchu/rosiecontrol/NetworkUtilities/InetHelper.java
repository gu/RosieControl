package gumanchu.rosiecontrol.NetworkUtilities;


import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;

import gumanchu.rosiecontrol.Constants;

/**
 * Class with helper functions for internet connection.
 */
public class InetHelper extends NetworkHelper {

    private static final String TAG = "InetHelper";

    InetAddress serverAddress;
    Socket serverSocket;

    InetRunnable inetRunnable;

    @Override
    public void connect() {
        Log.i(TAG, "Attempting to connect via Inet");

        inetRunnable = new InetRunnable();
        new Thread(inetRunnable).start();

        if (serverSocket.isConnected()) {
            super.connected = true;
        }

    }

    @Override
    public void disconnect() {
        Log.i(TAG, "Attempting to disconnect via Inet");

    }

    public class InetRunnable implements Runnable {

        @Override
        public void run() {

            try {
                serverAddress = InetAddress.getByName(Constants.SERVER_IP);
                serverSocket = new Socket(serverAddress, Constants.SERVER_PORT);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }
}
