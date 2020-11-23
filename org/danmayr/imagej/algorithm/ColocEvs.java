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
import ij.plugin.ImageInfo;

import java.awt.*;

import org.danmayr.imagej.excel.CsvToExcel;
import org.danmayr.imagej.gui.EvColocDialog;


public class ColocEvs extends CountEvs {

    /**
     * Creates a new analysing thread
     * 
     * @param dialog
     * @param analyseSettings
     */
    public ColocEvs(AnalyseSettings analyseSettings) {
        super(analyseSettings);

    }


    @Override
    public void analyseImage(File imageFile) {
        super.analyseImage(imageFile);

        String folderName = imageFile.getParent();

        String[] imageTitles = WindowManager.getImageTitles();

        //
        // Find red and green channel images
        //
        ImagePlus redChannel = null; // CY3
        ImagePlus greenChannel = null; // GFP
        for (int i = 0; i < imageTitles.length; i++) {
            String actTitle = imageTitles[i];
            ImagePlus img = WindowManager.getImage(actTitle);

            ImageInfo info = new ImageInfo();
            String imgInfo = info.getImageInfo(img);
            IJ.log(imgInfo);
            // Red Channel selection
            if (true == actTitle.endsWith("C=" + Integer.toString(mAnalyseSettings.mGreenChannel))) {
                greenChannel = img;
            } else {
                redChannel = img;
            }
        }

        redChannel= ApplyTherhold(redChannel);
        greenChannel=ApplyTherhold(greenChannel);

        // Calculate the sum of both images
        ImageCalculator ic = new ImageCalculator();
        ImagePlus sumImage = ic.run("Max create", greenChannel, redChannel);
        IJ.run(sumImage, "Set Scale...", "distance=0 known=0 unit=pixel global");
        // Merge red and green channels
        MergeChannels(redChannel, greenChannel, imageFile.getName(), rm);
        // Analyze particles
        AnalyzeParticles(sumImage);

        File redResult = MeasureAndSaveResult(redChannel, imageFile.getName(), rm, "red");
        File greenResult = MeasureAndSaveResult(greenChannel, imageFile.getName(), rm, "green");

        analyseChannel(folderName, imageFile.getName(),"colocRed", redResult, redResult);
        analyseChannel(folderName, imageFile.getName(), "colocGreen", greenResult, greenResult);
    }

}
