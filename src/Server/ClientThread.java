/*
* Class that handles a new thread in ServerSide when a TCP connection is made from ClientSide
 */
package Server;

import static Server.TCPServer.controlSession;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Random;
import java.util.Vector;

public class ClientThread extends Thread {

    private Socket clientSocket = null;
    private WriteXMLFile_ACKTiming writeXMLFile_ACKTiming = null;
    private WriteXMLFile_Deltas writeXMLFile_Deltas = null;
    private WriteXMLFile_bytes1sec writeXMLFile_bytes1sec = null;
    private WriteXMLFile_AvailBWVectors writeXMLFile_AvailBWVectors = null;
    private RTInputStream RTin;
    private RTOutputStream RTout;
    private DataInputStream dataIn;
    private DataOutputStream dataOut;
    public DataMeasurement dataMeasurement = null;
    private ReminderServer reminderServer;
    public String clientNumber = null;
    private boolean isAlgorithmDone;
    

    private String METHOD = null; //PGM-ProbeGapModel; PT-PacketTrain; MV-Moving Average; ACKTIMING-Write time Gap

    private double AvaBW = 0;
    private Vector<Double> AvailableBW_up = null;
    private Vector<Double> AvailableBW_down = null;

    private int byteCnt = 0;
    private int byteSecond = 0;
    private long runningTime = 0;

    public ClientThread(String _METHOD, String _clientNumber, Socket _clientSocket, DataMeasurement _dataMeasurement) {
        try {
            this.METHOD = _METHOD;
            this.clientNumber = _clientNumber;
            this.clientSocket = _clientSocket;
            this.dataMeasurement = _dataMeasurement;
            RTin = new RTInputStream(clientSocket.getInputStream());
            RTout = new RTOutputStream(clientSocket.getOutputStream());
            dataIn = new DataInputStream(RTin);
            dataOut = new DataOutputStream(RTout);
            AvailableBW_up = new Vector<Double>();
            AvailableBW_down = new Vector<Double>();
        } catch (IOException ex) {
            System.err.println("Client Thread Failure:" + ex.getMessage());
        }
    }

    //Runnable Data Receiver
    public void run() {

        try {
            dataOut.writeUTF(METHOD);
            switch (METHOD) {
                case "PGM":
                    Method_PGM();
                    break;
                case "PT":
                    Method_PT();
                    break;
                case "MV_Uplink":
                    Method_MV_Uplink_Server();
                    break;
                case "MV_Downlink":
                    Method_MV_Downlink_Server();
                    break;
                case "MV_Report":
                    Method_MV_Report_Server();
                    break;
                case "MV_readVectorUP":
                    Method_MV_UP_readVector_Server();
                    break;
                case "MV_readVectorDOWN":
                    Method_MV_DOWN_readVector_Server();
                    break;
                case "MV_Report_readVector":
                    Method_MV_Report_readVector_Server();
                    break;
                case "ACKTiming_UP":
                    Method_ACKTimingUP_Server();
                    break;
                case "ACKTiming_DOWN":
                    Method_ACKTimingDOWN_Server();
                    break;
                case "ACKTiming_Report":
                    Method_ACKTiming_Report_Server();
                    break;
                default:
                    System.err.println("INVALID MEHTHOD");
                    break;
            }

        } catch (Exception ex) {
            System.err.println("Receiving Data Failure" + ex.getMessage());
            ex.printStackTrace();
        } finally {
            try {
                if (clientSocket != null) {
                    clientSocket.close();
                    System.err.println("                                                     clientSocket CLOSED!");
                }
                if (isAlgorithmDone) {
                    TCPServer.clientSession.remove(this.clientNumber);
                    TCPServer.controlSession.remove(this.clientNumber);
                }

            } catch (Exception ex) {
                System.err.println("Closing Server Side Socket Failure" + ex.getMessage());
            }
        }
    }

    private void uplink_Server_rcv() {
        try {
            byte[] rcv_buf = new byte[Constants.BLOCKSIZE];
            int num_blocks = 0, n = 0;
            num_blocks = dataIn.readInt();
            System.out.println("\nuplink_Server_rcv with " + "Number Blocks=" + num_blocks);
            for (int i = 0; i < num_blocks; i++) {
                byteCnt = 0;
                //Cycle to read each block
                do {
                    n = RTin.read(rcv_buf, byteCnt, Constants.BLOCKSIZE - byteCnt);

                    if (n > 0) {
                        byteCnt += n;
                    }

                    if (byteCnt < Constants.BLOCKSIZE) {
                        //Keep reading MTU
                    } else {
                        RTin.readTimeVector.add(System.currentTimeMillis());
                        System.out.println("Reach the end of the block " + i + " with " + n + " bytes read & byteCount=" + byteCnt);
                        break;
                    }
                } while ((n > -1) && (byteCnt < Constants.BLOCKSIZE));
                if (n == -1) {
                    System.out.println("Exited with n=-1");
                    break;
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private boolean uplink_Server_rcvInSeconds(long _end) {
        try {
            byte[] rcv_buf = new byte[Constants.BLOCKSIZE];
            int n = 0;
            System.out.println("\nuplink_Server_rcvInSeconds");
            //Initialize Timer
            if (METHOD.equalsIgnoreCase("MV_Uplink")) {
                reminderServer = new ReminderServer(1, this.dataMeasurement);
            }
            long now = System.currentTimeMillis();
            while (System.currentTimeMillis() < _end) {
                byteCnt = 0;
                //Cycle to read each block
                do {
                    n = RTin.read(rcv_buf, byteCnt, Constants.BLOCKSIZE - byteCnt);

                    if (n > 0) {
                        byteCnt += n;
                        byteSecond += n;

                        if ((System.currentTimeMillis() >= (now + 1000)) && (METHOD.equalsIgnoreCase("MV_readVectorUP"))) {
                            now = System.currentTimeMillis();
                            dataMeasurement.add_SampleSecond_up(byteSecond, System.currentTimeMillis());
                            byteSecond = 0;
                        }

                    } else {
                        System.err.println("Read n<0");
                        break;
                    }

                    if (byteCnt < Constants.BLOCKSIZE) {
                        System.err.println("Read " + n + " bytes");
                        //Keep reading MTU
                    } else {
                        //MTU is finished
                        if (METHOD.equalsIgnoreCase("MV_Uplink")) {
                            dataMeasurement.add_SampleSecond_up(byteCnt, System.currentTimeMillis());
                        }
                        break;
                    }
                } while ((n > 0) && (byteCnt < Constants.BLOCKSIZE));
                if (n < 0) {
                    System.out.println("Exited with n=-1");
                    break;
                }
            }
            return true;
        } catch (IOException ex) {
            return false;
        } finally {
            if (METHOD.equalsIgnoreCase("MV_Uplink")) {
                reminderServer.timer.cancel();
            }
        }

    }

    private boolean downlink_Server_sndInSeconds() {
        boolean keepRunning = true;
        try {
            byte[] snd_buf = new byte[Constants.BLOCKSIZE];
            while (keepRunning) {
                RTout.write(snd_buf);
                RTout.writeTimeVector.add(System.currentTimeMillis());
            }
            return true;
        } catch (IOException ex) {
            return false;
        } finally {
            keepRunning = false;
        }
    }

    private void downlink_Server_snd() {
        try {
            int num_blocks = Constants.NUMBER_BLOCKS;
            byte[] snd_buf = new byte[Constants.BLOCKSIZE];

            dataOut.writeInt(num_blocks);
            dataOut.flush();
            System.out.println("\n downlink_Server_snd with " + "Number Blocks=" + num_blocks);
            for (int i = 0; i < num_blocks; i++) {
                RTout.write(snd_buf);
                RTout.writeTimeVector.add(System.currentTimeMillis());
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void ProbeGapModel(String direction, Vector<Long> deltaINvector, Vector<Long> deltaOUTvector) {
        AvailableBW_up.clear();
        int length = deltaINvector.size();
        int readVectorLength = RTin.readTimeVector.size() - 1;
        double Capacity = RTInputStream.bytesTotal / (RTin.readTimeVector.get(readVectorLength) - RTin.readTimeVector.get(0));
        //Calculate AvailableBW
        for (int i = 0; i < length; i++) {
            double deltaIN = deltaINvector.get(i);
            double deltaOUT = deltaOUTvector.get(i);
            double deltaResult = (deltaOUT - deltaIN) / deltaIN;
            AvailableBW_up.add((1 - deltaResult) * Capacity);
        }
        //Export to XML
        writeXMLFile_Deltas = new WriteXMLFile_Deltas("PGM-" + direction + "-", deltaINvector, deltaOUTvector);
        System.out.println("Probe Gap Model Done!");
    }

    private double PacketTrain() {
        AvaBW = 0;
        int length = RTin.readTimeVector.size() - 1;
        double deltaN = RTin.readTimeVector.get(length) - RTin.readTimeVector.get(0);
        int N = Constants.NUMBER_BLOCKS;
        int L = Constants.BLOCKSIZE;
        AvaBW = (((N - 1) * L) / deltaN);
        System.err.println("AvaBW: " + AvaBW);
        System.out.println("PTprocess is DONE!");
        return AvaBW;
    }

    private void Method_PGM() {
        isAlgorithmDone = false;
        //Parameters
        Constants.SOCKET_RCVBUF = 2920;
        Constants.SOCKET_RCVBUF = 2920;
        Constants.NUMBER_BLOCKS = 100;
        //Measurements
        try {
            //Uplink
            dataOut.writeByte(1);
            uplink_Server_rcv();
            //Downlink
            dataOut.writeByte(2);
            downlink_Server_snd();

        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            //Calculate Deltas - Skip the first two packets because TCP Buffer is empty
            for (int i = 2; i < RTin.readTimeVector.size(); i++) {
                if (i % 2 == 1) {
                    dataMeasurement.deltaINVector_downlink.add(RTout.writeTimeVector.get(i) - RTout.writeTimeVector.get(i - 1));
                    dataMeasurement.deltaOUTVector_uplink.add(RTin.readTimeVector.get(i) - RTin.readTimeVector.get(i - 1));
                }
            }
        }
        //Receive Report Measurements
        try {
            dataIn.readByte();
            int length = dataIn.readInt();
            dataMeasurement.deltaINVector_uplink.clear();
            dataMeasurement.deltaOUTVector_downlink.clear();
            //Read Delta Vectors
            for (int j = 0; j < length; j++) {
                dataMeasurement.deltaINVector_uplink.add(dataIn.readLong());
                dataMeasurement.deltaOUTVector_downlink.add(dataIn.readLong());
            }
            ProbeGapModel("Uplink-PGM", dataMeasurement.deltaINVector_uplink, dataMeasurement.deltaOUTVector_uplink);
            ProbeGapModel("Downlink-PGM", dataMeasurement.deltaINVector_downlink, dataMeasurement.deltaOUTVector_downlink);
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            RTin.readTimeVector.clear();
            RTout.writeTimeVector.clear();
            dataMeasurement.deltaINVector_uplink.clear();
            dataMeasurement.deltaOUTVector_uplink.clear();
            dataMeasurement.deltaINVector_downlink.clear();
            dataMeasurement.deltaOUTVector_downlink.clear();
            isAlgorithmDone = true;
            System.err.println("Method_PGM_Client along with Received Report is done!");
        }
    }

    private void Method_PT() {
        isAlgorithmDone = false;
        //Parameters
        Constants.NUMBER_BLOCKS = 10;
        Constants.SOCKET_RCVBUF = 14600;
        Constants.SOCKET_RCVBUF = 14600;

        //Measurements
        try {
            //Uplink
            AvailableBW_up.clear();
            dataOut.writeByte(1);
            for (int p = 0; p < 10; p++) {
                System.err.println("UPLINK PACKET TRAIN ROUND: " + p);
                uplink_Server_rcv();
                AvailableBW_up.add(PacketTrain());
            }

            //Downlink
            dataOut.writeByte(2);
            for (int p = 0; p < 10; p++) {
                System.err.println("DOWNLINK PACKET TRAIN ROUND: " + p);
                downlink_Server_snd();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        //Receive Report Measurements - AvailableBW_down Vector
        try {
            AvailableBW_down.clear();
            dataIn.readByte();
            int length = dataIn.readInt();
            for (int k = 0; k < length; k++) {
                AvailableBW_down.add(dataIn.readDouble());
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            writeXMLFile_AvailBWVectors = new WriteXMLFile_AvailBWVectors("AvalBW_downlink", AvailableBW_down);
            writeXMLFile_AvailBWVectors = new WriteXMLFile_AvailBWVectors("AvalBW_downlink", AvailableBW_up);
            AvailableBW_down.clear();
            AvailableBW_up.clear();
            RTin.readTimeVector.clear();
            RTout.writeTimeVector.clear();
            isAlgorithmDone = true;
            System.err.println("Method_PT_Client along with Report is done!");
        }
    }

    private void Method_MV_Uplink_Server() {
        isAlgorithmDone = false;

        //Parameters
        Constants.SOCKET_RCVBUF = 14600;
        Constants.SOCKET_RCVBUF = 14600;

        //Measurements
        try {
            //Uplink
            dataOut.writeByte(1);
            long end = System.currentTimeMillis() + runningTime;
            uplink_Server_rcvInSeconds(end);
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            try {
                writeXMLFile_bytes1sec = new WriteXMLFile_bytes1sec("Uplink-MV-1secBytes", dataMeasurement.SampleSecond_up);
                dataMeasurement.SampleSecond_up.clear();
                Thread c = new ClientThread("MV_Downlink", this.clientNumber, TCPServer.clientSession.get(clientNumber), this.dataMeasurement);
                c.start();
            } catch (Exception ex) {
                ex.printStackTrace();
            }

        }
    }

    private void Method_MV_Downlink_Server() {
        //Parameters
        Constants.SOCKET_RCVBUF = 14600;
        Constants.SOCKET_RCVBUF = 14600;
        //Measurements
        try {
            //Downlink
            dataOut.writeByte(1);
            downlink_Server_sndInSeconds();
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            try {
                Thread c = new ClientThread("MV_Report", this.clientNumber, TCPServer.controlSession.get(clientNumber), this.dataMeasurement);
                c.start();
            } catch (Exception ex) {
                ex.printStackTrace();
            }

        }
    }

    private void Method_MV_Report_Server() {
        //Receive Report - Sample Second Vector 
        dataMeasurement.SampleSecond_down.clear();
        try {
            dataIn.readByte();
            int length = dataIn.readInt();
            for (int l = 0; l < length; l++) {
                int bytecnt = dataIn.readInt();
                long sampleTime = dataIn.readLong();
                dataMeasurement.SampleSecond_down.add(new DataSecond(bytecnt, sampleTime));
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            writeXMLFile_bytes1sec = new WriteXMLFile_bytes1sec("Downlink-MV-1secBytes", dataMeasurement.SampleSecond_down);
            RTin.readTimeVector.clear();
            RTout.writeTimeVector.clear();
            dataMeasurement.SampleSecond_down.clear();
            isAlgorithmDone = true;
            System.err.println("Method_MV_Server along with Report is done!");
        }
    }

    private void Method_MV_UP_readVector_Server() {
        isAlgorithmDone = false;
        //Parameters
        Constants.SOCKET_RCVBUF = 14600;
        Constants.SOCKET_RCVBUF = 14600;

        //Measurements
        try {
            //Uplink
            dataOut.writeByte(1);
            long end = System.currentTimeMillis() + runningTime;
            uplink_Server_rcvInSeconds(end);
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            try {
                writeXMLFile_bytes1sec = new WriteXMLFile_bytes1sec("Uplink-MV_readVector-1secBytes", dataMeasurement.SampleSecond_up);
                dataMeasurement.SampleSecond_up.clear();
                Thread c = new ClientThread("MV_readVectorDOWN", this.clientNumber, TCPServer.clientSession.get(clientNumber), this.dataMeasurement);
                c.start();
            } catch (Exception ex) {
                ex.printStackTrace();
            }

        }
    }

    private void Method_MV_DOWN_readVector_Server() {
        //Parameters
        Constants.SOCKET_RCVBUF = 14600;
        Constants.SOCKET_RCVBUF = 14600;

        //Measurements
        try {
            //Uplink
            dataOut.writeByte(1);
            downlink_Server_sndInSeconds();
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            try {
                Thread c = new ClientThread("MV_Report_readVector", this.clientNumber, TCPServer.controlSession.get(clientNumber), this.dataMeasurement);
                c.start();
            } catch (Exception ex) {
                ex.printStackTrace();
            }

        }
    }

    private void Method_MV_Report_readVector_Server() {
        //Receive Report - Sample Second Vector 
        dataMeasurement.SampleSecond_down.clear();
        try {
            dataIn.readByte();
            int length = dataIn.readInt();
            for (int l = 0; l < length; l++) {
                int bytecnt = dataIn.readInt();
                long sampleTime = dataIn.readLong();
                dataMeasurement.SampleSecond_down.add(new DataSecond(bytecnt, sampleTime));
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            writeXMLFile_bytes1sec = new WriteXMLFile_bytes1sec("Downlink-MV_readVector-1secBytes", dataMeasurement.SampleSecond_down);
            RTin.readTimeVector.clear();
            RTout.writeTimeVector.clear();
            dataMeasurement.SampleSecond_down.clear();
            isAlgorithmDone = true;
            System.err.println("Method_MV_readVector_Server along with Report is done!");
        }
    }

    private void Method_ACKTimingUP_Server() {
        isAlgorithmDone = false;
        //Parameters
        Constants.SOCKET_RCVBUF = 14600;
        Constants.SOCKET_RCVBUF = 14600;

        //Measurements
        try {
            //Uplink
            dataOut.writeByte(1);
            long end = System.currentTimeMillis() + runningTime;
            uplink_Server_rcvInSeconds(end);
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            try {
                Thread c = new ClientThread("ACKTiming_DOWN", this.clientNumber, TCPServer.clientSession.get(clientNumber), this.dataMeasurement);
                c.start();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private void Method_ACKTimingDOWN_Server() {
        //Parameters
        Constants.SOCKET_RCVBUF = 14600;
        Constants.SOCKET_RCVBUF = 14600;

        //Measurements
        try {
            //Uplink
            dataOut.writeByte(1);
            downlink_Server_sndInSeconds();
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            try {
                //Discover ACKTimings
                for (int r = 1; r < RTout.writeTimeVector.size() - 1; r++) {
                    long write = RTout.writeTimeVector.get(r);
                    long write_after = RTout.writeTimeVector.get(r - 1);
                    if ((write - write_after) > 10) {
                        for (int l = r; l < RTout.writeTimeVector.size() - 1; l++) {
                            dataMeasurement.ACKTimingVector.add(RTout.writeTimeVector.get(l));
                        }
                        break;
                    }
                }
                writeXMLFile_ACKTiming = new WriteXMLFile_ACKTiming("Downlink-ACKTiming", dataMeasurement.ACKTimingVector);
                RTout.writeTimeVector.clear();
                Thread c = new ClientThread("ACKTiming_Report", this.clientNumber, TCPServer.controlSession.get(clientNumber), this.dataMeasurement);
                c.start();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private void Method_ACKTiming_Report_Server() {
        //Report ACKTiming Vector, sending size first 
        RTout.writeTimeVector.clear();
        try {
            dataIn.readByte();
            int length = dataIn.readInt();
            for (int k = 0; k < length; k++) {
                RTout.writeTimeVector.add(dataIn.readLong());
            }
            //Discover ACKTimings
            for (int r = 1; r < RTout.writeTimeVector.size() - 1; r++) {
                long write = RTout.writeTimeVector.get(r);
                long write_after = RTout.writeTimeVector.get(r - 1);
                if ((write - write_after) > 10) {
                    for (int l = r; l < RTout.writeTimeVector.size() - 1; l++) {
                        dataMeasurement.ACKTimingVector.add(RTout.writeTimeVector.get(l));
                    }
                    break;
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            writeXMLFile_ACKTiming = new WriteXMLFile_ACKTiming("Uplink-ACKTiming", dataMeasurement.ACKTimingVector);
            RTin.readTimeVector.clear();
            RTout.writeTimeVector.clear();
            dataMeasurement.aux_writeTimeVector.clear();
            isAlgorithmDone = true;
            System.err.println("Method_ACKTiming_Server along with Report is done!");

        }
    }

}
