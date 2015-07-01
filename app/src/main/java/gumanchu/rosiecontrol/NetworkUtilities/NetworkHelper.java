package gumanchu.rosiecontrol.NetworkUtilities;

import android.content.Context;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.concurrent.CountDownLatch;

/**
 * Abstract Class with general network stuff;
 */
public interface NetworkHelper {

    void connect(Context context);
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

}
