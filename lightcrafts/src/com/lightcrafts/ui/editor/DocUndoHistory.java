/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.editor;

import com.lightcrafts.app.ComboFrame;
import com.lightcrafts.ui.LightZoneSkin;
import static com.lightcrafts.ui.editor.Locale.LOCALE;
import com.lightcrafts.ui.toolkit.PaneTitle;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.undo.UndoableEdit;
import java.awt.*;
import java.awt.event.MouseWheelEvent;
import java.util.List;

/**
 * Renders the live undo stack in list form, allowing selection and
 * keyboard navigation of the stack.
 */
public final class DocUndoHistory
    extends JPanel implements UndoableEditListener {

    private DocUndoManager undo;
    private DefaultListModel model;
    private ListSelectionModel selection;

    private JScrollPane scroll;
    private JList list;

    // prevent update loops
    private boolean isEditing;
    private boolean isSelecting;

    private static final class UndoCellRenderer
        extends JLabel implements ListCellRenderer {

        UndoCellRenderer() {
            setForeground(LightZoneSkin.Colors.ToolPanesForeground);
            setBackground(LightZoneSkin.Colors.ToolPanesBackground);
        }

        public Component getListCellRendererComponent(JList list, Object value,
                                                      int index, boolean isSelected,
                                                      boolean cellHasFocus) {
            if (cellHasFocus || isSelected)
                setBackground(LightZoneSkin.Colors.ToolsBackground.darker().darker());
            else
                setBackground(LightZoneSkin.Colors.ToolPanesBackground);
            setText((String) value);
            return this;
        }
    }

    // A disabled component, for the no-Document display mode.
    public DocUndoHistory() {
        setEnabled(false);
        setBackground(LightZoneSkin.Colors.ToolPanesBackground);
        setOpaque(true);
        setBorder(LightZoneSkin.getPaneBorder());
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        list = new JList();
        list.setForeground(LightZoneSkin.Colors.ToolPanesForeground);
        list.setBackground(LightZoneSkin.Colors.ToolPanesBackground);
        list.setCellRenderer(new UndoCellRenderer());

        scroll = new JScrollPane(list);
        scroll.setHorizontalScrollBarPolicy(
            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
        );
        scroll.setVerticalScrollBarPolicy(
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
        );
        scroll.setBorder(null);

        add(new PaneTitle(LOCALE.get("DocUndoHistoryTitle")));
        add(scroll);
    }

    public DocUndoHistory(Document doc) {
        this();

        setEnabled(true);

        this.undo = doc.getUndoManager();

        model = new DefaultListModel();
        model.addElement(LOCALE.get("DocUndoHistoryOriginalItem"));
        list.setModel(model);

        selection = list.getSelectionModel();
        selection.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        undo.addUndoableEditListener(this);

        selection.addListSelectionListener(
            new ListSelectionListener() {
                public void valueChanged(ListSelectionEvent e) {
                    if (! isSelecting) {
                        if (! e.getValueIsAdjusting()) {
                            getComboFrame().getEditor().setMode( EditorMode.ARROW );
                            isEditing = true;
                            int selected = selection.getLeadSelectionIndex();
                            int index = model.size() - selected - 2;
                            System.out.println("selected index = " + index);
                            undo.setEditIndex(index);
                            isEditing = false;
                        }
                    }
                }
            }
        );
    }

    // Exposed only so the tree's bounds can be determined for the purpose of
    // dispatching our custom horizontal-scroll mouse wheel events.
    public JComponent getScrollPane() {
        return scroll;
    }

    // Special handling for Mighty Mouse and two-finger trackpad
    // horizontal scroll events
    public void horizontalMouseWheelMoved(MouseWheelEvent e) {
        if (e.getScrollType() >= 2) {
            if (scroll.isWheelScrollingEnabled()) {
                JScrollBar bar = scroll.getHorizontalScrollBar();
                int dir = e.getWheelRotation() < 0 ? -1 : 1;
                int inc = bar.getUnitIncrement(dir);
                int value = bar.getValue() - e.getWheelRotation() * inc;
                bar.setValue(value);
            }
        }
    }

    public ComboFrame getComboFrame() {
        return (ComboFrame)SwingUtilities.getAncestorOfClass(
            ComboFrame.class, this
        );
    }

    public void undoableEditHappened(UndoableEditEvent event) {
        if (! isEditing) {
            isSelecting = true;
            model.clear();
            model.addElement(LOCALE.get("DocUndoHistoryOriginalItem"));
            selection.removeSelectionInterval(0, model.getSize());
            List<UndoableEdit> edits = undo.getEdits();
            for (UndoableEdit edit : edits) {
                model.insertElementAt(edit.getPresentationName(), 0);
            }
            int index = edits.size() - undo.getEditIndex() - 1;
            selection.setSelectionInterval(index, index);
            isSelecting = false;
        }
    }
}
