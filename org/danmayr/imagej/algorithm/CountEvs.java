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

    protected class ImageMeasurement {
        public ImageMeasurement(String roi, double areaSize, double areaBinGrayScale, double areaGrayScale) {
            this.roi = roi;
            this.areaSize = areaSize;
            this.areaBinGrayScale = areaBinGrayScale;
            this.areaGrayScale = areaGrayScale;
        }

        public String roi;
        public double areaSize;
        public double areaBinGrayScale;
        public double areaGrayScale;

        // String mResultHeader = "file;directory;small;big;count;grayscale;areasize\n";

    }

    ///
    /// Struct for statistic measurment
    ///
    protected class ChannelStatistic {
        public double numberOfParticles = 0;
        public double numberOfTooSmallParticles = 0;
        public double numberOfTooBigParticles = 0;
        public double numberOfParticlesInRange = 0;
        public double avgGrayScale = 0;
        public double avgAreaSize = 0;
    }

    protected class FolderStatistic {
        public void add(double avgParticles, double avgTooSmallParticles, double avgTooBigParticles,
                double avgParticlesInRange, double avgGrayScale, double avgAreaSize) {
            mSum.avgParticles += avgParticles;
            mSum.avgTooSmallParticles += avgTooSmallParticles;
            mSum.avgTooBigParticles += avgTooBigParticles;
            mSum.avgParticlesInRange += avgParticlesInRange;
            mSum.avgGrayScale += avgGrayScale;
            mSum.avgAreaSize += avgAreaSize;
            mSum.couter++;
        }

        public void calc() {
            mAvg.avgParticles = mSum.avgParticles / mSum.couter;
            mAvg.avgTooSmallParticles = mSum.avgTooSmallParticles / mSum.couter;
            mAvg.avgTooBigParticles = mSum.avgTooBigParticles / mSum.couter;
            mAvg.avgParticlesInRange = mSum.avgParticlesInRange / mSum.couter;
            mAvg.avgGrayScale = mSum.avgGrayScale / mSum.couter;
            mAvg.avgAreaSize = mSum.avgAreaSize / mSum.couter;

            mSum.avgParticles = 0;
            mSum.avgTooBigParticles = 0;
            mSum.avgParticlesInRange = 0;
            mSum.avgGrayScale = 0;
            mSum.avgAreaSize = 0;
            mSum.avgTooSmallParticles = 0;
            mSum.couter = 0;
        }

        class Values {
            public double avgParticles = 0;
            public double avgTooSmallParticles = 0;
            public double avgTooBigParticles = 0;
            public double avgParticlesInRange = 0;
            public double avgGrayScale = 0;
            public double avgAreaSize = 0;
            public double couter = 0;
        };

        Values mSum = new Values();
        Values mAvg = new Values();

    }

    ///
    /// Informarion for one channel
    ///
    protected class Channel {

        public Channel(String channelName) {
            mChannelName = channelName;
        }

        public void addMeasurement(ImageMeasurement imageMeasurement) {
            mMeasurements.add(imageMeasurement);
        }

        ///
        /// Calculate statistics for this channel
        ///
        public void calcStatistics() {
            double avgGraySkale = 0;
            double avgAreaSize = 0;
            int exosomCount = 0;
            int numberOfTooBigParticles = 0;
            int numberOfTooSmallParticles = 0;

            for (ImageMeasurement entry : mMeasurements) {
                if (entry.areaBinGrayScale > mAnalyseSettings.mMinParticleSize) {
                    if (entry.areaBinGrayScale < mAnalyseSettings.mMaxParticleSize) {
                        if (entry.areaBinGrayScale > 0) {
                            avgGraySkale += entry.areaGrayScale;
                            avgAreaSize += entry.areaSize;
                            exosomCount++;
                        }
                    } else {
                        numberOfTooBigParticles++;
                    }
                } else {
                    numberOfTooSmallParticles++;
                }
            }

            mStatistics.numberOfParticles = mMeasurements.size();
            mStatistics.numberOfTooSmallParticles = numberOfTooSmallParticles;
            mStatistics.numberOfTooBigParticles = numberOfTooBigParticles;
            mStatistics.numberOfParticlesInRange = exosomCount;
            mStatistics.avgGrayScale = avgGraySkale / exosomCount;
            mStatistics.avgAreaSize = avgAreaSize / exosomCount;

        }

        public String channelName() {
            return mChannelName;
        }

        public String header() {
            String ret = "NrOfParticles;NrOfSmall;NrOfBig;NrOfExosomes;AvgGrayscale;AvgAreaSize";
            return ret;
        }

        public String toString() {
            calcStatistics();
            String ret = Double.toString(mStatistics.numberOfParticles) + ";"
                    + Double.toString(mStatistics.numberOfTooSmallParticles) + ";"
                    + Double.toString(mStatistics.numberOfTooBigParticles) + ";"
                    + Double.toString(mStatistics.numberOfParticlesInRange) + ";"
                    + Double.toString(mStatistics.avgGrayScale) + ";" + Double.toString(mStatistics.avgAreaSize);

            return ret;
        }

        public ChannelStatistic getStatistics() {
            return mStatistics;
        }

        String mChannelName = "";
        ChannelStatistic mStatistics = new ChannelStatistic();
        Vector<ImageMeasurement> mMeasurements = new Vector<>();

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
                actChannel = new Channel(channelName);
                mChannels.put(channelName, actChannel);
            }
            actChannel.addMeasurement(imageMeasurement);
        }

        public String header() {
            String channelNames = ".;";
            String entry = "Name;";
            for (Map.Entry<String, Channel> img : mChannels.entrySet()) {
                channelNames += img.getValue().mChannelName+";;;;;"+";;";
                entry = entry + img.getValue().header() + ";;";
            }
            channelNames = channelNames +"\n"+entry;
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
                while(mStatistics.size() < imgStatisitcs.size()){
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
                statistics += Double.toString(st.mAvg.avgParticles) + ";" + Double.toString(st.mAvg.avgTooSmallParticles)
                        + ";" + Double.toString(st.mAvg.avgTooBigParticles) + ";"
                        + Double.toString(st.mAvg.avgParticlesInRange) + ";" + Double.toString(st.mAvg.avgGrayScale)
                        + ";" + Double.toString(st.mAvg.avgAreaSize)+";;";
            }
            retal +=statistics+"\n";

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
        RoiManager rm = new RoiManager();

        for (int n = 0; n < imageTitles.length; n++) {
            ImagePlus image = WindowManager.getImage(imageTitles[n]);

            if (null != image) {

                String channelName = imageTitles[n];

                // mResults.get(folderName).mImages.get(imageName).mChannels.get(channelName);

                if (true == mAnalyseSettings.mEnhanceContrastForRed) {
                    EnhanceContrast(image);
                }
                ApplyFilter(image);
                ImagePlus thersholdImage = ApplyTherhold(image);

                AnalyzeParticles(thersholdImage);
                SaveImageWithOverlay(thersholdImage, channelName, rm);

                IJ.log("Save result " + image.toString());
                File resultThersholded = MeasureAndSaveResult(thersholdImage, channelName, rm, "th");
                File resultOriginal = MeasureAndSaveResult(image, channelName, rm, "or");

                analyseChannel(folderName, imageFile.getName(), channelName, resultThersholded, resultOriginal);
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
    private void analyseChannel(String folderName, String imageName, String channelName, File thersholdResult,
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
