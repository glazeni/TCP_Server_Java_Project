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

    public static int NUMBER_BLOCKS = 100;
    public static int BLOCKSIZE = 1460; //Mobiperf uses 1358 1460 without TCPHeader
    public static int SO_TIMEOUT = 30000; //10sec - Receiving Timeout
    public static int SOCKET_SNDBUF = 14600; //14,6 Kb
    public static int SOCKET_RCVBUF = 14600; //14,6 Kb
    public static int SERVERPORT = 20000;
    public static String SERVER_IP = "192.168.1.68";

}
