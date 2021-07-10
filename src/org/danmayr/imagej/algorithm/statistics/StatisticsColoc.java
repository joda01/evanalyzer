package org.danmayr.imagej.algorithm.statistics;

import java.util.Map;

import org.danmayr.imagej.algorithm.structs.*;

public class StatisticsColoc extends Statistics {
    public StatisticsColoc() {

    }

    public double[] getValues() {
        double[] values = { avgAreaSize, avgCircularity,valid,invalid };
        return values;
    }

    public String[] getTitle() {
        String[] title = { "area size", "circularity","valid","invalid" };
        return title;
    }
}
