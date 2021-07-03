package org.danmayr.imagej.algorithm.structs;

import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

import org.danmayr.imagej.algorithm.pipelines.Pipeline.ChannelType;
import org.danmayr.imagej.algorithm.structs.Pair;


///
/// \class  Image
/// \brief  One image consits of a number of channels
///
public class Image {
    String mImageName;
    TreeMap<ChannelType, Channel> mChannels = new TreeMap<>(); // Channel Nr, Channel
    TreeMap<ChannelType, Pair<String,String[]>> mStatisticTitles = new TreeMap<>();
    TreeMap<ChannelType, Pair<String,String[]>> mTitle = new TreeMap<>();
    TreeMap<ChannelType, double[]> mStatistics = new TreeMap<>();
    TreeMap<ChannelType, String> mCtrlImage = new TreeMap<>();


    ///
    /// \brief Constructor
    ///
    public Image(String name) {
        mImageName = name;
    }

    ///
    /// \brief Adds a channel to the image
    ///
    public void addChannel(TreeMap<ChannelType, Channel> channels) {
        mChannels = channels;
        for (Map.Entry<ChannelType, Channel> channel : getChannels().entrySet()) {
            if(channel != null ){
                if(channel.getValue() != null){
                    mStatisticTitles.put(channel.getKey(), new Pair<String, String[]>(channel.getValue().toString(), channel.getValue().getStatisticTitle()));
                    mTitle.put(channel.getKey(), new Pair<String, String[]>(channel.getValue().toString(), channel.getValue().getTitle()));
                    mStatistics.put(channel.getKey(), channel.getValue().getStatistics());
                    mCtrlImage.put(channel.getKey(), channel.getValue().getCtrlImagePath());
                }
            }
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
    public TreeMap<ChannelType, Channel> getChannels(){
        return mChannels;
    }

    public TreeMap<ChannelType, String> getCtrlImages(){
        return mCtrlImage;
    }

    public TreeMap<ChannelType, Pair<String,String[]>> getStatisticTitle(){
        return mStatisticTitles;
    }

    public TreeMap<ChannelType, double[]> getStatistics(){
        return mStatistics;
    }

    public TreeMap<ChannelType, Pair<String,String[]>> getTitle(){
        return mTitle;
    }

}