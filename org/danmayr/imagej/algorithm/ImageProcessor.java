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

import org.danmayr.imagej.algorithm.CalcColoc;
import org.danmayr.imagej.excel.CsvToExcel;
import org.danmayr.imagej.gui.EvColocDialog;

public class ImageProcessor extends Thread {

    EvColocDialog mDialog;
    boolean mStopping = false;
    ArrayList<File> mFoundFiles = new ArrayList<>();
    CalcColoc mAlgoCalcColoc;
    CountEvs mAlgoCountEvs;
    AnalyseSettings mAnalyseSettings;

    public ImageProcessor(final EvColocDialog dialog, final AnalyseSettings analyseSettings) {
        mDialog = dialog;
        mAnalyseSettings = analyseSettings;
        mAlgoCalcColoc = new CalcColoc(analyseSettings);
        mAlgoCountEvs = new CountEvs(analyseSettings);

    }

    /**
     * Start the analyse thread
     */
    public void run() {

        // Prepare results folder
        prepareOutputFolder();

        mFoundFiles.clear();
        mDialog.setProgressBarMaxSize(mFoundFiles.size());
        mDialog.setProgressBarValue(0);

        findFiles(new File(mAnalyseSettings.mInputFolder).listFiles());
        walkThroughFiles();

        String outColoc = mAlgoCalcColoc.writeAllOverStatisticToFile("coloc");
        String outCount = mAlgoCountEvs.writeAllOverStatisticToFile("count");

        String inputFiles[] = {outColoc,outCount};

        String xlsxResult = mAnalyseSettings.mOutputFolder + File.separator + "statistic_all_over_final";


        String convertCsvToXls = CsvToExcel.convertCsvToXls(xlsxResult, inputFiles);


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
    private void walkThroughFiles() {
        int value = 0;
        for (final File file : mFoundFiles) {
            value++;

            IJ.run("Bio-Formats Importer", "open=[" + file.getAbsoluteFile().toString()
                    + "] autoscale color_mode=Grayscale rois_import=[ROI manager] specify_range split_channels view=Hyperstack stack_order=XYCZT "
                    + mAnalyseSettings.mSelectedSeries + " c_begin_1=1 c_end_1=2 c_step_1=1");

            String[] imageTitles = WindowManager.getImageTitles();

            if (imageTitles.length > 1) {

                mAlgoCalcColoc.analyseImage(file);

            } else if (imageTitles.length > 0) {
                mAlgoCountEvs.analyseImage(file);
            }

            closeAllWindow();
            WindowManager.closeAllWindows();
            mDialog.setProgressBarValue(value);
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
    private void findFiles(final File[] files) {
        for (final File file : files) {
            if (file.isDirectory()) {
                findFiles(file.listFiles());
            } else if (file.getName().endsWith(".vsi")) {
                mFoundFiles.add(file);
                mDialog.setProgressBarMaxSize(mFoundFiles.size());
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
