package org.danmayr.imagej.algorithm.statistics;

import java.util.Map;

import org.danmayr.imagej.algorithm.structs.*;

public class Statistics {
    public Statistics() {

    }

    public void calcStatistics(Channel ch) {
        int nrOfInvalid = 0;
        int nrOfValid = 0;
        double areaSizeSum = 0;
        double grayScaleSum = 0;
        double circularitySum = 0;

        for (Map.Entry<Integer, ParticleInfo> entry : ch.getRois().entrySet()) {
            int chNr = entry.getKey();
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
        avgAreaSize = areaSizeSum / nrOfValid;
        avgGrayScale = grayScaleSum / nrOfValid;
        avgCircularity = circularitySum / nrOfValid;
        this.invalid = nrOfInvalid;
        this.valid = nrOfValid;
    }

    public double[] getValues() {
        double[] values = { avgAreaSize, avgGrayScale, avgCircularity,valid,invalid };
        return values;
    }

    public String[] getTitle() {
        String[] title = { "area size", "gray scale", "circularity","valid","invalid" };
        return title;
    }

    public int valid;
    public int invalid;
    public double avgAreaSize;
    public double avgGrayScale;
    public double avgCircularity;
}
