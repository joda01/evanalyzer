package org.danmayr.imagej.algorithm;

import java.util.Vector;

import org.danmayr.imagej.algorithm.pipelines.*;

public class AnalyseSettings {

    public enum ReportType {
        FullReport, FastReport
    }

    public enum CotrolPicture {
        WithControlPicture, WithoutControlPicture
    }

    public enum Function {
        noSelection("--No selection--"), calcColoc("EV Colocalization"), countExosomes("EV Counting"), countInCellExosomes("EV Counting in Cells"), countInCellExosomesWithCellSeparation("EV Counting in Cells with cell separation [BETA]");

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


    public CotrolPicture mSaveDebugImages = CotrolPicture.WithControlPicture;
    public ReportType reportType = ReportType.FullReport;
    public Function mSelectedFunction;
    public String mInputFolder;
    public String mOutputFolder;
    public int mSelectedSeries; // series_1  = 0
    public double mMinParticleSize = 0.0;
    public double mMaxParticleSize = 999999999;
    public double mMinCircularity = 0.0;
    public double minIntensity = 0.0;
    public String mOutputFileName="";
    public Vector<ChannelSettings> channelSettings = new Vector<ChannelSettings>();
    public boolean mCountEvsPerCell = false;
    public boolean mCalcColoc = false;

}
