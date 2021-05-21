package org.danmayr.imagej.algorithm.pipelines;

import ij.*;
import ij.process.*;
import ij.gui.*;

import java.io.File;
import ij.plugin.frame.RoiManager;

import java.util.*;
import org.danmayr.imagej.algorithm.structs.*;
import org.danmayr.imagej.algorithm.AnalyseSettings;
import org.danmayr.imagej.algorithm.ChannelSettings;
import org.danmayr.imagej.algorithm.filters.Filter;

///
/// \class  Channel
/// \brief  Channel of a picture
///
abstract public class Pipeline {

  // Enum which contains the color indexes for a RGBStackMerge
  // see: https://imagej.nih.gov/ij/developer/source/ij/plugin/RGBStackMerge.java.html
  private enum ChannelColor{
    RED(0),
    GREEN(1),
    BLUE(2),
    GRAY(3),
    CYAN(4),
    MAGENTA(5),
    YELLOW(7);

    private ChannelColor(int idx){
      mIdx = idx;
    }

    public int getIdx(){
      return mIdx;
    }

    int mIdx;
  }


  public enum ChannelType {
    EV_DAPI("dapi",ChannelColor.BLUE), 
    EV_GFP("gfp",ChannelColor.GREEN), 
    EV_CY3("cy3",ChannelColor.RED), 
    EV_CY5("cy5",ChannelColor.MAGENTA), 
    EV_CY7("cy7",ChannelColor.YELLOW),
    CELL("cell",ChannelColor.GRAY),
    NUCLEUS("nucleus",ChannelColor.CYAN),
    NEGATIVE_CONTROL("ctrl",ChannelColor.GRAY);

    private ChannelType(String name, ChannelColor chColor){
      mName = name;
      mChColor = chColor;
    }

    public String getName(){
      return mName;
    }

    public int getColorIdx(){
      return mChColor.getIdx();
    }

    private final String mName;
    private final ChannelColor mChColor;
  }

  protected AnalyseSettings mSettings;

  private TreeMap<ChannelType,ChannelSettings> imgChannel = new TreeMap<>();


  Pipeline(AnalyseSettings settings) {
    mSettings = settings;
  }

  ///
  /// \brief Process the image
  /// \author Joachim Danmayr
  ///
  public TreeMap<Integer, Channel> ProcessImage(File imageFile) {
    String[] imageTitles = WindowManager.getImageTitles();
    imgChannel.clear();

      for(int n = 0;n<mSettings.channelSettings.size();n++){
        for (int i = 0; i < imageTitles.length; i++) {
          String actTitle = imageTitles[i];
          if (true == actTitle.endsWith(mSettings.channelSettings.get(n).mChannelName)) {
            ChannelSettings chSet = mSettings.channelSettings.get(n);
            chSet.mChannelImg = WindowManager.getImage(actTitle);
            imgChannel.put(mSettings.channelSettings.get(n).type, chSet);
          }
        }
      }
      return startPipeline(imageFile);
  }

  ChannelSettings getImageOfChannel(ChannelType type) {
    if(imgChannel.containsKey(type)){
      return imgChannel.get(type);
    }else{
      return null;
    }
  }

  ///
  ///
  ///
  TreeMap<ChannelType,ChannelSettings> getEvChannels(){
    TreeMap<ChannelType,ChannelSettings> evChannel = new TreeMap<ChannelType,ChannelSettings>();
    
    ChannelSettings ev_dapi = getImageOfChannel(ChannelType.EV_DAPI);
    if(ev_dapi!=null){
      evChannel.put(ev_dapi.type, ev_dapi);
    }
    ChannelSettings ev_gfp = getImageOfChannel(ChannelType.EV_GFP);
    if(ev_gfp!=null){
      evChannel.put(ev_gfp.type, ev_gfp);
    }
    ChannelSettings ev_cy3 = getImageOfChannel(ChannelType.EV_CY3);
    if(ev_cy3!=null){
      evChannel.put(ev_cy3.type, ev_cy3);
    }
    ChannelSettings ev_cy5 = getImageOfChannel(ChannelType.EV_CY5);
    if(ev_cy5!=null){
      evChannel.put(ev_cy5.type, ev_cy5);
    }
    ChannelSettings ev_cy7 = getImageOfChannel(ChannelType.EV_CY7);
    if(ev_cy7!=null){
      evChannel.put(ev_cy7.type, ev_cy7);
    }
    return evChannel;
  }

  ///
  ///
  ///
  ChannelSettings getImageOfChannel(int idx) {
    if(imgChannel.values().toArray().length > idx){
      return (ChannelSettings)imgChannel.values().toArray()[idx];
    }else{
      return null;
    }
  }


  public static ImagePlus preFilterSetColoc(ImagePlus img, boolean enhanceContrast, String thMethod, int thMin,
      int thMax, double[] thershold) {
    return preFilterSetColoc(img, enhanceContrast, thMethod, thMin, thMax, thershold, true);
  }

  public static ImagePlus preFilterSetColocPreview(ImagePlus img, boolean enhanceContrast, String thMethod, int thMin,
      int thMax, double[] thershold) {
    return preFilterSetColoc(img, enhanceContrast, thMethod, thMin, thMax, thershold, false);
  }

  public static ImagePlus preFilterSetColoc(ImagePlus img, boolean enhanceContrast, String thMethod, int thMin,
      int thMax, double[] thershold, boolean convertToMask) {
    Filter.Make16BitImage(img);
    if (true == enhanceContrast) {
      Filter.EnhanceContrast(img);
    }

    Filter.SubtractBackground(img);
    Filter.ApplyGaus(img);

    ImagePlus beforeThershold = Filter.duplicateImage(img);

    Filter.ApplyThershold(img, thMethod, thMin, thMax, thershold, convertToMask);
    return beforeThershold;
  }

  abstract protected TreeMap<Integer, Channel> startPipeline(File imageFile);


protected void saveControlImages(String name, Channel measCh0, Channel measCh1, Channel measCh2, ChannelType type0,ChannelType type1,ChannelType type2, RoiManager rm, Channel measColoc)
{
  if (AnalyseSettings.CotrolPicture.WithControlPicture == mSettings.mSaveDebugImages) {
    //ImagePlus[] = {"red", "green", "blue", "gray", "cyan", "magenta", "yellow"};
      ImagePlus[] imgAry = {null, null, null,null, null, null, null};
      Channel[] chAry = {null, null, null,null, null, null, null};
      String[] chNames = {null, null, null,null, null, null, null};
      
      imgAry[type0.getColorIdx()] = getImageOfChannel(0).mChannelImg;
      chAry[type0.getColorIdx()] = measCh0;
      chNames[type0.getColorIdx()] = type0.getName();

      
      imgAry[type1.getColorIdx()] = getImageOfChannel(1).mChannelImg;
      chAry[type1.getColorIdx()] = measCh1;
      chNames[type1.getColorIdx()] = type1.getName();

      //imgAry[ch2s.type.getColorIdx()] = img2;
      //chAry[ch2s.type.getColorIdx()] = measCh2;
   
    
    name = name.replace("%", "");
    name = name.replace(" ", "");
    name = name.replace(":", "");
    name = name.toLowerCase();

    String path = mSettings.mOutputFolder + java.io.File.separator + name;

    if(null != measColoc){
      ImagePlus mergedChannel = Filter.MergeChannels(imgAry);
      Filter.SaveImageWithOverlay(mergedChannel, rm, path + "_merged.jpg");
      measColoc.addControlImagePath(name + "_merged.jpg");
    }

    for(int n=0;n<imgAry.length;n++){
        if(imgAry[n] != null && chAry[n]!=null){
            String fileName = "_"+chNames[n]+".jpg";
            Filter.SaveImageWithOverlay(imgAry[n], rm, path + fileName);
            chAry[n].addControlImagePath(name + fileName);

        }
    }
    
}
}

}