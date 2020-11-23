package org.danmayr.imagej.algorithm;

import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import javax.swing.JDialog;
import javax.swing.JWindow;

import ij.plugin.*;
import ij.plugin.frame.*;

import java.awt.*;

import org.danmayr.imagej.excel.CsvToExcel;
import org.danmayr.imagej.gui.EvColocDialog;

public class CountEvs extends BasicAlgorithm {

    // Constants for result index
    static int RESULT_FILE_ROI_IDX = 0;
    static int RESULT_FILE_IDX_AREA_SIZE = 1;
    static int RESULT_FILE_IDX_MEAN_GRAYSCALE = 2;

    // Constants for calcuation
    static int MAX_THERSHOLD = 255;

    protected RoiManager rm = new RoiManager();

    protected class FolderStatistic {
        public void add(double avgParticles, double avgTooSmallParticles, double avgTooBigParticles,
                double avgParticlesInRange, double avgGrayScale, double avgAreaSize) {
            mSum.numberOfParticles += avgParticles;
            mSum.numberOfTooSmallParticles += avgTooSmallParticles;
            mSum.numberOfTooBigParticles += avgTooBigParticles;
            mSum.numberOfParticlesInRange += avgParticlesInRange;
            mSum.avgGrayScale += avgGrayScale;
            mSum.avgAreaSize += avgAreaSize;
            mSum.counter++;
        }

        public void calc() {
            mAvg.numberOfParticles = mSum.numberOfParticles / mSum.counter;
            mAvg.numberOfTooSmallParticles = mSum.numberOfTooSmallParticles / mSum.counter;
            mAvg.numberOfTooBigParticles = mSum.numberOfTooBigParticles / mSum.counter;
            mAvg.numberOfParticlesInRange = mSum.numberOfParticlesInRange / mSum.counter;
            mAvg.avgGrayScale = mSum.avgGrayScale / mSum.counter;
            mAvg.avgAreaSize = mSum.avgAreaSize / mSum.counter;

            mSum.numberOfParticles = 0;
            mSum.numberOfTooBigParticles = 0;
            mSum.numberOfParticlesInRange = 0;
            mSum.avgGrayScale = 0;
            mSum.avgAreaSize = 0;
            mSum.numberOfTooSmallParticles = 0;
            mSum.counter = 0;
        }


        ChannelStatistic mSum = new ChannelStatistic();
        ChannelStatistic mAvg = new ChannelStatistic();

    }

    ///
    /// One image consits of a number of channels
    ///
    protected class Image {
        String mImageName;
        TreeMap<String, Channel> mChannels = new TreeMap<>(); // Channelname, Channel

        public Image(String name) {
            mImageName = name;
        }

        public void addChannel(String channelName, ImageMeasurement imageMeasurement) {
            Channel actChannel = mChannels.get(channelName);
            if (null == actChannel) {
                actChannel = new Channel(channelName, mAnalyseSettings);
                mChannels.put(channelName, actChannel);
            }
            actChannel.addMeasurement(imageMeasurement);
        }

        public String header() {
            String channelNames = ".;";
            String entry = "Name;";
            for (Map.Entry<String, Channel> img : mChannels.entrySet()) {
                channelNames += img.getValue().mChannelName + ";;;;;" + ";;";
                entry = entry + img.getValue().header() + ";;";
            }
            channelNames = channelNames + "\n" + entry;
            return channelNames;
        }

        public String toString() {
            String entry = mImageName + ";";
            for (Map.Entry<String, Channel> img : mChannels.entrySet()) {
                entry = entry + img.getValue().toString() + ";;";
            }
            return entry;
        }

        public Vector<ChannelStatistic> getStatistics() {
            Vector<ChannelStatistic> statistic = new Vector<>();
            for (Map.Entry<String, Channel> img : mChannels.entrySet()) {
                statistic.add(img.getValue().getStatistics());
            }
            return statistic;
        }
    }

    protected class Folder {
        String mFolderName;
        TreeMap<String, Image> mImages = new TreeMap<>(); // ImageName, Image
        Vector<FolderStatistic> mStatistics = new Vector<>();

        public Folder(String name) {
            mFolderName = name;
        }

        public void addImage(String imageName, String channelName, ImageMeasurement imageMeasurement) {
            Image actImage = mImages.get(imageName);
            if (null == actImage) {
                actImage = new Image(imageName);
                mImages.put(imageName, actImage);
            }
            actImage.addChannel(channelName, imageMeasurement);
        }

        ///
        /// Calculate statistics for this channel
        ///
        public void calcStatistics() {
            mStatistics.clear();
            for (Map.Entry<String, Image> entry : mImages.entrySet()) {
                Vector<ChannelStatistic> imgStatisitcs = entry.getValue().getStatistics();
                while (mStatistics.size() < imgStatisitcs.size()) {
                    mStatistics.add(new FolderStatistic());
                }
                for (int n = 0; n < imgStatisitcs.size(); n++) {
                    ChannelStatistic ch = imgStatisitcs.get(n);

                    mStatistics.get(n).add(ch.numberOfParticles, ch.numberOfTooSmallParticles,
                            ch.numberOfTooBigParticles, ch.numberOfParticlesInRange, ch.avgGrayScale, ch.avgAreaSize);
                }
            }

            for (int n = 0; n < mStatistics.size(); n++) {
                mStatistics.get(n).calc();
            }

        }

        public String header() {
            return mFolderName;
        }

        public String toString() {
            String retal = "";
            // retal += entry.getValue().header();
            for (Map.Entry<String, Image> entry : mImages.entrySet()) {
                if (retal.length() <= 0) {
                    retal = entry.getValue().header() + "\n";
                }
                retal = retal + entry.getValue().toString() + "\n";
            }
            calcStatistics();

            String statistics = "Avg;";
            for (int n = 0; n < mStatistics.size(); n++) {
                FolderStatistic st = mStatistics.get(n);
                statistics += Double.toString(st.mAvg.numberOfParticles) + ";"
                        + Double.toString(st.mAvg.numberOfTooSmallParticles) + ";"
                        + Double.toString(st.mAvg.numberOfTooBigParticles) + ";"
                        + Double.toString(st.mAvg.numberOfParticlesInRange) + ";" + Double.toString(st.mAvg.avgGrayScale)
                        + ";" + Double.toString(st.mAvg.avgAreaSize) + ";;";
            }
            retal += statistics + "\n";

            return retal;
        }
    }

    ///
    /// A folder consits of a number of Folders with images
    ///
    protected TreeMap<String, Folder> mResults = new TreeMap<>(); // Foldername, Folder

    void addMeasurement(String foldername, String imageName, String channelName, ImageMeasurement imageMeasurement) {
        Folder actFolder = mResults.get(foldername);
        if (null == actFolder) {
            actFolder = new Folder(foldername);
            mResults.put(foldername, actFolder);
        }
        actFolder.addImage(imageName, channelName, imageMeasurement);
    }

    /**
     * Creates a new analysing thread
     * 
     * @param dialog
     * @param analyseSettings
     */
    public CountEvs(AnalyseSettings analyseSettings) {
        super(analyseSettings);

    }

    /**
     * Analyse an image
     * 
     * @param imageFile
     */
    @Override
    public void analyseImage(File imageFile) {

        String folderName = imageFile.getParent();

        String[] imageTitles = WindowManager.getImageTitles();

        ImageInfo info = new ImageInfo();

        for (int n = 0; n < imageTitles.length; n++) {
            ImagePlus image = WindowManager.getImage(imageTitles[n]);

            if (null != image) {
                String imgTitle = imageTitles[n];
                /*
                 * String imgInfo = info.getImageInfo(image); String[] elements =
                 * imgInfo.split("\n");
                 */

                ImagePlus filtered = ApplyFilter(image);
                ImagePlus thersholdImage = ApplyTherhold(filtered);

                AnalyzeParticles(thersholdImage);
                SaveImageWithOverlay(thersholdImage, imgTitle, rm);

                File resultThersholded = MeasureAndSaveResult(thersholdImage, imgTitle, rm, "th");
                File resultOriginal = MeasureAndSaveResult(filtered, imgTitle, rm, "or");

                analyseChannel(folderName, imageFile.getName(), imgTitle, resultThersholded, resultOriginal);
                // Delete temporary files
                resultOriginal.delete();
                resultThersholded.delete();
            }
        }
    }

    /**
     * Calculats the colocalization of the given image
     * 
     * @param filename
     * @param directory
     * @param redChannelResult
     * @param greenChannelResult
     */
    protected void analyseChannel(String folderName, String imageName, String channelName, File thersholdResult,
            File originalPictureResult) {
        IJ.log("Analyse: " + imageName + " " + channelName);

        try {
            String[] thersholdRead = new String(
                    Files.readAllBytes(Paths.get(thersholdResult.getAbsoluteFile().toString())), StandardCharsets.UTF_8)
                            .split("\n");

            String[] originalRead = new String(
                    Files.readAllBytes(Paths.get(originalPictureResult.getAbsoluteFile().toString())),
                    StandardCharsets.UTF_8).split("\n");

            // First line is header therefore start with 1
            for (int i = 1; i < thersholdRead.length; i++) {

                String[] line = thersholdRead[i].split(",");
                String[] lineOri = originalRead[i].split(",");

                double areaSize = 0.0;

                try {
                    areaSize = Double.parseDouble(line[RESULT_FILE_IDX_AREA_SIZE]);
                } catch (NumberFormatException ex) {
                }

                double binScale = 0.0;
                try {
                    binScale = Double.parseDouble(line[RESULT_FILE_IDX_MEAN_GRAYSCALE]);
                } catch (NumberFormatException ex) {
                }

                double grayScale = 0.0;
                try {
                    grayScale = Double.parseDouble(lineOri[RESULT_FILE_IDX_MEAN_GRAYSCALE]);
                } catch (NumberFormatException ex) {
                }

                ImageMeasurement exosom = new ImageMeasurement(line[RESULT_FILE_ROI_IDX], areaSize, binScale,
                        grayScale);
                addMeasurement(folderName, imageName, channelName, exosom);
            }

        } catch (IOException ex) {
            IJ.log("Catch: " + ex.getMessage());

        }
    }

    public String writeAllOverStatisticToFile(String filename) {
        String outputfilename = mAnalyseSettings.mOutputFolder + File.separator + "statistic_all_over_final_" + filename
                + ".txt";

        String mAlloverStatistics = "";
        try {

            for (Map.Entry<String, Folder> entry : mResults.entrySet()) {
                Folder struct = entry.getValue();

                String mean = struct.toString();
                mAlloverStatistics += struct.header() + "\n";
                mAlloverStatistics = mAlloverStatistics + mean + "\n";
            }

            BufferedWriter writer = new BufferedWriter(new FileWriter(outputfilename));
            writer.write(mAlloverStatistics);
            writer.close();

        } catch (IOException ex) {

        }

        return outputfilename;
    }

}
