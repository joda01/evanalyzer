package org.danmayr.imagej.algorithm.calculators;

import java.util.Map;
import java.util.TreeMap;

import org.danmayr.imagej.algorithm.*;



public class ColocCalculator implements ChannelCalculator
{
    private ChannelStatistic mStatistics = new ChannelStatistic();
    private AnalyseSettings mAnalyseSettings;
    static int MAX_THERSHOLD = 255;

    double mColocCount = 0;

    String mName;
    public ColocCalculator(String name,AnalyseSettings settings){
        mName = name;
        mAnalyseSettings = settings;
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }


    @Override
    public String getName(){
        return mName;
    }

    @Override
    public void calcStatistics(Channel[] channels) {
        // TODO Auto-generated method stub
        if(channels.length > 1){
        TreeMap<String, ImageMeasurement> ch0 = channels[0].getMeasurement();
        TreeMap<String, ImageMeasurement> ch1 = channels[1].getMeasurement();
        
        double numberOfTooBigParticles = 0;
        double numberOfTooSmallParticles = 0;
        int colocCount = 0;

        for (Map.Entry<String, ImageMeasurement> entrySet : ch0.entrySet()) {
            ImageMeasurement entry00 = entrySet.getValue();
            ImageMeasurement entry01 = ch1.get(entrySet.getKey());
            if (entry00.areaBinGrayScale > mAnalyseSettings.mMinParticleSize) {
                if (entry00.areaBinGrayScale < mAnalyseSettings.mMaxParticleSize) {
                    if (entry00.areaBinGrayScale > 0) {
                        double sub = Math.abs(MAX_THERSHOLD - Math.abs(entry00.areaBinGrayScale - entry01.areaBinGrayScale));
                        if(sub > 0){
                            colocCount++;
                        }
                    }
                } else {
                    numberOfTooBigParticles++;
                }
            } else {
                numberOfTooSmallParticles++;
            }
        }

        mColocCount = colocCount;
    }

    }

    @Override
    public String toString() {
        String ret = Double.toString(mColocCount);

        return ret;
    }

    @Override
    public String header(){
        String ret = "NrOfColoc";
        return ret;
    }
}
