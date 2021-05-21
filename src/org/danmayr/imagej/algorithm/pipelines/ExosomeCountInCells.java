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

public class ExosomeCountInCells extends ExosomColoc {

        static int MAX_THERSHOLD = 255;
        TreeMap<Integer, Channel> mReturnChannels = new TreeMap<Integer, Channel>();

        public ExosomeCountInCells(AnalyseSettings settings) {
                super(settings);
        }

        @Override
        protected TreeMap<Integer, Channel> startPipeline(File img) {

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
                        Filter.ApplyThershold(evSubtracted, val.getValue().mThersholdMethod);
                        Filter.Watershed(evSubtracted);
                        Filter.AnalyzeParticles(evSubtracted, rm);
                        Channel evCh = Filter.MeasureImage(0, val.getValue().type.toString(), mSettings,
                                        evSubtractedOriginal, evSubtracted, rm);
                        addReturnChannel(evCh);
                }
        }

        ///
        /// Detect cells
        ///
        void CellShapeDetection(RoiManager rm) {
                ChannelSettings set = getImageOfChannel(ChannelType.CELL);
                ImagePlus cellsOriginal = set.mChannelImg;
                ImagePlus cellsEdited = Filter.duplicateImage(cellsOriginal);
                Filter.FindEdges(cellsEdited);
                Filter.ApplyThershold(cellsEdited, set.mThersholdMethod);
                Filter.ApplyGaus(cellsEdited);
                Filter.ApplyThershold(cellsEdited, set.mThersholdMethod);
                Filter.AddThersholdToROI(cellsEdited, rm);

                Channel chCell = Filter.MeasureImage(0, "Cell Area", null, cellsOriginal, cellsEdited, rm);
                addReturnChannel(chCell);

        }

        ///
        ///
        ///
        void NucleusSeparation() {

        }

        Channel CalcColoc(String name, int idx, RoiManager rm, ImagePlus img0, ImagePlus img1, ImagePlus img2,
                        ImagePlus img0Origial, ImagePlus img1Original, ImagePlus img2Original) {
                ImagePlus sumImageOriginal = Filter.ANDImages(img0Origial, img1Original);
                sumImageOriginal = Filter.ANDImages(sumImageOriginal, img2Original);

                ImagePlus sumImage = Filter.ANDImages(img0, img1);
                sumImage = Filter.ANDImages(sumImage, img2);

                Filter.AnalyzeParticles(sumImage, rm);
                Channel measColoc01 = Filter.MeasureImage(idx, name, mSettings, sumImageOriginal, sumImage, rm);
                return measColoc01;
        }

        void addReturnChannel(Channel ch) {
                mReturnChannels.put(mReturnChannels.size(), ch);
        }

}
