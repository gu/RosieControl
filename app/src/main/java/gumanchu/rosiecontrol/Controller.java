package gumanchu.rosiecontrol;

import android.util.Log;
import android.view.KeyEvent;

/**
 * Class used for controls
 */
public class Controller {
    private static final String TAG = "Controller";

    public static int keyDirection = 0;
    public static int keyRotation  = 0;
    public static int orientationX = 90;
    public static int orientationY = 90;

    public void setKey(int key) {
        switch (key) {
            case KeyEvent.KEYCODE_DPAD_LEFT:
                Log.i(TAG, "LEFT DOWN");
                keyDirection = key;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                Log.i(TAG, "RIGHT DOWN");
                keyDirection = key;
            case KeyEvent.KEYCODE_DPAD_UP:
                Log.i(TAG, "UP DOWN");
                keyDirection = key;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                Log.i(TAG, "DOWN DOWN");
                keyDirection = key;
            case KeyEvent.KEYCODE_ESCAPE:
                Log.i(TAG, "ESCAPE");
                keyDirection = key;
            case KeyEvent.KEYCODE_L:
                Log.i(TAG, "L DOWN");
                keyRotation = key;
            case KeyEvent.KEYCODE_J:
                Log.i(TAG, "J DOWN");
                keyRotation = key;
        }
    }

    public static void setOrientation(int x, int y) {
        orientationX = x;
        orientationY = y;
    }



}
