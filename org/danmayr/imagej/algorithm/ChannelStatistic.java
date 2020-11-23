package org.danmayr.imagej.algorithm;

///
/// Struct for statistic measurment
///
public class ChannelStatistic {
    public double numberOfParticles = 0;
    public double numberOfTooSmallParticles = 0;
    public double numberOfTooBigParticles = 0;
    public double numberOfParticlesInRange = 0;
    public double avgGrayScale = 0;
    public double avgAreaSize = 0;
    public double counter = 0;
}
