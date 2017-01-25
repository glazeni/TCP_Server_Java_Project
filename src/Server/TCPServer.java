 // TCPServer.java
// A server program implementing TCP socket
package Server;

import java.net.*;
import java.io.*;

public class TCPServer extends Thread {

    private Socket clientSocket = null;
    private ServerSocket listenSocket = null;
    private TCP_Properties TCP_param = null;
    private ClientThread clientThread = null;
    private DataMeasurement dataMeasurement = null;

    public TCPServer() {
        try {
            listenSocket = new ServerSocket(Constants.SERVERPORT);
            dataMeasurement = new DataMeasurement();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    @Override
    public void run() {
        try {
            System.err.println("TCPServer listening on port: " + Constants.SERVERPORT);
            clientSocket = listenSocket.accept();
            clientThread = new ClientThread(clientSocket, dataMeasurement);
            TCP_param = new TCP_Properties(clientSocket);
            clientThread.start();

            //Add client to connection list
            ServerUI.connlist.put(clientThread.toString(), clientThread);
            System.out.println(ServerUI.connlist.get(clientThread.toString()));
        } catch (IOException ex) {
            ex.printStackTrace();
            System.err.println("Server initialization failure " + ex.getMessage());
        } finally {
            if (listenSocket != null) {
                try {
                    listenSocket.close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    System.err.println("Closing ServerSocket failure" + ex.getMessage());
                }
            }
        }
    }

}
