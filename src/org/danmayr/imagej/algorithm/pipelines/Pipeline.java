package org.danmayr.imagej.algorithm.pipelines;

import ij.*;
import ij.process.*;
import ij.gui.*;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import ij.plugin.ZProjector;
import ij.plugin.frame.RoiManager;

import java.util.*;
import org.danmayr.imagej.algorithm.structs.*;
import org.danmayr.imagej.algorithm.structs.Image;
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

  public final int PARALLEL_WORKERS;

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
    FREE_21("free_21", Color.gray, false, false, 21 + 9),
    FREE_22("free_22", Color.gray, false, false, 22 + 9),
    FREE_23("free_23", Color.gray, false, false, 23 + 9),
    FREE_24("free_24", Color.gray, false, false, 24 + 9),
    FREE_25("free_25", Color.gray, false, false, 25 + 9),
    FREE_26("free_26", Color.gray, false, false, 26 + 9),
    FREE_27("free_27", Color.gray, false, false, 27 + 9),
    FREE_28("free_28", Color.gray, false, false, 28 + 9),
    FREE_29("free_29", Color.gray, false, false, 29 + 9),
    FREE_30("free_30", Color.gray, false, false, 30 + 9),
    FREE_31("free_31", Color.gray, false, false, 31 + 9),
    FREE_32("free_32", Color.gray, false, false, 32 + 9),
    FREE_33("free_33", Color.gray, false, false, 33 + 9),
    FREE_34("free_34", Color.gray, false, false, 34 + 9),
    FREE_35("free_35", Color.gray, false, false, 35 + 9),
    FREE_36("free_36", Color.gray, false, false, 36 + 9),
    FREE_37("free_37", Color.gray, false, false, 37 + 9),
    FREE_38("free_38", Color.gray, false, false, 38 + 9),
    FREE_39("free_39", Color.gray, false, false, 39 + 9),
    FREE_40("free_40", Color.gray, false, false, 40 + 9),
    FREE_41("free_41", Color.gray, false, false, 41 + 9),
    FREE_42("free_42", Color.gray, false, false, 42 + 9),
    FREE_43("free_43", Color.gray, false, false, 43 + 9),
    FREE_44("free_44", Color.gray, false, false, 44 + 9),
    FREE_45("free_45", Color.gray, false, false, 45 + 9),
    FREE_46("free_46", Color.gray, false, false, 46 + 9),
    FREE_47("free_47", Color.gray, false, false, 47 + 9),
    FREE_48("free_48", Color.gray, false, false, 48 + 9),
    FREE_49("free_49", Color.gray, false, false, 49 + 9),
    FREE_50("free_50", Color.gray, false, false, 50 + 9),
    FREE_51("free_51", Color.gray, false, false, 51 + 9),
    FREE_52("free_52", Color.gray, false, false, 52 + 9),
    FREE_53("free_53", Color.gray, false, false, 53 + 9),
    FREE_54("free_54", Color.gray, false, false, 54 + 9),
    FREE_55("free_55", Color.gray, false, false, 55 + 9),
    FREE_56("free_56", Color.gray, false, false, 56 + 9),
    FREE_57("free_10", Color.gray, false, false, 57 + 9),
    FREE_58("free_11", Color.gray, false, false, 58 + 9),
    FREE_59("free_12", Color.gray, false, false, 59 + 9),
    FREE_60("free_13", Color.gray, false, false, 60 + 9),
    FREE_61("free_14", Color.gray, false, false, 61 + 9),
    FREE_62("free_15", Color.gray, false, false, 62 + 9),
    FREE_63("free_16", Color.gray, false, false, 63 + 9),
    FREE_64("free_17", Color.gray, false, false, 64 + 9),
    FREE_65("free_18", Color.gray, false, false, 65 + 9),
    FREE_66("free_19", Color.gray, false, false, 66 + 9),
    FREE_67("free_20", Color.gray, false, false, 67 + 9),
    FREE_68("free_21", Color.gray, false, false, 68 + 9),
    FREE_69("free_22", Color.gray, false, false, 69 + 9),
    FREE_70("free_23", Color.gray, false, false, 70 + 9),
    FREE_71("free_24", Color.gray, false, false, 71 + 9),
    FREE_72("free_25", Color.gray, false, false, 72 + 9),
    FREE_73("free_26", Color.gray, false, false, 73 + 9),
    FREE_74("free_27", Color.gray, false, false, 74 + 9),
    FREE_75("free_28", Color.gray, false, false, 75 + 9),
    FREE_76("free_29", Color.gray, false, false, 76 + 9),
    FREE_77("free_30", Color.gray, false, false, 77 + 9),
    FREE_78("free_31", Color.gray, false, false, 78 + 9),
    FREE_79("free_32", Color.gray, false, false, 79 + 9),
    FREE_80("free_32", Color.gray, false, false, 80 + 9),
    FREE_81("free_33", Color.gray, false, false, 81 + 9),
    FREE_82("free_34", Color.gray, false, false, 82 + 9),
    FREE_83("free_35", Color.gray, false, false, 83 + 9),
    FREE_84("free_36", Color.gray, false, false, 84 + 9),
    FREE_85("free_37", Color.gray, false, false, 85 + 9),
    FREE_86("free_38", Color.gray, false, false, 86 + 9),
    FREE_87("free_39", Color.gray, false, false, 87 + 9),
    FREE_88("free_40", Color.gray, false, false, 88 + 9),
    FREE_89("free_41", Color.gray, false, false, 89 + 9),
    FREE_90("free_42", Color.gray, false, false, 90 + 9),
    FREE_91("free_43", Color.gray, false, false, 91 + 9),
    FREE_92("free_44", Color.gray, false, false, 92 + 9),
    FREE_93("free_45", Color.gray, false, false, 93 + 9),
    FREE_94("free_46", Color.gray, false, false, 94 + 9),
    FREE_95("free_47", Color.gray, false, false, 95 + 9),
    FREE_96("free_48", Color.gray, false, false, 96 + 9),
    FREE_97("free_49", Color.gray, false, false, 97 + 9),
    FREE_98("free_50", Color.gray, false, false, 98 + 9),
    FREE_99("free_50", Color.gray, false, false, 99 + 9),
    TETRASPECK_BEAD("tetraspeck_bead", Color.gray, false, false, 100 + 9),
    COLOC_ALL("coloc_all", Color.gray, false, false, 101 + 9),
    CELL_FLUORESCENCE("cell_fluorescence", Color.gray, false, true, 102 + 9),;

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

  protected final AnalyseSettings mSettings;
  protected String mUUID = "";

  private TreeMap<ChannelType, ChannelSettings> imgChannel = new TreeMap<>();
  TreeMap<ChannelType, ChannelSettings> evChannel = new TreeMap<ChannelType, ChannelSettings>();
  ChannelSettings cellChannel;

  Pipeline(AnalyseSettings settings, int parallelWorkers) {
    PARALLEL_WORKERS = parallelWorkers;
    mSettings = settings;
    cellChannel = new ChannelSettings(settings);
  }

  ///
  /// \brief Process the image
  /// \author Joachim Danmayr
  ///
  public Image ProcessImage(File imageFile, ImagePlus[] imagesLoaded) {
    // UUID uuid = UUID.randomUUID();
    // mUUID = uuid.toString();
    mUUID = removeSpecialCharacters(imageFile.getName());
    // String[] imageTitles = WindowManager.getImageTitles();

    File path = new File(getPath(imageFile));
    int cnt = 0;
    // If the filename still exists add a number at the end to make it unique
    while (path.exists()) {
      mUUID = removeSpecialCharacters(imageFile.getName()) + "__" + Integer.toString(cnt);
      path = new File(getPath(imageFile));
      cnt++;
    }
    if (path != null && !path.exists()) {
      path.mkdirs();
    }

    imgChannel.clear();
    PerformanceAnalyzer.start("preprocessing");
    if (null != imagesLoaded) {
      for (int n = 0; n < mSettings.getNrOfChannelSettings(); n++) {
        try {
          ChannelSettings chSet = mSettings.getChannelSettings(n).clone();

          if (chSet.getChNr() >= 0 && imagesLoaded.length > chSet.getChNr()) {
            chSet.setImg(preProcessingSteps(imagesLoaded[chSet.getChNr()], chSet));
            imgChannel.put(mSettings.getChannelSettings(n).getType(), chSet);
            if (true == mSettings.getChannelSettings(n).getType().isEvChannel()) {
              evChannel.put(mSettings.getChannelSettings(n).getType(), chSet);
            } else if (true == mSettings.getChannelSettings(n).getType().isCellChannel()) {
              cellChannel = chSet;
            }
          }
        } catch (CloneNotSupportedException ex) {
          IJ.log("Cannot create channel settings " + ex);
        }
      }
    }
    PerformanceAnalyzer.stop("preprocessing");

    PerformanceAnalyzer.start("analyze_img");
    TreeMap<ChannelType, Channel> result = startPipeline(imageFile);
    PerformanceAnalyzer.stop("analyze_img");
    Image im = new Image(imageFile.getName(), getUUID());
    im.addChannel(result);
    return im;
  }

  public String getUUID() {
    return mUUID;
  }

  ///
  /// Do some preprocessing
  ///
  private ImagePlus preProcessingSteps(ImagePlus imgIn, ChannelSettings chSettings) {
    ImagePlus dup = Filter.duplicateImage(imgIn);
    IJ.run(imgIn, "Set Scale...", "distance=0 known=0 unit=pixel global");
    Prefs.blackBackground = true;

    if (chSettings.getZProjectionSetting() != "OFF") {
      dup = ZProjector.run(dup, chSettings.getZProjectionSetting());
    }

    // Crop Image
    if (chSettings.getMarginCropPixel() > 0) {
      Filter.cropMarginOfImage(chSettings.getMarginCropPixel(), dup);
    }

    // Preprocessing
    for (int n = 0; n < chSettings.getProcessingStep().size(); n++) {
      ChannelSettings.PreProcessingStep preProcess = chSettings.getProcessingStep().get(n);
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
    if (null != backgroundOriginal) {
      th = Filter.SubtractImages(th, backgroundOriginal);
    }

    // if (true == enhanceContrast) {
    // Filter.EnhanceContrast(th);
    // }

    Filter.RollingBall(th);
    // Filter.ApplyGaus(th);
    Filter.Smooth(th);
    Filter.Smooth(th);

    ImagePlus beforeThershold = Filter.duplicateImage(th);
    Filter.ApplyThershold(th, thMethod, thMin, thMax, thershold, convertToMask);
    img.setImage(th);
    img = th;
    return beforeThershold;
  }

  abstract protected TreeMap<ChannelType, Channel> startPipeline(File imageFile);

  protected String getPath(File file) {
    return mSettings.getOutputFolder() + java.io.File.separator + getUUID();
  }

  protected String getPath(File file, String fileNamePrefix, String fileNameSufix) {
    String path = mSettings.getOutputFolder() + java.io.File.separator + getUUID() + java.io.File.separator
        + getName(file, fileNamePrefix, fileNameSufix);

    return path;
  }

  protected String getRelativeImagePath(File file, String fileNamePrefix, String fileNameSufix) {
    return getUUID() + java.io.File.separator + getName(file, fileNamePrefix, fileNameSufix);
  }

  protected String getName(File file, String fileNamePrefix, String fileNameSufix) {
    String name = file.getName().replace(java.io.File.separator, "") + "__" + fileNamePrefix + "__" + fileNameSufix;
    name = name.replace("%", "");
    name = name.replace(" ", "");
    name = name.replace(":", "");
    name = name.replace("^", "");
    name = name.replace("+", "");
    name = name.replace("*", "");
    name = name.replace("~", "");
    name = name.replace(".", "_");
    name = name.toLowerCase();
    name = name + ".jpg";

    return name;
  }

  protected String removeSpecialCharacters(String name) {
    name = name.replace("/", "");
    name = name.replace("\\", "");
    name = name.replace("ö", "oe");
    name = name.replace("ä", "ae");
    name = name.replace("ü", "ue");
    name = name.replace("?", "");
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