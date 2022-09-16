package org.danmayr.imagej;

public class Version {
    static final public int major = 7;
    static final public int minor = 4;
    static final public int fix = 0;
    public static String status = "alpha";   // beta, aplha, release
    public final static long build  = 1642186455;

    public static String getVersion() {
        return major + "." + minor + "." + fix + "-" + status + " " + Long.toString(build);
    }

    public static String getGitVersion(){
        return "v"+major + "." + minor + "." + fix;

    }

    public static int version(){
        return major*1000000+ minor*1000+fix;
    }

}