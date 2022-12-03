/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2014-     Masahiro Kitagawa */

package com.lightcrafts.ui.metadata2;

import com.lightcrafts.ui.LightZoneSkin;
import com.lightcrafts.ui.metadata2.UrgencyMetadataEntry.UrgencyObject;
import com.lightcrafts.utils.WebBrowser;
import lombok.val;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
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

        val editor = (DefaultCellEditor) getDefaultEditor(Object.class);
        val editComp = (JTextField) editor.getComponent();
        editComp.setForeground(LightZoneSkin.Colors.ToolPanesForeground);
        editComp.addFocusListener(
                new FocusAdapter() {
                    @Override
                    public void focusGained(FocusEvent event) {
                        editComp.selectAll();
                    }
                }
        );
        Action selectNext = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                EventQueue.invokeLater(editor::stopCellEditing);
            }
        };
        editComp.registerKeyboardAction(
            selectNext,
            KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0),
            WHEN_FOCUSED
        );

        // The rating field gets a specialized editor, and make it popup.
        val ratingEditor = initRatingEditor();
        setDefaultEditor(RatingMetadataEntry.RatingObject.class, ratingEditor);

        // The urgency field gets a specialized editor, and make it popup.
        val urgencyEditor = initUrgencyEditor();
        setDefaultEditor(UrgencyObject.class, urgencyEditor);

        // Setup the default renderer and also a specialized rating renderer.
        TableCellRenderer renderer = new MetadataTableCellRenderer();
        setDefaultRenderer(Object.class, renderer);

        TableCellRenderer ratingRenderer = new RatingTableCellRenderer();
        setDefaultRenderer(RatingMetadataEntry.RatingObject.class, ratingRenderer);

        TableCellRenderer urgencyRenderer = new UrgencyTableCellRenderer();
        setDefaultRenderer(UrgencyObject.class, urgencyRenderer);

        setShowGrid(false);
    }

    private TableCellEditor initUrgencyEditor() {

        class UrgencyRenderer extends JLabel implements ListCellRenderer<String> {
            @Override
            public Component getListCellRendererComponent(JList<? extends String> list,
                    String value, int index, boolean isSelected, boolean cellHasFocus) {
                val color = UrgencyObject.colorOf(index);
                setBackground(color);
                setText(Integer.valueOf(index).toString()); // DEBUG
                return this;
            }
        }

        val urgencyCombo = new JComboBox<String>();
        urgencyCombo.setEditable(false);
        urgencyCombo.setRenderer(new UrgencyRenderer());
        for (int urgency = 0; urgency <= 8; urgency++) {
            urgencyCombo.addItem(Integer.valueOf(urgency).toString());
        }
        urgencyCombo.setSelectedIndex(-1);

        var urgencyEditor = new DefaultCellEditor(urgencyCombo);
        val urgencyComp = (JComboBox<?>) urgencyEditor.getComponent();
        urgencyComp.addFocusListener(
                new FocusAdapter() {
                    @Override
                    public void focusGained(FocusEvent event) {
                        urgencyComp.setPopupVisible(true);
                    }
                }
        );
        return urgencyEditor;
    }

    private TableCellEditor initRatingEditor() {
        val ratingCombo = new JComboBox<String>();
        ratingCombo.setEditable(false);
        ratingCombo.setForeground(LightZoneSkin.Colors.ToolPanesForeground);
        ratingCombo.setFont(RatingTableCellRenderer.RatingFont);
        for (int rating=0; rating<=5; rating++) {
            // A star character with a glyph in the "Serif" logical font:
            ratingCombo.addItem("\u2605".repeat(rating));
        }
        ratingCombo.setSelectedIndex(-1);

        var ratingEditor = new DefaultCellEditor(ratingCombo);
        val ratingComp = (JComboBox<?>) ratingEditor.getComponent();
        ratingComp.setForeground(LightZoneSkin.Colors.ToolPanesForeground);
        ratingComp.addFocusListener(
                new FocusAdapter() {
                    @Override
                    public void focusGained(FocusEvent event) {
                        ratingComp.setPopupVisible(true);
                    }
                }
        );
        return ratingEditor;
    }

    // Make the cell renderer be determined by the class of each cell.
    @Override
    public TableCellRenderer getCellRenderer(int row, int col) {
        Object o = getValueAt(row, col);
        return getDefaultRenderer(o.getClass());
    }

    // Make the cell editor be determined by the class of each cell.
    @Override
    public TableCellEditor getCellEditor(int row, int col) {
        Object o = getValueAt(row, col);
        return getDefaultEditor(o.getClass());
    }

    // When editing ends, after changes are committed, refresh the whole
    // display from its file. 
    @Override
    public void editingStopped(ChangeEvent event) {
        super.editingStopped(event);
        // Must enqueue the refresh; can't mutate the data model in a
        // cell editor callback.
        EventQueue.invokeLater(() -> {
            val scroll = (MetadataScroll) SwingUtilities.getAncestorOfClass(
                    MetadataScroll.class, MetadataTable.this);
            if (scroll != null) {
                scroll.refresh();
            }
        });
    }
}
