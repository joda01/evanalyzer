package org.danmayr.imagej.algorithm.pipelines;

import ij.*;
import ij.process.*;
import ij.gui.*;

import java.io.File;

import ij.plugin.ZProjector;
import ij.plugin.frame.RoiManager;

import java.util.*;
import org.danmayr.imagej.algorithm.structs.*;
import org.danmayr.imagej.performance_analyzer.PerformanceAnalyzer;
import org.danmayr.imagej.algorithm.AnalyseSettings;
import org.danmayr.imagej.algorithm.ChannelSettings;
import org.danmayr.imagej.algorithm.filters.Filter;

///
/// \class  Channel
/// \brief  Channel of a picture
///
abstract public class Pipeline {
  // protected RoiManager rm = new RoiManager(false);

  // Enum which contains the color indexes for a RGBStackMerge
  // see:
  // https://imagej.nih.gov/ij/developer/source/ij/plugin/RGBStackMerge.java.html
  private enum ChannelColor {
    RED(0), GREEN(1), BLUE(2), GRAY(3), CYAN(4), MAGENTA(5), YELLOW(6);

    private ChannelColor(int idx) {
      mIdx = idx;
    }

    public int getIdx() {
      return mIdx;
    }

    int mIdx;
  }

  private static Map<Integer, ChannelType> map = new HashMap<Integer, ChannelType>();

  public enum ChannelType {
    EV_DAPI("dapi", ChannelColor.BLUE, true, 0), EV_GFP("gfp", ChannelColor.GREEN, true, 1),
    EV_CY3("cy3", ChannelColor.RED, true, 2), EV_CY5("cy5", ChannelColor.MAGENTA, true, 3),
    EV_CY7("cy7", ChannelColor.YELLOW, true, 4), CELL("cell", ChannelColor.GRAY, false, 5),
    NUCLEUS("nucleus", ChannelColor.CYAN, false, 6), NEGATIVE_CONTROL("ctrl", ChannelColor.GRAY, false, 7),
    BACKGROUND("background", ChannelColor.GRAY, false, 8), FREE_01("free_1", ChannelColor.GRAY, false, 9),
    FREE_02("free_2", ChannelColor.GRAY, false, 10), FREE_03("free_3", ChannelColor.GRAY, false, 11),
    FREE_04("free_4", ChannelColor.GRAY, false, 12), FREE_05("free_5", ChannelColor.GRAY, false, 13),
    FREE_06("free_6", ChannelColor.GRAY, false, 14), FREE_07("free_7", ChannelColor.GRAY, false, 15),
    FREE_08("free_8", ChannelColor.GRAY, false, 16), FREE_09("free_9", ChannelColor.GRAY, false, 17),
    FREE_10("free_10", ChannelColor.GRAY, false, 18), FREE_11("free_11", ChannelColor.GRAY, false, 19),
    FREE_12("free_12", ChannelColor.GRAY, false, 20), FREE_13("free_13", ChannelColor.GRAY, false, 21),
    FREE_14("free_14", ChannelColor.GRAY, false, 22), FREE_15("free_15", ChannelColor.GRAY, false, 23),
    FREE_16("free_16", ChannelColor.GRAY, false, 24), FREE_17("free_17", ChannelColor.GRAY, false, 25),
    FREE_18("free_18", ChannelColor.GRAY, false, 26), FREE_19("free_19", ChannelColor.GRAY, false, 27),
    FREE_20("free_20", ChannelColor.GRAY, false, 28), TETRASPECK_BEAD("tetraspeck_bead", ChannelColor.GRAY, false, 29),
    COLOC_ALL("coloc_all", ChannelColor.GRAY, false, 30);

    private ChannelType(String name, ChannelColor chColor, boolean evChannel, int i) {
      mName = name;
      mChColor = chColor;
      mIsEvChannel = evChannel;
      this.idx = i;
    }

    public static ChannelType getColocEnum(int idx) {
      return map.get(idx + getFirstFreeChannel());
    }

    static int getFirstFreeChannel() {
      return 9;
    }

    static {
      for (ChannelType pageType : ChannelType.values()) {
        map.put(pageType.idx, pageType);
      }
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

    int idx;
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
  public TreeMap<ChannelType, Channel> ProcessImage(File imageFile, ImagePlus[] imagesLoaded) {
    // String[] imageTitles = WindowManager.getImageTitles();
    imgChannel.clear();
    PerformanceAnalyzer.start("preprocessing");
    if (null != imagesLoaded) {
      for (int n = 0; n < mSettings.channelSettings.size(); n++) {
        ChannelSettings chSet = mSettings.channelSettings.get(n);
        if (chSet.mChannelNr >= 0 && imagesLoaded.length > chSet.mChannelNr) {
          chSet.mChannelImg = preProcessingSteps(imagesLoaded[chSet.mChannelNr], chSet);
          imgChannel.put(mSettings.channelSettings.get(n).type, chSet);
          if (true == mSettings.channelSettings.get(n).type.isEvChannel()) {
            evChannel.put(mSettings.channelSettings.get(n).type, chSet);
          }
        }
      }
    }
    PerformanceAnalyzer.stop("preprocessing");

    PerformanceAnalyzer.start("analyze_img");
    TreeMap<ChannelType, Channel> result = startPipeline(imageFile);
    PerformanceAnalyzer.stop("analyze_img");

    return result;
  }

  ///
  /// Do some preprocessing
  ///
  private ImagePlus preProcessingSteps(ImagePlus imgIn, ChannelSettings chSettings) {
    ImagePlus dup = Filter.duplicateImage(imgIn);
    IJ.run(imgIn, "Set Scale...", "distance=0 known=0 unit=pixel global");

    if (chSettings.ZProjector != "OFF") {
      return ZProjector.run(dup, chSettings.ZProjector);
    } else {
      return dup;
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
  ChannelSettings getTetraSpeckBead() {
    return getImageOfChannel(ChannelType.TETRASPECK_BEAD);
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
      AutoThresholder.Method thMethod, int thMin, int thMax, double[] thershold) {
    return preFilterSetColoc(img, background, enhanceContrast, thMethod, thMin, thMax, thershold, true);
  }

  public static ImagePlus preFilterSetColocPreview(ImagePlus img, ImagePlus background, boolean enhanceContrast,
      AutoThresholder.Method thMethod, int thMin, int thMax, double[] thershold) {
    return preFilterSetColoc(img, background, enhanceContrast, thMethod, thMin, thMax, thershold, false);
  }

  public static ImagePlus preFilterSetColoc(ImagePlus img, ImagePlus background, boolean enhanceContrast,
      AutoThresholder.Method thMethod, int thMin, int thMax, double[] thershold, boolean convertToMask) {

    PerformanceAnalyzer.start("filter_coloc");

    ImagePlus th = img;
    if (null != background) {
      th = Filter.SubtractImages(th, background);
    }

    // if (true == enhanceContrast) {
    // Filter.EnhanceContrast(th);
    // }

    Filter.SubtractBackground(th);
    //Filter.ApplyGaus(th);
    Filter.Smooth(th);
    Filter.Smooth(th);

    ImagePlus beforeThershold = Filter.duplicateImage(th);
    Filter.ApplyThershold(th, thMethod, thMin, thMax, thershold, convertToMask);
    img.setImage(th);
    img = th;
    PerformanceAnalyzer.stop("filter_coloc");
    return beforeThershold;
  }

  abstract protected TreeMap<ChannelType, Channel> startPipeline(File imageFile);

  protected String getPath(File file) {
    String name = file.getAbsolutePath().replace(java.io.File.separator, "");
    name = name.replace("%", "");
    name = name.replace(" ", "");
    name = name.replace(":", "");
    name = name.replace("^", "");
    name = name.replace("+", "");
    name = name.replace("*", "");
    name = name.replace("~", "");
    name = name.toLowerCase();

    return mSettings.mOutputFolder + java.io.File.separator + name;
  }

}