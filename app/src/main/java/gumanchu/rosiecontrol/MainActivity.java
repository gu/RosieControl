package gumanchu.rosiecontrol;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.RadioButton;


import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import gumanchu.rosiecontrol.NetworkUtilities.InetHelper;
import gumanchu.rosiecontrol.NetworkUtilities.NetworkHelper;


public class MainActivity extends Activity {

    private static final String TAG = "RosieControl";

    private int controlMethod = Constants.CONTROL_TYPE_BOTH;
    private int connectionType = Constants.CONNECTION_TYPE_INET;
    private int rosieView = Constants.DEFAULT_VIEW;

    private String rosieIP = Constants.SERVER_IP;


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
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    public void startView() {

    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    public void onButtonRosieClick(View view) {

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
