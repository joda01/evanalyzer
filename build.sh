#find . -name "*.class" -type f -delete
cd src
javac -source 1.8 -target 1.8 -cp "../libs/fijii/jars/ij-1.53c.jar":"../libs/poi-4.1.2/poi-4.1.2.jar":"../libs/poi-4.1.2/poi-excelant-4.1.2.jar":"../libs/poi-4.1.2/poi-ooxml-4.1.2.jar":"../libs/poi-4.1.2/poi-ooxml-schemas-4.1.2.jar":"../libs/commons-lang3-3.11/commons-lang3-3.11.jar":"../libs/opencsv/opencsv-4.1.jar":"../libs/fijii/plugins/bio-formats_plugins-6.6.0.jar":"../libs/fijii/jars/bio-formats/formats-api-6.5.1.jar" org/danmayr/imagej/updater/*.java org/danmayr/imagej/*.java org/danmayr/imagej/gui/*.java org/danmayr/imagej/algorithm/*.java org/danmayr/imagej/algorithm/filters/*.java org/danmayr/imagej/algorithm/pipelines/*.java org/danmayr/imagej/algorithm/structs/*.java org/danmayr/imagej/exports/*.java org/danmayr/imagej/algorithm/statistics/*.java org/danmayr/imagej/performance_analyzer/*.java ./*.java org/json/*.java -d ../bin/

cd ../bin
jar -cmvf META-INF/MANIFEST.MF ExosomeAnalyzer.jar plugins.config ./org/* Main.class github_auth_token.txt

cd ../src
jar -fu ../bin/ExosomeAnalyzer.jar org/danmayr/imagej/Version.java org/danmayr/imagej/gui/*.png templates/*.json


