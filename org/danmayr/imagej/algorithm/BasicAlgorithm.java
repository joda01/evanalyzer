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
import org.danmayr.imagej.gui.EvColocDialog;

abstract public class BasicAlgorithm extends Thread {
    // PlugIn mPlugin;
    AnalyseSettings mAnalyseSettings;
    EvColocDialog mDialog;
    ArrayList<File> mFoundFiles = new ArrayList<>();
    boolean mStopping = false;

    protected class MeasurementStruct {
        public MeasurementStruct(String dir){
            mDirectory = dir;
        }

        public void add(
            double numberOfTooSmallParticles,
            double numberOfTooBigParticles,
            double numberOfColocEvs,
            double numberOfNotColocEvs,
            double numberOfGfpOnly,
            double numberOfCy3Only,
            double numerOfFounfGfp,
            double numberOfFoundCy3){

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


        public void calcMean(){
            if(mNumberOfValues != 0){
                numberOfTooSmallParticles/=mNumberOfValues;
                numberOfTooBigParticles/=mNumberOfValues;
                numberOfColocEvs/=mNumberOfValues;
                numberOfNotColocEvs/=mNumberOfValues;
                numberOfGfpOnly/=mNumberOfValues;
                numberOfCy3Only/=mNumberOfValues;
                numerOfFounfGfp/=mNumberOfValues;
                numberOfFoundCy3/=mNumberOfValues;
                mNumberOfValues /=mNumberOfValues;
                mNumberOfValues = 0;
            }
        }

        public String mDirectory="";
        public double numberOfTooSmallParticles= 0;
        public double numberOfTooBigParticles= 0;
        public double numberOfColocEvs= 0;
        public double numberOfNotColocEvs= 0;
        public double numberOfGfpOnly= 0;
        public double numberOfCy3Only= 0;
        public double numerOfFounfGfp= 0;
        public double numberOfFoundCy3= 0;
        public double mNumberOfValues = 0;

        public String toString(){
            //  mAlloverStatistics = "file;directory;small;big;coloc;no coloc;GfpOnly;Cy3Only;GfpEvs;Cy3Evs\n";
            String retVal = "Mean" + ";" + mDirectory + ";" + Double.toString(numberOfTooSmallParticles) + ";"
            + Double.toString(numberOfTooBigParticles) + ";" + Double.toString(numberOfColocEvs) + ";"
            + Double.toString(numberOfNotColocEvs) + ";" + Double.toString(numberOfGfpOnly) + ";"
            + Double.toString(numberOfCy3Only) + ";" + Double.toString(numerOfFounfGfp) + ";"
            + Double.toString(numberOfFoundCy3);
            return retVal;
        }
    }

    // Temporary storag
    protected String mAlloverStatistics;
    protected TreeMap<String, MeasurementStruct> mMeanValues = new TreeMap<String, MeasurementStruct>();


    public BasicAlgorithm(EvColocDialog dialog, AnalyseSettings analyseSettings) {
        mAnalyseSettings = analyseSettings;
        mDialog = dialog;
    }

    abstract protected void analyseImage(File imageFile);

    /**
     * Start the analyse thread
     */
    public void run() {

        // Prepare results folder
        prepareOutputFolder();

        mAlloverStatistics = "file;directory;small;big;coloc;no coloc;GfpOnly;Cy3Only;GfpEvs;Cy3Evs\n";
        mFoundFiles.clear();
        mDialog.setProgressBarMaxSize(mFoundFiles.size());
        mDialog.setProgressBarValue(0);
        mMeanValues.clear();

        findFiles(new File(mAnalyseSettings.mInputFolder).listFiles());
        walkThroughFiles();
        writeAllOverStatisticToFile();
        mDialog.finishedAnalyse();
    }

    /**
     * Cancle the process after the actual image has been finished
     */
    public void cancle() {
        mStopping = true;
    }

    /**
     * List all images in directory and subdirectory
     * 
     * @param files
     */
    private void findFiles(File[] files) {
        for (File file : files) {
            if (file.isDirectory()) {
                findFiles(file.listFiles());
            } else if (file.getName().endsWith(".vsi")) {
                mFoundFiles.add(file);
                mDialog.setProgressBarMaxSize(mFoundFiles.size());
            }
        }
    }

    /**
     * Walk through all found files and analyse each image after the other
     */
    private void walkThroughFiles() {
        int value = 0;
        for (File file : mFoundFiles) {
            value++;
            analyseImage(file);
            closeAllWindow();
            WindowManager.closeAllWindows();
            mDialog.setProgressBarValue(value);
            if (true == mStopping) {
                break;
            }
        }
    }

    private void prepareOutputFolder() {
        File parentFile = new File(mAnalyseSettings.mOutputFolder);
        if (parentFile != null && !parentFile.exists()) {
            parentFile.mkdirs();
        }
        IJ.log(parentFile.getAbsolutePath());
    }

    private void closeAllWindow() {
        ImagePlus img;
        while (null != WindowManager.getCurrentImage()) {
            img = WindowManager.getCurrentImage();
            img.changes = false;
            img.close();
        }
    }

    private void writeAllOverStatisticToFile() {
        try {
            String outputfilename = mAnalyseSettings.mOutputFolder + File.separator + "statistic_all_over_final.txt";
            
            for (Map.Entry<String, MeasurementStruct> entry : mMeanValues.entrySet()) {
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
    }
}
