
package org.danmayr.imagej.algorithm.filters;

import java.awt.Color;
import ij.plugin.frame.RoiManager;


public class RoiOverlaySettings
{
    public RoiOverlaySettings(RoiManager rm, Color cc, boolean n){
        this.m=rm;
        this.c=cc;
        this.nr=n;
    }
    public RoiManager m;
    public Color c;
    public boolean nr;
}