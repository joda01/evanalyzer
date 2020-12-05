package org.danmayr.imagej.algorithm.calculators;

import java.util.Map;
import java.util.TreeMap;

import org.danmayr.imagej.algorithm.*;

public class AvgCalculator implements ChannelCalculator {

    public class ChannelStatistic {
        public double numberOfParticles = 0;
        public double numberOfTooSmallParticles = 0;
        public double numberOfTooBigParticles = 0;
        public double numberOfParticlesInRange = 0;
        public double avgGrayScale = 0;
        public double avgAreaSize = 0;
        public double counter = 0;
    }

    private ChannelStatistic mStatistics = new ChannelStatistic();
    private AnalyseSettings mAnalyseSettings;

    String mName;

    public AvgCalculator(String name, AnalyseSettings settings) {
        mName = name;
        mAnalyseSettings = settings;
    }

    // Default version of clone() method

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    @Override
    public String getName() {
        return mName;
    }

    @Override
    public void calcStatistics(Channel[] channels) {
        // TODO Auto-generated method stub
        TreeMap<String, ImageMeasurement> measurement = channels[0].getMeasurement();

        double avgGraySkale = 0;
        double avgAreaSize = 0;
        int exosomCount = 0;
        int numberOfTooBigParticles = 0;
        int numberOfTooSmallParticles = 0;

        for (Map.Entry<String, ImageMeasurement> entrySet : measurement.entrySet()) {
            ImageMeasurement entry = entrySet.getValue();
            if (entry.areaBinGrayScale > mAnalyseSettings.mMinParticleSize) {
                if (entry.areaBinGrayScale < mAnalyseSettings.mMaxParticleSize) {
                    if (entry.areaBinGrayScale > 0) {
                        avgGraySkale += entry.areaGrayScale;
                        avgAreaSize += entry.areaSize;
                        exosomCount++;
                    }
                } else {
                    numberOfTooBigParticles++;
                }
            } else {
                numberOfTooSmallParticles++;
            }
        }

        mStatistics.numberOfParticles = measurement.size();
        mStatistics.numberOfTooSmallParticles = numberOfTooSmallParticles;
        mStatistics.numberOfTooBigParticles = numberOfTooBigParticles;
        mStatistics.numberOfParticlesInRange = exosomCount;
        mStatistics.avgGrayScale = avgGraySkale / exosomCount;
        mStatistics.avgAreaSize = avgAreaSize / exosomCount;

    }

    @Override
    public String toString() {
        String ret = Double.toString(mStatistics.numberOfParticles) + ";"
                + Double.toString(mStatistics.numberOfTooSmallParticles) + ";"
                + Double.toString(mStatistics.numberOfTooBigParticles) + ";"
                + Double.toString(mStatistics.numberOfParticlesInRange) + ";"
                + Double.toString(mStatistics.avgGrayScale) + ";" + Double.toString(mStatistics.avgAreaSize);

        return ret;
    }

    @Override
    public String header() {
        String ret = "NrOfParticles;NrOfSmall;NrOfBig;NrOfExosomes;AvgGrayscale;AvgAreaSize";
        return ret;
    }

}
