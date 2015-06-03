package gumanchu.rosiecontrol;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.opengl.GLES20;
import android.opengl.Matrix;
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
import com.google.vrtoolkit.cardboard.Eye;
import com.google.vrtoolkit.cardboard.HeadTransform;
import com.google.vrtoolkit.cardboard.Viewport;

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
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.text.DecimalFormat;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

@SuppressWarnings("deprecation")
public class MainActivity extends CardboardActivity implements SensorEventListener, CardboardView.StereoRenderer, SurfaceTexture.OnFrameAvailableListener {

    private static final String TAG = "RosieControl";

    public static final String SERVERIP = "98.102.8.76";
    public static final int VIDEOPORT = 4321;
    public static final int CONTROLPORT = 4322;
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

    private static final int GL_TEXTURE_EXTERNAL_OES = 0x8D65;
    CardboardView cardboardView;
    private SurfaceTexture surface;
    int mProgram;
    CardboardOverlayView overlayView;
    float[] camera;
    float[] view;

    private FloatBuffer vertexBuffer, textureVerticesBuffer;
    private ShortBuffer drawListBuffer;
    int texture;
    int mPositionHandle;
    int mColorHandle;
    int mTextureCoordHandle;
    static final int COORDS_PER_VERTEX = 2;
    final int vertexStride = COORDS_PER_VERTEX * 4; // 4 bytes per vertex
    Camera cam;



    static float squareVertices[] = { // in counterclockwise order:
            -1.0f, -1.0f,   // 0.left - mid
            1.0f, -1.0f,   // 1. right - mid
            -1.0f, 1.0f,   // 2. left - top
            1.0f, 1.0f,   // 3. right - top
    };

    static float textureVertices[] = {
            0.0f, 1.0f,  // A. left-bottom
            1.0f, 1.0f,  // B. right-bottom
            0.0f, 0.0f,  // C. left-top
            1.0f, 0.0f   // D. right-top
    };

    private short drawOrder[] = {0, 2, 1, 1, 2, 3}; // order to draw vertices

    final String vertexShaderCode =
            "attribute vec4 position;" +
                    "attribute vec2 inputTextureCoordinate;" +
                    "varying vec2 textureCoordinate;" +
                    "void main()" +
                    "{" +
                    "gl_Position = position;" +
                    "textureCoordinate = inputTextureCoordinate;" +
                    "}";

    final String fragmentShaderCode =
            "#extension GL_OES_EGL_image_external : require\n" +
                    "precision mediump float;" +
                    "varying vec2 textureCoordinate;                            \n" +
                    "uniform samplerExternalOES s_texture;               \n" +
                    "void main(void) {" +
                    "  gl_FragColor = texture2D( s_texture, textureCoordinate );\n" +
                    //"  gl_FragColor = vec4(1.0, 0.0, 0.0, 1.0);\n" +
                    "}";

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
        AsyncServiceHelper.initOpenCV(OpenCVLoader.OPENCV_VERSION_2_4_11, this, mLoaderCallback);

        senManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_NORMAL);
        senManager.registerListener(this, mag, SensorManager.SENSOR_DELAY_NORMAL);

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cardboardView = (CardboardView) findViewById(R.id.cardboard_view);
        cardboardView.setRenderer(this);
        setCardboardView(cardboardView);

        overlayView = (CardboardOverlayView) findViewById(R.id.overlay);
        overlayView.show3DToast("Hi dad");

        camera = new float[16];
        view = new float[16];

        senManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accel = senManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mag = senManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        dFormat = new DecimalFormat("0.00");

    }

    @Override
    protected void onPause() {
        super.onPause();
        senManager.unregisterListener(this);
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
//        streamer.execute();
//
        controller = new ControlRunnable();
//        Thread t = new Thread(controller);
//        t.start();
    }


    //TODO: POINTER FOR START OF TEMPORARY CARDBOARD VIEWER


    private int loadGLShader(int type, String code) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, code);
        GLES20.glCompileShader(shader);

        // Get the compilation status.
        final int[] compileStatus = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0);

        // If the compilation failed, delete the shader.
        if (compileStatus[0] == 0) {
            Log.e(TAG, "Error compiling shader: " + GLES20.glGetShaderInfoLog(shader));
            GLES20.glDeleteShader(shader);
            shader = 0;
        }

        if (shader == 0) {
            throw new RuntimeException("Error creating shader.");
        }

        return shader;
    }

    static private int createTexture() {
        int[] texture = new int[1];

        GLES20.glGenTextures(1, texture, 0);
        GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, texture[0]);
        GLES20.glTexParameterf(GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameterf(GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);

        return texture[0];
    }

    public void startCamera(int texture) {
        surface = new SurfaceTexture(texture);
        surface.setOnFrameAvailableListener(this);

        cam = Camera.open();

        try {
            cam.setPreviewTexture(surface);
            cam.startPreview();
        } catch (IOException ioe) {
            Log.w(TAG, "CAM LAUNCH FAILED");
        }
    }


    @Override
    public void onSurfaceCreated(EGLConfig config) {
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 0.5f); // Dark background so text shows up well

        ByteBuffer bb = ByteBuffer.allocateDirect(squareVertices.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(squareVertices);
        vertexBuffer.position(0);

        ByteBuffer bb2 = ByteBuffer.allocateDirect(textureVertices.length * 4);
        bb2.order(ByteOrder.nativeOrder());
        textureVerticesBuffer = bb2.asFloatBuffer();
        textureVerticesBuffer.put(textureVertices);
        textureVerticesBuffer.position(0);

        ByteBuffer dlb = ByteBuffer.allocateDirect(drawOrder.length * 2);
        dlb.order(ByteOrder.nativeOrder());
        drawListBuffer = dlb.asShortBuffer();
        drawListBuffer.put(drawOrder);
        drawListBuffer.position(0);

        int vertexShader = loadGLShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = loadGLShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

        mProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(mProgram, vertexShader);
        GLES20.glAttachShader(mProgram, fragmentShader);
        GLES20.glLinkProgram(mProgram);

        texture = createTexture();
        startCamera(texture);


    }


    @Override
    public void onSurfaceChanged(int width, int height) {

    }

    @Override
    public void onFrameAvailable(SurfaceTexture arg0) {
        this.cardboardView.requestRender();
    }

    @Override
    public void onFinishFrame(Viewport viewport) {

    }

    @Override
    public void onNewFrame(HeadTransform headTransform) {

        float[] mtx = new float[16];
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        surface.updateTexImage();
        surface.getTransformMatrix(mtx);

    }

    @Override
    public void onDrawEye(Eye eye) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        GLES20.glUseProgram(mProgram);

        GLES20.glActiveTexture(GL_TEXTURE_EXTERNAL_OES);
        GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, texture);

        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "position");
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        GLES20.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, vertexStride, vertexBuffer);

        mTextureCoordHandle = GLES20.glGetAttribLocation(mProgram, "inputTextureCoordinate");
        GLES20.glEnableVertexAttribArray(mTextureCoordHandle);
        GLES20.glVertexAttribPointer(mTextureCoordHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, vertexStride, textureVerticesBuffer);

        mColorHandle = GLES20.glGetAttribLocation(mProgram, "s_texture");

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, drawOrder.length, GLES20.GL_UNSIGNED_SHORT, drawListBuffer);

        GLES20.glDisableVertexAttribArray(mPositionHandle);
        GLES20.glDisableVertexAttribArray(mTextureCoordHandle);

        Matrix.multiplyMM(view, 0, eye.getEyeView(), 0, camera, 0);


    }

    @Override
    public void onRendererShutdown() {
        Log.i(TAG, "onRendererShutdown");
    }

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
                s.close();
            } catch (IOException e) {
                e.printStackTrace();
            }


            return null;
        }

        @Override
        protected void onProgressUpdate(Bitmap ... item) {

//            imView.setImageBitmap(item[0]);
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
