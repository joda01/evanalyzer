package org.danmayr.imagej.algorithm.structs;


///
/// \class  ParticleInfo
/// \brief  Region on interest
///
public class ParticleInfo {
    static int VALID = 0x00;
    static int TOO_BIG = 0x02;
    static int TOO_SMALL = 0x04;
    static int WRONG_CIRCULARITY = 0x08;
    static int WRONG_INTENSITY = 0x10;

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
    /// \breif check if this particle matches the filter criteria
    ///
    public void validatearticle(double minAreaSize, double maxAreaSize, double minCircularity, double minGrayScale) {
        status = VALID;
        
        if(areaSize < minAreaSize){
            status |= TOO_SMALL;
        }

        if(areaSize > maxAreaSize){
            status |= TOO_BIG;
        }

        if(circularity < minCircularity){
            status |= WRONG_CIRCULARITY;
        }

        if(areaGrayScale<minGrayScale){
            status |= WRONG_INTENSITY;
        }
    }

    ///
    /// \breif check if this particle is valid
    ///
    public boolean isValid(){
        if(status == VALID){
            return true;
        }else{
            return false;
        }
    }

    ///
    /// \brief Returns the name of the roi
    ///
    public String toString() {
        return roiName + ";" + Double.toString(areaSize) + ";" + Double.toString(areaGrayScale) + ";"
                + Double.toString(circularity);
    }

    public int getRoiNr() {
        return roiName;
    }

    public double[] getValues() {
        double[] values = { areaSize, areaGrayScale, circularity };
        return values;
    }

    public String[] getTitle() {
        String[] title = { "area size", "gray scale", "circularity" };
        return title;
    }

    public int status = VALID;
    public int roiName;
    public double areaSize;
    public double areaGrayScale;
    public double circularity;
}
