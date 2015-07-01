package gumanchu.rosiecontrol;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

import gumanchu.rosiecontrol.NetworkUtilities.NetworkHelper;

/**
 * Activity to show basic, boring view.
 */
public class DefaultActivity extends Activity {
    private static final String TAG = "DefaultActivity";

    private ImageView defaultView;

    NetworkHelper nHelper;

    public DefaultActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.default_view);

        defaultView = (ImageView) findViewById(R.id.ivDefaultView);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

}
