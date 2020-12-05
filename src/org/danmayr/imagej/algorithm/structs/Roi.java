package org.danmayr.imagej.algorithm.struct;


///
/// \class  Roi
/// \brief  Region on interest
///
public class Roi {
    
    ///
    /// \brief Constructor
    ///
    public Roi(String roiName, double areaSize, double areaGrayScale, double circularity) {
        this.roiName = roiName;
        this.areaSize = areaSize;
        this.areaGrayScale = areaGrayScale;
        this.circularity = circularity;
    }

    ///
    /// \brief Returns the name of the roi
    ///
    public String toString(){
        return roiName;
    }

    public String roiName;
    public double areaSize;
    public double areaGrayScale;
    public double circularity;
}
