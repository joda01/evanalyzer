package org.danmayr.imagej.exports;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Map;
import java.util.TreeMap;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.danmayr.imagej.algorithm.pipelines.Pipeline.ChannelType;
import org.danmayr.imagej.algorithm.structs.Channel;
import org.danmayr.imagej.algorithm.structs.Folder;
import org.danmayr.imagej.algorithm.structs.Image;
import org.danmayr.imagej.algorithm.structs.Pair;
import org.danmayr.imagej.algorithm.structs.ParticleInfo;

public class DetailReportExcel {
    DetailReportExcel() {

    }



    static int WriteRow(XSSFSheet summarySheet, int row, String title, String value) {
        Row rowChName = summarySheet.createRow(row);
        rowChName.createCell(0).setCellValue(title);
        rowChName.createCell(1).setCellValue(value);
        row++;
        return row;
    }




    static CellStyle createCellStyleNumberEven(XSSFSheet sheet, boolean thickLine) {
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

    static CellStyle createCellStyleNumberOdd(XSSFSheet sheet, boolean thickLine) {
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

    static CellStyle createCellStyleHeader(XSSFSheet sheet, boolean thickLine) {
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

    private static CellStyle createCellStyleFooter(XSSFSheet sheet, boolean thickLine) {
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
        XSSFWorkbook workBook = new XSSFWorkbook();
        XSSFSheet imgSheet = (XSSFSheet) workBook.createSheet(fileName);


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
            fileOutputStream.close();
        } catch (Exception exObj) {
        }
    }

    ///
    /// Write folder statistics
    ///
    static int OVERVIEW_SHEET_START_STATISTIC_COLUMN = 4;
    private static int WriteOverviewFolderStatistics(XSSFSheet overviewSheet, Folder folder, int row) {
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
