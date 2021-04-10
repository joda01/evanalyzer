package org.danmayr.imagej.gui;

import ij.*;
import ij.process.*;
import ij.gui.*;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.danmayr.imagej.algorithm.*;
import org.danmayr.imagej.algorithm.pipelines.*;
import org.danmayr.imagej.algorithm.filters.*;

import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.IOException;
import java.util.prefs.Preferences;

import org.danmayr.imagej.Version;

///
///
///
public class EvColocDialog extends JFrame {

    private static final String PLEASE_SELECT_A_FUNCTION = "Please select a function!\n";

    private static final long serialVersionUID = 1L;

    private String mNameOfLastGeneratedReportFile = new String("");

    private JTextField mInputFolder = new JTextField(30);
    private JTextField mOutputFolder = new JTextField(30);

    private JButton mbInputFolder;
    private JButton mbOutputFolder;
    private JButton mbStart;
    private JButton mCancle;
    private JButton mClose;
    private JButton mOpenResult;
    private JProgressBar mProgressbar = new JProgressBar();
    private JComboBox mFunctionSelection;
    private JComboBox mSeries;
    private FileProcessor mActAnalyzer = null;
    private JPanel mMenu;

    public class ComboItem<T> {
        private T value;
        private String label;

        public ComboItem(T value, String label) {
            this.value = value;
            this.label = label;
        }

        public T getValue() {
            return this.value;
        }

        public String getLabel() {
            return this.label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    class PanelChannelSettings extends JPanel {

        class ChannelElements {
            SpinnerModel model = new SpinnerNumberModel(-1, // initial value
                    -1, // min
                    65535, // max
                    1); // step
            private JSpinner minTheshold = new JSpinner(model);
            private JComboBox channelType;
            private JComboBox thersholdMethod;
            private JCheckBox enchanceContrast;
            private int mChNr;

            public ChannelElements(JPanel panel, GridBagConstraints c, int gridX, int gridY, int chNr) {

                c.fill = GridBagConstraints.HORIZONTAL;
                c.gridx = gridX;
                c.gridy = gridY;
                c.weightx = 1;
                c.gridwidth = 1;
                panel.add(new JLabel("Channel " + Integer.toString(chNr)), c);

                ////////////////////////////////////////////////////
                ComboItem<Pipeline.ChannelType>[] channels0 = new ComboItem[4];
                channels0[0] = new ComboItem<Pipeline.ChannelType>(Pipeline.ChannelType.OFF, "OFF");
                channels0[1] = new ComboItem<Pipeline.ChannelType>(Pipeline.ChannelType.GFP, "GFP");
                channels0[2] = new ComboItem<Pipeline.ChannelType>(Pipeline.ChannelType.CY3, "CY3");
                channels0[3] = new ComboItem<Pipeline.ChannelType>(Pipeline.ChannelType.NEGATIVE_CONTROL,
                        "Negative Control");

                c.fill = GridBagConstraints.HORIZONTAL;
                c.gridy++;
                channelType = new JComboBox<ComboItem<Pipeline.ChannelType>>(channels0);
                panel.add(channelType, c);

                ////////////////////////////////////////////////////
                String[] thersholdAlgo = { "Li", "MaxEntropy", "Moments", "Otsu" };
                c.fill = GridBagConstraints.HORIZONTAL;
                c.gridy++;
                thersholdMethod = new JComboBox<String>(thersholdAlgo);
                thersholdMethod.addItemListener(new ItemListener() {
                    @Override
                    public void itemStateChanged(ItemEvent e) {
                        refreshPreview();
                    }
                });

                panel.add(thersholdMethod, c);

                ////////////////////////////////////////////////////
                minTheshold.addChangeListener(new ChangeListener() {
                    @Override
                    public void stateChanged(ChangeEvent e) {
                        refreshPreview();
                    }
                });
                c.gridy++;
                panel.add(minTheshold, c);

                ////////////////////////////////////////////////////
                c.fill = GridBagConstraints.HORIZONTAL;
                c.gridy++;
                enchanceContrast = new JCheckBox("Enhance contrast");
                enchanceContrast.setContentAreaFilled(false);
                panel.add(enchanceContrast, c);
            }
        }

        private JToggleButton thersholdPreview;
        public ChannelElements ch0;
        public ChannelElements ch1;
        public ChannelElements ch2;

        public PanelChannelSettings(Container parent) {
            GridBagLayout layout = new GridBagLayout();
            setLayout(layout);
            layout.preferredLayoutSize(parent);

            GridBagConstraints c = new GridBagConstraints();
            c.insets = new Insets(4, 4, 4, 4); // top padding
            c.anchor = GridBagConstraints.WEST;

            ch0 = new ChannelElements(this, c, 1, 0, 0);
            ch1 = new ChannelElements(this, c, 2, 0, 1);
            ch2 = new ChannelElements(this, c, 3, 0, 2);

            ////////////////////////////////////////////////////
            c.fill = GridBagConstraints.HORIZONTAL;
            c.gridx = 0;
            c.gridy = 1;
            c.weightx = 0.0;
            JLabel l = new JLabel("Type:");
            ImageIcon diamter = new ImageIcon(getClass().getResource("icons8-protein-16.png"));
            l.setIcon(diamter);
            l.setMinimumSize(new Dimension(200, l.getMinimumSize().height));
            l.setMaximumSize(new Dimension(200, l.getMaximumSize().height));
            l.setPreferredSize(new Dimension(200, l.getPreferredSize().height));
            l.setSize(new Dimension(200, l.getSize().height));
            this.add(l, c);

            ////////////////////////////////////////////////////
            c.fill = GridBagConstraints.HORIZONTAL;
            c.gridx = 0;
            c.gridy++;
            c.weightx = 0.0;
            JLabel l1 = new JLabel("Thersholding:");
            l1.setMinimumSize(new Dimension(200, l.getMinimumSize().height));
            l1.setMaximumSize(new Dimension(200, l.getMaximumSize().height));
            l1.setPreferredSize(new Dimension(200, l.getPreferredSize().height));
            l1.setSize(new Dimension(200, l.getSize().height));
            ImageIcon diamter1 = new ImageIcon(getClass().getResource("icons8-brightness-16.png"));
            l1.setIcon(diamter1);
            this.add(l1, c);

            ////////////////////////////////////////////////////
            c.fill = GridBagConstraints.HORIZONTAL;
            c.gridx = 0;
            c.gridy++;
            c.weightx = 1;
            c.weightx = 0.0;
            c.gridwidth = 1;
            JLabel l2 = new JLabel("Man. thersh. [-1-65535]:");
            l2.setMinimumSize(new Dimension(200, l.getMinimumSize().height));
            l2.setMaximumSize(new Dimension(200, l.getMaximumSize().height));
            l2.setPreferredSize(new Dimension(200, l.getPreferredSize().height));
            l2.setSize(new Dimension(200, l.getSize().height));
            ImageIcon diamter2 = new ImageIcon(getClass().getResource("icons8-plus-slash-minus-16.png"));
            l2.setIcon(diamter2);
            this.add(l2, c);

            c.fill = GridBagConstraints.HORIZONTAL;
            c.gridx = 0;
            c.gridy++;
            c.weightx = 1;
            c.weightx = 0.0;
            c.gridwidth = 1;
            JLabel l3 = new JLabel("Contrast:");
            l3.setMinimumSize(new Dimension(200, l3.getMinimumSize().height));
            l3.setMaximumSize(new Dimension(200, l3.getMaximumSize().height));
            l3.setPreferredSize(new Dimension(200, l3.getPreferredSize().height));
            l3.setSize(new Dimension(200, l3.getSize().height));
            ImageIcon diamter3 = new ImageIcon(getClass().getResource("icons8-plus-slash-minus-16.png"));
            l3.setIcon(diamter3);
            this.add(l3, c);

        
            JPanel previewButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT));

            ///////////////////////////////////////////////////////////////////
    
            thersholdPreview = new JToggleButton(new ImageIcon(getClass().getResource("icons8-eye-16.png")));
            thersholdPreview.addActionListener(new java.awt.event.ActionListener() {
                // Beim Drücken des Menüpunktes wird actionPerformed aufgerufen
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    if (thersholdPreview.isSelected()) {
                        mPrevImgIdx = 0;
                        startPreview();
                        refreshPreview();
                    } else {
                        endPreview();
                    }
                }
            });
            previewButtons.add(thersholdPreview);

          JButton prevPreviewImage = new JButton("<<");
            prevPreviewImage.addActionListener(new java.awt.event.ActionListener() {
                // Beim Drücken des Menüpunktes wird actionPerformed aufgerufen
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    if (thersholdPreview.isSelected()) {
                        endPreview();
                        if(mPrevImgIdx>0){
                            mPrevImgIdx--;
                        }
                        startPreview();
                        refreshPreview();
                    }
                }
            });
            previewButtons.add(prevPreviewImage);

            JButton nextPreviewImage = new JButton(">>");
            nextPreviewImage.addActionListener(new java.awt.event.ActionListener() {
                // Beim Drücken des Menüpunktes wird actionPerformed aufgerufen
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    if (thersholdPreview.isSelected()) {
                        endPreview();
                        mPrevImgIdx++;
                        startPreview();
                        refreshPreview();
                    }
                }
            });
            previewButtons.add(nextPreviewImage);

  
            c.gridy++;
            c.gridx = 3;
            c.weightx = 0;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.anchor = GridBagConstraints.LINE_END;
            c.gridwidth = 1;
            this.add(previewButtons, c);
        }

        private ImagePlus mOriginalImage0;
        private ImagePlus mPreviewImage0;

        private ImagePlus mOriginalImage1;
        private ImagePlus mPreviewImage1;

        private ImagePlus mOriginalImage2;
        private ImagePlus mPreviewImage2;

        private int mPrevImgIdx = 0;

        public void startPreview() {
            String[] imageTitles = WindowManager.getImageTitles();
            if (imageTitles.length <= 0) {
                File OpenImage = FileProcessor.getFile(mPrevImgIdx, mInputFolder.getText());
                if (null != OpenImage) {
                    FileProcessor.OpenImage(OpenImage, mSeries.getSelectedItem().toString());
                }
            }

            imageTitles = WindowManager.getImageTitles();

            if (imageTitles.length > 0) {

                for (int i = 0; i < imageTitles.length; i++) {
                    String actTitle = imageTitles[i];
                    ImagePlus imageTmp = WindowManager.getImage(actTitle);
                    if (true == actTitle.endsWith("C=" + Integer.toString(0))) {
                        mPreviewImage0 = imageTmp;
                        mOriginalImage0 = Filter.duplicateImage(imageTmp);
                    } else if (true == actTitle.endsWith("C=" + Integer.toString(1))) {
                        mPreviewImage1 = imageTmp;
                        mOriginalImage1 = Filter.duplicateImage(imageTmp);
                    }else if (true == actTitle.endsWith("C=" + Integer.toString(2))) {
                        mPreviewImage2 = imageTmp;
                        mOriginalImage2 = Filter.duplicateImage(imageTmp);
                    }

                    Dimension size = Toolkit.getDefaultToolkit().getScreenSize();
                    int width = (int)size.getWidth();

                    ImageWindow win = imageTmp.getWindow();
                    win.setSize(600, 400);
                    int newPos = this.getX() + this.getWidth() + 10 + i * 30;
                    win.setLocation(newPos, this.getY()+i*35);
                    IJ.run(imageTmp, "Scale to Fit", "");
                }

            } else {
                thersholdPreview.setSelected(false);
                JOptionPane.showMessageDialog(new JFrame(), "Open an image to apply the preview on it!", "Dialog",
                        JOptionPane.WARNING_MESSAGE);
            }
        }

        public void refreshPreview() {
            if (thersholdPreview.isSelected() == true) {
                setPreviewImage(mPreviewImage0, mOriginalImage0, ch0);
                setPreviewImage(mPreviewImage1, mOriginalImage1, ch1);
                setPreviewImage(mPreviewImage2, mOriginalImage2, ch2);
            }
        }

        private void setPreviewImage(ImagePlus imgPrev, ImagePlus imgOri, ChannelElements elem) {
            if (imgPrev != null) {

                int lowThershold = -1;
                try {
                    lowThershold = (Integer) elem.minTheshold.getValue();
                } catch (NumberFormatException ex) {
                }
                double[] th = new double[2];
                ImagePlus newImg = Filter.duplicateImage(imgOri);
                imgPrev.setImage(newImg);

                Pipeline.preFilterSetColocPreview(imgPrev, elem.enchanceContrast.isSelected(),
                        elem.thersholdMethod.getSelectedItem().toString(), lowThershold, 65535, th);

            } else {
                /*JOptionPane.showMessageDialog(new JFrame(), "Open an image to apply the preview on it!", "Dialog",
                        JOptionPane.WARNING_MESSAGE);*/
            }
        }

        public void endPreview() {
            closeAllWindow();
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

    class PanelFilter extends JPanel {

        private JTextField mMinParticleSize = new JTextField("5");
        private JTextField mMaxParticleSize = new JTextField("9999");
        private JTextField mMinCircularity = new JTextField("0");
        private JTextField mMinIntensity = new JTextField("0");

        public PanelFilter(Container parent) {
            GridBagLayout layout = new GridBagLayout();
            setLayout(layout);
            layout.preferredLayoutSize(parent);

            GridBagConstraints c = new GridBagConstraints();
            c.insets = new Insets(4, 4, 4, 4); // top padding
            c.anchor = GridBagConstraints.WEST;

            ////////////////////////////////////////////////////
            c.fill = GridBagConstraints.HORIZONTAL;
            c.gridx = 0;
            c.gridy = 0;
            c.weightx = 0;
            JLabel l = new JLabel("Particle size [0-9999]:");
            ImageIcon diamter = new ImageIcon(getClass().getResource("icons8-diameter-16.png"));
            l.setIcon(diamter);
            l.setMinimumSize(new Dimension(200, l.getMinimumSize().height));
            l.setMaximumSize(new Dimension(200, l.getMaximumSize().height));
            l.setPreferredSize(new Dimension(200, l.getPreferredSize().height));
            l.setSize(new Dimension(200, l.getSize().height));
            this.add(l, c);

            c.fill = GridBagConstraints.HORIZONTAL;
            c.gridx = 1;
            c.weightx = 1;
            this.add(mMinParticleSize, c);
            c.gridx = 2;
            c.weightx = 1;
            this.add(mMaxParticleSize, c);

            ////////////////////////////////////////////////////
            c.fill = GridBagConstraints.HORIZONTAL;
            c.gridx = 0;
            c.gridy++;
            c.weightx = 0;
            ImageIcon image = new ImageIcon(getClass().getResource("icons8-belarus-map-16.png"));
            JLabel circ = new JLabel("Min circularity [0-1]:");
            circ.setIcon(image);
            this.add(circ, c);

            c.fill = GridBagConstraints.HORIZONTAL;
            c.gridx = 1;
            c.weightx = 1;
            this.add(mMinCircularity, c);

            ////////////////////////////////////////////////////
            c.fill = GridBagConstraints.HORIZONTAL;
            c.gridx = 0;
            c.gridy++;
            c.weightx = 0;
            ImageIcon image1 = new ImageIcon(getClass().getResource("icons8-new-moon-16.png"));
            JLabel intensity = new JLabel("Min Intensity [0-65535]:");
            intensity.setIcon(image1);
            this.add(intensity, c);

            c.fill = GridBagConstraints.HORIZONTAL;
            c.gridx = 1;
            c.weightx = 1;
            this.add(mMinIntensity, c);
        }
    }

    class PanelReport extends JPanel {

        private JComboBox mComboReportGenerator;
        private JComboBox mControlPictures;
        private JTextField mReportFileName = new JTextField(30);

        public PanelReport(Container parent) {
            GridBagLayout layout = new GridBagLayout();
            setLayout(layout);
            layout.preferredLayoutSize(parent);

            GridBagConstraints c = new GridBagConstraints();
            c.insets = new Insets(4, 4, 4, 4); // top padding
            c.anchor = GridBagConstraints.WEST;

            ////////////////////////////////////////////////////
            c.fill = GridBagConstraints.HORIZONTAL;
            c.gridx = 0;
            c.gridy = 0;
            c.weightx = 0;
            JLabel l2 = new JLabel("Report filename:");
            ImageIcon diamter2 = new ImageIcon(getClass().getResource("icons8-rename-16.png"));
            l2.setIcon(diamter2);
            l2.setMinimumSize(new Dimension(200, l2.getMinimumSize().height));
            l2.setMaximumSize(new Dimension(200, l2.getMaximumSize().height));
            l2.setPreferredSize(new Dimension(200, l2.getPreferredSize().height));
            l2.setSize(new Dimension(200, l2.getSize().height));
            this.add(l2, c);

            c.fill = GridBagConstraints.HORIZONTAL;
            c.gridx = 1;
            c.weightx = 1;
            this.add(mReportFileName, c);

            ////////////////////////////////////////////////////
            c.fill = GridBagConstraints.HORIZONTAL;
            c.gridx = 0;
            c.gridy++;
            c.weightx = 0;
            JLabel l = new JLabel("Report type:");
            ImageIcon diamter = new ImageIcon(getClass().getResource("icons8-google-sheets-16.png"));
            l.setIcon(diamter);
            l.setMinimumSize(new Dimension(200, l.getMinimumSize().height));
            l.setMaximumSize(new Dimension(200, l.getMaximumSize().height));
            l.setPreferredSize(new Dimension(200, l.getPreferredSize().height));
            l.setSize(new Dimension(200, l.getSize().height));
            this.add(l, c);

            ComboItem<AnalyseSettings.ReportType>[] reportTypes = new ComboItem[2];
            reportTypes[0] = new ComboItem<AnalyseSettings.ReportType>(AnalyseSettings.ReportType.FullReport,
                    "Full report");
            reportTypes[1] = new ComboItem<AnalyseSettings.ReportType>(AnalyseSettings.ReportType.FastReport,
                    "Fast report");
            mComboReportGenerator = new JComboBox<ComboItem<AnalyseSettings.ReportType>>(reportTypes);
            c.fill = GridBagConstraints.HORIZONTAL;
            c.gridx = 1;
            c.weightx = 1;
            this.add(mComboReportGenerator, c);

            ////////////////////////////////////////////////////
            c.fill = GridBagConstraints.HORIZONTAL;
            c.gridx = 0;
            c.gridy++;
            c.weightx = 0;
            JLabel l1 = new JLabel("Control pictures:");
            ImageIcon diamter1 = new ImageIcon(getClass().getResource("icons8-image-gallery-16.png"));
            l1.setIcon(diamter1);
            l1.setMinimumSize(new Dimension(200, l1.getMinimumSize().height));
            l1.setMaximumSize(new Dimension(200, l1.getMaximumSize().height));
            l1.setPreferredSize(new Dimension(200, l.getPreferredSize().height));
            l1.setSize(new Dimension(200, l1.getSize().height));
            this.add(l1, c);

            ComboItem<AnalyseSettings.CotrolPicture>[] ctrlPictures = new ComboItem[2];
            ctrlPictures[0] = new ComboItem<AnalyseSettings.CotrolPicture>(
                    AnalyseSettings.CotrolPicture.WithControlPicture, "Generate control pictures");
            ctrlPictures[1] = new ComboItem<AnalyseSettings.CotrolPicture>(
                    AnalyseSettings.CotrolPicture.WithoutControlPicture, "No control pictures");
            mControlPictures = new JComboBox<ComboItem<AnalyseSettings.CotrolPicture>>(ctrlPictures);
            c.fill = GridBagConstraints.HORIZONTAL;
            c.gridx = 1;
            c.weightx = 1;
            this.add(mControlPictures, c);

        }
    }

    PanelChannelSettings chSettings = new PanelChannelSettings(this);
    PanelFilter filter = new PanelFilter(this);
    PanelReport reportSettings = new PanelReport(this);

    ///
    /// Constructor
    ///
    public EvColocDialog() {

        GridBagLayout layout = new GridBagLayout();
        setLayout(layout);

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4,4,4,4); // top padding

        ////////////////////////////////////////////////////
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 0;
        JLabel l1 = new JLabel("Input folder:");
        l1.setMinimumSize(new Dimension(200, l1.getMinimumSize().height));
        l1.setMaximumSize(new Dimension(200, l1.getMaximumSize().height));
        l1.setPreferredSize(new Dimension(200, l1.getPreferredSize().height));
        l1.setSize(new Dimension(200, l1.getSize().height));
        ImageIcon li1 = new ImageIcon(getClass().getResource("icons8-open-folder-in-new-tab-16.png"));
        l1.setIcon(li1);
        this.add(l1, c);
       

        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 1;
        c.weightx = 1;
        this.add(mInputFolder, c);

        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.LINE_END;
        c.gridx = 2;
        c.weightx = 0;
        c.weightx = 0;
        mbInputFolder = new JButton(new ImageIcon(getClass().getResource("icons8-opened-folder-16.png")));
        mbInputFolder.addActionListener(new java.awt.event.ActionListener() {
            // Beim Drücken des Menüpunktes wird actionPerformed aufgerufen
            public void actionPerformed(java.awt.event.ActionEvent e) {
                OpenDirectoryChooser(mInputFolder, mOutputFolder);
                String outFolder = mInputFolder.getText();
                outFolder = outFolder.substring(outFolder.lastIndexOf(File.separator) + 1);
                outFolder.replaceAll(File.separator, "");
                reportSettings.mReportFileName.setText(outFolder);
            }
        });
        this.add(mbInputFolder, c);

        ////////////////////////////////////////////////////
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        JLabel l2 = new JLabel("Output folder:");
        l2.setMinimumSize(new Dimension(200, l2.getMinimumSize().height));
        l2.setMaximumSize(new Dimension(200, l2.getMaximumSize().height));
        l2.setPreferredSize(new Dimension(200, l2.getPreferredSize().height));
        l2.setSize(new Dimension(200, l2.getSize().height));
        ImageIcon li2 = new ImageIcon(getClass().getResource("icons8-open-folder-in-new-tab-16.png"));
        l2.setIcon(li2);
        this.add(l2, c);


        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 1;
        c.weightx = 1;
        this.add(mOutputFolder, c);

        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.LINE_END;
        c.gridx = 2;
        c.weightx = 0;
        mbOutputFolder = new JButton(new ImageIcon(getClass().getResource("icons8-opened-folder-16.png")));
        mbOutputFolder.addActionListener(new java.awt.event.ActionListener() {
            // Beim Drücken des Menüpunktes wird actionPerformed aufgerufen
            public void actionPerformed(java.awt.event.ActionEvent e) {
                OpenDirectoryChooser(mOutputFolder, null);
            }
        });
        this.add(mbOutputFolder, c);

        ////////////////////////////////////////////////////
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        JLabel l3 = new JLabel("Series to import:");
        l3.setMinimumSize(new Dimension(200, l3.getMinimumSize().height));
        l3.setMaximumSize(new Dimension(200, l3.getMaximumSize().height));
        l3.setPreferredSize(new Dimension(200, l3.getPreferredSize().height));
        l3.setSize(new Dimension(200, l3.getSize().height));
        ImageIcon li3 = new ImageIcon(getClass().getResource("icons8-sheets-16.png"));
        l3.setIcon(li3);
        this.add(l3, c);

        String[] series = { "series_1", "series_2" };
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 1;
        c.weightx = 1;
        mSeries = new JComboBox<String>(series);
        this.add(mSeries, c);

        ////////////////////////////////////////////////////
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 3;
        this.add(new JSeparator(SwingConstants.HORIZONTAL), c);

        ////////////////////////////////////////////////////
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        JLabel l = new JLabel("Function:");
        l.setMinimumSize(new Dimension(200, l.getMinimumSize().height));
        l.setMaximumSize(new Dimension(200, l.getMaximumSize().height));
        l.setPreferredSize(new Dimension(200, l.getPreferredSize().height));
        l.setSize(new Dimension(200, l.getSize().height));
        ImageIcon li = new ImageIcon(getClass().getResource("icons8-lambda-16.png"));
        l.setIcon(li);
        this.add(l, c);

        AnalyseSettings.Function[] functions = { AnalyseSettings.Function.noSelection,
                AnalyseSettings.Function.calcColoc, AnalyseSettings.Function.countExosomes };
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 1;
        c.weightx = 1;
        mFunctionSelection = new JComboBox<AnalyseSettings.Function>(functions);
        this.add(mFunctionSelection, c);

        ////////////////////////////////////////////////////
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 3;
        this.add(new JSeparator(SwingConstants.HORIZONTAL), c);

        ////////////////////////////////////////////////////
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        c.gridwidth = 3;
        this.add(chSettings, c);

        ////////////////////////////////////////////////////
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 3;
        this.add(new JSeparator(SwingConstants.HORIZONTAL), c);

        ////////////////////////////////////////////////////
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        this.add(new JLabel("Filter: "), c);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 1;
        c.weightx = 1;
        this.add(filter, c);

        ////////////////////////////////////////////////////
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 3;
        this.add(new JSeparator(SwingConstants.HORIZONTAL), c);

        ////////////////////////////////////////////////////
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        this.add(new JLabel("Report settings: "), c);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 1;
        this.add(reportSettings, c);

        ////////////////////////////////////////////////////
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 3;
        this.add(new JSeparator(SwingConstants.HORIZONTAL), c);

        ////////////////////////////////////////////////////
        c.gridx = 0;
        c.gridy++;
        c.weightx = 3;
        mProgressbar.setStringPainted(true);
        mProgressbar.setString("0");
        this.add(mProgressbar, c);

        ////////////////////////////////////////////////////
        mMenu = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        // mMenu.setBackground(Color.WHITE);

        mbStart = new JButton();
        mbStart = new JButton(new ImageIcon(getClass().getResource("icons8-play-16.png")));
        mbStart.addActionListener(new java.awt.event.ActionListener() {
            // Beim Drücken des Menüpunktes wird actionPerformed aufgerufen
            public void actionPerformed(java.awt.event.ActionEvent e) {
                startAnalyse();
            }
        });
        mbStart.setText("Start");
        mMenu.add(mbStart);

        mOpenResult = new JButton();
        mOpenResult = new JButton(new ImageIcon(getClass().getResource("icons8-graph-16.png")));
        mOpenResult.addActionListener(new java.awt.event.ActionListener() {
            // Beim Drücken des Menüpunktes wird actionPerformed aufgerufen
            public void actionPerformed(java.awt.event.ActionEvent e) {
                openResultsDialog();
            }
        });
        mOpenResult.setText("Open result");
        mOpenResult.setEnabled(false);
        mMenu.add(mOpenResult);

        mCancle = new JButton();
        mCancle = new JButton(new ImageIcon(getClass().getResource("icons8-stop-16.png")));
        mCancle.addActionListener(new java.awt.event.ActionListener() {
            // Beim Drücken des Menüpunktes wird actionPerformed aufgerufen
            public void actionPerformed(java.awt.event.ActionEvent e) {
                cancleAnalyse();
            }
        });
        mCancle.setText("Cancle");
        mCancle.setEnabled(false);
        mMenu.add(mCancle);

        mClose = new JButton();
        mClose = new JButton(new ImageIcon(getClass().getResource("icons8-multiply-16.png")));
        mClose.addActionListener(new java.awt.event.ActionListener() {
            // Beim Drücken des Menüpunktes wird actionPerformed aufgerufen
            public void actionPerformed(java.awt.event.ActionEvent e) {
                dispose();
            }
        });
        mClose.setText("Close");
        mMenu.add(mClose);

        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 3;
        this.add(mMenu, c);

        ////////////////////////////////////////////////////
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 3;
        this.add(new JSeparator(SwingConstants.HORIZONTAL), c);

        ////////////////////////////////////////

        // Logo
        JLabel logo = new JLabel("(c) 2019 - 2021  SMJD", SwingConstants.RIGHT);
        ImageIcon logoIcon = new ImageIcon(getClass().getResource("logo_32.png"));
        logo.setIcon(logoIcon);

        c.gridx = 0;
        c.gridy++;
        c.weightx = 2;
        c.gridwidth = 2;
        this.add(logo, c);

        JButton about = new JButton(new ImageIcon(getClass().getResource("icons8-info-16.png")));
        about.addActionListener(new java.awt.event.ActionListener() {
            // Beim Drücken des Menüpunktes wird actionPerformed aufgerufen
            public void actionPerformed(java.awt.event.ActionEvent e) {
                JOptionPane.showMessageDialog(new JFrame(), "Exosome Analyzer v" + Version.getVersion()
                        + ".\n\nMany thanks to Melanie Schürz and Maria Jaritsch.\n\nLicensed under MIT.\nPreferably for use in non-profit research and development.\nIcons from https://icons8.de.\n\n (c) 2020 J. Danmayr",
                        "About", JOptionPane.INFORMATION_MESSAGE);

            }
        });
        about.setText("About");
        c.gridx = 2;
        c.weightx = 0;
        c.gridwidth = 1;
        this.add(about, c);

        // Pack it
        // setBackground(Color.WHITE);
        // getContentPane().setBackground(Color.WHITE);
        pack();

        // this.setAlwaysOnTop(true);
        this.setResizable(false);
        setTitle("Exosome analyzer");
    }

    public void setProgressBarMaxSize(int value, String lable) {
        mProgressbar.setMaximum(value);
        mProgressbar.setString(
                Integer.toString(value) + "/" + Integer.toString(mProgressbar.getMaximum()) + " " + lable + "");
    }

    public void setProgressBarValue(int value, String lable) {
        mProgressbar.setValue(value);
        mProgressbar.setString(
                Integer.toString(value) + "/" + Integer.toString(mProgressbar.getMaximum()) + " " + lable + "");
    }

    public void incrementProgressBarValue(String lable) {
        int value = mProgressbar.getValue() + 1;
        mProgressbar.setValue(value);
        mProgressbar.setString(
                Integer.toString(value) + "/" + Integer.toString(mProgressbar.getMaximum()) + " " + lable + "");
    }

    public void startAnalyse() {
        mbStart.setEnabled(false);
        mCancle.setEnabled(true);
        mOpenResult.setEnabled(false);
        String error = "";
        AnalyseSettings sett = new AnalyseSettings();
        sett.mInputFolder = mInputFolder.getText();
        File parentFile = new File(sett.mInputFolder);
        if (false == parentFile.exists()) {
            error = "Please select an existing input folder!\n";
        }

        sett.mOutputFolder = mOutputFolder.getText();
        if (sett.mOutputFolder.length() <= 0) {
            error = "Please select an output folder!\n";
        }
        sett.ch0.type = ((ComboItem<Pipeline.ChannelType>) chSettings.ch0.channelType.getSelectedItem()).getValue();
        sett.ch1.type = ((ComboItem<Pipeline.ChannelType>) chSettings.ch1.channelType.getSelectedItem()).getValue();
        sett.ch2.type = ((ComboItem<Pipeline.ChannelType>) chSettings.ch2.channelType.getSelectedItem()).getValue();

        int nrOfExpectedChannels = 0;
        if(sett.ch0.type != Pipeline.ChannelType.OFF){nrOfExpectedChannels++;}
        if(sett.ch1.type != Pipeline.ChannelType.OFF){nrOfExpectedChannels++;}
        if(sett.ch2.type != Pipeline.ChannelType.OFF){nrOfExpectedChannels++;}
        if(nrOfExpectedChannels == 0){
            error += "Please select at least for one channel a type!\n";
        }
        if(nrOfExpectedChannels == 3){
            error += "Three channel coloc is not supported yet! Please set at least one channel to OFF!\n";
        }

        sett.mSelectedFunction = (AnalyseSettings.Function) mFunctionSelection.getSelectedItem();



        if (sett.mSelectedFunction.equals(AnalyseSettings.Function.noSelection)) {
            error += PLEASE_SELECT_A_FUNCTION;
        }

        sett.mSelectedSeries = mSeries.getSelectedItem().toString();
        sett.ch0.mThersholdMethod = chSettings.ch0.thersholdMethod.getSelectedItem().toString();
        sett.ch1.mThersholdMethod = chSettings.ch1.thersholdMethod.getSelectedItem().toString();
        sett.ch2.mThersholdMethod = chSettings.ch2.thersholdMethod.getSelectedItem().toString();


        sett.ch0.enhanceContrast = chSettings.ch0.enchanceContrast.isSelected();
        sett.ch1.enhanceContrast = chSettings.ch1.enchanceContrast.isSelected();
        sett.ch2.enhanceContrast = chSettings.ch2.enchanceContrast.isSelected();

        try {
            sett.mMinParticleSize = Integer.parseInt(filter.mMinParticleSize.getText());
        } catch (NumberFormatException ex) {
            error += "Min particle size wrong!\n";
        }
        try {
            sett.mMaxParticleSize = Integer.parseInt(filter.mMaxParticleSize.getText());
        } catch (NumberFormatException ex) {
            error += "Max particle size wrong!\n";
        }

        try {
            sett.mMinCircularity = Integer.parseInt(filter.mMinCircularity.getText());
        } catch (NumberFormatException ex) {
            error += "Max particle size wrong!\n";
        }

        try {
            sett.minIntensity = Integer.parseInt(filter.mMinIntensity.getText());
        } catch (NumberFormatException ex) {
            error += "Max particle size wrong!\n";
        }

        try {
            sett.ch0.minThershold = (Integer) (chSettings.ch0.minTheshold.getValue());
        } catch (NumberFormatException ex) {
            error += "Min Therhold CH0 wrong!\n";
        }
        try {
            sett.ch1.minThershold = (Integer) (chSettings.ch1.minTheshold.getValue());
        } catch (NumberFormatException ex) {
            error += "Min Therhold CH1 wrong!\n";
        }
        try {
            sett.ch2.minThershold = (Integer) (chSettings.ch2.minTheshold.getValue());
        } catch (NumberFormatException ex) {
            error += "Min Therhold CH2 wrong!\n";
        }

        if (error.length() <= 0) {
            mActAnalyzer = new FileProcessor(this, sett);
            mActAnalyzer.start();
        } else {
            JOptionPane.showMessageDialog(new JFrame(), error, "Dialog", JOptionPane.WARNING_MESSAGE);
            finishedAnalyse(mNameOfLastGeneratedReportFile);
        }

        sett.mSaveDebugImages = ((ComboItem<AnalyseSettings.CotrolPicture>) reportSettings.mControlPictures
                .getSelectedItem()).getValue();
        sett.reportType = ((ComboItem<AnalyseSettings.ReportType>) reportSettings.mComboReportGenerator
                .getSelectedItem()).getValue();
        sett.mOutputFileName = reportSettings.mReportFileName.getText();
    }

    public void cancleAnalyse() {
        if (mActAnalyzer != null) {
            mCancle.setEnabled(false);
            mActAnalyzer.cancle();
            mProgressbar.setString("Canceling...");
        }
    }

    public void finishedAnalyse(String nameOfGeneratedReportFile) {
        mProgressbar.setString("Finished");
        mProgressbar.setValue(mProgressbar.getMaximum());

        mbStart.setEnabled(true);
        mCancle.setEnabled(false);

        if (nameOfGeneratedReportFile.length() > 0) {
            mOpenResult.setEnabled(true);
        }

        mNameOfLastGeneratedReportFile = nameOfGeneratedReportFile;
    }

    public void openResultsDialog() {
        try {
            Desktop.getDesktop().open(new File(mNameOfLastGeneratedReportFile));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void OpenDirectoryChooser(JTextField textfieldInput, JTextField textfieldOutput) {
        Preferences prefs = Preferences.userRoot().node(getClass().getName());
        String lastOpenFolder = prefs.get("LAST_USED_FOLDER", ".");

        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(new java.io.File(lastOpenFolder));
        chooser.setDialogTitle("select folder");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);
        int result = chooser.showOpenDialog(this);
        if (result != JFileChooser.CANCEL_OPTION) {
            String selectedPath = chooser.getSelectedFile().getAbsolutePath();
            prefs.put("LAST_USED_FOLDER", selectedPath);
            textfieldInput.setText(selectedPath);
            if (null != textfieldOutput) {
                String outputPath = selectedPath + File.separator + "results";
                textfieldOutput.setText(outputPath);
            }
        }
    }
}