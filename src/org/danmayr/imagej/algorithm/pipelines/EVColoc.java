package org.danmayr.imagej.algorithm.pipelines;

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
import org.danmayr.imagej.algorithm.statistics.Statistics;
import org.danmayr.imagej.algorithm.statistics.StatisticsColoc;
import org.danmayr.imagej.algorithm.structs.Channel;
import org.danmayr.imagej.algorithm.structs.ColocChannelSet;
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

public class EVColoc extends Pipeline {

    static int MAX_THERSHOLD = 255;

    public EVColoc(AnalyseSettings settings) {
        super(settings);
    }

    TreeMap<ChannelType, Channel> channels;
    Vector<ColocChannelSet> colocChannels = new Vector<>();
    ImagePlus background = null;

    protected File mImage;

    @Override
    protected TreeMap<ChannelType, Channel> startPipeline(File img) {
        mImage = img;
        background = null;
        channels = new TreeMap<ChannelType, Channel>();
        colocChannels.clear();
        Channel channelWithTetraSpeckBeats = null;
        if (null != getBackground()) {
            background = getBackground().mChannelImg;
            Filter.NormalizeHistogram(background);

        }

        if (null != getTetraSpeckBead()) {
            channelWithTetraSpeckBeats = FindTetraspeckBeads(getTetraSpeckBead());
        }

        //
        // Count EVS and create threshold pictures
        //
        TreeMap<ChannelType, ChannelSettings> evs = getEvChannels();
        ExecutorService exec = Executors.newFixedThreadPool(evs.size());
        for (Map.Entry<ChannelType, ChannelSettings> val : evs.entrySet()) {
            exec.execute(new EvCounting(channelWithTetraSpeckBeats, val));
        }
        exec.shutdown();
        try {
            exec.awaitTermination(1, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (true == mSettings.mCalcColoc) {
            TreeMap<ChannelType, Channel> ret = executeColocAlgorithm("coloc", mImage, colocChannels);
            channels.putAll(ret);
        }

        for (Map.Entry<ChannelType, Channel> e : channels.entrySet()) {
            e.getValue().ClearRoi();
        }

        return channels;
    }

    protected static TreeMap<ChannelType, Channel> executeColocAlgorithm(String title, File file,
            Vector<ColocChannelSet> channelsToAnalyze) {

        TreeMap<ChannelType, Channel> channelsOut = new TreeMap<ChannelType, Channel>();
        //
        // Calc Coloc for two channel combinations (0 - 1 | 0 - 2 | 1 - 2)
        //

        ColocChannelSet colocAll = null;
        int colocEnum = 0;
        for (int n = 0; n < channelsToAnalyze.size(); n++) {
            for (int m = n + 1; m < channelsToAnalyze.size(); m++) {
                ColocChannelSet img0 = channelsToAnalyze.get(n);
                ColocChannelSet img1 = channelsToAnalyze.get(m);
                Vector<ColocChannelSet> pics2Ch = new Vector<ColocChannelSet>();
                pics2Ch.add(img0);
                pics2Ch.add(img1);
                ColocChannelSet coloc02 = calculateRoiColoc(title, file, img0, img1);
                channelsOut.put(ChannelType.getColocEnum(colocEnum), coloc02.ch);
                colocEnum++;

                //
                // For multi channel coloc
                //
                colocAll = calculateRoiColoc(title, file, colocAll, coloc02);
            }
        }

        //
        // Save control images for all channels
        //
        Vector<ChannelInfoOverlaySettings> channelsToPrint = new Vector<ChannelInfoOverlaySettings>();
        for (Map.Entry<ChannelType, Channel> val : channelsOut.entrySet()) {
            channelsToPrint.add(
                    new ChannelInfoOverlaySettings(val.getValue().getRois(), val.getKey().getColor(), false, true));
        }
        if (null != colocAll && null != colocAll.ch) {
            channelsToPrint
                    .add(new ChannelInfoOverlaySettings(colocAll.ch.getRois(), new Color(255, 255, 255, 80), false,
                            true));
            String imageName = getName(file) + "_all_coloc.jpg";
            String imagePath = getPath(file) + "_all_coloc.jpg";
            Filter.SaveImageWithOverlayFromChannel(colocAll.imageAfterThershold, channelsToPrint, imagePath);

            // Only add to output if there are more than two channels. Else it would be
            // redundat
            if (channelsToAnalyze.size() > 2) {
                colocAll.ch.addControlImagePath(imageName);
                channelsOut.put(ChannelType.COLOC_ALL, colocAll.ch);
            }
        }
        return channelsOut;
    }

    //
    // Calculate the coloc by taking a ROI in channel 1 and looking for a ROI in
    // channel 2 which
    // has an intersection. All the ROIs with intersection are added to the ROI
    // coloc channel
    //
    protected static ColocChannelSet calculateRoiColoc(String title, File file, ColocChannelSet ch1,
            ColocChannelSet ch2) {
        if (ch1 != null && ch2 != null) {
            String valueNames[] = { "coloc area", "coloc factor", "coloc circularity", "coloc validity",
                    "intensity " + ch1.type.toString(),
                    "intensity " + ch2.type.toString(), "area " + ch1.type.toString(), "area " + ch2.type.toString() };

            String name = title + "_" + ch1.type.toString() + "_with_" + ch2.type.toString();
            Channel coloc = new Channel(name, new StatisticsColoc(), valueNames, 3);
            TreeMap<Integer, ParticleInfo> roiPic1 = ch1.ch.getRois();
            TreeMap<Integer, ParticleInfo> roiPic2 = ch2.ch.getRois();

            // Select setting of highest circularity of both channels
            ChannelSettings chSet = ch1.set.getMinCircularityDouble() > ch2.set.getMinCircularityDouble() ? ch1.set
                    : ch2.set;

            double circularityFilter = chSet.getMinCircularityDouble();
            double minParticleSize = chSet.getMinParticleSizeDouble();
            double maxParticleSize = chSet.getMaxParticleSizeDouble();

            int colocNr = 0;

            for (Map.Entry<Integer, ParticleInfo> particle1 : roiPic1.entrySet()) {
                for (Map.Entry<Integer, ParticleInfo> particle2 : roiPic2.entrySet()) {
                    Roi result = particle1.getValue().isPartOf(particle2.getValue());
                    if (null != result) {
                        result.setImage(ch1.imageAfterThershold);
                        int size = result.getContainedPoints().length;

                        if (size > 0) {
                            //
                            // Calculate coloc factor
                            //
                            double colocFactor = size / particle1.getValue().areaSize;
                            double colocFactor2 = size / particle2.getValue().areaSize;
                            if (colocFactor2 > colocFactor) {
                                colocFactor = colocFactor2;
                            }
                            colocFactor *= 100;
                            if (colocFactor >= mSettings.mMinColocFactor) {
                                //
                                // Particles have an intersection!!
                                //

                                //
                                // Calculate circularity
                                //
                                double perimeter = result.getLength();
                                double circularity = perimeter == 0.0 ? 0.0
                                        : 4.0 * Math.PI * (result.getStatistics().area / (perimeter * perimeter));
                                if (circularity > 1.0) {
                                    circularity = 1.0;
                                }

                                double[] intensityChannels = { particle1.getValue().areaGrayScale,
                                        particle2.getValue().areaGrayScale };
                                double[] areaChannels = { particle1.getValue().areaSize,
                                        particle2.getValue().areaSize };
                                double sizeInMicrometer = mSettings.pixelToMicrometer(size);
                                ParticleInfoColoc exosom = new ParticleInfoColoc(colocNr, sizeInMicrometer, colocFactor,
                                        circularity,
                                        intensityChannels,
                                        areaChannels, result, 0);
                                exosom.validatearticle(minParticleSize, maxParticleSize, circularityFilter, 0);
                                coloc.addRoi(exosom);
                                colocNr++;
                                break; // We have a match. We can continue with the next particle
                            }
                        }
                    }
                }
            }
            coloc.calcStatistics();

            //
            // Generate channel settings
            //
            ImagePlus sumImageOriginal = Filter.ANDImages(ch1.imgeBeforeThershold, ch2.imgeBeforeThershold);
            ImagePlus sumImageThersholded = Filter.ANDImages(ch1.imageAfterThershold, ch2.imageAfterThershold);
            ColocChannelSet channelSet = new ColocChannelSet(sumImageOriginal, sumImageThersholded,
                    ChannelType.COLOC_ALL, coloc, chSet);

            //
            // Save control image
            //
            if (ch1.type != ChannelType.COLOC_ALL) {
                Vector<ChannelInfoOverlaySettings> channelsToPrint = new Vector<ChannelInfoOverlaySettings>();
                channelsToPrint.add(new ChannelInfoOverlaySettings(roiPic1, ch1.type.getColor(), false, true));
                channelsToPrint.add(new ChannelInfoOverlaySettings(roiPic2, ch2.type.getColor(), false, true));
                channelsToPrint
                        .add(new ChannelInfoOverlaySettings(coloc.getRois(), new Color(255, 255, 255, 80), false,
                                true));
                String imageName = getName(file) + name + "_coloc.jpg";
                String imagePath = getPath(file) + name + "_" + "coloc.jpg";

                Filter.SaveImageWithOverlayFromChannel(ch1.imageAfterThershold, channelsToPrint, imagePath);
                coloc.addControlImagePath(imageName);
            }
            return channelSet;
        } else if (ch1 != null) {
            return ch1;
        } else if (ch2 != null) {
            return ch2;
        } else {
            return null;
        }
    }

    ///
    /// EV counting
    ///
    class EvCounting implements Runnable {

        Map.Entry<ChannelType, ChannelSettings> val;
        Channel rmWithTetraSpeckBeads;

        EvCounting(Channel rmWithTetraSpeckBeads, Map.Entry<ChannelType, ChannelSettings> v) {
            this.val = v;
            this.rmWithTetraSpeckBeads = rmWithTetraSpeckBeads;
        }

        @Override
        public void run() {
            final ChannelSettings img0 = val.getValue();
            ImagePlus img0Th = Filter.duplicateImage(img0.mChannelImg);
            RoiManager rm = new RoiManager(false);
            double[] in0 = new double[2];
            ImagePlus img0BeforeTh = preFilterSetColoc(img0Th, background, img0.enhanceContrast, img0.mThersholdMethod,
                    img0.minThershold, img0.maxThershold, in0);

            //
            // Find particles
            //
            ImagePlus analzeImg0 = Filter.AnalyzeParticles(img0Th, rm, 0, -1, img0.getMinCircularityDouble());
            Channel measCh0 = Filter.MeasureImage(img0.type.toString(), mSettings, img0, img0BeforeTh, img0Th, rm,
                    true);
            measCh0.setThershold(in0[0], in0[1]);

            //
            // Remove Tetraspec
            //
            int nrOfRemovedTetraSpecks = RemoveTetraSpeckBeads(analzeImg0, measCh0);

            measCh0.setNrOfRemovedParticles(nrOfRemovedTetraSpecks);

            //
            // Save control images
            //
            String path = getPath(mImage) + "_" + img0.type.toString() + ".jpg";
            String pathor = getPath(mImage) + "_" + img0.type.toString() + "ori.jpg";
            measCh0.addControlImagePath(getName(mImage) + "_" + img0.type.toString() + ".jpg");
            channels.put(img0.type, measCh0);

            Vector<ChannelInfoOverlaySettings> channelsToPrint = new Vector<ChannelInfoOverlaySettings>();
            channelsToPrint.add(
                    new ChannelInfoOverlaySettings(measCh0.getRois(), val.getKey().getColor(), false, false));
            if (null != rmWithTetraSpeckBeads) {
                channelsToPrint.add(
                        new ChannelInfoOverlaySettings(rmWithTetraSpeckBeads.getRois(), Color.GRAY, false, false));
            }
            Filter.SaveImageWithOverlayFromChannel(analzeImg0, channelsToPrint, path);
            Filter.SaveImageWithOverlayFromChannel(img0BeforeTh, null, pathor);

            colocChannels.add(new ColocChannelSet(img0BeforeTh, img0Th, img0.type, measCh0, img0));
        }

        ///
        /// Remove tetraspeck bead
        ///
        int RemoveTetraSpeckBeads(ImagePlus evImg, Channel thesholdPictureWhereTetraSpeckShouldBeRemoved) {

            int removedTetraSpecs = 0;
            if (null != rmWithTetraSpeckBeads && null != thesholdPictureWhereTetraSpeckShouldBeRemoved) {
                for (Map.Entry<Integer, ParticleInfo> tetraspecParticel : rmWithTetraSpeckBeads.getRois().entrySet()) {
                    for (Map.Entry<Integer, ParticleInfo> evParticle : thesholdPictureWhereTetraSpeckShouldBeRemoved
                            .getRois().entrySet()) {
                        Roi result = evParticle.getValue().isPartOf(tetraspecParticel.getValue());
                        if (null != result) {
                            result.setImage(evImg);
                            int size = result.getContainedPoints().length;

                            if (size > 0) {
                                //
                                // Particles have an intersection!! This must be a Tetraspeck Remove it!!
                                //

                                //
                                // Paint it black
                                //
                                evImg.setRoi(evParticle.getValue().getRoi());
                                evImg.getProcessor().setRoi(evParticle.getValue().getRoi());
                                Filter.PaintSelecttedRoiAreaBlack(evImg);
                                Filter.ClearRoiInImage(evImg);

                                //
                                // Remove from ROI list
                                //
                                thesholdPictureWhereTetraSpeckShouldBeRemoved.removeRoi(evParticle.getKey());
                                removedTetraSpecs++;
                                break; // We have a match. We can continue with the next particle
                            }
                        }
                    }
                }
                thesholdPictureWhereTetraSpeckShouldBeRemoved.calcStatistics();
            }
            return removedTetraSpecs;
        }

    }

    Channel FindTetraspeckBeads(ChannelSettings imageWithTetraSpeckBeads) {
        RoiManager rm = new RoiManager(false);
        double[] retTh = new double[2];
        ImagePlus thershodlImg = Filter.duplicateImage(imageWithTetraSpeckBeads.mChannelImg);
        Filter.RollingBall(thershodlImg);
        Filter.Smooth(thershodlImg);
        Filter.Smooth(thershodlImg);
        Filter.ApplyThershold(thershodlImg, imageWithTetraSpeckBeads.mThersholdMethod,
                imageWithTetraSpeckBeads.minThershold, imageWithTetraSpeckBeads.maxThershold, retTh, true);
        ResultsTable rt = new ResultsTable();
        Filter.AnalyzeParticles(thershodlImg, rm, imageWithTetraSpeckBeads.getMinParticleSizeDouble(),
                imageWithTetraSpeckBeads.getMaxParticleSizeDouble(),
                imageWithTetraSpeckBeads.getMinCircularityDouble(), true, rt, false);
        Channel tetraSpeckBeads = Filter.MeasureImage("TetraSpeck Beads", mSettings, imageWithTetraSpeckBeads,
                imageWithTetraSpeckBeads.mChannelImg, thershodlImg, rm, true);
        tetraSpeckBeads.setThershold(retTh[0], retTh[1]);
        String path = getPath(mImage) + "_tetraspeck.jpg";
        tetraSpeckBeads.addControlImagePath(getName(mImage) + "_tetraspeck.jpg");
        channels.put(ChannelType.TETRASPECK_BEAD, tetraSpeckBeads);
        Filter.SaveImageWithOverlay(imageWithTetraSpeckBeads.mChannelImg, rm, path);
        return tetraSpeckBeads;
    }

}
