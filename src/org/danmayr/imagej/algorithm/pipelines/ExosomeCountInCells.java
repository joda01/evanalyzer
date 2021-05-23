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
                CellShapeDetection(rm);

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
                        ImagePlus mask = Filter.AnalyzeParticles(evSubtracted, rm,0,-1,mSettings.mMinCircularity);
                        Filter.InvertImage(mask);
                        Filter.SaveImage(mask, getPath(mImage) + "_" + val.getValue().type.toString()+"_mask", rm);
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
        void CellShapeDetection(RoiManager rm) {
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
                        ImagePlus mask = Filter.AnalyzeParticles(cellsInEv, rm,0,-1,mSettings.mMinCircularity);
                        Filter.InvertImage(mask);
                        Filter.SaveImage(mask, getPath(mImage) + "_" + val.getValue().type.toString()+"_ev_in_cell_mask", rm);
                        Channel evsInCells = Filter.MeasureImage(0, val.getValue().type.toString() + " in Cell",
                                        mSettings, cellsInEv, cellsInEv, rm);
                        addReturnChannel(evsInCells);
                }
        }

        ///
        ///
        ///
        void NucleusSeparation() {

        }

        void addReturnChannel(Channel ch) {
                mReturnChannels.put(mReturnChannels.size(), ch);
        }

}
