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

    public ParticleInfoColoc(int roiName, double areaSize, double circularity, double[] intensityChannels,
            double[] areaSizeChannels, Roi roi) {
        super(roiName, areaSize, 255, 255, circularity, roi);
        this.intensityChannels = intensityChannels;
        this.areaSizeChannels = areaSizeChannels;
    }

    @Override
    public double[] getValues() {
        double[] values = new double[3 + intensityChannels.length + areaSizeChannels.length];
        values[0] = areaSize;
        values[1] = circularity;
        values[2] = status;

        int idx = 3;
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
