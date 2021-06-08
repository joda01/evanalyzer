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
import org.danmayr.imagej.algorithm.pipelines.*;

public class ExosomeColoc3Ch extends ExosomColoc {

    static int MAX_THERSHOLD = 255;

    public ExosomeColoc3Ch(AnalyseSettings settings) {
        super(settings);
    }

    @Override
    protected TreeMap<Integer, Channel> startPipeline(File img) {

        RoiManager rm = new RoiManager();

        ChannelSettings img0 = (ChannelSettings)getEvChannels().values().toArray()[0];
        ChannelSettings img1 = (ChannelSettings)getEvChannels().values().toArray()[1];
        ChannelSettings img2 = (ChannelSettings)getEvChannels().values().toArray()[2];
        ImagePlus background = null;
        if(null != getBackground()){
            background = getBackground().mChannelImg;
        }

        double[] in0 = new double[2];
        double[] in1 = new double[2];
        double[] in2 = new double[2];

        ImagePlus img0BeforeTh = preFilterSetColoc(img0.mChannelImg,background, img0.enhanceContrast, img0.mThersholdMethod,
                img0.minThershold, img0.maxThershold, in0);
        ImagePlus img1BeforeTh = preFilterSetColoc(img1.mChannelImg,background, img1.enhanceContrast, img1.mThersholdMethod,
                img1.minThershold, img1.maxThershold, in1);
        ImagePlus img2BeforeTh = preFilterSetColoc(img2.mChannelImg,background, img2.enhanceContrast, img2.mThersholdMethod,
                img2.minThershold, img2.maxThershold, in2);

        ImagePlus analzeImg0 = Filter.AnalyzeParticles(img0.mChannelImg, rm, 0, -1, mSettings.mMinCircularity);
        Channel measCh0 = Filter.MeasureImage( "ch0", mSettings, img0BeforeTh, img0.mChannelImg, rm);

        // Channel measCh1Temp = Filter.MeasureImage(1, "ch1", mSettings, img1BeforeTh,
        // img1, rm);
        // Channel measColocCh0 = calculateColoc(1, "Coloc Ch0 with Ch1", measCh0,
        // measCh1Temp);

        ImagePlus analzeImg1 = Filter.AnalyzeParticles(img1.mChannelImg, rm, 0, -1, mSettings.mMinCircularity);
        Channel measCh1 = Filter.MeasureImage( "ch1", mSettings, img1BeforeTh, img1.mChannelImg, rm);
        // Channel measCh0Temp = Filter.MeasureImage(0, "ch0", mSettings, img0BeforeTh,
        // img0, rm);
        // Channel measColocCh1 = calculateColoc(2, "Coloc Ch1 with Ch0", measCh0Temp,
        // measCh1);

        ImagePlus analzeImg2 = Filter.AnalyzeParticles(img2.mChannelImg, rm, 0, -1, mSettings.mMinCircularity);
        Channel measCh2 = Filter.MeasureImage("ch2", mSettings, img2BeforeTh, img2.mChannelImg, rm);

        // Coloc 01
        Channel coloc01 = CalcColoc("Coloc 01", 3, rm, img0.mChannelImg, img1.mChannelImg, img0BeforeTh, img1BeforeTh);
        Channel coloc02 = CalcColoc("Coloc 02", 4, rm, img0.mChannelImg, img2.mChannelImg, img0BeforeTh, img2BeforeTh);
        Channel coloc12 = CalcColoc("Coloc 12", 5, rm, img1.mChannelImg, img2.mChannelImg, img1BeforeTh, img2BeforeTh);
        Channel coloc012 = CalcColoc("Coloc 012", 6, rm, img0.mChannelImg, img1.mChannelImg, img2.mChannelImg,
                img0BeforeTh, img1BeforeTh, img2BeforeTh);

        measCh0.setThershold(in0[0], in0[1]);
        measCh1.setThershold(in1[0], in1[1]);
        measCh2.setThershold(in1[0], in1[1]);

        TreeMap<Integer, Channel> channels = new TreeMap<Integer, Channel>();

        channels.put(0, measCh0);
        channels.put(1, measCh1);
        channels.put(2, measCh2);

        channels.put(3, coloc01);
        channels.put(4, coloc02);
        channels.put(5, coloc12);
        channels.put(6, coloc012);

        // Save debug images
        String name = img.getAbsolutePath().replace(java.io.File.separator, "");
        saveControlImages(name, img0BeforeTh, img1BeforeTh, img2BeforeTh, measCh0, measCh1, measCh2, img0.type, img1.type,
                img2.type, rm, coloc012);

        return channels;
    }

    Channel CalcColoc(String name, int idx, RoiManager rm, ImagePlus img0, ImagePlus img1, ImagePlus img2,
            ImagePlus img0Origial, ImagePlus img1Original, ImagePlus img2Original) {
        ImagePlus sumImageOriginal = Filter.ANDImages(img0Origial, img1Original);
        sumImageOriginal = Filter.ANDImages(sumImageOriginal, img2Original);

        ImagePlus sumImage = Filter.ANDImages(img0, img1);
        sumImage = Filter.ANDImages(sumImage, img2);

        Filter.AnalyzeParticles(sumImage, rm, 0, -1, mSettings.mMinCircularity);
        Channel measColoc01 = Filter.MeasureImage(name, mSettings, sumImageOriginal, sumImage, rm);
        return measColoc01;
    }

}
