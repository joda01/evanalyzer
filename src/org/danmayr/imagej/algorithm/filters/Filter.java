
package org.danmayr.imagej.algorithm.filters;

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
import ij.plugin.filter.ParticleAnalyzer;

import org.danmayr.imagej.algorithm.structs.*;
import org.danmayr.imagej.algorithm.statistics.*;
import org.danmayr.imagej.algorithm.AnalyseSettings;



public class Filter {
    static int RESULT_FILE_ROI_IDX = 0;
    static int RESULT_FILE_IDX_AREA_SIZE = 1;
    static int RESULT_FILE_IDX_MEAN_GRAYSCALE = 2;
    static int RESULT_FILE_IDX_CIRCULARITY = 5;

    public Filter() {

    }

    public static ImagePlus AddImages(ImagePlus ch0, ImagePlus ch1)
    {
        ImageCalculator ic = new ImageCalculator();
        ImagePlus sumImage = ic.run("Max create", ch0, ch1);
        return sumImage;
    }


    public static ImagePlus EnhanceContrast(ImagePlus img) {
        ImagePlus cpy = img.duplicate();
        IJ.run(cpy, "Enhance Contrast...", "saturated=0.3 normalize");
        return cpy;
    }

    public static ImagePlus SubtractBackground(ImagePlus img) {
        ImagePlus cpy = img.duplicate();
        IJ.run(cpy, "Subtract Background...", "rolling=4 sliding");
        return cpy;
    }

    public static ImagePlus ApplyGaus(ImagePlus img) {
        ImagePlus cpy = img.duplicate();
        IJ.run(cpy, "Convolve...", "text1=[1 4 6 4 1\n4 16 24 16 4\n6 24 36 24 6\n4 16 24 16 4\n1 4 6 4 1] normalize");
        return cpy;
    }

    public static ImagePlus ApplyThershold(ImagePlus imp, String thersholdMethod, double lowerThershold, double upperThershold, double[] thRet) {
        int lower, upper;
        ImagePlus cpy = imp.duplicate();
        IJ.setAutoThreshold(cpy, thersholdMethod + " dark");
        if(lowerThershold > 0 && upperThershold > 0){
            IJ.setRawThreshold(cpy, lowerThershold, upperThershold, null);
        }
        Prefs.blackBackground = true;

        if(thRet != null){
            double[] th = getAutoThreshold(cpy);
            thRet[0] = th[0];
            thRet[1] = th[1];
        }
        IJ.run(cpy, "Convert to Mask", "");

        return cpy;
    }

    public static double[] getAutoThreshold(ImagePlus imp)
    {
        ImageProcessor ip = imp.getProcessor();
        double max = ip.getMaxThreshold();
        double min = ip.getMinThreshold();
        double[] ret = {min,max};
        return ret;
    }


    public static ImagePlus MergeChannels(ImagePlus red, ImagePlus green) {
        ImagePlus ary[] = { red, green };
        RGBStackMerge rgb = new RGBStackMerge();
        ImagePlus mrg = rgb.mergeChannels(ary, true);
        return mrg;
    }

    public static void SaveImage(ImagePlus image, String imageName) {
        IJ.saveAs(image, "Jpeg", imageName + ".jpg");
    }

    public static void SaveImageWithOverlay(ImagePlus image, RoiManager rm, String imageName) {
        rm.runCommand(image, "Show All");
        ImagePlus overlayimage = image.flatten();
        IJ.saveAs(overlayimage, "Jpeg", imageName + ".jpg");
        rm.runCommand(image, "Show None");
    }

    public static void AnalyzeParticles(ImagePlus image) {
        IJ.run(image, "Set Scale...", "distance=0 known=0 unit=pixel global");
        // https://imagej.nih.gov/ij/developer/api/ij/plugin/filter/ParticleAnalyzer.html
        // ParticleAnalyzer analyzer
        // Analyzer
        IJ.run(image, "Analyze Particles...", "clear add");
    }

    public static ImagePlus paintOval(ImagePlus image){
        ImagePlus cpy = image.duplicate();
        ImageProcessor ip = cpy.getProcessor();
        IJ.setForegroundColor(255,255,0);
        IJ.setBackgroundColor(255,255,0);
        ip.setColor(java.awt.Color.WHITE);
        ip.fillOval(20, 20, 10 ,10);
        return cpy;
    }

    public static Channel MeasureImage(int chNr, String channelName,AnalyseSettings settings,ImagePlus image, RoiManager rm) {
        // https://imagej.nih.gov/ij/developer/api/ij/plugin/frame/RoiManager.html
        // multiMeasure(ImagePlus imp)
        // import ij.plugin.frame.RoiManager
        // https://imagej.nih.gov/ij/developer/api/ij/measure/ResultsTable.html
        // ij.measure.ResultsTable
        IJ.run("Clear Results", "");
        rm.runCommand(image, "Measure");
        String fileNameResult = "tempfile.csv";
        IJ.saveAs("Results", fileNameResult);
        IJ.run("Clear Results", "");
        File resultFile = new File(fileNameResult);
        Channel ch = createChannelFromMeasurement(chNr, channelName,settings,resultFile);
        resultFile.delete();
        return ch;
    }



    private static Channel createChannelFromMeasurement(int chNr, String channelName,AnalyseSettings settings, File resultsFile) {

        Channel ch = new Channel(chNr,channelName, new Statistics());
        try {
            String[] readLines = new String(
                    Files.readAllBytes(Paths.get(resultsFile.getAbsoluteFile().toString())), StandardCharsets.UTF_8)
                            .split("\n");

            // First line is header therefore start with 1
            for (int i = 1; i < readLines.length; i++) {

                String[] line = readLines[i].split(",");

                double areaSize = 0.0;

                try {
                    areaSize = Double.parseDouble(line[RESULT_FILE_IDX_AREA_SIZE]);
                } catch (NumberFormatException ex) {
                }

                double grayScale = 0.0;
                try {
                    grayScale = Double.parseDouble(line[RESULT_FILE_IDX_MEAN_GRAYSCALE]);
                } catch (NumberFormatException ex) {
                }

                double circularity = 0.0;
                try {
                    circularity = Double.parseDouble(line[RESULT_FILE_IDX_CIRCULARITY]);
                } catch (NumberFormatException ex) {
                }

                int roiNr = 0;
                try {
                    roiNr = Integer.parseInt(line[RESULT_FILE_ROI_IDX]);
                } catch (NumberFormatException ex) {
                }

                ParticleInfo exosom = new ParticleInfo(roiNr, areaSize, grayScale, circularity);
                exosom.validatearticle(settings.mMinParticleSize, settings.mMaxParticleSize,
                settings.mMinCircularity, settings.minIntensity);
                ch.addRoi(exosom);
            }

            ch.calcStatistics();

        } catch (IOException ex) {
            IJ.log("Catch: " + ex.getMessage());
            ex.printStackTrace();
        }

        return ch;
    }



}
