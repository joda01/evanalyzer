package org.danmayr.imagej.algorithm.filters;

import java.awt.Color;
import java.util.TreeMap;

import org.danmayr.imagej.algorithm.structs.*;

import ij.plugin.frame.RoiManager;

public class ChannelInfoOverlaySettings {
    public ChannelInfoOverlaySettings(TreeMap<Integer, ParticleInfo> rm, Color cc, boolean n, boolean fill){
        this.m=rm;
        this.c=cc;
        this.nr=n;
        this.fill = fill;
    }

    public TreeMap<Integer, ParticleInfo> m=null;
    public Color c;
    public boolean nr;
    public boolean fill;
}
