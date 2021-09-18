package com.lightcrafts.ui.metadata2;

import lombok.val;

import javax.swing.*;
import java.awt.*;

public class UrgencyTableCellRenderer extends MetadataTableCellRenderer {
    @Override
    public Component getTableCellRendererComponent(
            JTable table,
            Object value,
            boolean isSelected,
            boolean hasFocus,
            int row,
            int column
    ) {
        val label = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        if (column > 0) {
            val color = ((UrgencyMetadataEntry.UrgencyObject)value).getColor();
            label.setBackground(color);
        }
        return label;
    }
}
