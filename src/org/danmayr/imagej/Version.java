package org.danmayr.imagej;

public class Version {
    static String major = "2";
    static String minor = "9";
    static String fix = "1";
    static String status = "beta";
    static long build  = 1618161635;

    public static String getVersion() {
        return major + "." + minor + "." + fix + "-" + status + " " + Long.toString(build);
    }
}