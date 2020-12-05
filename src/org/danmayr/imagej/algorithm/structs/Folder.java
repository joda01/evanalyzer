package org.danmayr.imagej.algorithm.struct;

import java.util.TreeMap;

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
    public void addImage(String imageName, int channelNr, Roi roi) {
        Image actImage = mImages.get(imageName);
        if (null == actImage) {
            actImage = new Image(imageName);
            mImages.put(imageName, actImage);
        }
        actImage.addChannel(channelNr, roi);
    }

    ///
    /// \brief Returns the folder name
    ///
    public String toString() {
        return mFolderName;
    }
}