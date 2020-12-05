
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

public class Filter {
    static int RESULT_FILE_ROI_IDX = 0;
    static int RESULT_FILE_IDX_AREA_SIZE = 1;
    static int RESULT_FILE_IDX_MEAN_GRAYSCALE = 2;
    static int RESULT_FILE_IDX_CIRCULARITY = 3;

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

    public static ImagePlus ApplyThershold(ImagePlus imp, String thersholdMethod) {
        ImagePlus cpy = imp.duplicate();
        IJ.setAutoThreshold(cpy, thersholdMethod + " dark");
        Prefs.blackBackground = true;
        IJ.run(cpy, "Convert to Mask", "");
        return cpy;
    }

    public static ImagePlus MergeChannels(ImagePlus red, ImagePlus green) {
        ImagePlus ary[] = { red, green };
        RGBStackMerge rgb = new RGBStackMerge();
        ImagePlus mrg = rgb.mergeChannels(ary, true);
        return mrg;
    }

    public static void SaveImageWithOverlay(ImagePlus image, RoiManager rm, String imageName) {
        rm.runCommand(image, "Show None");
        IJ.saveAs(image, "Jpeg", imageName + "_merged.jpg");
        rm.runCommand(image, "Show All");
        ImagePlus overlayimage = image.flatten();
        IJ.saveAs(overlayimage, "Jpeg", imageName + "_overlay.jpg");
    }

    public static void AnalyzeParticles(ImagePlus image) {
        IJ.run(image, "Set Scale...", "distance=0 known=0 unit=pixel global");
        // https://imagej.nih.gov/ij/developer/api/ij/plugin/filter/ParticleAnalyzer.html
        // ParticleAnalyzer analyzer
        // Analyzer
        IJ.run(image, "Analyze Particles...", "clear add");
    }


    public static Channel MeasureImage(int chNr, String channelName,ImagePlus image, RoiManager rm) {
        // https://imagej.nih.gov/ij/developer/api/ij/plugin/frame/RoiManager.html
        // multiMeasure(ImagePlus imp)
        // import ij.plugin.frame.RoiManager
        // https://imagej.nih.gov/ij/developer/api/ij/measure/ResultsTable.html
        // ij.measure.ResultsTable
        IJ.run("Clear Results", "");
        rm.runCommand(image, "Measure");
        String fileNameResult = "tempFile.csv";
        IJ.saveAs("Results", fileNameResult);
        IJ.run("Clear Results", "");
        File resultFile = new File(fileNameResult);
        Channel ch = createChannelFromMeasurement(chNr, channelName,resultFile);
        resultFile.delete();
        return ch;
    }



    private static Channel createChannelFromMeasurement(int chNr, String channelName, File resultsFile) {

        Channel ch = new Channel(chNr,channelName);
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

                ParticleInfo exosom = new ParticleInfo(line[RESULT_FILE_ROI_IDX], areaSize, grayScale, circularity);
                ch.addRoi(exosom);
            }

        } catch (IOException ex) {
            IJ.log("Catch: " + ex.getMessage());
        }

        return ch;
    }



}
