package gumanchu.rosiecontrol;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.ImageView;

import com.google.vrtoolkit.cardboard.CardboardActivity;
import com.google.vrtoolkit.cardboard.CardboardView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.text.DecimalFormat;

import gumanchu.rosiecontrol.CardboardUtilities.CardboardRenderer;
import gumanchu.rosiecontrol.CardboardUtilities.TextureHelper;

@SuppressWarnings("deprecation")
public class MainActivity extends CardboardActivity {

    private static final String TAG = "RosieControl";

    /*
     * Vars for VideoTask
     */
    Mat img, tmp, ret;
    Bitmap bm;
    long imgSize;
    int bytes, size;
    byte[] data;

    int currentKey, previousKey, currentKeyROT, previousKeyROT, previous_X, previous_Y;

    // BLUETOOTH VARS
    private static final int REQUEST_ENABLE_BT = 1;
    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothSocket mBluetoothSocket = null;
    private DataOutputStream mOutStream = null;
    private DataInputStream mInStream = null;

//    ControlBTHRunnable controlBTH;
    RosieBTHTask videoBTH;

    CardboardRenderer CR;

    Handler controlHandle;
    Runnable ctl;
    int current_X;
    int current_Y;


    // IP VARS
    ControlIPRunnable controlIP;
    RosieIPTask videoIP;
    DataInputStream in;
    DataOutputStream out;

    // Sensor vars.
    DecimalFormat dFormat;
    float orientationD[];





    CardboardView cardboardView;


    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    Log.i(TAG, "OpenCV Manager Connected");
                    startView();
                    break;
                case LoaderCallbackInterface.INIT_FAILED:
                    Log.i(TAG,"Init Failed");
                    break;
                case LoaderCallbackInterface.INSTALL_CANCELED:
                    Log.i(TAG,"Install Cancelled");
                    break;
                case LoaderCallbackInterface.INCOMPATIBLE_MANAGER_VERSION:
                    Log.i(TAG,"Incompatible Version");
                    break;
                case LoaderCallbackInterface.MARKET_ERROR:
                    Log.i(TAG,"Market Error");
                    break;
                default:
                    Log.i(TAG, "OpenCV Manager Install");
                    super.onManagerConnected(status);
                    break;
            }
        }
    };

    public MainActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        AsyncServiceHelper.initOpenCV(OpenCVLoader.OPENCV_VERSION_2_4_11, this, mLoaderCallback);

        orientationD = new float[3];

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        CheckBTState();

        dFormat = new DecimalFormat("0");
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    public void startView() {

        setCardboardView(cardboardView);
        cardboardView = (CardboardView) findViewById(R.id.cardboard_view);

        CR = new CardboardRenderer(this);
        cardboardView.setRenderer(CR);

        cardboardView.onResume();


//        imView = (ImageView) findViewById(R.id.imView);
        img = new Mat(480, 640, CvType.CV_8UC3);
        ret = new Mat(480, 640, CvType.CV_8UC3);
        tmp = new Mat();
        imgSize = img.total() * img.elemSize();

        bm = Bitmap.createBitmap(640, 480, Bitmap.Config.ARGB_8888);

        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(Constants.SERVER_ADDRESS_BLUETOOTH  );
        try {
            mBluetoothSocket = device.createRfcommSocketToServiceRecord(Constants.DEVICE_UUID);
        } catch (IOException e) {
            AlertBox("Fatal Error", "In onResume() and socket create failed: " + e.getMessage() + ".");
        }
        mBluetoothAdapter.cancelDiscovery();
        try {
            mBluetoothSocket.connect();
        } catch (IOException e) {
            try {
                mBluetoothSocket.close();
            } catch (IOException e2) {
                AlertBox("Fatal Error", "In onResume() and unable to close socket during connection failure" + e2.getMessage() + ".");
            }
        }
        try {
            mOutStream = new DataOutputStream(mBluetoothSocket.getOutputStream());
            mInStream = new DataInputStream(mBluetoothSocket.getInputStream());
        } catch (IOException e) {
            AlertBox("Fatal Error", "In onResume() and output stream creation failed:" + e.getMessage() + ".");
        }


        currentKey = 0;
        previousKey = 1;
        currentKeyROT = 0;
        previousKeyROT = 1;
        previous_X = -1;
        previous_Y = -1;


        // IP IMPLEMENTATION
//        videoIP = new RosieIPTask();
//        videoIP.execute();
//
//        controlIP = new ControlIPRunnable();
//        Thread controlThreadIP = new Thread(controlIP);
//        controlThreadIP.start();

        //BTH IMPLEMENTATION
        videoBTH = new RosieBTHTask();
        videoBTH.execute();

        controlHandle = new Handler();
        controlHandle.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    if (mBluetoothSocket.isConnected() && currentKey != KeyEvent.KEYCODE_ESCAPE) {

//                        Log.i(TAG, "X: " + dFormat.format(orientationD[0]) + ", Y: " + dFormat.format(orientationD[1]) + ", Z: " + dFormat.format(orientationD[2]));
                        CR.getOrientation(orientationD);

                        current_X = Integer.parseInt(dFormat.format(orientationD[1]));
                        current_Y = Integer.parseInt(dFormat.format(orientationD[0]));

//                        Log.i(TAG, "X: " + current_X + " Y: " + current_Y);

                        mOutStream.writeInt(currentKey);
                        mOutStream.writeInt(currentKeyROT);
                        mOutStream.writeInt(current_X);
                        mOutStream.writeInt(current_Y);
                        mOutStream.flush();

                        controlHandle.postDelayed(this, 75);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }, 1000);
    }

    @Override
    protected void onPause() {
        super.onPause();
        cardboardView.onPause();

        if (mOutStream != null) {
            try {
                mOutStream.flush();
            } catch (IOException e) {
                AlertBox("Fatal Error", "In onPause() and failed to flush output stream: " + e.getMessage() + ".");
            }
        }

        try     {
            mBluetoothSocket.close();
        } catch (IOException e2) {
            AlertBox("Fatal Error", "In onPause() and failed to close socket." + e2.getMessage() + ".");
        }
    }

    public class ControlIPRunnable implements Runnable {
        @Override
        public void run() {

            Looper.prepare();

            Handler controlHandle = new Handler();

            try {
                InetAddress serverAddr = InetAddress.getByName(Constants.SERVER_IP);
                Socket s = new Socket(serverAddr, Constants.CONTROL_PORT);

                out = new DataOutputStream(s.getOutputStream());

                while (s.isConnected() && currentKey != KeyEvent.KEYCODE_ESCAPE) {
                    if (currentKey != previousKey) {
                        out.writeInt(currentKey);
                        out.flush();
                    }
                    previousKey = currentKey;
                    controlHandle.postDelayed(controlIP, 100);
                }

                s.close();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public class RosieIPTask extends AsyncTask<Void, Bitmap, Void> {
        @Override
        protected Void doInBackground(Void ... unused) {

            try {
                InetAddress serverAddr = InetAddress.getByName(Constants.SERVER_IP);
                Socket s = new Socket(serverAddr, Constants.VIDEO_PORT);

                in = new DataInputStream(s.getInputStream());

                TextureHelper.setStreaming(true);
                while (s.isConnected()) {

                    bytes = 0;

                    size = in.readInt();
                    data = new byte[size];


                    for (int i = 0; i < size; i += bytes) {
                        bytes = in.read(data, i, size - i);
                    }

                    Log.i(TAG, "BYTES: " + bytes);

                    tmp = new Mat(1, size, CvType.CV_8UC1);
                    tmp.put(0, 0, data);

                    img = Highgui.imdecode(tmp, Highgui.CV_LOAD_IMAGE_UNCHANGED);

                    Imgproc.cvtColor(img, ret, Imgproc.COLOR_RGB2BGR);

                    Utils.matToBitmap(ret, bm);
                    TextureHelper.setMat(ret);
                    publishProgress(bm);
                }
                TextureHelper.setStreaming(false);
                s.close();
            } catch (IOException e) {
                e.printStackTrace();
            }


            return null;
        }

        @Override
        protected void onProgressUpdate(Bitmap ... item) {
//            imView.setImageBitmap(item[0]);
//            TextureHelper.setBitmap(item[0]);
        }

    }

    public class RosieBTHTask extends AsyncTask<Void, Bitmap, Void> {
        @Override
        protected Void doInBackground(Void ... unused) {
            try {
                while (mBluetoothSocket.isConnected() && currentKey != KeyEvent.KEYCODE_ESCAPE) {
                    CardboardRenderer.streaming = true;
                    bytes = 0;

                    size = mInStream.readInt();
                    data = new byte[size];

                    for (int i = 0; i < size; i+= bytes) {
                        bytes = mInStream.read(data, i, size - i);
                    }

                    tmp = new Mat(1, size, CvType.CV_8UC1);
                    tmp.put(0, 0, data);

                    img = Highgui.imdecode(tmp, Highgui.CV_LOAD_IMAGE_UNCHANGED);

                    Imgproc.cvtColor(img, ret, Imgproc.COLOR_RGB2BGR);

//                    Utils.matToBitmap(ret, bm);
                    TextureHelper.setMat(ret);
//                    publishProgress(bm);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            CardboardRenderer.streaming = false;

            return null;
        }

        @Override
        protected void onProgressUpdate(Bitmap ... item) {
//            imView.setImageBitmap(item[0]);
//            TextureHelper.setBitmap(item[0]);
//            TextureHelper.setBitmap(item[0]);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_LEFT:
                Log.i(TAG, "LEFT DOWN");
                currentKey = keyCode;
                return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                Log.i(TAG, "RIGHT DOWN");
                currentKey = keyCode;
                return true;
            case KeyEvent.KEYCODE_DPAD_UP:
                Log.i(TAG, "UP DOWN");
                currentKey = keyCode;
                return true;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                Log.i(TAG, "DOWN DOWN");
                currentKey = keyCode;
                return true;
            case KeyEvent.KEYCODE_ESCAPE:
                Log.i(TAG, "ESCAPE");
                currentKey = keyCode;
                return true;
            case KeyEvent.KEYCODE_L:
                Log.i(TAG, "L DOWN");
                currentKeyROT = keyCode;
                return true;
            case KeyEvent.KEYCODE_J:
                Log.i(TAG, "J DOWN");
                currentKeyROT = keyCode;
                return true;
            default:
                return super.onKeyDown(keyCode, event);
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {

        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_LEFT:
                Log.i(TAG, "LEFT UP");
                currentKey = 0;
                return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                Log.i(TAG, "RIGHT UP");
                currentKey = 0;
                return true;
            case KeyEvent.KEYCODE_DPAD_UP:
                Log.i(TAG, "UP UP");
                currentKey = 0;
                return true;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                Log.i(TAG, "DOWN UP");
                currentKey = 0;
                return true;
            case KeyEvent.KEYCODE_ESCAPE:
                Log.i(TAG, "ESCAPE");
                currentKey = 0;
                return true;
            case KeyEvent.KEYCODE_L:
                Log.i(TAG, "L UP");
                currentKeyROT = 0;
                return true;
            case KeyEvent.KEYCODE_J:
                Log.i(TAG, "J UP");
                currentKeyROT = 0;
                return true;

            default:
                return super.onKeyUp(keyCode, event);
        }

    }

    private void CheckBTState() {
        // Check for Bluetooth support and then check to make sure it is turned on

        // Emulator doesn't support Bluetooth and will return null
        if(mBluetoothAdapter ==null) {
            AlertBox("Fatal Error", "Bluetooth Not supported. Aborting.");
        } else {
            if (!mBluetoothAdapter.isEnabled()) {
                //Prompt user to turn on Bluetooth
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }
    }

    public void AlertBox( String title, String message ){
        new AlertDialog.Builder(this)
                .setTitle( title )
                .setMessage( message + " Press OK to exit." )
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface arg0, int arg1) {
                        finish();
                    }
                }).show();
    }

}
