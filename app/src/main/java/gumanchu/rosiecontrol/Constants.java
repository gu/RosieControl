package gumanchu.rosiecontrol;

import java.util.UUID;

/** Constants Class
 * Created by Freddy on 6/7/2015.
 */
public interface Constants {

    /*
     * Constants used for Bluetooth connection.
     */
    String SERVER_ADDRESS_BLUETOOTH = "98:58:8A:04:FD:97";
    UUID DEVICE_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    /*
     * Constants used for WiFi access.
     */
    String SERVER_IP = "10.0.7.69";
    int SERVER_PORT = 1234;

    /*
     * Constants for radio button values.
     */
    int DEFAULT_VIEW = 0;
    int CARDBOARD_VIEW = 1;
    int CONNECTION_TYPE_INET = 0;
    int CONNECTION_TYPE_BTH = 1;
    int CONTROL_TYPE_BOTH = 0;
    int CONTROL_TYPE_VID = 1;
    int CONTROL_TYPE_CTL = 2;

}
