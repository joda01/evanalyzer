package org.danmayr.imagej.algorithm;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Vector;

import org.danmayr.imagej.algorithm.pipelines.*;
import org.json.*;

import ij.IJ;

public class AnalyseSettings {

    public enum ReportType {
        FullReport, FastReport
    }

    public enum CotrolPicture {
        WithControlPicture, WithoutControlPicture
    }

    public enum Function {
        noSelection("--No selection--"),
        evColoc("EV coloc"),
        evCount("EV count"),
        evCountInTotalCellArea("EV count per total Cell area"),
        evCountPerCell("EV count per cell"),
        evCountPerCellRemoveCropped("EV count per cell remove cropped cells");

        private final String name;

        private Function(String s) {
            name = s;
        }

        public boolean equalsName(String otherName) {
            // (otherName == null) check is not needed because name.equals(null) returns
            // false
            return name.equals(otherName);
        }

        public String getStringName() {
            return this.name;
        }
    }

    public CotrolPicture mSaveDebugImages = CotrolPicture.WithControlPicture;
    public ReportType reportType = ReportType.FullReport;
    public Function mSelectedFunction;
    public String mInputFolder;
    public String mOutputFolder;
    public int mSelectedSeries; // series_1 = 0
    public String mReportName = "";
    public Vector<ChannelSettings> channelSettings = new Vector<ChannelSettings>();

    /// Autofilled
    public boolean mCountEvsPerCell = false;
    public boolean mRemoveCellsWithoutNucleus = false;
    public boolean mCalcColoc = false;
    public double mOnePixelInMicroMeter = 1;
    public double mMinColocFactor = 1;

    public double pixelToMicrometer(double pxl) {
        return pxl * mOnePixelInMicroMeter;
    }

    public double pixelAreaToMicrometer(double pxl) {
        return pxl * mOnePixelInMicroMeter * mOnePixelInMicroMeter;
    }

    public void saveSettings(String fileName, String title, String note) {

        JSONObject obj = new JSONObject();

        obj.put("title", title);
        obj.put("note", note);
        obj.put("ctrl_images", mSaveDebugImages);
        obj.put("report_type", reportType);
        obj.put("function", mSelectedFunction);
        obj.put("series", mSelectedSeries);
        obj.put("pixel_in_micrometer", mOnePixelInMicroMeter);
        obj.put("min_coloc_factor", mMinColocFactor);

        JSONArray ary = new JSONArray();
        for (int n = 0; n < channelSettings.size(); n++) {
            ary.put(channelSettings.get(n).saveSettings());
        }
        obj.put("channels", ary);

        try {
            FileWriter myWriter = new FileWriter(fileName);
            myWriter.write(obj.toString(2));
            myWriter.close();
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }

    }

    public void loadSettingsFromFile(String fileName) {
        try {
            String content = new String(Files.readAllBytes(Paths.get(fileName)));
            loadSettingsFromJson(content);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            IJ.log("Error " + e.getMessage());
        }
    }

    public void loadSettingsFromJson(String content) {

        JSONObject obj = new JSONObject(content);

        mSaveDebugImages = CotrolPicture.valueOf(obj.getString("ctrl_images"));
        reportType = ReportType.valueOf(obj.getString("report_type"));
        mSelectedFunction = Function.valueOf(obj.getString("function"));
        mSelectedSeries = obj.getInt("series");
        mSelectedSeries = obj.getInt("series");
        try {
            mOnePixelInMicroMeter = Double.parseDouble(obj.getString("pixel_in_micrometer"));
        } catch (Exception ex) {
            try {
                mOnePixelInMicroMeter = obj.getDouble("pixel_in_micrometer");
            } catch (Exception ex1) {
                try {
                    mOnePixelInMicroMeter = obj.getInt("pixel_in_micrometer");
                } catch (Exception ex2) {
                    mOnePixelInMicroMeter = 1;
                }
            }
        }

        try {
            mMinColocFactor = Double.parseDouble(obj.getString("min_coloc_factor"));
        } catch (Exception ex) {

        }

        JSONArray ary = obj.getJSONArray("channels");

        channelSettings.removeAllElements();
        for (int n = 0; n < ary.length(); n++) {
            ChannelSettings ch = new ChannelSettings(this);
            ch.loadSettings(ary.getJSONObject(n));
            channelSettings.add(ch);
        }

    }

}
