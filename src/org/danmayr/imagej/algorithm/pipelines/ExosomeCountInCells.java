package org.danmayr.imagej.algorithm.pipelines;

import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.plugin.frame.RoiManager;

import java.io.File;
import java.util.*;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.danmayr.imagej.algorithm.structs.*;
import org.danmayr.imagej.algorithm.filters.Filter;

import org.danmayr.imagej.algorithm.AnalyseSettings;
import org.danmayr.imagej.algorithm.ChannelSettings;
import org.danmayr.imagej.algorithm.statistics.*;
import org.danmayr.imagej.algorithm.pipelines.*;

public class ExosomeCountInCells extends ExosomColoc {

        File mImage;
        static int MAX_THERSHOLD = 255;
        TreeMap<Integer, Channel> mReturnChannels = new TreeMap<Integer, Channel>();
        TreeMap<ChannelType, ChannelSettings> mEditedEvs = new TreeMap<ChannelType, ChannelSettings>();

        public ExosomeCountInCells(AnalyseSettings settings) {
                super(settings);
        }

        @Override
        protected TreeMap<Integer, Channel> startPipeline(File img) {
                mImage = img;
                RoiManager rm = new RoiManager();

                EvSeparation(rm);
                ImagePlus cellArea = CellShapeDetection(rm);
                //NucleusSeparation(rm,cellArea);

                return mReturnChannels;
        }

        ///
        ///
        ///
        void EvSeparation(RoiManager rm) {

                TreeMap<ChannelType, ChannelSettings> evs = getEvChannels();
                for (Map.Entry<ChannelType, ChannelSettings> val : evs.entrySet()) {
                        ImagePlus evOriginal = val.getValue().mChannelImg;
                        ImagePlus evEdited = Filter.duplicateImage(evOriginal);
                        Filter.Median(evEdited);
                        ImagePlus evSubtracted = Filter.SubtractImages(evOriginal, evEdited);
                        ImagePlus evSubtractedOriginal = Filter.duplicateImage(evSubtracted);
                        Filter.SubtractBackground(evSubtracted);
                        Filter.ApplyGaus(evSubtracted);
                        Filter.ApplyThershold(evSubtracted, val.getValue().mThersholdMethod);
                        Filter.Watershed(evSubtracted);
                        ImagePlus mask = Filter.AnalyzeParticles(evSubtracted, rm, 0, -1, mSettings.mMinCircularity);
                        Filter.SaveImage(mask, getPath(mImage) + "_" + val.getValue().type.toString() + "_mask", rm);
                        Channel evCh = Filter.MeasureImage(0, val.getValue().type.toString(), mSettings,
                                        evSubtractedOriginal, evSubtracted, rm);
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

        ///
        /// Detect cells
        ///
        ImagePlus CellShapeDetection(RoiManager rm) {
                //
                // Detect Cell Area
                //
                ChannelSettings set = getImageOfChannel(ChannelType.CELL);
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
                Filter.ApplyThershold(cellsEdited, set.mThersholdMethod);
                Filter.AddThersholdToROI(cellsEdited, rm);
                Filter.SaveImage(cellsEdited, getPath(mImage) + "_" + set.type.toString(), rm);
                Channel chCell = Filter.MeasureImage(0, "Cell Area", null, cellsOriginal, cellsEdited, rm);
                addReturnChannel(chCell);

                //
                // Count EVS in Cells
                //
                for (Map.Entry<ChannelType, ChannelSettings> val : mEditedEvs.entrySet()) {
                        ImagePlus cellsInEv = Filter.ANDImages(cellsEdited, val.getValue().mChannelImg);
                        ImagePlus mask = Filter.AnalyzeParticles(cellsInEv, rm, 0, -1, mSettings.mMinCircularity);
                        Filter.SaveImage(mask,
                                        getPath(mImage) + "_" + val.getValue().type.toString() + "_ev_in_cell_mask",
                                        rm);
                        Channel evsInCells = Filter.MeasureImage(0, val.getValue().type.toString() + " in Cell",
                                        mSettings, cellsInEv, cellsInEv, rm);
                        addReturnChannel(evsInCells);
                }

                return cellsEdited;
        }

        ///
        ///
        ///
        void NucleusSeparation(RoiManager rm, ImagePlus cells) {
                ChannelSettings nuclues = getImageOfChannel(ChannelType.NUCLEUS);
                ImagePlus nucluesOriginal = nuclues.mChannelImg;
                ImagePlus nucluesEdited = Filter.duplicateImage(nucluesOriginal);
                Filter.Smooth(nucluesEdited);
                Filter.Smooth(nucluesEdited);
                Filter.SubtractBackground(nucluesEdited);
                Filter.ApplyThershold(nucluesEdited, nuclues.mThersholdMethod);
                Filter.FillHoles(nucluesEdited);
                ImagePlus nucleusMask = Filter.AnalyzeParticles(nucluesEdited, rm, 1000, -1, 0);
                Filter.FillHoles(nucleusMask);
                Filter.SaveImage(nucleusMask, getPath(mImage) +"_nucleus", rm);
                Filter.Voronoi(nucleusMask);
                Filter.InvertImage(nucleusMask);
                Filter.SaveImage(nucleusMask, getPath(mImage) +"_voronoi_original", rm);
                Filter.ApplyThershold(nucleusMask, "Yen");
                Filter.SaveImage(nucleusMask, getPath(mImage) +"_voronoi_grid", rm);
                ImagePlus andImg = Filter.ANDImages(cells, nucleusMask);
                ImagePlus separatedCells = Filter.XORImages(andImg, cells);
                ImagePlus analyzedCells = Filter.AnalyzeParticles(separatedCells, rm, 20, -1, 0);
                Filter.SaveImage(analyzedCells, getPath(mImage) +"_separated_cells", rm);

                //
                // Now analyze cell by cell
                //
                Filter.RoiSave(analyzedCells,rm);
                for (Map.Entry<ChannelType, ChannelSettings> val : mEditedEvs.entrySet()) {
                        ImagePlus evImg = val.getValue().mChannelImg;
                        for(int n = 0;n<rm.getCount();n++){
                                Filter.RoiOpen(evImg,rm);
                                rm.select(n);
                                ImagePlus analzedEvs = Filter.AnalyzeParticles(evImg, rm, 0, -1, 0);
                                Filter.SaveImage(analzedEvs, getPath(mImage) +"_evs_in_cell_"+Integer.toString(n), rm);
                        }
                }
        }

        void addReturnChannel(Channel ch) {
                mReturnChannels.put(mReturnChannels.size(), ch);
        }

}
