 // TCPServer.java
// A server program implementing TCP socket
package Server;

import java.net.*;
import java.io.*;
import java.util.HashMap;
import java.util.Random;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TCPServer extends Thread {

    private WriteXMLFile_GraphBW_TCPwindows writeXMLFile_GraphBW_TCPwindows = null;
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

    public static Vector<Double> GraphBW_up = null;
    public static Vector<Double> GraphBW_down = null;
    public static Vector<Integer> GraphTCPWindow_up = null;
    public static Vector<Integer> GraphTCPWindow_down = null;
    public static Vector<Double> GraphBW_up_Iperf = null;
    public static Vector<Double> GraphBW_down_Iperf = null;
    public static Vector<Integer> GraphTCPWindow_up_Iperf = null;
    public static Vector<Integer> GraphTCPWindow_down_Iperf = null;
    private int numRuns = 0;

    public TCPServer() {
        try {
            GraphBW_up = new Vector<Double>();
            GraphBW_down = new Vector<Double>();
            GraphTCPWindow_up = new Vector<Integer>();
            GraphTCPWindow_down = new Vector<Integer>();
            GraphBW_up_Iperf = new Vector<Double>();
            GraphBW_down_Iperf = new Vector<Double>();
            GraphTCPWindow_up_Iperf = new Vector<Integer>();
            GraphTCPWindow_down_Iperf = new Vector<Integer>();

            clientSession = new HashMap<>();
            clientBoolean = new HashMap<>();
            clientMeasurement = new HashMap<>();
            listenSocket = new ServerSocket(Constants.SERVERPORT);
            m_clientConnections = new ClientThread[MAX_CLIENTS];
            //ALGORITHM and ALGORITHM_UP are the same except for PGM and PT Methods in which there are just 1 TCP connection for Uplink and Downlink
            ALGORITHM = "MV_Uplink";
            //Algorithms defined for Downlink and Report
            ALGORITHM_DOWN = "MV_Downlink";
            ALGORITHM_REPORT = "MV_Report";
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
                    System.out.println("NUMBER_RUNS=" + numRuns);
                    //Generate Random Integer ID
                    ID = new Random().nextInt();

                    //Send ID to the Client
                    DataInputStream dis = new DataInputStream(clientSocket.getInputStream());
                    DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream());
                    dos.writeInt(ID);
                    dos.flush();
                    isNagleDisable = dis.readBoolean();
//                    Constants.NUMBER_PACKETS = dis.readInt();
//                    Constants.PACKETSIZE = dis.readInt();
                    Constants.BUFFERSIZE = dis.readInt();
                    Constants.SOCKET_RCVBUF = dis.readInt();
                    Constants.SOCKET_SNDBUF = dis.readInt();
                    System.err.println("isNagleDisable: " + isNagleDisable);
                    System.err.println("GAP:" + Constants.PACKET_GAP + "\n"
                            + "NUMBER_PACKETS:" + Constants.NUMBER_PACKETS + "\n"
                            + "PACKETSIZE:" + Constants.PACKETSIZE + "\n"
                            + "BUFFERSIZE:" + Constants.BUFFERSIZE + "\n"
                            + "SO_RCV:" + Constants.SOCKET_RCVBUF + "\n"
                            + "SO_SND:" + Constants.SOCKET_SNDBUF);
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
                    //Run Iperf Server from JAVA
                    //proc = Runtime.getRuntime().exec("iperf3 -s -p 20001");
                    continue;
                }

                if (clientSession.containsKey(ID) && clientBoolean.containsKey(ID) && !clientBoolean.get(ID)) {
                    //Report
                    TCP_param = new TCP_Properties(clientSocket, isNagleDisable);
                    Thread c = new ClientThread(this.ID, ALGORITHM_REPORT, clientSocket, clientMeasurement.get(ID), isNagleDisable);
                    c.start();
                    c.join();
                    /*
                    //Export GraphBW(TCPwindow)
                    if (numRuns == 9) {
                        if (isNagleDisable) {
                            writeXMLFile_GraphBW_TCPwindows = new WriteXMLFile_GraphBW_TCPwindows(ID + "-" + ALGORITHM_REPORT + "-Uplink-NagleOFF", GraphBW_up, GraphTCPWindow_up);
                            writeXMLFile_GraphBW_TCPwindows = new WriteXMLFile_GraphBW_TCPwindows(ID + "-" + ALGORITHM_REPORT + "-Downlink-NagleOFF", GraphBW_down, GraphTCPWindow_down);
                            writeXMLFile_GraphBW_TCPwindows = new WriteXMLFile_GraphBW_TCPwindows(ID + "-" + ALGORITHM_REPORT + "-Uplink_Iperf-NagleOFF", GraphBW_up_Iperf, GraphTCPWindow_up_Iperf);
                            writeXMLFile_GraphBW_TCPwindows = new WriteXMLFile_GraphBW_TCPwindows(ID + "-" + ALGORITHM_REPORT + "-Downlink_Iperf-NagleOFF", GraphBW_down_Iperf, GraphTCPWindow_down_Iperf);
                        } else {
                            writeXMLFile_GraphBW_TCPwindows = new WriteXMLFile_GraphBW_TCPwindows(ID + "-" + ALGORITHM_REPORT + "-Uplink-NagleON", GraphBW_up, GraphTCPWindow_up);
                            writeXMLFile_GraphBW_TCPwindows = new WriteXMLFile_GraphBW_TCPwindows(ID + "-" + ALGORITHM_REPORT + "-Downlink-NagleON", GraphBW_down, GraphTCPWindow_down);
                            writeXMLFile_GraphBW_TCPwindows = new WriteXMLFile_GraphBW_TCPwindows(ID + "-" + ALGORITHM_REPORT + "-Uplink_Iperf-NagleON", GraphBW_up_Iperf, GraphTCPWindow_up_Iperf);
                            writeXMLFile_GraphBW_TCPwindows = new WriteXMLFile_GraphBW_TCPwindows(ID + "-" + ALGORITHM_REPORT + "-Downlink_Iperf-NagleON", GraphBW_down_Iperf, GraphTCPWindow_down_Iperf);
                        }

                        break;
                    }
                    //Increment Number of Runs    
                    numRuns++;
                     */
                } else if (clientSession.containsKey(ID) && clientBoolean.containsKey(ID) && clientBoolean.get(ID)) {
                    //Downlink
                    clientBoolean.put(ID, false);
                    TCP_param = new TCP_Properties(clientSocket, isNagleDisable);
                    Thread c = new ClientThread(this.ID, ALGORITHM_DOWN, clientSocket, clientMeasurement.get(ID), isNagleDisable);
                    c.start();
                    c.join();
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            System.err.println("Server initialization failure " + ex.getMessage());
        } catch (InterruptedException ex) {
            Logger.getLogger(TCPServer.class.getName()).log(Level.SEVERE, null, ex);
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
