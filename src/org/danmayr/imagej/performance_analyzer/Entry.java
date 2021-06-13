package org.danmayr.imagej.performance_analyzer;

public class Entry{
    public Entry(String comm, long time){
        mStartTime = time;
        mComment = comm;
    }
    public long mStartTime = 0;
    public String mComment = "";
}