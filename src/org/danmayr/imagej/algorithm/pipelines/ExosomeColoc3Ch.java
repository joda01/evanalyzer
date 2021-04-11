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

public class ExosomeColoc3Ch extends ExosomColoc {

    static int MAX_THERSHOLD = 255;

    public ExosomeColoc3Ch(AnalyseSettings settings) {
        super(settings);
    }

    @Override
    protected TreeMap<Integer, Channel> startPipeline(File img, AnalyseSettings.ChannelSettings ch0s,
            AnalyseSettings.ChannelSettings ch1s, AnalyseSettings.ChannelSettings ch2s) {

        RoiManager rm = new RoiManager();

        ImagePlus img0 = getImageCh0();
        ImagePlus img1 = getImageCh1();
        ImagePlus img2 = getImageCh2();

        double[] in0 = new double[2];
        double[] in1 = new double[2];
        double[] in2 = new double[2];

        ImagePlus img0BeforeTh = preFilterSetColoc(img0, ch0s.enhanceContrast, ch0s.mThersholdMethod, ch0s.minThershold,
                ch0s.maxThershold, in0);
        ImagePlus img1BeforeTh = preFilterSetColoc(img1, ch1s.enhanceContrast, ch1s.mThersholdMethod, ch1s.minThershold,
                ch1s.maxThershold, in1);
        ImagePlus img2BeforeTh = preFilterSetColoc(img2, ch2s.enhanceContrast, ch2s.mThersholdMethod, ch2s.minThershold,
                ch2s.maxThershold, in2);

        Filter.AnalyzeParticles(img0, rm);
        Channel measCh0 = Filter.MeasureImage(0, "ch0", mSettings, img0BeforeTh, img0, rm);
        Channel measCh1Temp = Filter.MeasureImage(1, "ch1", mSettings, img1BeforeTh, img1, rm);
        Channel measColocCh0 = calculateColoc(1, "Coloc Ch0 with Ch1", measCh0, measCh1Temp);

        Filter.AnalyzeParticles(img1, rm);
        Channel measCh0Temp = Filter.MeasureImage(0, "ch0", mSettings, img0BeforeTh, img0, rm);
        Channel measCh1 = Filter.MeasureImage(1, "ch1", mSettings, img1BeforeTh, img1, rm);
        Channel measColocCh1 = calculateColoc(2, "Coloc Ch1 with Ch0", measCh0Temp, measCh1);

        Filter.AnalyzeParticles(img2, rm);
        Channel measCh2 = Filter.MeasureImage(0, "ch2", mSettings, img2BeforeTh, img2, rm);

        ImagePlus sumImage = Filter.AddImages(img0, img1);
        sumImage = Filter.AddImages(sumImage, img2);

        Filter.AnalyzeParticles(sumImage, rm);
        TreeMap<Integer, Channel> channels = new TreeMap<Integer, Channel>();

        Channel colocCh0 = Filter.MeasureImage(0, "ch0", mSettings, img0BeforeTh, img0, rm);
        Channel colocCh1 = Filter.MeasureImage(1, "ch1", mSettings, img1BeforeTh, img1, rm);
        Channel colocCh2 = Filter.MeasureImage(2, "ch2", mSettings, img2BeforeTh, img2, rm);

        Channel measColoc = calculateColoc(3, "Coloc all Ch", colocCh0, colocCh1, colocCh2);

        measCh0.setThershold(in0[0], in0[1]);
        measCh1.setThershold(in1[0], in1[1]);
        measCh2.setThershold(in1[0], in1[1]);

        channels.put(0, measCh0);
        // channels.put(1, measColocCh0);

        channels.put(1, measCh1);
        // channels.put(3, measColocCh1);

        channels.put(2, measCh2);
        // channels.put(5, measColocCh2);

        channels.put(3, measColoc);

        // Save debug images
        String name = img.getAbsolutePath().replace(java.io.File.separator, "");
        saveControlImages(name, measCh0, measCh1, measCh2, ch0s.type, ch1s.type, ch2s.type, rm, measColoc);

        return channels;
    }

    private Channel calculateColoc(int chNr, String chName, Channel ch0, Channel ch1, Channel ch2) {
        Channel ch = new Channel(chNr, chName, new ColocStatistic());

        TreeMap<Integer, ParticleInfo> roiCh0 = ch0.getRois();
        TreeMap<Integer, ParticleInfo> roiCh1 = ch1.getRois();
        TreeMap<Integer, ParticleInfo> roiCh2 = ch2.getRois();

        if (roiCh0.size() == roiCh1.size() && roiCh0.size() == roiCh2.size()) {

            for (Map.Entry<Integer, ParticleInfo> entry : roiCh0.entrySet()) {
                int key = entry.getKey();
                ParticleInfo ch0Info = entry.getValue();
                ParticleInfo ch1Info = roiCh1.get(key);
                ParticleInfo ch2Info = roiCh2.get(key);

                //////////////
                double colocValue01 = Math
                        .abs(MAX_THERSHOLD - Math.abs(ch0Info.areaThersholdScale - ch1Info.areaThersholdScale));
                double colocValue02 = Math
                        .abs(MAX_THERSHOLD - Math.abs(ch0Info.areaThersholdScale - ch2Info.areaThersholdScale));
                double colocValue12 = Math
                        .abs(MAX_THERSHOLD - Math.abs(ch1Info.areaThersholdScale - ch2Info.areaThersholdScale));

                // Calculate a acg coloc. If all three channels coloc to 100% the value is 255^3
                // Scale this down to 255 as maximu val = (x*255)/(255*255*255) = x/(255*255)
                double colocValue = (colocValue01*colocValue02*colocValue12)/(MAX_THERSHOLD*MAX_THERSHOLD);
                //////////////

                double areaSize0OfPixles = (ch0Info.areaThersholdScale * ch0Info.areaSize) / MAX_THERSHOLD;
                double areaSize1OfPixles = (ch1Info.areaThersholdScale * ch1Info.areaSize) / MAX_THERSHOLD;
                double areaSize2OfPixles = (ch2Info.areaThersholdScale * ch2Info.areaSize) / MAX_THERSHOLD;

                double smallArea = areaSize0OfPixles;
                if (areaSize1OfPixles < smallArea) {
                    smallArea = areaSize1OfPixles;
                }

                ColocRoi exosom = new ColocRoi(key, smallArea, areaSize0OfPixles, areaSize1OfPixles, areaSize2OfPixles,
                        ch0Info.areaGrayScale, ch1Info.areaGrayScale, ch2Info.areaGrayScale, ch0Info.areaThersholdScale,
                        ch0Info.circularity,colocValue01,colocValue02,colocValue12, colocValue);
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
                double areaSizeCh2, double areaGrayScale, double areaGrayScale2, double areaGrayScale3,
                double areaThersholdScale, double circularity, double coloValue01, double coloValue02,
                double coloValue12, double coloValue) {
            super(roiName, smallestAreaSize, areaGrayScale, areaThersholdScale, circularity);
            this.colocValue = coloValue;
            this.colocValue01 = coloValue01;
            this.colocValue02 = coloValue02;
            this.colocValue12 = coloValue12;
            this.areaSizeCh0 = areaSizeCh0;
            this.areaSizeCh1 = areaSizeCh1;
            this.areaSizeCh2 = areaSizeCh2;
            this.areaGrayScale2ndChannel = areaGrayScale2;
            this.areaGrayScale3ndChannel = areaGrayScale3;

        }

        ///
        /// \brief Returns the name of the roi
        ///
        public String toString() {
            return Integer.toString(roiName) + ";" + Double.toString(areaSizeCh0) + ";" + Double.toString(areaSizeCh1)
                    + ";" + Double.toString(areaSizeCh2) + ";" + Double.toString(colocValue);
        }

        public double colocValue;
        public double colocValue01;
        public double colocValue02;
        public double colocValue12;
        public double areaSizeCh0;
        public double areaSizeCh1;
        public double areaSizeCh2;
        public double areaGrayScale2ndChannel;
        public double areaGrayScale3ndChannel;

        @Override
        public double[] getValues() {
            double[] values = { areaSizeCh0, areaSizeCh1, areaSizeCh2, areaGrayScale, areaGrayScale2ndChannel,
                    areaGrayScale3ndChannel, colocValue01, colocValue02, colocValue12, colocValue };
            return values;
        }

        public String[] getTitle() {
            String[] title = { "area size CH0", "area size CH1", "area size CH2", "intensity CH0", "intensity CH1",
                    "intensity CH2", "coloc CH01", "coloc CH02", "coloc CH12", "coloc CH012" };
            return title;
        }

        ///
        /// \breif check if this particle matches the filter criteria
        ///
        @Override
        public void validatearticle(double minAreaSize, double maxAreaSize, double minCircularity,
                double minGrayScale) {
            status = VALID;

            if ((areaSizeCh0 < minAreaSize && areaSizeCh0 != 0) || ((areaSizeCh1 < minAreaSize) && areaSizeCh1 != 0)
                    || ((areaSizeCh2 < minAreaSize) && areaSizeCh2 != 0)) {
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
            double intensityMean1 = 0;
            double intensityMean2 = 0;
            double intensityMean3 = 0;
            mColocNr = 0;

            for (Map.Entry<Integer, ParticleInfo> entry : ch.getRois().entrySet()) {
                int chNr = entry.getKey();
                ColocRoi info = (ColocRoi) entry.getValue();

                if (false == info.isValid()) {
                    nrOfInvalid++;
                } else {
                    if (info.colocValue > 0) {
                        mColocNr++;

                        intensityMean1 += info.areaGrayScale;
                        intensityMean2 += info.areaGrayScale2ndChannel;
                        intensityMean3 += info.areaGrayScale3ndChannel;
                    } else {
                        nrOfNotColoc++;
                    }
                    if (info.colocValue01 > 0) {
                        mColoc01Nr++;
                    }
                    if (info.colocValue02 > 0) {
                        mColoc02Nr++;
                    }
                    if (info.colocValue12 > 0) {
                        mColoc12Nr++;
                    }
                }
            }
            this.invalid = nrOfInvalid;
            this.valid = nrOfNotColoc;

            this.intensityMeanoOfColocCh0 = intensityMean1 / (double) mColocNr;
            this.intensityMeanoOfColocCh1 = intensityMean2 / (double) mColocNr;
            this.intensityMeanoOfColocCh2 = intensityMean3 / (double) mColocNr;
        }

        public double[] getValues() {
            double[] values = { mColocNr, mColoc01Nr, mColoc02Nr, mColoc12Nr, valid, invalid, intensityMeanoOfColocCh0,
                    intensityMeanoOfColocCh1, intensityMeanoOfColocCh2 };
            return values;
        }

        public String[] getTitle() {
            String[] title = { "coloc CH012", "coloc CH01", "coloc CH02", "coloc CH12", "Not coloc", "invalid",
                    "intensity CH0", "intensity CH1", "intensity CH2" };
            return title;
        }

        public int mColocNr = 0;
        public int mColoc01Nr = 0;
        public int mColoc02Nr = 0;
        public int mColoc12Nr = 0;

        public double intensityMeanoOfColocCh0 = 0;
        public double intensityMeanoOfColocCh1 = 0;
        public double intensityMeanoOfColocCh2 = 0;

    }

}
