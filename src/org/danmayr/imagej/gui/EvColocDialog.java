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
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.TreeMap;
import java.util.Vector;
import java.util.prefs.Preferences;
import java.awt.BorderLayout;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.danmayr.imagej.Version;
import org.danmayr.imagej.algorithm.AnalyseSettings;
import org.danmayr.imagej.algorithm.ChannelSettings;
import org.danmayr.imagej.algorithm.FileProcessor;
import org.danmayr.imagej.algorithm.filters.Filter;
import org.danmayr.imagej.algorithm.pipelines.Pipeline;

import ij.*;
import ij.gui.*;
import ij.process.*;

///
///
///
public class EvColocDialog extends JFrame {

    private static final String PLEASE_SELECT_A_FUNCTION = "Please select a function!\n";
    private static final int NUMBEROFCHANNELSETTINGS = 4;

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
    private JLabel mLNewsTicker = new JLabel("Science news ...");

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
            private JComboBox channel;
            private JComboBox thersholdMethod;
            private JCheckBox enchanceContrast;
            private JComboBox mZProjection;

            ///
            ///
            ///
            public ChannelSettings getChannelSettings() {

                ChannelSettings chSet = new ChannelSettings();
                chSet.mChannelNr = channel.getSelectedIndex() - 1;
                chSet.type = ((ComboItem<Pipeline.ChannelType>) channelType.getSelectedItem()).getValue();
                chSet.mThersholdMethod = ((ComboItem<AutoThresholder.Method>) thersholdMethod.getSelectedItem())
                        .getValue();
                chSet.enhanceContrast = false;
                chSet.maxThershold = 65535;
                chSet.ZProjector = mZProjection.getSelectedItem().toString();

                try {
                    chSet.minThershold = (Integer) (minTheshold.getValue());
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(new JFrame(), "Min thershold wrong!", "Dialog",
                            JOptionPane.WARNING_MESSAGE);
                }

                return chSet;
            }

            ///
            ///
            ///
            public ChannelElements(JPanel panel, GridBagConstraints c, int gridX, int gridY, int chNr) {

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
                        } else {
                            thersholdMethod.setEnabled(true);
                            minTheshold.setEnabled(true);
                            channelType.setEnabled(true);
                            mZProjection.setEnabled(true);
                        }
                        refreshPreview();
                    }
                });
                channel.setSelectedIndex(0);
                panel.add(channel, c);

                ////////////////////////////////////////////////////
                ComboItem<Pipeline.ChannelType>[] channels0 = new ComboItem[9];
                channels0[0] = new ComboItem<Pipeline.ChannelType>(Pipeline.ChannelType.EV_DAPI, "EV (DAPI)");
                channels0[1] = new ComboItem<Pipeline.ChannelType>(Pipeline.ChannelType.EV_GFP, "EV (GFP)");
                channels0[2] = new ComboItem<Pipeline.ChannelType>(Pipeline.ChannelType.EV_CY3, "EV (CY3)");
                channels0[3] = new ComboItem<Pipeline.ChannelType>(Pipeline.ChannelType.EV_CY5, "EV (CY5)");
                channels0[4] = new ComboItem<Pipeline.ChannelType>(Pipeline.ChannelType.EV_CY7, "EV (CY7)");
                channels0[5] = new ComboItem<Pipeline.ChannelType>(Pipeline.ChannelType.CELL, "CELL");
                channels0[6] = new ComboItem<Pipeline.ChannelType>(Pipeline.ChannelType.NUCLEUS, "NUCLEUS");
                channels0[7] = new ComboItem<Pipeline.ChannelType>(Pipeline.ChannelType.NEGATIVE_CONTROL,
                        "Negative Control");
                channels0[8] = new ComboItem<Pipeline.ChannelType>(Pipeline.ChannelType.BACKGROUND, "Background");

                c.fill = GridBagConstraints.HORIZONTAL;
                c.gridy++;
                channelType = new JComboBox<ComboItem<Pipeline.ChannelType>>(channels0);
                channelType.addItemListener(new ItemListener() {
                    @Override
                    public void itemStateChanged(ItemEvent e) {
                        Pipeline.ChannelType type = ((ComboItem<Pipeline.ChannelType>) channelType.getSelectedItem())
                                .getValue();
                        if (Pipeline.ChannelType.CELL == type) {
                            // Select MinError
                            thersholdMethod.setSelectedIndex(4);
                        } else if (Pipeline.ChannelType.NUCLEUS == type) {
                            // Select triangle
                            thersholdMethod.setSelectedIndex(5);
                        } else {
                            // Select LI
                            thersholdMethod.setSelectedIndex(0);                                  
                        }

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
                int t = 0;
                ComboItem<AutoThresholder.Method>[] thersholds = new ComboItem[6];
                thersholds[t++] = new ComboItem<AutoThresholder.Method>(AutoThresholder.Method.Li, "LI");
                thersholds[t++] = new ComboItem<AutoThresholder.Method>(AutoThresholder.Method.MaxEntropy,
                        "MaxEntropy");
                thersholds[t++] = new ComboItem<AutoThresholder.Method>(AutoThresholder.Method.Moments, "Moments");
                thersholds[t++] = new ComboItem<AutoThresholder.Method>(AutoThresholder.Method.Otsu, "Otsu");
                thersholds[t++] = new ComboItem<AutoThresholder.Method>(AutoThresholder.Method.MinError, "MinError");
                thersholds[t++] = new ComboItem<AutoThresholder.Method>(AutoThresholder.Method.Triangle, "Triangle");
                c.fill = GridBagConstraints.HORIZONTAL;
                c.gridy++;
                thersholdMethod = new JComboBox<ComboItem<AutoThresholder.Method>>(thersholds);
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
                String[] zProjection = { "OFF", "max", "min", "avg" };
                c.fill = GridBagConstraints.HORIZONTAL;
                c.gridy++;
                mZProjection = new JComboBox<String>(zProjection);
                panel.add(mZProjection, c);

                ////////////////////////////////////////////////////
                /*
                 * c.fill = GridBagConstraints.HORIZONTAL; c.gridy++; enchanceContrast = new
                 * JCheckBox("Enhance contrast"); enchanceContrast.setContentAreaFilled(false);
                 * panel.add(enchanceContrast, c);
                 */

                thersholdMethod.setEnabled(false);
                minTheshold.setEnabled(false);
                channelType.setEnabled(false);
                mZProjection.setEnabled(false);
            }
        }

        private JToggleButton thersholdPreview;
        public Vector<ChannelElements> channelSettings = new Vector<ChannelElements>();

        public PanelChannelSettings(Container parent) {
            GridBagLayout layout = new GridBagLayout();
            setLayout(layout);
            layout.preferredLayoutSize(parent);

            GridBagConstraints c = new GridBagConstraints();
            c.insets = new Insets(4, 4, 4, 4); // top padding
            c.anchor = GridBagConstraints.WEST;

            for (int n = 0; n < NUMBEROFCHANNELSETTINGS; n++) {
                ChannelElements ch = new ChannelElements(this, c, n + 1, 0, n);
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
            JLabel l3 = new JLabel("Z-Projection:");
            l3.setMinimumSize(new Dimension(200, l3.getMinimumSize().height));
            l3.setMaximumSize(new Dimension(200, l3.getMaximumSize().height));
            l3.setPreferredSize(new Dimension(200, l3.getPreferredSize().height));
            l3.setSize(new Dimension(200, l3.getSize().height));
            ImageIcon diamter3 = new ImageIcon(getClass().getResource("icons8-normal-distribution-histogram-16.png"));
            l3.setIcon(diamter3);
            this.add(l3, c);

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
            JLabel l4 = new JLabel("Preview:");
            l4.setMinimumSize(new Dimension(200, l4.getMinimumSize().height));
            l4.setMaximumSize(new Dimension(200, l4.getMaximumSize().height));
            l4.setPreferredSize(new Dimension(200, l4.getPreferredSize().height));
            l4.setSize(new Dimension(200, l4.getSize().height));
            ImageIcon diamter4 = new ImageIcon(getClass().getResource("icons8-eye-16.png"));
            l4.setIcon(diamter4);
            this.add(l4, c);

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
            c.gridx = 4;
            c.weightx = 0;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.anchor = GridBagConstraints.LINE_END;
            c.gridwidth = 1;
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

                        Filter.SubtractBackground(mPreviewImage0[n]);
                        //Filter.ApplyGaus(mPreviewImage0[n]);
                        IJ.run(mPreviewImage0[n], "Convolve...", "text1=[1 4 6 4 1\n4 16 24 16 4\n6 24 36 24 6\n4 16 24 16 4\n1 4 6 4 1] normalize");
                    } else {
                        // Swap image
                        if (mPreviewImage0[n].getTitle() != imagesPerChannel.get(channelNr).getTitle()) {
                            mPreviewImage0[n].setImage(mOriginalImage0[n]);
                            mPreviewImage0[n] = null;
                            mOriginalImage0[n] = null;

                            mPreviewImage0[n] = imagesPerChannel.get(channelNr);
                            mOriginalImage0[n] = Filter.duplicateImage(mPreviewImage0[n]);

                            Filter.SubtractBackground(mPreviewImage0[n]);
                            Filter.ApplyGaus(mPreviewImage0[n]);
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
                Filter.ApplyThershold(imgPrev,
                        ((ComboItem<AutoThresholder.Method>) elem.thersholdMethod.getSelectedItem()).getValue(),
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
    public JTabbedPane tabbedPane = new JTabbedPane();

    public EvColocDialog() {

        BorderLayout boorderL = new BorderLayout();
        this.setLayout(boorderL);

        tabbedPane.addTab("main", CreateMainTab());
        tabbedPane.addTab("log", createLogPanel());

        this.add(tabbedPane, BorderLayout.CENTER);
        this.add(createFooter(), BorderLayout.SOUTH);

        // Pack it
        // setBackground(Color.WHITE);
        // getContentPane().setBackground(Color.WHITE);
        pack();

        // this.setAlwaysOnTop(true);
        this.setResizable(false);
        setTitle("Exosome analyzer " + Version.getVersion());
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
        c.gridy = 0;
        c.gridwidth = 3;
        JLabel titl = new JLabel("Version " + Version.getVersion() + " | pre release | multithreading experimental");
        titl.setHorizontalTextPosition(SwingConstants.CENTER);
        titl.setOpaque(true);
        titl.setBackground(Color.RED);
        mainTab.add(titl, c);

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
                reportSettings.mReportFileName.setText(outFolder);
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
        JLabel l = new JLabel("Function:");
        l.setMinimumSize(new Dimension(200, l.getMinimumSize().height));
        l.setMaximumSize(new Dimension(200, l.getMaximumSize().height));
        l.setPreferredSize(new Dimension(200, l.getPreferredSize().height));
        l.setSize(new Dimension(200, l.getSize().height));
        ImageIcon li = new ImageIcon(getClass().getResource("icons8-lambda-16.png"));
        l.setIcon(li);
        mainTab.add(l, c);

        AnalyseSettings.Function[] functions = { AnalyseSettings.Function.noSelection,
                AnalyseSettings.Function.calcColoc, AnalyseSettings.Function.countExosomes,
                AnalyseSettings.Function.countInCellExosomes };
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 1;
        c.weightx = 1;
        mFunctionSelection = new JComboBox<AnalyseSettings.Function>(functions);
        mFunctionSelection.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                AnalyseSettings.Function type = (AnalyseSettings.Function) mFunctionSelection.getSelectedItem();
                // Calc coloc
                if (AnalyseSettings.Function.calcColoc == type) {
                    chSettings.channelSettings.get(0).channel.setSelectedItem("C=0");
                    chSettings.channelSettings.get(1).channel.setSelectedItem("C=1");
                    for (int n = 2; n < chSettings.channelSettings.size(); n++) {
                        chSettings.channelSettings.get(n).channel.setSelectedItem("OFF");
                    }
                } else if (AnalyseSettings.Function.countInCellExosomes == type) {
                    chSettings.channelSettings.get(0).channel.setSelectedItem("C=0");
                    chSettings.channelSettings.get(1).channel.setSelectedItem("C=1");
                    chSettings.channelSettings.get(2).channel.setSelectedItem("C=3");
                    chSettings.channelSettings.get(3).channel.setSelectedItem("C=4");

                    chSettings.channelSettings.get(0).channelType.setSelectedIndex(0);
                    chSettings.channelSettings.get(1).channelType.setSelectedIndex(1);
                    chSettings.channelSettings.get(2).channelType.setSelectedIndex(6);
                    chSettings.channelSettings.get(3).channelType.setSelectedIndex(5);

                } else if (AnalyseSettings.Function.noSelection == type) {
                    for (int n = 0; n < chSettings.channelSettings.size(); n++) {
                        chSettings.channelSettings.get(n).channel.setSelectedItem("OFF");
                    }
                }
            }
        });
        mainTab.add(mFunctionSelection, c);

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
        mainTab.add(new JLabel("Filter: "), c);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 1;
        c.weightx = 1;
        mainTab.add(filter, c);

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
        mainTab.add(new JLabel("Report settings: "), c);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 1;
        mainTab.add(reportSettings, c);

        ////////////////////////////////////////////////////
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 3;
        mainTab.add(new JSeparator(SwingConstants.HORIZONTAL), c);

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
        c.gridwidth = 3;
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

        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 3;
        c.weightx = 1;
        p.add(mMenu, c);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 3;
        c.weightx = 1;
        int size = NewsTickerText.mNewsTicker.length - 1;
        int rand = 0 + (int) (Math.random() * ((size - 0) + 1));
        mLNewsTicker.setText(NewsTickerText.mNewsTicker[rand]);
        p.add(mLNewsTicker, c);

        ////////////////////////////////////////////////////
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 3;
        p.add(new JSeparator(SwingConstants.HORIZONTAL), c);

        ////////////////////////////////////////

        // Logo
        JLabel logo = new JLabel("(c) 2019 - 2021  SMJD", SwingConstants.RIGHT);
        ImageIcon logoIcon = new ImageIcon(getClass().getResource("logo_32.png"));
        logo.setIcon(logoIcon);

        c.gridx = 0;
        c.gridy++;
        c.weightx = 1;
        c.gridwidth = 1;
        p.add(logo, c);

        JButton about = new JButton(new ImageIcon(getClass().getResource("icons8-info-16.png")));
        about.addActionListener(new java.awt.event.ActionListener() {
            // Beim Drücken des Menüpunktes wird actionPerformed aufgerufen
            public void actionPerformed(java.awt.event.ActionEvent e) {
                JOptionPane.showMessageDialog(new JFrame(), "Exosome Analyzer v" + Version.getVersion()
                        + ".\n\nMany thanks to Melanie Schürz and Maria Jaritsch.\n\nLicensed under MIT.\nPreferably for use in non-profit research and development.\nIcons from https://icons8.de.\n\n (c) 2020 - 2021 J. Danmayr",
                        "About", JOptionPane.INFORMATION_MESSAGE);

            }
        });
        about.setText("About");
        c.gridx = 2;
        c.weightx = 0;
        c.gridwidth = 1;
        p.add(about, c);

        return p;
    }

    JTextArea mLog = new JTextArea();

    public JPanel createLogPanel() {
        JPanel p = new JPanel(new BorderLayout());
        JScrollPane sp = new JScrollPane(mLog); // JTextArea is placed in a JScrollPane.

        p.add(sp, BorderLayout.CENTER);
        return p;
    }

    static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

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

    public void incrementProgressBarValue(String lable) {
        int value = mProgressbar.getValue() + 1;
        mProgressbar.setValue(value);
        mProgressbar.setString(
                Integer.toString(value) + "/" + Integer.toString(mProgressbar.getMaximum()) + " " + lable + "");
    }

    ///
    /// Start analyzing
    ///
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
        sett.mSelectedSeries = mSeries.getSelectedIndex();

        //
        // Assign channel settings
        //
        sett.channelSettings.clear();
        for (int n = 0; n < NUMBEROFCHANNELSETTINGS; n++) {
            sett.channelSettings.add(chSettings.channelSettings.get(n).getChannelSettings());
        }

        //
        // Check channel settings
        //
        // It is not allowed to have two equal channel types
        for (int n = 0; n < NUMBEROFCHANNELSETTINGS; n++) {
            for (int m = 0; m < NUMBEROFCHANNELSETTINGS; m++) {
                if (sett.channelSettings.get(n).mChannelNr >= 0 && sett.channelSettings.get(m).mChannelNr >= 0) {
                    if (n != m) {
                        if (sett.channelSettings.get(n).type == sett.channelSettings.get(m).type) {

                            // Only check two different channel
                            error += "There are two equal channel types!\n";
                        }
                        if (sett.channelSettings.get(n).mChannelNr == sett.channelSettings.get(m).mChannelNr) {
                            // Only check two different channel
                            error += "There are two equal channel numbers!\n";
                        }
                    }
                }
            }
        }

        //
        //
        //
        sett.mSelectedFunction = (AnalyseSettings.Function) mFunctionSelection.getSelectedItem();
        if (sett.mSelectedFunction.equals(AnalyseSettings.Function.noSelection)) {
            error += PLEASE_SELECT_A_FUNCTION;
        }

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
            sett.mMinCircularity = Double.parseDouble(filter.mMinCircularity.getText());
        } catch (NumberFormatException ex) {
            error += "Circularity is wrong!\n";
        }

        try {
            sett.minIntensity = Integer.parseInt(filter.mMinIntensity.getText());
        } catch (NumberFormatException ex) {
            error += "Intensity wrong!\n";
        }

        if (error.length() <= 0) {
            tabbedPane.setSelectedIndex(1);
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