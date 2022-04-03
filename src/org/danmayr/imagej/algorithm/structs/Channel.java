package org.danmayr.imagej.algorithm.structs;

import java.util.Map;
import java.util.TreeMap;
import org.danmayr.imagej.algorithm.statistics.*;

import ij.gui.Roi;
import ij.plugin.frame.RoiManager;

///
/// \class  Channel
/// \brief  Channel of a picture
///
public class Channel {

    String mName;
    TreeMap<Integer, ParticleInfo> mRois = new TreeMap<>();
    Statistics mStatistics = null;
    String mControlImgPath = "";
    String[] mTitles = { "area size [µm²]", "intensity", "threshold scale", "circularity [0-1]", "validity" };
    String[] mTitleDynamic = { "" };

    ///
    /// \brief Constructor
    ///
    public Channel(String name, Statistics statistics) {
        mName = name;
        mStatistics = statistics;
    }

    ///
    /// \brief start of dynmic title is the index of the title where the dynamic
    /// part starts
    ///
    public Channel(String name, Statistics statistics, String[] titles, int startOfDynmicTitle) {
        mName = name;
        mTitles = titles;
        mStatistics = statistics;
        if (startOfDynmicTitle > 0) {
            mTitleDynamic = new String[titles.length - startOfDynmicTitle];
            int idx = 0;
            for (int n = startOfDynmicTitle; n < titles.length; n++) {
                mTitleDynamic[idx] = titles[n];
                idx++;
            }
        }
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

    public void removeRoi(Integer idx){
        mRois.remove(idx);
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

    public Statistics getStatistic() {
        return mStatistics;
    }

    public String[] getStatisticTitle() {
        return mStatistics.getTitle();
    }

    public String[] getTitle() {
        return mTitles;
    }

    public String[] getDynamicTitle() {
        return mTitleDynamic;
    }

    public void setThershold(double minTH, double maxTH) {
        mStatistics.setThershold(minTH, maxTH);
    }

    public void setNrOfRemovedParticles(int nrOfRemovedParticles) {
        mStatistics.setNrOfRemovedParticles(nrOfRemovedParticles);
    }

    ///
    /// \brief Returns the Region of Interests
    ///
    public TreeMap<Integer, ParticleInfo> getRois() {
        return mRois;
    }

    public void ClearRoi() {
        for (Map.Entry<Integer, ParticleInfo> e : mRois.entrySet()) {
            e.getValue().clearRoi();

        }
    }
}