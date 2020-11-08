package org.danmayr.imagej.excel;

import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;

import org.apache.commons.lang3.math.*;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import com.opencsv.CSVReader;

public class CsvToExcel {

    public static final char FILE_DELIMITER = ';';
    public static final String FILE_EXTN = ".xlsx";

    public static String convertCsvToXls(String xlsFileLocation, String[] csvFilePath) {
        String generatedXlsFilePath = "";
        FileOutputStream fileOutputStream = null;
        Workbook workBook = null;
        CSVReader reader = null;
        try {

            workBook = new SXSSFWorkbook();
            /**** Get the CSVReader Instance & Specify The Delimiter To Be Used ****/
            for (int n = 0; n < csvFilePath.length; n++) {
                String[] nextLine;
                reader = new CSVReader(new FileReader(csvFilePath[n]), FILE_DELIMITER);
                SXSSFSheet sheet = (SXSSFSheet) workBook.createSheet("Sheet " + Integer.toString(n));

                int rowNum = 0;
                // logger.info("Creating New .Xls File From The Already Generated .Csv File");
                while ((nextLine = reader.readNext()) != null) {
                    Row currentRow = sheet.createRow(rowNum++);
                    for (int i = 0; i < nextLine.length; i++) {
                        if (NumberUtils.isDigits(nextLine[i])) {
                            currentRow.createCell(i).setCellValue(Integer.parseInt(nextLine[i]));
                        } else if (NumberUtils.isNumber(nextLine[i])) {
                            currentRow.createCell(i).setCellValue(Double.parseDouble(nextLine[i]));
                        } else {
                            currentRow.createCell(i).setCellValue(nextLine[i]);
                        }
                    }
                }
            }

            generatedXlsFilePath = xlsFileLocation + FILE_EXTN;
            // logger.info("The File Is Generated At The Following Location?= " +
            // generatedXlsFilePath);

            fileOutputStream = new FileOutputStream(generatedXlsFilePath.trim());
            workBook.write(fileOutputStream);
        } catch (Exception exObj) {
            // logger.error("Exception In convertCsvToXls() Method?= " + exObj);
        } finally {
            try {

                /**** Closing The Excel Workbook Object ****/
                workBook.close();

                /**** Closing The File-Writer Object ****/
                fileOutputStream.close();

                /**** Closing The CSV File-ReaderObject ****/
                reader.close();
            } catch (IOException ioExObj) {
                // logger.error("Exception While Closing I/O Objects In convertCsvToXls()
                // Method?= " + ioExObj);
            } catch (NullPointerException nl) {

            }
        }

        return generatedXlsFilePath;
    }
}
