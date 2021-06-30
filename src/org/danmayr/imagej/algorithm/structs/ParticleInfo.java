package org.danmayr.imagej.algorithm.structs;


///
/// \class  ParticleInfo
/// \brief  Region on interest
///
public class ParticleInfo {
    static protected int VALID = 0x00;
    static protected int TOO_BIG = 0x02;
    static protected int TOO_SMALL = 0x04;
    static protected int WRONG_CIRCULARITY = 0x08;
    static protected int WRONG_INTENSITY = 0x10;

    ///
    /// \brief Constructor
    ///
    public ParticleInfo(int roiName, double areaSize, double areaGrayScale, double areaThersholdScale, double circularity) {
        this.roiName = roiName;
        this.areaSize = areaSize;
        this.areaThersholdScale = areaThersholdScale;
        this.circularity = circularity;
        this.areaGrayScale = areaGrayScale;
    }

    ///
    /// \breif check if this particle matches the filter criteria
    ///
    public boolean validatearticle(double minAreaSize, double maxAreaSize, double minCircularity, double minGrayScale) {
        boolean valid = true;
        status = VALID;
        
        if(areaSize < minAreaSize){
            status |= TOO_SMALL;
            valid = false;
        }

        if(areaSize > maxAreaSize){
            status |= TOO_BIG;
            valid = false;
        }

        if(circularity < minCircularity){
            status |= WRONG_CIRCULARITY;
            valid = false;
        }

        if(areaGrayScale<minGrayScale){
            status |= WRONG_INTENSITY;
            valid = false;
        }

        return valid;
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
        return roiName + ";" + Double.toString(areaSize) +";"+ Double.toString(areaGrayScale) + ";" + Double.toString(areaThersholdScale) + ";"
                + Double.toString(circularity);
    }

    public int getRoiNr() {
        return roiName;
    }

    public double[] getValues() {
        double[] values = { areaSize,areaGrayScale, areaThersholdScale, circularity };
        return values;
    }

    public String[] getTitle() {
        String[] title = { "area size", "intensity","thershold scale", "circularity" };
        return title;
    }

    public int status = VALID;
    public int roiName;
    public double areaSize;
    public double areaThersholdScale;
    public double areaGrayScale=0;
    public double circularity;
}
