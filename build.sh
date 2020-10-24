javac -source 1.7 -target 1.7 -cp "fijii/jars/ij-1.53c.jar" org/danmayr/imagej/*.java
jar cmvf META-INF/MANIFEST.MF EvColoc.jar plugins.config org/danmayr/imagej/*.class org/danmayr/imagej/*.png org/danmayr/imagej/*.jpg
