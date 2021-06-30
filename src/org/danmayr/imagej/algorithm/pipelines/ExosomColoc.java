package org.danmayr.imagej.algorithm.pipelines;

import java.io.File;
import java.util.TreeMap;

import org.danmayr.imagej.algorithm.AnalyseSettings;
import org.danmayr.imagej.algorithm.ChannelSettings;
import org.danmayr.imagej.algorithm.filters.Filter;
import org.danmayr.imagej.algorithm.structs.Channel;
import org.danmayr.imagej.performance_analyzer.PerformanceAnalyzer;

import ij.ImagePlus;
import ij.plugin.frame.RoiManager;

public class ExosomColoc extends Pipeline {

    static int MAX_THERSHOLD = 255;

    public ExosomColoc(AnalyseSettings settings) {
        super(settings);
    }

    ImagePlus img0BeforeTh;
    Channel measCh0;

    ImagePlus img1BeforeTh;
    Channel measCh1;

    ImagePlus background = null;

    @Override
    protected TreeMap<Integer, Channel> startPipeline(File img) {

        background = null;

        ChannelSettings img0 = (ChannelSettings) getEvChannels().values().toArray()[0];
        ChannelSettings img1 = (ChannelSettings) getEvChannels().values().toArray()[1];

        ImagePlus img0Th = Filter.duplicateImage(img0.mChannelImg);
        ImagePlus img1Th = Filter.duplicateImage(img1.mChannelImg);
        final TreeMap<Integer, Channel> channels = new TreeMap<Integer, Channel>();

        if (null != getBackground()) {
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
                measCh0 = Filter.MeasureImage(img0.type.toString(), mSettings, img0BeforeTh, img0.mChannelImg, rm);
                measCh0.setThershold(in0[0], in0[1]);
                channels.put(0, measCh0);


            }
        });
        t1.start();

       
        Thread t2 = new Thread(new Runnable() {
            @Override
            public void run() {
                RoiManager rm = new RoiManager(false);
                double[] in1 = new double[2];
                img1BeforeTh = preFilterSetColoc(img1Th, background, img1.enhanceContrast,
                        img1.mThersholdMethod, img1.minThershold, img1.maxThershold, in1);
                ImagePlus analzeImg1 = Filter.AnalyzeParticles(img1Th, rm, 0, -1, mSettings.mMinCircularity);
                measCh1 = Filter.MeasureImage(img1.type.toString(), mSettings, img1BeforeTh, img1.mChannelImg, rm);
                measCh1.setThershold(in1[0], in1[1]);
                channels.put(1, measCh1);

            }
        });
        t2.start();


        try {
            t1.join();
            t2.join();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        RoiManager rm = new RoiManager(false);
        Channel coloc01 = CalcColoc("Coloc of " + img0.type.toString() + " with " + img1.type.toString(), 3, rm, img0Th, img1Th, img0BeforeTh, img1BeforeTh);
        channels.put(3, coloc01);

        // Save debug images
        String name = img.getAbsolutePath().replace(java.io.File.separator, "");
        PerformanceAnalyzer.start("save_ctrl");
        saveControlImages(name, img0BeforeTh, img1BeforeTh, null, measCh0, measCh1, null, img0.type, img1.type, null,
                rm, coloc01);
        PerformanceAnalyzer.stop("save_ctrl");

        return channels;
    }

    protected Channel CalcColoc(String name, int idx, RoiManager rm, ImagePlus img0, ImagePlus img1,
            ImagePlus img0Origial, ImagePlus img1Original) {
        ImagePlus sumImageOriginal = Filter.ANDImages(img0Origial, img1Original);
        ImagePlus sumImage = Filter.ANDImages(img0, img1);
        Filter.AnalyzeParticles(sumImage, rm, 0, -1, mSettings.mMinCircularity);
        Channel measColoc01 = Filter.MeasureImage(name, mSettings, sumImageOriginal, sumImage, rm);
        return measColoc01;
    }

}
