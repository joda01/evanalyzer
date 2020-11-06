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

    protected class MeasurementStructCount implements MeasurementStruct  {
        public MeasurementStructCount(String dir){
            mDirectory = dir;
        }

        public void add(
            double numberOfTooSmallParticles,
            double numberOfTooBigParticles,
            double numberOfFoundEvs){

            this.numberOfTooSmallParticles += numberOfTooSmallParticles;
            this.numberOfTooBigParticles += numberOfTooBigParticles;
            this.numberOfFoundEvs += numberOfFoundEvs;
            mNumberOfValues++;
        }


        public void calcMean(){
            if(mNumberOfValues != 0){
                numberOfTooSmallParticles/=mNumberOfValues;
                numberOfTooBigParticles/=mNumberOfValues;
                numberOfFoundEvs/=mNumberOfValues;
                mNumberOfValues = 0;
            }
        }

        public String mDirectory="";
        public double numberOfTooSmallParticles= 0;
        public double numberOfTooBigParticles= 0;
        public double numberOfFoundEvs= 0;
        public double mNumberOfValues = 0;


        public String toString(){
            //  mAlloverStatistics = "file;directory;small;big;coloc;no coloc;GfpOnly;Cy3Only;GfpEvs;Cy3Evs\n";
            String retVal = "Mean" + ";" + mDirectory + ";" + Double.toString(numberOfTooSmallParticles) + ";"
            + Double.toString(numberOfTooBigParticles) + ";" + Double.toString(numberOfFoundEvs);
            return retVal;
        }
    }

    // Constants for result index
    static int RESULT_FILE_IDX_AREA_SIZE = 1;
    static int RESULT_FILE_IDX_MEAN_THERSHOLD = 2;

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

            //
            // Find red and green channel images
            //
            ImagePlus image = WindowManager.getImage(imageTitles[0]);

        
            // Process red channel
            if (null != image) {
                if (true == mAnalyseSettings.mEnhanceContrastForRed) {
                    EnhanceContrast(image);
                }
                ApplyFilter(image);
                ApplyTherhold(image);
            }

            RoiManager rm = new RoiManager();
            IJ.run(image, "Set Scale...", "distance=0 known=0 unit=pixel global");
            IJ.run(image, "Analyze Particles...", "clear add");

            IJ.run("Clear Results", "");
            rm.runCommand(image, "Measure");
            String fileNameResultRedChannel = mAnalyseSettings.mOutputFolder + File.separator + imageFile.getName()+ ".csv";
            IJ.saveAs("Results", fileNameResultRedChannel);
            IJ.run("Clear Results", "");
           

            // Save image with overlay
            SaveImageWithOverlay(image, imageFile, rm);

            calcEves(imageFile.getName(), imageFile.getParent(), new File(fileNameResultRedChannel));

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
    private void calcEves(String filename, String directory, File channelResultFile) {

        try {
            String[] fileRead = new String(
                    Files.readAllBytes(Paths.get(channelResultFile.getAbsoluteFile().toString())),
                    StandardCharsets.UTF_8).split("\n");

            String result = "ROI; Area; Measure\n";

            int numberOfTooSmallParticles = 0;
            int numberOfTooBigParticles = 0;
            int numerOfFounEvs = 0;

            // First line is header therefore start with 1
            for (int i = 1; i < fileRead.length; i++) {

                String[] line = fileRead[i].split(",");

                double areaSize = 0.0;

                try {
                    areaSize = Double.parseDouble(line[RESULT_FILE_IDX_AREA_SIZE]);
                } catch (NumberFormatException ex) {
                }

                if (areaSize >= mAnalyseSettings.mMinParticleSize) {
                    if (areaSize <= mAnalyseSettings.mMaxParticleSize) {

                        double meanAreaSize = 0.0;
                        try {
                            meanAreaSize = Double.parseDouble(line[RESULT_FILE_IDX_MEAN_THERSHOLD]);

                        } catch (NumberFormatException ex) {
                        }

                        //
                        // Count the found EVs
                        //
                        if (meanAreaSize > 0) {
                            numerOfFounEvs++;
                        }

                        result = result + line[0] + ";" + line[1] + ";" + line[2] + "\n";
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
            result = result + "Found Evs;" + Double.toString(numerOfFounEvs) + "\n";

            String outputfilename = mAnalyseSettings.mOutputFolder + File.separator + filename + "_final.txt";
            BufferedWriter writer = new BufferedWriter(new FileWriter(outputfilename));
            writer.write(result);
            writer.close();

            try {
                String retVal = filename + ";" + directory + ";" + Double.toString(numberOfTooSmallParticles) + ";"
                        + Double.toString(numberOfTooBigParticles) + ";" + Double.toString(numerOfFounEvs) + "\n";

                MeasurementStructCount struct = mMeanValues.get(directory);
                if (null == struct) {
                    struct = new MeasurementStructCount(directory);
                    mMeanValues.put(directory, struct);
                }
                struct.add(numberOfTooSmallParticles, numberOfTooBigParticles, numerOfFounEvs);

                mAlloverStatistics += retVal;
            } catch (NumberFormatException ex) {
                String retVal = filename + " ERROR\n";
                mAlloverStatistics += retVal;
            }

        } catch (IOException ex) {

        }
    }

    public void writeAllOverStatisticToFile() {
        try {
            String outputfilename = mAnalyseSettings.mOutputFolder + File.separator + "statistic_all_over_final.txt";
            String outputfilenameXlsx = mAnalyseSettings.mOutputFolder + File.separator + "statistic_all_over_final";

            
            for (Map.Entry<String, MeasurementStructCount> entry : mMeanValues.entrySet()) {
                MeasurementStruct struct = entry.getValue();
                
                struct.calcMean();
                String mean = struct.toString();
                mAlloverStatistics = mAlloverStatistics + mean + "\n";
            }            
            
            
            
            BufferedWriter writer = new BufferedWriter(new FileWriter(outputfilename));
            writer.write(mAlloverStatistics);
            writer.close();

            String convertCsvToXls = CsvToExcel.convertCsvToXls(outputfilenameXlsx, outputfilename);

        } catch (IOException ex) {

        }
    }

}
