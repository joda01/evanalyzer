package org.danmayr.imagej.algorithm.structs;

import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

import org.danmayr.imagej.algorithm.pipelines.Pipeline.ChannelType;
import org.danmayr.imagej.algorithm.structs.Pair;
import ij.*;


///
/// \class  Folder
/// \brief  Folder of pictures
///
public class Folder {

    String mFolderName;
    TreeMap<String, Image> mImages = new TreeMap<>(); // ImageName, Image

    ///
    /// \brief Constructor
    ///
    public Folder(String name) {
        mFolderName = name;
    }

    ///
    /// Add an image to the folder
    ///
    public void addImage(String imageName, TreeMap<ChannelType, Channel> channels) {
        Image actImage = mImages.get(imageName);
        if (null == actImage) {
            actImage = new Image(imageName);
            mImages.put(imageName, actImage);
        }
        actImage.addChannel(channels);
    }

    ///
    /// \brief Returns the folder name
    ///
    public String toString() {
        return mFolderName;
    }

    public TreeMap<String, Image> getImages(){
        return mImages;
    }


    public TreeMap<ChannelType, double[]> calcStatistic(){
        TreeMap<ChannelType, double[]> mValues = new TreeMap<>();

        for (Map.Entry<String, Image> image : getImages().entrySet()) {
            TreeMap<ChannelType, double[]> imgStatistic = image.getValue().getStatistics();
            
            for (Map.Entry<ChannelType, double[]> channel : imgStatistic.entrySet()) {
                double[] getValues = channel.getValue();
                double[] values = mValues.get(channel.getKey());
                if(values == null){
                    values = new double[getValues.length];
                    mValues.put(channel.getKey(), values);
                }
                for(int n=0;n<getValues.length;n++){
                    if(n < values.length && n < getValues.length)
                        values[n]+=getValues[n];// Array index out of bound
                    else
                        IJ.log("ERR: Values.length: " + values.length + " " + channel.getKey().toString());
                }
            }
        }

        for (Map.Entry<ChannelType, double[]> value : mValues.entrySet()) {
            double[] values = value.getValue();
            for(int n=0;n<values.length;n++){
                values[n] =values[n]/getImages().size();
            }
        }

        return mValues;
    }

    public TreeMap<ChannelType,  Pair<String,String[]>> getStatisticTitle(){
        Image img = mImages.firstEntry().getValue();
        return img.getStatisticTitle();
    }

}