package org.danmayr.imagej.algorithm.structs;

import java.util.TreeMap;

///
/// \class  Channel
/// \brief  Channel of a picture
///
public class Channel {

    int mChannelNr = 0;
    String mName;
    TreeMap<String, ParticleInfo> mRois = new TreeMap<>();

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
        mRois.put(roi.toString(), roi);
    }

    ///
    /// \brief returns the channel name
    ///
    public String toString() {
        return Integer.toString(mChannelNr);
    }

    public Integer getChannelNumber() {
        return mChannelNr;
    }

    ///
    /// \brief Returns the Region of Interests
    ///
    public TreeMap<String, ParticleInfo> getRois() {
        return mRois;
    }
}