package org.danmayr.imagej.excel;

import org.apache.commons.lang3.ArrayUtils;


public class InputFiles{
    public InputFiles(){}
    public void add(String csvFile, String sheetName){
        mCsvFileNames = ArrayUtils.add(mCsvFileNames,csvFile);
        mSheetNames = ArrayUtils.add(mSheetNames,sheetName);
    }
    String[] mCsvFileNames={};
    String[] mSheetNames={};
}