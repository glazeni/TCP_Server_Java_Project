/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Server;

import java.util.Timer;
import java.util.TimerTask;


/**
 *
 * @author glazen
 */
public class ReminderServer extends Thread {

    private Timer timer = null;
    private DataMeasurement dataMeasurement = null;
    private RTInputStream RTin = null;
    private int i = 0;

    public ReminderServer(int seconds, DataMeasurement _dataMeasurement, RTInputStream _RTin) {
        this.RTin = _RTin;
        this.dataMeasurement = _dataMeasurement;
        timer = new Timer();
        //timer.schedule(new RemindTask(), 0, seconds);
        timer.scheduleAtFixedRate(new RemindTask(this.RTin), 0, (seconds * 1000));
    }
    
    public synchronized void cancelTimer(){
        timer.cancel();
    }

    class RemindTask extends TimerTask {
        private RTInputStream RTinput = null;

        public RemindTask(RTInputStream _RTinput) {
            this.RTinput = _RTinput;
            //Do nothihng in constructor
        }

        @Override
        public void run() {
            try {
                dataMeasurement.add_SampleSecond_up(this.RTinput.getBytes2Bits());
                System.out.println("REMINDER SERVER" + i + " with " + "bits=" + this.RTinput.getBytes2Bits());
                i++;
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                this.RTinput.clearBytes();
            }
        }
    }
}
