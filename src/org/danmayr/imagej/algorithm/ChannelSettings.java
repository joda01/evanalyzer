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
    public String ZProjector = "OFF";
    public Vector<PreProcessingStep> preProcessing = new Vector<PreProcessingStep>();

    double snapAreaSize = 0;
    double marginToCrop = 0;
    double mMinCircularity = 0.0;
    double mMinParticleSize = 0;
    double mMaxParticleSize = 999999999;

    AnalyseSettings mAnalyzerSettings;

    public ChannelSettings(AnalyseSettings analyzerSettings) {
        mAnalyzerSettings = analyzerSettings;
    }

    public int getMarginCropPixel() {
        return (int) ((double) marginToCrop / (double) mAnalyzerSettings.mOnePixelInMicroMeter);
    }

    public int getSnapAreaSizePixel() {
        return (int) ((double) snapAreaSize / (double) mAnalyzerSettings.mOnePixelInMicroMeter);
    }

    public double getMarginCropDouble() {
        return marginToCrop;
    }

    public double getMinCircularityDouble() {
        return mMinCircularity;
    }

    public double getMinParticleSizeDouble() {
        return mMinParticleSize;
    }

    public double getMaxParticleSizeDouble() {
        return mMaxParticleSize;
    }

    public double getSnapAreaSizeDouble() {
        return snapAreaSize;
    }

    public void setMarginCropDouble(double v) {
        marginToCrop = v;
    }

    public void setMinCircularityDouble(double v) {
        mMinCircularity = v;
    }

    public void setMinParticleSizeDouble(double v) {
        mMinParticleSize = v;
    }

    public void setMaxParticleSizeDouble(double v) {
        mMaxParticleSize = v;
    }

    public void setSnapAreaSizeDoublw(double v) {
        snapAreaSize = v;
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    JSONObject saveSettings() {
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
        for (int n = 0; n < preProcessing.size(); n++) {
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

    void loadSettings(JSONObject obj) {
        mChannelIndex = obj.getInt("index");
        mChannelNr = obj.getInt("number");
        type = ChannelType.valueOf(obj.getString("type"));
        mThersholdMethod = AutoThresholder.Method.valueOf(obj.getString("thershold"));
        enhanceContrast = obj.getBoolean("enhance_contrast");
        minThershold = obj.getInt("thershold_min");
        maxThershold = obj.getInt("thershold_max");
        ZProjector = obj.getString("zprojection");

        JSONArray ary = obj.getJSONArray("preprocesing");
        preProcessing.removeAllElements();
        for (int i = 0; i < ary.length(); i++) {
            preProcessing.add(PreProcessingStep.valueOf(ary.getString(i)));
        }
        marginToCrop = Double.parseDouble(obj.getString("margin_crop"));
        mMinCircularity = Double.parseDouble(obj.getString("min_circularity"));
        mMinParticleSize = Double.parseDouble(obj.getString("min_particle_size"));
        mMaxParticleSize = Double.parseDouble(obj.getString("max_particle_size"));
        snapAreaSize = Double.parseDouble(obj.getString("snap_area_size"));
    }
}
