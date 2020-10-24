package org.danmayr.imagej;

import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import java.io.File;
import java.io.FilenameFilter;
import java.util.*;

import javax.swing.JDialog;
import javax.swing.JWindow;

import ij.plugin.*;
import ij.plugin.frame.*;

import java.awt.*;
import org.danmayr.imagej.EvColocDialog;

public class Analyzer extends Thread {

    // PlugIn mPlugin;
    AnalyseSettings mAnalyseSettings;
    EvColocDialog mDialog;
    ArrayList<File> mFoundFiles = new ArrayList<>();

    FilenameFilter mFileFilter = new FilenameFilter() {
        @Override
        public boolean accept(File f, String name) {
            // We want to find only .c files
            return f.getName().endsWith(".vsi") || f.isDirectory();
        }
    };

    boolean mStopping = false;

    /**
     * Creates a new analysing thread
     * 
     * @param dialog
     * @param analyseSettings
     */
    Analyzer(EvColocDialog dialog, AnalyseSettings analyseSettings) {
        mAnalyseSettings = analyseSettings;
        mDialog = dialog;
    }

    /**
     * Start the analyse thread
     */
    public void run() {
        mFoundFiles.clear();
        mDialog.setProgressBarMaxSize(mFoundFiles.size());
        mDialog.setProgressBarValue(0);

        findFiles(new File(mAnalyseSettings.mInputFolder).listFiles(mFileFilter));
        walkThroughFiles();
    }

    public void cancle(){
        mStopping = true;
    }

    /**
     * Walk through all found files and analyse each image after the other
     */
    public void walkThroughFiles() {
        int value = 0;
        for (File file : mFoundFiles) {
            value++;
            analyseImage(file);
            closeAllWindow();
            WindowManager.closeAllWindows();
            mDialog.setProgressBarValue(value);
        }
    }

    private void closeAllWindow(){
        ImagePlus img;
        while (null != WindowManager.getCurrentImage()) {
            img = WindowManager.getCurrentImage();
            img.changes = false;
            img.close();
        }
    }

    /**
     * Analyse an image
     * 
     * @param imageFile
     */
    public void analyseImage(File imageFile) {
        IJ.run("Bio-Formats Importer", "open=[" + imageFile.getAbsoluteFile().toString()
                + "] autoscale color_mode=Grayscale rois_import=[ROI manager] specify_range split_channels view=Hyperstack stack_order=XYCZT series_1 c_begin_1=1 c_end_1=2 c_step_1=1");

        // Remove scale
        IJ.run("Set Scale...", "distance=0 known=0 unit=pixel");

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

                // Red Channel selection
                if (true == actTitle.endsWith("C=" + Integer.toString(mAnalyseSettings.mRedChannel))) {
                    redChannel = img;
                } else {
                    greenChannel = img;
                }
            }

            // Process red channel
            if (null != redChannel) {
                EnhanceContrast(redChannel);
                ApplyFilter(redChannel);
                ApplyTherhold(redChannel);
            }

            // Process green channel
            if (null != greenChannel) {
                EnhanceContrast(greenChannel);
                ApplyFilter(greenChannel);
                ApplyTherhold(greenChannel);
            }

            // Calculate the sum of both images
            ImageCalculator ic = new ImageCalculator();
            RoiManager rm = new RoiManager();
            ImagePlus sumImage = ic.run("Max create", greenChannel, redChannel);

            // Analyze particles
            IJ.run(sumImage, "Analyze Particles...", "clear add");

            // Measure particles
            IJ.run("Clear Results", "");
            rm.runCommand(redChannel, "Measure");
            IJ.saveAs("Results", mAnalyseSettings.mOutputFolder + File.separator + imageFile.getName() + "_cye.csv");

            IJ.run("Clear Results", "");
            rm.runCommand(greenChannel, "Measure");
            IJ.saveAs("Results", mAnalyseSettings.mOutputFolder + File.separator + imageFile.getName() + "_gfp.csv");

            // Merge red and green channels
            MergeChannels(redChannel, greenChannel, imageFile,rm);

        } else {
            IJ.log("No image loaded");
        }
    }

    private void EnhanceContrast(ImagePlus img) {
        IJ.run(img, "Enhance Contrast...", "saturated=0.3 normalize");
    }

    private void ApplyFilter(ImagePlus img) {
        IJ.run(img, "Subtract Background...", "rolling=4 sliding");
        IJ.run(img, "Convolve...", "text1=[1 4 6 4 1\n4 16 24 16 4\n6 24 36 24 6\n4 16 24 16 4\n1 4 6 4 1] normalize");
    }

    private void ApplyTherhold(ImagePlus imp) {
        IJ.setAutoThreshold(imp, mAnalyseSettings.mThersholdMethod+" dark");
        Prefs.blackBackground = true;
        IJ.run(imp, "Convert to Mask", "");
    }

    private void MergeChannels(ImagePlus red, ImagePlus green, File imageFile,RoiManager rm) {

        IJ.run("Merge Channels...", "c1=[" + red.getTitle() + "] c2=[" + green.getTitle() + "] keep");
        ImagePlus imp = WindowManager.getImage("RGB");
        IJ.saveAs(imp, "Jpeg", mAnalyseSettings.mOutputFolder + File.separator + imageFile.getName() + "_merged.jpg");
        rm.runCommand(imp, "Show All");
        imp = imp.flatten();
        IJ.saveAs(imp, "Jpeg", mAnalyseSettings.mOutputFolder + File.separator + imageFile.getName() + "_merged_overlay.jpg");
    }

    /**
     * List all images in directory and subdirectory
     * 
     * @param files
     */
    public void findFiles(File[] files) {
        for (File file : files) {
            if (file.isDirectory()) {
                findFiles(file.listFiles(mFileFilter));
            } else if (file.getName().endsWith(".vsi")) {
                mFoundFiles.add(file);
                mDialog.setProgressBarMaxSize(mFoundFiles.size());
            }
        }
    }
}
