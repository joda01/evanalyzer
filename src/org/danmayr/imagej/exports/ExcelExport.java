package org.danmayr.imagej.exports;

import ij.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.apache.commons.lang3.math.*;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Cell;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.*;

import org.apache.poi.ss.usermodel.Hyperlink;
import org.apache.poi.common.usermodel.HyperlinkType;

//import org.apache.poi.xssf.usermodel.XSSFWorkbook;
//import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.*;

import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.streaming.SXSSFSheet;

import com.opencsv.CSVReader;
import java.util.*;

import org.danmayr.imagej.algorithm.structs.*;
import org.danmayr.imagej.algorithm.structs.Pair;
import org.danmayr.imagej.algorithm.AnalyseSettings;

import org.danmayr.imagej.gui.EvColocDialog;

public class ExcelExport {
    ExcelExport() {

    }

    public static String Export(String outputFolder, String reportFileName, FolderResults results, AnalyseSettings.ReportType reportType, AnalyseSettings settings,
            EvColocDialog mDialog) {

        mDialog.setProgressBarValue(0, "generating report ...");

        int max = getLoopCount(results);
        mDialog.setProgressBarMaxSize(max, "generating report ...");

        Workbook workBook = new SXSSFWorkbook(2000);
        CreationHelper createHelper = workBook.getCreationHelper();


        // Overview Sheet
        SXSSFSheet overviewSheet = (SXSSFSheet) workBook.createSheet("overview");
        int overViewRow = 0;
        int imgSheetCount = 0;
        
        // Summary Sheet
        SXSSFSheet summerySheet = (SXSSFSheet) workBook.createSheet("settings");
        WriteSummary(summerySheet,settings);

        //
        // Process folders
        //
        for (Map.Entry<String, Folder> folderMap : results.getFolders().entrySet()) {
            String folderName = folderMap.getKey();
            Folder folder = folderMap.getValue();
            overViewRow = WriteOverviewHeaderForFolder(overviewSheet, folder, overViewRow);

            //
            // Process images in Folder
            //
            for (Map.Entry<String, Image> imageMap : folder.getImages().entrySet()) {
                imgSheetCount++;
                String imageName = imageMap.getKey();
                Image image = imageMap.getValue();
                String sheetName = Integer.toString(imgSheetCount);

                //
                // Write image sumary to overview sheet
                //
                overViewRow = WriteOverviewImageSummery(overviewSheet, sheetName, createHelper, image, folderName,
                        imageName, overViewRow);

                //
                // Write image Particles
                //
                if (reportType == AnalyseSettings.ReportType.FullReport) {
                    SXSSFSheet imageSheet = (SXSSFSheet) workBook.createSheet(sheetName);
                    WriteImageSheet(imageSheet, image);
                }
                mDialog.incrementProgressBarValue("generating report ...");

            }

            //
            // Write the folder statistic to the end of the folder section
            //
            overViewRow = WriteOverviewFolderStatistics(overviewSheet, folder, overViewRow);
        }

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HHmmss");
        LocalDateTime now = LocalDateTime.now();
        String out = outputFolder + File.separator + dtf.format(now)+"__analysis-report__"+reportFileName+".xlsx";
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(out.trim());
            workBook.write(fileOutputStream);
        } catch (Exception exObj) {
        }
        return out;
    }

    ///
    /// Calculate how many loop iterations will be done
    ///
    private static int getLoopCount(FolderResults results) {
        int max = 0;
        for (Map.Entry<String, Folder> folderMap : results.getFolders().entrySet()) {
            Folder folder = folderMap.getValue();

            max = max + folder.getImages().size();

        }
        return max;

    }

    static int OVERVIEW_SHEET_START_STATISTIC_COLUMN = 4;


    ///
    /// Write summary
    ///
    private static int WriteSummary(SXSSFSheet summarySheet, AnalyseSettings settings)
    {
        int row = 0;
        summarySheet.setDefaultColumnWidth(25);
        row = WriteRow(summarySheet,row,"Save Cotrol Pictures",String.valueOf(settings.mSaveDebugImages));
        row = WriteRow(summarySheet,row,"Report Type",String.valueOf(settings.reportType));

        row = WriteRow(summarySheet,row,"Used Function",String.valueOf(settings.mSelectedFunction));

        row = WriteRow(summarySheet,row,"Input folder",String.valueOf(settings.mInputFolder));
        row = WriteRow(summarySheet,row,"Output folder",String.valueOf(settings.mOutputFolder));
        row = WriteRow(summarySheet,row,"Selected Series",String.valueOf(settings.mSelectedSeries));
        row = WriteRow(summarySheet,row,"Min particle Size",String.valueOf(settings.mMinParticleSize));
        row = WriteRow(summarySheet,row,"Max particle Size",String.valueOf(settings.mMaxParticleSize));


        row = WriteRow(summarySheet,row,"Min Circularity",String.valueOf(settings.mMinCircularity));
        row = WriteRow(summarySheet,row,"Min Intensity",String.valueOf(settings.minIntensity));

        row = WriteRow(summarySheet,row,"Report filename",String.valueOf(settings.mOutputFileName));

        row = WriteChannelSettingToSummarySheet(summarySheet, row,"CH0",settings.ch0);
        row = WriteChannelSettingToSummarySheet(summarySheet, row,"CH1",settings.ch1);
        return row;
    }


    private static int WriteChannelSettingToSummarySheet(SXSSFSheet summarySheet, int row, String chName, AnalyseSettings.ChannelSettings ch )
    {
        row = WriteRow(summarySheet,row,chName+" type",            String.valueOf(ch.type));
        row = WriteRow(summarySheet,row,chName+" therhsolding",    String.valueOf(ch.mThersholdMethod));
        row = WriteRow(summarySheet,row,chName+" enhance contrast",String.valueOf(ch.enhanceContrast));
        row = WriteRow(summarySheet,row,chName+" min Threshold",   String.valueOf(ch.minThershold));
        row = WriteRow(summarySheet,row,chName+" max Threshold",   String.valueOf(ch.maxThershold));
        return row;
    }

    private static int WriteRow(SXSSFSheet summarySheet,int row, String title, String value)
    {
        Row rowChName = summarySheet.createRow(row);
        rowChName.createCell(0).setCellValue(title);
        rowChName.createCell(1).setCellValue(value);
        row++;
        return row;
    }

    ///
    /// Write overview header
    ///
    private static int WriteOverviewHeaderForFolder(SXSSFSheet overviewSheet, Folder folder, int row) {
        Row rowChName = overviewSheet.createRow(row);
        Row rowTitles = overviewSheet.createRow(row + 1);

        TreeMap<Integer, Pair<String, String[]>> titles = folder.getStatisticTitle();
        int column = OVERVIEW_SHEET_START_STATISTIC_COLUMN;
        for (Map.Entry<Integer, Pair<String, String[]>> value : titles.entrySet()) {
            rowChName.createCell(column).setCellValue(value.getValue().getFirst());
            column++; // Additioal column at beginning. This is the line with the link to the picture
            for (int n = 0; n < value.getValue().getSecond().length; n++) {
                rowTitles.createCell(column).setCellValue(value.getValue().getSecond()[n]);
                column++;
            }
        }
        return row + 2;
    }

    ///
    /// Write image summary
    ///
    private static int WriteOverviewImageSummery(SXSSFSheet overviewSheet, String sheetName,
            CreationHelper createHelper, Image image, String folderName, String imageName, int row) {
        Row rowImgSummary = overviewSheet.createRow(row);

        rowImgSummary.createCell(0).setCellValue(folderName);
        rowImgSummary.createCell(1).setCellValue(imageName);

        Cell linkCell = rowImgSummary.createCell(2);
        linkCell.setCellValue("Go to " + sheetName);
        Hyperlink link2 = createHelper.createHyperlink(HyperlinkType.DOCUMENT);
        link2.setAddress("'" + sheetName + "'!A1");
        linkCell.setHyperlink(link2);

        TreeMap<Integer, String> ctrlImages = image.getCtrlImages();

        TreeMap<Integer, double[]> values = image.getStatistics();
        int column = OVERVIEW_SHEET_START_STATISTIC_COLUMN;
        for (Map.Entry<Integer, double[]> value : values.entrySet()) {

            Cell picCell = rowImgSummary.createCell(column);
            picCell.setCellValue("Open pic");
            Hyperlink picLik = createHelper.createHyperlink(HyperlinkType.FILE);
            picLik.setAddress(ctrlImages.get(value.getKey()));
            picCell.setHyperlink(picLik);
            column++;
            for (int n = 0; n < value.getValue().length; n++) {
                rowImgSummary.createCell(column).setCellValue(value.getValue()[n]);
                column++;
            }
        }
        return row + 1;
    }

    ///
    /// Write image statistics
    ///
    private static void WriteImageSheet(SXSSFSheet imgSheet, Image image) {
        ///
        /// Write header
        ///
        Row rowChName = imgSheet.createRow(0);
        Row rowTitles = imgSheet.createRow(0 + 1);

        TreeMap<Integer, Pair<String, String[]>> titles = image.getTitle();
        int column = 1;
        for (Map.Entry<Integer, Pair<String, String[]>> value : titles.entrySet()) {
            rowChName.createCell(column).setCellValue(value.getValue().getFirst());
            for (int n = 0; n < value.getValue().getSecond().length; n++) {
                rowTitles.createCell(column).setCellValue(value.getValue().getSecond()[n]);
                column++;
            }
        }

        ///
        /// Write values
        ///
        TreeMap<Integer, Vector<double[]>> rows = new TreeMap<>();

        for (Map.Entry<Integer, Channel> channelMap : image.getChannels().entrySet()) {
            Channel channel = channelMap.getValue();
            for (Map.Entry<Integer, ParticleInfo> roiMap : channel.getRois().entrySet()) {
                ParticleInfo info = roiMap.getValue();
                double values[] = info.getValues();

                Vector<double[]> vec = rows.get(roiMap.getKey());
                if (null == vec) {
                    vec = new Vector<>();
                    rows.put(roiMap.getKey(), vec);
                }
                vec.add(values);
            }
        }

        int row = 2;
        for (Map.Entry<Integer, Vector<double[]>> entry3 : rows.entrySet()) {
            column = 1;
            Row currentRow = imgSheet.createRow(row);
            currentRow.createCell(0).setCellValue(entry3.getKey());
            Vector<double[]> valVec = entry3.getValue();

            for (int n = 0; n < valVec.size(); n++) {
                for (int c = 0; c < valVec.get(n).length; c++) {
                    currentRow.createCell(column).setCellValue(valVec.get(n)[c]);
                    column++;
                }
            }
            row++;
        }

    }

    ///
    /// Write folder statistics
    ///
    private static int WriteOverviewFolderStatistics(SXSSFSheet overviewSheet, Folder folder, int row) {
        Row rowFolderStatistics = overviewSheet.createRow(row);

        TreeMap<Integer, double[]> statistics = folder.calcStatistic();
        int column = OVERVIEW_SHEET_START_STATISTIC_COLUMN;
        for (Map.Entry<Integer, double[]> value : statistics.entrySet()) {
            column++; // Additioal column at beginning. This is the line with the link to the picture
            for (int n = 0; n < value.getValue().length; n++) {
                rowFolderStatistics.createCell(column).setCellValue(value.getValue()[n]);
                column++;
            }
        }
        return row + 2;
    }

}
