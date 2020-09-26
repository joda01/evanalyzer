javac -source 1.7 -target 1.7 -cp "fijii/jars/ij-1.53c.jar" org/danmayr/imagej/EvColoc.java
jar cf EvColoc.jar org/danmayr/imagej/EvColoc.class
jar uvf EvColoc.jar plugins.config