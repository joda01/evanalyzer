package org.danmayr.imagej;

public class Version {
    static String major = "2";
    static String minor = "10";
    static String fix = "0";
    static String status = "alpha";
    static long build  = 1618161635;

    public static String getVersion() {
        return major + "." + minor + "." + fix + "-" + status + " " + Long.toString(build);
    }
}