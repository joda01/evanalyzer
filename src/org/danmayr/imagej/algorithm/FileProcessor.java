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
import java.sql.ResultSet;
import java.util.*;

import javax.swing.JDialog;
import javax.swing.JWindow;

import ij.plugin.*;
import ij.plugin.frame.*;

import java.awt.*;

import org.danmayr.imagej.algorithm.structs.*;
import org.danmayr.imagej.exports.*;


import org.danmayr.imagej.algorithm.*;
import org.danmayr.imagej.algorithm.filters.*;
import org.danmayr.imagej.algorithm.pipelines.*;

import org.danmayr.imagej.gui.EvColocDialog;

public class FileProcessor extends Thread {

    EvColocDialog mDialog;
    boolean mStopping = false;
    AnalyseSettings mAnalyseSettings;
    FolderResults mResuls = new FolderResults();

    public FileProcessor(final EvColocDialog dialog, final AnalyseSettings analyseSettings) {
        mDialog = dialog;
        mAnalyseSettings = analyseSettings;
    }

    /**
     * Start the analyse thread
     */
    public void run() {

        // Close all open windows
        closeAllWindow();
        WindowManager.closeAllWindows();

        // Prepare results folder
        prepareOutputFolder();

        mDialog.setProgressBarMaxSize(0,"look for images ...");
        mDialog.setProgressBarValue(0,"look for images ...");

        //
        // List all files in folders and subfolders
        //
        ArrayList<File> mFoundFiles = new ArrayList<>();
        findFiles(new File(mAnalyseSettings.mInputFolder).listFiles(), mFoundFiles);
        mDialog.setProgressBarMaxSize(mFoundFiles.size(),"analyzing ...");
        mDialog.setProgressBarValue(0,"analyzing ...");


        // Analyse images
        Pipeline pipeline = null;

        if (mAnalyseSettings.mSelectedFunction.equals(AnalyseSettings.Function.countExosomes)) {
            pipeline = new ExosomCount(mAnalyseSettings);
        }
        if (mAnalyseSettings.mSelectedFunction.equals(AnalyseSettings.Function.calcColoc)) {
            int nrOfEnabledCh = 0;
            for(ChannelSettings chSett : mAnalyseSettings.channelSettings){
                if(chSett.mChannelName != "OFF"){
                    nrOfEnabledCh++;
                }
            }
            if(nrOfEnabledCh == 3){
                pipeline = new ExosomeColoc3Ch(mAnalyseSettings);
            }else{
                pipeline = new ExosomColoc(mAnalyseSettings);
            }
        }
        if (mAnalyseSettings.mSelectedFunction.equals(AnalyseSettings.Function.countInCellExosomes)) {
            pipeline = new ExosomeCountInCells(mAnalyseSettings);
        }
        if (null == pipeline) {
            mDialog.finishedAnalyse("");
            return;
        }
        walkThroughFiles(pipeline, mFoundFiles);

        String reportFileName = ExcelExport.Export(mAnalyseSettings.mOutputFolder, mAnalyseSettings.mOutputFileName, mResuls, mAnalyseSettings.reportType,mAnalyseSettings, mDialog);

        // Write statistics to file
        /*
         * InputFiles input = new InputFiles(); input.add(analysisOutput,"Results");
         * input.add(negativeControl,"NegativeControls"); String xlsxResult =
         * mAnalyseSettings.mOutputFolder + File.separator + "statistic_all_over_final";
         * String convertCsvToXls = CsvToExcel.convertCsvToXls(xlsxResult, input);
         */

        mDialog.finishedAnalyse(reportFileName);
    }


    ///
    /// \brief  Get file from selected folder
    ///
    public static File getFile(int idx, String inputFolder){
        ArrayList<File> mFoundFiles = new ArrayList<>();
        findFiles(new File(inputFolder).listFiles(), mFoundFiles);

        if(idx < mFoundFiles.size()){
            return mFoundFiles.get(idx);
        }else{
            return null;
        }
    }

    public static void OpenImage(File imgToOpen, String series){
        IJ.run("Bio-Formats Importer", "open=[" + imgToOpen.getAbsoluteFile().toString()
        + "] autoscale color_mode=Grayscale rois_import=[ROI manager] specify_range split_channels view=Hyperstack stack_order=XYCZT "
        + series + " c_begin_1=1 c_end_1=6 c_step_1=1");
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
    private void walkThroughFiles(Pipeline algorithm, ArrayList<File> fileList) {
        int value = 0;
        for (final File file : fileList) {
            value++;

            OpenImage(file, mAnalyseSettings.mSelectedSeries);


            // String[] imageTitles = WindowManager.getImageTitles();
            // if (imageTitles.length > 1) {

            // File has:
            // * red channel and green channel
            // * red channel
            // * green channel
            // * red channel | negative control
            // * green channel negative control

            try {
                TreeMap<Integer, Channel> images = algorithm.ProcessImage(file);
                mResuls.addImage(file.getParent(),file.getName(), images);
            } catch (Exception ey) {
                ey.printStackTrace();
            }

            closeAllWindow();
            WindowManager.closeAllWindows();
            mDialog.incrementProgressBarValue("analyzing ...");
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
    private static void findFiles(final File[] files, ArrayList<File> foundFiles) {
        if (null != files) {
            for (final File file : files) {
                if (file.isDirectory()) {
                    findFiles(file.listFiles(), foundFiles);
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
