package org.danmayr.imagej.algorithm.pipelines;

import ij.*;
import ij.process.*;
import ij.gui.*;

import java.io.File;
import ij.plugin.frame.RoiManager;

import java.util.*;
import org.danmayr.imagej.algorithm.structs.*;
import org.danmayr.imagej.algorithm.AnalyseSettings;
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
    OFF("off",ChannelColor.GRAY), 
    DAPI("dapi",ChannelColor.BLUE), 
    GFP("gfp",ChannelColor.GREEN), 
    CY3("cy3",ChannelColor.RED), 
    CY5("cy5",ChannelColor.MAGENTA), 
    CY7("cy7",ChannelColor.YELLOW), 
    NEGATIVE_CONTROL("ctrl",ChannelColor.CYAN);

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

  private ImagePlus imgChannel0;
  private ImagePlus imgChannel1;
  private ImagePlus imgChannel2;

  Pipeline(AnalyseSettings settings) {
    mSettings = settings;
  }

  ///
  /// \brief Process the image
  /// \author Joachim Danmayr
  ///
  public TreeMap<Integer, Channel> ProcessImage(File imageFile) throws Exception {
    String[] imageTitles = WindowManager.getImageTitles();

    int nrOfExpectedChannels = 0;
    Pipeline.ChannelType mCh0 = mSettings.ch0.type;
    Pipeline.ChannelType mCh1 = mSettings.ch1.type;
    Pipeline.ChannelType mCh2 = mSettings.ch2.type;

    if (mCh0 != ChannelType.OFF) {
      nrOfExpectedChannels++;
    }
    if (mCh1 != ChannelType.OFF) {
      nrOfExpectedChannels++;
    }
    if (mCh2 != ChannelType.OFF) {
      nrOfExpectedChannels++;
    }

    AnalyseSettings.ChannelSettings ch0s = mSettings.ch0;
    AnalyseSettings.ChannelSettings ch1s = mSettings.ch1;

    if (1 == nrOfExpectedChannels) {
      String chToFind = "C=0";
      if (mCh0 != ChannelType.OFF) {
        chToFind = "C=0";
        ch0s = mSettings.ch0;
      } else if (mCh1 != ChannelType.OFF) {
        chToFind = "C=1";
        ch0s = mSettings.ch1;
      } else if (mCh2 != ChannelType.OFF) {
        chToFind = "C=2";
        ch0s = mSettings.ch2;
      }
      for (int i = 0; i < imageTitles.length; i++) {
        String actTitle = imageTitles[i];
        if (true == actTitle.endsWith(chToFind)) {
          imgChannel0 = WindowManager.getImage(actTitle);
        }
      }
    } else if (2 == nrOfExpectedChannels) {
      String ch0toFind = "C=0";
      String ch1toFind = "C=1";
      if (mCh0 != ChannelType.OFF && mCh1 != ChannelType.OFF) {
      }
      if (mCh0 != ChannelType.OFF && mCh2 != ChannelType.OFF) {
        ch1toFind = "C=2";
        ch1s = mSettings.ch2;
      }
      if (mCh1 != ChannelType.OFF && mCh2 != ChannelType.OFF) {
        ch0toFind = "C=1";
        ch1toFind = "C=2";
        ch0s = mSettings.ch1;
        ch1s = mSettings.ch2;
      }

      for (int i = 0; i < imageTitles.length; i++) {
        String actTitle = imageTitles[i];
        if (true == actTitle.endsWith(ch0toFind)) {
          imgChannel0 = WindowManager.getImage(actTitle);
        } else if (true == actTitle.endsWith(ch1toFind)) {
          imgChannel1 = WindowManager.getImage(actTitle);
        }
      }
    } else if (3 == nrOfExpectedChannels) {
      for (int i = 0; i < imageTitles.length; i++) {
        String actTitle = imageTitles[i];
        if (true == actTitle.endsWith("C=0")) {
          imgChannel0 = WindowManager.getImage(actTitle);
        } else if (true == actTitle.endsWith("C=1")) {
          imgChannel1 = WindowManager.getImage(actTitle);
        } else if (true == actTitle.endsWith("C=2")) {
          imgChannel2 = WindowManager.getImage(actTitle);
        }
      }
    }

    if (1 == nrOfExpectedChannels && null == getImageCh0()) {
      throw new Exception("One channel expected, zero channels given.");
    } else if (2 == nrOfExpectedChannels && (null == getImageCh0() || null == getImageCh1())) {
      throw new Exception("Two channel expected but just one or zero given.");
    } else if (3 == nrOfExpectedChannels && (null == getImageCh0() || null == getImageCh1() || null == getImageCh2())) {
      throw new Exception("Three channel expected but just two, one or zero given.");
    } else {
      return startPipeline(imageFile, ch0s, ch1s);
    }
    // return new TreeMap<Integer, Channel>();
  }

  ImagePlus getImageCh0() {
    return imgChannel0;
  }

  ImagePlus getImageCh1() {
    return imgChannel1;
  }

  ImagePlus getImageCh2() {
    return imgChannel2;
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

  abstract protected TreeMap<Integer, Channel> startPipeline(File imageFile, AnalyseSettings.ChannelSettings ch0s,
      AnalyseSettings.ChannelSettings ch1s);


protected void saveControlImages(String name, Channel measCh0, Channel measCh1, Channel measCh2, ChannelType type0,ChannelType type1,ChannelType type2, RoiManager rm, Channel measColoc)
{
  if (AnalyseSettings.CotrolPicture.WithControlPicture == mSettings.mSaveDebugImages) {
    //ImagePlus[] = {"red", "green", "blue", "gray", "cyan", "magenta", "yellow"};
      ImagePlus[] imgAry = {null, null, null,null, null, null, null};
      Channel[] chAry = {null, null, null,null, null, null, null};
      String[] chNames = {null, null, null,null, null, null, null};
      
      imgAry[type0.getColorIdx()] = getImageCh0();
      chAry[type0.getColorIdx()] = measCh0;
      chNames[type0.getColorIdx()] = type0.getName();

      
      imgAry[type1.getColorIdx()] = getImageCh1();
      chAry[type1.getColorIdx()] = measCh1;
      chNames[type1.getColorIdx()] = type1.getName();

      //imgAry[ch2s.type.getColorIdx()] = img2;
      //chAry[ch2s.type.getColorIdx()] = measCh2;
   
    
    name = name.replaceAll("%", "");
    name = name.replaceAll(" ", "");
    name = name.replaceAll(":", "");
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