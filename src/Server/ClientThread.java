/*
* Class that handles a new thread in ServerSide when a TCP connection is made from ClientSide
 */
package Server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Vector;
import org.apache.commons.math3.distribution.TDistribution;
import org.apache.commons.math3.exception.MathIllegalArgumentException;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

public class ClientThread extends Thread {

    private Socket clientSocket = null;
    private WriteXMLFile_ACKTiming writeXMLFile_ACKTiming = null;
    private WriteXMLFile_Deltas writeXMLFile_Deltas = null;
    private WriteXMLFile_bytes1sec writeXMLFile_bytes1sec = null;
    private WriteXMLFile_AvailBWVectors writeXMLFile_AvailBWVectors = null;
    private RTInputStream RTin = null;
    private RTOutputStream RTout = null;
    private DataInputStream dataIn = null;
    private DataOutputStream dataOut = null;
    private DataMeasurement dataMeasurement = null;
    private ReminderServer reminderServer = null;
    private boolean isAlgorithmDone = false;
    private boolean isThreadMethod;
    private boolean isIperfSettings;
    private boolean isNagleDisable;
    private String METHOD = null; //PGM-ProbeGapModel; PT-PacketTrain; MV-Moving Average; ACKTIMING-Write time Gap

    private double AvaBW = 0;
    private Vector<Double> AvailableBW_up = null;
    private Vector<Double> AvailableBW_down = null;
    private Vector<Integer> ByteSecondVector = null;

    private int ID = 0;
    private int byteCnt = 0;
    private long runningTime = 30000;
    long firstPacket = 0;
    long lastPacket = 0;
    
    private double lower_bound = 0;
    private double Mean = 0;
    private double upper_bound = 0;

    public ClientThread(int _ID, String _METHOD, Socket _clientSocket, DataMeasurement _dataMeasurement, boolean _isIperfSettings, boolean _isNagleDisable) {
        try {
            this.ID = _ID;
            this.METHOD = _METHOD;
            this.clientSocket = _clientSocket;
            this.dataMeasurement = _dataMeasurement;
            this.isIperfSettings = _isIperfSettings;
            this.isNagleDisable = _isNagleDisable;
            RTin = new RTInputStream(clientSocket.getInputStream());
            RTout = new RTOutputStream(clientSocket.getOutputStream());
            dataIn = new DataInputStream(RTin);
            dataOut = new DataOutputStream(RTout);
            AvailableBW_up = new Vector<Double>();
            AvailableBW_down = new Vector<Double>();
            ByteSecondVector = new Vector<Integer>();
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
            System.err.println("Receiving Data Failure: " + ex.getMessage());
            ex.printStackTrace();
        } finally {
            try {
                if (clientSocket != null) {
                    clientSocket.close();
                    System.err.println("                                                     clientSocket CLOSED!");
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

    private void uplink_Server_rcv() {
        try {
            byte[] rcv_buf = new byte[Constants.BLOCKSIZE];
            int num_blocks = 0, n = 0;
            num_blocks = dataIn.readInt();
            boolean isFirstPacket = true;
            System.out.println("\nuplink_Server_rcv with " + "Number Blocks=" + num_blocks);
            for (int i = 0; i < num_blocks; i++) {
                byteCnt = 0;
                //Cycle to read each block
                do {
                    n = RTin.read(rcv_buf, byteCnt, Constants.BLOCKSIZE - byteCnt);

                    if (n > 0) {
                        byteCnt += n;
                        if (byteCnt >= 1460 && isFirstPacket) {
                            firstPacket = System.currentTimeMillis();
                            isFirstPacket = false;
                            System.out.println("First Packet: " + firstPacket);
                        }
                    }

                    if (byteCnt < Constants.BLOCKSIZE) {
                        //Keep reading MTU
                    } else {
                        RTin.readTimeVector.add(System.currentTimeMillis());
                        System.out.println("Reach the end of the block " + i + " with " + n + " bytes read & byteCount=" + byteCnt);
                        break;
                    }
                } while ((n > -1) && (byteCnt < Constants.BLOCKSIZE));
                lastPacket = System.currentTimeMillis();
                System.out.println("Last Packet: " + lastPacket);
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
            if (isThreadMethod) {
                reminderServer = new ReminderServer(1, this.dataMeasurement, this.RTin);
            }
            while (System.currentTimeMillis() < _end) {
                byteCnt = 0;
                //Cycle to read each block
                do {
                    n = RTin.read(rcv_buf, byteCnt, Constants.BLOCKSIZE - byteCnt);

                    if (n > 0) {
                        byteCnt += n;
                        if (!isThreadMethod) {
                            dataMeasurement.add_SampleReadTime(byteCnt, System.currentTimeMillis());
                        }

                    } else {
                        System.out.println("Read n<0");
                        break;
                    }

                    if (byteCnt < Constants.BLOCKSIZE) {
                        //System.out.println("Read " + n + " bytes");
                        //Keep reading MTU
                    } else {
                        //MTU is finished
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
            if (isThreadMethod) {
                reminderServer.cancelTimer();
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

    private void ProbeGapModel(String name, Vector<Long> deltaINvector, Vector<Long> deltaOUTvector) {
        AvailableBW_up.clear();
        int length = deltaINvector.size();
        int readVectorLength = RTin.readTimeVector.size() - 1;
        double Capacity = RTin.getBytes() / (RTin.readTimeVector.get(readVectorLength) - RTin.readTimeVector.get(0));
        //Calculate AvailableBW
        for (int i = 0; i < length; i++) {
            double deltaIN = deltaINvector.get(i);
            double deltaOUT = deltaOUTvector.get(i);
            double deltaResult = (deltaOUT - deltaIN) / deltaIN;
            AvailableBW_up.add((1 - deltaResult) * Capacity);
        }
        //Export to XML
        writeXMLFile_Deltas = new WriteXMLFile_Deltas(ID + " " + name, deltaINvector, deltaOUTvector);
        writeXMLFile_AvailBWVectors = new WriteXMLFile_AvailBWVectors(ID + " " + name, AvailableBW_up);

        System.out.println("Probe Gap Model Done!");
    }

    private double PacketTrain() {
        AvaBW = 0;
//        int length = RTin.readTimeVector.size() - 1;
//        double deltaN = RTin.readTimeVector.get(length) - RTin.readTimeVector.get(0);
//        int N = Constants.NUMBER_BLOCKS;
//        int L = Constants.BLOCKSIZE;
        double deltaN = lastPacket - firstPacket;
        int N = 10;
        int L = Constants.BLOCKSIZE;
        AvaBW = (((N - 1) * L) / deltaN);
        System.err.println("AvaBW: " + AvaBW);
        System.out.println("PTprocess is DONE!");
        return AvaBW;
    }

    private void Method_PGM() {
        //Parameters
        Constants.SOCKET_RCVBUF = 2920;
        Constants.SOCKET_RCVBUF = 2920;
        Constants.BLOCKSIZE = 1460;
        Constants.NUMBER_BLOCKS = 1000;
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
            if (isNagleDisable) {
                ProbeGapModel("Uplink-PGM_NagleOFF", dataMeasurement.deltaINVector_uplink, dataMeasurement.deltaOUTVector_uplink);
                ProbeGapModel("Downlink-PGM_NagleOFF", dataMeasurement.deltaINVector_downlink, dataMeasurement.deltaOUTVector_downlink);
            } else {
                ProbeGapModel("Uplink-PGM_NagleON", dataMeasurement.deltaINVector_uplink, dataMeasurement.deltaOUTVector_uplink);
                ProbeGapModel("Downlink-PGM_NagleON", dataMeasurement.deltaINVector_downlink, dataMeasurement.deltaOUTVector_downlink);
            }

        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            dataMeasurement.deltaINVector_uplink.clear();
            dataMeasurement.deltaOUTVector_uplink.clear();
            dataMeasurement.deltaINVector_downlink.clear();
            dataMeasurement.deltaOUTVector_downlink.clear();
            isAlgorithmDone = true;
            System.err.println("Method_PGM_Client along with Received Report is done!");
        }
    }

    private void Method_PT() {
        //Parameters
        Constants.NUMBER_BLOCKS = 1;
        Constants.SOCKET_RCVBUF = 14600;
        Constants.SOCKET_RCVBUF = 14600;
        Constants.BLOCKSIZE = 14600;

        //Measurements
        try {
            //Uplink
            AvailableBW_up.clear();
            dataOut.writeByte(1);
            for (int p = 0; p < 1; p++) {
                System.err.println("UPLINK PACKET TRAIN ROUND: " + p);
                uplink_Server_rcv();
                AvailableBW_up.add(PacketTrain());
            }

//            //Downlink
//            dataOut.writeByte(2);
//            for (int p = 0; p < 10; p++) {
//                System.err.println("DOWNLINK PACKET TRAIN ROUND: " + p);
//                downlink_Server_snd();
//            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        //Receive Report Measurements - AvailableBW_down Vector
        AvailableBW_down.clear();
        try {
            dataIn.readByte();
            int length = dataIn.readInt();
            for (int k = 0; k < length; k++) {
                AvailableBW_down.add(dataIn.readDouble());
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            if (isNagleDisable) {
                writeXMLFile_AvailBWVectors = new WriteXMLFile_AvailBWVectors(ID + " AvalBW_downlink_NagleOFF", AvailableBW_down);
                writeXMLFile_AvailBWVectors = new WriteXMLFile_AvailBWVectors(ID + " AvalBW_uplink_NagleOFF", AvailableBW_up);
            } else {
                writeXMLFile_AvailBWVectors = new WriteXMLFile_AvailBWVectors(ID + " AvalBW_downlink_NagleON", AvailableBW_down);
                writeXMLFile_AvailBWVectors = new WriteXMLFile_AvailBWVectors(ID + " AvalBW_uplink_NagleON", AvailableBW_up);
            }
            isAlgorithmDone = true;
            System.err.println("Method_PT_Client along with Report is done!");
        }
    }

    private void Method_MV_Uplink_Server() {

        //Parameters
        if (isIperfSettings) {
            Constants.SOCKET_RCVBUF = 64000;
            Constants.SOCKET_RCVBUF = 64000;
            Constants.BLOCKSIZE = 8000;
        } else {
            Constants.SOCKET_RCVBUF = 14600;
            Constants.SOCKET_RCVBUF = 14600;
            Constants.BLOCKSIZE = 1460;
        }

        System.out.println("MV_Uplink_1secThread with TCP_SND/RCV_Windows=" + Constants.SOCKET_RCVBUF + " & PacketSize=" + Constants.BLOCKSIZE);
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
                int total = 0;
                for (int i = 0; i < dataMeasurement.SampleSecond_up.size(); i++) {
                    total += dataMeasurement.SampleSecond_up.get(i);
                }
                System.err.println("Average: " + (total / dataMeasurement.SampleSecond_up.size()) + " Transfered: " + total);
                Tstudent(dataMeasurement.SampleSecond_up);
                if (isIperfSettings && isNagleDisable) {
                    writeXMLFile_bytes1sec = new WriteXMLFile_bytes1sec(ID + " Uplink-MV-1secBytes_NagleOFF", dataMeasurement.SampleSecond_up, total, Mean, lower_bound, upper_bound, "MV_1secThread/iperf_Settings/");
                } else if (isIperfSettings && !isNagleDisable) {
                    writeXMLFile_bytes1sec = new WriteXMLFile_bytes1sec(ID + " Uplink-MV-1secBytes_NagleON", dataMeasurement.SampleSecond_up, total, Mean, lower_bound, upper_bound, "MV_1secThread/iperf_Settings/");
                } else if (!isIperfSettings && !isNagleDisable) {
                    writeXMLFile_bytes1sec = new WriteXMLFile_bytes1sec(ID + " Uplink-MV-1secBytes_NagleON", dataMeasurement.SampleSecond_up, total, Mean, lower_bound, upper_bound, "MV_1secThread/thesis_Settings/");
                } else if (!isIperfSettings && isNagleDisable) {
                    writeXMLFile_bytes1sec = new WriteXMLFile_bytes1sec(ID + " Uplink-MV-1secBytes_NagleOFF", dataMeasurement.SampleSecond_up, total, Mean, lower_bound, upper_bound, "MV_1secThread/thesis_Settings/");
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }

        }
    }

    private void Method_MV_Downlink_Server() {
        //Parameters
        if (isIperfSettings) {
            Constants.SOCKET_RCVBUF = 64000;
            Constants.SOCKET_RCVBUF = 64000;
            Constants.BLOCKSIZE = 8000;
        } else {
            Constants.SOCKET_RCVBUF = 14600;
            Constants.SOCKET_RCVBUF = 14600;
            Constants.BLOCKSIZE = 1460;
        }
        System.out.println("MV_Downlink_1secThread with TCP_SND/RCV_Windows=" + Constants.SOCKET_RCVBUF + " & PacketSize=" + Constants.BLOCKSIZE);
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
        int total = 0;
        try {
            dataIn.readByte();
            int length = dataIn.readInt();
            for (int l = 0; l < length; l++) {
                int bytecnt = dataIn.readInt();
                dataMeasurement.SampleSecond_down.add(bytecnt);
                total += bytecnt;
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            Tstudent(dataMeasurement.SampleSecond_down);

            //Export to XML File
            if (isIperfSettings && isNagleDisable) {
                writeXMLFile_bytes1sec = new WriteXMLFile_bytes1sec(ID + " Downlink-MV-1secBytes_NagleOFF", dataMeasurement.SampleSecond_down, total, Mean, lower_bound, upper_bound, "MV_1secThread/iperf_Settings/");
            } else if (isIperfSettings && !isNagleDisable) {
                writeXMLFile_bytes1sec = new WriteXMLFile_bytes1sec(ID + " Downlink-MV-1secBytes_NagleON", dataMeasurement.SampleSecond_down, total, Mean, lower_bound, upper_bound, "MV_1secThread/iperf_Settings/");
            } else if (!isIperfSettings && !isNagleDisable) {
                writeXMLFile_bytes1sec = new WriteXMLFile_bytes1sec(ID + " Downlink-MV-1secBytes_NagleON", dataMeasurement.SampleSecond_down, total, Mean, lower_bound, upper_bound, "MV_1secThread/thesis_Settings/");
            } else if (!isIperfSettings && isNagleDisable) {
                writeXMLFile_bytes1sec = new WriteXMLFile_bytes1sec(ID + " Downlink-MV-1secBytes_NagleOFF", dataMeasurement.SampleSecond_down, total, Mean, lower_bound, upper_bound, "MV_1secThread/thesis_Settings/");
            }

            isAlgorithmDone = true;
            System.err.println("Method_MV_Server along with Report is done!");
        }
    }

    private void Method_MV_UP_readVector_Server() {
        //Parameters
        if (isIperfSettings) {
            Constants.SOCKET_RCVBUF = 64000;
            Constants.SOCKET_RCVBUF = 64000;
            Constants.BLOCKSIZE = 8000;
        } else {
            Constants.SOCKET_RCVBUF = 14600;
            Constants.SOCKET_RCVBUF = 14600;
            Constants.BLOCKSIZE = 1460;
        }
        System.out.println("MV_Uplink_readVector with TCP_SND/RCV_Windows=" + Constants.SOCKET_RCVBUF + " & PacketSize=" + Constants.BLOCKSIZE);

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
                MovingAverageCalculation(dataMeasurement.SampleReadTime);
                Tstudent(ByteSecondVector);
                int total = 0;
                for (int i = 0; i < ByteSecondVector.size(); i++) {
                    total += ByteSecondVector.get(i);
                }
                //Export to XML
                if (isIperfSettings && isNagleDisable) {
                    writeXMLFile_bytes1sec = new WriteXMLFile_bytes1sec(ID + " Uplink-MV_readVector_NagleOFF", ByteSecondVector, total, Mean, lower_bound, upper_bound, "MV_readVector/iperf_Settings/");
                } else if (isIperfSettings && !isNagleDisable) {
                    writeXMLFile_bytes1sec = new WriteXMLFile_bytes1sec(ID + " Uplink-MV_readVector_NagleON", ByteSecondVector, total, Mean, lower_bound, upper_bound, "MV_readVector/iperf_Settings/");
                } else if (!isIperfSettings && !isNagleDisable) {
                    writeXMLFile_bytes1sec = new WriteXMLFile_bytes1sec(ID + " Uplink-MV_readVector_NagleON", ByteSecondVector, total, Mean, lower_bound, upper_bound, "MV_readVector/thesis_Settings/");
                } else if (!isIperfSettings && isNagleDisable) {
                    writeXMLFile_bytes1sec = new WriteXMLFile_bytes1sec(ID + " Uplink-MV_readVector_NagleOFF", ByteSecondVector, total, Mean, lower_bound, upper_bound, "MV_readVector/thesis_Settings/");
                }

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private void Method_MV_DOWN_readVector_Server() {
        //Parameters
        if (isIperfSettings) {
            Constants.SOCKET_RCVBUF = 64000;
            Constants.SOCKET_RCVBUF = 64000;
            Constants.BLOCKSIZE = 8000;
        } else {
            Constants.SOCKET_RCVBUF = 14600;
            Constants.SOCKET_RCVBUF = 14600;
            Constants.BLOCKSIZE = 1460;
        }

        System.out.println("MV_Downlink_readVector with TCP_SND/RCV_Windows=" + Constants.SOCKET_RCVBUF + " & PacketSize=" + Constants.BLOCKSIZE);
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
        ByteSecondVector.clear();
        try {
            dataIn.readByte();
            int length = dataIn.readInt();
            for (int l = 0; l < length; l++) {
                int bytecnt = dataIn.readInt();
                long sampleTime = dataIn.readLong();
                dataMeasurement.SampleReadTime.add(new DataSecond(bytecnt, sampleTime));
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            MovingAverageCalculation(dataMeasurement.SampleReadTime);
            Tstudent(ByteSecondVector);
            int total = 0;
            for (int i = 0; i < ByteSecondVector.size(); i++) {
                total += ByteSecondVector.get(i);
            }
            //Export to XML
            if (isIperfSettings && isNagleDisable) {
                writeXMLFile_bytes1sec = new WriteXMLFile_bytes1sec(ID + " Downlink-MV_readVector_NagleOFF", ByteSecondVector, total, Mean, lower_bound, upper_bound, "MV_readVector/iperf_Settings/");
            } else if (isIperfSettings && !isNagleDisable) {
                writeXMLFile_bytes1sec = new WriteXMLFile_bytes1sec(ID + " Downlink-MV_readVector_NagleON", ByteSecondVector, total, Mean, lower_bound, upper_bound, "MV_readVector/iperf_Settings/");
            } else if (!isIperfSettings && !isNagleDisable) {
                writeXMLFile_bytes1sec = new WriteXMLFile_bytes1sec(ID + " Downlink-MV_readVector_NagleON", ByteSecondVector, total, Mean, lower_bound, upper_bound, "MV_readVector/thesis_Settings/");
            } else if (!isIperfSettings && isNagleDisable) {
                writeXMLFile_bytes1sec = new WriteXMLFile_bytes1sec(ID + " Downlink-MV_readVector_NagleOFF", ByteSecondVector, total, Mean, lower_bound, upper_bound, "MV_readVector/thesis_Settings/");
            }

            isAlgorithmDone = true;
            System.err.println("Method_MV_readVector_Server along with Report is done!");
        }
    }

    private void Method_ACKTimingUP_Server() {
        //Parameters
        if (isIperfSettings) {
            Constants.SOCKET_RCVBUF = 64000;
            Constants.SOCKET_RCVBUF = 64000;
            Constants.BLOCKSIZE = 8000;
        } else {
            Constants.SOCKET_RCVBUF = 14600;
            Constants.SOCKET_RCVBUF = 14600;
            Constants.BLOCKSIZE = 1460;
        }
        System.out.println("ACKTiming_Uplink with TCP_SND/RCV_Windows=" + Constants.SOCKET_RCVBUF + " & PacketSize=" + Constants.BLOCKSIZE);
        //Measurements
        dataMeasurement.SampleSecond_up.clear();
        try {
            //Uplink
            dataOut.writeByte(1);
            long end = System.currentTimeMillis() + runningTime;
            uplink_Server_rcvInSeconds(end);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void Method_ACKTimingDOWN_Server() {
        //Parameters
        if (isIperfSettings) {
            Constants.SOCKET_RCVBUF = 64000;
            Constants.SOCKET_RCVBUF = 64000;
            Constants.BLOCKSIZE = 8000;
        } else {
            Constants.SOCKET_RCVBUF = 14600;
            Constants.SOCKET_RCVBUF = 14600;
            Constants.BLOCKSIZE = 1460;
        }
        System.out.println("MV_Downlink_1secThread with TCP_SND/RCV_Windows=" + Constants.SOCKET_RCVBUF + " & PacketSize=" + Constants.BLOCKSIZE);
        //Measurements
        try {
            //Uplink
            dataOut.writeByte(2);
            downlink_Server_sndInSeconds();
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            try {
                //Discover ACKTimings
                long threshold = 10;
                dataMeasurement.ACKTimingVector.clear();
                for (int r = 1; r < RTout.writeTimeVector.size() - 1; r++) {
                    long write = RTout.writeTimeVector.get(r);
                    long write_after = RTout.writeTimeVector.get(r - 1);
                    if ((write - write_after) > threshold) {
                        for (int l = r; l < RTout.writeTimeVector.size() - 1; l++) {
                            dataMeasurement.ACKTimingVector.add(RTout.writeTimeVector.get(l));
                        }
                        break;
                    }
                }
                writeXMLFile_ACKTiming = new WriteXMLFile_ACKTiming(ID + " Downlink-ACKTiming", dataMeasurement.ACKTimingVector);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private void Method_ACKTiming_Report_Server() {
        //Receive Report ACKTiming Vector, sending size first 
        RTout.writeTimeVector.clear();
        try {
            dataIn.readByte();
            int length = dataIn.readInt();
            for (int k = 0; k < length; k++) {
                RTout.writeTimeVector.add(dataIn.readLong());
            }
            //Discover ACKTimings
            int threshold = 10;
            dataMeasurement.ACKTimingVector.clear();
            for (int r = 1; r < RTout.writeTimeVector.size() - 1; r++) {
                long write = RTout.writeTimeVector.get(r);
                long write_after = RTout.writeTimeVector.get(r - 1);
                if ((write - write_after) > threshold) {
                    for (int l = r; l < RTout.writeTimeVector.size() - 1; l++) {
                        dataMeasurement.ACKTimingVector.add(RTout.writeTimeVector.get(l));
                    }
                    break;
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            writeXMLFile_ACKTiming = new WriteXMLFile_ACKTiming(ID + " Uplink-ACKTiming", dataMeasurement.ACKTimingVector);
            isAlgorithmDone = true;
            System.err.println("Method_ACKTiming_Server along with Report is done!");

        }
    }

    private void MovingAverageCalculation(Vector<DataSecond> Vector_Read_or_Write) {
        int i, j = 0, bytesTotal = 0;
        double average = 0;
        ByteSecondVector.clear();
        System.out.println("Size: " + Vector_Read_or_Write.size());
        for (i = 0; i < Vector_Read_or_Write.size(); i++) {
            bytesTotal = 0;

            while ((Vector_Read_or_Write.get(j).sampleTime - Vector_Read_or_Write.get(i).sampleTime) < 1000) {
                if (j == Vector_Read_or_Write.size() - 1) {
                    break;
                }
                bytesTotal += Vector_Read_or_Write.get(j).bytesRead;
                j++;
            }
            ByteSecondVector.add(bytesTotal);
            //average = bytesTotal / (Vector_Read_or_Write.get(j).sampleTime - Vector_Read_or_Write.get(i).sampleTime);
            System.out.println("Second Interval = [" + i + "," + j + "]" + " with bytesTotal=" + bytesTotal); // + "and Capacity=" + average);
            i = j;
        }
    }

    private void Tstudent(Vector<Integer> Vector) {
        SummaryStatistics stats = new SummaryStatistics();
        for (int i = 0; i < Vector.size() - 1; i++) {
            stats.addValue(Vector.get(i));
        }

        // Calculate 90% confidence interval
        double ci = calcMeanCI(stats, 0.90);
        Mean = stats.getMean();
        System.out.println(String.format("Mean: %f", Mean));
        lower_bound = stats.getMean() - ci;
        upper_bound = stats.getMean() + ci;
        System.out.println(String.format("Confidence Interval 90%%: %f, %f", lower_bound, upper_bound));
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

}
