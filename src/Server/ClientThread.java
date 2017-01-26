/*
* Class that handles a new thread in ServerSide when a TCP connection is made from ClientSide
 */
package Server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Random;

public class ClientThread extends Thread {

    private long startThread = System.currentTimeMillis();
    private Socket clientSocket = null;
    private WriteXMLFile_Deltas writeXMLFile_Deltas = null;
    private WriteXMLFile_bytes1sec writeXML1secBytes_Server = null;
    private WriteXMLFile_bytes1sec writeXML1secBytes_Client = null;

    private RTInputStream RTin;
    private RTOutputStream RTout;
    private DataInputStream dataIn;
    private DataOutputStream dataOut;
    private DataMeasurement dataMeasurement;
    private ReminderServer reminderServer;

    private int byteCnt = 0;

    public ClientThread(Socket _clientSocket, DataMeasurement _dataMeasurement) {
        try {
            this.clientSocket = _clientSocket;
            this.dataMeasurement = _dataMeasurement;
            RTin = new RTInputStream(clientSocket.getInputStream());
            RTout = new RTOutputStream(clientSocket.getOutputStream());
            dataIn = new DataInputStream(RTin);
            dataOut = new DataOutputStream(RTout);
        } catch (IOException ex) {
            System.err.println("Client Thread Failure:" + ex.getMessage());
        }
    }

    //Runnable Data Receiver
    public void run() {

        try {
            ServerUI.isTCPservDone = false;
            uplink_Server_rcv();
            sleep(1000);
            downlink_Server_snd();
            try {
                //Read length
                int length = dataIn.readInt();
                dataMeasurement.deltaINVector_uplink.clear();
                dataMeasurement.deltaOUTVector_downlink.clear();
                //Read Delta Vectors
                for (int j = 0; j < length; j++) {
                    dataMeasurement.deltaINVector_uplink.add(dataIn.readLong());
                    dataMeasurement.deltaOUTVector_downlink.add(dataIn.readLong());
                }
                //Receive 1secBytes Vector
                int size = dataIn.readInt();
                dataMeasurement.SampleSecondClient.clear();
                for (int k = 0; k < size; k++) {
                    int bytecnt = dataIn.readInt();
                    long sampleTime = dataIn.readLong();
                    dataMeasurement.SampleSecondClient.add(new Data1secBytes(bytecnt, sampleTime));
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        } catch (Exception ex) {
            System.err.println("Receiving Data Failure" + ex.getMessage());
        } finally {
            try {
                if (clientSocket != null) {
                    clientSocket.close();
                }

                for (int i = 1; i < RTin.readTimeVector.size(); i++) {
                    dataMeasurement.deltaINVector_downlink.add(RTout.writeTimeVector.get(i) - RTout.writeTimeVector.get(i - 1));
                    dataMeasurement.deltaOUTVector_uplink.add(RTin.readTimeVector.get(i) - RTin.readTimeVector.get(i - 1));
                }

                writeXMLFile_Deltas = new WriteXMLFile_Deltas("Deltas", dataMeasurement.deltaINVector_uplink, dataMeasurement.deltaINVector_downlink,
                        dataMeasurement.deltaOUTVector_uplink, dataMeasurement.deltaOUTVector_downlink);
                writeXML1secBytes_Client = new WriteXMLFile_bytes1sec("Client-1secBytes", dataMeasurement.SampleSecondClient);
                writeXML1secBytes_Server = new WriteXMLFile_bytes1sec("Server-1secBytes", dataMeasurement.SampleSecondServer);
                ServerUI.isTCPservDone = true;
                DataMeasurement.TimeThread(startThread, System.currentTimeMillis());
            } catch (IOException ex) {
                System.err.println("Closing Server Side Socket Failure" + ex.getMessage());
            }
        }
    }

    private void uplink_Server_rcv() {
        try {
            byte[] rcv_buf = new byte[Constants.BLOCKSIZE_UPLINK];
            int num_blocks = 0, n = 0;
            num_blocks = dataIn.readInt();
            System.out.println("\nuplink_Server_rcv with " + "Number Blocks=" + num_blocks);
            //Initialize Timer
            reminderServer = new ReminderServer(1, this.dataMeasurement);
            for (int i = 0; i < num_blocks; i++) {
                byteCnt = 0;
                //Cycle to read each block
                do {
                    n = RTin.read(rcv_buf, byteCnt, Constants.BLOCKSIZE_UPLINK - byteCnt);

                    if (n > 0) {
                        byteCnt += n;
                    }

                    if (byteCnt < Constants.BLOCKSIZE_UPLINK) {
                        //Keep reading MTU
                    } else {
                        RTin.readTimeVector.add(System.currentTimeMillis());
                        dataMeasurement.deltaByteCount_uplink.add(byteCnt);
                        System.out.println("Reach the end of the block " + i + " with " + n + " bytes read & byteCount=" + byteCnt);
                        break;
                    }
                } while ((n > 0) && (byteCnt < Constants.BLOCKSIZE_UPLINK));
                if (n == -1) {
                    System.out.println("Exited with n=-1");
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            reminderServer.timer.cancel();
        }
    }

    private void downlink_Server_snd() {
        try {
            int num_blocks = Constants.NUMBER_BLOCKS;
            byte[] snd_buf = new byte[Constants.BLOCKSIZE_DOWNLINK];
            new Random().nextBytes(snd_buf);

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

}
