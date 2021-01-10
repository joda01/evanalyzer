package org.danmayr.imagej.algorithm;

import org.danmayr.imagej.algorithm.pipelines.*;

public class AnalyseSettings {

    public enum ReportType {
        FullReport, FastReport
    }

    public enum CotrolPicture {
        WithControlPicture, WithoutControlPicture
    }

    public enum Function {
        noSelection("--No selection--"), calcColoc("Calc Colocalization"), countExosomes("Count Exosomes");

        private final String name;

        private Function(String s) {
            name = s;
        }

        public boolean equalsName(String otherName) {
            // (otherName == null) check is not needed because name.equals(null) returns
            // false
            return name.equals(otherName);
        }

        public String toString() {
            return this.name;
        }
    }

    public class ChannelSettings {
        public Pipeline.ChannelType type;
        public String mThersholdMethod;
        public boolean enhanceContrast;
        public int minThershold = -1;
        public int maxThershold = 65535;
    }

    public CotrolPicture mSaveDebugImages = CotrolPicture.WithControlPicture;
    public ReportType reportType = ReportType.FullReport;
    public Function mSelectedFunction;
    public String mInputFolder;
    public String mOutputFolder;
    public String mSelectedSeries; // series_1
    public double mMinParticleSize = 0.0;
    public double mMaxParticleSize = 999999999;
    public double mMinCircularity = 0.0;
    public double minIntensity = 0.0;

    public String mOutputFileName="";

    public ChannelSettings ch0 = new ChannelSettings();
    public ChannelSettings ch1 = new ChannelSettings();

}
