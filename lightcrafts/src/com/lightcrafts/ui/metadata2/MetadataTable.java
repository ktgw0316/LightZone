/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2014-     Masahiro Kitagawa */

package com.lightcrafts.ui.metadata2;

import com.lightcrafts.ui.LightZoneSkin;
import com.lightcrafts.utils.WebBrowser;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.awt.event.*;
import java.net.URI;

class MetadataTable extends JTable {

    MetadataTable(final MetadataTableModel model) {
        super(model);

        setBackground(LightZoneSkin.Colors.ToolPanesBackground);
        setForeground(LightZoneSkin.Colors.ToolPanesForeground);

        setOpaque(true);

        setRowSelectionAllowed(false);

        // No keystroke controls here:
        setFocusable(false);
        setEnabled(false);

        // Hack to constrain the column sizes...
        getColumnModel().getColumn(0).setMinWidth(100);
        getColumnModel().getColumn(0).setMaxWidth(120);
        
        getColumnModel().getColumn(1).setMinWidth(300);
        // getColumnModel().getColumn(1).setMaxWidth(300);

        setFont(LightZoneSkin.fontSet.getSmallFont());

        // Selection is not allowed, so a single click should enter the editor:
        addMouseListener(
            new MouseAdapter() {
                @Override
                public void mouseClicked(final MouseEvent event) {
                    // Another mouse event handler may be using this click
                    // to define a focus change or other end-edit action,
                    // so enqueue our start-edit action.
                    EventQueue.invokeLater(() -> {
                        Point p = event.getPoint();
                        int row = rowAtPoint(p);
                        int col = columnAtPoint(p);
                        if (editCellAt(row, col) && editorComp != null) {
                            editorComp.requestFocusInWindow();
                        }
                        else {
                            URI uri = model.getURIAt(row);
                            if (uri != null) {
                                WebBrowser.browse(uri);
                            }
                        }
                    });
                }
            }
        );

        // Set up the default cell editor:
        //
        //   Same font as the renderer;
        //   Select-all on focus gained;
        //   Tab key ends editing.

        final DefaultCellEditor editor =
            (DefaultCellEditor) getDefaultEditor(Object.class);
        final JTextField editComp = (JTextField) editor.getComponent();
        editComp.setForeground(
            LightZoneSkin.Colors.ToolPanesForeground
        );
        editComp.setFont(LightZoneSkin.fontSet.getSmallFont());
        editComp.addFocusListener(
            new FocusAdapter() {
                public void focusGained(FocusEvent event) {
                    editComp.selectAll();
                }
            }
        );
        Action selectNext = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                EventQueue.invokeLater(
                    new Runnable() {
                        public void run() {
                            editor.stopCellEditing();
                        }
                    }
                );
            }
        };
        editComp.registerKeyboardAction(
            selectNext,
            KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0),
            WHEN_FOCUSED
        );
        // The rating field gets a specialized editor, and make it popup.
        JComboBox ratingCombo = new JComboBox();
        ratingCombo.setEditable(false);        
        for (int rating=0; rating<=5; rating++) {
            String item = "";
            for (int n=1; n<=rating; n++) {
                // A star character with a glyph in the "Serif" logical font:
                item += "\u2605";
            }
            ratingCombo.addItem(item);
        }
        ratingCombo.setForeground(
            LightZoneSkin.Colors.ToolPanesForeground
        );
        ratingCombo.setFont(RatingTableCellRenderer.RatingFont);

        DefaultCellEditor ratingEditor = new DefaultCellEditor(ratingCombo);
        final JComboBox ratingComp = (JComboBox) ratingEditor.getComponent();
        ratingComp.setForeground(
            LightZoneSkin.Colors.ToolPanesForeground
        );
        ratingComp.addFocusListener(
            new FocusAdapter() {
                public void focusGained(FocusEvent event) {
                    ratingComp.setPopupVisible(true);
                }
            }
        );
        setDefaultEditor(RatingMetadataEntry.RatingObject.class, ratingEditor);

        // Setup the default renderer and also a specialized rating renderer.
        TableCellRenderer renderer = new MetadataTableCellRenderer();
        setDefaultRenderer(Object.class, renderer);

        TableCellRenderer ratingRenderer = new RatingTableCellRenderer();
        setDefaultRenderer(
            RatingMetadataEntry.RatingObject.class, ratingRenderer
        );
        // setRowHeight(15);
        // setRowMargin(10);
        setShowGrid(false);
    }

    // Make the cell renderer be determined by the class of each cell.
    public TableCellRenderer getCellRenderer(int row, int col) {
        Object o = getValueAt(row, col);
        return getDefaultRenderer(o.getClass());
    }

    // Make the cell editor be determined by the class of each cell.
    public TableCellEditor getCellEditor(int row, int col) {
        Object o = getValueAt(row, col);
        return getDefaultEditor(o.getClass());
    }

    // When editing ends, after changes are committed, refresh the whole
    // display from its file. 
    public void editingStopped(ChangeEvent event) {
        super.editingStopped(event);
        // Must enqueue the refresh; can't mutate the data model in a
        // cell editor callback.
        EventQueue.invokeLater(
            new Runnable() {
                public void run() {
                    final MetadataScroll scroll =
                        (MetadataScroll) SwingUtilities.getAncestorOfClass(
                            MetadataScroll.class, MetadataTable.this
                        );
                    if (scroll != null) {
                        scroll.refresh();
                    }
                }
            }
        );
    }
}
