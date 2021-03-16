package org.danmayr.imagej.algorithm.pipelines;

import ij.*;
import ij.process.*;
import ij.gui.*;

import java.io.File;
import java.util.*;
import org.danmayr.imagej.algorithm.structs.*;
import org.danmayr.imagej.algorithm.AnalyseSettings;
import org.danmayr.imagej.algorithm.filters.Filter;

///
/// \class  Channel
/// \brief  Channel of a picture
///
abstract public class Pipeline {

  public enum ChannelType {
    OFF, GFP, CY3, NEGATIVE_CONTROL
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
    Pipeline.ChannelType  mCh0 = mSettings.ch0.type;
    Pipeline.ChannelType  mCh1 = mSettings.ch1.type;
    Pipeline.ChannelType  mCh2 = mSettings.ch2.type;

    if(mCh0 != ChannelType.OFF){nrOfExpectedChannels++;}
    if(mCh1 != ChannelType.OFF){nrOfExpectedChannels++;}
    if(mCh2 != ChannelType.OFF){nrOfExpectedChannels++;}

    if(1==nrOfExpectedChannels)
    {
      String chToFind="C=0";
      if(mCh0 != ChannelType.OFF){
        chToFind="C=0";
      }else if(mCh1 != ChannelType.OFF){
        chToFind="C=1";
      }else if(mCh2 != ChannelType.OFF){
        chToFind="C=2";
      }
      for (int i = 0; i < imageTitles.length; i++) {
        String actTitle = imageTitles[i];
        if (true == actTitle.endsWith(chToFind)) {
          imgChannel0 = WindowManager.getImage(actTitle);
        }
      }
    }
    else if(2==nrOfExpectedChannels){
      String ch0toFind = "C=0";
      String ch1toFind = "C=1";
      if(mCh0 != ChannelType.OFF && mCh1 != ChannelType.OFF){}
      if(mCh0 != ChannelType.OFF && mCh2 != ChannelType.OFF){ch1toFind="C=2";}
      if(mCh1 != ChannelType.OFF && mCh2 != ChannelType.OFF){ch0toFind="C=1";ch1toFind="C=2";}

      for (int i = 0; i < imageTitles.length; i++) {
        String actTitle = imageTitles[i];
        if (true == actTitle.endsWith(ch0toFind)) {
          imgChannel0 = WindowManager.getImage(actTitle);
        } else if (true == actTitle.endsWith(ch1toFind)) {
          imgChannel1 = WindowManager.getImage(actTitle);
        }
      }
    }
    else if(3==nrOfExpectedChannels){
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
    }
    else {
      return startPipeline(imageFile);
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

  abstract protected TreeMap<Integer, Channel> startPipeline(File imageFile);

}