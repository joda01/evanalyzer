package org.danmayr.imagej.algorithm.pipelines;

import ij.*;
import ij.process.*;
import ij.gui.*;

import java.io.File;

import ij.plugin.ZProjector;
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
  // see:
  // https://imagej.nih.gov/ij/developer/source/ij/plugin/RGBStackMerge.java.html
  private enum ChannelColor {
    RED(0), GREEN(1), BLUE(2), GRAY(3), CYAN(4), MAGENTA(5), YELLOW(7);

    private ChannelColor(int idx) {
      mIdx = idx;
    }

    public int getIdx() {
      return mIdx;
    }

    int mIdx;
  }

  public enum ChannelType {
    EV_DAPI("dapi", ChannelColor.BLUE, true), EV_GFP("gfp", ChannelColor.GREEN, true),
    EV_CY3("cy3", ChannelColor.RED, true), EV_CY5("cy5", ChannelColor.MAGENTA, true),
    EV_CY7("cy7", ChannelColor.YELLOW, true), CELL("cell", ChannelColor.GRAY, false),
    NUCLEUS("nucleus", ChannelColor.CYAN, false), NEGATIVE_CONTROL("ctrl", ChannelColor.GRAY, false),
    BACKGROUND("background", ChannelColor.GRAY, false);
    ;

    private ChannelType(String name, ChannelColor chColor, boolean evChannel) {
      mName = name;
      mChColor = chColor;
      mIsEvChannel = evChannel;
    }

    public String getName() {
      return mName;
    }

    public int getColorIdx() {
      return mChColor.getIdx();
    }

    public boolean isEvChannel() {
      return mIsEvChannel;
    }

    private final String mName;
    private final ChannelColor mChColor;
    private final boolean mIsEvChannel;
  }

  protected AnalyseSettings mSettings;

  private TreeMap<ChannelType, ChannelSettings> imgChannel = new TreeMap<>();
  TreeMap<ChannelType, ChannelSettings> evChannel = new TreeMap<ChannelType, ChannelSettings>();

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

    for (int n = 0; n < mSettings.channelSettings.size(); n++) {
      for (int i = 0; i < imageTitles.length; i++) {
        String actTitle = imageTitles[i];
        if (true == actTitle.endsWith(mSettings.channelSettings.get(n).mChannelName)) {
          ChannelSettings chSet = mSettings.channelSettings.get(n);
          chSet.mChannelImg = preProcessingSteps(WindowManager.getImage(actTitle),chSet);
          imgChannel.put(mSettings.channelSettings.get(n).type, chSet);
          if (true == mSettings.channelSettings.get(n).type.isEvChannel()) {
            evChannel.put(mSettings.channelSettings.get(n).type, chSet);
          }
        }
      }
    }

    return startPipeline(imageFile);
  }


  ///
  /// Do some preprocessing
  ///
  private ImagePlus preProcessingSteps(ImagePlus imgIn,ChannelSettings chSettings){
    if(chSettings.ZProjector != "OFF"){
      return ZProjector.run(imgIn, chSettings.ZProjector);
    }else{
      return imgIn;
    }
  }

  ChannelSettings getImageOfChannel(ChannelType type) {
    if (imgChannel.containsKey(type)) {
      return imgChannel.get(type);
    } else {
      return null;
    }
  }

  ///
  ///
  ///
  TreeMap<ChannelType, ChannelSettings> getEvChannels() {
    return evChannel;
  }

  ///
  ///
  ///
  ChannelSettings getBackground() {
    return getImageOfChannel(ChannelType.BACKGROUND);
  }

  ///
  ///
  ///
  ChannelSettings getImageOfChannel(int idx) {
    if (imgChannel.values().toArray().length > idx) {
      return (ChannelSettings) imgChannel.values().toArray()[idx];
    } else {
      return null;
    }
  }

  public static ImagePlus preFilterSetColoc(ImagePlus img, ImagePlus background, boolean enhanceContrast,
      String thMethod, int thMin, int thMax, double[] thershold) {
    return preFilterSetColoc(img, background, enhanceContrast, thMethod, thMin, thMax, thershold, true);
  }

  public static ImagePlus preFilterSetColocPreview(ImagePlus img, ImagePlus background, boolean enhanceContrast,
      String thMethod, int thMin, int thMax, double[] thershold) {
    return preFilterSetColoc(img, background, enhanceContrast, thMethod, thMin, thMax, thershold, false);
  }

  public static ImagePlus preFilterSetColoc(ImagePlus img, ImagePlus background, boolean enhanceContrast,
      String thMethod, int thMin, int thMax, double[] thershold, boolean convertToMask) {

    ImagePlus th = img;
    if (null != background) {
      th = Filter.SubtractImages(th, background);
    }

    if (true == enhanceContrast) {
      Filter.EnhanceContrast(th);
    }

    Filter.SubtractBackground(th);
    Filter.ApplyGaus(th);

    ImagePlus beforeThershold = Filter.duplicateImage(th);

    Filter.ApplyThershold(th, thMethod, thMin, thMax, thershold, convertToMask);
    img.setImage(th);
    return beforeThershold;
  }

  abstract protected TreeMap<Integer, Channel> startPipeline(File imageFile);

  protected void saveControlImages(String name, ImagePlus img0, ImagePlus img1, ImagePlus img2, Channel measCh0,
      Channel measCh1, Channel measCh2, ChannelType type0, ChannelType type1, ChannelType type2, RoiManager rm,
      Channel measColoc) {
    if (AnalyseSettings.CotrolPicture.WithControlPicture == mSettings.mSaveDebugImages) {
      // ImagePlus[] = {"red", "green", "blue", "gray", "cyan", "magenta", "yellow"};
      ImagePlus[] imgAry = { null, null, null, null, null, null, null };
      Channel[] chAry = { null, null, null, null, null, null, null };
      String[] chNames = { null, null, null, null, null, null, null };

      imgAry[type0.getColorIdx()] = img0;
      chAry[type0.getColorIdx()] = measCh0;
      chNames[type0.getColorIdx()] = type0.getName();

      if (type1 != null) {
        imgAry[type1.getColorIdx()] = img1;
        chAry[type1.getColorIdx()] = measCh1;
        chNames[type1.getColorIdx()] = type1.getName();
      }

      if (type2 != null) {
        imgAry[type2.getColorIdx()] = img2;
        chAry[type2.getColorIdx()] = measCh2;
        chNames[type2.getColorIdx()] = type2.getName();
      }
      // imgAry[ch2s.type.getColorIdx()] = img2;
      // chAry[ch2s.type.getColorIdx()] = measCh2;

      name = name.replace("%", "");
      name = name.replace(" ", "");
      name = name.replace(":", "");
      name = name.toLowerCase();

      String path = mSettings.mOutputFolder + java.io.File.separator + name;

      if (null != measColoc) {
        ImagePlus mergedChannel = Filter.MergeChannels(imgAry);
        Filter.SaveImage(mergedChannel, path + "_merged.jpg", rm);
        measColoc.addControlImagePath(name + "_merged.jpg");
      }

      for (int n = 0; n < imgAry.length; n++) {
        if (imgAry[n] != null && chAry[n] != null) {
          String fileName = "_" + chNames[n] + ".jpg";
          Filter.SaveImage(imgAry[n], path + fileName, rm);
          chAry[n].addControlImagePath(name + fileName);

        }
      }

    }
  }

  protected String getPath(File file) {
    String name = file.getName();
    name = name.replace("%", "");
    name = name.replace(" ", "");
    name = name.replace(":", "");
    name = name.toLowerCase();

    return mSettings.mOutputFolder + java.io.File.separator + name;
  }

}