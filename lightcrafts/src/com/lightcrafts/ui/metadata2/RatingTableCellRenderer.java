/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.metadata2;

import com.lightcrafts.platform.Platform;

import javax.swing.*;
import java.awt.*;

// The metadata table font on Windows lacks a star character.

class RatingTableCellRenderer extends MetadataTableCellRenderer {

    final static Font RatingFont = new Font("Serif", Font.PLAIN, 12);

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
        if (Platform.isWindows()) {
            if (column > 0) {
                comp.setFont(RatingFont);
            }
        }
        return comp;
    }
}
