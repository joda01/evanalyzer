package org.danmayr.imagej.algorithm.pipelines;

import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.measure.ResultsTable;
import ij.plugin.frame.RoiManager;

import java.io.File;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

import org.danmayr.imagej.algorithm.structs.*;
import org.danmayr.imagej.performance_analyzer.PerformanceAnalyzer;
import org.danmayr.imagej.algorithm.filters.Filter;

import org.danmayr.imagej.algorithm.AnalyseSettings;
import org.danmayr.imagej.algorithm.ChannelSettings;
import org.danmayr.imagej.algorithm.statistics.*;
import org.danmayr.imagej.algorithm.pipelines.*;

public class ExosomeCountInCells extends ExosomColoc {

        File mImage;
        static int MAX_THERSHOLD = 255;
        TreeMap<ChannelType, Channel> mReturnChannels;
        TreeMap<ChannelType, ChannelSettings> mEditedEvs = new TreeMap<ChannelType, ChannelSettings>();

        public ExosomeCountInCells(AnalyseSettings settings) {
                super(settings);
        }

        @Override
        protected TreeMap<ChannelType, Channel> startPipeline(File img) {
                mReturnChannels = new TreeMap<ChannelType, Channel>();
                mEditedEvs.clear();
                mImage = img;

                //System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "12");

                PerformanceAnalyzer.start("CntInCells:EvSeparation");
                EvSeparation();
                PerformanceAnalyzer.stop("CntInCells:EvSeparation");

                PerformanceAnalyzer.start("CntInCells:CellShapeDetection");
                ImagePlus cellArea = CellShapeDetection();
                PerformanceAnalyzer.stop("CntInCells:CellShapeDetection");

                PerformanceAnalyzer.start("CntInCells:NucleusSeparation");
                NucleusSeparation(cellArea);
                PerformanceAnalyzer.stop("CntInCells:NucleusSeparation");

                return mReturnChannels;
        }

        ///
        /// EV separation
        ///
        void EvSeparation() {
                // Vector<Thread> threads = new Vector<>();
                TreeMap<ChannelType, ChannelSettings> evs = getEvChannels();

                ExecutorService exec = Executors.newFixedThreadPool(evs.size());
                for (Map.Entry<ChannelType, ChannelSettings> val : evs.entrySet()) {
                        exec.execute(new EvSeperatorRunner(val));
                }
                exec.shutdown();
                try {
                        exec.awaitTermination(1, TimeUnit.HOURS);
                } catch (InterruptedException e) {
                        e.printStackTrace();
                }

        }

        class EvSeperatorRunner implements Runnable {

                Map.Entry<ChannelType, ChannelSettings> val;

                EvSeperatorRunner(Map.Entry<ChannelType, ChannelSettings> v) {
                        this.val = v;
                }

                @Override
                public void run() {
                        IJ.log(LocalTime.now() + " " + val.getValue().type.toString() + " - value: " + " - thread: "
                                        + Thread.currentThread().getName());
                        RoiManager rm = new RoiManager(false);

                        ImagePlus evOriginal = val.getValue().mChannelImg;
                        if (null != evOriginal) {
                                ImagePlus evEdited = Filter.duplicateImage(evOriginal);
                                Filter.Median(evEdited);

                                ImagePlus evSubtracted = Filter.SubtractImages(evOriginal, evEdited);
                                ImagePlus evSubtractedOriginal = Filter.duplicateImage(evSubtracted);
                                // Filter.SubtractBackground(evSubtracted);
                                // Filter.ApplyGaus(evSubtracted);
                                Filter.Smooth(evSubtracted);
                                double[] in = new double[2];
                                Filter.ApplyThershold(evSubtracted, val.getValue().mThersholdMethod,
                                                val.getValue().minThershold, val.getValue().maxThershold, in, true);
                                Filter.Watershed(evSubtracted); // Multi thread problem
                                ImagePlus mask = Filter.AnalyzeParticles(evSubtracted, rm, 0, -1,
                                                mSettings.mMinCircularity);

                                Filter.SaveImage(mask, getPath(mImage) + "_" + val.getValue().type.toString() + "_mask"+".jpg",
                                                rm);
                                Channel evCh = Filter.MeasureImage(val.getValue().type.toString(), mSettings,
                                                evSubtractedOriginal, evSubtracted, rm);
                                evCh.setThershold(in[0], in[1]);
                                addReturnChannel(evCh);
                                try {
                                        ChannelSettings setNew = (ChannelSettings) val.getValue().clone();
                                        setNew.mChannelImg = evSubtracted;
                                        mEditedEvs.put(val.getKey(), setNew);
                                } catch (CloneNotSupportedException e) {
                                        e.printStackTrace();
                                }

                        }

                }

        }

        ///
        /// Detect cells
        ///
        ImagePlus CellShapeDetection() {
                //
                // Detect Cell Area
                //
                ChannelSettings set = getImageOfChannel(ChannelType.CELL);
                if (null != set) {
                        RoiManager rm = new RoiManager(false);

                        ImagePlus cellsOriginal = set.mChannelImg;
                        ImagePlus cellsEdited = Filter.duplicateImage(cellsOriginal);

                        Filter.FindEdges(cellsEdited);
                        Filter.Smooth(cellsEdited);
                        Filter.Smooth(cellsEdited);
                        Filter.ApplyThershold(cellsEdited, set.mThersholdMethod);
                        Filter.Smooth(cellsEdited);
                        Filter.Smooth(cellsEdited);
                        Filter.Smooth(cellsEdited);
                        Filter.Smooth(cellsEdited);

                        // Filter.ApplyThershold(cellsEdited, set.mThersholdMethod);
                        // Filter.FillHoles(cellsEdited);
                        double[] in = new double[2];
                        Filter.ApplyThershold(cellsEdited, set.mThersholdMethod, set.minThershold, set.maxThershold, in,
                                        true);
                        Filter.AddThersholdToROI(cellsEdited, rm);

                        Filter.SaveImage(cellsEdited, getPath(mImage) + "_" + set.type.toString()+".jpg", rm);
                        Channel chCell = Filter.MeasureImage("Cell Area", null, cellsOriginal, cellsEdited, rm);
                        chCell.setThershold(in[0], in[1]);
                        addReturnChannel(chCell);

                        ExecutorService exec = Executors.newFixedThreadPool(mEditedEvs.size());
                        for (Map.Entry<ChannelType, ChannelSettings> val : mEditedEvs.entrySet()) {
                                exec.execute(new CellShapeDetectionRunner(val, rm, cellsEdited));
                        }
                        exec.shutdown();
                        try {
                                exec.awaitTermination(1, TimeUnit.HOURS);
                        } catch (InterruptedException e) {
                                e.printStackTrace();
                        }
                        return cellsEdited;
                }
                return null;

        }

        class CellShapeDetectionRunner implements Runnable {
                Map.Entry<ChannelType, ChannelSettings> val;
                RoiManager rm;
                ImagePlus cellsEdited;

                CellShapeDetectionRunner(Map.Entry<ChannelType, ChannelSettings> v, RoiManager manager,
                                ImagePlus editedCells) {
                        this.val = v;
                        this.rm = manager;
                        this.cellsEdited = editedCells;
                }

                @Override
                public void run() {
                        IJ.log(LocalTime.now() + " " + val.getValue().type.toString() + " - value: " + " - thread: "
                                        + Thread.currentThread().getName());

                        RoiManager rmEvs = new RoiManager(false);

                        ImagePlus evChannelImg = val.getValue().mChannelImg;
                        ImagePlus evChannelImgOriginal = getEvChannels().get(val.getKey()).mChannelImg;

                        Filter.SetRoiInImage(evChannelImgOriginal, rm, 0);
                        Filter.SetRoiInImage(evChannelImg, rm, 0);

                        //
                        // Calculate cell original thershold
                        //
                        Channel cellArea = Filter.MeasureImage("cell area in" + val.getValue().type.toString(), null,
                                        evChannelImgOriginal, evChannelImg, rm);
                        addReturnChannel(cellArea);

                        //
                        //
                        //
                        ImagePlus cellsInEv = Filter.ANDImages(cellsEdited, evChannelImg);
                        ImagePlus mask = Filter.AnalyzeParticles(cellsInEv, rmEvs, 0, -1, mSettings.mMinCircularity);
                        Filter.SaveImage(mask,
                                        getPath(mImage) + "_" + val.getValue().type.toString() + "_ev_in_cell_mask.jpg",
                                        rmEvs);
                        Channel evsInCells = Filter.MeasureImage(val.getValue().type.toString() + " in Cell", mSettings,
                                        evChannelImgOriginal, mask, rmEvs);
                        addReturnChannel(evsInCells);

                        Filter.ClearRoiInImage(evChannelImgOriginal);
                        Filter.ClearRoiInImage(evChannelImg);
                }
        }

        ///
        ///
        ///
        void NucleusSeparation(ImagePlus cells) {
                RoiManager rm = new RoiManager(false);
                ChannelSettings nuclues = getImageOfChannel(ChannelType.NUCLEUS);
                if (null != nuclues) {
                        ImagePlus nucluesOriginal = nuclues.mChannelImg;
                        ImagePlus nucluesEdited = Filter.duplicateImage(nucluesOriginal);
                        Filter.Smooth(nucluesEdited);
                        Filter.Smooth(nucluesEdited);
                        Filter.SubtractBackground(nucluesEdited);
                        Filter.ApplyThershold(nucluesEdited, nuclues.mThersholdMethod);
                        Filter.FillHoles(nucluesEdited);
                        ImagePlus nucleusMask = Filter.AnalyzeParticles(nucluesEdited, rm, 1000, -1, 0);
                        Filter.FillHoles(nucleusMask);
                        Filter.SaveImage(nucleusMask, getPath(mImage) + "_nucleus.jpg", rm);
                        Filter.Voronoi(nucleusMask);
                        // Filter.SaveImage(nucleusMask, getPath(mImage) + "_voronoi_original", rm);
                        Filter.ApplyThershold(nucleusMask, AutoThresholder.Method.Yen);
                        Filter.SaveImage(nucleusMask, getPath(mImage) + "_voronoi_grid.jpg", rm);
                        ImagePlus andImg = Filter.ANDImages(cells, nucleusMask);
                        ImagePlus separatedCells = Filter.XORImages(andImg, cells);
                        ImagePlus analyzedCells = Filter.AnalyzeParticles(separatedCells, rm, 2000, -1, 0);
                        Filter.SaveImage(analyzedCells, getPath(mImage) + "_separated_cells.jpg", rm);
                        IJ.log("Number of cells " + Integer.toString(rm.getCount()));

                        //
                        // Now analyze cell by cell
                        //
                        // Filter.RoiSave(analyzedCells, rm);
                        if(true == mSettings.mCountEvsPerCell){
                                for(Map.Entry<Pipeline.ChannelType,ChannelSettings> val : mEditedEvs.entrySet()){
                                        // mEditedEvs.entrySet().parallelStream().forEach((val) -> {
                                        ImagePlus evImg = val.getValue().mChannelImg;
                                        //ImagePlus evImgOri =getEvChannels().get(val.getKey());
                                        evImg.show();
                                        ResultsTable rt = new ResultsTable();

                                        //
                                        // Contains Cell information
                                        //
                                        String valueNames[] = { "area size", "intensity", "circularity", "valid", "invalid" };
                                        Channel evsInCell = new Channel("evs_per_cell_in_" + val.getKey().toString(),
                                                        new CellInfoStatistics(),valueNames,0);

                                        //
                                        // Measure the cell
                                        //
                                        Channel cellInfo = Filter.MeasureImage("cell_info", mSettings, evImg, evImg, rm);

                                        for (int n = 0; n < rm.getCount(); n++) { // Filter.RoiOpen(evImg, rm); rm.select(n);
                                                rt.reset();
                                                rm.selectAndMakeVisible(evImg, n);
                                                Filter.SetRoiInImage(evImg, rm, n);
                                                Filter.AnalyzeParticlesDoNotAdd(evImg, rm, 0, -1, 0, rt);
                                                Channel cell = Filter.createChannelFromMeasurement(
                                                                "evs_per_cell_in_" + Integer.toString(n), mSettings, rt, rt,rm);
                                                cell.calcStatistics();
                                                Statistics stat = cell.getStatistic();
                                                CellInfo info = new CellInfo(n, stat.valid, stat.invalid, cellInfo.getRois().get(n).areaSize,
                                                cellInfo.getRois().get(n).areaGrayScale, cellInfo.getRois().get(n).circularity);
                                                evsInCell.addRoi(info);
                                        }
                                        evsInCell.calcStatistics();
                                        addReturnChannel(evsInCell);
                                        evImg.hide();

                                }
                        }
                }
        }

        void addReturnChannel(Channel ch) {
                mReturnChannels.put(ChannelType.getColocEnum(mReturnChannels.size()), ch);
        }

        ///
        /// CellInfo class
        ///
        class CellInfo extends ParticleInfo {
                public CellInfo(int roiName, int valid, int invalid, double areaSize, double areaGrayScale,
                                double circularity) {
                        super(roiName, areaSize, areaGrayScale, 255, circularity);
                        this.valid = valid;
                        this.invalid = invalid;

                }

                ///
                /// \brief check if this particle is valid
                ///
                public boolean isValid() {
                        return true;
                }

                ///
                /// \brief Returns the name of the roi
                ///
                public String toString() {
                        return roiName + ";" + Double.toString(areaSize) + ";" + Double.toString(areaGrayScale) + ";"
                                        + Double.toString(areaThersholdScale) + ";" + Double.toString(circularity);
                }

                public int getRoiNr() {
                        return roiName;
                }

                public double[] getValues() {
                        double[] values = { areaSize, areaGrayScale, circularity, valid, invalid };
                        return values;
                }

                public String[] getTitle() {
                        String[] title = { "area size", "intensity", "circularity", "valid", "invalid" };
                        return title;
                }

                int valid = 0;
                int invalid = 0;
        }

        public class CellInfoStatistics extends Statistics {
                public CellInfoStatistics() {

                }

                public void setThershold(double minTH, double maxTH) {
                        this.minTH = minTH;
                        this.maxTH = maxTH;
                }

                public void calcStatistics(Channel ch) {
                        int nrOfInvalid = 0;
                        int nrOfValid = 0;
                        double areaSizeSum = 0;
                        double grayScaleSum = 0;
                        double circularitySum = 0;

                        for (Map.Entry<Integer, ParticleInfo> entry : ch.getRois().entrySet()) {
                                CellInfo info = (CellInfo) entry.getValue();

                                nrOfInvalid += info.invalid;
                                nrOfValid += info.valid;
                                areaSizeSum += info.areaSize;
                                grayScaleSum += info.areaGrayScale;
                                circularitySum += info.circularity;

                        }
                        this.avgAreaSize = areaSizeSum / ch.getRois().size();
                        this.avgGrayScale = grayScaleSum / ch.getRois().size();
                        this.avgCircularity = circularitySum / ch.getRois().size();
                        this.invalid = nrOfInvalid;
                        this.valid = nrOfValid;
                }

                public double[] getValues() {
                        // double[] values = { avgAreaSize, avgGrayScale, avgCircularity, valid, invalid
                        // };
                        double[] values = { valid, invalid };
                        return values;
                }

                public String[] getTitle() {
                        // String[] title = { "area size", "intensity", "circularity", "valid",
                        // "invalid" };
                        String[] title = { "valid", "invalid" };
                        return title;
                }

                public int valid;
                public int invalid;
                public double avgAreaSize;
                public double avgGrayScale;
                public double avgCircularity;
        }

}
