package org.danmayr.imagej.algorithm.structs;

import java.awt.Rectangle;
import java.io.File;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.poi.sl.draw.binding.CTPath2D;
import org.danmayr.imagej.algorithm.AnalyseSettings;
import org.danmayr.imagej.algorithm.ChannelSettings;
import org.danmayr.imagej.algorithm.filters.ChannelInfoOverlaySettings;
import org.danmayr.imagej.algorithm.filters.Filter;
import org.danmayr.imagej.algorithm.filters.RoiOverlaySettings;
import org.danmayr.imagej.algorithm.pipelines.Pipeline.ChannelType;
import org.danmayr.imagej.algorithm.statistics.Statistics;
import org.danmayr.imagej.algorithm.statistics.StatisticsColoc;
import org.danmayr.imagej.algorithm.structs.Channel;
import org.danmayr.imagej.algorithm.structs.ParticleInfo;
import org.danmayr.imagej.algorithm.structs.ParticleInfoColoc;
import org.danmayr.imagej.performance_analyzer.PerformanceAnalyzer;
import java.awt.Color;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.measure.ResultsTable;
import ij.plugin.Colors;
import ij.plugin.frame.RoiManager;
import ij.process.AutoThresholder;

//
// Contains the result pictures from the counting
//
// bth = before thrshold
// ath = after thershold
public class ColocChannelSet {
    public ColocChannelSet(ImagePlus bth, ImagePlus ath, ChannelType t, Channel ch, ChannelSettings set) {
        this.imgeBeforeThershold = bth;
        this.imageAfterThershold = ath;
        this.type = t;
        this.ch = ch;
        this.set = set;
    }

    public ImagePlus imgeBeforeThershold;
    public ImagePlus imageAfterThershold;
    public ChannelType type;
    public Channel ch;
    public ChannelSettings set;
}
