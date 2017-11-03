/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.metadata2;

import com.lightcrafts.image.ImageInfo;
import com.lightcrafts.image.metadata.ImageMetadata;
import com.lightcrafts.image.metadata.ImageMetadataDirectory;
import com.lightcrafts.ui.LightZoneSkin;
import static com.lightcrafts.ui.metadata2.Locale.LOCALE;
import com.lightcrafts.ui.toolkit.PaneTitle;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * A vertical box holding MetadataSectionTables, or specialized error
 * components if there is trouble with metadata.
 */
public class MetadataStack extends JPanel implements Scrollable {

    private List<MetadataTable> tables;

    // A flag telling if we're showing one of our error messages.
    // (Needed for the Scrollable implementation.)
    private boolean error;

    public MetadataStack(ImageInfo info) {
        tables = new LinkedList<MetadataTable>();
        setImage(info);
        setBackground(LightZoneSkin.Colors.ToolPanesBackground);
        setOpaque(true);        
    }

    public void setImage(ImageInfo info) {
        removeAll();
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        tables.clear();

        add(new PaneTitle(LOCALE.get("MetadataTitle")));

        ImageMetadata meta = null;
        try {
            meta = info.getMetadata();
        }
        catch (Throwable t) {
            // BadImageFileException, IOException, UnknownImageTypeException
            t.printStackTrace();
        }
        if (meta == null) {
            String no = LOCALE.get("NoLabel");
            JLabel label = new JLabel(no);
            label.setAlignmentX(.5f);
            add(Box.createVerticalGlue());
            add(label);
            add(Box.createVerticalGlue());
            error = true;
            return;
        }
        Collection<ImageMetadataDirectory> directories =
            meta.getDirectories();
        if (directories.isEmpty()) {
            String empty = LOCALE.get("EmptyLabel");
            JLabel label = new JLabel(empty);
            label.setAlignmentX(.5f);
            add(Box.createVerticalGlue());
            add(label);
            add(Box.createVerticalGlue());
            error = true;
            return;
        }
        error = false;
        
        MetadataPresentation present = new MetadataPresentation();

        List<MetadataSection> sections = present.getSections();

        for (MetadataSection section : sections) {
            MetadataTableModel model =
                new MetadataTableModel(info, meta, section);
            MetadataTable table = new MetadataTable(model);
            tables.add(table);
            DefaultButtons buttons = new DefaultButtons(table, meta);
            Box control = Box.createHorizontalBox();
            control.add(table);
            control.add(buttons);
            add(control);
            add(Box.createVerticalStrut(4));
            add(new JSeparator());
            add(Box.createVerticalStrut(4));
        }
        add(Box.createVerticalGlue());
    }

    boolean isEditing() {
        for (MetadataTable table : tables) {
            if (table.isEditing()) {
                return true;
            }
        }
        return false;
    }

    void endEditing() {
        for (MetadataTable table : tables) {
            if (table.isEditing()) {
                TableCellEditor editor = table.getCellEditor();
                editor.stopCellEditing();
            }
        }
    }

    public boolean getScrollableTracksViewportHeight() {
        return false;
    }

    public boolean getScrollableTracksViewportWidth() {
        return false;
    }

    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    public int getScrollableBlockIncrement(
        Rectangle visibleRect, int orientation, int direction
    ) {
        // If we have any JTables, then defer to one of them:
        Component[] comps = getComponents();
        for (Component comp : comps) {
            if (comp instanceof JTable) {
                JTable table = (JTable) comp;
                return table.getScrollableBlockIncrement(
                    visibleRect, orientation, direction
                );
            }
        }
        return 1;
    }

    public int getScrollableUnitIncrement(
        Rectangle visibleRect, int orientation, int direction
    ) {
        // If we have any JTables, then defer to one of them:
        Component[] comps = getComponents();
        for (Component comp : comps) {
            if (comp instanceof JTable) {
                JTable table = (JTable) comp;
                return table.getScrollableUnitIncrement(
                    visibleRect, orientation, direction
                );
            }
        }
        return 1;
    }
}
