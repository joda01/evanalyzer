[![Release to Update Site](https://github.com/joda01/evanalyzer/actions/workflows/release.yaml/badge.svg)](https://github.com/joda01/evanalyzer/actions/workflows/release.yaml)
# EVAnalyzer

Please forward to [EVAnalyzer 2](https://github.com/joda01/imagec) powered by ImageC


![./doc/screenshot_v80.png](./doc/screenshot_v80.png)

[](https://onlinelibrary.wiley.com/doi/full/10.1002/jev2.12282)

## Description
EVAnalyzer is a Fiji ImageJ plugin designed for analyzing microscope pictures of EVs. This plugin can be used for:
*  Counting the number of EVs.
*  Calculating the colocalization of evs and other particles.
*  Detect biological cells and counting EVs updatekn from cells.
*  Analysis of a large amount of images automatically.
*  Generating XLSX (Excel) reports with automatic statistics generation.


## Installation
*  Download and instal Fiji from [Fiji](https://imagej.net/Fiji/Downloads).
*  Add EVAnalyzer to update sites:
  1.  Open Fiji
  2.  Click `Help -> Update`
  ![add_update_site_01.png](doc/add_update_site_01.png)
  3.  Click `Manage update sites`
  ![add_update_site_02.png](doc/add_update_site_02.png)
  4.  Click `Add update site`
    ![add_update_site_03.png](doc/add_update_site_03.png)
  5. Look `EVAnalayzer` in the update site list or enter `EVAnalayzer` in name field and `https://sites.imagej.net/evanalyzer/` in the URL field
    ![add_update_site_04.png](doc/add_update_site_04.png)
  6. Restart Fiji


## Roadmap

- [ ] Make it possible to automatically generate reports for 10'000 and more images per run.
- [ ] Export of `R` files and automatic diagram generation.
- [ ] Implement automatic unit test as positive and negative control reference to be sure that an update does not influence future analysis results.
- [ ] Improve preview functionaltity to be more detailed.
- [ ] Extend to support more than 5 channels.
- [ ] Include handbook and Help functionality directly from the PlugIn.


## Contributing

If you have some questions, feature requests or if you found a bug, please just create an issue in the Issues tab.

## Coding

For development Visual Studio Code is used. Compiling is done within a docker image (dev container).
*  Download and install [Visual Studio Code](https://code.visualstudio.com/)
*  Install ```Remote Container``` plugin.
*  Clone the repository and open the cloned folder in Visual Studio Code.
*  Reopen the project in Dev Container.
*  Run ```./build.sh``` to compile the project.


## Common issues
*  Images used in the plugin cannot be in the RGB format, and must be monochromatic.
*  Actually VSI, TIFF, ICS, and CZI images can be processed.

---

## Research Article

First published: 27. November 2022 | https://doi.org/10.1002/jev2.12282

## Cite

Schürz, M., Danmayr, J., Jaritsch, M., Klinglmayr, E., Benirschke, H. M., Matea, C. -. T., Zimmerebner, P., Rauter, J., Wolf, M., Gomes, F. G., Kratochvil, Z., Heger, Z., Miller, A., Heuser, T., Stanojlovic, V., Kiefer, J., Plank, T., Johnson, L., Himly, M., … Meisner-Kober, N. (2022). EVAnalyzer: High content imaging for rigorous characterisation of single extracellular vesicles using standard laboratory equipment and a new open-source ImageJ/Fiji plugin. Journal of Extracellular Vesicles, 11, e12282. https://doi.org/10.1002/jev2.12282

