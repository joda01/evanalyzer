package org.danmayr.imagej.algorithm.statistics;

import java.util.Map;

import org.danmayr.imagej.algorithm.structs.*;

public class StatisticsColoc extends Statistics {
    String title[] = { "area size [µm]", "circularity [0-1]","valid","invalid" }; 
    
    double[] intensityChannelsSum = null;
    double[] areaSizeChannelsSum = null;
    double[] retValues  = null;
    
    public StatisticsColoc() {

    }

    public void calcStatistics(Channel ch) {
        String dynTitle[] = ch.getDynamicTitle();
        title = new String[4+dynTitle.length];
        title[0]="coloc area";
        title[1]="coloc circ.";
        title[2]="coloc valid";
        title[3]="coloc invalid";
        retValues = new  double[4+dynTitle.length];

        for(int u = 0;u<dynTitle.length;u++){
            title[u+4] = dynTitle[u];
        }
        
        int nrOfInvalid = 0;
        int nrOfValid = 0;
        double areaSizeSum = 0;
        double grayScaleSum = 0;
        double circularitySum = 0;

        intensityChannelsSum = null;
        areaSizeChannelsSum = null;

        for (Map.Entry<Integer, ParticleInfo> entry : ch.getRois().entrySet()) {
            ParticleInfoColoc info = (ParticleInfoColoc)entry.getValue();

            if (false == info.isValid()) {
                nrOfInvalid++;
            } else {
                nrOfValid++;
                areaSizeSum += info.areaSize;
                grayScaleSum += info.areaGrayScale;
                circularitySum += info.circularity;

                if(intensityChannelsSum == null){
                    intensityChannelsSum = new double[info.intensityChannels.length];
                    areaSizeChannelsSum = new double[info.areaSizeChannels.length];
                }

                for(int n=0;n<intensityChannelsSum.length;n++){
                    intensityChannelsSum[n] += info.intensityChannels[n];
                }

                for(int n=0;n<areaSizeChannelsSum.length;n++){
                    areaSizeChannelsSum[n] += info.areaSizeChannels[n];
                }

            }
        }
        if(nrOfValid > 0){
            avgAreaSize = areaSizeSum / nrOfValid;
            avgGrayScale = grayScaleSum / nrOfValid;
            avgCircularity = circularitySum / nrOfValid;

            int idx = 4;
            for(int n=0;n<intensityChannelsSum.length;n++){
                retValues[idx] = intensityChannelsSum[n] / nrOfValid;
                idx++;
            }

            for(int n=0;n<areaSizeChannelsSum.length;n++){
                retValues[idx] = areaSizeChannelsSum[n] / nrOfValid;
                idx++;
            }

        }else{
            avgAreaSize = 0;
            avgGrayScale = 0;
            avgCircularity = 0;
        }
        this.invalid = nrOfInvalid;
        this.valid = nrOfValid;



        retValues[0] = avgAreaSize;
        retValues[1] = avgCircularity;
        retValues[2] = valid;
        retValues[3] = invalid;
    }

    public double[] getValues() {
        return retValues;
    }

    @Override
    public String[] getTitle() {
        return title;
    }
}
