/*
 * Class that defines TCP Socket Properties
 */
package Server;

import java.net.*;

public class TCP_Properties {

    public TCP_Properties(Socket s) throws SocketException {
        //s.setPerformancePreferences(0, 0, 1); 
        
        s.setSendBufferSize(Constants.SOCKET_RCVBUF);
        s.setReceiveBufferSize(Constants.SOCKET_RCVBUF);
        s.setSoTimeout(Constants.SO_TIMEOUT);
        s.setTcpNoDelay(true);//true- Disable Nagle's Algorithm / false-otherwise
        s.setSoLinger(false, 0); //Set to false: The connection will be closed only when the data transmitted 
        //to the socket has been successfully delivered
    }
}
