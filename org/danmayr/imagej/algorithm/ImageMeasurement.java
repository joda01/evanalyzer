package org.danmayr.imagej.algorithm;

public class ImageMeasurement {
    public ImageMeasurement(String roi, double areaSize, double areaBinGrayScale, double areaGrayScale) {
        this.roi = roi;
        this.areaSize = areaSize;
        this.areaBinGrayScale = areaBinGrayScale;
        this.areaGrayScale = areaGrayScale;
    }

    public String roi;
    public double areaSize;
    public double areaBinGrayScale;
    public double areaGrayScale;

    // String mResultHeader = "file;directory;small;big;count;grayscale;areasize\n";

}