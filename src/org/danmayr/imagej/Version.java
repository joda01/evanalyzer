package org.danmayr.imagej;

public class Version {
    static String major = "5";
    static String minor = "1";
    static String fix = "4";
    public static String status = "release";   // beta, aplha, release
    static long build  = 1622055908;

    public static String getVersion() {
        return major + "." + minor + "." + fix + "-" + status + " " + Long.toString(build);
    }
}