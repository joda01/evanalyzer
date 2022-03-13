package org.danmayr.imagej.gui;

import java.awt.Component;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.plaf.basic.BasicComboBoxRenderer;

import org.danmayr.imagej.algorithm.AnalyseSettings;

public class ItemRendererFunction extends BasicComboBoxRenderer { 
    
    JComboBox comboBox;
    public ItemRendererFunction(JComboBox comboBox){
        this.comboBox = comboBox;
    }
    @Override
    public Component getListCellRendererComponent(JList list, Object value,
        int index, boolean isSelected, boolean cellHasFocus) {
      super.getListCellRendererComponent(list, value, index, isSelected,
          cellHasFocus);
      if (value != null) {
        AnalyseSettings.Function item = (AnalyseSettings.Function) value;
        setText(item.getStringName());
      }
      if (index == -1) {
        AnalyseSettings.Function item = (AnalyseSettings.Function) value;
        setText("" + item.getStringName());
      }
      return this;
    }
  }