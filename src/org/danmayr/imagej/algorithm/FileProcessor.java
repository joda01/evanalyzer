package org.danmayr.imagej.algorithm;

import ij.*;
import ij.process.*;

import java.io.File;

import loci.plugins.BF;
import loci.plugins.in.ImporterOptions;
import ij.gui.*;
import java.awt.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.ResultSet;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.swing.JDialog;
import javax.swing.JWindow;

import ij.plugin.*;
import ij.plugin.frame.*;

import java.awt.*;

import org.danmayr.imagej.algorithm.structs.*;
import org.danmayr.imagej.algorithm.structs.Image;
import org.danmayr.imagej.exports.*;

import org.danmayr.imagej.algorithm.*;
import org.danmayr.imagej.algorithm.filters.*;
import org.danmayr.imagej.algorithm.pipelines.*;
import org.danmayr.imagej.algorithm.pipelines.Pipeline.ChannelType;
import org.danmayr.imagej.gui.Dialog;
import org.danmayr.imagej.performance_analyzer.PerformanceAnalyzer;

public class FileProcessor extends Thread {

    Dialog mDialog;
    boolean mStopping = false;
    AnalyseSettings mAnalyseSettings;
    FolderResults mResuls = new FolderResults();

    public FileProcessor(final Dialog dialog, final AnalyseSettings analyseSettings) {
        mDialog = dialog;
        mAnalyseSettings = analyseSettings;
    }

    /**
     * Start the analyse thread
     */
    public void run() {

        mStopping = false;
        // Close all open windows
        closeAllWindow();
        WindowManager.closeAllWindows();

        // Prepare results folder
        prepareOutputFolder();

        mDialog.setProgressBarMaxSize(0, "look for images ...");
        mDialog.setProgressBarValue(0, "look for images ...");

        //
        // List all files in folders and subfolders
        //
        ArrayList<File> mFoundFiles = new ArrayList<>();
        findFiles(new File(mAnalyseSettings.mInputFolder).listFiles(), mFoundFiles);
        mDialog.setProgressBarMaxSize(mFoundFiles.size(), "analyzing ...");
        mDialog.setProgressBarValue(0, "analyzing ...");

        ExcelExportStream expStream = new ExcelExportStream(mAnalyseSettings.mOutputFolder, "report", mAnalyseSettings);

        walkThroughFiles(mFoundFiles, expStream);

        // String reportFileName = ExcelExport.Export(mAnalyseSettings.mOutputFolder,
        // "report",
        // mResuls, mAnalyseSettings.reportType, mAnalyseSettings, mDialog);

        // Write statistics to file
        /*
         * InputFiles input = new InputFiles(); input.add(analysisOutput,"Results");
         * input.add(negativeControl,"NegativeControls"); String xlsxResult =
         * mAnalyseSettings.mOutputFolder + File.separator + "statistic_all_over_final";
         * String convertCsvToXls = CsvToExcel.convertCsvToXls(xlsxResult, input);
         */

        mDialog.finishedAnalyse(expStream.getFileName());
    }

    ///
    /// \brief Get file from selected folder
    ///
    public static File getFile(int idx, String inputFolder) {
        ArrayList<File> mFoundFiles = new ArrayList<>();
        findFiles(new File(inputFolder).listFiles(), mFoundFiles);

        if (idx < mFoundFiles.size()) {
            return mFoundFiles.get(idx);
        } else {
            return null;
        }
    }

    public static ImagePlus[] OpenImage(File imgToOpen, int series, boolean showImg) {
        ImagePlus[] imps = null;
        try {
            String fileName = imgToOpen.getAbsoluteFile().toString();
            ImporterOptions opt = new ImporterOptions();
            opt.setColorMode(ImporterOptions.COLOR_MODE_GRAYSCALE);
            opt.setStackOrder(ImporterOptions.ORDER_XYZCT);
            opt.setSeriesOn(series, true);
            opt.setSplitChannels(true);
            opt.setSpecifyRanges(false);
            opt.setId(fileName);
            imps = BF.openImagePlus(opt);

            if (showImg == true) {
                for (ImagePlus imp : imps)
                    imp.show();
                IJ.run("Tile", "");
                IJ.run("Tile", "");
            }

        } catch (Exception exc) {
            IJ.error("Sorry, an error occurred: " + exc.getMessage());
            IJ.log("ERROR " + exc.getMessage());
        }

        return imps;

        /*
         * IJ.run("Bio-Formats Importer", "open=[" +
         * imgToOpen.getAbsoluteFile().toString() +
         * "] autoscale color_mode=Grayscale rois_import=[ROI manager] specify_range split_channels view=Hyperstack stack_order=XYCZT "
         * + series); PerformanceAnalyzer.stop(); PerformanceAnalyzer.start("Tile");
         * IJ.run("Tile", ""); IJ.run("Tile", ""); PerformanceAnalyzer.stop();
         */
    }

    /**
     * Cancle the process after the actual image has been finished
     */
    public void cancle() {
        mStopping = true;
    }

    private void walkThroughFiles(ArrayList<File> fileList, ExcelExportStream exporter) {

        IJ.log("Using " + mAnalyseSettings.mNrOfCpuCoresToUse + " CPU cores!");
        mDialog.addLogEntryNewLine();
        PerformanceAnalyzer.start("analyze_files");
        mDialog.setAlwaysOnTop(true);

        int parallelWorkers = mAnalyseSettings.mNrOfCpuCoresToUse - fileList.size();
        if (parallelWorkers < 1) {
            parallelWorkers = 1;
        }

        IJ.log("Parallel workers " + parallelWorkers + " CPU cores!");

        int nrOfCPUS = mAnalyseSettings.mNrOfCpuCoresToUse - parallelWorkers;
        if (nrOfCPUS < 1) {
            nrOfCPUS = 1;
        }

        IJ.log("CPU workers " + nrOfCPUS + " CPU cores!");

        ThreadPoolExecutor exec = (ThreadPoolExecutor) Executors.newFixedThreadPool(nrOfCPUS);

        for (File file : fileList) {
            if (true == mStopping) {
                break;
            }
            ProcessImage e = new ProcessImage(file, parallelWorkers,exporter);
            exec.execute(e);
            IJ.log("TH: " + exec.getQueue().size());
            while (exec.getQueue().size() >= mAnalyseSettings.mNrOfCpuCoresToUse) {
                try {
                    sleep(1000);
                } catch (InterruptedException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
            }
        }
        exec.shutdown();
        try {
            exec.awaitTermination(7, TimeUnit.DAYS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        closeAllWindow();
        mDialog.setAlwaysOnTop(false);
        mDialog.tabbedPane.setSelectedIndex(0);
        PerformanceAnalyzer.stop("analyze_files");
    }

    //
    // Process images thread
    //
    class ProcessImage implements Runnable {
        File fileToAnalyse;
        Pipeline pipeline = null;
        boolean mCanceled = false;
        ExcelExportStream exporter;

        ProcessImage(File fileToAnalyse, int parallelWorkers, ExcelExportStream exporter) {
            mCanceled = false;
            this.fileToAnalyse = fileToAnalyse;
            this.exporter = exporter;

            if (mAnalyseSettings.mSelectedFunction.equals(AnalyseSettings.Function.evCount)) {
                mAnalyseSettings.mCalcColoc = false;
                mAnalyseSettings.mCountEvsPerCell = false;
                pipeline = new EVColoc(mAnalyseSettings, parallelWorkers);
            }
            if (mAnalyseSettings.mSelectedFunction.equals(AnalyseSettings.Function.evColoc)) {
                mAnalyseSettings.mCalcColoc = true;
                mAnalyseSettings.mCountEvsPerCell = false;
                pipeline = new EVColoc(mAnalyseSettings, parallelWorkers);
            }
            if (mAnalyseSettings.mSelectedFunction.equals(AnalyseSettings.Function.evCountInTotalCellArea)) {
                mAnalyseSettings.mCountEvsPerCell = false;
                mAnalyseSettings.mCalcColoc = false;
                pipeline = new EVCountInCells(mAnalyseSettings, parallelWorkers);
            }
            if (mAnalyseSettings.mSelectedFunction.equals(AnalyseSettings.Function.evCountPerCell)) {
                mAnalyseSettings.mCountEvsPerCell = true;
                mAnalyseSettings.mCalcColoc = false;
                pipeline = new EVCountInCells(mAnalyseSettings, parallelWorkers);
            }
            if (mAnalyseSettings.mSelectedFunction
                    .equals(AnalyseSettings.Function.evCountPerCellRemoveCropped)) {
                mAnalyseSettings.mCountEvsPerCell = true;
                mAnalyseSettings.mRemoveCellsWithoutNucleus = true;
                mAnalyseSettings.mCalcColoc = false;
                pipeline = new EVCountInCells(mAnalyseSettings, parallelWorkers);
            }
        }

        public void cancel() {
            mCanceled = true;
        }

        @Override
        public void run() {
            if (this.pipeline != null && false == this.mCanceled) {
                // TODO Auto-generated method stu
                ImagePlus[] imagesLoaded = OpenImage(this.fileToAnalyse, mAnalyseSettings.mSelectedSeries, false);
                if (imagesLoaded != null && imagesLoaded.length > 0) {

                    // Process images
                    Image image = this.pipeline.ProcessImage(this.fileToAnalyse, imagesLoaded);

                    // Only write details if full report should be created
                    if (mAnalyseSettings.reportType == AnalyseSettings.ReportType.FullReport) {
                        ExcelExport.WriteImageSheet(mAnalyseSettings.mOutputFolder, image);
                    }

                    exporter.writeRow(this.fileToAnalyse.getParent().toString(), image);

                    // Cleanup RAM
                    image.ClearParticleInf();
                    image = null;

                    // Add to results for summary report at the end
                    // mResuls.addImage(this.fileToAnalyse.getParent(), image);

                    // Close all open windows
                    for (int n = 0; n < imagesLoaded.length; n++) {
                        imagesLoaded[n].close();
                    }
                } else {
                    IJ.log("WARN: There was a problem loading the image: " + this.fileToAnalyse);
                }
            }
            mDialog.incrementProgressBarValue("analyzing ...");
        }
    }

    /**
     * List all images in directory and subdirectory
     * 
     * @param files
     */
    private static void findFiles(final File[] files, ArrayList<File> foundFiles) {
        if (null != files) {
            for (final File file : files) {
                if (file.isDirectory()) {
                    findFiles(file.listFiles(), foundFiles);
                } else if (file.getName().toLowerCase().endsWith(".vsi")
                        || file.getName().toLowerCase().endsWith(".tiff")
                        || file.getName().toLowerCase().endsWith(".czi")
                        || file.getName().toLowerCase().endsWith(".ics")
                        || file.getName().toLowerCase().endsWith(".tif")) {
                    foundFiles.add(file);
                }
            }
        }
    }

    private void prepareOutputFolder() {
        final File parentFile = new File(mAnalyseSettings.mOutputFolder);
        if (parentFile != null && !parentFile.exists()) {
            parentFile.mkdirs();
        }
    }

    private void closeAllWindow() {
        ImagePlus img;
        while (null != WindowManager.getCurrentImage()) {
            img = WindowManager.getCurrentImage();
            img.changes = false;
            img.close();
        }
    }

}
