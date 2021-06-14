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

    ImagePlus img0BeforeTh;
    Channel measCh0;

    ImagePlus img1BeforeTh;
    Channel measCh1;

    ImagePlus img2BeforeTh;
    Channel measCh2;

    ImagePlus background = null;


    @Override
    protected TreeMap<Integer, Channel> startPipeline(File img) {

        background = null;

        final ChannelSettings img0 = (ChannelSettings)getEvChannels().values().toArray()[0];
        final ChannelSettings img1 = (ChannelSettings)getEvChannels().values().toArray()[1];
        final ChannelSettings img2 = (ChannelSettings)getEvChannels().values().toArray()[2];
        ImagePlus img0Th = Filter.duplicateImage(img0.mChannelImg);
        ImagePlus img1Th = Filter.duplicateImage(img1.mChannelImg);
        ImagePlus img2Th = Filter.duplicateImage(img2.mChannelImg);
        final TreeMap<Integer, Channel> channels = new TreeMap<Integer, Channel>();


        if(null != getBackground()){
            background = getBackground().mChannelImg;
        }


        Thread t1 = new Thread(new Runnable() {
                @Override
                public void run() {
                    RoiManager rm = new RoiManager(false);
                    double[] in0 = new double[2];
                    img0BeforeTh = preFilterSetColoc(img0Th, background, img0.enhanceContrast,
                            img0.mThersholdMethod, img0.minThershold, img0.maxThershold, in0);
                    ImagePlus analzeImg0 = Filter.AnalyzeParticles(img0Th, rm, 0, -1, mSettings.mMinCircularity);
                    measCh0 = Filter.MeasureImage("ch0", mSettings, img0BeforeTh, img0.mChannelImg, rm);
                    measCh0.setThershold(in0[0], in0[1]);
                    channels.put(0, measCh0);
                }
            });
            t1.start();

            Thread t2 = new Thread(new Runnable() {
                @Override
                public void run() {
                    RoiManager rm = new RoiManager(false);
                    double[] in0 = new double[2];
                    img1BeforeTh = preFilterSetColoc(img1Th, background, img1.enhanceContrast,
                            img1.mThersholdMethod, img1.minThershold, img1.maxThershold, in0);
                    ImagePlus analzeImg1 = Filter.AnalyzeParticles(img1Th, rm, 0, -1, mSettings.mMinCircularity);
                    measCh1 = Filter.MeasureImage("ch1", mSettings, img1BeforeTh, img1.mChannelImg, rm);
                    measCh1.setThershold(in0[0], in0[1]);
                    channels.put(1, measCh1);
                }
            });
            t2.start();


            Thread t3 = new Thread(new Runnable() {
                @Override
                public void run() {
                    RoiManager rm = new RoiManager(false);
                    double[] in0 = new double[2];
                    img2BeforeTh = preFilterSetColoc(img2Th, background, img2.enhanceContrast,
                            img2.mThersholdMethod, img2.minThershold, img2.maxThershold, in0);
                    ImagePlus analzeImg2 = Filter.AnalyzeParticles(img2Th, rm, 0, -1, mSettings.mMinCircularity);
                    measCh2 = Filter.MeasureImage("ch2", mSettings, img2BeforeTh, img2.mChannelImg, rm);
                    measCh2.setThershold(in0[0], in0[1]);
                    channels.put(2, measCh2);
                }
            });
            t3.start();

      
            try {
                t1.join();
                t2.join();
                t3.join();
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        // Coloc 01
        RoiManager rm = new RoiManager(false);
        Channel coloc01 = CalcColoc("Coloc 01", 3, rm, img0.mChannelImg, img1.mChannelImg, img0BeforeTh, img1BeforeTh);
        Channel coloc02 = CalcColoc("Coloc 02", 4, rm, img0.mChannelImg, img2.mChannelImg, img0BeforeTh, img2BeforeTh);
        Channel coloc12 = CalcColoc("Coloc 12", 5, rm, img1.mChannelImg, img2.mChannelImg, img1BeforeTh, img2BeforeTh);
        Channel coloc012 = CalcColoc("Coloc 012", 6, rm, img0.mChannelImg, img1.mChannelImg, img2.mChannelImg,
                img0BeforeTh, img1BeforeTh, img2BeforeTh);

       
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
