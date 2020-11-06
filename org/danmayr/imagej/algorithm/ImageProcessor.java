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
    AnalyseSettings mAnalyseSettings;

    


    public ImageProcessor(final EvColocDialog dialog, final AnalyseSettings analyseSettings) {
        mDialog = dialog;
        mAnalyseSettings = analyseSettings;
        mAlgoCalcColoc = new CalcColoc(analyseSettings);
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
        mAlgoCalcColoc.writeAllOverStatisticToFile();
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
            mAlgoCalcColoc.analyseImage(file);
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
