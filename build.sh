#find . -name "*.class" -type f -delete
cd src
javac -source 1.8 -target 1.8 -cp "../libs/fijii/jars/ij-1.53c.jar":"../libs/evanalyzer/poi-4.1.2.jar":"../libs/evanalyzer/poi-excelant-4.1.2.jar":"../libs/evanalyzer/poi-ooxml-4.1.2.jar":"../libs/evanalyzer/poi-ooxml-schemas-4.1.2.jar":"../libs/evanalyzer/commons-lang3-3.11.jar":"../libs/evanalyzer/opencsv-4.1.jar":"../libs/fijii/plugins/bio-formats_plugins-6.6.0.jar":"../libs/fijii/jars/bio-formats/formats-api-6.5.1.jar" org/danmayr/imagej/*.java org/danmayr/imagej/gui/*.java org/danmayr/imagej/algorithm/*.java org/danmayr/imagej/algorithm/filters/*.java org/danmayr/imagej/algorithm/pipelines/*.java org/danmayr/imagej/algorithm/structs/*.java org/danmayr/imagej/exports/*.java org/danmayr/imagej/algorithm/statistics/*.java org/danmayr/imagej/performance_analyzer/*.java ./*.java org/json/*.java -d ../bin/

cd ../bin
jar -cmvf META-INF/MANIFEST.MF EVAnalyzer.jar plugins.config ./org/* Main.class META-INF/maven/evanalyzer/evanalyzer_/pom.properties META-INF/maven/evanalyzer/evanalyzer_/pom.xml

cd ../src
jar -fu ../bin/EVAnalyzer.jar org/danmayr/imagej/Version.java org/danmayr/imagej/gui/*.png templates/*.json


