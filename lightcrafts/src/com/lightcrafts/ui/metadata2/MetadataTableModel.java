/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.metadata2;

import com.lightcrafts.image.ImageInfo;
import com.lightcrafts.image.metadata.ImageMetadata;
import com.lightcrafts.image.types.ImageType;

import javax.swing.table.AbstractTableModel;
import java.net.URI;

/**
 * Provide a basic two-column, read-only table model containing key-value
 * pairs for all metadata tags in a File.
 */

class MetadataTableModel extends AbstractTableModel {

    private final static String[] ColumnNames = {"Key", "Value"};

    private ImageInfo info;
    private ImageMetadata meta;
    private MetadataSection section;

    // The ImageInfo and ImageMetadata here must be connected: if the
    // ImageMetadata is mutated and then ImageInfo.writeMetadata() runs,
    // the mutation will be written out.
    MetadataTableModel(
        ImageInfo info, ImageMetadata meta, MetadataSection section
    ) {
        this.info = info;
        this.meta = meta;
        this.section = section;
    }

    public String getColumnName(int col) {
        return ColumnNames[col];
    }

    public int getRowCount() {
        return section.size();
    }

    public int getColumnCount() {
        return 2;
    }

    public Object getValueAt(int row, int col) {
        MetadataEntry entry = section.get(row);
        if (col == 0) {
            return entry.getLabel(meta) + ':';
        }
        return entry.getValue(meta);
    }

    public boolean isCellEditable(int row, int col) {
        MetadataEntry entry = section.get(row);
        return ((col == 1) && entry.isEditable(info));
    }

    public void setValueAt(Object value, int row, int col) {
        if (col == 0) {
            return;
        }
        MetadataEntry entry = section.get(row);
        entry.setValue(meta, (String) value);
        commit();
    }

    // For the DefaultButtons, learn about the actual metadata structure.
    MetadataEntry getEntryAt(int row) {
        return section.get(row);
    }

    URI getURIAt(int row) {
        return getEntryAt(row).getURI(meta);
    }

    // Write the current in-memory metadata back to its file.
    void commit() {
        ImageType type = meta.getImageType();
        try {
            type.writeMetadata(info);
        }
        catch (Throwable t) {
            // BadImageFileException, IOException, UnknownImageTypeException
            t.printStackTrace();
            // do nothing, hope the user figures it out
        }
    }
}
