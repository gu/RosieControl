package gumanchu.rosiecontrol.NetworkUtilities;

import java.io.DataInputStream;
import java.io.DataOutputStream;

/**
 * Abstract Class with general network stuff;
 */
public abstract class NetworkHelper {
    public DataInputStream inputStream;
    public DataOutputStream outputStream;

    public abstract void connect();
    public abstract void disconnect();

    public boolean connected = false;

    public DataInputStream getInputStream() {
        if (inputStream != null) {
            return inputStream;
        } else {
            return null;
        }
    }

    public DataOutputStream getOutputStream() {
        if (outputStream != null) {
            return outputStream;
        } else {
            return null;
        }
    }

    public void setInputStream(DataInputStream in) {
        inputStream = in;
    }

    public void setOutputStream(DataOutputStream out) {
        outputStream = out;
    }

    public boolean isConnected() {
        return connected;
    }
}
