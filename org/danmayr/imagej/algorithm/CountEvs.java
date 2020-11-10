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

    protected class MeasurementStructCount implements MeasurementStruct {
        public MeasurementStructCount(String dir) {
            mDirectory = dir;
        }

        public void add(double numberOfTooSmallParticles, double numberOfTooBigParticles, double numberOfFoundEvs,
                double grayScale, double areaSize) {

            this.numberOfTooSmallParticles += numberOfTooSmallParticles;
            this.numberOfTooBigParticles += numberOfTooBigParticles;
            this.numberOfFoundEvs += numberOfFoundEvs;
            this.grayScale += grayScale;
            this.areaSize += areaSize;
            mNumberOfValues++;
        }

        public void calcMean() {
            if (mNumberOfValues != 0) {
                numberOfTooSmallParticles /= mNumberOfValues;
                numberOfTooBigParticles /= mNumberOfValues;
                numberOfFoundEvs /= mNumberOfValues;
                grayScale /= mNumberOfValues;
                areaSize /= mNumberOfValues;
                mNumberOfValues = 0;
            }
        }

        public String mDirectory = "";
        public double mNumberOfValues = 0;
        public double numberOfTooSmallParticles = 0;
        public double numberOfTooBigParticles = 0;
        public double numberOfFoundEvs = 0;
        public double grayScale = 0;
        public double areaSize = 0;

        public String toString() {
            // mAlloverStatistics = "file;directory;small;big;coloc;no
            // coloc;GfpOnly;Cy3Only;GfpEvs;Cy3Evs\n";
            String retVal = "Mean" + ";" + mDirectory + ";" + Double.toString(numberOfTooSmallParticles) + ";"
                    + Double.toString(numberOfTooBigParticles) + ";" + Double.toString(numberOfFoundEvs) + ";"
                    + Double.toString(grayScale) + ";" + Double.toString(areaSize);
            return retVal;
        }
    }

    // Constants for result index
    static int RESULT_FILE_ROI_IDX = 0;
    static int RESULT_FILE_IDX_AREA_SIZE = 1;
    static int RESULT_FILE_IDX_MEAN_GRAYSCALE = 2;

    // Constants for calcuation
    static int MAX_THERSHOLD = 255;

    protected TreeMap<String, MeasurementStructCount> mMeanValues = new TreeMap<String, MeasurementStructCount>();

    /**
     * Creates a new analysing thread
     * 
     * @param dialog
     * @param analyseSettings
     */
    public CountEvs(AnalyseSettings analyseSettings) {
        super(analyseSettings);
        mAlloverStatistics = "file;directory;small;big;count;grayscale;areasize\n";
    }

    /**
     * Analyse an image
     * 
     * @param imageFile
     */
    @Override
    public void analyseImage(File imageFile) {

        // List all images
        String[] imageTitles = WindowManager.getImageTitles();

        if (imageTitles.length > 0) {

            ImagePlus image = WindowManager.getImage(imageTitles[0]);

            RoiManager rm = new RoiManager();

            if (null != image) {
                if (true == mAnalyseSettings.mEnhanceContrastForRed) {
                    EnhanceContrast(image);
                }
                ApplyFilter(image);
                ImagePlus thersholdImage = ApplyTherhold(image);

                AnalyzeParticles(thersholdImage);
                String resultThersholded = MeasureAndSaveResult(thersholdImage, imageFile, rm, "th");
                String resultOriginal = MeasureAndSaveResult(image, imageFile, rm, "or");

                // Save image with overlay
                SaveImageWithOverlay(thersholdImage, imageFile, rm);

                calcEves(imageFile.getName(), imageFile.getParent(), new File(resultThersholded),
                        new File(resultOriginal));
            }
        } else {
            IJ.log("No image loaded");
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
    private void calcEves(String filename, String directory, File thersholdResult, File originalPictureResult) {

        try {
            String[] thersholdRead = new String(
                    Files.readAllBytes(Paths.get(thersholdResult.getAbsoluteFile().toString())),
                    StandardCharsets.UTF_8).split("\n");

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

                        result = result + line[RESULT_FILE_ROI_IDX] + ";" + line[RESULT_FILE_IDX_AREA_SIZE] + ";" + line[RESULT_FILE_IDX_MEAN_GRAYSCALE] +";" + lineOri[RESULT_FILE_IDX_MEAN_GRAYSCALE] + "\n";
                    } else {
                        numberOfTooBigParticles++;
                    }
                } else {
                    numberOfTooSmallParticles++;
                }
            }

            meanGrayScale/=numerOfFounEvs;
            meanAreaSize/=numerOfFounEvs;
            // Add the rest of the stastic
            result = result + "\n------------------------------------------\n";
            result = result + "Statistic:\n";
            result = result + "------------------------------------------\n";
            result = result + "Small (" + Double.toString(mAnalyseSettings.mMinParticleSize) + ")\t"
                    + Double.toString(numberOfTooSmallParticles) + "\n";
            result = result + "Big (" + Double.toString(mAnalyseSettings.mMaxParticleSize) + ")\t"
                    + Double.toString(numberOfTooBigParticles) + "\n";
            result = result + "Found Evs;" + Double.toString(numerOfFounEvs) + "\n";
            result = result + "Mean gray scale:" + Double.toString(meanGrayScale) + "\n";
            result = result + "Mean area size:" + Double.toString(meanAreaSize) + "\n";


            String outputfilename = mAnalyseSettings.mOutputFolder + File.separator + filename + "_final.txt";
            BufferedWriter writer = new BufferedWriter(new FileWriter(outputfilename));
            writer.write(result);
            writer.close();

            try {
                String retVal = filename + ";" + directory + ";" + Double.toString(numberOfTooSmallParticles) + ";"
                        + Double.toString(numberOfTooBigParticles) + ";" + Double.toString(numerOfFounEvs) + ";" + Double.toString(meanGrayScale)+ ";" + Double.toString(meanAreaSize)+ "\n";

                MeasurementStructCount struct = mMeanValues.get(directory);
                if (null == struct) {
                    struct = new MeasurementStructCount(directory);
                    mMeanValues.put(directory, struct);
                }
                struct.add(numberOfTooSmallParticles, numberOfTooBigParticles, numerOfFounEvs,meanGrayScale,meanAreaSize);

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
