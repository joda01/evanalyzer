package org.danmayr.imagej;

public class Version {
    static String major = "3";
    static String minor = "0";
    static String fix = "0";
    static String status = "alpha";
    static long build  = 1618161635;

    public static String getVersion() {
        return major + "." + minor + "." + fix + "-" + status + " " + Long.toString(build);
    }
}