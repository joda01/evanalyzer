package org.danmayr.imagej.algorithm.statistics;

import java.util.Map;

import org.danmayr.imagej.algorithm.structs.*;

public class Statistics {
    public Statistics() {

    }

    public void setThershold(double minTH, double maxTH){
        this.minTH = minTH;
        this.maxTH = maxTH;
    }

    public void setNrOfRemovedParticles(int nrOfRemovedParticles)
    {
        this.nrOfRemovedParticles = nrOfRemovedParticles;
    }

    public void calcStatistics(Channel ch) {
        int nrOfInvalid = 0;
        int nrOfValid = 0;
        double areaSizeSum = 0;
        double grayScaleSum = 0;
        double circularitySum = 0;

        for (Map.Entry<Integer, ParticleInfo> entry : ch.getRois().entrySet()) {
            ParticleInfo info = entry.getValue();

            if (false == info.isValid()) {
                nrOfInvalid++;
            } else {
                nrOfValid++;
                areaSizeSum += info.areaSize;
                grayScaleSum += info.areaGrayScale;
                circularitySum += info.circularity;
            }
        }
        if(nrOfValid > 0){
            avgAreaSize = areaSizeSum / nrOfValid;
            avgGrayScale = grayScaleSum / nrOfValid;
            avgCircularity = circularitySum / nrOfValid;

        }else{
            avgAreaSize = 0;
            avgGrayScale = 0;
            avgCircularity = 0;
        }
        this.invalid = nrOfInvalid;
        this.valid = nrOfValid;
    }

    public double[] getValues() {
        double[] values = { avgAreaSize, avgGrayScale, avgCircularity,valid,invalid, this.nrOfRemovedParticles,minTH };
        return values;
    }

    public String[] getTitle() {
        String[] title = { "area size", "intensity", "circularity","valid","invalid","tetraspeck","threshold" };
        return title;
    }

    public int valid;
    public int invalid;
    public int nrOfRemovedParticles = 0;
    public double avgAreaSize;
    public double avgGrayScale;
    public double avgCircularity;
    public double minTH = -1;
    public double maxTH = -1;
}
