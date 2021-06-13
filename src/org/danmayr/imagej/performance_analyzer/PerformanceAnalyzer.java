package org.danmayr.imagej.performance_analyzer;

import java.util.TreeMap;

import ij.IJ;

public class PerformanceAnalyzer {

    static TreeMap<String, Entry> mEntry = new TreeMap<>();

    public static void start(String comment) {
        Entry ent = new Entry(comment, System.nanoTime());
        mEntry.put(comment, ent);

    }

    public static void stop(String comment) {
        long diff = System.nanoTime() - mEntry.get(comment).mStartTime;
        IJ.log("Stop " + comment + "; " + Double.toString((double)diff/(double)1000000000));

    }
}
