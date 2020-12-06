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

public class ExosomColoc extends Pipeline {

    RoiManager rm = new RoiManager();
    static int MAX_THERSHOLD = 255;

    public ExosomColoc(AnalyseSettings settings, ChannelType ch0, ChannelType ch1) {
        super(settings, ch0, ch1);
    }

    @Override
    protected TreeMap<Integer, Channel> startPipeline(File img) {
       

        ImagePlus img0 = Filter.SubtractBackground(getImageCh0());
        ImagePlus img1 = Filter.SubtractBackground(getImageCh1());

        img0 = Filter.ApplyGaus(img0);
        img1 = Filter.ApplyGaus(img1);

        img0 = Filter.ApplyThershold(img0, mSettings.mThersholdMethod);
        img1 = Filter.ApplyThershold(img1, mSettings.mThersholdMethod);

        ImagePlus sumImage = Filter.AddImages(img0, img1);

        Filter.AnalyzeParticles(sumImage);
        TreeMap<Integer, Channel> channels = new TreeMap<Integer, Channel>();

        Channel measCh0 = Filter.MeasureImage(0, "ch0", img0, rm);
        Channel measCh1 = Filter.MeasureImage(1, "ch1", img1, rm);
        Channel measColoc = calculateColoc(measCh0, measCh1);

        channels.put(0, measCh0);
        channels.put(1, measCh1);
        channels.put(2, measColoc);

        return channels;
    }

    private Channel calculateColoc(Channel ch0, Channel ch1) {
        Channel ch = new Channel(2, "Coloc");

        TreeMap<Integer, ParticleInfo> roiCh0 = ch0.getRois();
        TreeMap<Integer, ParticleInfo> roiCh1 = ch1.getRois();

        if (roiCh0.size() == roiCh1.size()) {

            for (Map.Entry<Integer, ParticleInfo> entry : roiCh0.entrySet()) {
                int key = entry.getKey();
                ParticleInfo ch0Info = entry.getValue();
                ParticleInfo ch1Info = roiCh1.get(key);

                double colocValue = Math.abs(MAX_THERSHOLD - Math.abs(ch0Info.areaGrayScale - ch1Info.areaGrayScale));

                ColocRoi exosom = new ColocRoi(key, ch0Info.areaSize, ch0Info.areaGrayScale, ch0Info.circularity, colocValue);
                ch.addRoi(exosom);
            }
        } else {
            IJ.log("calculateColoc ROI size not equal.");
        }

        return ch;
    }

    class ColocRoi extends ParticleInfo {

        public ColocRoi(int roiName, double areaSize, double areaGrayScale, double circularity, double coloValue) {
            super(roiName, areaSize, areaGrayScale, circularity);
            this.colocValue = coloValue;
        }

        ///
        /// \brief Returns the name of the roi
        ///
        public String toString() {
            return Integer.toString(roiName) + ";" + Double.toString(areaSize) + ";"
                    + Double.toString(areaGrayScale) + ";" + Double.toString(circularity) + ";"
                    + Double.toString(colocValue);
        }

        public double colocValue;

        @Override
        public double[] getValues(){
            double[] values={areaSize,areaGrayScale,circularity,colocValue};
            return values;
        }

        public String[] getTitle(){
            String[] title = {"area size","gray scale","circularity","coloc"};
            return title;
        }

    }

}
