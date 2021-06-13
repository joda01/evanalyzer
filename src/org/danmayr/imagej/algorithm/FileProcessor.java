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

import javax.swing.JDialog;
import javax.swing.JWindow;

import ij.plugin.*;
import ij.plugin.frame.*;

import java.awt.*;

import org.danmayr.imagej.algorithm.structs.*;
import org.danmayr.imagej.exports.*;

import org.danmayr.imagej.algorithm.*;
import org.danmayr.imagej.algorithm.filters.*;
import org.danmayr.imagej.algorithm.pipelines.*;

import org.danmayr.imagej.gui.EvColocDialog;
import org.danmayr.imagej.performance_analyzer.PerformanceAnalyzer;

public class FileProcessor extends Thread {

    EvColocDialog mDialog;
    boolean mStopping = false;
    AnalyseSettings mAnalyseSettings;
    FolderResults mResuls = new FolderResults();

    public FileProcessor(final EvColocDialog dialog, final AnalyseSettings analyseSettings) {
        mDialog = dialog;
        mAnalyseSettings = analyseSettings;
    }

    /**
     * Start the analyse thread
     */
    public void run() {

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

        // Analyse images
        Pipeline pipeline = null;

        if (mAnalyseSettings.mSelectedFunction.equals(AnalyseSettings.Function.countExosomes)) {
            pipeline = new ExosomCount(mAnalyseSettings);
        }
        if (mAnalyseSettings.mSelectedFunction.equals(AnalyseSettings.Function.calcColoc)) {
            int nrOfEnabledCh = 0;
            for (ChannelSettings chSett : mAnalyseSettings.channelSettings) {
                if (chSett.mChannelNr >= 0 && true == chSett.type.isEvChannel()) {
                    nrOfEnabledCh++;
                }
            }
            if (nrOfEnabledCh == 3) {
                pipeline = new ExosomeColoc3Ch(mAnalyseSettings);
            } else {
                pipeline = new ExosomColoc(mAnalyseSettings);
            }
        }
        if (mAnalyseSettings.mSelectedFunction.equals(AnalyseSettings.Function.countInCellExosomes)) {
            pipeline = new ExosomeCountInCells(mAnalyseSettings);
        }
        if (null == pipeline) {
            mDialog.finishedAnalyse("");
            return;
        }
        walkThroughFiles(pipeline, mFoundFiles);

        String reportFileName = ExcelExport.Export(mAnalyseSettings.mOutputFolder, mAnalyseSettings.mOutputFileName,
                mResuls, mAnalyseSettings.reportType, mAnalyseSettings, mDialog);

        // Write statistics to file
        /*
         * InputFiles input = new InputFiles(); input.add(analysisOutput,"Results");
         * input.add(negativeControl,"NegativeControls"); String xlsxResult =
         * mAnalyseSettings.mOutputFolder + File.separator + "statistic_all_over_final";
         * String convertCsvToXls = CsvToExcel.convertCsvToXls(xlsxResult, input);
         */

        mDialog.finishedAnalyse(reportFileName);
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

    public static ImagePlus[] OpenImage(File imgToOpen, int series) {

        int n = Runtime.getRuntime().availableProcessors();
        IJ.log("Available Processors: " + n);

        ImagePlus[] imps = null;
        try {
            PerformanceAnalyzer.start("open_image");
            String fileName = imgToOpen.getAbsoluteFile().toString();
            IJ.log(imgToOpen.getAbsoluteFile().toString());
            ImporterOptions opt = new ImporterOptions();
            opt.setColorMode(ImporterOptions.COLOR_MODE_GRAYSCALE);
            opt.setStackOrder(ImporterOptions.ORDER_XYZCT);
            opt.setSeriesOn(series, true);
            opt.setSplitChannels(true);
            opt.setSpecifyRanges(false);
            opt.setId(fileName);
            imps = BF.openImagePlus(opt);

            /*
             * PerformanceAnalyzer.start("Show image " + imgToOpen.getName()); for
             * (ImagePlus imp : imps) imp.show();
             */
            PerformanceAnalyzer.stop("open_image");
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

    /**
     * Walk through all found files and analyse each image after the other
     */
    Vector<Pair<File, ImagePlus[]>> mLoadedImages = new Vector<>();
    boolean mAllLoaded = false;

    private void walkThroughFiles(Pipeline algorithm, ArrayList<File> fileList) {
        PerformanceAnalyzer.start("walk_through_files");
        Thread t1 = new Thread(new Runnable() {
            @Override
            public void run() {
                while(mLoadedImages.size() > 3){
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
                for (final File file : fileList) {
                    ImagePlus[] imagesLoaded = OpenImage(file, mAnalyseSettings.mSelectedSeries);
                    mLoadedImages.add(new Pair(file, imagesLoaded));
                }
                mAllLoaded = true;
            }
        });
        t1.start();
        while (false == mAllLoaded) {
            while (mLoadedImages.size() > 0) {

                File file = mLoadedImages.elementAt(0).getFirst();
                TreeMap<Integer, Channel> images = algorithm.ProcessImage(file, mLoadedImages.elementAt(0).getSecond());
                mResuls.addImage(file.getParent(), file.getName(), images);
                mLoadedImages.removeElementAt(0);

                closeAllWindow();
                mDialog.incrementProgressBarValue("analyzing ...");
                if (true == mStopping) {
                    break;
                }
            }
        }
        try {
            t1.join();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        PerformanceAnalyzer.stop("walk_through_files");
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
                } else if (file.getName().endsWith(".vsi")) {
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
        IJ.log(parentFile.getAbsolutePath());
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
