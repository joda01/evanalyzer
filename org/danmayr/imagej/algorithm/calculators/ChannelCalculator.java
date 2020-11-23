package org.danmayr.imagej.algorithm.calculators;

import org.danmayr.imagej.algorithm.*;


public interface ChannelCalculator {
    public String toString();
    public void calcStatistics(Channel[] channels);
};