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
import java.awt.*;

///
/// \class  Channel
/// \brief  Channel of a picture
///
abstract public class Pipeline {
  // protected RoiManager rm = new RoiManager(false);

  // Enum which contains the color indexes for a RGBStackMerge
  // see:
  // https://imagej.nih.gov/ij/developer/source/ij/plugin/RGBStackMerge.java.html

  private static Map<Integer, ChannelType> map = new HashMap<Integer, ChannelType>();

  public enum ChannelType {
    EV_DAPI("dapi", Color.blue, true, false, 0),
    EV_GFP("gfp", Color.green, true, false, 1),
    EV_CY3("cy3", Color.yellow, true, false, 2),
    EV_CY5("cy5", Color.red, true, false, 3),
    EV_CY7("cy7", Color.magenta, true, false, 4),
    EV_CY3FCY5("cy3fcy5", Color.yellow, true, false, 5),
    CELL_BRIGHTFIELD("cell_brightfield", Color.gray, false, true, 6),
    NUCLEUS("nucleus", Color.cyan, false, false, 7),
    NEGATIVE_CONTROL("ctrl", Color.gray, false, false, 8),
    BACKGROUND("background", Color.gray, false, false, 9),
    FREE_01("free_1", Color.gray, false, false, 1 + 9),
    FREE_02("free_2", Color.gray, false, false, 2 + 9),
    FREE_03("free_3", Color.gray, false, false, 3 + 9),
    FREE_04("free_4", Color.gray, false, false, 4 + 9),
    FREE_05("free_5", Color.gray, false, false, 5 + 9),
    FREE_06("free_6", Color.gray, false, false, 6 + 9),
    FREE_07("free_7", Color.gray, false, false, 7 + 9),
    FREE_08("free_8", Color.gray, false, false, 8 + 9),
    FREE_09("free_9", Color.gray, false, false, 9 + 9),
    FREE_10("free_10", Color.gray, false, false, 10 + 9),
    FREE_11("free_11", Color.gray, false, false, 11 + 9),
    FREE_12("free_12", Color.gray, false, false, 12 + 9),
    FREE_13("free_13", Color.gray, false, false, 13 + 9),
    FREE_14("free_14", Color.gray, false, false, 14 + 9),
    FREE_15("free_15", Color.gray, false, false, 15 + 9),
    FREE_16("free_16", Color.gray, false, false, 16 + 9),
    FREE_17("free_17", Color.gray, false, false, 17 + 9),
    FREE_18("free_18", Color.gray, false, false, 18 + 9),
    FREE_19("free_19", Color.gray, false, false, 19 + 9),
    FREE_20("free_20", Color.gray, false, false, 20 + 9),
    FREE_21("free_20", Color.gray, false, false, 21 + 9),
    FREE_22("free_20", Color.gray, false, false, 22 + 9),
    FREE_23("free_20", Color.gray, false, false, 23 + 9),
    FREE_24("free_20", Color.gray, false, false, 24 + 9),
    FREE_25("free_20", Color.gray, false, false, 25 + 9),
    FREE_26("free_20", Color.gray, false, false, 26 + 9),
    FREE_27("free_20", Color.gray, false, false, 27 + 9),
    FREE_28("free_20", Color.gray, false, false, 28 + 9),
    FREE_29("free_20", Color.gray, false, false, 29 + 9),
    FREE_30("free_20", Color.gray, false, false, 30 + 9),
    FREE_31("free_20", Color.gray, false, false, 31 + 9),
    FREE_32("free_20", Color.gray, false, false, 32 + 9),
    FREE_33("free_20", Color.gray, false, false, 33 + 9),
    FREE_34("free_20", Color.gray, false, false, 34 + 9),
    FREE_35("free_20", Color.gray, false, false, 35 + 9),
    FREE_36("free_20", Color.gray, false, false, 36 + 9),
    FREE_37("free_20", Color.gray, false, false, 37 + 9),
    FREE_38("free_20", Color.gray, false, false, 38 + 9),
    FREE_39("free_20", Color.gray, false, false, 39 + 9),
    FREE_40("free_20", Color.gray, false, false, 40 + 9),
    FREE_41("free_20", Color.gray, false, false, 41 + 9),
    FREE_42("free_20", Color.gray, false, false, 42 + 9),
    TETRASPECK_BEAD("tetraspeck_bead", Color.gray, false, false, 43 + 9),
    COLOC_ALL("coloc_all", Color.gray, false, false, 44 + 9),
    CELL_FLUORESCENCE("cell_fluorescence", Color.gray, false, true, 45 + 9),;

    private ChannelType(String name, Color chColor, boolean evChannel, boolean cellChannel, int i) {
      mName = name;
      mColor = chColor;
      mIsEvChannel = evChannel;
      mIsCellChannel = cellChannel;
      this.idx = i;
    }

    public static ChannelType getColocEnum(int idx) {
      return map.get(idx + getFirstFreeChannel());
    }

    static int getFirstFreeChannel() {
      return 10;
    }

    static {
      for (ChannelType pageType : ChannelType.values()) {
        map.put(pageType.idx, pageType);
      }
    }

    public String getName() {
      return mName;
    }

    public Color getColor() {
      return mColor;
    }

    public boolean isEvChannel() {
      return mIsEvChannel;
    }

    public boolean isCellChannel() {
      return mIsCellChannel;
    }

    int idx;
    private final String mName;
    private final Color mColor;
    private final boolean mIsEvChannel;
    private final boolean mIsCellChannel;
  }

  protected static AnalyseSettings mSettings;

  private TreeMap<ChannelType, ChannelSettings> imgChannel = new TreeMap<>();
  TreeMap<ChannelType, ChannelSettings> evChannel = new TreeMap<ChannelType, ChannelSettings>();
  ChannelSettings cellChannel;

  Pipeline(AnalyseSettings settings) {
    mSettings = settings;
    cellChannel = new ChannelSettings(settings);
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
          } else if (true == mSettings.channelSettings.get(n).type.isCellChannel()) {
            cellChannel = chSet;
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
    Prefs.blackBackground = true;

    if (chSettings.ZProjector != "OFF") {
      dup = ZProjector.run(dup, chSettings.ZProjector);
    }

    // Crop Image
    if (chSettings.getMarginCropPixel() > 0) {
      Filter.cropMarginOfImage(chSettings.getMarginCropPixel(), dup);
    }

    // Preprocessing
    for (int n = 0; n < chSettings.preProcessing.size(); n++) {
      ChannelSettings.PreProcessingStep preProcess = chSettings.preProcessing.get(n);
      if (preProcess == ChannelSettings.PreProcessingStep.EdgeDetection) {
        Filter.FindEdges(dup);
      }
      if (preProcess == ChannelSettings.PreProcessingStep.EnhanceContrast) {
        Filter.EnhanceContrast(dup);
      }

    }
    return dup;
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
  ChannelSettings getCellChannel() {
    return cellChannel;
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

  public static ImagePlus preFilterSetColoc(File file, ImagePlus img, ImagePlus background, boolean enhanceContrast,
      AutoThresholder.Method thMethod, int thMin, int thMax, double[] thershold) {
    return preFilterSetColoc(file, img, background, enhanceContrast, thMethod, thMin, thMax, thershold, true);
  }

  public static ImagePlus preFilterSetColocPreview(File file, ImagePlus img, ImagePlus background,
      boolean enhanceContrast,
      AutoThresholder.Method thMethod, int thMin, int thMax, double[] thershold) {
    return preFilterSetColoc(file, img, background, enhanceContrast, thMethod, thMin, thMax, thershold, false);
  }

  public static ImagePlus preFilterSetColoc(File file, ImagePlus img, ImagePlus backgroundOriginal,
      boolean enhanceContrast,
      AutoThresholder.Method thMethod, int thMin, int thMax, double[] thershold, boolean convertToMask) {

    ImagePlus th = img;

    Filter.Smooth(th);
    Filter.Smooth(th);
    Filter.RollingBall(th);


    if (null != backgroundOriginal) {
      ImagePlus background = Filter.duplicateImage(backgroundOriginal);

      if (null != file) {
        String imagePath = getPath(file) + "_original.jpg";
        Filter.SaveImageWithOverlayFromChannel(th, null, imagePath);
      }

      int[] minMax = Filter.getMinMax(th);
     // int[] minMaxTh = Filter.getMinMax(th);


      //Filter.NormalizeHistogram(background,minMax);
      //int[] lutTh = Filter.NormalizeHistogram(th,minMax);
      Filter.RollingBall(background);

      th = Filter.SubtractImages(th, background);

      Filter.Smooth(background);
      Filter.Smooth(background);

      //Filter.ApplyLUT(background, lutBack);
      //Filter.ApplyLUT(th, lutTh);

      if (null != file) {
        String imagePath = getPath(file) + "_transformed.jpg";
        Filter.SaveImageWithOverlayFromChannel(th, null, imagePath);
      }

    }

    // Filter.ApplyGaus(th);

    ImagePlus beforeThershold = Filter.duplicateImage(th);
    Filter.ApplyThershold(th, thMethod, thMin, thMax, thershold, convertToMask);
    img.setImage(th);
    img = th;
    return beforeThershold;
  }

  abstract protected TreeMap<ChannelType, Channel> startPipeline(File imageFile);

  protected static String getPath(File file) {
    String name = file.getAbsolutePath().replace(java.io.File.separator, "");
    name = name.replace("%", "");
    name = name.replace(" ", "");
    name = name.replace(":", "");
    name = name.replace("^", "");
    name = name.replace("+", "");
    name = name.replace("*", "");
    name = name.replace("~", "");
    name = name.replace(".", "_");
    name = name.toLowerCase();

    return mSettings.mOutputFolder + java.io.File.separator + getName(file);
  }

  protected static String getName(File file) {
    String name = file.getAbsolutePath().replace(java.io.File.separator, "");
    name = name.replace("%", "");
    name = name.replace(" ", "");
    name = name.replace(":", "");
    name = name.replace("^", "");
    name = name.replace("+", "");
    name = name.replace("*", "");
    name = name.replace("~", "");
    name = name.replace(".", "_");
    name = name.toLowerCase();

    return name;
  }

}