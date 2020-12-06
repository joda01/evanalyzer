package org.danmayr.imagej.algorithm.structs;

import java.util.TreeMap;

///
/// \class  Channel
/// \brief  Channel of a picture
///
public class Channel {

    int mChannelNr = 0;
    String mName;
    TreeMap<Integer, ParticleInfo> mRois = new TreeMap<>();

    ///
    /// \brief Constructor
    ///
    public Channel(int channelNr, String name) {
        mChannelNr = channelNr;
        mName = name;
    }

    ///
    /// \brief Add a Region of Interest
    ///
    public void addRoi(ParticleInfo roi) {
        mRois.put(roi.roiName, roi);
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

    ///
    /// \brief Returns the Region of Interests
    ///
    public TreeMap<Integer, ParticleInfo> getRois() {
        return mRois;
    }
}