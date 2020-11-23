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

import org.danmayr.imagej.algorithm.BasicAlgorithm;
import org.danmayr.imagej.algorithm.CalcColoc;
import org.danmayr.imagej.algorithm.Function;
import org.danmayr.imagej.excel.CsvToExcel;
import org.danmayr.imagej.excel.InputFiles;
import org.danmayr.imagej.gui.EvColocDialog;

public class ImageProcessor extends Thread {

    EvColocDialog mDialog;
    boolean mStopping = false;
    AnalyseSettings mAnalyseSettings;

    public ImageProcessor(final EvColocDialog dialog, final AnalyseSettings analyseSettings) {
        mDialog = dialog;
        mAnalyseSettings = analyseSettings;
    }

    /**
     * Start the analyse thread
     */
    public void run() {

        // Prepare results folder
        prepareOutputFolder();

        mDialog.setProgressBarMaxSize(0);
        mDialog.setProgressBarValue(0);

        //
        // List all files in folders and subfolders
        //
        ArrayList<File>  mFoundFiles = new ArrayList<>();
        findFiles(new File(mAnalyseSettings.mInputFolder).listFiles(),mFoundFiles);
        ArrayList<File> mFoundNegativeControlFiles = new ArrayList<>();
        findFiles(new File(mAnalyseSettings.mNegativeControl).listFiles(),mFoundNegativeControlFiles);
        mDialog.setProgressBarMaxSize(mFoundFiles.size()+mFoundNegativeControlFiles.size());
        
        
        // Analyse images
        BasicAlgorithm mAnalysisAlgorithm = null;

        if(mAnalyseSettings.mSelectedFunction.equals(Function.countExosomes)){
            mAnalysisAlgorithm = new CountEvs(mAnalyseSettings);
            IJ.log("Count Exosomes");

        }if(mAnalyseSettings.mSelectedFunction.equals(Function.calcColoc)){
            mAnalysisAlgorithm = new ColocEvs(mAnalyseSettings);
        }
        if(null ==mAnalysisAlgorithm){
            mDialog.finishedAnalyse();
            return;
        }
        walkThroughFiles(mAnalysisAlgorithm,mFoundFiles);
        String analysisOutput = mAnalysisAlgorithm.writeAllOverStatisticToFile("analysis");


        // Analyse negative control
        CountEvs mNegativeControlAlgorithm = new CountEvs(mAnalyseSettings);
        walkThroughFiles(mNegativeControlAlgorithm,mFoundNegativeControlFiles);
        String negativeControl = mNegativeControlAlgorithm.writeAllOverStatisticToFile("negativeControl");


        // Write statistics to file
        InputFiles input = new InputFiles();
        input.add(analysisOutput,"Results");
        input.add(negativeControl,"NegativeControls");
        String xlsxResult = mAnalyseSettings.mOutputFolder + File.separator + "statistic_all_over_final";
        String convertCsvToXls = CsvToExcel.convertCsvToXls(xlsxResult, input);

        mDialog.finishedAnalyse();
    }

    /**
     * Cancle the process after the actual image has been finished
     */
    public void cancle() {
        mStopping = true;
    }

    /**
     * Walk through all found files and analyse each image after the other
     */
    private void walkThroughFiles(BasicAlgorithm algorithm, ArrayList<File> fileList) {
        int value = 0;
        for (final File file : fileList) {
            value++;

            IJ.run("Bio-Formats Importer", "open=[" + file.getAbsoluteFile().toString()
                    + "] autoscale color_mode=Grayscale rois_import=[ROI manager] specify_range split_channels view=Hyperstack stack_order=XYCZT "
                    + mAnalyseSettings.mSelectedSeries + " c_begin_1=1 c_end_1=2 c_step_1=1");

            //String[] imageTitles = WindowManager.getImageTitles();
            //if (imageTitles.length > 1) {

            // File has:
            // * red channel and green channel
            // * red channel 
            // * green channel
            // * red channel | negative control
            // * green channel negative control

    
            algorithm.analyseImage(file);
           
            closeAllWindow();
            WindowManager.closeAllWindows();
            mDialog.incrementProgressBarValue();
            if (true == mStopping) {
                break;
            }
        }
    }

    /**
     * List all images in directory and subdirectory
     * 
     * @param files
     */
    private void findFiles(final File[] files,ArrayList<File> foundFiles) {
        if(null != files){
            for (final File file : files) {
                if (file.isDirectory()) {
                    findFiles(file.listFiles(),foundFiles);
                } else if (file.getName().endsWith(".vsi")) {
                    foundFiles.add(file);
                }
            }
        }
    }

    private void prepareOutputFolder() {
        final File parentFile = new File(mAnalyseSettings.mOutputFolder);
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

}
