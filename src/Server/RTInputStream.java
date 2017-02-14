/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Server;

import java.io.InputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.util.Vector;

public class RTInputStream extends FilterInputStream {

    private int bytesTotal = 0;
    public static int bytesGraph=0;
    public Vector<Long> readTimeVector = null;

    public RTInputStream(InputStream in) {
        super(in);
        readTimeVector = new Vector<Long>();
    }

    @Override
    public int read() throws IOException {
        long start = 0;
        int cnt = super.read();
        return cnt;
    }
    

    @Override
    public int read(byte data[]) throws IOException {
        long start = 0;
        int cnt = super.read(data);
        return cnt;
    }

    @Override
    public int read(byte data[], int off, int len) throws IOException {
        long start = System.currentTimeMillis();
        int count = super.read(data, off, len);

        bytesTotal += count;//Sum of all read bytes
        bytesGraph += count;
        return count;
    }
    
    public int getBytes(){
        return bytesTotal;
    }
    
    public void clearBytes(){
        bytesTotal=0;
    }
}
