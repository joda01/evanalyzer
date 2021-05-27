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
        int nrOfPics =getEvChannels().values().toArray().length;

        ChannelSettings img0 = (ChannelSettings) getEvChannels().values().toArray()[0];
        ImagePlus background = null;
        if (null != getBackground()) {
            background = getBackground().mChannelImg;
        }

        ImagePlus analzeImg0 = null;
        ImagePlus analzeImg1 = null;
        ImagePlus analzeImg2 = null;

        Pipeline.ChannelType type0 = img0.type;
        Pipeline.ChannelType type1 = null;
        Pipeline.ChannelType type2 = null;

        double[] in0 = new double[2];
        ImagePlus img0BeforeTh = preFilterSetColoc(img0.mChannelImg, background, img0.enhanceContrast,
                img0.mThersholdMethod, img0.minThershold, img0.maxThershold, in0);

        analzeImg0 = Filter.AnalyzeParticles(img0.mChannelImg, rm, 0, -1, mSettings.mMinCircularity);
        Channel measCh0 = Filter.MeasureImage("ch0", mSettings, img0BeforeTh, img0.mChannelImg, rm);
        measCh0.setThershold(in0[0], in0[1]);
        channels.put(0, measCh0);

        Channel measCh1 = null;
        if (2 == nrOfPics) {
            ChannelSettings img1 = (ChannelSettings) getEvChannels().values().toArray()[1];

            double[] in1 = new double[2];
            type1 = img1.type;

            ImagePlus img1BeforeTh = preFilterSetColoc(img1.mChannelImg, background, img1.enhanceContrast,
                    img1.mThersholdMethod, img1.minThershold, img1.maxThershold, in1);

            analzeImg1 = Filter.AnalyzeParticles(img1.mChannelImg, rm, 0, -1, mSettings.mMinCircularity);
            measCh1 = Filter.MeasureImage("ch1", mSettings, img1BeforeTh, img1.mChannelImg, rm);

            measCh1.setThershold(in1[0], in1[1]);
            channels.put(1, measCh1);
        }

        Channel measCh2 = null;
        if (3 == nrOfPics) {
            ChannelSettings img2 = (ChannelSettings) getEvChannels().values().toArray()[2];

            double[] in1 = new double[2];
            type2 = img2.type;

            ImagePlus img2BeforeTh = preFilterSetColoc(img2.mChannelImg, background, img2.enhanceContrast,
                    img2.mThersholdMethod, img2.minThershold, img2.maxThershold, in1);

            analzeImg2 = Filter.AnalyzeParticles(img2.mChannelImg, rm, 0, -1, mSettings.mMinCircularity);
            measCh2 = Filter.MeasureImage("ch2", mSettings, img2BeforeTh, img2.mChannelImg, rm);

            measCh2.setThershold(in1[0], in1[1]);
            channels.put(2, measCh2);
        }

        // Save debug images
        String name = img.getAbsolutePath().replace(java.io.File.separator, "");
        saveControlImages(name, analzeImg0, analzeImg1, analzeImg2, measCh0, measCh1, measCh2, type0, type1,
                type2, rm, null);

        return channels;
    }

}
