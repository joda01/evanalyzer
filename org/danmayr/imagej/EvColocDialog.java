package org.danmayr.imagej;

import javax.swing.*;
import java.awt.*;

///
///
///
public class EvColocDialog extends JFrame {

    private static final long serialVersionUID = 1L;

    private JTextField mInputFolder = new JTextField(30);
    private JTextField mOutputFolder = new JTextField(30);
    private JButton mbInputFolder;
    private JButton mbOutputFolder;
    private JButton mbStart;
    private JButton mCancle;
    private JProgressBar mProgressbar = new JProgressBar();
    private JComboBox mRedChannel;
    private JComboBox mThersholdMethod;
    private Analyzer mActAnalyzer = null;

    ///
    /// Constructor
    ///
    public EvColocDialog() {

        setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(2, 2, 2, 5); // top padding

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
                OpenDirectoryChooser(mInputFolder);
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
                OpenDirectoryChooser(mOutputFolder);
            }
        });
        this.add(mbOutputFolder, c);

        ////////////////////////////////////////////////////
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 3;
        this.add(new JLabel("Nr. of channels:"), c);

        String[] nrOfChanneld = { "2.0", "1.0" };
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 1;
        c.gridy = 3;
        this.add(new JComboBox<String>(nrOfChanneld), c);

        ////////////////////////////////////////////////////
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 4;
        this.add(new JLabel("Green channel:"), c);

        String[] redChannel = { "0", "1" };
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 1;
        c.gridy = 4;
        mRedChannel = new JComboBox<String>(redChannel);
        this.add(mRedChannel, c);

        ////////////////////////////////////////////////////
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 5;
        this.add(new JLabel("Thersholding:"), c);

        String[] thersholdAlgo = { "Li", "MaxEntropy" };
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 1;
        c.gridy = 5;
        mThersholdMethod = new JComboBox<String>(thersholdAlgo);
        this.add(mThersholdMethod, c);

        ////////////////////////////////////////////////////
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 1;
        c.gridy = 6;
        c.gridwidth = 2;
        this.add(new JCheckBox("Enhance contrast for C=0"), c);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 1;
        c.gridy = 7;
        c.gridwidth = 2;
        this.add(new JCheckBox("Enhance contrast for C=1"), c);

        ////////////////////////////////////////////////////
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 8;
        c.gridwidth = 1;
        this.add(new JLabel("Min particle size (Square Pixel):"), c);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 1;
        c.gridy = 8;
        this.add(new JTextField(5), c);

        ////////////////////////////////////////////////////
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 9;
        this.add(new JLabel("Max particle size (Square Pixel):"), c);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 1;
        c.gridy = 9;
        this.add(new JTextField(999999999), c);

        ////////////////////////////////////////////////////
        JPanel menu = new JPanel(new FlowLayout());

        mbStart = new JButton();
        mbStart = new JButton(new ImageIcon(getClass().getResource("open.png")));
        mbStart.addActionListener(new java.awt.event.ActionListener() {
            // Beim Drücken des Menüpunktes wird actionPerformed aufgerufen
            public void actionPerformed(java.awt.event.ActionEvent e) {
                startAnalyse();
            }
        });
        mbStart.setText("Start");
        menu.add(mbStart);

        mCancle = new JButton();
        mCancle = new JButton(new ImageIcon(getClass().getResource("open.png")));
        mCancle.addActionListener(new java.awt.event.ActionListener() {
            // Beim Drücken des Menüpunktes wird actionPerformed aufgerufen
            public void actionPerformed(java.awt.event.ActionEvent e) {
                cancleAnalyse();
            }
        });
        mCancle.setText("Cancle");
        menu.add(mCancle);

        c.gridx = 0;
        c.gridy = 10;
        c.gridwidth = 2;
        this.add(menu, c);

        ////////////////////////////////////////////////////
        c.gridx = 0;
        c.gridy = 11;
        c.gridwidth = 3;
        mProgressbar.setStringPainted(true);
        mProgressbar.setString("0");
        this.add(mProgressbar, c);

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
        AnalyseSettings sett = new AnalyseSettings();
        sett.mInputFolder = mInputFolder.getText();
        sett.mOutputFolder = mOutputFolder.getText();
        sett.mRedChannel = Integer.parseInt(mRedChannel.getSelectedItem().toString());
        sett.mThersholdMethod = mThersholdMethod.getSelectedItem().toString();

        mActAnalyzer = new Analyzer(this, sett);
        mActAnalyzer.start();
    }

    public void cancleAnalyse() {
        if (mActAnalyzer != null) {
            mActAnalyzer.cancle();
            mProgressbar.setString("Canceling...");
        }
    }

    public void OpenDirectoryChooser(JTextField textfield) {
        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(new java.io.File("."));
        chooser.setDialogTitle("select folder");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);
        int result = chooser.showOpenDialog(this);
        if (result != JFileChooser.CANCEL_OPTION) {
            textfield.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }
}