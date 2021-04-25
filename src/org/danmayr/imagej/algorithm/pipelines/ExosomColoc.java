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
import org.danmayr.imagej.algorithm.statistics.*;
import org.danmayr.imagej.algorithm.pipelines.*;

public class ExosomColoc extends Pipeline {

    static int MAX_THERSHOLD = 255;

    public ExosomColoc(AnalyseSettings settings) {
        super(settings);
    }

    @Override
    protected TreeMap<Integer, Channel> startPipeline(File img, AnalyseSettings.ChannelSettings ch0s,
            AnalyseSettings.ChannelSettings ch1s, AnalyseSettings.ChannelSettings ch2s) {

        RoiManager rm = new RoiManager();

        ImagePlus img0 = getImageCh0();
        ImagePlus img1 = getImageCh1();

        double[] in0 = new double[2];
        double[] in1 = new double[2];

        ImagePlus img0BeforeTh = preFilterSetColoc(img0, ch0s.enhanceContrast, ch0s.mThersholdMethod, ch0s.minThershold,
                ch0s.maxThershold, in0);
        ImagePlus img1BeforeTh = preFilterSetColoc(img1, ch1s.enhanceContrast, ch1s.mThersholdMethod, ch1s.minThershold,
                ch1s.maxThershold, in1);


        Filter.AnalyzeParticles(img0, rm);
        Channel measCh0 = Filter.MeasureImage(0, "ch0", mSettings, img0BeforeTh, img0, rm);
        // Channel measCh1Temp = Filter.MeasureImage(1, "ch1", mSettings, img1BeforeTh,
        // img1, rm);
        // Channel measColocCh0 = calculateColoc(1, "Coloc Ch0 with Ch1", measCh0,
        // measCh1Temp);

        Filter.AnalyzeParticles(img1, rm);
        Channel measCh1 = Filter.MeasureImage(1, "ch1", mSettings, img1BeforeTh, img1, rm);
        // Channel measCh0Temp = Filter.MeasureImage(0, "ch0", mSettings, img0BeforeTh,
        // img0, rm);
        // Channel measColocCh1 = calculateColoc(2, "Coloc Ch1 with Ch0", measCh0Temp,
        // measCh1);



        // Coloc 01
        Channel coloc01 = CalcColoc("Coloc 01", 3, rm, img0, img1, img0BeforeTh, img1BeforeTh);

        measCh0.setThershold(in0[0], in0[1]);
        measCh1.setThershold(in1[0], in1[1]);

        TreeMap<Integer, Channel> channels = new TreeMap<Integer, Channel>();

        channels.put(0, measCh0);
        channels.put(1, measCh1);

        channels.put(3, coloc01);

        // Save debug images
        String name = img.getAbsolutePath().replace(java.io.File.separator, "");
        saveControlImages(name, measCh0, measCh1, null, ch0s.type, ch1s.type, ch2s.type, rm, coloc01);

        return channels;
    }

    protected Channel CalcColoc(String name, int idx, RoiManager rm, ImagePlus img0, ImagePlus img1, ImagePlus img0Origial,
            ImagePlus img1Original) {
        ImagePlus sumImageOriginal = Filter.ANDImages(img0Origial, img1Original);
        ImagePlus sumImage = Filter.ANDImages(img0, img1);
        Filter.AnalyzeParticles(sumImage, rm);
        Channel measColoc01 = Filter.MeasureImage(idx, name, mSettings, sumImageOriginal, sumImage, rm);
        return measColoc01;
    }

}
