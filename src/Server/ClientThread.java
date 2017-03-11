/*
* Class that handles a new thread in ServerSide when a TCP connection is made from ClientSide
 */
package Server;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Random;
import java.util.Vector;
import java.util.concurrent.locks.LockSupport;
import org.apache.commons.math3.distribution.TDistribution;
import org.apache.commons.math3.exception.MathIllegalArgumentException;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.commons.math3.stat.descriptive.moment.Mean;

public class ClientThread extends Thread {

    private Socket clientSocket = null;
    private WriteXMLFile_bytes1sec writeXMLFile_bytes1sec = null;
    private WriteXMLFile_AvailBWVectors writeXMLFile_AvailBWVectors = null;
    private WriteXMLFile_GraphBW_TCPwindows writeXMLFile_GraphBW_TCPwindows = null;
    private RTInputStream RTin = null;
    private RTOutputStream RTout = null;
    private DataInputStream dataIn = null;
    private DataOutputStream dataOut = null;
    private PrintWriter outCtrl = null;
    private BufferedReader inCtrl = null;

    private DataMeasurement dataMeasurement = null;
    private ReminderServer reminderServer = null;
    private Tstudent tstudent = null;
    private Tstudent tstudent_shellUP = null;
    private Tstudent tstudent_shellDOWN = null;
    private boolean isAlgorithmDone = false;
    private boolean isThreadMethod;
    private boolean isNagleDisable;
    private String METHOD = null; // PT-PacketTrain; MV-Moving Average; 

    private Vector<Double> AvailableBW_up = null;
    private Vector<Double> AvailableBW_down = null;
    private Vector<Integer> ByteSecondVector = null;

    private int ID = 0;
    private int byteCnt = 0;
    private long runningTime = 10000;

    public ClientThread(int _ID, String _METHOD, Socket _clientSocket, DataMeasurement _dataMeasurement, boolean _isNagleDisable) {
        try {
            this.ID = _ID;
            this.METHOD = _METHOD;
            this.clientSocket = _clientSocket;
            this.dataMeasurement = _dataMeasurement;
            this.isNagleDisable = _isNagleDisable;
            RTin = new RTInputStream(clientSocket.getInputStream());
            RTout = new RTOutputStream(clientSocket.getOutputStream());
            dataIn = new DataInputStream(RTin);
            dataOut = new DataOutputStream(RTout);
            outCtrl = new PrintWriter(clientSocket.getOutputStream(), true);
            inCtrl = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            AvailableBW_up = new Vector<Double>();
            AvailableBW_down = new Vector<Double>();
            ByteSecondVector = new Vector<Integer>();
        } catch (IOException ex) {
            System.out.println("Client Thread Failure:" + ex.getMessage());
        }
    }

    //Runnable Data Receiver
    @Override
    public void run() {
        try {
            dataOut.writeUTF(METHOD);
            switch (METHOD) {
                case "PT_Uplink":
                    Method_PT_Uplink();
                    break;
                case "PT_Downlink":
                    Method_PT_Downlink();
                    break;
                case "PT_Report":
                    Method_PT_Report();
                    break;
                case "MV_Uplink":
                    isThreadMethod = true;
                    Method_MV_Uplink_Server();
                    break;
                case "MV_Downlink":
                    isThreadMethod = true;
                    Method_MV_Downlink_Server();
                    break;
                case "MV_Report":
                    Method_MV_Report_Server();
                    break;
                case "MV_readVectorUP":
                    isThreadMethod = false;
                    Method_MV_UP_readVector_Server();
                    break;
                case "MV_readVectorDOWN":
                    isThreadMethod = false;
                    Method_MV_DOWN_readVector_Server();
                    break;
                case "MV_Report_readVector":
                    Method_MV_Report_readVector_Server();
                    break;
                default:
                    System.err.println("INVALID MEHTHOD");
                    break;
            }

        } catch (Exception ex) {
            System.err.println("Receiving Data Failure: " + ex.getMessage());
            ex.printStackTrace();
        } finally {
            try {
                if (clientSocket != null) {
                    clientSocket.close();
                    System.out.println("                                                     clientSocket CLOSED!");
                }
                if (isAlgorithmDone) {
                    TCPServer.clientSession.remove(this.ID);
                    TCPServer.clientBoolean.remove(this.ID);
                    TCPServer.clientMeasurement.remove(this.ID);
                }

            } catch (Exception ex) {
                System.err.println("Closing Server Side Socket Failure" + ex.getMessage());
            }
        }
    }

    private double uplink_Server_rcv() {
        int num_packets = 0;
        String inputLine = "";
        int counter = 0;
        int singlePktSize = 0;
        long startTime = 0;
        long endTime = 0;
        double gapTimeSrv = 0.0;
        double gapTimeClt = 0.0;
        double byteCounter = 0.0;
        double estTotalUpBandWidth = 0.0;
        double estAvailiableUpBandWidth = 0.0;
        double availableBWFraction = 1.0;
        try {
            System.out.println("uplink_Server_rcv STARTED!");
            //Receive Packet Train
            while ((inputLine = inCtrl.readLine()) != null) {
                if (startTime == 0) {
                    startTime = System.currentTimeMillis();
                    singlePktSize = inputLine.length();
                }

                byteCounter += inputLine.length();

                System.out.println("Received the " + (counter) + " message with size: " + inputLine.length());
                // increase the counter which is equal to the number of packets
                counter++;
                //read "END" msg
                if (inputLine.substring(0, Constants.FINAL_MSG.length()).equals(Constants.FINAL_MSG)) {
                    gapTimeClt = Double.parseDouble(inputLine.substring(Constants.FINAL_MSG.length() + 1));
                    System.out.println("Detect last uplink link message with GAP=" + gapTimeClt);
                    break;
                }
            }
            endTime = System.currentTimeMillis();

        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            gapTimeSrv = endTime - startTime;
            // Bandwidth calculation
            // 1 Mbit/s = 125 Byte/ms 
            estTotalUpBandWidth = byteCounter / gapTimeSrv / 125.0;
            availableBWFraction = Math.min(gapTimeClt / gapTimeSrv, 1.0);
            estAvailiableUpBandWidth = estTotalUpBandWidth / availableBWFraction;

            // Display information at the server side
            System.out.println("Receive single Pkt size is " + singlePktSize + " Bytes.");
            System.out.println("Total receiving " + counter + " packets.");
            System.out.println("Client gap time is " + gapTimeClt + " ms.");
            System.out.println("Total package received " + byteCounter + " Bytes with " + gapTimeSrv + " ms total GAP.");
            System.out.println("Estimated Total upload bandwidth is " + estTotalUpBandWidth + " Mbits/sec.");
            System.out.println("Availabe fraction is " + availableBWFraction);
            System.out.println("Estimated Available upload bandwidth is " + estAvailiableUpBandWidth + " Mbits/sec.");
            System.out.println("uplink_Server_rcv DONE");
        }
        return estAvailiableUpBandWidth;
    }

    private boolean uplink_Server_rcvInSeconds(long _end) {
        try {
            byte[] rcv_buf = new byte[Constants.BUFFERSIZE];
            int n = 0;
            System.out.println("\nuplink_Server_rcvInSeconds STARTED!");
            //Initialize Timer
            if (isThreadMethod) {
                reminderServer = new ReminderServer(1, this.dataMeasurement, this.RTin);
            }
            while (System.currentTimeMillis() < _end) {
                byteCnt = 0;
                //Cycle to read each block
                //do {
                n = RTin.read(rcv_buf, byteCnt, Constants.BUFFERSIZE - byteCnt);
                //n= RTin.read(rcv_buf);
                if (n > 0) {
                    byteCnt += n;
                    if (!isThreadMethod) {
                        dataMeasurement.add_SampleReadTime(n, System.currentTimeMillis());
                    }

                } else {
                    System.out.println("Read n<0");
                    break;
                }

                //} while ((n > 0) && (byteCnt < Constants.BUFFERSIZE));
                if (n == -1) {
                    System.out.println("Exited with n=-1");
                    break;
                }
            }
            return true;
        } catch (IOException ex) {
            return false;
        } finally {
            if (isThreadMethod) {
                reminderServer.cancelTimer();
            }
            System.out.println("\nuplink_Server_rcvInSeconds DONE!");
        }

    }

    private boolean downlink_Server_sndInSeconds() {
        System.out.println("\ndownlink_Server_sndInSeconds STARTED!");
        boolean keepRunning = true;
        try {
            byte[] snd_buf = new byte[Constants.BUFFERSIZE];
            new Random().nextBytes(snd_buf);
            while (keepRunning) {
                RTout.write(snd_buf);
            }
            return true;
        } catch (IOException ex) {
            return false;
        } finally {
            System.out.println("\ndownlink_Server_sndInSeconds DONE!");
            keepRunning = false;
        }
    }

    private void downlink_Server_snd() {
        int counter = 0;
        long beforeTime = 0;
        long afterTime = 0;
        double diffTime = 0;
        try {
            System.out.println("downlink_Server_snd STARTED!");

            byte[] payload = new byte[Constants.PACKETSIZE];
            Random rand = new Random();
            // Randomize the payload with chars between 'a' to 'z' and 'A' to 'Z'  to assure there is no "\r\n"
            for (int i = 0; i < payload.length; i++) {
                payload[i] = (byte) ('A' + rand.nextInt(52));
            }
            //Send Packet Train
            while (counter < Constants.NUMBER_PACKETS) {
                // start recording the first packet send time
                if (beforeTime == 0) {
                    beforeTime = System.currentTimeMillis();
                }
                // send packet with constant gap
                outCtrl.println(new String(payload));
                outCtrl.flush();

                // create train gap
                try {
                    LockSupport.parkNanos(Constants.PACKET_GAP);
                    //Thread.sleep(Constants.PACKET_GAP);

                } catch (Exception e) {
                    e.printStackTrace();
                }
                counter++;
            }
            afterTime = System.currentTimeMillis();
            diffTime = afterTime - beforeTime;
            outCtrl.println("END:" + diffTime);
            outCtrl.flush();
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            System.out.println("downlink_Server_snd DONE");
        }
    }

    private void Method_PT_Uplink() {
        System.out.println("Method_PT_Uplink with BufferSize=" + Constants.BUFFERSIZE + "& PacketSize=" + Constants.PACKETSIZE);
        //Measurements
        Vector<Integer> GAP_PacketSizeVector = new Vector<Integer>();
        try {
            //Uplink
            dataOut.writeByte(1);
            for (int p = 1; p < 11; p++) {
                dataOut.writeByte(1);
                double BW = uplink_Server_rcv();
                AvailableBW_up.add(BW);
                GAP_PacketSizeVector.add(512*p);
            }

        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            //GraphBW(PACKET_GAP)
            writeXMLFile_GraphBW_TCPwindows = new WriteXMLFile_GraphBW_TCPwindows(ID + "PacketTrain-Uplink", AvailableBW_up, GAP_PacketSizeVector);

            //Export to XML
//            tstudent = new Tstudent(AvailableBW_up, true);
//            if (isNagleDisable) {
//                writeXMLFile_AvailBWVectors = new WriteXMLFile_AvailBWVectors(ID + " PT-AvalBW_uplink_NagleOFF", AvailableBW_up, tstudent.getTotalBytes(), tstudent.getMeanVector(), tstudent.getLowerBoundVector(), tstudent.getUpperBoundVector());
//            } else {
//                writeXMLFile_AvailBWVectors = new WriteXMLFile_AvailBWVectors(ID + " PT-AvalBW_uplink_NagleON", AvailableBW_up, tstudent.getTotalBytes(), tstudent.getMeanVector(), tstudent.getLowerBoundVector(), tstudent.getUpperBoundVector());
//            }
        }
    }

    private void Method_PT_Downlink() {
        System.out.println("Method_PT_Downlink with BufferSize=" + Constants.BUFFERSIZE + "& PacketSize=" + Constants.PACKETSIZE);
        dataMeasurement.GAP_PacketSizeVector.clear();
        try {
            //Downlink
            dataOut.writeByte(2);
            for (int p = 1; p < 11; p++) {
//                Constants.PACKET_GAP = (int) Math.pow(10, p);
//                dataMeasurement.GAP_PacketSizeVector.add((int) Constants.PACKET_GAP);
//                System.out.println("PACKET_GAP=" + Constants.PACKET_GAP);
                Constants.PACKETSIZE = 1460*p;
                dataMeasurement.GAP_PacketSizeVector.add((int) Constants.PACKETSIZE);
                System.out.println("PACKET_SIZE=" + Constants.PACKETSIZE);                
                dataIn.readByte(); 
                downlink_Server_snd();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void Method_PT_Report() {
        //Receive Report Measurements - AvailableBW_down Vector
        AvailableBW_down.clear();
        dataMeasurement.ByteSecondShell_up.clear();
        dataMeasurement.ByteSecondShell_down.clear();
        tstudent = null;
        tstudent_shellUP = null;
        tstudent_shellDOWN = null;
        try {
            //Receive AvailableBW_down Vector
            dataIn.readByte();
            int length = dataIn.readInt();
            for (int k = 0; k < length; k++) {
                AvailableBW_down.add(dataIn.readDouble());
            }
            //Receive ByteSecondShell Up
            int length_shellUP = dataIn.readInt();
            for (int k = 0; k < length_shellUP; k++) {
                dataMeasurement.ByteSecondShell_up.add(dataIn.readInt());
            }
            //Receive ByteSecondShell Up
            int length_shellDOWN = dataIn.readInt();
            for (int k = 0; k < length_shellDOWN; k++) {
                dataMeasurement.ByteSecondShell_down.add(dataIn.readInt());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            tstudent_shellUP = new Tstudent(dataMeasurement.ByteSecondShell_up);
            tstudent_shellDOWN = new Tstudent(dataMeasurement.ByteSecondShell_down);
            writeXMLFile_GraphBW_TCPwindows = new WriteXMLFile_GraphBW_TCPwindows(ID + "PacketTrain-Downlink", AvailableBW_down, dataMeasurement.GAP_PacketSizeVector);
            writeXMLFile_GraphBW_TCPwindows = new WriteXMLFile_GraphBW_TCPwindows(ID + "PacketTrain-Uplink_Iperf", tstudent_shellUP.getMeanVector(), true);
            writeXMLFile_GraphBW_TCPwindows = new WriteXMLFile_GraphBW_TCPwindows(ID + "PacketTrain-Downlink_Iperf", tstudent_shellDOWN.getMeanVector(), true);

//            tstudent = new Tstudent(AvailableBW_down, true);
//            tstudent_shellUP = new Tstudent(dataMeasurement.ByteSecondShell_up);
//            tstudent_shellDOWN = new Tstudent(dataMeasurement.ByteSecondShell_down);
//            if (isNagleDisable) {
//                writeXMLFile_AvailBWVectors = new WriteXMLFile_AvailBWVectors(ID + " PT-AvalBW_downlink_NagleOFF", AvailableBW_down, tstudent.getTotalBytes(), tstudent.getMeanVector(), tstudent.getLowerBoundVector(), tstudent.getUpperBoundVector());
//                writeXMLFile_AvailBWVectors = new WriteXMLFile_AvailBWVectors(ID + " PT-iperfShell_uplink_NagleOFF", dataMeasurement.ByteSecondShell_up, tstudent_shellUP.getTotalBytes(), tstudent_shellUP.getMeanVector(), tstudent_shellUP.getLowerBoundVector(), tstudent_shellUP.getUpperBoundVector(), true);
//                writeXMLFile_AvailBWVectors = new WriteXMLFile_AvailBWVectors(ID + " PT-iperfShell_downlink_NagleOFF", dataMeasurement.ByteSecondShell_down, tstudent_shellDOWN.getTotalBytes(), tstudent_shellDOWN.getMeanVector(), tstudent_shellDOWN.getLowerBoundVector(), tstudent_shellDOWN.getUpperBoundVector(), true);
//            } else {
//                writeXMLFile_AvailBWVectors = new WriteXMLFile_AvailBWVectors(ID + " PT-AvalBW_downlink_NagleON", AvailableBW_down, tstudent.getTotalBytes(), tstudent.getMeanVector(), tstudent.getLowerBoundVector(), tstudent.getUpperBoundVector());
//                writeXMLFile_AvailBWVectors = new WriteXMLFile_AvailBWVectors(ID + " PT-iperfShell_uplink_NagleON", dataMeasurement.ByteSecondShell_up, tstudent_shellUP.getTotalBytes(), tstudent_shellUP.getMeanVector(), tstudent_shellUP.getLowerBoundVector(), tstudent_shellUP.getUpperBoundVector(), true);
//                writeXMLFile_AvailBWVectors = new WriteXMLFile_AvailBWVectors(ID + " PT-iperfShell_downlink_NagleON", dataMeasurement.ByteSecondShell_down, tstudent_shellDOWN.getTotalBytes(), tstudent_shellDOWN.getMeanVector(), tstudent_shellDOWN.getLowerBoundVector(), tstudent_shellDOWN.getUpperBoundVector(), true);
//            }
            isAlgorithmDone = true;
            System.out.println("Method_PT_Client along with Report is done!");
        }

    }

    private void Method_MV_Uplink_Server() {
        System.out.println("MV_Uplink_1secThread with TCP_SND/RCV_Windows=" + Constants.SOCKET_RCVBUF + " & BufferSize=" + Constants.BUFFERSIZE);
        //Measurements
        dataMeasurement.SampleSecond_up.clear();
        try {
            //Uplink
            dataOut.writeByte(1);
            long end = System.currentTimeMillis() + runningTime;
            uplink_Server_rcvInSeconds(end);
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            try {
                //GraphBW(TCPWindows)
                TCPServer.GraphBW_up.add(getMean(dataMeasurement.SampleSecond_up));
                TCPServer.GraphTCPWindow_up.add(Constants.SOCKET_RCVBUF);

                //Export to XML
                tstudent = new Tstudent(dataMeasurement.SampleSecond_up);
                if (isNagleDisable) {
                    writeXMLFile_bytes1sec = new WriteXMLFile_bytes1sec(ID + " thesisSettings_Uplink-MV-1secBytes_NagleOFF", dataMeasurement.SampleSecond_up, tstudent.getTotalBytes(), tstudent.getMeanVector(), tstudent.getLowerBoundVector(), tstudent.getUpperBoundVector(), "MV_1secThread/");
                } else {
                    writeXMLFile_bytes1sec = new WriteXMLFile_bytes1sec(ID + " thesisSettings_Uplink-MV-1secBytes_NagleON", dataMeasurement.SampleSecond_up, tstudent.getTotalBytes(), tstudent.getMeanVector(), tstudent.getLowerBoundVector(), tstudent.getUpperBoundVector(), "MV_1secThread/");
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private void Method_MV_Downlink_Server() {
        System.out.println("MV_Downlink_1secThread with TCP_SND/RCV_Windows=" + Constants.SOCKET_RCVBUF + " & BufferSize=" + Constants.BUFFERSIZE);
        //Measurements
        try {
            //Downlink
            dataOut.writeByte(2);
            downlink_Server_sndInSeconds();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void Method_MV_Report_Server() {
        //Receive Report - Sample Second Vector 
        dataMeasurement.SampleSecond_down.clear();
        dataMeasurement.ByteSecondShell_down.clear();
        dataMeasurement.ByteSecondShell_up.clear();
        try {
            //Report MV_Downlink
            dataIn.readByte();
            int length = dataIn.readInt();
            for (int l = 0; l < length; l++) {
                int bytecnt = dataIn.readInt();
                dataMeasurement.SampleSecond_down.add(bytecnt);
            }
            //Report Shell Vector from terminal Uplink
            int length_shellUP = dataIn.readInt();
            for (int b = 0; b < length_shellUP; b++) {
                dataMeasurement.ByteSecondShell_up.add(dataIn.readInt());
                System.out.println("ByteSecondShell_UP: " + dataMeasurement.ByteSecondShell_up.get(b));
            }
            //Report Shell Vector from terminal Downlink
            int length_shellDOWN = dataIn.readInt();
            for (int b = 0; b < length_shellDOWN; b++) {
                dataMeasurement.ByteSecondShell_down.add(dataIn.readInt());
                System.out.println("ByteSecondShell_DOWN: " + dataMeasurement.ByteSecondShell_down.get(b));
            }

        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            //GraphBW(TCPwindow)
            TCPServer.GraphBW_down.add(getMean(dataMeasurement.SampleSecond_down));
            TCPServer.GraphTCPWindow_down.add(Constants.SOCKET_RCVBUF);
            TCPServer.GraphBW_up_Iperf.add(getMean(dataMeasurement.ByteSecondShell_up));
            TCPServer.GraphTCPWindow_up_Iperf.add(Constants.SOCKET_RCVBUF);
            TCPServer.GraphBW_down_Iperf.add(getMean(dataMeasurement.ByteSecondShell_down));
            TCPServer.GraphTCPWindow_down_Iperf.add(Constants.SOCKET_RCVBUF);

            //Export to XML
            tstudent = new Tstudent(dataMeasurement.SampleSecond_down);
            tstudent_shellUP = new Tstudent(dataMeasurement.ByteSecondShell_up);
            tstudent_shellDOWN = new Tstudent(dataMeasurement.ByteSecondShell_down);
            if (isNagleDisable) {
                writeXMLFile_bytes1sec = new WriteXMLFile_bytes1sec(ID + " thesisSettings_Downlink-MV-1secBytes_NagleOFF", dataMeasurement.SampleSecond_down, tstudent.getTotalBytes(), tstudent.getMeanVector(), tstudent.getLowerBoundVector(), tstudent.getUpperBoundVector(), "MV_1secThread/");
                writeXMLFile_bytes1sec = new WriteXMLFile_bytes1sec(ID + " thesisSettings_iperfShell_Uplink-MV-1secBytes_NagleOFF", dataMeasurement.ByteSecondShell_up, tstudent_shellUP.getTotalBytes(), tstudent_shellUP.getMeanVector(), tstudent_shellUP.getLowerBoundVector(), tstudent_shellUP.getUpperBoundVector(), "MV_1secThread/");
                writeXMLFile_bytes1sec = new WriteXMLFile_bytes1sec(ID + " thesisSettings_iperfShell_Downlink-MV-1secBytes_NagleOFF", dataMeasurement.ByteSecondShell_down, tstudent_shellDOWN.getTotalBytes(), tstudent_shellDOWN.getMeanVector(), tstudent_shellDOWN.getLowerBoundVector(), tstudent_shellDOWN.getUpperBoundVector(), "MV_1secThread/");
            } else {
                writeXMLFile_bytes1sec = new WriteXMLFile_bytes1sec(ID + " thesisSettings_Downlink-MV-1secBytes_NagleON", dataMeasurement.SampleSecond_down, tstudent.getTotalBytes(), tstudent.getMeanVector(), tstudent.getLowerBoundVector(), tstudent.getUpperBoundVector(), "MV_1secThread/");
                writeXMLFile_bytes1sec = new WriteXMLFile_bytes1sec(ID + " thesisSettings_iperfShell_Uplink-MV-1secBytes_NagleON", dataMeasurement.ByteSecondShell_up, tstudent_shellUP.getTotalBytes(), tstudent_shellUP.getMeanVector(), tstudent_shellUP.getLowerBoundVector(), tstudent_shellUP.getUpperBoundVector(), "MV_1secThread/");
                writeXMLFile_bytes1sec = new WriteXMLFile_bytes1sec(ID + " thesisSettings_iperfShell_Downlink-MV-1secBytes_NagleON", dataMeasurement.ByteSecondShell_down, tstudent_shellDOWN.getTotalBytes(), tstudent_shellDOWN.getMeanVector(), tstudent_shellDOWN.getLowerBoundVector(), tstudent_shellDOWN.getUpperBoundVector(), "MV_1secThread/");
            }
            isAlgorithmDone = true;
            System.out.println("Method_MV_Server along with Report is done!");
        }
    }

    private void Method_MV_UP_readVector_Server() {
        System.out.println("MV_Uplink_readVector with TCP_SND/RCV_Windows=" + Constants.SOCKET_RCVBUF + " & BufferSize=" + Constants.BUFFERSIZE);
        //Measurements
        dataMeasurement.SampleReadTime.clear();
        ByteSecondVector.clear();
        try {
            //Uplink
            dataOut.writeByte(1);
            long end = System.currentTimeMillis() + runningTime;
            uplink_Server_rcvInSeconds(end);
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            try {
                //Calculate Moving Average
                ByteSecondVector = MovingAverageCalculation(dataMeasurement.SampleReadTime);

                //GraphBW(TCPWindows)
                TCPServer.GraphBW_up.add(getMean(ByteSecondVector));
                TCPServer.GraphTCPWindow_up.add(Constants.SOCKET_RCVBUF);

                //Export to XML
                tstudent = new Tstudent(ByteSecondVector);
                if (isNagleDisable) {
                    writeXMLFile_bytes1sec = new WriteXMLFile_bytes1sec(ID + " thesisSettings_Uplink-MV_readVector_NagleOFF", ByteSecondVector, tstudent.getTotalBytes(), tstudent.getMeanVector(), tstudent.getLowerBoundVector(), tstudent.getUpperBoundVector(), "MV_readVector/");
                } else {
                    writeXMLFile_bytes1sec = new WriteXMLFile_bytes1sec(ID + " thesisSettings_Uplink-MV_readVector_NagleON", ByteSecondVector, tstudent.getTotalBytes(), tstudent.getMeanVector(), tstudent.getLowerBoundVector(), tstudent.getUpperBoundVector(), "MV_readVector/");
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private void Method_MV_DOWN_readVector_Server() {
        System.out.println("MV_Downlink_readVector with TCP_SND/RCV_Windows=" + Constants.SOCKET_RCVBUF + " & BufferSize=" + Constants.BUFFERSIZE);
        //Measurements
        try {
            //Uplink
            dataOut.writeByte(2);
            downlink_Server_sndInSeconds();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void Method_MV_Report_readVector_Server() {
        //Receive Report - Sample Second Vector 
        dataMeasurement.SampleReadTime.clear();
        dataMeasurement.ByteSecondShell_up.clear();
        dataMeasurement.ByteSecondShell_down.clear();
        ByteSecondVector.clear();
        try {
            dataIn.readByte();
            //Report MV_readVector_Downlink
            int length = dataIn.readInt();
            for (int l = 0; l < length; l++) {
                int bytecnt = dataIn.readInt();
                long sampleTime = dataIn.readLong();
                dataMeasurement.SampleReadTime.add(new DataSecond(bytecnt, sampleTime));
            }
            //Report Shell Vector from terminal Uplink
            int length_shellUP = dataIn.readInt();
            for (int b = 0; b < length_shellUP; b++) {
                dataMeasurement.ByteSecondShell_up.add(dataIn.readInt());
                System.out.println("ByteSecondShell_UP: " + dataMeasurement.ByteSecondShell_up.get(b));
            }
            //Report Shell Vector from terminal Downlink
            int length_shellDOWN = dataIn.readInt();
            for (int b = 0; b < length_shellDOWN; b++) {
                dataMeasurement.ByteSecondShell_down.add(dataIn.readInt());
                System.out.println("ByteSecondShell_DOWN: " + dataMeasurement.ByteSecondShell_down.get(b));
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            //Calculate Moving Average
            ByteSecondVector = MovingAverageCalculation(dataMeasurement.SampleReadTime);

            //GraphBW(TCPwindow)
            TCPServer.GraphBW_down.add(getMean(ByteSecondVector));
            TCPServer.GraphTCPWindow_down.add(Constants.SOCKET_RCVBUF);
            TCPServer.GraphBW_up_Iperf.add(getMean(dataMeasurement.ByteSecondShell_up));
            TCPServer.GraphTCPWindow_up_Iperf.add(Constants.SOCKET_RCVBUF);
            TCPServer.GraphBW_down_Iperf.add(getMean(dataMeasurement.ByteSecondShell_down));
            TCPServer.GraphTCPWindow_down_Iperf.add(Constants.SOCKET_RCVBUF);

            //Export to XML
            tstudent = new Tstudent(ByteSecondVector);
            tstudent_shellUP = new Tstudent(dataMeasurement.ByteSecondShell_up);
            tstudent_shellDOWN = new Tstudent(dataMeasurement.ByteSecondShell_down);
            if (isNagleDisable) {
                writeXMLFile_bytes1sec = new WriteXMLFile_bytes1sec(ID + " thesisSettings_Downlink-MV_readVector_NagleOFF", ByteSecondVector, tstudent.getTotalBytes(), tstudent.getMeanVector(), tstudent.getLowerBoundVector(), tstudent.getUpperBoundVector(), "MV_readVector/");
                writeXMLFile_bytes1sec = new WriteXMLFile_bytes1sec(ID + " thesisSettings_iperfShell_Uplink-MV_readVector_NagleOFF", dataMeasurement.ByteSecondShell_up, tstudent_shellUP.getTotalBytes(), tstudent_shellUP.getMeanVector(), tstudent_shellUP.getLowerBoundVector(), tstudent_shellUP.getUpperBoundVector(), "MV_readVector/");
                writeXMLFile_bytes1sec = new WriteXMLFile_bytes1sec(ID + " thesisSettings_iperfShell_Downlink-MV_readVector_NagleOFF", dataMeasurement.ByteSecondShell_down, tstudent_shellDOWN.getTotalBytes(), tstudent_shellDOWN.getMeanVector(), tstudent_shellDOWN.getLowerBoundVector(), tstudent_shellDOWN.getUpperBoundVector(), "MV_readVector/");
            } else {
                writeXMLFile_bytes1sec = new WriteXMLFile_bytes1sec(ID + " thesisSettings_Downlink-MV_readVector_NagleON", ByteSecondVector, tstudent.getTotalBytes(), tstudent.getMeanVector(), tstudent.getLowerBoundVector(), tstudent.getUpperBoundVector(), "MV_readVector/");
                writeXMLFile_bytes1sec = new WriteXMLFile_bytes1sec(ID + " thesisSettings_iperfShell_Uplink-MV_readVector_NagleON", dataMeasurement.ByteSecondShell_up, tstudent_shellUP.getTotalBytes(), tstudent_shellUP.getMeanVector(), tstudent_shellUP.getLowerBoundVector(), tstudent_shellUP.getUpperBoundVector(), "MV_readVector/");
                writeXMLFile_bytes1sec = new WriteXMLFile_bytes1sec(ID + " thesisSettings_iperfShell_Downlink-MV_readVector_NagleON", dataMeasurement.ByteSecondShell_down, tstudent_shellDOWN.getTotalBytes(), tstudent_shellDOWN.getMeanVector(), tstudent_shellDOWN.getLowerBoundVector(), tstudent_shellDOWN.getUpperBoundVector(), "MV_readVector/");
            }

            isAlgorithmDone = true;
            System.out.println("Method_MV_readVector_Server along with Report is done!");
        }
    }

    private Vector<Integer> MovingAverageCalculation(Vector<DataSecond> Vector_Read_or_Write) {
        int i, j = 0, bytesTotal = 0;
        double average = 0;
        ByteSecondVector.clear();
        System.out.println("Size:  " + Vector_Read_or_Write.size());
        for (i = 0; i < Vector_Read_or_Write.size(); i++) {
            bytesTotal = 0;

            while ((Vector_Read_or_Write.get(j).sampleTime - Vector_Read_or_Write.get(i).sampleTime) < 1000) {
                if (j == Vector_Read_or_Write.size() - 1) {
                    break;
                }
                //Multiply by 8 to convert bytes to bits
                bytesTotal += Vector_Read_or_Write.get(j).bytesRead * 8;
                j++;
            }
            ByteSecondVector.add(bytesTotal);
            //average = bytesTotal / (Vector_Read_or_Write.get(j).sampleTime - Vector_Read_or_Write.get(i).sampleTime);
            System.out.println("Second Interval = [" + i + "," + j + "]" + " with bytesTotal=" + bytesTotal); // + "and Capacity=" + average);
            i = j;
        }
        return ByteSecondVector;

    }

    private class Tstudent {

        private SummaryStatistics stats = null;
        private Vector<Double> MeanVector = null;
        private Vector<Double> LowerBoundVector = null;
        private Vector<Double> UpperBoundVector = null;
        private int total = 0;

        private Tstudent(Vector<Integer> Vector) {
            stats = new SummaryStatistics();
            MeanVector = new Vector<Double>();
            LowerBoundVector = new Vector<Double>();
            UpperBoundVector = new Vector<Double>();

            //Caculate Capacity Graph in intervals from [0,1],[0,2] ... [0,30]
            for (int i = 1; i < Vector.size(); i++) {
                total += Vector.get(i);
                stats.clear();
                for (int j = 0; j <= i; j++) {
                    stats.addValue(Vector.get(j));
//                    System.out.println("Interval [" + j + "," + i + "]");
                }
                // Calculate 90% confidence interval
                double ci = calcMeanCI(stats, 0.90);
                double Mean = stats.getMean();
                double lower_bound = stats.getMean() - ci;
                double upper_bound = stats.getMean() + ci;
                MeanVector.add(Mean);
                LowerBoundVector.add(lower_bound);
                UpperBoundVector.add(upper_bound);
//                System.out.println("Total Bytes = " + total);
//                System.out.println("Mean" + i + " = " + stats.getMean());
//                System.out.println(String.format("Confidence Interval 90%%: %f, %f", lower_bound, upper_bound));
            }
        }

        private Tstudent(Vector<Double> Vector, boolean isDouble) {
            stats = new SummaryStatistics();
            MeanVector = new Vector<Double>();
            LowerBoundVector = new Vector<Double>();
            UpperBoundVector = new Vector<Double>();

            //Caculate Capacity Graph in intervals from [0,1],[0,2] ... [0,30]
            for (int i = 1; i < Vector.size(); i++) {
                total += Vector.get(i);
                stats.clear();
                for (int j = 0; j <= i; j++) {
                    stats.addValue(Vector.get(j));
//                    System.out.println("Interval [" + j + "," + i + "]");
                }
                // Calculate 90% confidence interval
                double ci = calcMeanCI(stats, 0.90);
                double Mean = stats.getMean();
                double lower_bound = stats.getMean() - ci;
                double upper_bound = stats.getMean() + ci;
                MeanVector.add(Mean);
                LowerBoundVector.add(lower_bound);
                UpperBoundVector.add(upper_bound);
//                System.out.println("Total Bytes = " + total);
//                System.out.println("Mean" + i + " = " + stats.getMean());
//                System.out.println(String.format("Confidence Interval 90%%: %f, %f", lower_bound, upper_bound));
            }
        }

        public int getTotalBytes() {
            return total;
        }

        public Vector<Double> getMeanVector() {
            return MeanVector;
        }

        public Vector<Double> getLowerBoundVector() {
            return LowerBoundVector;
        }

        public Vector<Double> getUpperBoundVector() {
            return UpperBoundVector;
        }
    }

    private static double calcMeanCI(SummaryStatistics stats, double level) {
        try {
            // Create T Distribution with N-1 degrees of freedom
            TDistribution tDist = new TDistribution(stats.getN() - 1);
            // Calculate critical value
            double critVal = tDist.inverseCumulativeProbability(1.0 - (1 - level) / 2);
            // Calculate confidence interval
            return critVal * stats.getStandardDeviation() / Math.sqrt(stats.getN());
        } catch (MathIllegalArgumentException e) {
            return Double.NaN;
        }
    }

    private double getMean(Vector<Integer> Vector) {
        SummaryStatistics stats = new SummaryStatistics();
        for (int i = 0; i < Vector.size(); i++) {
            stats.addValue(Vector.get(i));
        }
        return stats.getMean();
    }
}
