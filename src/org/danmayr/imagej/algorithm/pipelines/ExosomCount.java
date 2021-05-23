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
import org.danmayr.imagej.algorithm.ChannelSettings;
import org.danmayr.imagej.algorithm.statistics.*;

public class ExosomCount extends Pipeline {

    static int MAX_THERSHOLD = 255;

    public ExosomCount(AnalyseSettings settings) {
        super(settings);
    }

    @Override
    protected TreeMap<Integer, Channel> startPipeline(File img) {

        TreeMap<Integer, Channel> channels = new TreeMap<Integer, Channel>();
        RoiManager rm = new RoiManager();

        ChannelSettings img0 = getImageOfChannel(0);
        ChannelSettings img1 = getImageOfChannel(1);
        ChannelSettings img2 = getImageOfChannel(2);

        double[] in0 = new double[2];
        ImagePlus img0BeforeTh = preFilterSetColoc(img0.mChannelImg, img0.enhanceContrast, img0.mThersholdMethod, img0.minThershold,
        img0.maxThershold, in0);

        Filter.AnalyzeParticles(img0.mChannelImg, rm,0,-1,mSettings.mMinCircularity);
        Channel measCh0 = Filter.MeasureImage(0, "ch0", mSettings, img0BeforeTh, img0.mChannelImg, rm);
        measCh0.setThershold(in0[0], in0[1]);
        channels.put(0, measCh0);

        Channel measCh1 = null;
        if (null != img1) {
            double[] in1 = new double[2];

            ImagePlus img1BeforeTh = preFilterSetColoc(img1.mChannelImg, img1.enhanceContrast, img1.mThersholdMethod,
            img1.minThershold, img1.maxThershold, in1);

            Filter.AnalyzeParticles(img1.mChannelImg, rm,0,-1,mSettings.mMinCircularity);
            measCh1 = Filter.MeasureImage(1, "ch1", mSettings, img1BeforeTh, img1.mChannelImg, rm);

            measCh1.setThershold(in1[0], in1[1]);
            channels.put(1, measCh1);
        }

        Channel measCh2 = null;
        if (null != img2) {
            double[] in1 = new double[2];

            ImagePlus img2BeforeTh = preFilterSetColoc(img2.mChannelImg, img2.enhanceContrast, img2.mThersholdMethod,
            img2.minThershold, img2.maxThershold, in1);

            Filter.AnalyzeParticles(img2.mChannelImg, rm,0,-1,mSettings.mMinCircularity);
            measCh2 = Filter.MeasureImage(1, "ch2", mSettings, img2BeforeTh, img2.mChannelImg, rm);

            measCh2.setThershold(in1[0], in1[1]);
            channels.put(2, measCh2);
        }

        // Save debug images
        String name = img.getAbsolutePath().replace(java.io.File.separator, "");
        saveControlImages(name,measCh0,measCh1,null,img0.type,img2.type,null, rm, null);  

        return channels;
    }

}
