
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
import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.io.*;
import ij.plugin.filter.*;
import ij.plugin.Colors;
import ij.plugin.OverlayLabels;
import ij.plugin.FolderOpener;
import ij.util.*;
import ij.macro.*;
import ij.measure.*;
import ij.plugin.OverlayCommands;

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

    public static ImagePlus AddImages(ImagePlus ch0, ImagePlus ch1) {
        ImageCalculator ic = new ImageCalculator();
        ImagePlus sumImage = ic.run("Max create", ch0, ch1);
        return sumImage;
    }

    public static ImagePlus ANDImages(ImagePlus ch0, ImagePlus ch1) {
        ImageCalculator ic = new ImageCalculator();
        ImagePlus sumImage = ic.run("AND create", ch0, ch1);
        return sumImage;
    }

    public static ImagePlus duplicateImage(ImagePlus img) {
        return img.duplicate();
    }

    public static void Make16BitImage(ImagePlus img) {
        IJ.run(img, "16-bit", "");
    }

    public static void EnhanceContrast(ImagePlus img) {
        IJ.run(img, "Enhance Contrast...", "saturated=0.3 normalize");
    }

    public static void SubtractBackground(ImagePlus img) {
        IJ.run(img, "Subtract Background...", "rolling=4 sliding");
    }

    public static void ApplyGaus(ImagePlus img) {
        IJ.run(img, "Convolve...", "text1=[1 4 6 4 1\n4 16 24 16 4\n6 24 36 24 6\n4 16 24 16 4\n1 4 6 4 1] normalize");
    }

    public static void ApplyThershold(ImagePlus img, String thersholdMethod, double lowerThershold,
            double upperThershold, double[] thRet, boolean convertToMask) {
        int lower, upper;
        IJ.setAutoThreshold(img, thersholdMethod + " dark");
        if (lowerThershold >= 0 && upperThershold >= 0) {
            IJ.setRawThreshold(img, lowerThershold, upperThershold, null);
        }
        Prefs.blackBackground = true;

        if (thRet != null) {
            double[] th = getAutoThreshold(img);
            thRet[0] = th[0];
            thRet[1] = th[1];
        }

        if (true == convertToMask) {
            IJ.run(img, "Convert to Mask", "");
        }
    }

    public static double[] getAutoThreshold(ImagePlus imp) {
        ImageProcessor ip = imp.getProcessor();
        double max = ip.getMaxThreshold();
        double min = ip.getMinThreshold();
        double[] ret = { min, max };
        return ret;
    }

    //
    // Merge channels with clors
    // Each index coresponds to one color
    // {"red", "green", "blue", "gray", "cyan", "magenta", "yellow"};
    //
    public static ImagePlus MergeChannels(ImagePlus[] ary) {
        RGBStackMerge rgb = new RGBStackMerge();
        ImagePlus mrg = rgb.mergeChannels(ary, true);
        return mrg;
    }

    public static void SaveImage(ImagePlus image, String imageName) {
        IJ.saveAs(image, "Jpeg", imageName);
    }

    public static void SaveImageWithOverlay(ImagePlus image, RoiManager rm, String imageName) {
        rm.runCommand(image, "Show All without labels");
        PaintRoiLabels(image,rm);
        //IJ.run(image,rescource, "font=SanSerif label=red label_0=14 additional=none label_1=right");
        ImagePlus overlayimage = image.flatten();
        IJ.saveAs(overlayimage, "Jpeg", imageName);
        rm.runCommand(image, "Show None");
    }

    private static void PaintRoiLabels(ImagePlus image,RoiManager rm)
    {

        Overlay ov = new Overlay();

        int fontSize = 12;

        Font font = new Font("SansSerif", Font.PLAIN, fontSize);

        Roi[] rois = rm.getRoisAsArray();
        for(int n = 0;n< rois.length;n++){
            Rectangle rec = rois[n].getBounds();
            
            double p;
            if (fontSize < 16){ p = 10;}
            else if (fontSize < 24){ p = 12;}
            else{ p= 20;}
            
            double x1 = rec.getX() + rec.getWidth() + 5;
            double y1 = rec.getY() + 0.5*rec.getHeight()+p;
            
            TextRoi lbl = new TextRoi(x1, y1, Integer.toString(n+1), font);
            lbl.setStrokeColor(Color.red);
            lbl.setFillColor(Color.black);
            ov.add(lbl); 
        }

        image.setOverlay(ov);

		
    }


    public static void InvertImage(ImagePlus image){
        IJ.run(image, "Invert", "");
    }


    public static void AnalyzeParticles(ImagePlus image, RoiManager rm) {
        image.setRoi(new OvalRoi(1, 1, 1, 1));
        rm.addRoi(image.getRoi());
        rm.runCommand(image, "Delete");
        IJ.run(image, "Select None", "");
        IJ.run(image, "Set Scale...", "distance=0 known=0 unit=pixel global");
        // https://imagej.nih.gov/ij/developer/api/ij/plugin/filter/ParticleAnalyzer.html
        // ParticleAnalyzer analyzer
        // Analyzer
        IJ.run(image, "Analyze Particles...", "clear add");
    }

    public static ImagePlus paintOval(ImagePlus image) {
        ImagePlus cpy = image.duplicate();
        ImageProcessor ip = cpy.getProcessor();
        IJ.setForegroundColor(255, 255, 0);
        IJ.setBackgroundColor(255, 255, 0);
        ip.setColor(java.awt.Color.WHITE);
        ip.fillOval(20, 20, 10, 10);
        return cpy;
    }

    public static Channel MeasureImage(int chNr, String channelName, AnalyseSettings settings, ImagePlus imageOrigial,
            ImagePlus imageThershold, RoiManager rm) {
        // https://imagej.nih.gov/ij/developer/api/ij/plugin/frame/RoiManager.html
        // multiMeasure(ImagePlus imp)
        // import ij.plugin.frame.RoiManager
        // https://imagej.nih.gov/ij/developer/api/ij/measure/ResultsTable.html
        // ij.measure.ResultsTable

        File original = new File("tempfileOriginal.csv");
        File thershold = new File("tempfileThershold.csv");

        original.delete();
        thershold.delete();

        measure(imageOrigial, original, rm);
        measure(imageThershold, thershold, rm);

        Channel ch = createChannelFromMeasurement(chNr, channelName, settings, original, thershold);

        original.delete();
        thershold.delete();
        return ch;
    }

    private static void measure(ImagePlus image, File resultFileName, RoiManager rm) {
        IJ.run("Clear Results", "");
        IJ.run("Set Measurements...", "area mean min shape redirect=None decimal=3");
        rm.runCommand(image, "Measure");
        IJ.saveAs("Results", resultFileName.getAbsolutePath());
        IJ.run("Clear Results", "");
    }

    private static Channel createChannelFromMeasurement(int chNr, String channelName, AnalyseSettings settings,
            File originalFile, File thesholdFile) {

        Channel ch = new Channel(chNr, channelName, new Statistics());
        try {
            String[] readLinesThershold = new String(
                    Files.readAllBytes(Paths.get(thesholdFile.getAbsoluteFile().toString())), StandardCharsets.UTF_8)
                            .split("\n");

            String[] readLinesOriginal = new String(
                    Files.readAllBytes(Paths.get(originalFile.getAbsoluteFile().toString())), StandardCharsets.UTF_8)
                            .split("\n");

            // First line is header therefore start with 1
            for (int i = 1; i < readLinesThershold.length; i++) {

                String[] lineThershold = readLinesThershold[i].split(",");
                String[] lineOriginal = readLinesOriginal[i].split(",");

                double areaSize = 0.0;

                try {
                    areaSize = Double.parseDouble(lineOriginal[RESULT_FILE_IDX_AREA_SIZE]);
                } catch (NumberFormatException ex) {
                }

                double grayScale = 0.0;
                try {
                    grayScale = Double.parseDouble(lineOriginal[RESULT_FILE_IDX_MEAN_GRAYSCALE]);
                } catch (NumberFormatException ex) {
                }

                double thersholdScale = 0.0;
                try {
                    thersholdScale = Double.parseDouble(lineThershold[RESULT_FILE_IDX_MEAN_GRAYSCALE]);
                } catch (NumberFormatException ex) {
                }

                double circularity = 0.0;
                try {
                    circularity = Double.parseDouble(lineOriginal[RESULT_FILE_IDX_CIRCULARITY]);
                } catch (NumberFormatException ex) {
                }

                int roiNr = 0;
                try {
                    roiNr = Integer.parseInt(lineOriginal[RESULT_FILE_ROI_IDX]);
                } catch (NumberFormatException ex) {
                }

                ParticleInfo exosom = new ParticleInfo(roiNr, areaSize, grayScale, thersholdScale, circularity);
                exosom.validatearticle(settings.mMinParticleSize, settings.mMaxParticleSize, settings.mMinCircularity,
                        settings.minIntensity);
                ch.addRoi(exosom);
            }

            ch.calcStatistics();

        } catch (IOException ex) {
            IJ.log("No File: " + ex.getMessage());

            ParticleInfo exosom = new ParticleInfo(0, 1, 0, 0, 0);
            exosom.validatearticle(settings.mMinParticleSize, settings.mMaxParticleSize, settings.mMinCircularity,
                    settings.minIntensity);
            ch.addRoi(exosom);
        }

        return ch;
    }

}
