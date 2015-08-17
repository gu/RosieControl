package gumanchu.rosiecontrol;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.ViewFlipper;


import com.google.vrtoolkit.cardboard.CardboardActivity;
import com.google.vrtoolkit.cardboard.CardboardView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.io.IOException;

import gumanchu.rosiecontrol.CardboardUtilities.CardboardRenderer;
import gumanchu.rosiecontrol.CardboardUtilities.TextureHelper;
import gumanchu.rosiecontrol.NetworkUtilities.BluetoothHelper;
import gumanchu.rosiecontrol.NetworkUtilities.InetHelper;
import gumanchu.rosiecontrol.NetworkUtilities.NetworkHelper;


public class MainActivity extends CardboardActivity {
    private static final String TAG = "RosieControl";

    /*
     * Values input by user to determine type of connection.
     */
    private int controlMethod = Constants.CONTROL_TYPE_BOTH;
    private int connectionType = Constants.CONNECTION_TYPE_INET;
    public static int rosieView = Constants.DEFAULT_VIEW;
    private String rosieIP = Constants.SERVER_IP;

    /*
     * Object to handle gamepad control input.
     */
    Controller controller;

    /*
     * Object to handle network (WiFi/Bluetooth) connections.
     */
    static NetworkHelper nHelper;
    Handler handler;

    static Mat defaultFrame;
    static Bitmap defaultBmp;
    static Context context;

    /*
     * UI Elements
     */
    private static ViewFlipper viewFlipper;

    /*
     * Cardboard elements.
     */
    static CardboardRenderer cardboardRenderer;
    static CardboardView cardboardView;

    public static ImageView imageView;

    /*
     * UI elements.
     */
    static TextView tvStatus;
    RadioButton bt1;
    RadioButton bt2;
    RadioButton bt3;
    RadioButton bt4;
    RadioButton bt5;
    RadioButton bt6;
    RadioButton bt7;
    EditText editText;

    /*
     * Callback to check if OpenCV has been loaded properly.
     *
     * This callback is run after the initOpenCV() function.
     */
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    Log.i(TAG, "OpenCV Manager Connected");
                    /*
                     * Enables core UI elements after OpenCV has been loaded.
                     *
                     * Necessary to prevent users from trying to load the stream, which include
                     * OpenCV elements, before the library is ready to be used.
                     */
                    enableRadio();
            }
        }
    };

    /*
     * Main method.  Does nothing.
     */
    public MainActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this.getApplicationContext();
        setContentView(R.layout.activity_main);

        /*
         * viewFlipper is used to switch between menu, cardboard view, and default view.
         *
         * Depends on what user selects through radio selections.
         */
        viewFlipper = (ViewFlipper) findViewById(R.id.viewFlipper);
        // Sets default view to be the middle (index = 1) child.
        viewFlipper.setDisplayedChild(1);

        handler = new Handler();
        controller = new Controller();
        tvStatus = (TextView) findViewById(R.id.tvStatus);
        bt1 = (RadioButton) findViewById(R.id.rbConn1);
        bt2 = (RadioButton) findViewById(R.id.rbConn2);
        bt3 = (RadioButton) findViewById(R.id.rbCtl1);
        bt4 = (RadioButton) findViewById(R.id.rbCtl2);
        bt5 = (RadioButton) findViewById(R.id.rbCtl3);
        bt6 = (RadioButton) findViewById(R.id.rbView1);
        bt7 = (RadioButton) findViewById(R.id.rbView2);
        imageView = (ImageView) findViewById(R.id.ivDefaultView);
        editText = (EditText) findViewById(R.id.etInetAddress);

        /*
         * Attempts to load OpenCV using the AsyncServiceHelper.
         *
         * Runs the BaseLoaderCallback function upon completion.
         */
        tvStatus.setText("Initializing OpenCV...");
        AsyncServiceHelper.initOpenCV(OpenCVLoader.OPENCV_VERSION_2_4_11, this, mLoaderCallback);

    }

    @Override
    protected void onResume() {
        super.onResume();
        /*
         * Attempts to resume the cardboard view if it is being used.
         *
         * TODO: Implement resume for default view.
         */
        if (cardboardView !=  null) {
            cardboardView.onResume();
        }
    }

    public void enableRadio() {
        /*
         * Enables UI elements for user selections after OpenCV is loaded.
         */
        tvStatus.setText("OpenCV Loaded.  Please select parameters.");
        bt1.setEnabled(true);
        bt2.setEnabled(true);
        bt3.setEnabled(true);
        bt4.setEnabled(true);
        bt5.setEnabled(true);
        bt6.setEnabled(true);
        bt7.setEnabled(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        /*
         * Attempts to pause the cardboard view if it is being used.
         *
         * TODO: Implement something similar for default view.
         */
        if (cardboardView != null) {
            cardboardView.onPause();
        }
    }

    /*
     * Callback function after the main button on the menu is pressed.
     *
     * Attempts to connect via the options chosen by the user.
     */
    public void onButtonRosieClick(View view) {

        tvStatus.setText("Preparing Connection");
        Log.i(TAG, "In button click");

        /*
         * Enables the cardboard view if it has been selected.
         */
        switch (rosieView) {
            case Constants.CARDBOARD_VIEW:
                cardboardView = (CardboardView) findViewById(R.id.cardboard_view);
                setCardboardView(cardboardView);
                break;
        }


        /*
         * Initializes new NetworkHelper based off of input.
         */
        switch (connectionType) {
            case Constants.CONNECTION_TYPE_INET:

                nHelper = new InetHelper();
                nHelper.connect(this, editText.getText().toString());

                //TODO: make initView static and access from AsyncTask
                //TODO: try passing viewFlipper to AsyncTask
                //TODO: try making viewFlipper static to directly access ?

                break;
            case Constants.CONNECTION_TYPE_BTH:

                nHelper = new BluetoothHelper();
                nHelper.connect(this, editText.getText().toString());

                break;
        }
    }

    public static void failedConnection() {
        tvStatus.setText("Failed to connect to Rosie via Wifi");
    }

    public static void initView() {

        //TODO: Add logic to change "Wifi" to the appropriate connection type.
        tvStatus.setText("Connected to Rosie via Wifi");


        /*
         * Loads a default placeholder image while connecting to the stream.
         */
        Mat img = new Mat(480, 640, CvType.CV_8UC3);
        try {
            img = Utils.loadResource(context, R.drawable.sad_danbo);
        } catch (IOException e) {
            e.printStackTrace();
        }

        /*
         * Flips to the appropriate view based off of the input.
         */
        switch(rosieView) {
            case Constants.DEFAULT_VIEW:

                viewFlipper.setInAnimation(context, R.anim.slide_in_from_right);
                viewFlipper.setOutAnimation(context, R.anim.slide_out_to_left);
                viewFlipper.showPrevious();

                imageView.setImageResource(R.drawable.sad_danbo);

                defaultFrame = new Mat(480, 640, CvType.CV_8UC3);
                defaultBmp = Bitmap.createBitmap(640, 480, Bitmap.Config.ARGB_8888);

                Log.i(TAG, "status: " + nHelper.isConnected());

                break;
            case Constants.CARDBOARD_VIEW:

                cardboardRenderer = new CardboardRenderer(context);
                cardboardView.setRenderer(cardboardRenderer);
                cardboardView.onResume();

                viewFlipper.setInAnimation(context, R.anim.slide_in_from_left);
                viewFlipper.setOutAnimation(context, R.anim.slide_out_to_right);
                viewFlipper.showNext();

                TextureHelper.setMat(img);
                break;
        }
    }

    /*
     * Callback function for when a Radio button for the view is selected.
     */
    public void onRadioViewClick(View view) {
        boolean checked = ((RadioButton) view).isChecked();

        switch(view.getId()) {
            case R.id.rbView1:
                if (checked) {
                    Log.i(TAG, "DEFAULT VIEW SELECTED");
                    rosieView = Constants.DEFAULT_VIEW;
                }
                break;
            case R.id.rbView2:
                if (checked) {
                    Log.i(TAG, "CARDBOARD VIEW SELECTED");
                    rosieView = Constants.CARDBOARD_VIEW;
                }
                break;
        }
    }

    /*
     * Callback function for when a Radio button for the connection is selected.
     */
    public void onRadioConnClick(View view) {
        boolean checked = ((RadioButton) view).isChecked();

        switch(view.getId()) {
            case R.id.rbConn1:
                if (checked) {
                    Log.i(TAG, "Wifi connection selected");
                    connectionType = Constants.CONNECTION_TYPE_INET;
                }
                break;
            case R.id.rbConn2:
                if (checked) {
                    Log.i(TAG, "Bluetooth Connection selected");
                    connectionType = Constants.CONNECTION_TYPE_BTH;
                }
                break;
        }
    }

    /*
     * Callback function for when a Radio button for the control is selected.
     */
    public void onRadioCtlClick(View view) {
        boolean checked = ((RadioButton) view).isChecked();

        switch(view.getId()) {
            case R.id.rbCtl3:
                if (checked) {
                    Log.i(TAG, "Both methods selected");
                    controlMethod = Constants.CONTROL_TYPE_BOTH;
                }
                break;
            case R.id.rbCtl2:
                if (checked) {
                    Log.i(TAG, "Control method selected");
                    controlMethod = Constants.CONTROL_TYPE_CTL;
                }
                break;
            case R.id.rbCtl1:
                if (checked) {
                    Log.i(TAG, "Video method selected");
                    controlMethod = Constants.CONTROL_TYPE_VID;
                }
                break;
        }
    }

    /*
     * Simple callback function to pass key inputs to the Controller object.
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        controller.setKey(keyCode, true);
        Log.i(TAG, "KEYDIR: " + keyCode);
        return super.onKeyDown(keyCode, event);
    }
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        controller.setKey(keyCode, false);
        return super.onKeyDown(keyCode, event);
    }
}
