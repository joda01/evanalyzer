package org.danmayr.imagej.algorithm;

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
        noSelection("--No selection--"), calcColoc("EV Colocalization"), countEVs("EV Counting"),
        countInCellEVs("EV Counting in Cells"),
        countInCellEVsWithCellSeparation("EV Counting in Cells with cell separation"),
        countInCellEVsWithCellSeparationExcludeCellsWithoutNucleus(
                "EV Counting in Cells with cell separation + remove nucleus on edge and remove cells without nucleus");

        private final String name;

        private Function(String s) {
            name = s;
        }

        public boolean equalsName(String otherName) {
            // (otherName == null) check is not needed because name.equals(null) returns
            // false
            return name.equals(otherName);
        }
    }

    public CotrolPicture mSaveDebugImages = CotrolPicture.WithControlPicture;
    public ReportType reportType = ReportType.FullReport;
    public Function mSelectedFunction;
    public String mInputFolder;
    public String mOutputFolder;
    public int mSelectedSeries; // series_1 = 0
    public double minIntensity = 0.0;
    public String mOutputFileName = "";
    public Vector<ChannelSettings> channelSettings = new Vector<ChannelSettings>();

    /// Autofilled
    public boolean mCountEvsPerCell = false;
    public boolean mRemoveCellsWithoutNucleus = false;
    public boolean mCalcColoc = false;

    public void saveSettings(String fileName, String title, String note) {
        JSONObject obj = new JSONObject();

        obj.put("title", title);
        obj.put("note", note);
        obj.put("ctrl_images", mSaveDebugImages);
        obj.put("report_type", reportType);
        obj.put("function", mSelectedFunction);
        obj.put("series", mSelectedSeries);
        obj.put("min_intensity", minIntensity);

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
        minIntensity = obj.getInt("min_intensity");
        mSelectedSeries = obj.getInt("series");

        JSONArray ary = obj.getJSONArray("channels");

        channelSettings.removeAllElements();
        for (int n = 0; n < ary.length(); n++) {
            ChannelSettings ch = new ChannelSettings();
            ch.loadSettings(ary.getJSONObject(n));
            channelSettings.add(ch);
        }

    }

}
