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

public class ExosomCount extends Pipeline {

    static int MAX_THERSHOLD = 255;

    public ExosomCount(AnalyseSettings settings) {
        super(settings);
    }

    @Override
    protected TreeMap<Integer, Channel> startPipeline(File img, AnalyseSettings.ChannelSettings ch0s,
            AnalyseSettings.ChannelSettings ch1s) {

        TreeMap<Integer, Channel> channels = new TreeMap<Integer, Channel>();
        RoiManager rm = new RoiManager();

        ImagePlus img0 = getImageCh0();
        ImagePlus img1 = getImageCh1();

        double[] in0 = new double[2];
        ImagePlus img0BeforeTh = preFilterSetColoc(img0, ch0s.enhanceContrast, ch0s.mThersholdMethod, ch0s.minThershold,
                ch0s.maxThershold, in0);

        Filter.AnalyzeParticles(img0, rm);
        Channel measCh0 = Filter.MeasureImage(0, "ch0", mSettings, img0BeforeTh, img0, rm);
        measCh0.setThershold(in0[0], in0[1]);
        channels.put(0, measCh0);

        Channel measCh1 = null;
        if (null != img1) {
            double[] in1 = new double[2];

            ImagePlus img1BeforeTh = preFilterSetColoc(img1, ch1s.enhanceContrast, ch1s.mThersholdMethod,
                    ch1s.minThershold, ch1s.maxThershold, in1);

            Filter.AnalyzeParticles(img1, rm);
            measCh1 = Filter.MeasureImage(1, "ch1", mSettings, img1BeforeTh, img1, rm);

            measCh1.setThershold(in1[0], in1[1]);
            channels.put(1, measCh1);
        }

        // Save debug images
        String name = img.getAbsolutePath().replace(java.io.File.separator, "");
        saveControlImages(name,measCh0,measCh1,null,ch0s.type,ch1s.type,null, rm, null);  

        return channels;
    }

}
