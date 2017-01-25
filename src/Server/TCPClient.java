// TCPClient.java
// A client program implementing TCP socket
package Server;

import java.net.*;
import java.io.*;

//Java Client Main Class
public class TCPClient extends Thread {

    public Socket s = null;
    private Connection connection = null;
    private TCP_Properties TCP_param = null;
    private DataMeasurement dataMeasurement = null;

    public TCPClient() {
        try {
            s = new Socket(Constants.SERVER_IP, Constants.SERVERPORT);
            TCP_param = new TCP_Properties(s);
            dataMeasurement = new DataMeasurement();
            connection = new Connection(s, dataMeasurement);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            connection.start();
            System.err.println("Client started connected to Port: " + Constants.SERVERPORT + "\n");
        } catch (Exception ex) {
            System.err.println("Client connection error: " + ex.getMessage());
        }

    }
}
