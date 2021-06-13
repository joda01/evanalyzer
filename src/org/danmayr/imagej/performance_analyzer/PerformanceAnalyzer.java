package org.danmayr.imagej.performance_analyzer;

import java.util.TreeMap;

import org.danmayr.imagej.gui.EvColocDialog;

import ij.IJ;

public class PerformanceAnalyzer {

    static TreeMap<String, Entry> mEntry = new TreeMap<>();
    static EvColocDialog mDialog = null;

    public static void setGui(EvColocDialog dialog) {
        mDialog = dialog;
    }

    public static void start(String comment) {
        Entry ent = new Entry(comment, System.nanoTime());
        mEntry.put(comment, ent);
        if (null != mDialog) {
            mDialog.addLogEntry("CMD START: " + leftpad(comment, 50) + "\t");
        }
    }

    public static void stop(String comment) {
        long diff = System.nanoTime() - mEntry.get(comment).mStartTime;
        if (null == mDialog) {
            IJ.log("CMD: " + comment + "; " + Double.toString((double) diff / (double) 1000000000));
        } else {
            mDialog.addLogEntry("CMD FINISH: " + leftpad(comment, 50) + "\t"
                    + Double.toString((double) diff / (double) 1000000000) + " s");
        }

    }

    private static String leftpad(String text, int length) {
        // String.format("%15s",s) // pads right
        // String.format("%-15s",s) // pads left

        return String.format("%-" + length + "s", text);
    }
}
