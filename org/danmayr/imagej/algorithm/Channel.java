package org.danmayr.imagej.algorithm;

import java.util.Vector;
import org.danmayr.imagej.algorithm.ImageMeasurement;

///
/// Informarion for one channel
///
public class Channel {

    AnalyseSettings mAnalyseSettings;
    
    public Channel(String channelName,AnalyseSettings analyseSettings) {
        mChannelName = channelName;
        mAnalyseSettings = analyseSettings;
    }

    public void addMeasurement(ImageMeasurement imageMeasurement) {
        mMeasurements.add(imageMeasurement);
    }

    ///
    /// Calculate statistics for this channel
    ///
    public void calcStatistics() {
        double avgGraySkale = 0;
        double avgAreaSize = 0;
        int exosomCount = 0;
        int numberOfTooBigParticles = 0;
        int numberOfTooSmallParticles = 0;

        for (ImageMeasurement entry : mMeasurements) {
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

        mStatistics.numberOfParticles = mMeasurements.size();
        mStatistics.numberOfTooSmallParticles = numberOfTooSmallParticles;
        mStatistics.numberOfTooBigParticles = numberOfTooBigParticles;
        mStatistics.numberOfParticlesInRange = exosomCount;
        mStatistics.avgGrayScale = avgGraySkale / exosomCount;
        mStatistics.avgAreaSize = avgAreaSize / exosomCount;

    }

    public String channelName() {
        return mChannelName;
    }

    public String header() {
        String ret = "NrOfParticles;NrOfSmall;NrOfBig;NrOfExosomes;AvgGrayscale;AvgAreaSize";
        return ret;
    }

    public String toString() {
        calcStatistics();
        String ret = Double.toString(mStatistics.numberOfParticles) + ";"
                + Double.toString(mStatistics.numberOfTooSmallParticles) + ";"
                + Double.toString(mStatistics.numberOfTooBigParticles) + ";"
                + Double.toString(mStatistics.numberOfParticlesInRange) + ";"
                + Double.toString(mStatistics.avgGrayScale) + ";" + Double.toString(mStatistics.avgAreaSize);

        return ret;
    }

    public ChannelStatistic getStatistics() {
        return mStatistics;
    }

    String mChannelName = "";
    ChannelStatistic mStatistics = new ChannelStatistic();
    Vector<ImageMeasurement> mMeasurements = new Vector<>();

}