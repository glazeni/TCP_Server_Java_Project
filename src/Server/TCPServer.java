 // TCPServer.java
// A server program implementing TCP socket
package Server;

import java.net.*;
import java.io.*;
import java.nio.channels.Selector;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TCPServer extends Thread {

    private Socket clientSocket = null;
    private Socket clientControl = null;
    private ServerSocket listenSocket = null;
    private TCP_Properties TCP_param = null;
    private ClientThread clientThread = null;
    private DataMeasurement dataMeasurement = null;
    protected static HashMap<String, Socket> clientSession = null;
    protected static HashMap<String, Socket> controlSession = null;
    private boolean keepRunning;
    public static boolean isUplinkDone;
    public static boolean isDownlinkDone;
    public static boolean isControlDone;
    private String ALGORITHM = null;
    public static String clientNumber = null;
    public static int MAX_CLIENTS = 30; // Depending on the Method, a client might need to use 3 sockets, so the MAX_CLIENTS is 10.
    public ClientThread[] m_clientConnections = new ClientThread[MAX_CLIENTS];

    public TCPServer() {
        try {
            clientSession = new HashMap<>();
            controlSession = new HashMap<>();
            listenSocket = new ServerSocket(Constants.SERVERPORT);
            dataMeasurement = new DataMeasurement();
            ALGORITHM = "ACKTiming_UP";
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    @Override
    public void run() {
        keepRunning = true;
        isUplinkDone = false;
        isDownlinkDone = false;
        isControlDone = false;
        clientSession.clear();
        controlSession.clear();
        try {

            while (keepRunning && listenSocket != null) {
                System.err.println("TCPServer listening on port: " + Constants.SERVERPORT);

                clientSocket = listenSocket.accept();
  
                clientNumber = clientSocket.getRemoteSocketAddress().toString().split(":")[0].replace("/", "");
                if (clientSession.containsKey(clientNumber) && isUplinkDone && isDownlinkDone) {
                    clientControl = clientSocket;
                    TCP_param = new TCP_Properties(clientControl);
                    controlSession.put(clientNumber, clientControl);

                    isControlDone = true;
                    isDownlinkDone = false;
                    isUplinkDone = false;

                } else if (clientSession.containsKey(clientNumber) && isUplinkDone) {
                    TCP_param = new TCP_Properties(clientSocket);
                    clientSession.put(clientNumber, clientSocket);

                    isDownlinkDone = true;
                    isControlDone = false;
                    isUplinkDone = true;

                } else {
                    
                    TCP_param = new TCP_Properties(clientSocket);
                    clientSession.put(clientNumber, clientSocket);
                    for (int i = 0; i < MAX_CLIENTS; i++) {
                        if (this.m_clientConnections[i] == null) {
                            this.m_clientConnections[i] = new ClientThread(ALGORITHM, clientNumber, clientSocket, dataMeasurement);
                            this.m_clientConnections[i].start();
                            break;
                        }
                    }
                    isUplinkDone = true;
                    isDownlinkDone = false;
                    isControlDone = false;
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            System.err.println("Server initialization failure " + ex.getMessage());
        } finally {
            keepRunning = false;
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
