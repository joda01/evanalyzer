package org.danmayr.imagej.algorithm.structs;

import java.util.TreeMap;

///
/// \class  Image
/// \brief  One image consits of a number of channels
///
public class Image {
    String mImageName;
    TreeMap<Integer, Channel> mChannels = new TreeMap<>(); // Channel Nr, Channel

    ///
    /// \brief Constructor
    ///
    public Image(String name) {
        mImageName = name;
    }

    ///
    /// \brief Adds a channel to the image
    ///
    public void addChannel(TreeMap<Integer, Channel> channels) {
        mChannels = channels;
    }

    ///
    /// \brief Returns the image name
    ///
    public String toString(){
        return mImageName;
    }

    ///
    /// \brief Returns the channels of the image
    ///
    public TreeMap<Integer, Channel> getChannels(){
        return mChannels;
    }

}