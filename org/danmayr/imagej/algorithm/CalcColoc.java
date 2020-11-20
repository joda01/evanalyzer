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
import ij.plugin.ImageInfo;

import java.awt.*;

import org.danmayr.imagej.excel.CsvToExcel;
import org.danmayr.imagej.gui.EvColocDialog;

public class CalcColoc extends BasicAlgorithm {

    String mAlloverStatistics = "";

    protected class MeasurementStructColoc implements MeasurementStruct {
        public MeasurementStructColoc(String dir) {
            mDirectory = dir;
        }

        public void add(double numberOfTooSmallParticles, double numberOfTooBigParticles, double numberOfColocEvs,
                double numberOfNotColocEvs, double numberOfGfpOnly, double numberOfCy3Only, double numerOfFounfGfp,
                double numberOfFoundCy3) {

            this.numberOfTooSmallParticles += numberOfTooSmallParticles;
            this.numberOfTooBigParticles += numberOfTooBigParticles;
            this.numberOfColocEvs += numberOfColocEvs;
            this.numberOfNotColocEvs += numberOfNotColocEvs;
            this.numberOfGfpOnly += numberOfGfpOnly;
            this.numberOfCy3Only += numberOfCy3Only;
            this.numerOfFounfGfp += numerOfFounfGfp;
            this.numberOfFoundCy3 += numberOfFoundCy3;
            mNumberOfValues++;
        }

        public void calcMean() {
            if (mNumberOfValues != 0) {
                numberOfTooSmallParticles /= mNumberOfValues;
                numberOfTooBigParticles /= mNumberOfValues;
                numberOfColocEvs /= mNumberOfValues;
                numberOfNotColocEvs /= mNumberOfValues;
                numberOfGfpOnly /= mNumberOfValues;
                numberOfCy3Only /= mNumberOfValues;
                numerOfFounfGfp /= mNumberOfValues;
                numberOfFoundCy3 /= mNumberOfValues;
                mNumberOfValues /= mNumberOfValues;
                mNumberOfValues = 0;
            }
        }

        public String mDirectory = "";
        public double numberOfTooSmallParticles = 0;
        public double numberOfTooBigParticles = 0;
        public double numberOfColocEvs = 0;
        public double numberOfNotColocEvs = 0;
        public double numberOfGfpOnly = 0;
        public double numberOfCy3Only = 0;
        public double numerOfFounfGfp = 0;
        public double numberOfFoundCy3 = 0;
        public double mNumberOfValues = 0;

        public String toString() {
            // mAlloverStatistics = "file;directory;small;big;coloc;no
            // coloc;GfpOnly;Cy3Only;GfpEvs;Cy3Evs\n";
            String retVal = "Mean" + ";" + mDirectory + ";" + Double.toString(numberOfTooSmallParticles) + ";"
                    + Double.toString(numberOfTooBigParticles) + ";" + Double.toString(numberOfColocEvs) + ";"
                    + Double.toString(numberOfNotColocEvs) + ";" + Double.toString(numberOfGfpOnly) + ";"
                    + Double.toString(numberOfCy3Only) + ";" + Double.toString(numerOfFounfGfp) + ";"
                    + Double.toString(numberOfFoundCy3);
            return retVal;
        }
    }

    // Constants for result index
    static int RESULT_FILE_IDX_AREA_SIZE = 1;
    static int RESULT_FILE_IDX_MEAN_THERSHOLD = 2;

    // Constants for calcuation
    static int MAX_THERSHOLD = 255;

    protected TreeMap<String, MeasurementStructColoc> mMeanValues = new TreeMap<String, MeasurementStructColoc>();

    /**
     * Creates a new analysing thread
     * 
     * @param dialog
     * @param analyseSettings
     */
    public CalcColoc(AnalyseSettings analyseSettings) {
        super(analyseSettings);
        mAlloverStatistics = "file;directory;small;big;coloc;no coloc;GfpOnly;Cy3Only;GfpEvs;Cy3Evs\n";

    }

    /**
     * Analyse an image
     * 
     * @param imageFile
     */
    @Override
    public void analyseImage(File imageFile) {
        // Remove scale

        // List all images
        String[] imageTitles = WindowManager.getImageTitles();

        if (imageTitles.length > 0) {

            //
            // Find red and green channel images
            //
            ImagePlus redChannel = null; // CY3
            ImagePlus greenChannel = null; // GFP
            for (int i = 0; i < imageTitles.length; i++) {
                String actTitle = imageTitles[i];
                ImagePlus img = WindowManager.getImage(actTitle);

                ImageInfo info = new ImageInfo();
                String imgInfo = info.getImageInfo(img);
                IJ.log(imgInfo);
                // Red Channel selection
                if (true == actTitle.endsWith("C=" + Integer.toString(mAnalyseSettings.mGreenChannel))) {
                    greenChannel = img;
                } else {
                    redChannel = img;
                }
            }

            // Process red channel
            if (null != redChannel) {
                if (true == mAnalyseSettings.mEnhanceContrastForRed) {
                    EnhanceContrast(redChannel);
                }
                ApplyFilter(redChannel);
                ApplyTherhold(redChannel);
                IJ.run(redChannel, "Set Scale...", "distance=0 known=0 unit=pixel global");
            }

            // Process green channel
            if (null != greenChannel) {
                if (true == mAnalyseSettings.mEnhanceContrastForGreen) {
                    EnhanceContrast(greenChannel);
                }
                ApplyFilter(greenChannel);
                ApplyTherhold(greenChannel);
                IJ.run(greenChannel, "Set Scale...", "distance=0 known=0 unit=pixel global");
            }

            RoiManager rm = new RoiManager();

            if (null != redChannel && null != greenChannel) {
                // Calculate the sum of both images
                ImageCalculator ic = new ImageCalculator();
                ImagePlus sumImage = ic.run("Max create", greenChannel, redChannel);
                IJ.run(sumImage, "Set Scale...", "distance=0 known=0 unit=pixel global");
                // Merge red and green channels
                MergeChannels(redChannel, greenChannel, imageFile.getName(), rm);
                // Analyze particles
                IJ.run(sumImage, "Analyze Particles...", "clear add");
            }

            String fileNameResultRedChannel = "";
            if (null != redChannel) {
                // Measure particles
                IJ.run("Clear Results", "");
                rm.runCommand(redChannel, "Measure");
                fileNameResultRedChannel = mAnalyseSettings.mOutputFolder + File.separator + imageFile.getName()
                        + "_cye.csv";
                IJ.saveAs("Results", fileNameResultRedChannel);
            }

            String fileNameGreenChannel = "";
            if (null != greenChannel) {
                IJ.run("Clear Results", "");
                rm.runCommand(greenChannel, "Measure");
                fileNameGreenChannel = mAnalyseSettings.mOutputFolder + File.separator + imageFile.getName()
                        + "_gfp.csv";
                IJ.saveAs("Results", fileNameGreenChannel);
            }

            calculateColoc(imageFile.getName(), imageFile.getParent(), new File(fileNameResultRedChannel),
                    new File(fileNameGreenChannel));

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
    private void calculateColoc(String filename, String directory, File redChannelResult, File greenChannelResult) {

        try {
            String[] redFileRead = new String(
                    Files.readAllBytes(Paths.get(redChannelResult.getAbsoluteFile().toString())),
                    StandardCharsets.UTF_8).split("\n");
            String[] greenFileRead = new String(
                    Files.readAllBytes(Paths.get(greenChannelResult.getAbsoluteFile().toString())),
                    StandardCharsets.UTF_8).split("\n");

            String result = "ROI; Area; GFP; CY3; DIFF\n";

            int numberOfTooSmallParticles = 0;
            int numberOfTooBigParticles = 0;
            int numberOfColocEvs = 0;
            int numberOfNotColocEvs = 0;
            int numberOfGfpOnly = 0;
            int numberOfCy3Only = 0;
            int numerOfFounfGfp = 0;
            int numberOfFoundCy3 = 0;

            // First line is header therefore start with 1
            for (int i = 1; i < redFileRead.length; i++) {

                String[] linegfp = greenFileRead[i].split(",");
                String[] linecy3 = redFileRead[i].split(",");

                double areaSize = 0.0;

                try {
                    areaSize = Double.parseDouble(linegfp[RESULT_FILE_IDX_AREA_SIZE]);
                } catch (NumberFormatException ex) {
                }

                if (areaSize >= mAnalyseSettings.mMinParticleSize) {
                    if (areaSize <= mAnalyseSettings.mMaxParticleSize) {

                        double gfpTh = 0.0, cy3Th = 0.0;
                        try {
                            gfpTh = Double.parseDouble(linegfp[RESULT_FILE_IDX_MEAN_THERSHOLD]);
                            cy3Th = Double.parseDouble(linecy3[RESULT_FILE_IDX_MEAN_THERSHOLD]);

                        } catch (NumberFormatException ex) {
                        }

                        // Coloc algorithm
                        double sub = Math.abs(MAX_THERSHOLD - Math.abs(gfpTh - cy3Th));

                        // Calculate the sum of coloc EVs
                        if (sub > 0) {
                            numberOfColocEvs++;
                        } else {
                            numberOfNotColocEvs++;
                            // Take all values which do not coloc
                            // All with 255 in gfp = gfp only
                            // All with 255 in cy3 = cy3 only

                            if (MAX_THERSHOLD == gfpTh) {
                                numberOfGfpOnly++;
                            }
                            if (MAX_THERSHOLD == cy3Th) {
                                numberOfCy3Only++;
                            }
                        }

                        //
                        // Count the found EVs
                        //
                        if (gfpTh > 0) {
                            numerOfFounfGfp++;
                        }
                        if (cy3Th > 0) {
                            numberOfFoundCy3++;
                        }

                        result = result + linegfp[0] + ";" + linegfp[1] + ";" + linegfp[2] + ";" + linecy3[2] + ";"
                                + Double.toString(sub) + "\n";
                    } else {
                        numberOfTooBigParticles++;
                    }
                } else {
                    numberOfTooSmallParticles++;
                }
            }

            // Add the rest of the stastic
            result = result + "\n------------------------------------------\n";
            result = result + "Statistic:\n";
            result = result + "------------------------------------------\n";
            result = result + "Small (" + Double.toString(mAnalyseSettings.mMinParticleSize) + ")\t"
                    + Double.toString(numberOfTooSmallParticles) + "\n";
            result = result + "Big (" + Double.toString(mAnalyseSettings.mMaxParticleSize) + ")\t"
                    + Double.toString(numberOfTooBigParticles) + "\n";
            result = result + "Coloc    ;" + Double.toString(numberOfColocEvs) + "\n";
            result = result + "Not Coloc;" + Double.toString(numberOfNotColocEvs) + "\n";
            result = result + "GFP only;" + Double.toString(numberOfGfpOnly) + "\n";
            result = result + "CY3 only;" + Double.toString(numberOfCy3Only) + "\n";
            result = result + "GFP Evs;" + Double.toString(numerOfFounfGfp) + "\n";
            result = result + "CY3 Evs;" + Double.toString(numberOfFoundCy3) + "\n";

            String outputfilename = mAnalyseSettings.mOutputFolder + File.separator + filename + "_final.txt";
            BufferedWriter writer = new BufferedWriter(new FileWriter(outputfilename));
            writer.write(result);
            writer.close();

            try {
                String retVal = filename + ";" + directory + ";" + Double.toString(numberOfTooSmallParticles) + ";"
                        + Double.toString(numberOfTooBigParticles) + ";" + Double.toString(numberOfColocEvs) + ";"
                        + Double.toString(numberOfNotColocEvs) + ";" + Double.toString(numberOfGfpOnly) + ";"
                        + Double.toString(numberOfCy3Only) + ";" + Double.toString(numerOfFounfGfp) + ";"
                        + Double.toString(numberOfFoundCy3) + "\n";

                MeasurementStructColoc struct = (MeasurementStructColoc) mMeanValues.get(directory);
                if (null == struct) {
                    struct = new MeasurementStructColoc(directory);
                    mMeanValues.put(directory, struct);
                }
                struct.add(numberOfTooSmallParticles, numberOfTooBigParticles, numberOfColocEvs, numberOfNotColocEvs,
                        numberOfGfpOnly, numberOfCy3Only, numerOfFounfGfp, numberOfFoundCy3);

                mAlloverStatistics += retVal;
            } catch (NumberFormatException ex) {
                String retVal = filename + " ERROR\n";
                mAlloverStatistics += retVal;
            }

        } catch (IOException ex) {

        }
    }

    public String writeAllOverStatisticToFile(String filename) {
        String outputfilename = mAnalyseSettings.mOutputFolder + File.separator + "statistic_all_over_final_"
        + filename + ".txt";
        try {
           
            for (Map.Entry<String, MeasurementStructColoc> entry : mMeanValues.entrySet()) {
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
