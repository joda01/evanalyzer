package org.danmayr.imagej;

public class Version {
    static final public int major = 8;
    static final public int minor = 2;
    static final public int fix = 1;
    public static String status = "beta";   // beta, aplha, release
    public final static long build  = 1692514466;

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