package Server;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Vector;
import org.apache.commons.math3.distribution.TDistribution;
import org.apache.commons.math3.exception.MathIllegalArgumentException;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author glazen
 */
public class ReadTXTFile extends Thread {

    BufferedReader buffReader = null;
    String line = null;
    String[] parts = null;
    String part1 = null;
    String part2 = null;
    int multiplier = 0;
    Vector<Integer> ShellVector = null;

    public ReadTXTFile() throws IOException {
        ShellVector = new Vector<Integer>();
        try {
            StringBuilder sb = new StringBuilder();
            buffReader = new BufferedReader(new FileReader("/Users/glazen/Desktop/ANDROID_RESULTS/VODAFONE/3G/3G_VODAFONE-Uplink_NagleON.txt"));

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
                    ShellVector.add(value);
                    System.out.print("Value " + value + "\n");
                }
            }
            //Export to XML
            Tstudent tstudent = new Tstudent(ShellVector);
            WriteXMLFile_bytes1sec writeXMLFile_bytes1sec = new WriteXMLFile_bytes1sec("3G_VODAFONE-Uplink_NagleON", ShellVector, tstudent.getTotalBytes(), tstudent.getMeanVector(), tstudent.getLowerBoundVector(), tstudent.getUpperBoundVector(), "/Results/");
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            buffReader.close();
        }
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

//    public static void main(String[] args) throws IOException {
//        ReadTXTFile readTXTFile = new ReadTXTFile();
//        readTXTFile.start();
//    }
}
