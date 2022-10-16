package org.danmayr.imagej.exports;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Hyperlink;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.IndexedColorMap;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.danmayr.imagej.algorithm.AnalyseSettings;
import org.danmayr.imagej.algorithm.ChannelSettings;
import org.danmayr.imagej.algorithm.pipelines.Pipeline.ChannelType;
import org.danmayr.imagej.algorithm.structs.Channel;
import org.danmayr.imagej.algorithm.structs.Folder;
import org.danmayr.imagej.algorithm.structs.FolderResults;
import org.danmayr.imagej.algorithm.structs.Image;
import org.danmayr.imagej.algorithm.structs.Pair;
import org.danmayr.imagej.algorithm.structs.ParticleInfo;
import org.danmayr.imagej.gui.Dialog;

import ij.IJ;

import org.danmayr.imagej.Version;

public class ExcelExportStream {

    static int OVERVIEW_SHEET_START_STATISTIC_COLUMN = 4;
    private String fileName = "";

    public ExcelExportStream(String outputFolder, String reportFileName, AnalyseSettings settings) {

        SXSSFWorkbook workBook = new SXSSFWorkbook(10);
        CreationHelper createHelper = workBook.getCreationHelper();

        // Overview Sheet
        SXSSFSheet overviewSheet = (SXSSFSheet) workBook.createSheet("overview");

        // Summary Sheet
        SXSSFSheet summerySheet = (SXSSFSheet) workBook.createSheet("settings");
        WriteSummary(summerySheet, settings);

        fileName = outputFolder + File.separator + reportFileName + ".xlsx".trim();
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(fileName);
            workBook.write(fileOutputStream);
            workBook.close();
            fileOutputStream.close();
        } catch (Exception exObj) {
        }
    }

    public String getFileName() {
        return fileName;
    }

    public void writeHeader(String folderName, Folder folder)
            throws IOException, InvalidFormatException {
        SXSSFWorkbook workbook = new SXSSFWorkbook(new XSSFWorkbook(new FileInputStream(fileName)));
        SXSSFSheet sheet = workbook.getSheetAt(0);

        //
        // Write header

        WriteOverviewHeaderForFolder(sheet, folder, 0);

        try {
            FileOutputStream out = new FileOutputStream(fileName);
            workbook.write(out);
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    //
    // Append a row to the overview excel
    //
    public void writeRow(String folderName, Image img) {

        try {
            //SXSSFWorkbook workbook = new SXSSFWorkbook(new XSSFWorkbook(new FileInputStream(fileName)));
            XSSFWorkbook original = (XSSFWorkbook)WorkbookFactory.create(new FileInputStream(fileName));
            XSSFSheet orgSheet = original.getSheetAt(0);
            //XSSFRow baseRow = orgSheet.getRow(orgSheet.getLastRowNum());	//Reference row: Copy the style of each cell
            int rowNum = orgSheet.getLastRowNum() + 1;
            SXSSFWorkbook workbook = new SXSSFWorkbook(original);
            SXSSFSheet sheet = workbook.getSheetAt(0);
            
            
            IJ.log("Row " + rowNum);
            CreationHelper createHelper = workbook.getCreationHelper();

            String imageName = img.getImageName();
            String sheetName = Integer.toString(rowNum);

            //
            // Write image sumary to overview sheet
            //
            WriteOverviewImageSummery(sheet, sheetName, createHelper, img, folderName,
                    imageName, rowNum);

            try {
                FileOutputStream out = new FileOutputStream(fileName);
                workbook.write(out);
                out.close();
                workbook.close();
                original.close();	//Loaded into SXSSF Workbook(?)Close for


            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (IOException ex) {
        }
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

    ///
    /// Write image summary
    ///
    private static int WriteOverviewImageSummery(SXSSFSheet overviewSheet, String sheetName,
            CreationHelper createHelper, Image image, String folderName, String imageName, int row) {
        Row rowImgSummary = overviewSheet.createRow(row);

        ///
        /// Cell Styles
        ///
        CellStyle actStyleThinLine;
        if (row % 2 == 0) {
            actStyleThinLine = ExcelExport.createCellStyleNumberEven(overviewSheet, false);
        } else {
            actStyleThinLine = ExcelExport.createCellStyleNumberOdd(overviewSheet, false);
        }

        CellStyle actStyleThickLine;
        if (row % 2 == 0) {
            actStyleThickLine = ExcelExport.createCellStyleNumberEven(overviewSheet, true);
        } else {
            actStyleThickLine = ExcelExport.createCellStyleNumberOdd(overviewSheet, true);
        }

        ///
        /// Folder name
        ///
        Cell cellFolderName = rowImgSummary.createCell(0);
        cellFolderName.setCellValue(folderName);
        cellFolderName.setCellStyle(actStyleThinLine);

        ///
        /// Original image name
        ///
        Cell cellImgName = rowImgSummary.createCell(1);
        cellImgName.setCellValue(imageName);
        cellImgName.setCellStyle(actStyleThinLine);

        ///
        /// Internal image name
        ///
        Cell cellInternalImageName = rowImgSummary.createCell(2);
        cellInternalImageName.setCellValue(image.getUUID());
        cellInternalImageName.setCellStyle(actStyleThinLine);

        ///
        /// Link to details XLSX
        ///
        Cell linkCell = rowImgSummary.createCell(3);
        linkCell.setCellValue("Open Details " + sheetName);
        Hyperlink link2 = createHelper.createHyperlink(HyperlinkType.FILE);
        link2.setAddress(image.getUUID() + ".xlsx");
        linkCell.setHyperlink(link2);
        linkCell.setCellStyle(actStyleThinLine);

        TreeMap<ChannelType, String> ctrlImages = image.getCtrlImages();

        ///
        /// Write values for one image
        ///
        TreeMap<ChannelType, double[]> values = image.getStatistics();
        int column = OVERVIEW_SHEET_START_STATISTIC_COLUMN;
        for (Map.Entry<ChannelType, double[]> value : values.entrySet()) {
            Cell picCell = rowImgSummary.createCell(column);
            String rlativePath = ctrlImages.get(value.getKey());

            if (rlativePath.length() > 0) {
                picCell.setCellValue("Open pic");
            } else {
                picCell.setCellValue("-");
            }
            rlativePath = rlativePath.replace("\\", "/");
            Hyperlink picLik = createHelper.createHyperlink(HyperlinkType.FILE);
            picLik.setAddress(rlativePath);
            picCell.setHyperlink(picLik);
            picCell.setCellStyle(actStyleThickLine);
            column++;
            for (int n = 0; n < value.getValue().length; n++) {
                Cell valueCell = rowImgSummary.createCell(column);
                valueCell.setCellValue(value.getValue()[n]);
                valueCell.setCellStyle(actStyleThinLine);
                valueCell.getCellStyle().setDataFormat(
                        overviewSheet.getWorkbook().getCreationHelper().createDataFormat().getFormat("0.00")); // or
                                                                                                               // #####.##
                                                                                                               // or
                                                                                                               // number
                column++;
            }
        }
        return row + 1;
    }

    ///
    /// Write overview header
    ///
    private static int WriteOverviewHeaderForFolder(SXSSFSheet overviewSheet, Folder folder, int row) {
        ///
        /// Cell Styles
        ///

        Row rowChName = overviewSheet.createRow(row);
        Row rowTitles = overviewSheet.createRow(row + 1);

        TreeMap<ChannelType, Pair<String, String[]>> titles = folder.getStatisticTitle();
        int column = OVERVIEW_SHEET_START_STATISTIC_COLUMN;
        int startColumnMerge = OVERVIEW_SHEET_START_STATISTIC_COLUMN;
        CellStyle actStyleThinLine = ExcelExport.createCellStyleHeader(overviewSheet, false);
        CellStyle actStyleThickLine = ExcelExport.createCellStyleHeader(overviewSheet, true);
        for (Map.Entry<ChannelType, Pair<String, String[]>> value : titles.entrySet()) {
            // Channel Name
            Cell rowNameFirst = rowChName.createCell(column);
            rowNameFirst.setCellValue(value.getValue().getFirst());
            rowNameFirst.setCellStyle(actStyleThickLine);

            // Value titles
            Cell emptyCell = rowTitles.createCell(column);
            emptyCell.setCellValue("-");
            emptyCell.setCellStyle(actStyleThickLine);
            column++; // Additioal column at beginning. This is the line with the link to the picture
            for (int n = 0; n < value.getValue().getSecond().length; n++) {
                Cell rowTitleCell = rowTitles.createCell(column);
                rowTitleCell.setCellValue(value.getValue().getSecond()[n]);
                rowTitleCell.setCellStyle(actStyleThinLine);
                column++;
            }
            overviewSheet.addMergedRegion(new CellRangeAddress(row, row, startColumnMerge, column - 1));
            startColumnMerge = column;
        }
        return row + 2;
    }

    ///
    /// Write summary
    ///
    private static int WriteSummary(SXSSFSheet summarySheet, AnalyseSettings settings) {
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

    private static int WriteChannelSettingToSummarySheet(SXSSFSheet summarySheet, int row, String chName,
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
