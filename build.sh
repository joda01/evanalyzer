javac -source 1.7 -target 1.7 -cp "fijii/jars/ij-1.53c.jar" org/danmayr/imagej/*.java org/danmayr/imagej/gui/*.java org/danmayr/imagej/algorithm/*.java
jar cmvf META-INF/MANIFEST.MF EvColoc.jar plugins.config org/danmayr/imagej/*.class org/danmayr/imagej/gui/*.class org/danmayr/imagej/algorithm/*.class org/danmayr/imagej/gui/*.png org/danmayr/imagej/gui/*.jpg
