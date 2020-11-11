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

    protected class ImageMeasurement
    {
        String roi;
        double areaSize;
        double areaBinGrayScale;
        double areaGrayScale;

        //String mResultHeader = "file;directory;small;big;count;grayscale;areasize\n";

    }

    ///
    /// Struct for statistic measurment
    ///
    protected class ChannelStatistic 
    {
        public double numberOfParticles = 0;
        public double numberOfTooSmallParticles = 0;
        public double numberOfTooBigParticles = 0;
        public double numberOfParticlesInRange = 0;
        public double avgGrayScale = 0;
        public double avgAreaSize = 0;
    }

    ///
    /// Informarion for one channel
    ///
    protected class Channel {

        public Channel(String channelName){
            mChannelName = channelName;
        }

        String mChannelName = "";
        ChannelStatistic mStatistics;
        Vector<ImageMeasurement> mMeasurements = new Vector<>();

        void addMeasurement(ImageMeasurement imageMeasurement){
            mMeasurements.add(imageMeasurement);
        }
    }

    ///
    /// One image consits of a number of channels
    ///
    protected class Image{
        String mImageName;
        TreeMap<String, Channel> mChannels = new TreeMap<>();           // Channelname | Channel
        void addChannel(String channelName, ImageMeasurement imageMeasurement){
            Channel actChannel = mChannels.get(channelName);
            if(null == actChannel){
                actChannel = new Channel(channelName);
                mChannels.put(channelName, actChannel);
            }
            actChannel.addMeasurement(imageMeasurement);
        }
    }

    protected class Folder{
        String mFolderName;
        TreeMap<String, Image> mImages = new TreeMap<>();           // ImageName | Image
        
        void addImage(String imageName,String channelName, ImageMeasurement imageMeasurement){
            Image actImage = mImages.get(imageName);
            if(null == actImage){
                actImage = new Image();
                mImages.put(imageName, actImage);
            }
            actImage.addChannel(channelName, imageMeasurement);
        }
    }

    ///
    /// A folder consits of a number of Folders with images
    ///
    protected TreeMap<String, Folder> mResults = new TreeMap<>();     // Foldername | Folder


    void addMeasurement(String foldername, String imageName, String channelName, ImageMeasurement imageMeasurement)
    {
        Folder actFolder = mResults.get(foldername);
        if(null == actFolder){
             actFolder = new Folder();
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
        String imageName = imageFile.getName();

        String[] imageTitles = WindowManager.getImageTitles();

        for(int n=0;n<imageTitles.length;n++){
            ImagePlus image = WindowManager.getImage(imageTitles[n]);



            if (null != image) {
                RoiManager rm = new RoiManager();
                String channelName = imageTitles[n];

                mResults.get(folderName).mImages.get(imageName).mChannels.get(channelName);

                if (true == mAnalyseSettings.mEnhanceContrastForRed) {
                    EnhanceContrast(image);
                }
                ApplyFilter(image);
                ImagePlus thersholdImage = ApplyTherhold(image);

                AnalyzeParticles(thersholdImage);
                SaveImageWithOverlay(thersholdImage, imageFile, rm);

                File resultThersholded = MeasureAndSaveResult(thersholdImage, imageFile, rm, "th");
                File resultOriginal    = MeasureAndSaveResult(image, imageFile, rm, "or");

                analyseMeasurement(imageFile.getName(), imageFile.getParent(), resultThersholded, resultOriginal);
                

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
    private ImageMeasurement analyseChannel(String filename, String directory, File thersholdResult, File originalPictureResult) {

        try {
            String[] thersholdRead = new String(
                    Files.readAllBytes(Paths.get(thersholdResult.getAbsoluteFile().toString())), StandardCharsets.UTF_8)
                            .split("\n");

            String[] originalRead = new String(
                    Files.readAllBytes(Paths.get(originalPictureResult.getAbsoluteFile().toString())),
                    StandardCharsets.UTF_8).split("\n");

            String result = "ROI; Area; AvgBinScale; AvgGrayScale\n";

            double meanAreaSize = 0.0;
            double meanGrayScale = 0.0;
            int numberOfTooSmallParticles = 0;
            int numberOfTooBigParticles = 0;
            int numerOfFounEvs = 0;

            // First line is header therefore start with 1
            for (int i = 1; i < thersholdRead.length; i++) {

                String[] line = thersholdRead[i].split(",");
                String[] lineOri = originalRead[i].split(",");

                double areaSize = 0.0;

                try {
                    areaSize = Double.parseDouble(line[RESULT_FILE_IDX_AREA_SIZE]);
                } catch (NumberFormatException ex) {
                }

                if (areaSize >= mAnalyseSettings.mMinParticleSize) {
                    if (areaSize <= mAnalyseSettings.mMaxParticleSize) {

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

                        //
                        // Count the found EVs
                        //
                        if (binScale > 0) {
                            numerOfFounEvs++;
                            meanGrayScale = meanGrayScale + grayScale;
                            meanAreaSize = meanAreaSize + areaSize;
                        }

                        result = result + line[RESULT_FILE_ROI_IDX] + ";" + line[RESULT_FILE_IDX_AREA_SIZE] + ";"
                                + line[RESULT_FILE_IDX_MEAN_GRAYSCALE] + ";" + lineOri[RESULT_FILE_IDX_MEAN_GRAYSCALE]
                                + "\n";
                    } else {
                        numberOfTooBigParticles++;
                    }
                } else {
                    numberOfTooSmallParticles++;
                }

                ImageMeasurement exosom = new ImageMeasurement();
                addMeasurement(folderName,imageName,channelName,exosom);

            }

            meanGrayScale /= numerOfFounEvs;
            meanAreaSize /= numerOfFounEvs;
            // Add the rest of the stastic
            result = result + "\n------------------------------------------\n";
            result = result + "Statistic:\n";
            result = result + "------------------------------------------\n";
            result = result + "Small (" + Double.toString(mAnalyseSettings.mMinParticleSize) + ")\t"
                    + Double.toString(numberOfTooSmallParticles) + "\n";
            result = result + "Big (" + Double.toString(mAnalyseSettings.mMaxParticleSize) + ")\t"
                    + Double.toString(numberOfTooBigParticles) + "\n";
            result = result + "Found Evs;" + Double.toString(numerOfFounEvs) + "\n";
            result = result + "Avg gray scale:" + Double.toString(meanGrayScale) + "\n";
            result = result + "Avg area size:" + Double.toString(meanAreaSize) + "\n";

            String outputfilename = mAnalyseSettings.mOutputFolder + File.separator + filename + "_final.txt";
            BufferedWriter writer = new BufferedWriter(new FileWriter(outputfilename));
            writer.write(result);
            writer.close();

            try {
                String retVal = filename + ";" + directory + ";" + Double.toString(numberOfTooSmallParticles) + ";"
                        + Double.toString(numberOfTooBigParticles) + ";" + Double.toString(numerOfFounEvs) + ";"
                        + Double.toString(meanGrayScale) + ";" + Double.toString(meanAreaSize) + "\n";

                MeasurementStructCount struct = mMeanValues.get(directory);
                if (null == struct) {
                    struct = new MeasurementStructCount(directory);
                    mMeanValues.put(directory, struct);
                }
                struct.add(numberOfTooSmallParticles, numberOfTooBigParticles, numerOfFounEvs, meanGrayScale,
                        meanAreaSize);

                mAlloverStatistics += retVal;
            } catch (NumberFormatException ex) {
                String retVal = filename + " ERROR\n";
                mAlloverStatistics += retVal;
            }

        } catch (IOException ex) {

        }
    }

    public String writeAllOverStatisticToFile(String filename) {
        String outputfilename = mAnalyseSettings.mOutputFolder + File.separator + "statistic_all_over_final_" + filename
                + ".txt";

        mAlloverStatistics += "\n";
        try {

            for (Map.Entry<String, MeasurementStructCount> entry : mMeanValues.entrySet()) {
                MeasurementStruct struct = entry.getValue();

                struct.calcMean();
                String mean = struct.toString();
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
