package org.danmayr.imagej.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.util.Vector;
/*from ww  w .  j  a  va  2  s. c  o  m*/
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.plaf.basic.BasicComboBoxRenderer;

import ij.process.AutoThresholder;

class ThersholdItemRenderer extends BasicComboBoxRenderer {
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if (value instanceof AutoThresholder.Method) {
            AutoThresholder.Method val = (AutoThresholder.Method)value;
            if(val == AutoThresholder.Method.Default){
                setText("Manual");
            }
        }
        return this;
    }
  }