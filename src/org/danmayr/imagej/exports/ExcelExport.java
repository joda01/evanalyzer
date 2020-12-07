package org.danmayr.imagej.exports;

import ij.*;

import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;

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

public class ExcelExport {
    ExcelExport() {

    }

    /*
     * public static void Export(String outputFolder, FolderResults results) {
     * 
     * FileOutputStream fileOutputStream = null;
     * 
     * Workbook workBook = new SXSSFWorkbook(2000); CreationHelper createHelper =
     * workBook.getCreationHelper();
     * 
     * int rowNum = 0;
     * 
     * outputFolder = outputFolder + "/result.xlsx"; int sheetNr = 0;
     * 
     * SXSSFSheet overview = (SXSSFSheet) workBook.createSheet("overview"); int
     * overviewRowCnt = 2; for (Map.Entry<String, Folder> entry :
     * results.getFolders().entrySet()) { String folderName = entry.getKey(); int
     * overViewRow = 0;
     * 
     * Folder folder = entry.getValue(); for (Map.Entry<String, Image> entry1 :
     * folder.getImages().entrySet()) { String imgName = entry1.getKey(); Image
     * image = entry1.getValue();
     * 
     * // Overview table Row overviewRowChannelName = overview.createRow(0); Row
     * overviewRowHeader = overview.createRow(1);
     * 
     * Row overviewRow = overview.createRow(overviewRowCnt++);
     * overviewRow.createCell(0).setCellValue(folderName);
     * overviewRow.createCell(1).setCellValue(imgName);
     * 
     * // Link to sheet Cell linkCell = overviewRow.createCell(2); String sheetName
     * = Integer.toString(sheetNr); linkCell.setCellValue("Sheet Nr: " + sheetName);
     * Hyperlink link2 = createHelper.createHyperlink(HyperlinkType.DOCUMENT);
     * link2.setAddress("'" + sheetName + "'!A1"); linkCell.setHyperlink(link2);
     * 
     * SXSSFSheet imageSheet = (SXSSFSheet) workBook.createSheet(sheetName);
     * sheetNr++; int column = 1; int columnSumary = 4; int columnAdd = 0; int
     * columnAddSumary = 0;
     * 
     * String[] channelNames = image.getChannelNames(); String[] channelTitles =
     * image.getChannelTitles();
     * 
     * for (Map.Entry<Integer, Channel> entry2 : image.getChannels().entrySet()) {
     * int chName = entry2.getKey(); Channel channel = entry2.getValue();
     * 
     * int row = 2; // Write channel name Row chanelNameRow =
     * imageSheet.createRow(0);
     * chanelNameRow.createCell(column).setCellValue(channel.toString());
     * 
     * // Write summary for the channel
     * overviewRowChannelName.createCell(columnSumary).setCellValue(channel.toString
     * ());
     * 
     * String[] overViewTitle = channel.getTitle(); for (int x = 0; x <
     * overViewTitle.length; x++) { overviewRowHeader.createCell(columnSumary +
     * x).setCellValue(overViewTitle[x]); } columnAddSumary = overViewTitle.length +
     * 1;
     * 
     * double[] sumarayValues = channel.getValues(); for (int x = 0; x <
     * sumarayValues.length; x++) { overviewRow.createCell(columnSumary +
     * x).setCellValue(sumarayValues[x]); }
     * 
     * // Link to picture Cell linkPicture = overviewRow.createCell(columnSumary +
     * sumarayValues.length); String fileName = channel.getCtrlImagePath();
     * linkPicture.setCellValue("Open control picture"); Hyperlink linkImage =
     * createHelper.createHyperlink(HyperlinkType.FILE);
     * linkImage.setAddress(fileName); linkPicture.setHyperlink(linkImage);
     * 
     * column += columnAdd; columnSumary += columnAddSumary; } }
     * 
     * // // Write folder statistics // TreeMap<Integer, double[]> folderStatistics
     * = folder.calcStatistic();
     * 
     * overviewRowCnt++; Row overviewRow = overview.createRow(overviewRowCnt); int
     * folderSumaryCell = 4;
     * 
     * for (Map.Entry<Integer, double[]> folderStatistic :
     * folderStatistics.entrySet()) { double values[] = folderStatistic.getValue();
     * for (int x = 0; x < values.length; x++) {
     * overviewRow.createCell(folderSumaryCell).setCellValue(values[x]);
     * folderSumaryCell++; } folderSumaryCell++; // One free column because this
     * column is used for the image link } overviewRowCnt++;
     * 
     * }
     * 
     * try { fileOutputStream = new FileOutputStream(outputFolder.trim());
     * workBook.write(fileOutputStream); } catch (Exception exObj) { }
     * 
     * }
     */

    public static void Export(String outputFolder, FolderResults results) {
        Workbook workBook = new SXSSFWorkbook(2000);
        CreationHelper createHelper = workBook.getCreationHelper();

        // Overview Sheet
        SXSSFSheet overviewSheet = (SXSSFSheet) workBook.createSheet("overview");
        int overViewRow = 0;

        //
        //
        //
        for (Map.Entry<String, Folder> folderMap : results.getFolders().entrySet()) {
            String folderName = folderMap.getKey();
            Folder folder = folderMap.getValue();
            overViewRow = WriteOverviewHeaderForFolder(overviewSheet, folder, overViewRow);

            //
            // Process images in Folder
            //
            int imgSheetCount = 0;
            for (Map.Entry<String, Image> imageMap : folder.getImages().entrySet()) {
                imgSheetCount++;
                String imageName = imageMap.getKey();
                Image image = imageMap.getValue();
                String sheetName = Integer.toString(imgSheetCount);
                SXSSFSheet imageSheet = (SXSSFSheet) workBook.createSheet(sheetName);

                //
                // Write image sumary to overview sheet
                //
                overViewRow = WriteOverviewImageSummery(overviewSheet, sheetName, createHelper, image, folderName,
                        imageName, overViewRow);

                //
                // Write image Particles
                //
                WriteImageSheet(imageSheet, image);

            }

            //
            // Write the folder statistic to the end of the folder section
            //
            overViewRow = WriteOverviewFolderStatistics(overviewSheet, folder, overViewRow);
        }

        try {
            String out = outputFolder + "/result.xlsx";
            FileOutputStream fileOutputStream = new FileOutputStream(out.trim());
            workBook.write(fileOutputStream);
        } catch (Exception exObj) {
        }

    }

    static int OVERVIEW_SHEET_START_STATISTIC_COLUMN = 4;

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

            for(int n=0;n<valVec.size();n++){
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
