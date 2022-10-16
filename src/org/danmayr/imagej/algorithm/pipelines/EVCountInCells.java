package org.danmayr.imagej.algorithm.pipelines;

import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.measure.ResultsTable;
import ij.plugin.frame.RoiManager;

import java.awt.Color;
import java.io.File;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

import org.danmayr.imagej.algorithm.structs.*;
import org.danmayr.imagej.performance_analyzer.PerformanceAnalyzer;
import org.danmayr.imagej.algorithm.filters.Filter;
import org.danmayr.imagej.algorithm.filters.RoiOverlaySettings;
import org.danmayr.imagej.algorithm.AnalyseSettings;
import org.danmayr.imagej.algorithm.ChannelSettings;
import org.danmayr.imagej.algorithm.statistics.*;
import org.danmayr.imagej.algorithm.pipelines.*;

public class EVCountInCells extends EVColoc {

    static int MAX_THERSHOLD = 255;
    TreeMap<ChannelType, Channel> mReturnChannels;
    TreeMap<ChannelType, ChannelSettings> mEditedEvs = new TreeMap<ChannelType, ChannelSettings>();
    TreeMap<ChannelType, ColocChannelSet> colocChannels = new TreeMap<>();
    TreeMap<ChannelType, ColocChannelSet> colocChannelsEvInCells = new TreeMap<>();

    public EVCountInCells(AnalyseSettings settings, int parallelWorkers) {
        super(settings,parallelWorkers);
    }

    @Override
    protected TreeMap<ChannelType, Channel> startPipeline(File img) {
        mReturnChannels = new TreeMap<ChannelType, Channel>();
        mEditedEvs.clear();
        colocChannels.clear();
        colocChannelsEvInCells.clear();
        mImage = img;

        // System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism",
        // "12");

        PerformanceAnalyzer.start("CntInCells:EvSeparation");
        EvSeparation();
        PerformanceAnalyzer.stop("CntInCells:EvSeparation");

        PerformanceAnalyzer.start("CntInCells:executeColocAlgorithm");
        EVColoc("coloc_all_evs", colocChannels);
        PerformanceAnalyzer.stop("CntInCells:executeColocAlgorithm");

        PerformanceAnalyzer.start("CntInCells:CellShapeDetection");
        ImagePlus cellArea = CellShapeDetection();
        PerformanceAnalyzer.stop("CntInCells:CellShapeDetection");

        PerformanceAnalyzer.start("CntInCells:NucleusSeparation");
        NucleusSeparation(cellArea);
        PerformanceAnalyzer.stop("CntInCells:NucleusSeparation");

        PerformanceAnalyzer.start("CntInCells:executeColocAlgorithmForEvsInCell");
        EVColoc("coloc_incell_evs", colocChannelsEvInCells);
        PerformanceAnalyzer.stop("CntInCells:executeColocAlgorithmForEvsInCell");

        for (Map.Entry<ChannelType, Channel> e : mReturnChannels.entrySet()) {
            e.getValue().ClearRoi();
        }

        colocChannelsEvInCells.clear();
        colocChannels.clear();
        mEditedEvs.clear();

        return mReturnChannels;
    }

    ///
    /// EV Coloc
    ///
    void EVColoc(String title, TreeMap<ChannelType, ColocChannelSet> evChannelsToAnalyze) {
        TreeMap<ChannelType, Channel> ret = executeColocAlgorithm(title, mImage, evChannelsToAnalyze);
        int startIdx = mReturnChannels.size();
        for (Map.Entry<ChannelType, Channel> val : ret.entrySet()) {
            ChannelType type = ChannelType
                    .getColocEnum(val.getKey().idx + startIdx + ChannelType.getFirstFreeChannel() * 2);

            addReturnChannel(val.getValue(), type, val.getValue().getCtrlImagePath());
        }
    }

    ///
    /// EV separation
    ///
    void EvSeparation() {
        // Vector<Thread> threads = new Vector<>();
        TreeMap<ChannelType, ChannelSettings> evs = getEvChannels();

        ExecutorService exec = Executors.newFixedThreadPool(PARALLEL_WORKERS);
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

            RoiManager rm = new RoiManager(false);

            ImagePlus evOriginal = val.getValue().getImage();
            if (null != evOriginal) {
                ImagePlus evEdited = Filter.duplicateImage(evOriginal);
                Filter.Median(evEdited);

                ImagePlus evSubtracted = Filter.SubtractImages(evOriginal, evEdited);
                ImagePlus evSubtractedOriginal = Filter.duplicateImage(evSubtracted);
                // Filter.RollingBall(evSubtracted);
                // Filter.ApplyGaus(evSubtracted);
                Filter.Smooth(evSubtracted);
                double[] in = new double[2];
                Filter.ApplyThershold(evSubtracted, val.getValue().threholdMethod(), val.getValue().minThershold(),
                        val.getValue().maxThershold(), in, true);
                Filter.Watershed(evSubtracted); // Multi thread problem
                Filter.AnalyzeParticles(evSubtracted, rm, 0, -1,
                        val.getValue().getMinCircularityDouble());

                // Save control images of EV channels

                Filter.SaveImage(evSubtracted, getPath(mImage, "01", val.getValue().getType().toString() + "_ev"), rm);

                Channel evCh = Filter.MeasureImage(val.getValue().getType().toString(), mSettings, val.getValue(),
                        evSubtractedOriginal, evSubtracted, rm, true);
                evCh.setThershold(in[0], in[1]);
                addReturnChannel(evCh, val.getKey(),
                        getRelativeImagePath(mImage, "01", val.getValue().getType().toString() + "_ev"));
                try {
                    ChannelSettings setNew = (ChannelSettings) val.getValue().clone(evSubtracted);
                    mEditedEvs.put(val.getKey(), setNew);
                    colocChannels.put(val.getValue().getType(),
                            new ColocChannelSet(evSubtractedOriginal, evSubtracted, val.getValue().getType(), evCh,
                                    val.getValue()));
                } catch (CloneNotSupportedException e) {
                    e.printStackTrace();
                    IJ.log("ERR");
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
        ChannelSettings cellChannelSetting = getCellChannel();
        if (null != cellChannelSetting) {
            RoiManager rm = new RoiManager(false);

            ImagePlus cellsOriginal = cellChannelSetting.getImage();
            ImagePlus cellsEdited = Filter.duplicateImage(cellsOriginal);

            // For labeled cells -->
            if (cellChannelSetting.getType() == ChannelType.CELL_FLUORESCENCE) {
                Filter.FindEdges(cellsEdited);
                Filter.RollingBall(cellsEdited);
            }
            // <--

            Filter.FindEdges(cellsEdited);
            Filter.Smooth(cellsEdited);
            Filter.Smooth(cellsEdited);
            double[] in = new double[2];
            Filter.ApplyThershold(cellsEdited, cellChannelSetting.threholdMethod(), cellChannelSetting.minThershold(),
                    cellChannelSetting.maxThershold(), in, true);
            Filter.Smooth(cellsEdited);
            Filter.Smooth(cellsEdited);
            Filter.Smooth(cellsEdited);
            Filter.Smooth(cellsEdited);

            if (cellChannelSetting.getType() == ChannelType.CELL_FLUORESCENCE) {
                Filter.Smooth(cellsEdited);
                Filter.Smooth(cellsEdited);
                Filter.Smooth(cellsEdited);
                Filter.Smooth(cellsEdited);
                Filter.Smooth(cellsEdited);
                Filter.Smooth(cellsEdited);
            }

            // Filter.ApplyThershold(cellsEdited, set.mThersholdMethod);
            // Filter.FillHoles(cellsEdited);
            Filter.ApplyThershold(cellsEdited, AutoThresholder.Method.Default, 4, 255, null, true);
            Filter.AddThersholdToROI(cellsEdited, rm);

            Filter.SaveImage(cellsEdited, getPath(mImage, "02", cellChannelSetting.getType().toString() + "_cell_area"),
                    rm);

            Channel chCell = Filter.MeasureImage("Cell Area", mSettings, cellChannelSetting, cellsOriginal, cellsEdited,
                    rm, false);
            chCell.setThershold(in[0], in[1]);
            addReturnChannel(chCell, cellChannelSetting.getType(), getRelativeImagePath(mImage, "02", cellChannelSetting.getType().toString() + "_cell_area"));

            ExecutorService exec = Executors.newFixedThreadPool(PARALLEL_WORKERS);
            for (Map.Entry<ChannelType, ChannelSettings> val : mEditedEvs.entrySet()) {
                exec.execute(new CellShapeDetectionRunner(val, rm, cellsEdited, cellChannelSetting));
            }
            exec.shutdown();
                try {
                exec.awaitTermination(1, TimeUnit.HOURS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return cellsEdited;
        } else {
            IJ.log("NULL");
        }
        return null;

    }

    class CellShapeDetectionRunner implements Runnable {
        Map.Entry<ChannelType, ChannelSettings> evChannel;
        RoiManager cellArea;
        ImagePlus cellsEdited;
        ChannelSettings cellChannelSettings;

        CellShapeDetectionRunner(Map.Entry<ChannelType, ChannelSettings> evChannel, RoiManager cellArea,
                ImagePlus editedCells, ChannelSettings cellChannelSettings) {
            this.evChannel = evChannel;
            this.cellArea = cellArea;
            this.cellsEdited = editedCells;
            this.cellChannelSettings = cellChannelSettings;
        }

        @Override
        public void run() {
            /*
             * IJ.log(LocalTime.now() + " " + evChannel.getValue().getType().toString() +
             * " - value: " + " - thread: " + Thread.currentThread().getName());
             */

            RoiManager rmEvs = new RoiManager(false);

            ImagePlus evChannelImg = evChannel.getValue().getImage();
            ImagePlus evChannelImgOriginal = getEvChannels().get(evChannel.getKey()).getImage();

            Filter.SetRoiInImage(evChannelImgOriginal, cellArea, 0);
            Filter.SetRoiInImage(evChannelImg, cellArea, 0);

            //
            // Calculate cell original thershold
            //
            Channel cellAreaChannel = Filter.MeasureImage("cell area in" + evChannel.getValue().getType().toString(),
                    mSettings,
                    cellChannelSettings, evChannelImgOriginal, evChannelImg, cellArea, false);

            ChannelType cellAreaEvType = ChannelType.getColocEnum(evChannel.getValue().getType().idx);

            addReturnChannel(cellAreaChannel, cellAreaEvType, "");

            //
            //
            //
            ImagePlus cellsInEv = Filter.ANDImages(cellsEdited, evChannelImg);
            ImagePlus mask = Filter.AnalyzeParticles(cellsInEv, rmEvs, 0, -1,
                    evChannel.getValue().getMinCircularityDouble());

            // Save control image
            // Filter.SaveImage(mask, getPath(mImage) + "_" +
            // evChannel.getValue().getType().toString() + "_ev_in_cell_mask.jpg", rmEvs);

            Channel evsInCells = Filter.MeasureImage(evChannel.getValue().getType().toString() + " in Cell", mSettings,
                    evChannel.getValue(), evChannelImgOriginal, mask, rmEvs, true);

            ChannelType inCellEvType = ChannelType
                    .getColocEnum(evChannel.getValue().getType().idx + ChannelType.getFirstFreeChannel());

            addReturnChannel(evsInCells, inCellEvType, "");

            colocChannelsEvInCells
                    .put(evChannel.getKey(),
                            new ColocChannelSet(evChannelImgOriginal, mask, evChannel.getValue().getType(), evsInCells,
                                    evChannel.getValue()));

            Filter.ClearRoiInImage(evChannelImgOriginal);
            Filter.ClearRoiInImage(evChannelImg);
        }
    }

    ///
    ///
    ///
    void NucleusSeparation(ImagePlus cells) {
        RoiManager nucleusRoiAll = new RoiManager(false); // All nucleus
        RoiManager nucleusRoiFiltered = new RoiManager(false); // Nuclues with all nucleus removed on edge

        RoiManager cellRoi = new RoiManager(false);
        ChannelSettings nuclues = getImageOfChannel(ChannelType.NUCLEUS);
        if (null != nuclues) {
            ImagePlus nucluesOriginal = nuclues.getImage();
            ImagePlus nucluesEdited = Filter.duplicateImage(nucluesOriginal);
            Filter.Smooth(nucluesEdited);
            Filter.Smooth(nucluesEdited);
            Filter.RollingBall(nucluesEdited);
            Filter.ApplyThershold(nucluesEdited, nuclues.threholdMethod());
            Filter.FillHoles(nucluesEdited);

            //
            // Find all nuclues
            //
            ImagePlus nucleusMask = Filter.AnalyzeParticles(nucluesEdited, nucleusRoiAll, 1000, -1,
                    nuclues.getMinCircularityDouble());
            Filter.FillHoles(nucleusMask);
            // Filter.SaveImage(nucleusMask, getPath(mImage) + "_nucleus.jpg",
            // nucleusRoiAll);

            // Create voronoi grid which is used for cell separation
            Filter.Voronoi(nucleusMask);

            //
            // Remove all nucleus on edge
            //
            ImagePlus nucleusMaskFiltered = Filter.AnalyzeParticles(nucluesEdited, nucleusRoiFiltered, 1000, -1,
                    nuclues.getMinCircularityDouble(), true, null, mSettings.removeCellsWithoutNucleus());
            Filter.FillHoles(nucleusMaskFiltered);
            /*
             * Filter.SaveImage(nucleusMaskFiltered, getPath(mImage) +
             * "_nucleus_filtered.jpg", nucleusRoiFiltered);
             */

            // Filter.SaveImage(nucleusMask, getPath(mImage) + "_voronoi_original", rm);
            Filter.ApplyThershold(nucleusMask, AutoThresholder.Method.Yen);
            // Filter.SaveImage(nucleusMask, getPath(mImage) + "_voronoi_grid.jpg",
            // nucleusRoiAll);

            // Cut the cell area with the voronoi grid to get separated cells
            ImagePlus andImg = Filter.ANDImages(cells, nucleusMask);
            ImagePlus separatedCells = Filter.XORImages(andImg, cells);
            ImagePlus analyzedCells = Filter.AnalyzeParticles(separatedCells, cellRoi, 2000, -1, 0);
            // Filter.SaveImage(analyzedCells, getPath(mImage) + "_separated_cells.jpg",
            // cellRoi);

            RoiManager filteredCells = new RoiManager(false);
            // Only use the cells which have a nucleus
            for (int c = cellRoi.getCount() - 1; c >= 0; c--) {
                for (int n = 0; n < nucleusRoiFiltered.getCount(); n++) {
                    if (true == cellRoi.getRoi(c).getBounds().intersects(nucleusRoiFiltered.getRoi(n).getBounds())) {
                        Roi result = Filter.and(cellRoi.getRoi(c), nucleusRoiFiltered.getRoi(n));
                        if (result != null && result.size() > 0) {
                            // Cell has nucles. Add cell to the ROIManager for cell
                            // analyzing
                            filteredCells.addRoi(cellRoi.getRoi(c));
                            result = null;
                            break;
                        }
                        result = null;
                    }
                }
            }

            cellRoi = filteredCells;

            //
            // Now analyze cell by cell
            //
            // Filter.RoiSave(analyzedCells, rm);
            if (true == mSettings.countEvsPerCell()) {
                // CellByCellEvCouting

                ExecutorService exec = Executors.newFixedThreadPool(PARALLEL_WORKERS);
                for (Map.Entry<Pipeline.ChannelType, ChannelSettings> val : mEditedEvs.entrySet()) {
                    exec.execute(new CellByCellEvCountingAndAnalyzing(val, cellRoi, nucleusRoiAll, nucleusRoiFiltered,
                            analyzedCells));
                }
                // IJ.log("Wait for finsihed");
                exec.shutdown();
                    try {
                    exec.awaitTermination(1, TimeUnit.HOURS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            // New
            filteredCells = null;

        }
        // new
        cellRoi = null;
        nucleusRoiAll = null;
        nucleusRoiFiltered = null;

    }

    class CellByCellEvCountingAndAnalyzing implements Runnable {

        Map.Entry<Pipeline.ChannelType, ChannelSettings> val;
        RoiManager cellRoi;
        RoiManager nucleusRoiAll;
        RoiManager nucleusRoiFiltered;
        ImagePlus analyzedCells;

        CellByCellEvCountingAndAnalyzing(Map.Entry<Pipeline.ChannelType, ChannelSettings> val, RoiManager cellRoi,
                RoiManager nucleusRoiAll, RoiManager nucleusRoiFiltered, ImagePlus analyzedCells) {
            this.val = val;
            this.cellRoi = cellRoi;
            this.nucleusRoiAll = nucleusRoiAll;
            this.nucleusRoiFiltered = nucleusRoiFiltered;
            this.analyzedCells = analyzedCells;
        }

        @Override
        public void run() {
            // TODO Auto-generated method stub
            // mEditedEvs.entrySet().parallelStream().forEach((val) -> {
            ImagePlus evImg = val.getValue().getImage();
            // ImagePlus evImgOri =getEvChannels().get(val.getKey());
            // evImg.show();
            ResultsTable rt = new ResultsTable();

            //
            // Contains Cell information
            //
            String valueNames[] = { "area size [µm²]", "intensity", "circularity [0-1]", "valid", "invalid" };
            Channel evsInCell = new Channel("evs_per_cell_in_" + val.getKey().toString(),
                    new CellInfoStatistics(), valueNames, 0);

            //
            // Measure the cell
            //
            Channel cellInfo = Filter.MeasureImage("cell_info", mSettings, val.getValue(), evImg, evImg,
                    cellRoi, true);

            //
            // Prepare overlays
            //
            Vector<RoiOverlaySettings> roiOverLay = new Vector<RoiOverlaySettings>();
            roiOverLay.add(new RoiOverlaySettings(nucleusRoiAll, Color.BLACK, false));
            roiOverLay.add(new RoiOverlaySettings(nucleusRoiFiltered, Color.RED, false));
            roiOverLay.add(new RoiOverlaySettings(cellRoi, Color.RED, true));

            for (int n = 0; n < cellRoi.getCount(); n++) { // Filter.RoiOpen(evImg, rm);
                                                           // rm.select(n);
                rt.reset();
                // cellRoi.selectAndMakeVisible(evImg, n);
                Filter.SetRoiInImage(evImg, cellRoi, n);
                RoiManager evsInCellroi = new RoiManager(false);
                Filter.AnalyzeParticles(evImg, evsInCellroi, 0, -1, 0, rt);
                roiOverLay.add(new RoiOverlaySettings(evsInCellroi, Color.RED, false));
                Channel cell = Filter.createChannelFromMeasurement("evs_per_cell_in_" + Integer.toString(n),
                        mSettings, val.getValue(), rt, rt, cellRoi, true);
                cell.calcStatistics();
                Statistics stat = cell.getStatistic();
                CellInfo info = new CellInfo(n, stat.valid, stat.invalid, cellInfo.getRois().get(n).areaSize,
                        cellInfo.getRois().get(n).areaGrayScale, cellInfo.getRois().get(n).circularity);
                evsInCell.addRoi(info);
                // IJ.log("Cell: " + n);
            }

            evsInCell.calcStatistics();
            // evImg.hide();

            ChannelType inCellEvType = ChannelType
                    .getColocEnum(val.getKey().idx + ChannelType.getFirstFreeChannel() * 2);
            Filter.SaveImageWithOverlay(analyzedCells, roiOverLay,
                    getPath(mImage, "03", val.getKey().toString() + "_separated_overlay_cells"));

            roiOverLay.clear();
            addReturnChannel(evsInCell, inCellEvType, getRelativeImagePath(mImage, "03", val.getKey().toString() + "_separated_overlay_cells"));

        }
    }

    synchronized void addReturnChannel(Channel ch, ChannelType type, String pathToCtrlImage) {
        if (ch != null && type != null) {
            ch.addControlImagePath(pathToCtrlImage);
            mReturnChannels.put(type, ch);
        }
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

        public int getRoiNr() {
            return roiName;
        }

        public double[] getValues() {
            double[] values = { areaSize, areaGrayScale, circularity, valid, invalid };
            return values;
        }

        public String[] getTitle() {
            String[] title = { "area size [µm²]", "intensity", "circularity [0-1]", "valid", "invalid" };
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
            this.nrOfCells = ch.getRois().size();
            this.avgAreaSize = areaSizeSum / ch.getRois().size();
            this.avgGrayScale = grayScaleSum / ch.getRois().size();
            this.avgCircularity = circularitySum / ch.getRois().size();
            this.invalid = nrOfInvalid;
            this.valid = nrOfValid;
            this.avgEvsPerCell = (double) this.valid / (double) ch.getRois().size();
            this.avgInvalidEvsPerCell = (double) this.invalid / (double) ch.getRois().size();
        }

        public double[] getValues() {
            // double[] values = { avgAreaSize, avgGrayScale, avgCircularity, valid, invalid
            // };
            double[] values = { nrOfCells, avgEvsPerCell, avgInvalidEvsPerCell };
            return values;
        }

        public String[] getTitle() {
            // String[] title = { "area size", "intensity", "circularity", "valid",
            // "invalid" };
            String[] title = { "nr. of cells", "avg. evs per cell", "avg. invalid evs per cell" };
            return title;
        }

        public int nrOfCells;
        public int valid;
        public int invalid;
        public double avgEvsPerCell;
        public double avgInvalidEvsPerCell;
        public double avgAreaSize;
        public double avgGrayScale;
        public double avgCircularity;
    }

}
