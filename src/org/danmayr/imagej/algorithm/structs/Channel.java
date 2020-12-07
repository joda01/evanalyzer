package org.danmayr.imagej.algorithm.structs;

import java.util.TreeMap;
import org.danmayr.imagej.algorithm.statistics.*;

///
/// \class  Channel
/// \brief  Channel of a picture
///
public class Channel {

    int mChannelNr = 0;
    String mName;
    TreeMap<Integer, ParticleInfo> mRois = new TreeMap<>();
    Statistics mStatistics = null;
    String mControlImgPath = "";

    ///
    /// \brief Constructor
    ///
    public Channel(int channelNr, String name, Statistics statistics) {
        mChannelNr = channelNr;
        mName = name;
        mStatistics = statistics;
    }

    ///
    /// \brief Add a Region of Interest
    ///
    public void addRoi(ParticleInfo roi) {
        mRois.put(roi.roiName, roi);
    }

    public void addControlImagePath(String ctrlImagePath){
        mControlImgPath = ctrlImagePath;
    }

    public String getCtrlImagePath(){
        return mControlImgPath;
    }

    ///
    /// \brief returns the channel name
    ///
    public String toString() {
        return mName;
    }

    public Integer getChannelNumber() {
        return mChannelNr;
    }

    public void calcStatistics(){
        mStatistics.calcStatistics(this);
    }

    public double[] getStatistics() {
        return mStatistics.getValues();
    }

    public String[] getTitle() {
        return mStatistics.getTitle();
    }

    ///
    /// \brief Returns the Region of Interests
    ///
    public TreeMap<Integer, ParticleInfo> getRois() {
        return mRois;
    }
}