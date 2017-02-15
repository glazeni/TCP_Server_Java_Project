/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Server;

import java.util.Date;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;
import org.jfree.data.time.Second;

/**
 *
 * @author glazen
 */
public class ReminderServer extends Thread {

    public volatile boolean isRunning;
    public Timer timer = null;
    private DataMeasurement dataMeasurement = null;
    private RTInputStream RTin = null;
    public int i = 0;

    public ReminderServer(int seconds, DataMeasurement _dataMeasurement, RTInputStream _RTin) {
        this.RTin = _RTin;
        this.dataMeasurement = _dataMeasurement;
        
        timer = new Timer();
        //timer.schedule(new RemindTask(), 0, seconds);
        timer.scheduleAtFixedRate(new RemindTask(), 0, (seconds * 1000));

    }

    class RemindTask extends TimerTask {

        public RemindTask() {
            //Do nothihng in constructor
        }

        public void run() {
            try {
                dataMeasurement.add_SampleSecond_up(RTin.getBytes(), System.currentTimeMillis());
                System.out.println("REMINDER SERVER" + i+ " with " + "bytes=" + RTin.getBytes());
                i++;
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                RTin.clearBytes();
            }
        }
    }
}
