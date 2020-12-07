package org.danmayr.imagej.algorithm.structs;

import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;
import org.danmayr.imagej.algorithm.structs.Pair;


///
/// \class  Image
/// \brief  One image consits of a number of channels
///
public class Image {
    String mImageName;
    TreeMap<Integer, Channel> mChannels = new TreeMap<>(); // Channel Nr, Channel
    TreeMap<Integer, Pair<String,String[]>> mStatisticTitles = new TreeMap<>();
    TreeMap<Integer, Pair<String,String[]>> mTitle = new TreeMap<>();
    TreeMap<Integer, double[]> mStatistics = new TreeMap<>();
    TreeMap<Integer, String> mCtrlImage = new TreeMap<>();


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
        for (Map.Entry<Integer, Channel> channel : getChannels().entrySet()) {
            mStatisticTitles.put(channel.getKey(), new Pair<String, String[]>(channel.getValue().toString(), channel.getValue().getStatisticTitle()));
            mTitle.put(channel.getKey(), new Pair<String, String[]>(channel.getValue().toString(), channel.getValue().getTitle()));
            mStatistics.put(channel.getKey(), channel.getValue().getStatistics());
            mCtrlImage.put(channel.getKey(), channel.getValue().getCtrlImagePath());
        }
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

    public TreeMap<Integer, String> getCtrlImages(){
        return mCtrlImage;
    }

    public TreeMap<Integer, Pair<String,String[]>> getStatisticTitle(){
        return mStatisticTitles;
    }

    public TreeMap<Integer, double[]> getStatistics(){
        return mStatistics;
    }

    public TreeMap<Integer, Pair<String,String[]>> getTitle(){
        return mTitle;
    }

}