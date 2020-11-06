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


    public void writeAllOverStatisticToFile() {
        try {
            String outputfilename = mAnalyseSettings.mOutputFolder + File.separator + "statistic_all_over_final.txt";
            String outputfilenameXlsx = mAnalyseSettings.mOutputFolder + File.separator + "statistic_all_over_final";

            
            for (Map.Entry<String, MeasurementStruct> entry : mMeanValues.entrySet()) {
                MeasurementStruct struct = entry.getValue();
                
                struct.calcMean();
                String mean = struct.toString();
                mAlloverStatistics = mAlloverStatistics + mean + "\n";
            }            
            
            
            
            BufferedWriter writer = new BufferedWriter(new FileWriter(outputfilename));
            writer.write(mAlloverStatistics);
            writer.close();

            convertCsvToXls = CsvToExcel.convertCsvToXls(outputfilenameXlsx, outputfilename);

        } catch (IOException ex) {

        }
    }


    protected void EnhanceContrast(ImagePlus img) {
        IJ.run(img, "Enhance Contrast...", "saturated=0.3 normalize");
    }

    protected void ApplyFilter(ImagePlus img) {
        IJ.run(img, "Subtract Background...", "rolling=4 sliding");
        IJ.run(img, "Convolve...", "text1=[1 4 6 4 1\n4 16 24 16 4\n6 24 36 24 6\n4 16 24 16 4\n1 4 6 4 1] normalize");
    }

    protected void ApplyTherhold(ImagePlus imp) {
        IJ.setAutoThreshold(imp, mAnalyseSettings.mThersholdMethod + " dark");
        Prefs.blackBackground = true;
        IJ.run(imp, "Convert to Mask", "");
    }

    protected void MergeChannels(ImagePlus red, ImagePlus green, File imageFile, RoiManager rm) {

        IJ.run("Merge Channels...", "c1=[" + red.getTitle() + "] c2=[" + green.getTitle() + "] keep");
        ImagePlus imp = WindowManager.getImage("RGB");
        IJ.saveAs(imp, "Jpeg", mAnalyseSettings.mOutputFolder + File.separator + imageFile.getName() + "_merged.jpg");

        SaveImageWithOverlay(imp,imageFile,rm);
    }

    protected void SaveImageWithOverlay(ImagePlus image, File imageFile, RoiManager rm){
        rm.runCommand(image, "Show All");
        image = image.flatten();
        IJ.saveAs(image, "Jpeg",mAnalyseSettings.mOutputFolder + File.separator + imageFile.getName() + "_overlay.jpg");
    }
}
