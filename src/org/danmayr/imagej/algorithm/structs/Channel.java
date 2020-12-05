package org.danmayr.imagej.algorithm.struct;

import java.util.TreeMap;

///
/// \class  Channel
/// \brief  Channel of a picture
///
public class Channel {

    int mChannelNr = 0;
    TreeMap<String, Roi> mRois = new TreeMap<>();

    ///
    /// \brief Constructor
    ///
    public Channel(int channelNr) {
        mChannelNr = channelNr;
    }

    ///
    /// \brief Add a Region of Interest
    ///
    public void addRoi(Roi roi) {
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
    public TreeMap<String, Roi> getRois() {
        return mRois;
    }
}