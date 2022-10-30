package org.danmayr.imagej.algorithm.structs;

import java.util.TreeMap;

import org.danmayr.imagej.algorithm.pipelines.Pipeline.ChannelType;

///
/// \class  Folder
/// \brief  Folder of pictures
///
public class FolderResults {

    TreeMap<String, Folder> mFolders = new TreeMap<>(); // ImageName, Image

    ///
    /// \brief Constructor
    ///
    public FolderResults() {
    }

    ///
    /// Add an image to the folder
    ///
    public void addImage(String folderName, Image img) {
        Folder actFolder = mFolders.get(folderName);
        if (null == actFolder) {
            actFolder = new Folder(folderName);
            mFolders.put(folderName, actFolder);
        }
        actFolder.addImage(img);
    }

    public TreeMap<String, Folder> getFolders(){
        return mFolders;
    }
}