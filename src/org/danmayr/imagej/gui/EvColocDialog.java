package org.danmayr.imagej.gui;

import ij.*;
import ij.process.*;
import ij.gui.*;

import javax.swing.*;

import org.danmayr.imagej.algorithm.*;
import org.danmayr.imagej.algorithm.pipelines.*;
import org.danmayr.imagej.algorithm.filters.*;

import java.awt.*;
import java.io.File;
import java.io.IOException;

///
///
///
public class EvColocDialog extends JFrame {

    private static final String PLEASE_SELECT_A_FUNCTION = "Please select a function!\n";

    private static final long serialVersionUID = 1L;

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
        private JTextField minTheshold = new JTextField("-1");
        private JComboBox channelType;
        private JComboBox thersholdMethod;
        private JCheckBox enchanceContrast;
        private JToggleButton thersholdPreview;

        public PanelChannelSettings(Container parent) {
            GridBagLayout layout = new GridBagLayout();
            setLayout(layout);
            layout.preferredLayoutSize(parent);

            GridBagConstraints c = new GridBagConstraints();
            c.insets = new Insets(5, 5, 5, 5); // top padding
            c.anchor = GridBagConstraints.WEST;

            ////////////////////////////////////////////////////
            c.fill = GridBagConstraints.HORIZONTAL;
            c.gridx = 0;
            c.gridy = 0;
            c.weightx = 0.0;
            JLabel l = new JLabel("Type:");
            ImageIcon diamter = new ImageIcon(getClass().getResource("type.png"));
            l.setIcon(diamter);
            l.setMinimumSize(new Dimension(200, l.getMinimumSize().height));
            l.setMaximumSize(new Dimension(200, l.getMaximumSize().height));
            l.setPreferredSize(new Dimension(200, l.getPreferredSize().height));
            l.setSize(new Dimension(200, l.getSize().height));
            this.add(l, c);

            ComboItem<Pipeline.ChannelType>[] channels0 = new ComboItem[4];
            channels0[0] = new ComboItem<Pipeline.ChannelType>(Pipeline.ChannelType.OFF, "OFF");
            channels0[1] = new ComboItem<Pipeline.ChannelType>(Pipeline.ChannelType.GFP, "GFP");
            channels0[2] = new ComboItem<Pipeline.ChannelType>(Pipeline.ChannelType.CY3, "CY3");
            channels0[3] = new ComboItem<Pipeline.ChannelType>(Pipeline.ChannelType.NEGATIVE_CONTROL,
                    "Negative Control");

            c.fill = GridBagConstraints.HORIZONTAL;
            c.gridx = 1;
            c.weightx = 1;
            channelType = new JComboBox<ComboItem<Pipeline.ChannelType>>(channels0);
            this.add(channelType, c);

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
            ImageIcon diamter1 = new ImageIcon(getClass().getResource("thershold.png"));
            l1.setIcon(diamter1);
            this.add(l1, c);
            

            String[] thersholdAlgo = { "Li", "MaxEntropy" };
            c.fill = GridBagConstraints.HORIZONTAL;
            c.gridx = 1;
            c.weightx = 1;
            thersholdMethod = new JComboBox<String>(thersholdAlgo);
            this.add(thersholdMethod, c);

            ////////////////////////////////////////////////////
            c.fill = GridBagConstraints.HORIZONTAL;
            c.gridx = 0;
            c.gridy++;
            c.weightx = 1;
            c.weightx = 0.0;
            JLabel l2 = new JLabel("Man. thersh. [-1-65535]:");
            l2.setMinimumSize(new Dimension(200, l.getMinimumSize().height));
            l2.setMaximumSize(new Dimension(200, l.getMaximumSize().height));
            l2.setPreferredSize(new Dimension(200, l.getPreferredSize().height));
            l2.setSize(new Dimension(200, l.getSize().height));
            ImageIcon diamter2 = new ImageIcon(getClass().getResource("edge.png"));
            l2.setIcon(diamter2);
            this.add(l2, c);

            c.fill = GridBagConstraints.HORIZONTAL;
            c.gridx = 1;
            c.weightx = 1;
            this.add(minTheshold, c);

            c.fill = GridBagConstraints.HORIZONTAL;
            c.gridx = 2;
            c.weightx = 0;
            thersholdPreview = new JToggleButton(new ImageIcon(getClass().getResource("preview.png")));
            thersholdPreview.addActionListener(new java.awt.event.ActionListener() {
                // Beim Drücken des Menüpunktes wird actionPerformed aufgerufen
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    if(thersholdPreview.isSelected()){
                        startPreview();
                        refreshPreview();
                    }else{
                        endPreview();
                    }
                }
            });
            this.add(thersholdPreview, c);

            ////////////////////////////////////////////////////
            c.fill = GridBagConstraints.HORIZONTAL;
            c.gridx = 1;
            c.gridy++;
            c.weightx = 2;
            c.weightx = 1;
            enchanceContrast = new JCheckBox("Enhance contrast");
            enchanceContrast.setContentAreaFilled(false);
            this.add(enchanceContrast, c);
        }

        private ImagePlus mOriginalImage;
        private ImagePlus mPreviewImage;


        public void startPreview(){
            String[] imageTitles = WindowManager.getImageTitles();
            if (imageTitles.length > 0) {
               ImagePlus image = IJ.getImage();//WindowManager.getImage(imageTitles[0]);
               mPreviewImage = image;
               mOriginalImage = Filter.duplicateImage(image);

            }else{
               JOptionPane.showMessageDialog(new JFrame(), "Open an image to apply the preview on it!", "Dialog", JOptionPane.WARNING_MESSAGE);
            }
        }


        public void endPreview(){
            mPreviewImage.setImage(mOriginalImage);
        }

        public void refreshPreview()
        {
             if (mPreviewImage != null) {

                int lowThershold = -1;
                try {
                    lowThershold = Integer.parseInt(minTheshold.getText());
                } catch (NumberFormatException ex) {
                }
                double[] th = new double[2];
                Pipeline.preFilterSetColocPreview(mPreviewImage,enchanceContrast.isSelected(), thersholdMethod.getSelectedItem().toString(),lowThershold,65535,th);
             }else{
                JOptionPane.showMessageDialog(new JFrame(), "Open an image to apply the preview on it!", "Dialog", JOptionPane.WARNING_MESSAGE);
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
            c.insets = new Insets(5, 5, 5, 5); // top padding
            c.anchor = GridBagConstraints.WEST;

            ////////////////////////////////////////////////////
            c.fill = GridBagConstraints.HORIZONTAL;
            c.gridx = 0;
            c.gridy = 0;
            c.weightx = 0;
            JLabel l = new JLabel("Particle size [0-9999]:");
            ImageIcon diamter = new ImageIcon(getClass().getResource("diameter.png"));
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
            ImageIcon image = new ImageIcon(getClass().getResource("polygon.png"));
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
            ImageIcon image1 = new ImageIcon(getClass().getResource("grayscale.png"));
            JLabel intensity = new JLabel("Min Intensity [0-255]:");
            intensity.setIcon(image1);
            this.add(intensity, c);

            c.fill = GridBagConstraints.HORIZONTAL;
            c.gridx = 1;
            c.weightx = 1;
            this.add(mMinIntensity, c);

        }
    }

    PanelChannelSettings ch0Settings = new PanelChannelSettings(this);
    PanelChannelSettings ch1Settings = new PanelChannelSettings(this);
    PanelFilter filter = new PanelFilter(this);

    ///
    /// Constructor
    ///
    public EvColocDialog() {

        GridBagLayout layout = new GridBagLayout();
        setLayout(layout);

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5); // top padding

        ////////////////////////////////////////////////////
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 0;
        JLabel l = new JLabel("Function:");
        l.setMinimumSize(new Dimension(200, l.getMinimumSize().height));
        l.setMaximumSize(new Dimension(200, l.getMaximumSize().height));
        l.setPreferredSize(new Dimension(200, l.getPreferredSize().height));
        l.setSize(new Dimension(200, l.getSize().height));
        this.add(l, c);

        AnalyseSettings.Function[] functions = { AnalyseSettings.Function.noSelection,
                AnalyseSettings.Function.calcColoc, AnalyseSettings.Function.countExosomes };
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 1;
        c.gridy = 0;
        c.weightx = 1;
        mFunctionSelection = new JComboBox<AnalyseSettings.Function>(functions);
        this.add(mFunctionSelection, c);

        ////////////////////////////////////////////////////
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        this.add(new JLabel("Input folder:"), c);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 1;
        c.weightx = 1;
        this.add(mInputFolder, c);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 2;
        c.weightx = 0;
        c.weightx = 0;
        mbInputFolder = new JButton(new ImageIcon(getClass().getResource("open.png")));
        mbInputFolder.addActionListener(new java.awt.event.ActionListener() {
            // Beim Drücken des Menüpunktes wird actionPerformed aufgerufen
            public void actionPerformed(java.awt.event.ActionEvent e) {
                OpenDirectoryChooser(mInputFolder, mOutputFolder);
            }
        });
        this.add(mbInputFolder, c);

        ////////////////////////////////////////////////////
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        this.add(new JLabel("Output folder:"), c);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 1;
        c.weightx = 1;
        this.add(mOutputFolder, c);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 2;
        c.weightx = 0;
        mbOutputFolder = new JButton(new ImageIcon(getClass().getResource("open.png")));
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
        this.add(new JLabel("Series to import:"), c);

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
        this.add(new JLabel("Channel 0: "), c);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 1;
        c.weightx = 1;
        this.add(ch0Settings, c);

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
        this.add(new JLabel("Channel 1: "), c);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 1;
        this.add(ch1Settings, c);

        ////////////////////////////////////////////////////
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 3;
        this.add(new JSeparator(SwingConstants.HORIZONTAL), c);

        ////////////////////////////////////////////////////
        mMenu = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        // mMenu.setBackground(Color.WHITE);

        mbStart = new JButton();
        mbStart = new JButton(new ImageIcon(getClass().getResource("start.png")));
        mbStart.addActionListener(new java.awt.event.ActionListener() {
            // Beim Drücken des Menüpunktes wird actionPerformed aufgerufen
            public void actionPerformed(java.awt.event.ActionEvent e) {
                startAnalyse();
            }
        });
        mbStart.setText("Start");
        mMenu.add(mbStart);

        mOpenResult = new JButton();
        mOpenResult = new JButton(new ImageIcon(getClass().getResource("normal.png")));
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
        mCancle = new JButton(new ImageIcon(getClass().getResource("stop.png")));
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
        mClose = new JButton(new ImageIcon(getClass().getResource("close.png")));
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
        c.gridx = 0;
        c.gridy++;
        c.weightx = 3;
        mProgressbar.setStringPainted(true);
        mProgressbar.setString("0");
        this.add(mProgressbar, c);

        // Logo
        c.gridx = 0;
        c.gridy++;
        c.weightx = 2;
        this.add(new JLabel(new ImageIcon(getClass().getResource("logo.png"))), c);

        c.gridx = 0;
        c.gridy++;
        c.weightx = 3;
        this.add(new JLabel("(c) 2019 - 2020  SMMJJD  | v2.1.0 - alpha", SwingConstants.RIGHT), c);

        // Pack it
        // setBackground(Color.WHITE);
        // getContentPane().setBackground(Color.WHITE);
        pack();

        this.setAlwaysOnTop(true);
        this.setResizable(false);
        setTitle("EV analyzer");
    }

    public void setProgressBarMaxSize(int value) {
        mProgressbar.setMaximum(value);
        mProgressbar.setString(Integer.toString(0) + "/" + Integer.toString(mProgressbar.getMaximum()));
    }

    public void setProgressBarValue(int value) {
        mProgressbar.setValue(value);
        mProgressbar.setString(Integer.toString(value) + "/" + Integer.toString(mProgressbar.getMaximum()));
    }

    public void incrementProgressBarValue() {
        int value = mProgressbar.getValue() + 1;
        mProgressbar.setValue(value);
        mProgressbar.setString(Integer.toString(value) + "/" + Integer.toString(mProgressbar.getMaximum()));
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
        sett.ch0.type = ((ComboItem<Pipeline.ChannelType>) ch0Settings.channelType.getSelectedItem()).getValue();
        sett.ch1.type = ((ComboItem<Pipeline.ChannelType>) ch1Settings.channelType.getSelectedItem()).getValue();

        sett.mSelectedFunction = (AnalyseSettings.Function) mFunctionSelection.getSelectedItem();

        if (sett.mSelectedFunction.equals(AnalyseSettings.Function.noSelection)) {
            error += PLEASE_SELECT_A_FUNCTION;
        }

        sett.mSelectedSeries = mSeries.getSelectedItem().toString();
        sett.ch0.mThersholdMethod = ch0Settings.thersholdMethod.getSelectedItem().toString();
        sett.ch1.mThersholdMethod = ch1Settings.thersholdMethod.getSelectedItem().toString();

        sett.ch0.enhanceContrast = ch0Settings.enchanceContrast.isSelected();
        sett.ch1.enhanceContrast = ch1Settings.enchanceContrast.isSelected();
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
            sett.ch0.minThershold = Integer.parseInt(ch0Settings.minTheshold.getText());
        } catch (NumberFormatException ex) {
            error += "Min Therhold CH0 wrong!\n";
        }
        try {
            sett.ch1.minThershold = Integer.parseInt(ch0Settings.minTheshold.getText());
        } catch (NumberFormatException ex) {
            error += "Min Therhold CH1 wrong!\n";
        }

        if (error.length() <= 0) {
            mActAnalyzer = new FileProcessor(this, sett);
            mActAnalyzer.start();
        } else {
            JOptionPane.showMessageDialog(new JFrame(), error, "Dialog", JOptionPane.WARNING_MESSAGE);
            finishedAnalyse();
        }
    }

    public void cancleAnalyse() {
        if (mActAnalyzer != null) {
            mCancle.setEnabled(false);
            mActAnalyzer.cancle();
            mProgressbar.setString("Canceling...");
        }
    }

    public void finishedAnalyse() {
        mbStart.setEnabled(true);
        mOpenResult.setEnabled(true);
        mCancle.setEnabled(false);
    }

    public void openResultsDialog() {
        try {
            Desktop.getDesktop().open(new File(mOutputFolder.getText() + File.separator + "result.xlsx"));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void OpenDirectoryChooser(JTextField textfieldInput, JTextField textfieldOutput) {
        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(new java.io.File("."));
        chooser.setDialogTitle("select folder");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);
        int result = chooser.showOpenDialog(this);
        if (result != JFileChooser.CANCEL_OPTION) {
            String selectedPath = chooser.getSelectedFile().getAbsolutePath();
            textfieldInput.setText(selectedPath);
            if (null != textfieldOutput) {
                String outputPath = selectedPath + File.separator + "results";
                textfieldOutput.setText(outputPath);
            }
        }
    }
}