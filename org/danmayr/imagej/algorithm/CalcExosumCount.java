package org.danmayr.imagej.algorithm;

import java.io.File;

public class CalcExosumCount extends BasicAlgorithm {
    
    
    @Override
    protected void analyseImage(File imageFile) {
        IJ.run("Bio-Formats Importer", "open=[" + imageFile.getAbsoluteFile().toString()
                + "] autoscale color_mode=Grayscale rois_import=[ROI manager] specify_range split_channels view=Hyperstack stack_order=XYCZT series_1 c_begin_1=1 c_end_1=2 c_step_1=1");

        // Remove scale
        IJ.run("Set Scale...", "distance=0 known=0 unit=pixel");

        // List all images
        String[] imageTitles = WindowManager.getImageTitles();
    }
}
