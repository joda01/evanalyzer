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

import org.danmayr.imagej.excel.CsvToExcel;
import org.danmayr.imagej.gui.EvColocDialog;

abstract public class BasicAlgorithm {
    
    protected interface MeasurementStruct{
        public void calcMean();
        public String toString();
    }
    
    AnalyseSettings mAnalyseSettings;

    // Temporary storag
	private String convertCsvToXls;


    public BasicAlgorithm(AnalyseSettings analyseSettings) {
        mAnalyseSettings = analyseSettings;
    }

    abstract public void analyseImage(File imageFile);


    abstract public String writeAllOverStatisticToFile(String filename);


    protected void EnhanceContrast(ImagePlus img) {
        IJ.run(img, "Enhance Contrast...", "saturated=0.3 normalize");
    }

    protected ImagePlus ApplyFilter(ImagePlus img) {
        ImagePlus cpy = img.duplicate();
        IJ.run(cpy, "Subtract Background...", "rolling=4 sliding");
        IJ.run(cpy, "Convolve...", "text1=[1 4 6 4 1\n4 16 24 16 4\n6 24 36 24 6\n4 16 24 16 4\n1 4 6 4 1] normalize");
        return cpy;
    }

    protected ImagePlus ApplyTherhold(ImagePlus imp) {
        ImagePlus cpy = imp.duplicate();
        IJ.setAutoThreshold(cpy, mAnalyseSettings.mThersholdMethod + " dark");
        Prefs.blackBackground = true;
        IJ.run(cpy, "Convert to Mask", "");
        return cpy;
    }

    protected void MergeChannels(ImagePlus red, ImagePlus green, String imageFile, RoiManager rm) {
        ImagePlus ary[] = {red,green};
        
        RGBStackMerge rgb = new RGBStackMerge();
        ImagePlus mrg = rgb.mergeChannels(ary,true);

        SaveImageWithOverlay(mrg,imageFile,rm);
    }

    protected void SaveImageWithOverlay(ImagePlus image, String imageName, RoiManager rm){
        rm.runCommand(image,"Show None");
        IJ.saveAs(image, "Jpeg", mAnalyseSettings.mOutputFolder + File.separator + imageName + "_merged.jpg");
        rm.runCommand(image, "Show All");
        ImagePlus overlayimage = image.flatten();
        IJ.saveAs(overlayimage, "Jpeg",mAnalyseSettings.mOutputFolder + File.separator + imageName + "_overlay.jpg");
    }

    protected File MeasureAndSaveResult(ImagePlus image,String imageName,RoiManager rm, String fileNamePraefix){
        IJ.run("Clear Results", "");
        rm.runCommand(image, "Measure");
        String fileNameResult = mAnalyseSettings.mOutputFolder + File.separator + imageName+fileNamePraefix +".csv";
        IJ.saveAs("Results", fileNameResult);
        IJ.run("Clear Results", "");
        return new File(fileNameResult);
    }

    protected void AnalyzeParticles(ImagePlus image){
        IJ.run(image, "Set Scale...", "distance=0 known=0 unit=pixel global");
        IJ.run(image, "Analyze Particles...", "clear add");
    }
}
