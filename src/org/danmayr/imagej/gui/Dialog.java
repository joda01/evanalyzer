package org.danmayr.imagej.gui;

import java.awt.Color;
import java.awt.Container;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.TreeMap;
import java.util.Vector;
import java.util.prefs.Preferences;
import java.awt.BorderLayout;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.danmayr.imagej.EVAnalyzer;
import org.danmayr.imagej.Version;
import org.danmayr.imagej.algorithm.AnalyseSettings;
import org.danmayr.imagej.algorithm.ChannelSettings;
import org.danmayr.imagej.algorithm.FileProcessor;
import org.danmayr.imagej.algorithm.filters.Filter;
import org.danmayr.imagej.algorithm.pipelines.Pipeline;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.MaskFormatter;
import java.awt.*;

import ij.*;
import ij.gui.*;
import ij.process.*;
import java.awt.event.*;

///
///
///
public class Dialog extends JFrame {

    private static final String PLEASE_SELECT_A_FUNCTION = "Please select a function!\n";
    private static final int NUMBEROFCHANNELSETTINGS = 5;

    private static final long serialVersionUID = 1L;

    private String mNameOfLastGeneratedReportFile = new String("");

    private JMenuBar mMenuBar = new JMenuBar();

    private JTextField mInputFolder = new JTextField(30);
    private JTextField mOutputFolder = new JTextField(30);

    private JButton mbInputFolder;
    private JButton mbOutputFolder;
    private JButton mbStart;
    private JButton mCancle;

    private JButton mClose;
    private JButton mOpenResult;
    private JProgressBar mProgressbar = new JProgressBar();
    SpinnerModel modelMicrometer = new SpinnerNumberModel(1, // initial value
            0.001, // min
            1, // max
            0.001); // step
    private JSpinner mPixelInMicrometer = new JSpinner(modelMicrometer);

    SpinnerModel modelColocFactor = new SpinnerNumberModel((double) 1.0, // initial value
            (double) 0, // min
            (double) 100.0, // max
            (double) 1.0); // step
    private JSpinner mMinColocFactor = new JSpinner(modelColocFactor);
    private JComboBox mFunctionSelection;
    private JComboBox mSeries;
    private FileProcessor mActAnalyzer = null;
    private JPanel mMenu;
    private JLabel mLNewsTicker = new JLabel("Science news ...");

    class PanelChannelSettings extends JPanel {

        private JToggleButton thersholdPreview;
        private JToggleButton bLockMinCircularity = new JToggleButton();
        private JToggleButton bLockZprojection = new JToggleButton();
        private JToggleButton bLockMarginCrop = new JToggleButton();
        private JToggleButton bLockParticleSize = new JToggleButton();
        private JToggleButton bLockSnapArea = new JToggleButton();

        class ChannelElements {
            int mNr = 0;

            SpinnerModel model = new SpinnerNumberModel(-1, // initial value
                    -1, // min
                    65535, // max
                    1); // step
            private JSpinner minTheshold = new JSpinner(model);

            SpinnerModel model2 = new SpinnerNumberModel(0, // initial value
                    0, // min
                    65535, // max
                    0.1); // step
            private JSpinner snapAreaSize = new JSpinner(model2);
            private JComboBox channelType;
            private JComboBox channel;
            private JComboBox thersholdMethod;
            private JCheckBox enchanceContrast;
            private JComboBox mZProjection;
            private JComboBox mPreProcesssingSteps;

            SpinnerModel modelMin = new SpinnerNumberModel(5, // initial value
                    0, // min
                    999999, // max
                    0.1); // step
            private JSpinner mMinParticleSize = new JSpinner(modelMin);
            SpinnerModel modelMax = new SpinnerNumberModel(999999, // initial value
                    0, // min
                    999999, // max
                    0.1); // step
            private JSpinner mMaxParticleSize = new JSpinner(modelMax);

            SpinnerModel modelCirc = new SpinnerNumberModel(0, // initial value
                    0, // min
                    1, // max
                    0.01); // step
            private JSpinner minCirculartiy = new JSpinner(modelCirc);

            SpinnerModel modelCrop = new SpinnerNumberModel(0, // initial value
                    0, // min
                    65535, // max
                    0.1); // step
            private JSpinner marginToCrop = new JSpinner(modelCrop);

            ///
            ///
            ///
            public ChannelSettings getChannelSettings(AnalyseSettings sett) {

                ChannelSettings chSet = new ChannelSettings(sett);
                chSet.mChannelNr = channel.getSelectedIndex() - 1;
                chSet.mChannelIndex = this.mNr;
                chSet.type = (Pipeline.ChannelType) channelType.getSelectedItem();
                chSet.mThersholdMethod = (AutoThresholder.Method) thersholdMethod.getSelectedItem();
                chSet.enhanceContrast = false;
                chSet.maxThershold = 65535;
                chSet.ZProjector = mZProjection.getSelectedItem().toString();
                chSet.setSnapAreaSizeDoublw((Double) snapAreaSize.getValue());

                try {
                    chSet.minThershold = (Integer) (minTheshold.getValue());
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(new JFrame(), "Min thershold wrong!", "Dialog",
                            JOptionPane.WARNING_MESSAGE);
                }
                chSet.setMarginCropDouble((Double) marginToCrop.getValue());
                chSet.preProcessing.clear();
                chSet.preProcessing.add((ChannelSettings.PreProcessingStep) mPreProcesssingSteps.getSelectedItem());

                try {
                    chSet.setMinCircularityDouble((Double) (minCirculartiy.getValue()));
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(new JFrame(), "Min circulartiy wrong!", "Dialog",
                            JOptionPane.WARNING_MESSAGE);
                }

                try {
                    chSet.setMinParticleSizeDouble((Double) mMinParticleSize.getValue());
                    chSet.setMaxParticleSizeDouble((Double) mMaxParticleSize.getValue());
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(new JFrame(), "Particle Size wrong", "Dialog",
                            JOptionPane.WARNING_MESSAGE);
                }

                return chSet;
            }

            public void loadChannelSettings(ChannelSettings set) {

                bLockSnapArea.setSelected(false);
                bLockMarginCrop.setSelected(false);
                bLockMinCircularity.setSelected(false);
                bLockParticleSize.setSelected(false);
                bLockSnapArea.setSelected(false);
                bLockZprojection.setSelected(false);

                channel.setSelectedIndex(set.mChannelNr + 1);
                channelType.setSelectedItem(set.type);
                thersholdMethod.setSelectedItem(set.mThersholdMethod);
                minTheshold.setValue(set.minThershold);
                mZProjection.setSelectedItem(set.ZProjector);
                minCirculartiy.setValue(set.getMinCircularityDouble());
                mPreProcesssingSteps.setSelectedItem(set.preProcessing.get(0));
                marginToCrop.setValue(set.getMarginCropDouble());

                mMaxParticleSize.setValue(set.getMaxParticleSizeDouble());
                mMinParticleSize.setValue(set.getMinParticleSizeDouble());
                snapAreaSize.setValue(set.getSnapAreaSizeDouble());
            }

            //
            ///
            ///
            public ChannelElements(JPanel panel, GridBagConstraints c, int gridX, int gridY, int chNr) {

                this.mNr = chNr;
                c.fill = GridBagConstraints.HORIZONTAL;
                c.gridx = gridX;
                c.gridy = gridY;
                c.weightx = 1;
                c.gridwidth = 1;
                // panel.add(new JLabel("Layer " + Integer.toString(chNr)), c);
                panel.add(new JLabel(""), c);

                ////////////////////////////////////////////////////
                String[] channelSel = { "OFF", "C=0", "C=1", "C=2", "C=3", "C=4", "C=5", "C=6", "C=7", "C=8", "C=9" };
                c.fill = GridBagConstraints.HORIZONTAL;
                c.gridy++;
                channel = new JComboBox<String>(channelSel);
                channel.addItemListener(new ItemListener() {
                    @Override
                    public void itemStateChanged(ItemEvent e) {
                        if (channel.getSelectedItem().toString() == "OFF") {
                            thersholdMethod.setEnabled(false);
                            minTheshold.setEnabled(false);
                            channelType.setEnabled(false);
                            mZProjection.setEnabled(false);
                            minCirculartiy.setEnabled(false);
                            minCirculartiy.setEnabled(false);
                            mPreProcesssingSteps.setEnabled(false);
                            marginToCrop.setEnabled(false);
                            mMinParticleSize.setEnabled(false);
                            mMaxParticleSize.setEnabled(false);
                            snapAreaSize.setEnabled(false);
                        } else {
                            thersholdMethod.setEnabled(true);
                            minTheshold.setEnabled(true);
                            channelType.setEnabled(true);
                            mZProjection.setEnabled(true);
                            minCirculartiy.setEnabled(true);
                            minCirculartiy.setEnabled(true);
                            mPreProcesssingSteps.setEnabled(true);
                            marginToCrop.setEnabled(true);
                            mMinParticleSize.setEnabled(true);
                            mMaxParticleSize.setEnabled(true);
                            snapAreaSize.setEnabled(true);
                        }
                        refreshPreview();
                    }
                });
                channel.setSelectedIndex(0);
                panel.add(channel, c);

                ////////////////////////////////////////////////////
                Pipeline.ChannelType[] channels0 = { Pipeline.ChannelType.EV_DAPI, Pipeline.ChannelType.EV_GFP,
                        Pipeline.ChannelType.EV_CY3, Pipeline.ChannelType.EV_CY5, Pipeline.ChannelType.EV_CY7,
                        Pipeline.ChannelType.EV_CY3FCY5, Pipeline.ChannelType.CELL_BRIGHTFIELD,
                        Pipeline.ChannelType.CELL_FLUORESCENCE, Pipeline.ChannelType.NUCLEUS,
                        Pipeline.ChannelType.NEGATIVE_CONTROL, Pipeline.ChannelType.BACKGROUND,
                        Pipeline.ChannelType.TETRASPECK_BEAD };

                c.fill = GridBagConstraints.HORIZONTAL;
                c.gridy++;
                channelType = new JComboBox<Pipeline.ChannelType>(channels0);
                channelType.addItemListener(new ItemListener() {
                    @Override
                    public void itemStateChanged(ItemEvent e) {
                        Pipeline.ChannelType type = (Pipeline.ChannelType) channelType.getSelectedItem();
                        if (Pipeline.ChannelType.BACKGROUND == type) {
                            thersholdMethod.setVisible(false);
                            minTheshold.setVisible(false);
                        } else {
                            thersholdMethod.setVisible(true);
                            minTheshold.setVisible(true);
                        }
                    }
                });

                panel.add(channelType, c);

                ////////////////////////////////////////////////////
                AutoThresholder.Method[] thersholds = {AutoThresholder.Method.Default, AutoThresholder.Method.Li, AutoThresholder.Method.MaxEntropy,
                        AutoThresholder.Method.Moments, AutoThresholder.Method.Otsu, AutoThresholder.Method.MinError,
                        AutoThresholder.Method.Triangle };
                c.fill = GridBagConstraints.HORIZONTAL;
                c.gridy++;
                thersholdMethod = new JComboBox<AutoThresholder.Method>(thersholds);
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

                c.gridy++;
                minCirculartiy.addChangeListener(new ChangeListener() {
                    @Override
                    public void stateChanged(ChangeEvent e) {
                        refreshPreview();
                        if (bLockMinCircularity.isSelected()) {
                            syncMinCircularitySetting((Double) ((JSpinner) e.getSource()).getValue(), mNr);
                        }
                    }
                });
                panel.add(minCirculartiy, c);

                ////////////////////////////////////////////////////

                c.gridy++;
                JPanel sizePanel = new JPanel(new GridLayout(0, 2));
                sizePanel.add(mMinParticleSize);
                sizePanel.add(mMaxParticleSize);
                mMinParticleSize.addChangeListener(new ChangeListener() {
                    @Override
                    public void stateChanged(ChangeEvent e) {
                        if (bLockParticleSize.isSelected()) {
                            syncParticleSizeMin((Double) ((JSpinner) e.getSource()).getValue(), mNr);
                        }
                    }
                });
                mMaxParticleSize.addChangeListener(new ChangeListener() {
                    @Override
                    public void stateChanged(ChangeEvent e) {
                        if (bLockParticleSize.isSelected()) {
                            syncParticleSizeMax((Double) ((JSpinner) e.getSource()).getValue(), mNr);
                        }
                    }
                });
                panel.add(sizePanel, c);

                ////////////////////////////////////////////////////
                snapAreaSize.addChangeListener(new ChangeListener() {
                    @Override
                    public void stateChanged(ChangeEvent e) {
                        if (bLockSnapArea.isSelected()) {
                            syncSnapAreaSize((Double) ((JSpinner) e.getSource()).getValue(), mNr);
                        }
                    }
                });
                c.gridy++;
                panel.add(snapAreaSize, c);

                ////////////////////////////////////////////////////
                String[] zProjection = { "OFF", "max", "min", "avg" };
                c.fill = GridBagConstraints.HORIZONTAL;
                c.gridy++;
                mZProjection = new JComboBox<String>(zProjection);
                mZProjection.addItemListener(new ItemListener() {
                    @Override
                    public void itemStateChanged(ItemEvent e) {
                        if (bLockZprojection.isSelected()) {
                            syncZProjection(((JComboBox) e.getSource()).getSelectedIndex(), mNr);
                        }
                    }
                });

                panel.add(mZProjection, c);

                ////////////////////////////////////////////////////
                marginToCrop.addChangeListener(new ChangeListener() {
                    @Override
                    public void stateChanged(ChangeEvent e) {
                        refreshPreview();
                        if (bLockMarginCrop.isSelected()) {
                            syncMarginCrop((Double) ((JSpinner) e.getSource()).getValue(), mNr);
                        }
                    }
                });
                c.gridy++;
                panel.add(marginToCrop, c);

                ////////////////////////////////////////////////////
                ChannelSettings.PreProcessingStep[] preprocessingSteps = { ChannelSettings.PreProcessingStep.None,
                        ChannelSettings.PreProcessingStep.EdgeDetection,
                        ChannelSettings.PreProcessingStep.EnhanceContrast };

                c.gridy++;
                mPreProcesssingSteps = new JComboBox<ChannelSettings.PreProcessingStep>(preprocessingSteps);
                panel.add(mPreProcesssingSteps, c);

                ////////////////////////////////////////////////////
                /*
                 * c.fill = GridBagConstraints.HORIZONTAL; c.gridy++; enchanceContrast = new
                 * JCheckBox("Enhance contrast"); enchanceContrast.setContentAreaFilled(false);
                 * panel.add(enchanceContrast, c);
                 */

                thersholdMethod.setEnabled(false);
                minCirculartiy.setEnabled(false);
                minTheshold.setEnabled(false);
                channelType.setEnabled(false);
                mZProjection.setEnabled(false);
                minCirculartiy.setEnabled(false);
                mPreProcesssingSteps.setEnabled(false);
                marginToCrop.setEnabled(false);
                mMaxParticleSize.setEnabled(false);
                mMinParticleSize.setEnabled(false);
                snapAreaSize.setEnabled(false);
            }

            private MaskFormatter createFormatter(String s) {
                MaskFormatter formatter = null;
                try {
                    formatter = new MaskFormatter(s);
                    formatter.setPlaceholderCharacter('_');
                } catch (java.text.ParseException exc) {
                    System.err.println("formatter is bad: " + exc.getMessage());
                    System.exit(-1);
                }
                return formatter;
            }

        }

        void syncMinCircularitySetting(Double value, int myIdx) {
            for (int n = 0; n < NUMBEROFCHANNELSETTINGS; n++) {
                if (n != myIdx) {
                    channelSettings.get(n).minCirculartiy.setValue(value);
                }
            }
        }

        void syncMarginCrop(Double value, int myIdx) {
            for (int n = 0; n < NUMBEROFCHANNELSETTINGS; n++) {
                if (n != myIdx) {
                    channelSettings.get(n).marginToCrop.setValue(value);
                }
            }
        }

        void syncZProjection(int idx, int myIdx) {
            for (int n = 0; n < NUMBEROFCHANNELSETTINGS; n++) {
                ItemListener[] itm = channelSettings.get(n).mZProjection.getItemListeners().clone();
                channelSettings.get(n).mZProjection.removeItemListener(itm[0]);
                if (n != myIdx) {
                    channelSettings.get(n).mZProjection.setSelectedIndex(idx);
                }
                channelSettings.get(n).mZProjection.addItemListener(itm[0]);
            }
        }

        void syncParticleSizeMin(Double value, int myIdx) {
            for (int n = 0; n < NUMBEROFCHANNELSETTINGS; n++) {
                if (n != myIdx) {
                    channelSettings.get(n).mMinParticleSize.setValue(value);
                }
            }
        }

        void syncParticleSizeMax(Double value, int myIdx) {
            for (int n = 0; n < NUMBEROFCHANNELSETTINGS; n++) {
                if (n != myIdx) {
                    channelSettings.get(n).mMaxParticleSize.setValue(value);
                }
            }
        }

        void syncSnapAreaSize(Double value, int myIdx) {
            for (int n = 0; n < NUMBEROFCHANNELSETTINGS; n++) {
                if (n != myIdx) {
                    channelSettings.get(n).snapAreaSize.setValue(value);
                }
            }
        }

        public Vector<ChannelElements> channelSettings = new Vector<ChannelElements>();

        public PanelChannelSettings(Container parent) {
            GridBagLayout layout = new GridBagLayout();
            setLayout(layout);
            layout.preferredLayoutSize(parent);

            GridBagConstraints c = new GridBagConstraints();
            c.insets = new Insets(4, 4, 4, 4); // top padding
            c.anchor = GridBagConstraints.WEST;

            for (int n = 0; n < NUMBEROFCHANNELSETTINGS; n++) {
                ChannelElements ch = new ChannelElements(this, c, n + 2, 0, n);
                channelSettings.add(ch);
            }

            ////////////////////////////////////////////////////
            c.fill = GridBagConstraints.HORIZONTAL;
            c.gridx = 0;
            c.gridy = 1;
            c.weightx = 0.0;
            JLabel lc = new JLabel("Channel:");
            lc.setMinimumSize(new Dimension(200, lc.getMinimumSize().height));
            lc.setMaximumSize(new Dimension(200, lc.getMaximumSize().height));
            lc.setPreferredSize(new Dimension(200, lc.getPreferredSize().height));
            lc.setSize(new Dimension(200, lc.getSize().height));
            ImageIcon diamter1c = new ImageIcon(getClass().getResource("icons8-bring-forward-16.png"));
            lc.setIcon(diamter1c);
            this.add(lc, c);

            ////////////////////////////////////////////////////
            c.fill = GridBagConstraints.HORIZONTAL;
            c.gridx = 0;
            c.gridy++;
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
            JLabel l1 = new JLabel("Thershold algorithm:");
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
            JLabel l2 = new JLabel("Manual thershold:");
            l2.setToolTipText("Range [-1 to 65535]\nA value of -1 means automatic threshold detection.");
            l2.setMinimumSize(new Dimension(200, l.getMinimumSize().height));
            l2.setMaximumSize(new Dimension(200, l.getMaximumSize().height));
            l2.setPreferredSize(new Dimension(200, l.getPreferredSize().height));
            l2.setSize(new Dimension(200, l.getSize().height));
            ImageIcon diamter2 = new ImageIcon(getClass().getResource("icons8-plus-slash-minus-16.png"));
            l2.setIcon(diamter2);
            this.add(l2, c);

            ////////////////////////////////////////////////////////
            c.fill = GridBagConstraints.HORIZONTAL;
            c.gridx = 0;
            c.gridy++;
            c.weightx = 1;
            c.weightx = 0.0;
            c.gridwidth = 1;
            JLabel lci = new JLabel("Min circularity:");
            lci.setToolTipText("Range [0 to 1]\nA value of 1 means perfect circle!");
            lci.setMinimumSize(new Dimension(200, lci.getMinimumSize().height));
            lci.setMaximumSize(new Dimension(200, lci.getMaximumSize().height));
            lci.setPreferredSize(new Dimension(200, lci.getPreferredSize().height));
            lci.setSize(new Dimension(200, lci.getSize().height));
            ImageIcon diamterci = new ImageIcon(getClass().getResource("icons8-belarus-map-16.png"));
            lci.setIcon(diamterci);
            this.add(lci, c);

            ////////////////////////////////////////////////////////
            c.gridx = 1;
            bLockMinCircularity.setIcon(new ImageIcon(getClass().getResource("icons8-entsperren-16.png")));
            bLockMinCircularity.setSelectedIcon(new ImageIcon(getClass().getResource("icons8-sperren-16.png")));
            bLockMinCircularity.setBorderPainted(false);
            bLockMinCircularity.setSelected(true);
            bLockMinCircularity.setOpaque(false);
            bLockMinCircularity.setContentAreaFilled(false);
            bLockMinCircularity.setFocusPainted(false);
            this.add(bLockMinCircularity, c);
            c.gridx = 0;

            ////////////////////////////////////////////////////////
            {
                c.fill = GridBagConstraints.HORIZONTAL;
                c.gridx = 0;
                c.gridy++;
                c.weightx = 1;
                c.weightx = 0.0;
                c.gridwidth = 1;
                JLabel lpt = new JLabel("Particle size range [µm²]:");
                lpt.setToolTipText(
                        "Range [0 to 9999]\nValue in pixel. Particle size must be in the given range.\nElse it will be ignored for calculation.");
                lpt.setMinimumSize(new Dimension(200, lpt.getMinimumSize().height));
                lpt.setMaximumSize(new Dimension(200, lpt.getMaximumSize().height));
                lpt.setPreferredSize(new Dimension(200, lpt.getPreferredSize().height));
                lpt.setSize(new Dimension(200, lpt.getSize().height));
                ImageIcon diamterlpt = new ImageIcon(getClass().getResource("icons8-diameter-16.png"));
                lpt.setIcon(diamterlpt);
                this.add(lpt, c);

                ////////////////////////////////////////////////////////
                c.gridx = 1;
                bLockParticleSize.setIcon(new ImageIcon(getClass().getResource("icons8-entsperren-16.png")));
                bLockParticleSize.setSelectedIcon(new ImageIcon(getClass().getResource("icons8-sperren-16.png")));
                bLockParticleSize.setBorderPainted(false);
                bLockParticleSize.setSelected(true);
                bLockParticleSize.setOpaque(false);
                bLockParticleSize.setContentAreaFilled(false);
                bLockParticleSize.setFocusPainted(false);
                this.add(bLockParticleSize, c);
                c.gridx = 0;
            }
            ////////////////////////////////////////////////////////
            {
                c.fill = GridBagConstraints.HORIZONTAL;
                c.gridx = 0;
                c.gridy++;
                c.weightx = 1;
                c.weightx = 0.0;
                c.gridwidth = 1;
                JLabel lpt = new JLabel("Snap area diameter [µm]:");
                lpt.setToolTipText(
                        "Range [0 to 9999]\nValue in pixel. Particle size must be in the given range.\nElse it will be ignored for calculation.");
                lpt.setMinimumSize(new Dimension(200, lpt.getMinimumSize().height));
                lpt.setMaximumSize(new Dimension(200, lpt.getMaximumSize().height));
                lpt.setPreferredSize(new Dimension(200, lpt.getPreferredSize().height));
                lpt.setSize(new Dimension(200, lpt.getSize().height));
                ImageIcon diamterlpt = new ImageIcon(getClass().getResource("icons8-move-from-center-16.png"));
                lpt.setIcon(diamterlpt);
                this.add(lpt, c);

                ////////////////////////////////////////////////////////
                c.gridx = 1;
                bLockSnapArea.setIcon(new ImageIcon(getClass().getResource("icons8-entsperren-16.png")));
                bLockSnapArea.setSelectedIcon(new ImageIcon(getClass().getResource("icons8-sperren-16.png")));
                bLockSnapArea.setBorderPainted(false);
                bLockSnapArea.setSelected(true);
                bLockSnapArea.setOpaque(false);
                bLockSnapArea.setContentAreaFilled(false);
                bLockSnapArea.setFocusPainted(false);
                this.add(bLockSnapArea, c);
                c.gridx = 0;
            }

            ////////////////////////////////////////////////////
            // c.fill = GridBagConstraints.HORIZONTAL;
            // c.gridx = 0;
            // c.gridy++;
            // c.weightx = 0;
            // ImageIcon image1 = new
            //////////////////////////////////////////////////// ImageIcon(getClass().getResource("icons8-new-moon-16.png"));
            // JLabel intensity = new JLabel("Min Intensity [0-65535]:");
            // intensity.setIcon(image1);
            // this.add(intensity, c);

            ////////////////////////////////////////////////////////
            c.fill = GridBagConstraints.HORIZONTAL;
            c.gridx = 0;
            c.gridy++;
            c.weightx = 1;
            c.weightx = 0.0;
            c.gridwidth = 1;
            JLabel l3 = new JLabel("Z-Projection:");
            l3.setMinimumSize(new Dimension(200, l3.getMinimumSize().height));
            l3.setMaximumSize(new Dimension(200, l3.getMaximumSize().height));
            l3.setPreferredSize(new Dimension(200, l3.getPreferredSize().height));
            l3.setSize(new Dimension(200, l3.getSize().height));
            ImageIcon diamter3 = new ImageIcon(getClass().getResource("icons8-normal-distribution-histogram-16.png"));
            l3.setIcon(diamter3);
            this.add(l3, c);

            ////////////////////////////////////////////////////////
            c.gridx = 1;
            bLockZprojection.setIcon(new ImageIcon(getClass().getResource("icons8-entsperren-16.png")));
            bLockZprojection.setSelectedIcon(new ImageIcon(getClass().getResource("icons8-sperren-16.png")));
            bLockZprojection.setBorderPainted(false);
            bLockZprojection.setSelected(true);
            bLockZprojection.setOpaque(false);
            bLockZprojection.setContentAreaFilled(false);
            bLockZprojection.setFocusPainted(false);
            this.add(bLockZprojection, c);
            c.gridx = 0;

            ////////////////////////////////////////////////////////
            c.fill = GridBagConstraints.HORIZONTAL;
            c.gridx = 0;
            c.gridy++;
            c.weightx = 1;
            c.weightx = 0.0;
            c.gridwidth = 1;
            JLabel l4 = new JLabel("Margin-Crop [µm]:");
            l4.setToolTipText("Range [0 to 65535]\nThe margin of the image will be cropped on each side.");
            l4.setMinimumSize(new Dimension(200, l4.getMinimumSize().height));
            l4.setMaximumSize(new Dimension(200, l4.getMaximumSize().height));
            l4.setPreferredSize(new Dimension(200, l4.getPreferredSize().height));
            l4.setSize(new Dimension(200, l4.getSize().height));
            ImageIcon diamter4 = new ImageIcon(getClass().getResource("icons8-crop-16.png"));
            l4.setIcon(diamter4);
            this.add(l4, c);

            ////////////////////////////////////////////////////////
            c.gridx = 1;
            bLockMarginCrop.setIcon(new ImageIcon(getClass().getResource("icons8-entsperren-16.png")));
            bLockMarginCrop.setSelectedIcon(new ImageIcon(getClass().getResource("icons8-sperren-16.png")));
            bLockMarginCrop.setBorderPainted(false);
            bLockMarginCrop.setSelected(true);
            bLockMarginCrop.setOpaque(false);
            bLockMarginCrop.setContentAreaFilled(false);
            bLockMarginCrop.setFocusPainted(false);
            this.add(bLockMarginCrop, c);
            c.gridx = 0;

            ////////////////////////////////////////////////////////
            c.fill = GridBagConstraints.HORIZONTAL;
            c.gridx = 0;
            c.gridy++;
            c.weightx = 1;
            c.weightx = 0.0;
            c.gridwidth = 1;
            JLabel l5 = new JLabel("Pre processing step 1:");
            l5.setMinimumSize(new Dimension(200, l5.getMinimumSize().height));
            l5.setMaximumSize(new Dimension(200, l5.getMaximumSize().height));
            l5.setPreferredSize(new Dimension(200, l5.getPreferredSize().height));
            l5.setSize(new Dimension(200, l5.getSize().height));
            ImageIcon diamter5 = new ImageIcon(getClass().getResource("icons8-bring-forward-16.png"));
            l5.setIcon(diamter5);
            this.add(l5, c);

            /*
             * c.fill = GridBagConstraints.HORIZONTAL; c.gridx = 0; c.gridy++; c.weightx =
             * 1; c.weightx = 0.0; c.gridwidth = 1; JLabel l3 = new JLabel("Contrast:");
             * l3.setMinimumSize(new Dimension(200, l3.getMinimumSize().height));
             * l3.setMaximumSize(new Dimension(200, l3.getMaximumSize().height));
             * l3.setPreferredSize(new Dimension(200, l3.getPreferredSize().height));
             * l3.setSize(new Dimension(200, l3.getSize().height)); ImageIcon diamter3 = new
             * ImageIcon(getClass().getResource("icons8-plus-slash-minus-16.png"));
             * l3.setIcon(diamter3); this.add(l3, c);
             */

            c.fill = GridBagConstraints.HORIZONTAL;
            c.gridx = 0;
            c.gridy++;
            c.weightx = 1;
            c.weightx = 0.0;
            c.gridwidth = 1;
            JLabel l8 = new JLabel("Preview:");
            l8.setMinimumSize(new Dimension(200, l8.getMinimumSize().height));
            l8.setMaximumSize(new Dimension(200, l8.getMaximumSize().height));
            l8.setPreferredSize(new Dimension(200, l4.getPreferredSize().height));
            l8.setSize(new Dimension(200, l8.getSize().height));
            ImageIcon diamter55 = new ImageIcon(getClass().getResource("icons8-eye-16.png"));
            l8.setIcon(diamter55);
            this.add(l8, c);

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
                        if (mPrevImgIdx > 0) {
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
            c.gridx = 0;
            c.weightx = 0;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.anchor = GridBagConstraints.LINE_END;
            c.gridwidth = 7;
            this.add(previewButtons, c);
        }

        //
        // Preview option
        //
        private ImagePlus[] mOriginalImage0 = new ImagePlus[NUMBEROFCHANNELSETTINGS];
        private ImagePlus[] mPreviewImage0 = new ImagePlus[NUMBEROFCHANNELSETTINGS];

        private int mPrevImgIdx = 0;

        public void startPreview() {
            for (int n = 0; n < NUMBEROFCHANNELSETTINGS; n++) {
                mOriginalImage0[n] = null;
                mPreviewImage0[n] = null;
            }

            String[] imageTitles = WindowManager.getImageTitles();
            if (imageTitles.length <= 0) {
                File OpenImage = FileProcessor.getFile(mPrevImgIdx, mInputFolder.getText());
                if (null != OpenImage) {
                    FileProcessor.OpenImage(OpenImage, mSeries.getSelectedIndex(), true);
                }
            }

            imageTitles = WindowManager.getImageTitles();

            if (imageTitles.length > 0) {

                assignImagesForPreview();

            } else {
                thersholdPreview.setSelected(false);
                JOptionPane.showMessageDialog(new JFrame(), "Open an image to apply the preview on it!", "Dialog",
                        JOptionPane.WARNING_MESSAGE);
            }
        }

        public void assignImagesForPreview() {
            String[] imageTitles = WindowManager.getImageTitles();

            imageTitles = WindowManager.getImageTitles();

            // List images
            TreeMap<String, ImagePlus> imagesPerChannel = new TreeMap<String, ImagePlus>();
            for (int i = 0; i < imageTitles.length; i++) {
                String actTitle = imageTitles[i];
                ImagePlus imageTmp = WindowManager.getImage(actTitle);

                for (int c = 0; c < imageTitles.length; c++) {
                    String ch = "C=" + Integer.toString(c);
                    if (true == actTitle.endsWith(ch)) {
                        imagesPerChannel.put(ch, imageTmp);
                        break;
                    }
                }

                // Align images
                /*
                 * Dimension size = Toolkit.getDefaultToolkit().getScreenSize(); ImageWindow win
                 * = imageTmp.getWindow(); win.setSize(600, 400); int newPos = this.getX() +
                 * this.getWidth() + 10 + i * 30; win.setLocation(newPos, this.getY() + i * 35);
                 * IJ.run(imageTmp, "Scale to Fit", "");
                 */
            }

            for (int n = 0; n < NUMBEROFCHANNELSETTINGS; n++) {
                String channelNr = channelSettings.get(n).channel.getSelectedItem().toString();
                if (Pipeline.ChannelType.BACKGROUND == (Pipeline.ChannelType) channelSettings.get(n).channelType
                        .getSelectedItem()) {
                    // No preview for background
                    continue;
                }

                // Remove image
                if (channelNr == "OFF") {
                    if (null != mPreviewImage0[n]) {
                        mPreviewImage0[n].setImage(mOriginalImage0[n]);
                        mPreviewImage0[n] = null;
                        mOriginalImage0[n] = null;
                    }
                } else {
                    if (null == mPreviewImage0[n]) {
                        // Assign image
                        mPreviewImage0[n] = imagesPerChannel.get(channelNr);
                        mOriginalImage0[n] = Filter.duplicateImage(mPreviewImage0[n]);

                        Filter.RollingBall(mPreviewImage0[n]);
                        // Filter.ApplyGaus(mPreviewImage0[n]);
                        IJ.run(mPreviewImage0[n], "Convolve...",
                                "text1=[1 4 6 4 1\n4 16 24 16 4\n6 24 36 24 6\n4 16 24 16 4\n1 4 6 4 1] normalize");
                    } else {
                        // Swap image
                        if (mPreviewImage0[n].getTitle() != imagesPerChannel.get(channelNr).getTitle()) {
                            mPreviewImage0[n].setImage(mOriginalImage0[n]);
                            mPreviewImage0[n] = null;
                            mOriginalImage0[n] = null;

                            mPreviewImage0[n] = imagesPerChannel.get(channelNr);
                            mOriginalImage0[n] = Filter.duplicateImage(mPreviewImage0[n]);

                            Filter.RollingBall(mPreviewImage0[n]);
                            Filter.Smooth(mPreviewImage0[n]);
                            Filter.Smooth(mPreviewImage0[n]);
                        }
                    }
                }
            }
        }

        public void refreshPreview() {
            if (thersholdPreview.isSelected() == true) {
                assignImagesForPreview();
                for (int n = 0; n < NUMBEROFCHANNELSETTINGS; n++) {
                    if (null != mPreviewImage0[n]) {
                        setPreviewImage(mPreviewImage0[n], mOriginalImage0[n], channelSettings.get(n));
                    }
                }

            }
        }

        private void setPreviewImage(ImagePlus imgPrev, ImagePlus imgOri, ChannelElements elem) {
            if (imgPrev != null && imgOri != null) {

                int lowThershold = -1;
                try {
                    lowThershold = (Integer) elem.minTheshold.getValue();
                } catch (NumberFormatException ex) {
                }
                double[] th = new double[2];
                Filter.ApplyThershold(imgPrev, ((AutoThresholder.Method) elem.thersholdMethod.getSelectedItem()),
                        lowThershold, 65535, th, false);

            } else {
                /*
                 * JOptionPane.showMessageDialog(new JFrame(),
                 * "Open an image to apply the preview on it!", "Dialog",
                 * JOptionPane.WARNING_MESSAGE);
                 */
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

        public PanelFilter(Container parent) {

        }
    }

    class PanelReport extends JPanel {

        private JComboBox mComboReportGenerator;
        private JComboBox mControlPictures;
        private JTextField mReportName = new JTextField(30);

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
            JLabel l2 = new JLabel("Report name:");
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
            this.add(mReportName, c);

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

            AnalyseSettings.ReportType[] reportTypes = { AnalyseSettings.ReportType.FullReport,
                    AnalyseSettings.ReportType.FastReport };
            mComboReportGenerator = new JComboBox<AnalyseSettings.ReportType>(reportTypes);
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

            AnalyseSettings.CotrolPicture[] ctrlPictures = { AnalyseSettings.CotrolPicture.WithControlPicture,
                    AnalyseSettings.CotrolPicture.WithoutControlPicture };

            mControlPictures = new JComboBox<AnalyseSettings.CotrolPicture>(ctrlPictures);
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
    public JTabbedPane tabbedPane = new JTabbedPane();

    public Dialog() {

        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                super.windowClosing(e);
            }

            @Override
            public void windowOpened(WindowEvent e) {
                super.windowOpened(e);
            }
        });

        BorderLayout boorderL = new BorderLayout();
        this.setLayout(boorderL);

        tabbedPane.addTab("Analyzer", CreateMainTab());
        tabbedPane.addTab("Settings", createSettingsTab());
        tabbedPane.addTab("Logging", createLogPanel());

        //
        // File Menu
        //
        JMenu fileMenu = new JMenu("File");
        {
            JMenuItem it1 = new JMenuItem("Open");
            it1.setIcon(new ImageIcon(getClass().getResource("icons8-opened-folder-16.png")));
            it1.addActionListener(new java.awt.event.ActionListener() {
                // Beim Drücken des Menüpunktes wird actionPerformed aufgerufen
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    AnalyseSettings sett = new AnalyseSettings();
                    String file = OpenJsonFileChooser(false);
                    if (file.length() > 0) {
                        sett.loadSettingsFromFile(file);
                        loadAnalyzeSettings(sett);
                    }
                }
            });

            JMenu template = new JMenu("Presets");
            template.setIcon(new ImageIcon(getClass().getResource("icons8-empty-box-16.png")));
            template.add(generateTemplateMenuItem("coloc_two_ch.json", "Colocalization 2 Channels"));
            template.add(generateTemplateMenuItem("coloc_three_ch.json", "Colocalization 3 Channels"));
            template.add(
                    generateTemplateMenuItem("in_cell_counting_brightfield_with_separation.json",
                            "In Cell EV Counting"));

            JMenuItem save = new JMenuItem("Save");
            save.setIcon(new ImageIcon(getClass().getResource("icons8-update-16.png")));
            save.addActionListener(new java.awt.event.ActionListener() {
                // Beim Drücken des Menüpunktes wird actionPerformed aufgerufen
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    AnalyseSettings sett = getAnalyzeSettings(false);
                    if (sett != null) {
                        String file = OpenJsonFileChooser(true);
                        if (file.length() > 0) {
                            sett.saveSettings(file, "title", "note");
                        }
                    }
                }
            });
            fileMenu.add(save);
            fileMenu.add(it1);
            fileMenu.add(template);
        }

        //
        // Help menu
        //
        JMenu helpMenu = new JMenu("Help");
        {
            JMenuItem it1 = new JMenuItem("Help");
            JMenuItem it2 = new JMenuItem("About");
            it2.setIcon(new ImageIcon(getClass().getResource("icons8-info-16.png")));
            it2.addActionListener(new java.awt.event.ActionListener() {
                // Beim Drücken des Menüpunktes wird actionPerformed aufgerufen
                public void actionPerformed(java.awt.event.ActionEvent e) {

                    openAboutDialog();
                }
            });

            helpMenu.add(it1);
            helpMenu.add(it2);
        }

        mMenuBar.add(fileMenu);
        mMenuBar.add(helpMenu);
        mMenuBar.add(Box.createHorizontalGlue());
        JMenu logo = new JMenu("");
        logo.setContentAreaFilled(false);
        logo.setEnabled(false);
        ImageIcon ico = new ImageIcon(getClass().getResource("logo_24.png"));
        logo.setDisabledIcon(ico);
        logo.setIcon(ico);
        logo.setAlignmentX(JComponent.RIGHT_ALIGNMENT);
        logo.addActionListener(new java.awt.event.ActionListener() {
            // Beim Drücken des Menüpunktes wird actionPerformed aufgerufen
            public void actionPerformed(java.awt.event.ActionEvent e) {
                openAboutDialog();
            }
        });
        logo.addMenuListener(new MenuListener() {
            // Beim Drücken des Menüpunktes wird actionPerformed aufgerufen
            @Override
            public void menuSelected(MenuEvent e) {
                openAboutDialog();
            }

            @Override
            public void menuDeselected(MenuEvent e) {
                // TODO Auto-generated method stub
            }

            @Override
            public void menuCanceled(MenuEvent e) {
            }
        });

        mMenuBar.add(logo);

        mMenuBar.revalidate();
        this.setJMenuBar(mMenuBar);

        this.add(tabbedPane, BorderLayout.CENTER);
        this.add(createFooter(), BorderLayout.SOUTH);

        // Pack it
        // setBackground(Color.WHITE);
        // getContentPane().setBackground(Color.WHITE);
        pack();

        // this.setAlwaysOnTop(true);
        this.setResizable(false);
        ImageIcon iconImg = new ImageIcon(getClass().getResource("icon.png"));
        setIconImage(iconImg.getImage());
        setTitle("EVAnalyzer " + Version.getVersion());
        setVisible(true);
    }

    static void renderSplashFrame(Graphics2D g, int frame) {
        final String[] comps = { "foo", "bar", "baz" };
        g.setComposite(AlphaComposite.Clear);
        g.fillRect(120, 140, 200, 40);
        g.setPaintMode();
        g.setColor(Color.BLACK);
        g.drawString("Loading " + comps[(frame / 5) % 3] + "...", 120, 150);
    }

    ///
    ///
    ///
    public JPanel createSettingsTab() {
        JPanel borderLayoutPanel = new JPanel(new BorderLayout());
        JPanel gridBagLayoutPanel = new JPanel(new GridBagLayout());
        borderLayoutPanel.add(gridBagLayoutPanel, BorderLayout.NORTH);

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4); // top padding

        ////////////////////////////////////////////////////
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        gridBagLayoutPanel.add(new JLabel("Report settings: "), c);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 1;
        c.weightx = 1;
        gridBagLayoutPanel.add(reportSettings, c);

        ////////////////////////////////////////////////////
        // c.fill = GridBagConstraints.HORIZONTAL;
        // c.gridx = 0;
        // c.gridy++;
        // c.gridwidth = 3;
        // gridBagLayoutPanel.add(new JSeparator(SwingConstants.HORIZONTAL), c);

        return borderLayoutPanel;
    }

    ///
    ///
    ///
    ///
    public JPanel CreateMainTab() {
        ////////////////////////////////////////////////////////////////////
        JPanel mainTab = new JPanel();

        GridBagLayout layout = new GridBagLayout();
        mainTab.setLayout(layout);

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4); // top padding

        ////////////////////////////////////////////////////
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        c.gridwidth = 1;
        JLabel l1 = new JLabel("Input folder:");
        l1.setMinimumSize(new Dimension(200, l1.getMinimumSize().height));
        l1.setMaximumSize(new Dimension(200, l1.getMaximumSize().height));
        l1.setPreferredSize(new Dimension(200, l1.getPreferredSize().height));
        l1.setSize(new Dimension(200, l1.getSize().height));
        ImageIcon li1 = new ImageIcon(getClass().getResource("icons8-open-folder-in-new-tab-16.png"));
        l1.setIcon(li1);
        mainTab.add(l1, c);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 1;
        c.weightx = 1;
        mainTab.add(mInputFolder, c);

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
                outFolder.replace(File.separator, "");
            }
        });
        mainTab.add(mbInputFolder, c);

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
        mainTab.add(l2, c);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 1;
        c.weightx = 1;
        mainTab.add(mOutputFolder, c);

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
        mainTab.add(mbOutputFolder, c);

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
        mainTab.add(l3, c);

        String[] series = { "series_1", "series_2", "series_3", "series_4", "series_5" };
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 1;
        c.weightx = 1;
        mSeries = new JComboBox<String>(series);
        mainTab.add(mSeries, c);

        ////////////////////////////////////////////////////
        {
            c.fill = GridBagConstraints.HORIZONTAL;
            c.gridx = 0;
            c.gridy++;
            c.weightx = 0;
            JLabel l = new JLabel("Pixel in [µm]:");
            l.setMinimumSize(new Dimension(200, l.getMinimumSize().height));
            l.setMaximumSize(new Dimension(200, l.getMaximumSize().height));
            l.setPreferredSize(new Dimension(200, l.getPreferredSize().height));
            l.setSize(new Dimension(200, l.getSize().height));
            ImageIcon li = new ImageIcon(getClass().getResource("icons8-lineal-16.png"));
            l.setIcon(li);
            mainTab.add(l, c);
            c.fill = GridBagConstraints.HORIZONTAL;
            c.gridx = 1;
            c.weightx = 1;
            mainTab.add(mPixelInMicrometer, c);
        }

        ////////////////////////////////////////////////////
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 3;
        mainTab.add(new JSeparator(SwingConstants.HORIZONTAL), c);

        ////////////////////////////////////////////////////
        // Function
        {
            c.fill = GridBagConstraints.HORIZONTAL;
            c.gridx = 0;
            c.gridy++;
            c.weightx = 0;
            c.gridwidth = 1;
            JLabel l = new JLabel("Function:");
            l.setMinimumSize(new Dimension(200, l.getMinimumSize().height));
            l.setMaximumSize(new Dimension(200, l.getMaximumSize().height));
            l.setPreferredSize(new Dimension(200, l.getPreferredSize().height));
            l.setSize(new Dimension(200, l.getSize().height));
            ImageIcon li = new ImageIcon(getClass().getResource("icons8-lambda-16.png"));
            l.setIcon(li);
            mainTab.add(l, c);

            AnalyseSettings.Function[] functions = { AnalyseSettings.Function.noSelection,
                    AnalyseSettings.Function.evCount,
                    AnalyseSettings.Function.evColoc,
                    AnalyseSettings.Function.evCountInTotalCellArea,
                    AnalyseSettings.Function.evCountPerCell,
                    AnalyseSettings.Function.evCountPerCellRemoveCropped };
            c.fill = GridBagConstraints.HORIZONTAL;
            c.gridx = 1;
            c.weightx = 1;
            mFunctionSelection = new JComboBox<AnalyseSettings.Function>(functions);
            mFunctionSelection.setRenderer(new ItemRendererFunction(mFunctionSelection));
            mFunctionSelection.addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent e) {
                    AnalyseSettings.Function type = (AnalyseSettings.Function) mFunctionSelection.getSelectedItem();
                    if (AnalyseSettings.Function.noSelection == type) {
                        for (int n = 0; n < chSettings.channelSettings.size(); n++) {
                            chSettings.channelSettings.get(n).channel.setSelectedItem("OFF");
                        }
                    }
                }
            });
            mainTab.add(mFunctionSelection, c);
        }

        ////////////////////////////////////////////////////
        // Min colco factor
        {
            c.fill = GridBagConstraints.HORIZONTAL;
            c.gridx = 0;
            c.gridy++;
            c.weightx = 0;
            JLabel l = new JLabel("Min Coloc factor [%]");
            l.setMinimumSize(new Dimension(200, l.getMinimumSize().height));
            l.setMaximumSize(new Dimension(200, l.getMaximumSize().height));
            l.setPreferredSize(new Dimension(200, l.getPreferredSize().height));
            l.setSize(new Dimension(200, l.getSize().height));
            ImageIcon li = new ImageIcon(getClass().getResource("icons8-intersection-16.png"));
            l.setIcon(li);
            mainTab.add(l, c);
            c.fill = GridBagConstraints.HORIZONTAL;
            c.gridx = 1;
            c.weightx = 1;
            mainTab.add(mMinColocFactor, c);
        }

        ////////////////////////////////////////////////////
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 3;
        mainTab.add(new JSeparator(SwingConstants.HORIZONTAL), c);

        ////////////////////////////////////////////////////
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        c.gridwidth = 3;
        mainTab.add(chSettings, c);

        ////////////////////////////////////////////////////
        // c.fill = GridBagConstraints.HORIZONTAL;
        // c.gridx = 0;
        // c.gridy++;
        // c.gridwidth = 3;
        // mainTab.add(new JSeparator(SwingConstants.HORIZONTAL), c);

        ////////////////////////////////////////////////////
        // c.fill = GridBagConstraints.HORIZONTAL;
        // c.gridx = 0;
        // c.gridy++;
        // c.weightx = 0;
        // mainTab.add(new JLabel("Filter: "), c);
        //
        // c.fill = GridBagConstraints.HORIZONTAL;
        // c.gridx = 1;
        // c.weightx = 1;
        // mainTab.add(filter, c);

        ////////////////////////////////////////////////////
        // c.fill = GridBagConstraints.HORIZONTAL;
        // c.gridx = 0;
        // c.gridy++;
        // c.gridwidth = 3;
        // mainTab.add(new JSeparator(SwingConstants.HORIZONTAL), c);

        ////////////////////////////////////////////////////

        return mainTab;
    }

    ///
    ///
    ///
    public JPanel createFooter() {
        JPanel p = new JPanel(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4); // top padding

        ////////////////////////////////////////////////////
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.gridwidth = 4;
        mProgressbar.setStringPainted(true);
        mProgressbar.setString("0");
        p.add(mProgressbar, c);

        ////////////////////////////////////////////////////
        mMenu = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        // mMenu.setBackground(Color.WHITE);

        mbStart = new JButton();
        mbStart = new JButton(new ImageIcon(getClass().getResource("icons8-play-16.png")));
        mbStart.addActionListener(new java.awt.event.ActionListener() {
            // Beim Drücken des Menüpunktes wird actionPerformed aufgerufen
            public void actionPerformed(java.awt.event.ActionEvent e) {
                startAnalyse(getAnalyzeSettings(true));
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

        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 4;
        c.weightx = 1;
        p.add(mMenu, c);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 4;
        c.weightx = 1;
        int size = NewsTickerText.mNewsTicker.length - 1;
        int rand = 0 + (int) (Math.random() * ((size - 0) + 1));
        mLNewsTicker.setText(NewsTickerText.mNewsTicker[rand]);
        p.add(mLNewsTicker, c);

        ////////////////////////////////////////////////////
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 4;
        p.add(new JSeparator(SwingConstants.HORIZONTAL), c);

        ////////////////////////////////////////

        // Logo
        JLabel logo = new JLabel("(c) 2019 - 2022  SMJD", SwingConstants.RIGHT);
        // ImageIcon logoIcon = new ImageIcon(getClass().getResource("logo_48.png"));
        // logo.setIcon(logoIcon);

        c.gridx = 0;
        c.gridy++;
        c.weightx = 1;
        c.gridwidth = 1;
        p.add(logo, c);

        return p;
    }

    JTextArea mLog = new JTextArea();

    public JPanel createLogPanel() {
        JPanel p = new JPanel(new BorderLayout());
        JScrollPane sp = new JScrollPane(mLog); // JTextArea is placed in a JScrollPane.

        p.add(sp, BorderLayout.CENTER);
        return p;
    }

    static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH.mm.ss");

    public void addLogEntry(String text) {
        LocalDateTime localDateTime = LocalDateTime.now();
        String formatDateTime = localDateTime.format(formatter);
        mLog.insert(formatDateTime + "\t" + text + "\n", 0);
    }

    public void addLogEntryNewLine() {
        mLog.insert("\n", 0);
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

    synchronized public void incrementProgressBarValue(String lable) {
        int value = mProgressbar.getValue() + 1;
        mProgressbar.setValue(value);
        mProgressbar.setString(
                Integer.toString(value) + "/" + Integer.toString(mProgressbar.getMaximum()) + " " + lable + "");
    }

    //
    // Get analyze settings
    // \param[in] forAnalysis set to true if you want to get the settings prepared
    // for analysis
    //
    AnalyseSettings getAnalyzeSettings(boolean forAnalysis) {
        String error = "";
        AnalyseSettings sett = new AnalyseSettings();

        sett.mOnePixelInMicroMeter = (Double) mPixelInMicrometer.getValue();
        sett.mMinColocFactor = (Double) mMinColocFactor.getValue();

        if (true == forAnalysis) {
            sett.mInputFolder = mInputFolder.getText();
            File parentFile = new File(sett.mInputFolder);
            if (false == parentFile.exists()) {
                error = "Please select an existing input folder!\n";
            }

            String outputFolder = reportSettings.mReportName.getText();
            if (outputFolder.length() <= 0) {
                DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH.mm.ss");
                LocalDateTime now = LocalDateTime.now();
                outputFolder = dtf.format(now);
            }

            sett.mOutputFolder = mOutputFolder.getText() + java.io.File.separator + outputFolder;
            if (sett.mOutputFolder.length() <= 0) {
                error = "Please select an output folder!\n";
            }
            sett.mSelectedSeries = mSeries.getSelectedIndex();
        }

        //
        // Assign channel settings
        //
        sett.channelSettings.clear();
        for (int n = 0; n < NUMBEROFCHANNELSETTINGS; n++) {
            sett.channelSettings.add(chSettings.channelSettings.get(n).getChannelSettings(sett));
        }

        //
        //
        //
        sett.mSelectedFunction = (AnalyseSettings.Function) mFunctionSelection.getSelectedItem();
        if (sett.mSelectedFunction.equals(AnalyseSettings.Function.noSelection)) {
            error += PLEASE_SELECT_A_FUNCTION;
        }

        sett.mSaveDebugImages = (AnalyseSettings.CotrolPicture) reportSettings.mControlPictures.getSelectedItem();
        sett.reportType = (AnalyseSettings.ReportType) reportSettings.mComboReportGenerator.getSelectedItem();

        if (error.length() <= 0) {

        } else {
            JOptionPane.showMessageDialog(new JFrame(), error, "Dialog", JOptionPane.WARNING_MESSAGE);
            sett = null;
            // finishedAnalyse(mNameOfLastGeneratedReportFile);
        }
        return sett;
    }

    //
    // Load analyze settings
    //
    void loadAnalyzeSettings(AnalyseSettings sett) {
        mSeries.setSelectedIndex(sett.mSelectedSeries);
        mFunctionSelection.setSelectedItem(sett.mSelectedFunction);
        reportSettings.mControlPictures.setSelectedItem(sett.mSaveDebugImages);
        reportSettings.mComboReportGenerator.setSelectedItem(sett.reportType);
        mPixelInMicrometer.setValue(sett.mOnePixelInMicroMeter);
        mMinColocFactor.setValue(sett.mMinColocFactor);

        for (int n = 0; n < sett.channelSettings.size(); n++) {
            int chIdx = sett.channelSettings.get(n).mChannelIndex;
            chSettings.channelSettings.get(chIdx).loadChannelSettings(sett.channelSettings.get(chIdx));
        }
    }

    ///
    /// Start analyzing
    ///
    public void startAnalyse(AnalyseSettings sett) {
        if (null != sett) {
            // Creeate folder if not exists
            final File parentFile = new File(sett.mOutputFolder);
            boolean directoryExists = true;
            if (parentFile != null && !parentFile.exists()) {
                directoryExists = parentFile.mkdirs();
            }
            if (true == directoryExists) {
                sett.saveSettings(sett.mOutputFolder + java.io.File.separator + "settings.json",
                        sett.mReportName, "-");
                mbStart.setEnabled(false);
                mCancle.setEnabled(true);
                mOpenResult.setEnabled(false);
                tabbedPane.setSelectedIndex(2);
                mActAnalyzer = new FileProcessor(this, sett);
                mActAnalyzer.start();
            } else {
                IJ.log("Cannnot create directory: " + parentFile);
            }
        } else {
            finishedAnalyse("");
        }

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

    public String OpenJsonFileChooser(boolean save) {
        String returnFile = "";
        Preferences prefs = Preferences.userRoot().node(getClass().getName());
        String lastOpenFolder = prefs.get("LAST_USED_SETTINGS_FOLDER", ".");

        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(new java.io.File(lastOpenFolder));
        chooser.setDialogTitle("select settings file");
        chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        chooser.setAcceptAllFileFilterUsed(false);

        FileNameExtensionFilter filter = new FileNameExtensionFilter("JSON setting files", "json");
        chooser.addChoosableFileFilter(filter);
        int result;
        if (false == save) {
            result = chooser.showOpenDialog(this);
        } else {
            result = chooser.showSaveDialog(this);
        }
        if (result != JFileChooser.CANCEL_OPTION) {
            String selectedFile = chooser.getSelectedFile().getAbsolutePath();
            if (!selectedFile.endsWith(".json") && !selectedFile.endsWith(".JSON")) {
                selectedFile = selectedFile + ".json";
            }
            prefs.put("LAST_USED_SETTINGS_FOLDER", selectedFile);
            returnFile = selectedFile;
        }
        return returnFile;
    }

    JMenuItem generateTemplateMenuItem(String filename, String title) {
        JMenuItem it2 = new JMenuItem(title);
        it2.addActionListener(new java.awt.event.ActionListener() {
            // Beim Drücken des Menüpunktes wird actionPerformed aufgerufen
            public void actionPerformed(java.awt.event.ActionEvent e) {
                AnalyseSettings sett = new AnalyseSettings();
                String content = LoadTemplate(filename);
                if (content.length() > 0) {
                    sett.loadSettingsFromJson(content);
                    loadAnalyzeSettings(sett);
                }
            }
        });
        return it2;
    }

    String LoadTemplate(String filename) {
        String retString = "";
        try {
            InputStream is = getClass().getClassLoader().getResourceAsStream("templates/" + filename);
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String strCurrentLine;
            while ((strCurrentLine = br.readLine()) != null) {
                retString += strCurrentLine;
            }

        } catch (IOException ex) {
            IJ.log("Can not load token!");
            ex.printStackTrace();

        } catch (NullPointerException ex) {
            IJ.log("Nullptr Exception! " + ex.getMessage());
        }
        return retString;
    }

    private void openAboutDialog() {
        JOptionPane.showMessageDialog(this, "EVAnalyzer v" + Version.getVersion()
                + ".\n\nCopyright 2019 - 2022 Joachim Danmayr\nMany thanks to Melanie Schürz and Maria Jaritsch.\n\nLicensed under GPL v3.\nPreferably for use in non-profit research and development.\nIcons from https://icons8.de.\n\n",
                "About", JOptionPane.INFORMATION_MESSAGE);
    }

}
