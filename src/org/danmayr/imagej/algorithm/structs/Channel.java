package org.danmayr.imagej.algorithm.structs;

import java.util.Map;
import java.util.TreeMap;
import org.danmayr.imagej.algorithm.statistics.*;

import ij.gui.Roi;

///
/// \class  Channel
/// \brief  Channel of a picture
///
public class Channel {

    String mName;
    TreeMap<Integer, ParticleInfo> mRois = new TreeMap<>();
    Statistics mStatistics = null;
    String mControlImgPath = "";
    Roi[] ary = null;
    String[] mTitles= { "area size", "intensity","thershold scale", "circularity","validity" };

    ///
    /// \brief Constructor
    ///
    public Channel(String name, Statistics statistics) {
        mName = name;
        mStatistics = statistics;
    }

    public Channel(String name, Statistics statistics, String[] titles) {
        mName = name;
        mTitles = titles;
        mStatistics = statistics;
    }

    ///
    /// \brief Add a Region of Interest
    ///
    public void addRoi(ParticleInfo roi) {
        mRois.put(roi.roiName, roi);
    }

    public void addControlImagePath(String ctrlImagePath) {
        mControlImgPath = ctrlImagePath;
    }

    public String getCtrlImagePath() {
        return mControlImgPath;
    }

    ///
    /// \brief returns the channel name
    ///
    public String toString() {
        return mName;
    }

    public void calcStatistics() {
        mStatistics.calcStatistics(this);
    }

    public double[] getStatistics() {
        return mStatistics.getValues();
    }

    public Statistics getStatistic(){
        return mStatistics;
    }

    public String[] getStatisticTitle() {
        return mStatistics.getTitle();
    }

    public String[] getTitle() {
        return mTitles;
    }

    public void setThershold(double minTH, double maxTH) {
        mStatistics.setThershold(minTH, maxTH);
    }

    public Roi[] getRoisAsArray()
    {
        if(null == ary){
            ary = new  Roi[mRois.size()];
        }else if(ary.length != mRois.size()){
            ary = new  Roi[mRois.size()];
        }
        int i = 0;
        for(Map.Entry<Integer, ParticleInfo> e : mRois.entrySet()){
            ary[i] = e.getValue().getRoi();
            i++;
        }
        return ary;
    }

    ///
    /// \brief Returns the Region of Interests
    ///
    public TreeMap<Integer, ParticleInfo> getRois() {
        return mRois;
    }
}