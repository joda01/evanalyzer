package org.danmayr.imagej.algorithm.structs;

import java.util.TreeMap;
import org.danmayr.imagej.algorithm.statistics.*;

///
/// \class  Channel
/// \brief  Channel of a picture
///
public class Channel {

    String mName;
    TreeMap<Integer, ParticleInfo> mRois = new TreeMap<>();
    Statistics mStatistics = null;
    String mControlImgPath = "";

    ///
    /// \brief Constructor
    ///
    public Channel(String name, Statistics statistics) {
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

    public void calcStatistics(){
        mStatistics.calcStatistics(this);
    }

    public double[] getStatistics() {
        return mStatistics.getValues();
    }

    public String[] getStatisticTitle() {
        return mStatistics.getTitle();
    }


    public String[] getTitle() {
        return mRois.firstEntry().getValue().getTitle();
    }


    public void setThershold(double minTH, double maxTH){
        mStatistics.setThershold(minTH, maxTH);
    }

    ///
    /// \brief Returns the Region of Interests
    ///
    public TreeMap<Integer, ParticleInfo> getRois() {
        return mRois;
    }
}