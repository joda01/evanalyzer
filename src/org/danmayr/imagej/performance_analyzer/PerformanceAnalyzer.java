package org.danmayr.imagej.performance_analyzer;

import ij.IJ;

public class PerformanceAnalyzer {
    static long mStartTime = 0;
    static String mComment = "";

    public static void start(String comment)
    {
        if(mStartTime!=0){
            stop();
        }
        mComment = comment;
        mStartTime = System.nanoTime();
        //IJ.log("Start " + comment);
    }

    public static void stop()
    {
        if(mStartTime > 0){
            long diff = System.nanoTime() - mStartTime;
            IJ.log("Stop " + mComment + "; " + Long.toString(diff));
            mStartTime = 0;
            mComment = "";
        }
    }
}
