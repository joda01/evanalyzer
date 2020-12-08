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

    public ExosomCount(AnalyseSettings settings, ChannelType ch0, ChannelType ch1) {
        super(settings, ch0, ch1);
    }

    @Override
    protected TreeMap<Integer, Channel> startPipeline(File img) {

        RoiManager rm = new RoiManager();

        ImagePlus img0 = getImageCh0();
        ImagePlus img1 = getImageCh1();

        if (mSettings.ch0 == Pipeline.ChannelType.NEGATIVE_CONTROL) {
            img0 = Filter.paintOval(img0);
        } 
        if (mSettings.ch1 == Pipeline.ChannelType.NEGATIVE_CONTROL) {
            img1 = Filter.paintOval(img1);
        }

        img0 = Filter.SubtractBackground(img0);
        img1 = Filter.SubtractBackground(img1);

        img0 = Filter.ApplyGaus(img0);
        img1 = Filter.ApplyGaus(img1);

        img0 = Filter.ApplyThershold(img0, mSettings.mThersholdMethod,-1,-1, null);
        img1 = Filter.ApplyThershold(img1, mSettings.mThersholdMethod,-1,-1, null);

        TreeMap<Integer, Channel> channels = new TreeMap<Integer, Channel>();

        Filter.AnalyzeParticles(img0);
        Channel measCh0 = Filter.MeasureImage(0, "ch0", mSettings, img0, rm);

        Filter.AnalyzeParticles(img1);
        Channel measCh1 = Filter.MeasureImage(1, "ch1", mSettings, img1, rm);

        channels.put(0, measCh0);
        channels.put(1, measCh1);

        // Save debug images
        if (true == mSettings.mSaveDebugImages) {
            Channel greenChannel;
            Channel redChannel;

            ImagePlus greenImg;
            ImagePlus redImg;
            if (mSettings.ch0 == Pipeline.ChannelType.GFP) {
                greenImg = img0;
                redImg = img1;

                greenChannel = measCh0;
                redChannel = measCh1;

            } else if (mSettings.ch0 == Pipeline.ChannelType.CY3) {
                redImg = img0;
                greenImg = img1;

                greenChannel = measCh1;
                redChannel = measCh0;
            } else {
                return channels;
            }

            String name = img.getAbsolutePath().replace(java.io.File.separator, "");
            name = name.replaceAll("%", "");
            name = name.replaceAll(" ", "");

            String path = mSettings.mOutputFolder + java.io.File.separator + name;
            ImagePlus mergedChannel = Filter.MergeChannels(redImg, greenImg);
            Filter.SaveImageWithOverlay(greenImg, rm, path + "_gfp");
            Filter.SaveImageWithOverlay(redImg, rm, path + "_cy3");

            greenChannel.addControlImagePath(name + "_gfp.jpg");
            redChannel.addControlImagePath(name + "_cy3.jpg");
        }

        return channels;
    }

}
