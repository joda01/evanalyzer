package org.danmayr.imagej;

public class Version {
    static String major = "2";
    static String minor = "5";
    static String fix = "1";
    static String status = "beta";
    static long build;

    public static String getVersion() {
        return major + "." + minor + "." + fix + "-" + status + " " + Long.toString(build);
    }
}