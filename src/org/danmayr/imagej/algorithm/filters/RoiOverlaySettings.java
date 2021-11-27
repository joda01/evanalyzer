
package org.danmayr.imagej.algorithm.filters;

import java.awt.Color;

import org.danmayr.imagej.algorithm.structs.*;

import ij.plugin.frame.RoiManager;


public class RoiOverlaySettings
{
    public RoiOverlaySettings(RoiManager rm, Color cc, boolean n){
        this.m=rm;
        this.c=cc;
        this.nr=n;
    }

    public RoiManager m=null;
    public Color c;
    public boolean nr;
}