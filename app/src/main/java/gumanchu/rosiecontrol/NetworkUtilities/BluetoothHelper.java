package gumanchu.rosiecontrol.NetworkUtilities;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import gumanchu.rosiecontrol.CardboardUtilities.TextureHelper;
import gumanchu.rosiecontrol.Constants;

/**
 * Class holding functions for network connections
 */
public class BluetoothHelper extends AsyncTask<Void, Boolean, Void> implements NetworkHelper {
    private static final String TAG = "BluetoothHelper";

    Context context;
    ProgressDialog dialog;

    boolean connected = false;

    DataInputStream dataInputStream;
    DataOutputStream dataOutputStream;

    BluetoothAdapter bluetoothAdapter;
    BluetoothDevice bluetoothDevice;
    BluetoothSocket bluetoothSocket;

    /*
     * Stremaing stuff
     */

    int bytes, size;
    long imgSize;
    byte[] data;
    Mat buff, rev, ret;

    @Override
    public void connect(Context context) {

        this.context = context;
        dialog = new ProgressDialog(this.context);


        this.dialog.setMessage("Attempting to connect via Bluetooth");
        this.dialog.show();

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothDevice = bluetoothAdapter.getRemoteDevice(Constants.SERVER_ADDRESS_BLUETOOTH);

        try {
            bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(Constants.DEVICE_UUID);
            bluetoothAdapter.cancelDiscovery();
            bluetoothSocket.connect();

            dataInputStream = new DataInputStream(bluetoothSocket.getInputStream());
            dataOutputStream = new DataOutputStream(bluetoothSocket.getOutputStream());

        } catch (IOException e) {
            e.printStackTrace();
        }

        if (bluetoothSocket.isConnected()) {
            this.dialog.dismiss();
            connected = true;
        }

    }

    @Override
    public void disconnect() {
        try {
            bluetoothSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void read() {

    }

    public void write() {

    }

    @Override
    protected void onPreExecute() {
        if (this.dialog.isShowing()) {
            Log.i(TAG, "Force closing dialog.");
            this.dialog.dismiss();
        }
    }

    @Override
    protected Void doInBackground(final Void ... unused) {
        try {
            while (bluetoothSocket.isConnected()) {
                bytes = 0;
                size = dataInputStream.readInt();
                data = new byte[size];

                for (int i = 0; i < size; i += bytes) {
                    bytes = dataInputStream.read(data, i, size - i);
                }

                buff = new Mat(1, size, CvType.CV_8UC1);
                buff.put(0, 0, data);

                rev = Highgui.imdecode(buff, Highgui.CV_LOAD_IMAGE_UNCHANGED);

                Imgproc.cvtColor(rev, ret, Imgproc.COLOR_RGB2BGR);

                TextureHelper.setMat(ret);
                Log.i(TAG, "Got frame");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    protected void onProgressUpdate(Boolean ... status) {

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
