package org.danmayr.imagej.exports;

import java.io.File;

import org.danmayr.imagej.algorithm.AnalyseSettings;
import org.danmayr.imagej.algorithm.structs.Image;

abstract public class ReportGenerator {

    private String fileName = "";


    public ReportGenerator(String outputFolder, String reportFileName, AnalyseSettings settings) {
        fileName = outputFolder + File.separator + reportFileName + ".csv".trim();

    }

    public String getFileName() {
        return fileName;
    }

    public abstract void writeHeader(String folderName, Image folder);
    public abstract void writeRow(String folderName, Image img);
}
