package org.danmayr.imagej;

import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;

import javax.swing.JDialog;
import javax.swing.JWindow;

import ij.plugin.*;
import ij.plugin.frame.*;

import java.awt.*;

public class EvColoc implements PlugIn {

	@Override
	public void run(String arg) {
		ImagePlus imp = IJ.getImage();
		
		
		JDialog win = new JDialog();
		win.setSize(800, 600);
		win.setVisible(true);
		
		IJ.run(imp, "Invert", "");
		IJ.wait(1000);
		IJ.run(imp, "Invert", "");
	}

	public static void main(final String... args) {
		IJ.log("Hello, World!");


// set the plugins.dir property to make the plugin appear in the Plugins menu
		// see: https://stackoverflow.com/a/7060464/1207769
		Class<?> clazz = EvColoc.class;
		//java.net.URL url = clazz.getProtectionDomain().getCodeSource().getLocation();
		//System.setProperty("plugins.dir", file.getAbsolutePath());

		// start ImageJ
		new ImageJ();

		// open the Clown sample
		ImagePlus image = IJ.openImage("http://imagej.net/images/clown.jpg");
		image.show();

		// run the plugin
		IJ.runPlugIn(clazz.getName(), "");

	}

}


// javac -cp "/home/joachim/Documents/programme/Fiji.app/jars/ij-1.53c.jar" EvColoc.java
// jar cf jar-file input-file(s)
// jar uvf EvColoc.jar plugins.config