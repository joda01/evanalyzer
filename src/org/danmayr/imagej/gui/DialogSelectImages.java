package org.danmayr.imagej.gui;

import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

///
/// This dialog is shown after looking for images in folders and subfolders.
/// It allows to select if all images or just a couple of them should be analyzed
///
public class DialogSelectImages extends JDialog {
    public DialogSelectImages(JFrame root) {
        super(root);
        this.setSize(400, 300);
        this.setModal(true);
        this.setLocationRelativeTo(root);

        GridBagLayout layout = new GridBagLayout();
        this.setLayout(layout);

    }

    ArrayList<File> mFiles;
    ArrayList<File> mFilesSelected = new ArrayList<File>();
    JList lList;

    /**
     * @param files
     * @return
     */
    public ArrayList<File> show(ArrayList<File> files) {
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4); // top padding
        c.fill = GridBagConstraints.HORIZONTAL;

        this.mFiles = files;

        // Selected images
        JScrollPane scrollPane = new JScrollPane();
        lList = new JList<>(createDefaultListModel());
        lList.setVisibleRowCount(10);
        lList.setCellRenderer(new FileRenderer());
        scrollPane.setViewportView(lList);
        c.gridx = 0;
        c.gridy = 1;
        c.weightx = 2;
        c.gridwidth = 4;
        this.add(scrollPane, c);

        // Search field
        JTextField tSearch = new JTextField();
        tSearch.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                filter();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                filter();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
            }

            private void filter() {
                String filter = tSearch.getText();
                filterModel((DefaultListModel<File>) lList.getModel(), filter);
            }
        });
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.gridwidth = 4;
        this.add(tSearch, c);

        // Analyze selected
        JButton bCancel = new JButton();
        bCancel.setText("Cancel");
        bCancel.addActionListener(new java.awt.event.ActionListener() {
            // Beim Drücken des Menüpunktes wird actionPerformed aufgerufen
            public void actionPerformed(java.awt.event.ActionEvent e) {
                mFilesSelected.clear();
                setVisible(false);
            }
        });
        c.gridx = 0;
        c.gridy = 3;
        c.weightx = 1;
        c.gridwidth = 1;
        this.add(bCancel, c);

        // Analyze selected
        JButton bSelected = new JButton(new ImageIcon(getClass().getResource("icons8-play-16.png")));
        bSelected.setText("Analyze selected");
        bSelected.addActionListener(new java.awt.event.ActionListener() {
            // Beim Drücken des Menüpunktes wird actionPerformed aufgerufen
            public void actionPerformed(java.awt.event.ActionEvent e) {
                getSelected();
                setVisible(false);
            }
        });
        c.gridx = 1;
        c.gridy = 3;
        c.weightx = 1;
        c.gridwidth = 1;
        this.add(bSelected, c);

        this.pack();
        this.setResizable(false);
        this.setVisible(true);

        return mFilesSelected;
    }

    private void getSelected() {
        List l = lList.getSelectedValuesList();
        for (Object t : l) {
            mFilesSelected.add((File) t);
        }
    }

    private ListModel<File> createDefaultListModel() {
        DefaultListModel<File> model = new DefaultListModel<>();
        for (File s : this.mFiles) {
            model.addElement(s);
        }
        return model;
    }

    public void filterModel(DefaultListModel<File> model, String filter) {
        for (File s : mFiles) {
            if (!s.getName().contains(filter)) {
                if (model.contains(s)) {
                    model.removeElement(s);
                }
            } else {
                if (!model.contains(s)) {
                    model.addElement(s);
                }
            }
        }
    }

    public class FileRenderer extends JLabel implements ListCellRenderer<File> {
        @Override
        public Component getListCellRendererComponent(JList<? extends File> list, File value, int index,
                boolean isSelected, boolean cellHasFocus) {
            setText(value.getName());
            if (isSelected) {
                this.setForeground(Color.BLUE);
            } else {
                this.setForeground(Color.BLACK);
            }
            return this;
        }

    }

}
