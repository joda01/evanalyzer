package org.danmayr.imagej.algorithm;

import java.util.Vector;

import org.danmayr.imagej.algorithm.pipelines.*;
import org.danmayr.imagej.algorithm.pipelines.Pipeline.ChannelType;

import ij.ImagePlus;
import ij.process.AutoThresholder;
import org.json.*;


public class ChannelSettings implements Cloneable {

    public enum PreProcessingStep {
        None, EdgeDetection
    }

    public ImagePlus mChannelImg;
    public int mChannelIndex = 0;
    public int mChannelNr = 0;
    public Pipeline.ChannelType type;
    public AutoThresholder.Method mThersholdMethod;
    public boolean enhanceContrast;
    public int minThershold = -1;
    public int maxThershold = 65535;
    public int snapAreaSize = 0;
    public String ZProjector = "OFF";
    public Vector<PreProcessingStep> preProcessing = new Vector<PreProcessingStep>();
    public int marginToCrop = 0;
    public double mMinCircularity = 0.0;
    public double mMinParticleSize = 0.0;
    public double mMaxParticleSize = 999999999;

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    JSONObject saveSettings(){
        JSONObject obj = new JSONObject();

        obj.put("index", mChannelIndex);
        obj.put("number", mChannelNr);
        obj.put("type", type.toString());
        obj.put("thershold", mThersholdMethod.toString());
        obj.put("enhance_contrast", enhanceContrast);
        obj.put("thershold_min", minThershold);
        obj.put("thershold_max", maxThershold);
        obj.put("zprojection", ZProjector);
        
        JSONArray ary = new JSONArray();
        for(int n = 0;n<preProcessing.size();n++){
            ary.put(preProcessing.get(n));
        }
        obj.put("preprocesing", ary);
        obj.put("margin_crop", marginToCrop);
        obj.put("min_circularity", mMinCircularity);
        obj.put("min_particle_size", mMinParticleSize);
        obj.put("max_particle_size", mMaxParticleSize);
        obj.put("snap_area_size", snapAreaSize);

        return obj;
    }

    void loadSettings(JSONObject obj){
        mChannelIndex = obj.getInt("index");
        mChannelNr = obj.getInt("number");
        type = ChannelType.valueOf(obj.getString("type"));
        mThersholdMethod = AutoThresholder.Method.valueOf(obj.getString("thershold"));
        enhanceContrast = obj.getBoolean("enhance_contrast");
        minThershold = obj.getInt("thershold_min");
        maxThershold = obj.getInt("thershold_max");
        ZProjector = obj.getString("zprojection");

        JSONArray ary =  obj.getJSONArray("preprocesing");
        preProcessing.removeAllElements();
        for(int i = 0;i<ary.length();i++){
            preProcessing.add(PreProcessingStep.valueOf(ary.getString(i)));
        }
        marginToCrop = obj.getInt("margin_crop");
        mMinCircularity = Double.parseDouble(obj.getString("min_circularity"));
        mMinParticleSize = obj.getInt("min_particle_size");
        mMaxParticleSize = obj.getInt("max_particle_size");
        snapAreaSize = obj.getInt("snap_area_size");
    }
}
