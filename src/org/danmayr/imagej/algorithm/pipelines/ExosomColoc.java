package org.danmayr.imagej.algorithm.pipelines;

import java.nio.file.DirectoryStream.Filter;
import java.util.*;
import org.danmayr.structs.*;
import org.danmayr.filters.Filter;



public class ExosomColoc extends Pipeline {

    public ExosomColoc(ChannelType ch0, ChannelType ch1)
    {
        super(ch0,ch1);
    }


    @Override
    protected TreeMap<Integer, Channel> startPipeline(File img)
    {
        RoiManager rm = new RoiManager();

        
        ImagePlus img0 = Filter.SubtractBackground(getImageCh0());
        ImagePlus img1 = Filter.SubtractBackground(getImageCh1());

        img0 = Filter.ApplyGaus(img0);
        img1 = Filter.ApplyGaus(img1);

        img0 = Filter.ApplyThershold(img0);
        img1 = Filter.ApplyThershold(img0);

        ImagePlus sumImage = AddImages(img0,img1);

        AnalyzeParticles(sumImage);

        Channel measCh0 = MeasureAndSaveResult("ch0",img0,rm);
        Channel measCh1 = MeasureAndSaveResult("ch1",img1,rm);
    }


    private Channel calculateColoc(Channel ch0, Channel ch1)
    {
        TreeMap<String, Roi> roiCh0 = ch0.getRois();
        TreeMap<String, Roi> roiCh1 = ch1.getRois();
    }

    class ColocRoi extends Roi{

        public ColocRoi(String roiName, double areaSize, double areaGrayScale, double circularity) {
            super(roiName, areaSize, areaGrayScale, circularity);
            // TODO Auto-generated constructor stub
        }

    }

}
