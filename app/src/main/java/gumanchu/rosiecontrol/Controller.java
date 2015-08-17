package gumanchu.rosiecontrol;

import android.util.Log;
import android.view.KeyEvent;

/**
 * Class used to process control inputs from the controller and orientation of the device.
 */
public class Controller {
    private static final String TAG = "Controller";

    /*
     * Default constant values for controls.
     */
    public static int keyDirection = 0;
    public static int keyRotation  = 0;
    public static int orientationX = 90;
    public static int orientationY = 90;

    /*
     * Takes input from MainActivity and processes it.
     */
    public void setKey(int key, boolean value) {
        switch (key) {
            /*
             * Directional input for linear movement.
             * Correspond to the left joystick on the Red Samurai controller
             */
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_DPAD_DOWN:
                if (value) {
                    keyDirection = key;
                }
                else
                    keyDirection = 0;
                break;
            /*
             * Rotational input for spinning in place.
             * Correspond to the right joystick on the Red Samurai controller
             */
            case KeyEvent.KEYCODE_L:
            case KeyEvent.KEYCODE_J:
                if (value)
                    keyRotation = key;
                else
                    keyRotation = 0;
                break;
            // Input for escaping the program.  Corresponds to the ESCAPE button on the gamepad.
            case KeyEvent.KEYCODE_ESCAPE:
                keyDirection = key;
                break;
        }
    }

    public static void setOrientation(int x, int y) {
        orientationX = x;
        orientationY = y;
    }



}
