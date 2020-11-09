package org.danmayr.imagej.gui;

import javax.swing.*;

import org.danmayr.imagej.algorithm.*;

import java.awt.*;
import java.io.File;
import java.io.IOException;

///
///
///
public class EvColocDialog extends JFrame {

    private static final long serialVersionUID = 1L;

    private JTextField mInputFolder = new JTextField(30);
    private JTextField mNegativeControls = new JTextField(30);
    private JTextField mOutputFolder = new JTextField(30);
    private JTextField mMinParticleSize = new JTextField("5");
    private JTextField mMaxParticleSize = new JTextField("999999999");
    private JButton mbInputFolder;
    private JButton mbNegativeControls;
    private JButton mbOutputFolder;
    private JButton mbStart;
    private JButton mCancle;
    private JButton mClose;
    private JButton mOpenResult;
    private JProgressBar mProgressbar = new JProgressBar();
    private JComboBox mGreenChannel;
    private JComboBox mThersholdMethod;
    private JComboBox mSeries;
    private ImageProcessor mActAnalyzer = null;
    private JCheckBox mEnhanceContrastRed;
    private JCheckBox mEnhanceContrastGreen;
    private JPanel mMenu;

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
        this.add(new JLabel("Input folder:"), c);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 1;
        c.gridy = 0;
        this.add(mInputFolder, c);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 2;
        c.gridy = 0;
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
        c.gridy = 0;
        this.add(new JLabel("Negative controls:"), c);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 1;
        c.gridy = 0;
        this.add(mNegativeControls, c);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 2;
        c.gridy = 0;
        mbNegativeControls = new JButton(new ImageIcon(getClass().getResource("open.png")));
        mbNegativeControls.addActionListener(new java.awt.event.ActionListener() {
            // Beim Drücken des Menüpunktes wird actionPerformed aufgerufen
            public void actionPerformed(java.awt.event.ActionEvent e) {
                OpenDirectoryChooser(mNegativeControls, null);
            }
        });
        this.add(mbInputFolder, c);


        ////////////////////////////////////////////////////
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 1;
        this.add(new JLabel("Output folder:"), c);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 1;
        c.gridy = 1;
        this.add(mOutputFolder, c);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 2;
        c.gridy = 1;
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
        c.gridy = 4;
        this.add(new JLabel("Series to import:"), c);

        String[] series = { "series_1", "series_2" };
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 1;
        c.gridy = 4;
        mSeries = new JComboBox<String>(series);
        this.add(mSeries, c);

        ////////////////////////////////////////////////////
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 5;
        this.add(new JLabel("Green channel:"), c);

        String[] channels = { "0", "1" };
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 1;
        c.gridy = 5;
        mGreenChannel = new JComboBox<String>(channels);
        this.add(mGreenChannel, c);

        ////////////////////////////////////////////////////
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 6;
        this.add(new JLabel("Thersholding:"), c);

        String[] thersholdAlgo = { "Li", "MaxEntropy" };
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 1;
        c.gridy = 6;
        mThersholdMethod = new JComboBox<String>(thersholdAlgo);
        this.add(mThersholdMethod, c);

        ////////////////////////////////////////////////////
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 1;
        c.gridy = 7;
        c.gridwidth = 2;
        mEnhanceContrastGreen = new JCheckBox("Enhance contrast for green channel");
        mEnhanceContrastGreen.setContentAreaFilled(false);
        this.add(mEnhanceContrastGreen, c);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 1;
        c.gridy = 8;
        c.gridwidth = 2;
        mEnhanceContrastRed = new JCheckBox("Enhance contrast for red channel");
        mEnhanceContrastRed.setContentAreaFilled(false);
        this.add(mEnhanceContrastRed, c);

        ////////////////////////////////////////////////////
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 9;
        c.gridwidth = 1;
        this.add(new JLabel("Min particle size (Square Pixel):"), c);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 1;
        c.gridy = 9;
        this.add(mMinParticleSize, c);

        ////////////////////////////////////////////////////
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 10;
        this.add(new JLabel("Max particle size (Square Pixel):"), c);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 1;
        c.gridy = 10;
        this.add(mMaxParticleSize, c);

        ////////////////////////////////////////////////////
        mMenu = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        mMenu.setBackground(Color.WHITE);

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
        c.gridy = 11;
        c.gridwidth = 3;
        this.add(mMenu, c);

        ////////////////////////////////////////////////////
        c.gridx = 0;
        c.gridy = 12;
        c.gridwidth = 3;
        mProgressbar.setStringPainted(true);
        mProgressbar.setString("0");
        this.add(mProgressbar, c);

        // Logo
        c.gridx = 0;
        c.gridy = 13;
        c.gridwidth = 2;
        this.add(new JLabel(new ImageIcon(getClass().getResource("logo.jpg"))), c);

        c.gridx = 0;
        c.gridy = 14;
        c.gridwidth = 3;
        this.add(new JLabel("(c) 2019 - 2020  MSJDMJ  | v1.2.0", SwingConstants.RIGHT), c);

        // Pack it
        setBackground(Color.WHITE);
        getContentPane().setBackground(Color.WHITE);
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
        try {
            sett.mGreenChannel = Integer.parseInt(mGreenChannel.getSelectedItem().toString());
        } catch (NumberFormatException ex) {
            error = "Wrong selected channel!\n";
        }

        sett.mNegativeControl = mNegativeControls.getText();
    
        sett.mSelectedSeries = mSeries.getSelectedItem().toString();
        sett.mThersholdMethod = mThersholdMethod.getSelectedItem().toString();
        sett.mEnhanceContrastForGreen = mEnhanceContrastGreen.isSelected();
        sett.mEnhanceContrastForRed = mEnhanceContrastRed.isSelected();
        try {
            sett.mMinParticleSize = Integer.parseInt(mMinParticleSize.getText());
        } catch (NumberFormatException ex) {
            error += "Min particle size wrong!\n";
        }
        try {
            sett.mMaxParticleSize = Integer.parseInt(mMaxParticleSize.getText());
        } catch (NumberFormatException ex) {
            error += "Max particle size wrong!\n";
        }

        if (error.length() <= 0) {
            mActAnalyzer = new ImageProcessor(this, sett);
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
            Desktop.getDesktop()
                    .open(new File(mOutputFolder.getText() + File.separator + "statistic_all_over_final.xlsx"));
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