 // TCPServer.java
// A server program implementing TCP socket
package Server;

import java.net.*;
import java.io.*;
import java.util.HashMap;
import java.util.Random;

public class TCPServer extends Thread {

    private Socket clientSocket = null;
    private ServerSocket listenSocket = null;
    private TCP_Properties TCP_param = null;
    private ClientThread clientThread = null;
    private DataMeasurement dataMeasurement = null;
    private RunShellCommandsClient runShell = null;
    protected static HashMap<Integer, Socket> clientSession = null;
    protected static HashMap<Integer, Boolean> clientBoolean = null;
    protected static HashMap<Integer, DataMeasurement> clientMeasurement = null;

    private boolean keepRunning;
    private String ALGORITHM = null;
    private String ALGORITHM_DOWN = null;
    private String ALGORITHM_REPORT = null;
    private boolean isNagleDisable;
    private int ID = 0;
    private int MAX_CLIENTS = 30; // Depending on the Method, a client might need to use 3 sockets, so the MAX_CLIENTS is 10.
    private ClientThread[] m_clientConnections = null;

    public TCPServer() {
        try {
            clientSession = new HashMap<>();
            clientBoolean = new HashMap<>();
            clientMeasurement = new HashMap<>();
            listenSocket = new ServerSocket(Constants.SERVERPORT);
            m_clientConnections = new ClientThread[MAX_CLIENTS];
            //ALGORITHM and ALGORITHM_UP are the same except for PGM and PT Methods in which there are just 1 TCP connection for Uplink and Downlink
            ALGORITHM = "MV_readVectorUP";
            //Algorithms defined for Downlink and Report
            ALGORITHM_DOWN = "MV_readVectorDOWN";
            ALGORITHM_REPORT = "MV_Report_readVector";
            //isIperfSettings = _isIperfSettings; //true - Iperf Settings; false - Thesis Settings
            //isNagleDisable = _isNagleDisable; //true - Enable Nagle's Algorithm; false - Disable Nagle's Algorithm
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    @Override
    public void run() {
        keepRunning = true;
        clientSession.clear();
        clientBoolean.clear();
        clientMeasurement.clear();
        try {

            while (keepRunning && listenSocket != null) {
                System.err.println("TCPServer listening on port: " + Constants.SERVERPORT);

                clientSocket = listenSocket.accept();

                try {
                    DataInputStream dis = new DataInputStream(clientSocket.getInputStream());
                    ID = dis.readInt();
                    if (!clientSession.containsKey(ID)) {
                        throw new IOException();
                    }
                } catch (IOException ex) {
                    ID = new Random().nextInt();

                    //Send ID to the Client
                    DataInputStream dis = new DataInputStream(clientSocket.getInputStream());
                    DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream());
                    dos.writeInt(ID);
                    dos.flush();
                    isNagleDisable = dis.readBoolean();
                    Constants.BLOCKSIZE = dis.readInt();
                    Constants.SOCKET_RCVBUF = dis.readInt();
                    Constants.SOCKET_SNDBUF = dis.readInt();
                    System.err.println("isNagleDisable: " + isNagleDisable);
                    System.err.println("BLOCKSIZE:" + Constants.BLOCKSIZE + "\n" + "SO_RCV:" + Constants.SOCKET_RCVBUF + "\n" + "SO_SND:" + Constants.SOCKET_SNDBUF);
                    //Create New Client
                    TCP_param = new TCP_Properties(clientSocket, isNagleDisable);
                    clientSession.put(ID, clientSocket);
                    clientBoolean.put(ID, true);
                    clientMeasurement.put(ID, new DataMeasurement());
                    for (int i = 0; i < MAX_CLIENTS; i++) {
                        if (this.m_clientConnections[i] == null) {
                            this.m_clientConnections[i] = new ClientThread(ID, ALGORITHM, clientSocket, clientMeasurement.get(ID), isNagleDisable);
                            this.m_clientConnections[i].start();
                            break;
                        }
                    }
                    //proc = Runtime.getRuntime().exec("iperf3 -s -p 20001");
                    continue;
                }

                if (clientSession.containsKey(ID) && clientBoolean.containsKey(ID) && !clientBoolean.get(ID)) {
                    //Report
                    TCP_param = new TCP_Properties(clientSocket, isNagleDisable);
                    Thread c = new ClientThread(this.ID, ALGORITHM_REPORT, clientSocket, clientMeasurement.get(ID), isNagleDisable);
                    c.start();
                } else if (clientSession.containsKey(ID) && clientBoolean.containsKey(ID) && clientBoolean.get(ID)) {
                    //Downlink
                    clientBoolean.put(ID, false);
                    TCP_param = new TCP_Properties(clientSocket, isNagleDisable);
                    Thread c = new ClientThread(this.ID, ALGORITHM_DOWN, clientSocket, clientMeasurement.get(ID), isNagleDisable);
                    c.start();

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

    public static void main(String[] args) {
        TCPServer tcpServ = new TCPServer();
        tcpServ.start();
    }
}
