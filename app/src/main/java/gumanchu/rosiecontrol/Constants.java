package gumanchu.rosiecontrol;

import java.util.UUID;

/** Constants Class
 * Created by Freddy on 6/7/2015.
 */
public interface Constants {

    String SERVER_ADDRESS_BLUETOOTH = "98:58:8A:04:40:7D";
    String SERVER_ADDRESS_BLUETOOTH2 = "D8:FC:93:E4:6D:8E";

    int DEFAULT_VIEW = 0;
    int CARDBOARD_VIEW = 1;

    int CONNECTION_TYPE_INET = 0;
    int CONNECTION_TYPE_BTH = 1;

    int CONTROL_TYPE_BOTH = 0;
    int CONTROL_TYPE_VID = 1;
    int CONTROL_TYPE_CTL = 2;

    UUID DEVICE_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    String SERVER_IP = "192.168.0.115";
    int SERVER_PORT = 1234;
}
