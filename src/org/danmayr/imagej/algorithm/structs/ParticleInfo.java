package org.danmayr.imagej.algorithm.structs;

///
/// \class  ParticleInfo
/// \brief  Region on interest
///
public class ParticleInfo {

    ///
    /// \brief Constructor
    ///
    public ParticleInfo(int roiName, double areaSize, double areaGrayScale, double circularity) {
        this.roiName = roiName;
        this.areaSize = areaSize;
        this.areaGrayScale = areaGrayScale;
        this.circularity = circularity;
    }

    ///
    /// \brief Returns the name of the roi
    ///
    public String toString() {
        return roiName + ";" + Double.toString(areaSize) + ";" 
                + Double.toString(areaGrayScale) + ";" + Double.toString(circularity);
    }

    public int getRoiNr(){
        return roiName;
    }

    public double[] getValues(){
        double[] values={areaSize,areaGrayScale,circularity};
        return values;
    }

    public String[] getTitle(){
        String[] title = {"area size","gray scale","circularity"};
        return title;
    }

    public int roiName;
    public double areaSize;
    public double areaGrayScale;
    public double circularity;
}
