package org.danmayr.imagej.exports;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Hyperlink;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.danmayr.imagej.Version;
import org.danmayr.imagej.algorithm.AnalyseSettings;
import org.danmayr.imagej.algorithm.ChannelSettings;
import org.danmayr.imagej.algorithm.pipelines.Pipeline.ChannelType;
import org.danmayr.imagej.algorithm.structs.Image;
import org.danmayr.imagej.algorithm.structs.Pair;

import ij.IJ;

public class CSVExportStream {

    static String SEPARATOR = ";";
    static int OVERVIEW_SHEET_START_STATISTIC_COLUMN = 4;
    private String fileName = "";

    public CSVExportStream(String outputFolder, String reportFileName, AnalyseSettings settings) {
        fileName = outputFolder + File.separator + reportFileName + ".csv".trim();
    }

    public String getFileName() {
        return fileName;
    }

    boolean folderWritten = false;

    //
    // Write CSV file header
    //
    public synchronized void writeHeader(String folderName, Image folder) {
        if (false == folderWritten) {
            folderWritten = true;
            String rowChannelNames = "";
            String rowTitles = "";
            for (int n = 0; n < OVERVIEW_SHEET_START_STATISTIC_COLUMN; n++) {
                rowChannelNames += SEPARATOR;
                rowTitles += SEPARATOR;
            }

            TreeMap<ChannelType, Pair<String, String[]>> titles = folder.getStatisticTitle();
            for (Map.Entry<ChannelType, Pair<String, String[]>> value : titles.entrySet()) {
                // Channel Name
                rowChannelNames = rowChannelNames + SEPARATOR + value.getValue().getFirst();

                // Value titles
                rowTitles = rowTitles + SEPARATOR + "-";
                for (int n = 0; n < value.getValue().getSecond().length; n++) {
                    rowTitles = rowTitles + SEPARATOR + value.getValue().getSecond()[n];
                    rowChannelNames = rowChannelNames + SEPARATOR;
                }
            }
            AppendLine(rowChannelNames);
            AppendLine(rowTitles);
        }
    }

    //
    // Append line to csv file
    //
    private void AppendLine(String line) {
        try {
            FileWriter fw = new FileWriter(this.fileName, true); // the true will append the new data
            fw.write(line + "\n");// appends the string to the file
            fw.close();
        } catch (IOException ioe) {
            System.err.println("IOException: " + ioe.getMessage());
        }
    }

    private String createHyperlink(String text, String link){
        return "=HYPERLINK(\""+link+"\",\""+text+"\")";
    }

    //
    // Append a row to the overview excel
    //
    public synchronized void writeRow(String folderName, Image img) {

        String imageName = img.getImageName();

        ///
        /// Folder name
        ///
        String row = folderName;

        ///
        /// Original image name
        ///
        row = row + SEPARATOR + imageName;

        ///
        /// Internal image name
        ///
        row = row + SEPARATOR + img.getUUID();

        ///
        /// Link to details XLSX
        ///
        row = row +SEPARATOR + createHyperlink("Open Details",img.getUUID() + ".xlsx")+SEPARATOR;



        TreeMap<ChannelType, String> ctrlImages = img.getCtrlImages();

        ///
        /// Write values for one image
        ///
        TreeMap<ChannelType, double[]> values = img.getStatistics();
        for (Map.Entry<ChannelType, double[]> value : values.entrySet()) {
            String rlativePath = ctrlImages.get(value.getKey());
            rlativePath = rlativePath.replace("\\", "/");
            
            if (rlativePath.length() > 0) {
                row = row + SEPARATOR + createHyperlink("Open pic",rlativePath);

            } else {
                row = row + SEPARATOR + "-";
            }
            for (int n = 0; n < value.getValue().length; n++) {
                row = row + SEPARATOR + value.getValue()[n];
            }
        }

        AppendLine(row);

    }

    ///
    /// Write summary
    ///
    private static int WriteSummary(XSSFSheet summarySheet, AnalyseSettings settings) {
        int row = 0;
        summarySheet.setDefaultColumnWidth(25);
        row = ExcelExport.WriteRow(summarySheet, row, "Used program Version", Version.getVersion());

        row = ExcelExport.WriteRow(summarySheet, row, "Save Cotrol Pictures",
                String.valueOf(settings.mSaveDebugImages));
        row = ExcelExport.WriteRow(summarySheet, row, "Report Type", String.valueOf(settings.reportType));

        row = ExcelExport.WriteRow(summarySheet, row, "Used Function", String.valueOf(settings.mSelectedFunction));

        row = ExcelExport.WriteRow(summarySheet, row, "Input folder", String.valueOf(settings.mInputFolder));
        row = ExcelExport.WriteRow(summarySheet, row, "Output folder", String.valueOf(settings.mOutputFolder));
        row = ExcelExport.WriteRow(summarySheet, row, "Selected Series", String.valueOf(settings.mSelectedSeries));

        row = ExcelExport.WriteRow(summarySheet, row, "Count EVs per Cells",
                String.valueOf(settings.countEvsPerCell()));

        row = ExcelExport.WriteRow(summarySheet, row, "Report name", String.valueOf(settings.mReportName));

        for (int n = 0; n < settings.channelSettings.size(); n++) {
            row = WriteChannelSettingToSummarySheet(summarySheet, row,
                    "C=" + settings.channelSettings.get(n).mChannelNr, settings.channelSettings.get(n));
        }
        return row;
    }

    private static int WriteChannelSettingToSummarySheet(XSSFSheet summarySheet, int row, String chName,
            ChannelSettings ch) {
        row = ExcelExport.WriteRow(summarySheet, row, chName + " type", String.valueOf(ch.type));
        row = ExcelExport.WriteRow(summarySheet, row, chName + " thresholding", String.valueOf(ch.mThersholdMethod));
        row = ExcelExport.WriteRow(summarySheet, row, chName + " enhance contrast", String.valueOf(ch.enhanceContrast));
        row = ExcelExport.WriteRow(summarySheet, row, chName + " min Threshold", String.valueOf(ch.minThershold));
        row = ExcelExport.WriteRow(summarySheet, row, chName + " max Threshold", String.valueOf(ch.maxThershold));
        row = ExcelExport.WriteRow(summarySheet, row, chName + " min Circularity",
                String.valueOf(ch.getMinCircularityDouble()));
        row = ExcelExport.WriteRow(summarySheet, row, chName + " Min particle Size",
                String.valueOf(ch.getMinParticleSizeDouble()));
        row = ExcelExport.WriteRow(summarySheet, row, chName + " Max particle Size",
                String.valueOf(ch.getMaxParticleSizeDouble()));
        row = ExcelExport.WriteRow(summarySheet, row, chName + " Margin crop",
                String.valueOf(ch.getMarginCropDouble()));
        row = ExcelExport.WriteRow(summarySheet, row, chName + " Z-Projection ", String.valueOf(ch.ZProjector));
        row = ExcelExport.WriteRow(summarySheet, row, chName + " Preprocessing ", String.valueOf(ch.preProcessing));

        return row;
    }
}
