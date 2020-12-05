package org.danmayr.imagej.algorithm.pipelines;

import ij.*;
import ij.process.*;
import ij.gui.*;

import java.io.File;
import java.util.*;
import org.danmayr.imagej.algorithm.structs.*;
import org.danmayr.imagej.algorithm.AnalyseSettings;


///
/// \class  Channel
/// \brief  Channel of a picture
///
abstract public class Pipeline {

  public enum ChannelType {
    OFF, GFP, CY3
  }

  protected ChannelType mCh0;
  protected ChannelType mCh1;
  protected AnalyseSettings mSettings;

  private ImagePlus imgChannel0;
  private ImagePlus imgChannel1;
  private ImagePlus imgChannel2;

  private int nrOfExpectedChannels = 0;

  Pipeline(AnalyseSettings settings, ChannelType ch0) {
    mCh0 = ch0;
    mCh1 = ChannelType.OFF;
    nrOfExpectedChannels = 1;
    mSettings = settings;
  }

  Pipeline(AnalyseSettings settings, ChannelType ch0, ChannelType ch1) {
    mCh0 = ch0;
    mCh1 = ch1;
    nrOfExpectedChannels = 2;
    mSettings = settings;
  }

  ///
  /// \brief Process the image
  /// \author Joachim Danmayr
  ///
  public TreeMap<Integer, Channel> ProcessImage(File imageFile) throws Exception
  {
    String[] imageTitles = WindowManager.getImageTitles();

    for (int i = 0; i < imageTitles.length; i++) {
      String actTitle = imageTitles[i];
      if (true == actTitle.endsWith("C=1")) {
        imgChannel0 = WindowManager.getImage(actTitle);
      } else if (true == actTitle.endsWith("C=2")) {
        imgChannel1 = WindowManager.getImage(actTitle);
      } else if (true == actTitle.endsWith("C=3")) {
        imgChannel2 = WindowManager.getImage(actTitle);
      }
    }

    if(1 == nrOfExpectedChannels && null == getImageCh0()){
      throw new Exception("One channel expected, zero channels given.");
    } else if(2 == nrOfExpectedChannels && (null == getImageCh0() || null == getImageCh1())){
      throw new Exception("Two channel expected but just one or zero given.");
    }
    else{
      return startPipeline(imageFile);
    }
    //return new TreeMap<Integer, Channel>();
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

  abstract protected TreeMap<Integer, Channel> startPipeline(File imageFile);

}