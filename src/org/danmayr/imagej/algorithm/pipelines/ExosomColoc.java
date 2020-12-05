package org.danmayr.imagej.algorithm.pipelines;

import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.plugin.frame.RoiManager;

import java.io.File;
import java.util.*;
import org.danmayr.imagej.algorithm.structs.*;
import org.danmayr.imagej.algorithm.filters.Filter;

import org.danmayr.imagej.algorithm.AnalyseSettings;


public class ExosomColoc extends Pipeline {

    public ExosomColoc(AnalyseSettings settings, ChannelType ch0, ChannelType ch1)
    {
        super(settings,ch0,ch1);
    }


    @Override
    protected TreeMap<Integer, Channel> startPipeline(File img)
    {
        RoiManager rm = new RoiManager();

        
        ImagePlus img0 = Filter.SubtractBackground(getImageCh0());
        ImagePlus img1 = Filter.SubtractBackground(getImageCh1());

        img0 = Filter.ApplyGaus(img0);
        img1 = Filter.ApplyGaus(img1);

        img0 = Filter.ApplyThershold(img0, mSettings.mThersholdMethod);
        img1 = Filter.ApplyThershold(img0, mSettings.mThersholdMethod);

        ImagePlus sumImage = Filter.AddImages(img0,img1);

        Filter.AnalyzeParticles(sumImage);

        Channel measCh0 = Filter.MeasureImage(0,"ch0",img0,rm);
        Channel measCh1 = Filter.MeasureImage(1,"ch1",img1,rm);

        TreeMap<Integer, Channel> channels = new TreeMap<Integer, Channel>();

        channels.put(0, measCh0);
        channels.put(1, measCh1);


        return channels;
    }


    private Channel calculateColoc(Channel ch0, Channel ch1)
    {
        TreeMap<String, ParticleInfo> roiCh0 = ch0.getRois();
        TreeMap<String, ParticleInfo> roiCh1 = ch1.getRois();

        return new Channel(1,"");
    }

    class ColocRoi extends ParticleInfo{

        public ColocRoi(String roiName, double areaSize, double areaGrayScale, double circularity) {
            super(roiName, areaSize, areaGrayScale, circularity);
            // TODO Auto-generated constructor stub
        }

    }

}
