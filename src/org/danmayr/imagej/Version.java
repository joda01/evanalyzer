package org.danmayr.imagej;

public class Version {
    static String major = "3";
    static String minor = "0";
    static String fix = "7";
    static String status = "alpha";
    static long build  = 1622055908;

    public static String getVersion() {
        return major + "." + minor + "." + fix + "-" + status + " " + Long.toString(build);
    }
}