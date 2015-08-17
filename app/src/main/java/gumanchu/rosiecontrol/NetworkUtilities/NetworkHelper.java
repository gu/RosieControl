package gumanchu.rosiecontrol.NetworkUtilities;

import android.content.Context;
import android.os.Parcelable;

import java.io.DataInputStream;
import java.io.DataOutputStream;

/**
 * Abstract Class with function headers for networking processes.
 */
public interface NetworkHelper {

    /**
     * Function header to establish a connection between the Android device and Rosie's server
     * application.  There can be different implementations based off of the type of connection,
     * for example Bluetooth, TCP connections, or UDP connections.
     *
     * @param context: Context from the MainActivity in order to show the ProgressDialog object.
     */
    void connect(Context context, String url);

    // TODO: Implement secondary functions.
    void disconnect();
    void read();
    void write();


//    void setInputStream(DataInputStream in);
//
//    DataInputStream getInputStream();
//
//    void setOutputStream(DataOutputStream out);
//
//    DataOutputStream getOutputStream();

    boolean isConnected();

//    void setControls();

}
