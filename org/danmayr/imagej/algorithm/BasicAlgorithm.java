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
    protected String mAlloverStatistics;
	private String convertCsvToXls;


    public BasicAlgorithm(AnalyseSettings analyseSettings) {
        mAnalyseSettings = analyseSettings;
    }

    abstract public void analyseImage(File imageFile);


    abstract public String writeAllOverStatisticToFile(String filename);


    protected void EnhanceContrast(ImagePlus img) {
        IJ.run(img, "Enhance Contrast...", "saturated=0.3 normalize");
    }

    protected void ApplyFilter(ImagePlus img) {
        IJ.run(img, "Subtract Background...", "rolling=4 sliding");
        IJ.run(img, "Convolve...", "text1=[1 4 6 4 1\n4 16 24 16 4\n6 24 36 24 6\n4 16 24 16 4\n1 4 6 4 1] normalize");
    }

    protected ImagePlus ApplyTherhold(ImagePlus imp) {
        ImagePlus cpy = imp.duplicate();
        IJ.setAutoThreshold(cpy, mAnalyseSettings.mThersholdMethod + " dark");
        Prefs.blackBackground = true;
        IJ.run(cpy, "Convert to Mask", "");
        return cpy;
    }

    protected void MergeChannels(ImagePlus red, ImagePlus green, File imageFile, RoiManager rm) {

        IJ.run("Merge Channels...", "c1=[" + red.getTitle() + "] c2=[" + green.getTitle() + "] keep");
        ImagePlus imp = WindowManager.getImage("RGB");

        SaveImageWithOverlay(imp,imageFile,rm);
    }

    protected void SaveImageWithOverlay(ImagePlus image, File imageFile, RoiManager rm){
        IJ.saveAs(image, "Jpeg", mAnalyseSettings.mOutputFolder + File.separator + imageFile.getName() + "_merged.jpg");
        
        rm.runCommand(image, "Show All");
        image = image.flatten();
        IJ.saveAs(image, "Jpeg",mAnalyseSettings.mOutputFolder + File.separator + imageFile.getName() + "_overlay.jpg");
    }

    protected String MeasureAndSaveResult(ImagePlus image,File imageFile,RoiManager rm, String fileNamePraefix){
        IJ.run("Clear Results", "");
        rm.runCommand(image, "Measure");
        String fileNameResult = mAnalyseSettings.mOutputFolder + File.separator + imageFile.getName()+fileNamePraefix +".csv";
        IJ.saveAs("Results", fileNameResult);
        IJ.run("Clear Results", "");
        return fileNameResult;
    }

    protected void AnalyzeParticles(ImagePlus image){
        IJ.run(image, "Set Scale...", "distance=0 known=0 unit=pixel global");
        IJ.run(image, "Analyze Particles...", "clear add");
    }
}
