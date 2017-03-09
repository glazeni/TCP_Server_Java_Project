/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author glazen
 */
package Server;

public class Constants {

    public static int NUMBER_PACKETS = 50;
    public static int PACKETSIZE = 1460; //Only used in Packet Train Method
    public static int BUFFERSIZE = 512000; // 256Kb
    public static int SO_TIMEOUT = 60000; //30sec - Receiving/Writting Timeout
    public static int SOCKET_SNDBUF = 51200; //128 Kb
    public static int SOCKET_RCVBUF = 51200; //128 Kb
    public static int SERVERPORT = 20000;
    public static final String FINAL_MSG = "END";
    public static long PACKET_GAP=0;
    public static String SERVER_IP = "193.136.127.218";

}
