package org.danmayr.imagej.algorithm.structs;


import java.awt.Rectangle;

import org.danmayr.imagej.algorithm.filters.Filter;

import ij.gui.OvalRoi;
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
    Roi mSnapArea = null;

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

    public ParticleInfo(int roiName, double areaSize, double areaGrayScale, double areaThersholdScale, double circularity, Roi roi, double snapArea) {
        this(roiName,areaSize,areaGrayScale,areaThersholdScale,circularity);
        this.mRoi = roi;
        if(snapArea > 0){
            mSnapArea = generateSnapArea(roi, snapArea);
        }else{
            mSnapArea = roi;
        }
    }


    public Roi generateSnapArea(Roi inRoi, double snapArea){
        Rectangle bounds = inRoi.getBounds();

        if(snapArea > bounds.width && snapArea > bounds.height){
            double centerX = bounds.x+bounds.width/2;
            double centerY = bounds.y+bounds.height/2;

            double pointX = centerX-snapArea/2;
            double pointY = centerY-snapArea/2;

            double width = snapArea;

            OvalRoi ovalroi = new OvalRoi((int)pointX,(int)pointY,(int)width,(int)width);
            return ovalroi;
        }else{
            return inRoi;
        }
    }

    ///
    /// Calculate the intersection of two ROIs.
    /// Returns NULL if they do not intersect
    ///
    public Roi isPartOf(ParticleInfo roiIn)
    {
        if(null != getSnapArea() && null != roiIn.getSnapArea()){
            // Only calculate the intersection of the ROIs if the bouding boxes intersects
            // this makes it faster
            if(true == getSnapArea().getBounds().intersects(roiIn.getSnapArea().getBounds())){
                return Filter.and(getSnapArea(), roiIn.getSnapArea());
            }else{
                return null;
            }
        }else{
            return null;
        }
    }

    public void clearRoi()
    {
        mRoi = null;
        mSnapArea = null;
    }

    public Roi getRoi(){
        return mRoi;
    }

    public Roi getSnapArea(){
        return mSnapArea;
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
