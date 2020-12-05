package org.danmayr.imagej.algorithm;


public class AnalyseSettings {
    public enum Function {
        noSelection ("--No selection--"),
        calcColoc ("Calc Colocalization"),
        countExosomes ("Count Exosomes");
    
        private final String name;       
    
        private Function(String s) {
            name = s;
        }
    
        public boolean equalsName(String otherName) {
            // (otherName == null) check is not needed because name.equals(null) returns false 
            return name.equals(otherName);
        }
    
        public String toString() {
           return this.name;
        }
    }


    public Function mSelectedFunction;
    public String mInputFolder;
    public String mNegativeControl;
    public String mOutputFolder;
    public int mGreenChannel;
    public String mThersholdMethod;
    public boolean mEnhanceContrastForGreen;
    public boolean mEnhanceContrastForRed;
    public int mMinParticleSize;
    public int mMaxParticleSize;
    public String mSelectedSeries;      // series_1
}
