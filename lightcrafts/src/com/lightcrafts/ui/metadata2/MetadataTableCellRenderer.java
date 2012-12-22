/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.metadata2;

import com.lightcrafts.ui.LightZoneSkin;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableModel;
import java.awt.*;

class MetadataTableCellRenderer extends DefaultTableCellRenderer {

    public Component getTableCellRendererComponent(
        JTable table,
        Object value,
        boolean isSelected,
        boolean hasFocus,
        int row,
        int column
    ) {
        Component comp = super.getTableCellRendererComponent(
            table, value, isSelected, hasFocus, row, column
        );
        JLabel label = (JLabel) comp;
        if (column == 0) {
            label.setHorizontalAlignment(RIGHT);
            label.setForeground(
                LightZoneSkin.Colors.ToolPanesForeground.darker()
            );
        }
        else {
            label.setHorizontalAlignment(LEFT);
            label.setForeground(LightZoneSkin.Colors.ToolPanesForeground);
        }
        // label.setMaximumSize(new Dimension(100, 20));

        TableModel model = table.getModel();
        if (model.isCellEditable(row, column)) {
            // Some cells get a background to show they are editable.
            label.setBackground(LightZoneSkin.Colors.FrameBackground);
        }
        else {
            label.setBackground(LightZoneSkin.Colors.ToolPanesBackground);
        }
        return label;
    }
}
