package org.danmayr.imagej.exports;

import ij.*;

import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;

import org.apache.commons.lang3.math.*;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFSheet;

import com.opencsv.CSVReader;
import java.util.*;

import org.danmayr.imagej.algorithm.structs.*;

public class ExcelExport {
    ExcelExport() {

    }

    public static void Export(String outputFolder, FolderResults results) {

        FileOutputStream fileOutputStream = null;

        Workbook workBook = new XSSFWorkbook();

        int rowNum = 0;

        outputFolder = outputFolder + "/result.xlsx";
        int sheetNr = 0;

        for (Map.Entry<String, Folder> entry : results.getFolders().entrySet()) {
            String key = entry.getKey();

            Folder folder = entry.getValue();
            for (Map.Entry<String, Image> entry1 : folder.getImages().entrySet()) {
                String imgName = entry1.getKey();
                Image image = entry1.getValue();

                XSSFSheet imageSheet = (XSSFSheet) workBook.createSheet(Integer.toString(sheetNr));
                sheetNr++;
                int column = 1;
                int columnAdd = 0;

                for (Map.Entry<Integer, Channel> entry2 : image.getChannels().entrySet()) {
                    int chName = entry2.getKey();
                    Channel channel = entry2.getValue();
                    
                    
                    int row = 2;
                    // Write channel name
                    Row chanelNameRow = imageSheet.getRow(0);
                    if (null == chanelNameRow) {
                        chanelNameRow = imageSheet.createRow(0);
                    }
                    chanelNameRow.createCell(column).setCellValue(channel.toString());
                    



                    for (Map.Entry<Integer, ParticleInfo> entry3 : channel.getRois().entrySet()) {
                        int chNr = entry3.getKey();
                        ParticleInfo info = entry3.getValue();

                        
                        // Write title
                        String titles[] = info.getTitle();
                        Row currentRow = imageSheet.getRow(1);
                        if (null == currentRow) {
                            currentRow = imageSheet.createRow(1);
                        }
                        for (int c = 0; c < titles.length; c++) {
                            currentRow.createCell(column+c).setCellValue(titles[c]);
                        }


                        // Write title
                        
                        // Write ROI number
                        currentRow = imageSheet.getRow(row);
                        if (null == currentRow) {
                            currentRow = imageSheet.createRow(row);
                        }
                        currentRow.createCell(0).setCellValue(info.getRoiNr());
                        double values[] = info.getValues();
                        for (int c = 0; c < values.length; c++) {
                            currentRow = imageSheet.getRow(row);
                            if (null == currentRow) {
                                currentRow = imageSheet.createRow(row);
                            }
                            currentRow.createCell(column+c).setCellValue(values[c]);
                        }
                        columnAdd = values.length;
                        row++;
                    }

                    column += columnAdd;
                }
            }
        }

        try {
            fileOutputStream = new FileOutputStream(outputFolder.trim());
            workBook.write(fileOutputStream);
        } catch (Exception exObj) {
        }

    }
}
