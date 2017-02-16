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
    private ServerSocket listenSocket = null;
    private TCP_Properties TCP_param = null;
    private ClientThread clientThread = null;
    private DataMeasurement dataMeasurement = null;
    protected static HashMap<Integer, Socket> clientSession = null;
    protected static HashMap<Integer, Boolean> clientBoolean = null;
    protected static HashMap<Integer, DataMeasurement> clientMeasurement = null;

    private boolean keepRunning;
    private String ALGORITHM = null;
    private String ALGORITHM_DOWN = null;
    private String ALGORITHM_REPORT = null;
    private boolean isIperfSettings;
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
            ALGORITHM = "MV_Uplink";
            //Algorithms defined for Downlink and Report
            ALGORITHM_DOWN = "MV_Downlink";
            ALGORITHM_REPORT = "MV_Report";
            isIperfSettings = true; //true - Iperf Settings; false - Thesis Settings
            isNagleDisable=false; //true - Enable Nagle's Algorithm; false - Disable Nagle's Algorithm
            RunShellCommandFromJava("iperf3 -s -p 20001");
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
                    TCP_param = new TCP_Properties(clientSocket,isNagleDisable);
                    clientSession.put(ID, clientSocket);
                    clientBoolean.put(ID, true);
                    clientMeasurement.put(ID, new DataMeasurement());
                    for (int i = 0; i < MAX_CLIENTS; i++) {
                        if (this.m_clientConnections[i] == null) {
                            this.m_clientConnections[i] = new ClientThread(ID, ALGORITHM, clientSocket, clientMeasurement.get(ID), isIperfSettings, isNagleDisable);
                            this.m_clientConnections[i].start();
                            break;
                        }
                    }
                    //Send ID to the Client
                    DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream());
                    dos.writeInt(ID);
                    dos.flush();
                    dos.writeBoolean(isIperfSettings);
                    dos.flush();
                    dos.writeBoolean(isNagleDisable);
                    dos.flush();
                    continue;
                }

                if (clientSession.containsKey(ID) && clientBoolean.containsKey(ID) && !clientBoolean.get(ID)) {
                    //Report
                    TCP_param = new TCP_Properties(clientSocket,isNagleDisable);
                    Thread c = new ClientThread(this.ID, ALGORITHM_REPORT, clientSocket, clientMeasurement.get(ID), isIperfSettings,isNagleDisable);
                    c.start();
                } else if (clientSession.containsKey(ID) && clientBoolean.containsKey(ID) && clientBoolean.get(ID)) {
                    //Downlink
                    clientBoolean.put(ID, false);
                    TCP_param = new TCP_Properties(clientSocket,isNagleDisable);
                    Thread c = new ClientThread(this.ID, ALGORITHM_DOWN, clientSocket, clientMeasurement.get(ID), isIperfSettings,isNagleDisable);
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

    private void RunShellCommandFromJava(String command) {

        try {
            Process proc = Runtime.getRuntime().exec(command);

            // Read the output
            BufferedReader reader
                    = new BufferedReader(new InputStreamReader(proc.getInputStream()));

            String line = "";
            while ((line = reader.readLine()) != null) {
                System.out.print(line + "\n");
            }
            try {
                proc.waitFor();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
