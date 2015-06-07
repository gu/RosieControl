package gumanchu.rosiecontrol;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
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
public class MainActivity extends CardboardActivity implements SensorEventListener {

    private static final String TAG = "RosieControl";

    private static final float ALPHA = 0.2f;

    /*
     * Vars for VideoTask
     */
//    private ImageView imView;
    Mat img, tmp, ret;
    Bitmap bm;
    long imgSize;
    int bytes, size;
    byte[] data;

    int currentKey, previousKey;

    // BLUETOOTH VARS
    private static final int REQUEST_ENABLE_BT = 1;
    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothSocket mBluetoothSocket = null;
    private DataOutputStream mOutStream = null;
    private DataInputStream mInStream = null;
    ControlBTHRunnable controlBTH;
    RosieBTHTask videoBTH;

    // IP VARS
    ControlIPRunnable controlIP;
    RosieIPTask videoIP;
    DataInputStream in;
    DataOutputStream out;

    // Sensor vars.
    private SensorManager senManager;
    private Sensor accel;
    private Sensor mag;
    float[] mGravity;
    float[] mGeomagnetic;
    DecimalFormat dFormat;



    //TODO: START OF CARD?BOARD VARS

    CardboardView cardboardView;

    //TODO: END OF CARDBOARD VARS

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

        cardboardView = (CardboardView) findViewById(R.id.cardboard_view);
        cardboardView.setRenderer(new CardboardRenderer(this));
        setCardboardView(cardboardView);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        CheckBTState();

//        senManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
//        accel = senManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
//        mag = senManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
//
//        dFormat = new DecimalFormat("0.00");
//
//        bm = BitmapFactory.decodeResource(getResources(), R.drawable.ossim);

    }

    @Override
    protected void onResume() {
        super.onResume();
        cardboardView.onResume();
        AsyncServiceHelper.initOpenCV(OpenCVLoader.OPENCV_VERSION_2_4_11, this, mLoaderCallback);

//        senManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_NORMAL);
//        senManager.registerListener(this, mag, SensorManager.SENSOR_DELAY_NORMAL);

    }

    public void startView() {
//        imView = (ImageView) findViewById(R.id.imView);
        img = new Mat(480, 640, CvType.CV_8UC3);
        ret = new Mat(480, 640, CvType.CV_8UC3);
        tmp = new Mat();
        imgSize = img.total() * img.elemSize();


        bm = Bitmap.createBitmap(640, 480, Bitmap.Config.ARGB_8888);


        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(Constants.SERVER_ADDRESS_BLUETOOTH);
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

    }

    @Override
    protected void onPause() {
        super.onPause();
        cardboardView.onPause();
//        senManager.unregisterListener(this);



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


    //TODO: POINTER FOR START OF TEMPORARY CARDBOARD VIEWER



    //TODO: END OF TEMPORARY POINTER


    @Override
    public void onSensorChanged(SensorEvent event) {
        float R[] = new float[9];
        float I[] = new float[9];
        float orientation[] = new float[3];
        float orientationD[] = new float[3];
        boolean success;

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
            mGravity = applyLowPassFilter(event.values.clone(), mGravity);
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
            mGeomagnetic = applyLowPassFilter(event.values.clone(), mGeomagnetic);
        if (mGravity != null && mGeomagnetic != null) {
            success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic);
            if (success) {
                SensorManager.getOrientation(R, orientation);
                orientationD[0] = (float) Math.toDegrees(orientation[0]);
                orientationD[1] = (float) Math.toDegrees(orientation[1]);
                orientationD[2] = (float) Math.toDegrees(orientation[2]);
                Log.i(TAG, "X: " + dFormat.format(orientationD[1]) + ", Y: " + dFormat.format(orientationD[2]) + ", Z: " + dFormat.format(orientationD[0]));
            }
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private float[] applyLowPassFilter(float[] input, float[] output) {
        if (output == null) return input;

        for ( int i = 0; i < input.length; i++ ) {
            output[i] = output[i] + ALPHA * (input[i] - output[i]);
        }
        return output;
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

    public class ControlBTHRunnable implements Runnable {
        @Override
        public void run() {

            Looper.prepare();
            Handler controlHandle = new Handler();

            try {
                while (mBluetoothSocket.isConnected() && currentKey != KeyEvent.KEYCODE_ESCAPE) {
                    if (currentKey != previousKey) {
                        mOutStream.writeInt(currentKey);
                        mOutStream.flush();
                    }
                    previousKey = currentKey;
                    controlHandle.postDelayed(controlBTH, 100);
                }
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

//                    if (currentKey != previousKey) {
//                        mOutStream.writeInt(currentKey);
//                        mOutStream.flush();
//                    }
//                    previousKey = currentKey;

                    Log.i(TAG, "SIZE: " + size);

                    tmp = new Mat(1, size, CvType.CV_8UC1);
                    tmp.put(0, 0, data);

                    img = Highgui.imdecode(tmp, Highgui.CV_LOAD_IMAGE_UNCHANGED);

                    Imgproc.cvtColor(img, ret, Imgproc.COLOR_RGB2BGR);

                    Utils.matToBitmap(ret, bm);
                    publishProgress(bm);
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
            TextureHelper.setBitmap(item[0]);
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
