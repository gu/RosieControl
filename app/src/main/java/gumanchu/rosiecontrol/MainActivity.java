package gumanchu.rosiecontrol;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;


import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import java.io.Serializable;

import gumanchu.rosiecontrol.NetworkUtilities.BluetoothHelper;
import gumanchu.rosiecontrol.NetworkUtilities.InetHelper;
import gumanchu.rosiecontrol.NetworkUtilities.NetworkHelper;


public class MainActivity extends Activity implements Serializable {

    private static final String TAG = "RosieControl";

    private int controlMethod = Constants.CONTROL_TYPE_BOTH;
    private int connectionType = Constants.CONNECTION_TYPE_INET;
    private int rosieView = Constants.DEFAULT_VIEW;
    private String rosieIP = Constants.SERVER_IP;


    NetworkHelper nHelper;
    Handler handler;
    Intent intent;


    /*
     * UI Elements
     */
    TextView tvStatus;
    RadioButton bt1;
    RadioButton bt2;
    RadioButton bt3;
    RadioButton bt4;
    RadioButton bt5;
    RadioButton bt6;
    RadioButton bt7;


    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    Log.i(TAG, "OpenCV Manager Connected");
                    enableRadio();
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

        handler = new Handler();

        tvStatus = (TextView) findViewById(R.id.tvStatus);

        bt1 = (RadioButton) findViewById(R.id.rbConn1);
        bt2 = (RadioButton) findViewById(R.id.rbConn2);
        bt3 = (RadioButton) findViewById(R.id.rbCtl1);
        bt4 = (RadioButton) findViewById(R.id.rbCtl2);
        bt5 = (RadioButton) findViewById(R.id.rbCtl3);
        bt6 = (RadioButton) findViewById(R.id.rbView1);
        bt7 = (RadioButton) findViewById(R.id.rbView2);

        tvStatus.setText("Initializing OpenCV...");
        AsyncServiceHelper.initOpenCV(OpenCVLoader.OPENCV_VERSION_2_4_11, this, mLoaderCallback);

    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    public void enableRadio() {
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
    }

    public void onButtonRosieClick(View view) {

        tvStatus.setText("Preparing Connection");


        switch (connectionType) {
            case Constants.CONNECTION_TYPE_INET:

                nHelper = new InetHelper();
                nHelper.connect(this);

                handler.postDelayed(new Runnable() {
                    public void run() {
                        initView();
                    }
                }, 5000);

                break;
            case Constants.CONNECTION_TYPE_BTH:

//                nHelper = new BluetoothHelper(this);
//                nHelper.connect();

                break;
        }
    }

    public void initView() {


        if (nHelper.isConnected()) {
            tvStatus.setText("Connected to Rosie via Wifi");
        } else {
            tvStatus.setText("Failed to connect to Rosie via Wifi");
        }

        if (nHelper.isConnected()) {
            //TODO: use serielizable to pass netwokr helper.
            intent.putExtra("")
        }

    }

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
}
