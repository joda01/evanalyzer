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

    File file;

    @Override
    protected TreeMap<ChannelType, Channel> startPipeline(File img) {
        file = img;
        background = null;
        channels = new TreeMap<ChannelType, Channel>();
        colocChannels.clear();
        RoiManager rmWithTetraSpeckBeads = new RoiManager(false);

        if (null != getBackground()) {
            background = getBackground().mChannelImg;
        }

        if (null != getTetraSpeckBead()) {
            FindTetraspeckBeads(rmWithTetraSpeckBeads, getTetraSpeckBead());
        }

        //
        // Count EVS and create threshold pictures
        //
        TreeMap<ChannelType, ChannelSettings> evs = getEvChannels();
        ExecutorService exec = Executors.newFixedThreadPool(evs.size());
        for (Map.Entry<ChannelType, ChannelSettings> val : evs.entrySet()) {
            exec.execute(new EvCounting(rmWithTetraSpeckBeads, val));
        }
        exec.shutdown();
        try {
            exec.awaitTermination(1, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        RoiManager rm = new RoiManager(false);

        //
        // Calc Coloc for two channel combinations (0 - 1 | 0 - 2 | 1 - 2)
        //
        if (true == mSettings.mCalcColoc) {
            ColocChannelSet colocAll = null;
            int colocEnum = 0;
            for (int n = 0; n < colocChannels.size(); n++) {
                for (int m = n + 1; m < colocChannels.size(); m++) {
                    ColocChannelSet img0 = colocChannels.get(n);
                    ColocChannelSet img1 = colocChannels.get(m);
                    Vector<ColocChannelSet> pics2Ch = new Vector<ColocChannelSet>();
                    pics2Ch.add(img0);
                    pics2Ch.add(img1);
                    ColocChannelSet coloc02 = calculateRoiColoc(img0, img1);
                    channels.put(ChannelType.getColocEnum(colocEnum), coloc02.ch);
                    colocEnum++;

                    //
                    // For multi channel coloc
                    //
                    colocAll = calculateRoiColoc(colocAll, coloc02);
                }
            }

            //
            // Save control images for all channels
            //
            Vector<ChannelInfoOverlaySettings> channelsToPrint = new Vector<ChannelInfoOverlaySettings>();
            for (Map.Entry<ChannelType, Channel> val : channels.entrySet()) {
                channelsToPrint.add(
                        new ChannelInfoOverlaySettings(val.getValue().getRois(), val.getKey().getColor(), false, true));
            }
            channelsToPrint
                    .add(new ChannelInfoOverlaySettings(colocAll.ch.getRois(), new Color(255, 255, 255, 80), false,
                            true));
            String imageName = getName(file) + "_all_coloc.jpg";
            String imagePath = getPath(file) + "_all_coloc.jpg";
            Filter.SaveImageWithOverlayFromChannel(colocAll.imageAfterThershold, channelsToPrint, imagePath);
            colocAll.ch.addControlImagePath(imageName);
            channels.put(ChannelType.COLOC_ALL, colocAll.ch);

        }

        for (Map.Entry<ChannelType, Channel> e : channels.entrySet()) {
            e.getValue().ClearRoi();
        }

        return channels;
    }

    //
    // Calculate the coloc by taking a ROI in channel 1 and looking for a ROI in
    // channel 2 which
    // has an intersection. All the ROIs with intersection are added to the ROI
    // coloc channel
    //
    ColocChannelSet calculateRoiColoc(ColocChannelSet ch1, ColocChannelSet ch2) {
        if (ch1 != null && ch2 != null) {
            String valueNames[] = { "coloc area", "coloc circularity", "coloc validity",
                    "intensity " + ch1.type.toString(),
                    "intensity " + ch2.type.toString(), "area " + ch1.type.toString(), "area " + ch2.type.toString() };

            String name = ch1.type.toString() + "_with_" + ch2.type.toString();
            Channel coloc = new Channel(name, new StatisticsColoc(), valueNames, 3);
            TreeMap<Integer, ParticleInfo> roiPic1 = ch1.ch.getRois();
            TreeMap<Integer, ParticleInfo> roiPic2 = ch2.ch.getRois();

            // Select setting of highest circularity of both channels
            ChannelSettings chSet = ch1.set.getMinCircularityDouble() > ch2.set.getMinCircularityDouble() ? ch1.set : ch2.set;

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
                            //

                            double[] intensityChannels = { particle1.getValue().areaGrayScale,
                                    particle2.getValue().areaGrayScale };
                            double[] areaChannels = { particle1.getValue().areaSize, particle2.getValue().areaSize };
                            double sizeInMicrometer = mSettings.pixelToMicrometer(size);
                            ParticleInfoColoc exosom = new ParticleInfoColoc(colocNr, sizeInMicrometer, circularity,
                                    intensityChannels,
                                    areaChannels, result, 0);
                            exosom.validatearticle(minParticleSize, maxParticleSize, circularityFilter,0);
                            coloc.addRoi(exosom);
                            colocNr++;
                            break; // We have a match. We can continue with the next particle
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
            if(ch1.type != ChannelType.COLOC_ALL){
                Vector<ChannelInfoOverlaySettings> channelsToPrint = new Vector<ChannelInfoOverlaySettings>();
                channelsToPrint.add(new ChannelInfoOverlaySettings(roiPic1, ch1.type.getColor(), false, true));
                channelsToPrint.add(new ChannelInfoOverlaySettings(roiPic2, ch2.type.getColor(), false, true));
                channelsToPrint
                        .add(new ChannelInfoOverlaySettings(coloc.getRois(), new Color(255, 255, 255, 80), false, true));
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
        RoiManager rmWithTetraSpeckBeads;

        EvCounting(RoiManager rmWithTetraSpeckBeads, Map.Entry<ChannelType, ChannelSettings> v) {
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
            // Remove TetraSpeckBeads
            //
            int nrOfRemovedTetraSpecks = RemoveTetraSpeckBeads(img0Th, img0.type);

            ImagePlus analzeImg0 = Filter.AnalyzeParticles(img0Th, rm, 0, -1, img0.getMinCircularityDouble());
            Channel measCh0 = Filter.MeasureImage(img0.type.toString(), mSettings, img0, img0BeforeTh, img0Th, rm);
            measCh0.setThershold(in0[0], in0[1]);
            measCh0.setNrOfRemovedParticles(nrOfRemovedTetraSpecks);
            channels.put(img0.type, measCh0);
            colocChannels.add(new ColocChannelSet(img0BeforeTh, img0Th, img0.type, measCh0, img0));
        }

        ///
        /// Remove tetraspeck bead
        ///
        int RemoveTetraSpeckBeads(ImagePlus thesholdPictureWhereTetraSpeckShouldBeRemoved, ChannelType type) {
            int removedTetraSpecs = 0;
            for (int n = 0; n < rmWithTetraSpeckBeads.getCount(); n++) {
                // Calculate center of mass of the ROI for selecting
                Rectangle boundingBox = rmWithTetraSpeckBeads.getRoi(n).getPolygon().getBounds();

                // int x = boundingBox.getLocation().x + boundingBox.width / 2;
                // int y = boundingBox.getLocation().y + boundingBox.height / 2;
                boolean found = false;
                for (int x = boundingBox.getLocation().x; x < boundingBox.getLocation().x + boundingBox.width; x++) {
                    for (int y = boundingBox.getLocation().y; y < boundingBox.getLocation().y
                            + boundingBox.height; y++) {
                        int pixelVal = thesholdPictureWhereTetraSpeckShouldBeRemoved.getProcessor().get(x, y);
                        // Select only white pixels

                        if (pixelVal > 200) {
                            Roi roi = Filter.doWand(thesholdPictureWhereTetraSpeckShouldBeRemoved, x, y, 20);
                            thesholdPictureWhereTetraSpeckShouldBeRemoved.setRoi(roi);
                            thesholdPictureWhereTetraSpeckShouldBeRemoved.getProcessor().setRoi(roi);
                            Filter.PaintSelecttedRoiAreaBlack(thesholdPictureWhereTetraSpeckShouldBeRemoved);
                            Filter.ClearRoiInImage(thesholdPictureWhereTetraSpeckShouldBeRemoved);
                            removedTetraSpecs++;
                            break;
                        }
                    }
                    if (true == found) {
                        break;
                    }
                }
            }
            return removedTetraSpecs;
        }
    }

    void FindTetraspeckBeads(RoiManager rm, ChannelSettings imageWithTetraSpeckBeads) {
        double[] retTh = new double[2];
        ImagePlus thershodlImg = Filter.duplicateImage(imageWithTetraSpeckBeads.mChannelImg);
        Filter.SubtractBackground(thershodlImg);
        Filter.Smooth(thershodlImg);
        Filter.Smooth(thershodlImg);
        Filter.ApplyThershold(thershodlImg, imageWithTetraSpeckBeads.mThersholdMethod,
                imageWithTetraSpeckBeads.minThershold, imageWithTetraSpeckBeads.maxThershold, retTh, true);
        ResultsTable rt = new ResultsTable();
        Filter.AnalyzeParticles(thershodlImg, rm, imageWithTetraSpeckBeads.getMinParticleSizeDouble(),
                imageWithTetraSpeckBeads.getMaxParticleSizeDouble(),
                imageWithTetraSpeckBeads.getMinCircularityDouble(), true, rt, false);
        Channel tetraSpeckBeads = Filter.MeasureImage("TetraSpeck Beads", mSettings, imageWithTetraSpeckBeads,
                imageWithTetraSpeckBeads.mChannelImg, thershodlImg, rm);
        tetraSpeckBeads.setThershold(retTh[0], retTh[1]);
        String path = getPath(file) + "_tetraspeck.jpg";
        tetraSpeckBeads.addControlImagePath(getName(file) + "_tetraspeck.jpg");
        channels.put(ChannelType.TETRASPECK_BEAD, tetraSpeckBeads);
        Filter.SaveImageWithOverlay(imageWithTetraSpeckBeads.mChannelImg, rm, path);
    }

    //
    // Contains the result pictures from the counting
    //
    // bth = before thrshold
    // ath = after thershold
    class ColocChannelSet {
        ColocChannelSet(ImagePlus bth, ImagePlus ath, ChannelType t, Channel ch, ChannelSettings set) {
            this.imgeBeforeThershold = bth;
            this.imageAfterThershold = ath;
            this.type = t;
            this.ch = ch;
            this.set = set;
        }

        ImagePlus imgeBeforeThershold;
        ImagePlus imageAfterThershold;
        ChannelType type;
        Channel ch;
        ChannelSettings set;
    }
}
