/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Vector;
import org.apache.commons.math3.distribution.TDistribution;
import org.apache.commons.math3.exception.MathIllegalArgumentException;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

/**
 *
 * @author glazen
 */
public class RunShellCommands extends Thread {

    private String cmd = null;
    private Vector<Integer> ByteSecondShell = null;
    private BufferedReader buffReader = null;
    private String line = null;
    private String[] parts = null;
    private String part1 = null;
    private String part2 = null;
    private double Mean = 0;
    private double lower_bound = 0;
    private double upper_bound = 0;
    private boolean isIperfSettings;
    private boolean isNagleDisable;
    private int ID = 0;
    private int multiplier = 0;
    private String direction = null;
    private WriteXMLFile_bytes1sec writeXMLFile_bytes1sec = null;

    public RunShellCommands(int _ID, String _cmd, String _direction, boolean _isIperfSettings, boolean _isNagleDisable) {
        this.ID = _ID;
        this.cmd = _cmd;
        this.direction = _direction;
        this.isIperfSettings = _isIperfSettings;
        this.isNagleDisable = _isNagleDisable;
        this.ByteSecondShell = new Vector<Integer>();
    }

    @Override
    public void run() {
        try {
            Process proc = Runtime.getRuntime().exec(cmd);

            // Read the output
            buffReader = new BufferedReader(new InputStreamReader(proc.getInputStream()));

            while ((line = buffReader.readLine()) != null) {
                if (line.contains("sender")) {
                    break;
                }
                System.out.print("Line " + line + "\n");
                part1 = line.replaceAll("\\s+", "");
                if (part1.contains("sec")) {
                    parts = part1.split("sec");
                    parts = parts[1].split("Bytes");
                    part2 = parts[1].substring(0, parts[1].length() - 5);
                    if (part2.contains("G")) {
                        multiplier = 1000000000;
                    } else if (part2.contains("M")) {
                        multiplier = 1000000;
                    } else if (part2.contains("K")) {
                        multiplier = 1000;
                    } else {
                        multiplier = 1;
                    }
                    part2 = parts[1].substring(0, parts[1].length() - 6);
                    int value = (int) Math.round(Float.parseFloat(part2) * multiplier);
                    this.ByteSecondShell.add(value);
                    System.out.print("Value " + value + "\n");
                }
            }
            try {
                proc.waitFor();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            int total = 0;
            for (int i = 0; i < ByteSecondShell.size(); i++) {
                total += ByteSecondShell.get(i);
            }
            System.err.println("Average: " + (total / ByteSecondShell.size()) + " Transfered: " + total);
            Tstudent(ByteSecondShell);
            if (isIperfSettings && isNagleDisable) {
                writeXMLFile_bytes1sec = new WriteXMLFile_bytes1sec(ID + direction + "-iperf_NagleOFF", ByteSecondShell, total, Mean, lower_bound, upper_bound, "MV_1secThread/iperf_Settings/");
            } else if (isIperfSettings && !isNagleDisable) {
                writeXMLFile_bytes1sec = new WriteXMLFile_bytes1sec(ID + direction + "-iperf_NagleON", ByteSecondShell, total, Mean, lower_bound, upper_bound, "MV_1secThread/iperf_Settings/");
            } else if (!isIperfSettings && !isNagleDisable) {
                writeXMLFile_bytes1sec = new WriteXMLFile_bytes1sec(ID + direction + "-iperf_NagleON", ByteSecondShell, total, Mean, lower_bound, upper_bound, "MV_1secThread/thesis_Settings/");
            } else if (!isIperfSettings && isNagleDisable) {
                writeXMLFile_bytes1sec = new WriteXMLFile_bytes1sec(ID + direction + "-iperf_NagleOFF", ByteSecondShell, total, Mean, lower_bound, upper_bound, "MV_1secThread/thesis_Settings/");
            }
        }

    }

    private Vector<Integer> getByteSecondShellVector() {
        return ByteSecondShell;
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
