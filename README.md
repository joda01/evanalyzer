# Exosome Analyzer

## Description
Exosome Analyzer is a Fiji ImageJ plugin designed for analyzing microscope pictures of Exosomes. This plugin can be used for:
*  Counting the number of exosomes.
*  Calculating the colocalization of exosomes and other particles.
*  Detect biological cells and counting exosomes updatekn from cells.
*  Analysis of a large amount of images automatically.
*  Generating XLSX (Excel) reports with automatic statistics generation.


## Installation
*  Download and instal Fiji from [Fiji](https://imagej.net/Fiji/Downloads).
*  Copy the file ```ExosomeAnalyzer.jar``` to the folder ```Plugins``` of your Fiji installation.
*  Copy the content of the [jars.zip](https://github.com/joda01/exosome_analyzer/raw/master/bin/jars.zip) folder in the ```jars``` folder of your Fiji installation.
*  Restart Fiji.
*  You will find the plugin the menu: Plugins -> Analyze -> Exosoms


## Contributing
For development Visual Studio Code is used. Compiling is done within a docker image (dev container).
*  Download and install [Visual Studio Code](https://code.visualstudio.com/)
*  Install ```Remote Container``` plugin.
*  Clone the repository and open the cloned folder in Visual Studio Code.
*  Reopen the project in Dev Container.
*  Run ```./src/build.sh``` to compile the project.


## Common issues
*  Images used in the plugin cannot be in the RGB format, and must be monochromatic.
*  Actually only VSI images can be processed.

## Screenshot

![myimage-alt-tag](./doc/screeshot.png)
