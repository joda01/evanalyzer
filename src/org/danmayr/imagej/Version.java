package org.danmayr.imagej;

public class Version {
    static String major = "4";
    static String minor = "3";
    static String fix = "0";
    public static String status = "alpha";   // beta, aplha, release
    static long build  = 1622055908;

    public static String getVersion() {
        return major + "." + minor + "." + fix + "-" + status + " " + Long.toString(build);
    }
}