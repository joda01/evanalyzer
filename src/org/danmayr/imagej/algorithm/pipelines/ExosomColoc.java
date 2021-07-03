package org.danmayr.imagej.algorithm.pipelines;

import java.io.File;
import java.util.Map;
import java.util.TreeMap;
import ij.process.AutoThresholder;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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

    ImagePlus background = null;

    final TreeMap<ChannelType, Channel> channels = new TreeMap<ChannelType, Channel>();
    final Vector<ColocChannelSet> colocChannels = new Vector<>();

    @Override
    protected TreeMap<ChannelType, Channel> startPipeline(File img) {

        background = null;
        channels.clear();
        colocChannels.clear();

        if (null != getBackground()) {
            background = getBackground().mChannelImg;
        }

        //
        // Count EVS and create threshold pictures
        //
        TreeMap<ChannelType, ChannelSettings> evs = getEvChannels();
        ExecutorService exec = Executors.newFixedThreadPool(evs.size());
        for (Map.Entry<ChannelType, ChannelSettings> val : evs.entrySet()) {
            exec.execute(new EvCounting(val));
        }
        exec.shutdown();
        try {
            exec.awaitTermination(1, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Channel colocAll = null;
        RoiManager rm = new RoiManager(false);

        //
        // Calc Coloc for two channel combinations (0 - 1   | 0 - 2 | 1 - 2)
        //
        if(true == mSettings.mCalcColoc){
            String nameOfAllChannels="Coloc of ";
            int colocEnum = 0;
            for (int n = 0; n < colocChannels.size(); n++) {
                for (int m = n+1; m < colocChannels.size(); m++) {
                    ColocChannelSet img0 = colocChannels.get(n);
                    ColocChannelSet img1 = colocChannels.get(m);
                    Channel coloc01 = CalcColoc("Coloc of " + img0.type.toString() + " with " + img1.type.toString(), rm,
                    img0.imageAfterThershold, img1.imageAfterThershold, img0.imgeBeforeThershold, img0.imgeBeforeThershold);
                    channels.put(ChannelType.getColocEnum(colocEnum), coloc01);
                    colocAll = coloc01;
                    colocEnum++;
                }

                nameOfAllChannels = colocChannels.get(n).type.toString();
                if(n<colocChannels.size()){
                    nameOfAllChannels += " with ";
                }
            }

            //
            // Calc coloc for all channels
            //
            if(colocChannels.size() > 2){
                colocAll = CalcColoc(nameOfAllChannels,rm,colocChannels);
                channels.put(ChannelType.COLOC_ALL, colocAll);
            }
        }


        //
        // Save debug images
        //
        String name = img.getAbsolutePath().replace(java.io.File.separator, "");
        PerformanceAnalyzer.start("save_ctrl");
        saveControlImages(name, rm, colocAll);
        PerformanceAnalyzer.stop("save_ctrl");

        return channels;
    }

    //
    // Calc coloc for two channels
    //
    Channel CalcColoc(String name, RoiManager rm, ImagePlus img0, ImagePlus img1, ImagePlus img0Origial,
            ImagePlus img1Original) {
        ImagePlus sumImageOriginal = Filter.ANDImages(img0Origial, img1Original);
        ImagePlus sumImage = Filter.ANDImages(img0, img1);
        Filter.ApplyThershold(sumImage, AutoThresholder.Method.Yen);
        Filter.AnalyzeParticles(sumImage, rm, 0, -1, mSettings.mMinCircularity);
        Channel measColoc01 = Filter.MeasureImage(name, mSettings, sumImageOriginal, sumImage, rm);
        return measColoc01;
    }

    //
    // Calc coloc for more than 2 channels
    //
    Channel CalcColoc(String name, RoiManager rm, Vector<ColocChannelSet> pics) {
        ImagePlus sumImageOriginal = pics.get(0).imgeBeforeThershold;
        ImagePlus sumImageThersholded = pics.get(0).imageAfterThershold;

        for (int n = 1; n < pics.size(); n++) {
            sumImageOriginal = Filter.ANDImages(sumImageOriginal, pics.get(n).imgeBeforeThershold);
            sumImageThersholded = Filter.ANDImages(sumImageThersholded, pics.get(n).imageAfterThershold);
        }
        Filter.ApplyThershold(sumImageThersholded, AutoThresholder.Method.Yen);
        Filter.AnalyzeParticles(sumImageThersholded, rm, 0, -1, mSettings.mMinCircularity);
        Channel measColoc = Filter.MeasureImage(name, mSettings, sumImageOriginal, sumImageThersholded, rm);

        return measColoc;
    }


    ///
    /// EV counting
    ///
    class EvCounting implements Runnable {

        Map.Entry<ChannelType, ChannelSettings> val;

        EvCounting(Map.Entry<ChannelType, ChannelSettings> v) {
            this.val = v;
        }

        @Override
        public void run() {
            final ChannelSettings img0 = val.getValue();
            ImagePlus img0Th = Filter.duplicateImage(img0.mChannelImg);
            RoiManager rm = new RoiManager(false);
            double[] in0 = new double[2];
            ImagePlus img0BeforeTh = preFilterSetColoc(img0Th, background, img0.enhanceContrast, img0.mThersholdMethod,
                    img0.minThershold, img0.maxThershold, in0);
            ImagePlus analzeImg0 = Filter.AnalyzeParticles(img0Th, rm, 0, -1, mSettings.mMinCircularity);
            Channel measCh0 = Filter.MeasureImage(img0.type.toString(), mSettings, img0BeforeTh, img0.mChannelImg, rm);
            measCh0.setThershold(in0[0], in0[1]);
            channels.put(img0.type, measCh0);
            colocChannels.add(new ColocChannelSet(img0BeforeTh, img0Th, img0.type,measCh0));
        }
    }

    //
    // Contains the result pictures from the counting
    //
    class ColocChannelSet {
        ColocChannelSet(ImagePlus bth, ImagePlus ath, ChannelType t, Channel ch) {
            this.imgeBeforeThershold = bth;
            this.imageAfterThershold = ath;
            this.type = t;
            this.ch = ch;
        }
  
        ImagePlus imgeBeforeThershold;
        ImagePlus imageAfterThershold;
        ChannelType type;
        Channel ch;
    }
  
    ///
    /// Save control images
    ///
    void saveControlImages(String name, RoiManager rm, Channel measColoc) {
      if (AnalyseSettings.CotrolPicture.WithControlPicture == mSettings.mSaveDebugImages) {
        // ImagePlus[] = {"red", "green", "blue", "gray", "cyan", "magenta", "yellow"};
        ImagePlus[] imgAry = { null, null, null, null, null, null, null };
        Channel[] chAry = { null, null, null, null, null, null, null };
        String[] chNames = { null, null, null, null, null, null, null };
  

        for(int n = 0;n<colocChannels.size();n++){
            if(colocChannels.get(n) != null){
                int colorIdx = colocChannels.get(n).type.getColorIdx();
                if(colorIdx < imgAry.length){
                    imgAry[colorIdx] = colocChannels.get(n).imgeBeforeThershold;
                    chAry[colorIdx] = colocChannels.get(n).ch;
                    chNames[colorIdx] = colocChannels.get(n).ch.toString();
                }
            }
        }

        name = name.replace("%", "");
        name = name.replace(" ", "");
        name = name.replace(":", "");
        name = name.replace("^", "");
        name = name.replace("+", "");
        name = name.replace("*", "");
        name = name.replace("~", "");
        name = name.toLowerCase();
  
        String path = mSettings.mOutputFolder + java.io.File.separator + name;
  
        if (null != measColoc) {
          ImagePlus mergedChannel = Filter.MergeChannels(imgAry);
          Filter.SaveImage(mergedChannel, path + "_merged.jpg", rm);
          measColoc.addControlImagePath(name + "_merged.jpg");
        }
  
        for (int n = 0; n < imgAry.length; n++) {
          if (imgAry[n] != null && chAry[n] != null) {
            String fileName = "_" + chNames[n] + ".jpg";
            ImagePlus newImg = Filter.duplicateImage(imgAry[n]);
            Filter.SaveImageWithOverlay(newImg, rm, path + fileName);
            chAry[n].addControlImagePath(name + fileName);
          }
        }
  
      }
    }


}
