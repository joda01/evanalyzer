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

public class ExosomColoc extends Pipeline {

    static int MAX_THERSHOLD = 255;

    public ExosomColoc(AnalyseSettings settings, ChannelType ch0, ChannelType ch1) {
        super(settings, ch0, ch1);
    }

    @Override
    protected TreeMap<Integer, Channel> startPipeline(File img) {

        RoiManager rm = new RoiManager();

        ImagePlus img0 = getImageCh0();
        ImagePlus img1 = getImageCh1();

        double[] in0 = new double[2];
        double[] in1 = new double[2];

        ImagePlus img0BeforeTh = preFilterSetColoc(img0, mSettings.ch0.enhanceContrast, mSettings.ch0.mThersholdMethod,
                mSettings.ch0.minThershold, mSettings.ch0.maxThershold, in0);
        ImagePlus img1BeforeTh = preFilterSetColoc(img1, mSettings.ch1.enhanceContrast, mSettings.ch1.mThersholdMethod,
                mSettings.ch1.minThershold, mSettings.ch1.maxThershold, in1);

        Filter.AnalyzeParticles(img0);
        Channel measCh0 = Filter.MeasureImage(0, "ch0", mSettings, img0BeforeTh, img0, rm);

        Filter.AnalyzeParticles(img1);
        Channel measCh1 = Filter.MeasureImage(1, "ch1", mSettings, img1BeforeTh, img1, rm);

        ImagePlus sumImage = Filter.AddImages(img0, img1);

        Filter.AnalyzeParticles(sumImage);
        TreeMap<Integer, Channel> channels = new TreeMap<Integer, Channel>();

        Channel colocCh0 = Filter.MeasureImage(0, "ch0", mSettings, img0BeforeTh, img0, rm);
        Channel colocCh1 = Filter.MeasureImage(1, "ch1", mSettings, img1BeforeTh, img1, rm);
        Channel measColoc = calculateColoc(colocCh0, colocCh1);

        measCh0.setThershold(in0[0], in0[1]);
        measCh1.setThershold(in1[0], in1[1]);

        channels.put(0, measCh0);
        channels.put(1, measCh1);
        channels.put(2, measColoc);

        // Save debug images
        if (AnalyseSettings.CotrolPicture.WithControlPicture == mSettings.mSaveDebugImages) {
            Channel greenChannel;
            Channel redChannel;

            ImagePlus greenImg;
            ImagePlus redImg;
            if (mSettings.ch0.type == Pipeline.ChannelType.GFP) {
                greenImg = img0;
                redImg = img1;

                greenChannel = measCh0;
                redChannel = measCh1;

            } else if (mSettings.ch0.type == Pipeline.ChannelType.CY3) {
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
            name = name.replaceAll(":", "");
            name = name.toLowerCase();

            String path = mSettings.mOutputFolder + java.io.File.separator + name;
            ImagePlus mergedChannel = Filter.MergeChannels(redImg, greenImg);
            Filter.SaveImage(mergedChannel, path + "_merged.jpg");
            Filter.SaveImageWithOverlay(greenImg, rm, path + "_gfp.jpg");
            Filter.SaveImageWithOverlay(redImg, rm, path + "_cy3.jpg");

            greenChannel.addControlImagePath(name + "_gfp.jpg");
            redChannel.addControlImagePath(name + "_cy3.jpg");
            measColoc.addControlImagePath(name + "_merged.jpg");
        }

        return channels;
    }

    private Channel calculateColoc(Channel ch0, Channel ch1) {
        Channel ch = new Channel(2, "Coloc", new ColocStatistic());

        TreeMap<Integer, ParticleInfo> roiCh0 = ch0.getRois();
        TreeMap<Integer, ParticleInfo> roiCh1 = ch1.getRois();

        if (roiCh0.size() == roiCh1.size()) {

            for (Map.Entry<Integer, ParticleInfo> entry : roiCh0.entrySet()) {
                int key = entry.getKey();
                ParticleInfo ch0Info = entry.getValue();
                ParticleInfo ch1Info = roiCh1.get(key);

                double colocValue = Math
                        .abs(MAX_THERSHOLD - Math.abs(ch0Info.areaThersholdScale - ch1Info.areaThersholdScale));

                double areaSize0OfPixles = (ch0Info.areaThersholdScale * ch0Info.areaSize) / MAX_THERSHOLD;
                double areaSize1OfPixles = (ch1Info.areaThersholdScale * ch1Info.areaSize) / MAX_THERSHOLD;

                double smallArea = areaSize0OfPixles;
                if (areaSize1OfPixles < smallArea) {
                    smallArea = areaSize1OfPixles;
                }

                ColocRoi exosom = new ColocRoi(key, smallArea, areaSize0OfPixles, areaSize1OfPixles,
                        ch0Info.areaGrayScale, ch0Info.areaThersholdScale, ch0Info.circularity, colocValue);
                exosom.validatearticle(mSettings.mMinParticleSize, mSettings.mMaxParticleSize,
                        mSettings.mMinCircularity, mSettings.minIntensity);
                ch.addRoi(exosom);
            }
            ch.calcStatistics();
        } else {
            IJ.log("calculateColoc ROI size not equal.");
        }

        return ch;
    }

    class ColocRoi extends ParticleInfo {

        public ColocRoi(int roiName, double smallestAreaSize, double areaSizeCh0, double areaSizeCh1,
                double areaGrayScale, double areaThersholdScale, double circularity, double coloValue) {
            super(roiName, smallestAreaSize, areaGrayScale, areaThersholdScale, circularity);
            this.colocValue = coloValue;
            this.areaSizeCh0 = areaSizeCh0;
            this.areaSizeCh1 = areaSizeCh1;
        }

        ///
        /// \brief Returns the name of the roi
        ///
        public String toString() {
            return Integer.toString(roiName) + ";" + Double.toString(areaSizeCh0) + ";" + Double.toString(areaSizeCh1)
                    + ";" + Double.toString(colocValue);
        }

        public double colocValue;
        public double areaSizeCh0;
        public double areaSizeCh1;

        @Override
        public double[] getValues() {
            double[] values = { areaSizeCh0, areaSizeCh1, colocValue };
            return values;
        }

        public String[] getTitle() {
            String[] title = { "area size CH0", "area size CH1", "coloc" };
            return title;
        }

        ///
        /// \breif check if this particle matches the filter criteria
        ///
        @Override
        public void validatearticle(double minAreaSize, double maxAreaSize, double minCircularity,
                double minGrayScale) {
            status = VALID;

            if (areaSize < minAreaSize && areaSize != 0) {
                status |= TOO_SMALL;
            }

            if (areaSize > maxAreaSize) {
                status |= TOO_BIG;
            }

            if (circularity < minCircularity) {
                status |= WRONG_CIRCULARITY;
            }

            if (areaGrayScale < minGrayScale) {
                status |= WRONG_INTENSITY;
            }
        }

    }

    class ColocStatistic extends Statistics {
        @Override
        public void calcStatistics(Channel ch) {
            int nrOfInvalid = 0;
            int nrOfNotColoc = 0;
            mColocNr = 0;

            for (Map.Entry<Integer, ParticleInfo> entry : ch.getRois().entrySet()) {
                int chNr = entry.getKey();
                ParticleInfo info = entry.getValue();

                if (false == info.isValid()) {
                    nrOfInvalid++;
                } else {
                    if (info.areaThersholdScale > 0) {
                        mColocNr++;
                    }else{
                        nrOfNotColoc++;
                    }
                }
            }
            this.invalid = nrOfInvalid;
            this.valid = nrOfNotColoc;
        }

        public double[] getValues() {
            double[] values = { mColocNr, valid, invalid };
            return values;
        }

        public String[] getTitle() {
            String[] title = { "coloc", "ot coloc", "invalid" };
            return title;
        }

        public int mColocNr = 0;
    }

}
