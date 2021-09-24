package org.danmayr.imagej.algorithm;

import java.util.Vector;

import org.danmayr.imagej.algorithm.pipelines.*;

import ij.ImagePlus;
import ij.process.AutoThresholder;


public class ChannelSettings implements Cloneable{

    public enum PreProcessingStep {
        None, EdgeDetection
    }

    public ImagePlus mChannelImg;
    public int mChannelNr=0;
    public Pipeline.ChannelType type;
    public AutoThresholder.Method mThersholdMethod;
    public boolean enhanceContrast;
    public int minThershold = -1;
    public int maxThershold = 65535;
    public String ZProjector = "OFF";
    public Vector<PreProcessingStep> preProcessing = new Vector<PreProcessingStep>();
    public int marginToCrop = 0;
    public double mMinCircularity = 0.0;


    public Object clone() throws CloneNotSupportedException
    {
        return super.clone();
    }
}
