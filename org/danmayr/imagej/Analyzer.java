package org.danmayr.imagej;

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
import org.danmayr.imagej.EvColocDialog;

public class Analyzer extends Thread {

    // PlugIn mPlugin;
    AnalyseSettings mAnalyseSettings;
    EvColocDialog mDialog;
    ArrayList<File> mFoundFiles = new ArrayList<>();
    boolean mStopping = false;

    // Temporary storag
    String mAlloverStatistics;

    // Constants for result index
    static int RESULT_FILE_IDX_AREA_SIZE = 1;
    static int RESULT_FILE_IDX_MEAN_THERSHOLD = 2;

    // Constants for calcuation
    static int MAX_THERSHOLD = 255;

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

        // Prepare results folder
        prepareOutputFolder();

        mAlloverStatistics = "file;small;big;coloc;no coloc;GfpOnly;Cy3Only;GfpEvs;Cy3Evs\n";
        mFoundFiles.clear();
        mDialog.setProgressBarMaxSize(mFoundFiles.size());
        mDialog.setProgressBarValue(0);

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
            if (true == mStopping) {
                break;
            }
        }
    }

    private void closeAllWindow() {
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
                if (true == mAnalyseSettings.mEnhanceContrastForRed) {
                    EnhanceContrast(redChannel);
                }
                ApplyFilter(redChannel);
                ApplyTherhold(redChannel);
            }

            // Process green channel
            if (null != greenChannel) {
                if (true == mAnalyseSettings.mEnhanceContrastForGreen) {
                    EnhanceContrast(greenChannel);
                }
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
            String fileNameResultRedChannel = mAnalyseSettings.mOutputFolder + File.separator + imageFile.getName()
                    + "_cye.csv";
            IJ.saveAs("Results", fileNameResultRedChannel);

            IJ.run("Clear Results", "");
            rm.runCommand(greenChannel, "Measure");
            String fileNameGreenChannel = mAnalyseSettings.mOutputFolder + File.separator + imageFile.getName()
                    + "_gfp.csv";
            IJ.saveAs("Results", fileNameGreenChannel);

            // Merge red and green channels
            MergeChannels(redChannel, greenChannel, imageFile, rm);

            calculateColoc(imageFile.getName(), new File(fileNameResultRedChannel), new File(fileNameGreenChannel));

        } else {
            IJ.log("No image loaded");
        }
    }

    private void prepareOutputFolder() {
        File parentFile = new File(mAnalyseSettings.mOutputFolder);
        if (parentFile != null && !parentFile.exists()) {
            parentFile.mkdirs();
        }
        IJ.log(parentFile.getAbsolutePath());
    }

    private void EnhanceContrast(ImagePlus img) {
        IJ.run(img, "Enhance Contrast...", "saturated=0.3 normalize");
    }

    private void ApplyFilter(ImagePlus img) {
        IJ.run(img, "Subtract Background...", "rolling=4 sliding");
        IJ.run(img, "Convolve...", "text1=[1 4 6 4 1\n4 16 24 16 4\n6 24 36 24 6\n4 16 24 16 4\n1 4 6 4 1] normalize");
    }

    private void ApplyTherhold(ImagePlus imp) {
        IJ.setAutoThreshold(imp, mAnalyseSettings.mThersholdMethod + " dark");
        Prefs.blackBackground = true;
        IJ.run(imp, "Convert to Mask", "");
    }

    private void MergeChannels(ImagePlus red, ImagePlus green, File imageFile, RoiManager rm) {

        IJ.run("Merge Channels...", "c1=[" + red.getTitle() + "] c2=[" + green.getTitle() + "] keep");
        ImagePlus imp = WindowManager.getImage("RGB");
        IJ.saveAs(imp, "Jpeg", mAnalyseSettings.mOutputFolder + File.separator + imageFile.getName() + "_merged.jpg");
        rm.runCommand(imp, "Show All");
        imp = imp.flatten();
        IJ.saveAs(imp, "Jpeg",
                mAnalyseSettings.mOutputFolder + File.separator + imageFile.getName() + "_merged_overlay.jpg");
    }

    /**
     * List all images in directory and subdirectory
     * 
     * @param files
     */
    public void findFiles(File[] files) {
        for (File file : files) {
            if (file.isDirectory()) {
                findFiles(file.listFiles());
            } else if (file.getName().endsWith(".vsi")) {
                mFoundFiles.add(file);
                mDialog.setProgressBarMaxSize(mFoundFiles.size());
            }
        }
    }


    private void writeAllOverStatisticToFile(){
        try{
        String outputfilename = mAnalyseSettings.mOutputFolder + File.separator + "statistic_all_over_final.txt";
        BufferedWriter writer = new BufferedWriter(new FileWriter(outputfilename));
        writer.write(mAlloverStatistics);
        writer.close();
        }catch(IOException ex){

        }
    }

    /**
     * Calculats the colocalization of the given image
     * 
     * @param filename
     * @param redChannelResult
     * @param greenChannelResult
     */
    private void calculateColoc(String filename, File redChannelResult, File greenChannelResult) {

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

                if (areaSize > mAnalyseSettings.mMinParticleSize) {
                    if (areaSize < mAnalyseSettings.mMaxParticleSize) {

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
                String retVal = filename + ";" + Double.toString(numberOfTooSmallParticles) + ";"
                        + Double.toString(numberOfTooBigParticles) + ";" + Double.toString(numberOfColocEvs) + ";"
                        + Double.toString(numberOfNotColocEvs) + ";" + Double.toString(numberOfGfpOnly) + ";"
                        + Double.toString(numberOfCy3Only) + ";" + Double.toString(numerOfFounfGfp) + ";"
                        + Double.toString(numberOfFoundCy3) + "\n";
                mAlloverStatistics += retVal;
            } catch (NumberFormatException ex) {
                String retVal = filename + " ERROR\n";
                mAlloverStatistics += retVal;
            }

        } catch (IOException ex) {

        }
    }
}
