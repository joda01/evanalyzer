package org.danmayr.imagej.algorithm.runtime_analyzer;

import java.util.Vector;
import java.util.Calendar;

public class RunTimeAnalyzer {

  static class Entry {
    public String text;
    public long time;
  }

  static String text;
  static long mStartTime = 0;
  static Vector<Entry> mEntries = new Vector<>();

  public static void startAnalyzing(String description) {
    mStartTime = getActTime();
    text = description;
  }

  public static void stopAnalyzing() {
    if (mStartTime > 0) {
      Entry ent = new Entry();
      ent.time = getActTime() - mStartTime;
      ent.text = text;
      mEntries.add(ent);
      mStartTime = 0;
    }
  }

  static long getActTime() { // creating Calendar instance
    Calendar calendar = Calendar.getInstance();
    // Returns current time in millis
    long timeMilli2 = calendar.getTimeInMillis();
    System.out.println("Time in milliseconds using Calendar: " + timeMilli2);
    return timeMilli2;
  }

  public static void generateReport(String folder){
    
  }
}