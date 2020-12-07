package org.danmayr.imagej.algorithm.structs;

import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;
import org.danmayr.imagej.algorithm.structs.Pair;


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
    public void addImage(String imageName, TreeMap<Integer, Channel> channels) {
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


    public TreeMap<Integer, double[]> calcStatistic(){
        TreeMap<Integer, double[]> mValues = new TreeMap<>();

        for (Map.Entry<String, Image> image : getImages().entrySet()) {
            TreeMap<Integer, double[]> imgStatistic = image.getValue().getStatistics();
            
            for (Map.Entry<Integer, double[]> channel : imgStatistic.entrySet()) {
                double[] getValues = channel.getValue();
                double[] values = mValues.get(channel.getKey());
                if(values == null){
                    values = new double[getValues.length];
                    mValues.put(channel.getKey(), values);
                }
                for(int n=0;n<getValues.length;n++){
                    values[n]+=getValues[n];
                }
            }
        }

        for (Map.Entry<Integer, double[]> value : mValues.entrySet()) {
            double[] values = value.getValue();
            for(int n=0;n<values.length;n++){
                values[n] =values[n]/getImages().size();
            }
        }

        return mValues;
    }

    public TreeMap<Integer,  Pair<String,String[]>> getTitle(){
        Image img = mImages.firstEntry().getValue();
        return img.getTitle();
    }

}