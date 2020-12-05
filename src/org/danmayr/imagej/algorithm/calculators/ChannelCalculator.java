package org.danmayr.imagej.algorithm.calculators;

import org.danmayr.imagej.algorithm.*;


public interface ChannelCalculator extends Cloneable
{
    public String getName();
    public String toString();
    public String header();
    public void calcStatistics(Channel[] channels);

};