package org.danmayr.imagej.algorithm.structs;

import org.danmayr.imagej.algorithm.filters.Filter;

import ij.gui.Roi;

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
    Roi mRoi = null;

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

    public ParticleInfo(int roiName, double areaSize, double areaGrayScale, double areaThersholdScale, double circularity, Roi roi) {
        this(roiName,areaSize,areaGrayScale,areaThersholdScale,circularity);
        this.mRoi = roi;
    }

    ///
    /// Calculate the intersection of two ROIs.
    /// Returns NULL if they do not intersect
    ///
    public Roi isPartOf(ParticleInfo roiIn)
    {
        if(null != mRoi && null != roiIn.getRoi()){
            // Only calculate the intersection of the ROIs if the bouding boxes intersects
            // this makes it faster
            if(true == mRoi.getBounds().intersects(roiIn.getRoi().getBounds())){
                return Filter.and(mRoi, roiIn.getRoi());
            }else{
                return null;
            }
        }else{
            return null;
        }
    }


    public Roi getRoi(){
        return mRoi;
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

    public int getRoiNr() {
        return roiName;
    }

    public double[] getValues() {
        double[] values = { areaSize,areaGrayScale, areaThersholdScale, circularity, status };
        return values;
    }

    public int status = VALID;
    public int roiName;
    public double areaSize;
    public double areaThersholdScale;
    public double areaGrayScale=0;
    public double circularity;
}
