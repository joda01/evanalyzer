package org.danmayr.imagej.algorithm.structs;

import org.danmayr.imagej.algorithm.filters.Filter;

import ij.gui.Roi;

///
/// \class  ParticleInfo
/// \brief  Region on interest
///
public class ParticleInfoColoc extends ParticleInfo {

    public double[] intensityChannels;
    public double[] areaSizeChannels;
    public double colocFactor;

    public ParticleInfoColoc(int roiName, double areaSize, double colocFactor, double circularity, double[] intensityChannels,
            double[] areaSizeChannels, Roi roi, int snapArea) {
        super(roiName, areaSize, 255, 255, circularity, roi, snapArea);
        this.intensityChannels = intensityChannels;
        this.areaSizeChannels = areaSizeChannels;
        this.colocFactor=colocFactor;
    }

    @Override
    public double[] getValues() {
        double[] values = new double[4 + intensityChannels.length + areaSizeChannels.length];
        values[0] = areaSize;
        values[1] = colocFactor;
        values[2] = circularity;
        values[3] = status;
        

        int idx = 4;
        for (int n = 0; n < this.intensityChannels.length; n++) {
            values[idx] = intensityChannels[n];
            idx++;
        }

        for (int n = 0; n < this.areaSizeChannels.length; n++) {
            values[idx] = areaSizeChannels[n];
            idx++;
        }

        return values;
    }
}
