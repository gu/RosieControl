package gumanchu.rosiecontrol;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;

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

@SuppressWarnings("deprecation")
public class MainActivity extends CardboardActivity implements SensorEventListener {

    private static final String TAG = "RosieControl";

    public static final String SERVERIP = "98.102.8.76";
    public static final int VIDEOPORT = 1234;
    public static final int CONTROLPORT = 1235;
    private static final float ALPHA = 0.2f;

    /*
     * Vars for VideoTask
     */
//    private ImageView imView;
    Mat img, tmp, ret;
    Bitmap bm;
    long imgSize;
    int size;
    int bytes = 0;
    byte[] data;

    // Used for ControlTask
    int currentKey, previousKey;
    ControlRunnable controller;
    RosieTask streamer;

    // Sensor vars.
    private SensorManager senManager;
    private Sensor accel;
    private Sensor mag;
    float[] mGravity;
    float[] mGeomagnetic;
    DecimalFormat dFormat;

    // Vars for Data transfer
    DataInputStream in;
    DataOutputStream out;


    //TODO: START OF CARD?BOARD VARS

    CardboardView cardboardView;



    //TODO: END OF CARDBOARD VARS

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    Log.i(TAG, "OpenCV Manager Connected");
                    displayView();
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
    protected void onResume() {
        super.onResume();
        cardboardView.onResume();
        AsyncServiceHelper.initOpenCV(OpenCVLoader.OPENCV_VERSION_2_4_11, this, mLoaderCallback);

//        senManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_NORMAL);
//        senManager.registerListener(this, mag, SensorManager.SENSOR_DELAY_NORMAL);

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cardboardView = (CardboardView) findViewById(R.id.cardboard_view);
        cardboardView.setRenderer(new CardboardRenderer(this));
        setCardboardView(cardboardView);

//        senManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
//        accel = senManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
//        mag = senManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
//
//        dFormat = new DecimalFormat("0.00");
//
//        bm = BitmapFactory.decodeResource(getResources(), R.drawable.ossim);

    }

    @Override
    protected void onPause() {
        super.onPause();
        cardboardView.onPause();
//        senManager.unregisterListener(this);
    }

    public void displayView() {
//        imView = (ImageView) findViewById(R.id.imView);
        img = new Mat(480, 640, CvType.CV_8UC3);
        ret = new Mat(480, 640, CvType.CV_8UC3);
        tmp = new Mat();
        imgSize = img.total() * img.elemSize();

        currentKey = 0;
        previousKey = 1;

        bm = Bitmap.createBitmap(640, 480, Bitmap.Config.ARGB_8888);



        streamer = new RosieTask();
        streamer.execute();
//
        controller = new ControlRunnable();
//        Thread t = new Thread(controller);
//        t.start();
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

    public class ControlRunnable implements Runnable {
        @Override
        public void run() {

            Looper.prepare();

            Handler controlHandle = new Handler();

            try {
                InetAddress serverAddr = InetAddress.getByName(SERVERIP);
                Socket s = new Socket(serverAddr, CONTROLPORT);

                out = new DataOutputStream(s.getOutputStream());

                while (s.isConnected() && currentKey != KeyEvent.KEYCODE_ESCAPE) {
                    if (currentKey != previousKey) {
                        out.writeInt(currentKey);
                        out.flush();
                    }
                    previousKey = currentKey;
                    controlHandle.postDelayed(controller, 100);
                }

                s.close();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    class RosieTask extends AsyncTask<Void, Bitmap, Void> {
        @Override
        protected Void doInBackground(Void ... unused) {

            try {
                InetAddress serverAddr = InetAddress.getByName(SERVERIP);
                Socket s = new Socket(serverAddr, VIDEOPORT );

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
            CardboardRenderer.stream= item[0];
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
