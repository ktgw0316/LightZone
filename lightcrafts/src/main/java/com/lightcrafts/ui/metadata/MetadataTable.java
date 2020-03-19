/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.metadata;

import com.lightcrafts.platform.Platform;
import com.lightcrafts.ui.datatips.xswing.DataTipManager;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.KeyEvent;

class MetadataTable extends JTable {

    private final static Color RowColor = new Color(239, 239, 239);

    // JTable normally swallows all sorts of keystrokes for selection,
    // focus navigation, and editing.  We prefer to allow these to get
    // processed by any enclosing scroll pane.
    protected boolean processKeyBinding(
        KeyStroke ks, KeyEvent e, int condition, boolean pressed
    ) {
        return false;
    }

    MetadataTable(MetadataTableModel model) {
        super(model);

        // Use a small version of the default Font:

        Font font = getFont();
        font = font.deriveFont(9f);
        setFont(font);
        setRowSelectionAllowed(false);

        // Enable datatips:
        DataTipManager.get().register(this);

        // No keystroke controls here:
        setFocusable(false);

        setDefaultRenderer(
            Object.class,
            new DefaultTableCellRenderer() {
                private Color NormalColor = getBackground();
                public Component getTableCellRendererComponent(
                    JTable table,
                    Object value,
                    boolean isSelected,
                    boolean hasFocus,
                    int row,
                    int column
                ) {
                    Component renderer = super.getTableCellRendererComponent(
                        table, value, false, false, row, column
                    );
                    if (row % 2 == 1) {
                        renderer.setBackground(RowColor);
                    }
                    else {
                        renderer.setBackground(NormalColor);
                    }
                    return renderer;
                }
            }
        );
    }

    void dispose() {
        DataTipManager.get().unregister(this);
    }
}
