package org.danmayr.imagej.exports;

import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import org.apache.poi.common.usermodel.HyperlinkType;
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
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.IndexedColorMap;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
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
import org.danmayr.imagej.Version;

public class ExcelExport {
    ExcelExport() {

    }

    public static String Export(String outputFolder, String reportFileName, FolderResults results,
            AnalyseSettings.ReportType reportType, AnalyseSettings settings, Dialog mDialog) {

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
        WriteSummary(summerySheet, settings);

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
                String imageName = imageMap.getValue().getImageName();
                Image image = imageMap.getValue();
                String sheetName = Integer.toString(imgSheetCount);

                //
                // Write image sumary to overview sheet
                //
                overViewRow = WriteOverviewImageSummery(overviewSheet, sheetName, createHelper, image, folderName,
                        imageName, overViewRow);

                mDialog.incrementProgressBarValue("generating report ...");

            }

            //
            // Write the folder statistic to the end of the folder section
            //
            overViewRow = WriteOverviewFolderStatistics(overviewSheet, folder, overViewRow);
        }

        String out = outputFolder + File.separator + reportFileName + ".xlsx";
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
    private static int WriteSummary(SXSSFSheet summarySheet, AnalyseSettings settings) {
        int row = 0;
        summarySheet.setDefaultColumnWidth(25);
        row = WriteRow(summarySheet, row, "Used program Version", Version.getVersion());

        row = WriteRow(summarySheet, row, "Save Cotrol Pictures", String.valueOf(settings.mSaveDebugImages));
        row = WriteRow(summarySheet, row, "Report Type", String.valueOf(settings.reportType));

        row = WriteRow(summarySheet, row, "Used Function", String.valueOf(settings.mSelectedFunction));

        row = WriteRow(summarySheet, row, "Input folder", String.valueOf(settings.mInputFolder));
        row = WriteRow(summarySheet, row, "Output folder", String.valueOf(settings.mOutputFolder));
        row = WriteRow(summarySheet, row, "Selected Series", String.valueOf(settings.mSelectedSeries));

        row = WriteRow(summarySheet, row, "Count EVs per Cells", String.valueOf(settings.countEvsPerCell()));

        row = WriteRow(summarySheet, row, "Report name", String.valueOf(settings.mReportName));

        for (int n = 0; n < settings.channelSettings.size(); n++) {
            row = WriteChannelSettingToSummarySheet(summarySheet, row,
                    "C=" + settings.channelSettings.get(n).mChannelNr, settings.channelSettings.get(n));
        }
        return row;
    }

    private static int WriteChannelSettingToSummarySheet(SXSSFSheet summarySheet, int row, String chName,
            ChannelSettings ch) {
        row = WriteRow(summarySheet, row, chName + " type", String.valueOf(ch.type));
        row = WriteRow(summarySheet, row, chName + " thresholding", String.valueOf(ch.mThersholdMethod));
        row = WriteRow(summarySheet, row, chName + " enhance contrast", String.valueOf(ch.enhanceContrast));
        row = WriteRow(summarySheet, row, chName + " min Threshold", String.valueOf(ch.minThershold));
        row = WriteRow(summarySheet, row, chName + " max Threshold", String.valueOf(ch.maxThershold));
        row = WriteRow(summarySheet, row, chName + " min Circularity", String.valueOf(ch.getMinCircularityDouble()));
        row = WriteRow(summarySheet, row, chName + " Min particle Size", String.valueOf(ch.getMinParticleSizeDouble()));
        row = WriteRow(summarySheet, row, chName + " Max particle Size", String.valueOf(ch.getMaxParticleSizeDouble()));
        row = WriteRow(summarySheet, row, chName + " Margin crop", String.valueOf(ch.getMarginCropDouble()));
        row = WriteRow(summarySheet, row, chName + " Z-Projection ", String.valueOf(ch.ZProjector));
        row = WriteRow(summarySheet, row, chName + " Preprocessing ", String.valueOf(ch.preProcessing));

        return row;
    }

    private static int WriteRow(SXSSFSheet summarySheet, int row, String title, String value) {
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
        ///
        /// Cell Styles
        ///

        Row rowChName = overviewSheet.createRow(row);
        Row rowTitles = overviewSheet.createRow(row + 1);

        TreeMap<ChannelType, Pair<String, String[]>> titles = folder.getStatisticTitle();
        int column = OVERVIEW_SHEET_START_STATISTIC_COLUMN;
        int startColumnMerge = OVERVIEW_SHEET_START_STATISTIC_COLUMN;
        CellStyle actStyleThinLine = createCellStyleHeader(overviewSheet, false);
        CellStyle actStyleThickLine = createCellStyleHeader(overviewSheet, true);
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
            actStyleThinLine = createCellStyleNumberEven(overviewSheet, false);
        } else {
            actStyleThinLine = createCellStyleNumberOdd(overviewSheet, false);
        }

        CellStyle actStyleThickLine;
        if (row % 2 == 0) {
            actStyleThickLine = createCellStyleNumberEven(overviewSheet, true);
        } else {
            actStyleThickLine = createCellStyleNumberOdd(overviewSheet, true);
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
        link2.setAddress(image.getUUID()+".xlsx");
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

            if(rlativePath.length() > 0){
                picCell.setCellValue("Open pic");
            }else{
                picCell.setCellValue("-");
            }
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

    private static CellStyle createCellStyleNumberEven(SXSSFSheet sheet, boolean thickLine) {
        Font defaultFont = sheet.getWorkbook().createFont();
        defaultFont.setFontHeightInPoints((short) 10);
        defaultFont.setFontName("Arial");
        defaultFont.setColor(IndexedColors.BLACK.getIndex());
        defaultFont.setBold(false);
        defaultFont.setItalic(false);

        CellStyle cellStyle = sheet.getWorkbook().createCellStyle();
        cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        cellStyle.setFillForegroundColor(IndexedColors.WHITE.index);
        cellStyle.setBorderBottom(BorderStyle.THIN);
        cellStyle.setBorderTop(BorderStyle.THIN);
        if (true == thickLine) {
            cellStyle.setBorderLeft(BorderStyle.THICK);
        } else {
            cellStyle.setBorderLeft(BorderStyle.THIN);
        }
        cellStyle.setBorderRight(BorderStyle.THIN);
        cellStyle.setFont(defaultFont);
        return cellStyle;
    }

    private static CellStyle createCellStyleNumberOdd(SXSSFSheet sheet, boolean thickLine) {
        Font defaultFont = sheet.getWorkbook().createFont();
        defaultFont.setFontHeightInPoints((short) 10);
        defaultFont.setFontName("Arial");
        defaultFont.setColor(IndexedColors.BLACK.getIndex());
        defaultFont.setBold(false);
        defaultFont.setItalic(false);

        XSSFCellStyle cellStyle = (XSSFCellStyle) sheet.getWorkbook().createCellStyle();
        cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        XSSFColor col = new XSSFColor(new java.awt.Color(224, 224, 224));
        cellStyle.setFillForegroundColor(col);
        cellStyle.setBorderBottom(BorderStyle.THIN);
        cellStyle.setBorderTop(BorderStyle.THIN);
        if (true == thickLine) {
            cellStyle.setBorderLeft(BorderStyle.THICK);
        } else {
            cellStyle.setBorderLeft(BorderStyle.THIN);
        }
        cellStyle.setBorderRight(BorderStyle.THIN);
        cellStyle.setFont(defaultFont);
        return cellStyle;
    }

    private static CellStyle createCellStyleHeader(SXSSFSheet sheet, boolean thickLine) {
        Font defaultFont = sheet.getWorkbook().createFont();
        defaultFont.setFontHeightInPoints((short) 10);
        defaultFont.setFontName("Arial");
        defaultFont.setColor(IndexedColors.BLACK.getIndex());
        defaultFont.setBold(true);
        defaultFont.setItalic(false);
        defaultFont.setColor(IndexedColors.WHITE.index);

        XSSFCellStyle cellStyle = (XSSFCellStyle) sheet.getWorkbook().createCellStyle();
        cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        cellStyle.setFillForegroundColor(IndexedColors.GREY_80_PERCENT.index);
        cellStyle.setBorderBottom(BorderStyle.THIN);
        cellStyle.setBorderTop(BorderStyle.THIN);
        if (true == thickLine) {
            cellStyle.setBorderLeft(BorderStyle.THICK);
        } else {
            cellStyle.setBorderLeft(BorderStyle.THIN);
        }
        cellStyle.setBorderRight(BorderStyle.THIN);
        cellStyle.setFont(defaultFont);
        return cellStyle;
    }

    private static CellStyle createCellStyleFooter(SXSSFSheet sheet, boolean thickLine) {
        Font defaultFont = sheet.getWorkbook().createFont();
        defaultFont.setFontHeightInPoints((short) 10);
        defaultFont.setFontName("Arial");
        defaultFont.setColor(IndexedColors.BLACK.getIndex());
        defaultFont.setBold(true);
        defaultFont.setItalic(false);
        defaultFont.setColor(IndexedColors.BLACK.index);

        XSSFCellStyle cellStyle = (XSSFCellStyle) sheet.getWorkbook().createCellStyle();
        cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        cellStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.index);
        cellStyle.setBorderBottom(BorderStyle.THIN);
        cellStyle.setBorderTop(BorderStyle.THIN);
        if (true == thickLine) {
            cellStyle.setBorderLeft(BorderStyle.THICK);
        } else {
            cellStyle.setBorderLeft(BorderStyle.THIN);
        }
        cellStyle.setBorderRight(BorderStyle.THIN);
        cellStyle.setFont(defaultFont);
        return cellStyle;
    }

    ///
    /// Write image statistics
    ///
    public static void WriteImageSheet(String outputFolder,Image image) {

        //
        // Create workbook
        //
        String fileName = image.getUUID();
        Workbook workBook = new SXSSFWorkbook(2000);
        SXSSFSheet imgSheet = (SXSSFSheet) workBook.createSheet(fileName);


        CellStyle actStyleThinLine = createCellStyleHeader(imgSheet, false);
        CellStyle actStyleThickLine = createCellStyleHeader(imgSheet, true);



        ///
        /// Write header
        ///
        Row rowChName = imgSheet.createRow(0);
        Row rowTitles = imgSheet.createRow(0 + 1);

        //
        // Write back button
        //
        Cell rowBackButton = rowChName.createCell(0);
        // rowBackButton.

        TreeMap<ChannelType, Pair<String, String[]>> titles = image.getTitle();
        TreeMap<ChannelType, Integer> channelColumnSize = new TreeMap<>(); // Stores the number of Columns per channel

        int column = 1;
        int startColumnMerge = column;
        for (Map.Entry<ChannelType, Pair<String, String[]>> value : titles.entrySet()) {
            // Channel name
            Cell rowNameFirst = rowChName.createCell(column);
            rowNameFirst.setCellValue(value.getValue().getFirst());
            rowNameFirst.setCellStyle(actStyleThickLine);
            channelColumnSize.put(value.getKey(), column);
            for (int n = 0; n < value.getValue().getSecond().length; n++) {
                Cell rowTitleCell = rowTitles.createCell(column);
                rowTitleCell.setCellValue(value.getValue().getSecond()[n]);
                rowTitleCell.setCellStyle(actStyleThinLine);
                column++;
            }
            imgSheet.addMergedRegion(new CellRangeAddress(0, 0, startColumnMerge, column - 1));
            startColumnMerge = column;
        }

        ///
        /// Prepare values
        ///
        TreeMap<Integer, TreeMap<ChannelType, double[]>> rows = new TreeMap<>(); // <ROI <ChannelNr,values>>

        for (Map.Entry<ChannelType, Channel> channelMap : image.getChannels().entrySet()) {
            Channel channel = channelMap.getValue();
            for (Map.Entry<Integer, ParticleInfo> roiMap : channel.getRois().entrySet()) {
                ParticleInfo info = roiMap.getValue();
                double values[] = info.getValues();

                TreeMap<ChannelType, double[]> vec = rows.get(roiMap.getKey());
                if (null == vec) {
                    vec = new TreeMap<>();
                    rows.put(roiMap.getKey(), vec);
                }
                vec.put(channelMap.getKey(), values);
            }
        }

        ///
        /// Write values
        ///
        int row = 2;

        ///
        /// Cell Styles
        ///
        CellStyle actStyleThinLineEven = createCellStyleNumberEven(imgSheet, false);
        CellStyle actStyleThinLineOdd = createCellStyleNumberOdd(imgSheet, false);
        CellStyle actStyleThickLineEven = createCellStyleNumberEven(imgSheet, true);
        CellStyle actStyleThickLineOdd = createCellStyleNumberOdd(imgSheet, true);

        for (Map.Entry<Integer, TreeMap<ChannelType, double[]>> entry3 : rows.entrySet()) {
            Row currentRow = imgSheet.createRow(row);
            Cell number = currentRow.createCell(0);
            number.setCellValue(entry3.getKey());
            if (row % 2 == 0) {
                number.setCellStyle(actStyleThinLineEven);
            } else {
                number.setCellStyle(actStyleThinLineOdd);
            }

            TreeMap<ChannelType, double[]> valVec = entry3.getValue();

            for (Map.Entry<ChannelType, double[]> valVecEntry : valVec.entrySet()) {
                Object key = valVecEntry.getKey();
                if (null != key && null != channelColumnSize && true == channelColumnSize.containsKey(key)) { // channelColumnSize.containsKey(key)
                                                                                                              // was
                                                                                                              // false
                    int columnCnt = channelColumnSize.get(key);
                    double[] valVecVal = valVecEntry.getValue();

                    if (null != valVecVal) {
                        // First Column
                        boolean firstCol = true;
                        for (int c = 0; c < valVecVal.length; c++) {
                            Cell valueCell = currentRow.createCell(columnCnt);
                            valueCell.setCellValue(valVecVal[c]);
                            if (row % 2 == 0) {
                                if (false == firstCol) {
                                    valueCell.setCellStyle(actStyleThinLineEven);
                                } else {
                                    valueCell.setCellStyle(actStyleThickLineEven);
                                }
                            } else {
                                if (false == firstCol) {
                                    valueCell.setCellStyle(actStyleThinLineOdd);
                                } else {
                                    valueCell.setCellStyle(actStyleThickLineOdd);
                                }
                            }
                            firstCol = false;
                            columnCnt++;
                        }
                    }
                }
            }
            row++;
        }

        String out = outputFolder + File.separator + fileName + ".xlsx";
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(out.trim());
            workBook.write(fileOutputStream);
            workBook.close();
        } catch (Exception exObj) {
        }
    }

    ///
    /// Write folder statistics
    ///
    private static int WriteOverviewFolderStatistics(SXSSFSheet overviewSheet, Folder folder, int row) {
        Row rowFolderStatistics = overviewSheet.createRow(row);

        TreeMap<ChannelType, double[]> statistics = folder.calcStatistic();
        int column = OVERVIEW_SHEET_START_STATISTIC_COLUMN;
        CellStyle actStyleThinLine = createCellStyleFooter(overviewSheet, false);
        CellStyle actStyleThickLine = createCellStyleFooter(overviewSheet, true);

        for (Map.Entry<ChannelType, double[]> value : statistics.entrySet()) {
            Cell emptyCell = rowFolderStatistics.createCell(column);
            emptyCell.setCellValue("Avg:");
            emptyCell.setCellStyle(actStyleThickLine);
            // Channel Name
            column++; // Additioal column at beginning. This is the line with the link to the picture
            // Channel Name
            for (int n = 0; n < value.getValue().length; n++) {
                Cell cellStatistics = rowFolderStatistics.createCell(column);
                cellStatistics.setCellValue(value.getValue()[n]);
                cellStatistics.setCellStyle(actStyleThinLine);
                cellStatistics.getCellStyle().setDataFormat(
                        overviewSheet.getWorkbook().getCreationHelper().createDataFormat().getFormat("0.00")); // or
                                                                                                               // #####.##
                                                                                                               // or
                                                                                                               // number
                column++;
            }
        }
        return row + 2;
    }

}
