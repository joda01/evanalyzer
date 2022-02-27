package org.danmayr.imagej;

import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import java.io.*;
import java.lang.management.ManagementFactory;

import javax.swing.JDialog;
import javax.swing.JWindow;

import ij.plugin.*;
import ij.plugin.frame.*;

import java.awt.*;
import org.danmayr.imagej.gui.Dialog;
import org.danmayr.imagej.performance_analyzer.PerformanceAnalyzer;

public class EVAnalyzer implements PlugIn {

	Dialog dialog;

	@Override
	public void run(String arg) {

		dialog = new Dialog();
		PerformanceAnalyzer.setGui(dialog);
		dialog.toFront();
		dialog.setVisible(true);
	}

	public static void main(String[] args) {

		// set the plugins.dir property to make the plugin appear in the Plugins menu
		// see: https://stackoverflow.com/a/7060464/1207769
		Class<?> clazz = EVAnalyzer.class;
		// java.net.URL url = clazz.getProtectionDomain().getCodeSource().getLocation();
		// System.setProperty("plugins.dir", file.getAbsolutePath());

		// start ImageJ
		new ImageJ();

		// run the plugin
		IJ.runPlugIn(clazz.getName(), "");

	}

	public static void restart() {

		try {
			String execNames[] = { "ImageJ-linux64", "ImageJ-linux32", "ImageJ-win32.exe", "ImageJ-win64.exe" };
			String path = new File(".").getCanonicalPath();

			for (int n = 0; n < execNames.length; n++) {
				File tempFile = new File(path + File.separator + execNames[n]);
				if (tempFile.exists()) {
					String[] params = new String[3];
					params[0] = path + File.separator + execNames[n];
					params[1] = "-run";
					params[2] = "EVAnalyzer";
					Runtime.getRuntime().exec(params);
					break;
				}
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			Thread.currentThread().sleep(2000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} // 10 seconds delay before restart

		System.exit(0);
	}

}

// javac -cp "/home/joachim/Documents/programme/Fiji.app/jars/ij-1.53c.jar"
// EvColoc.java
// jar cf jar-file input-file(s)
// jar uvf EvColoc.jar plugins.config