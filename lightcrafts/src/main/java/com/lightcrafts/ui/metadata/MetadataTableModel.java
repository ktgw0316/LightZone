/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.metadata;

import com.lightcrafts.image.metadata.ImageMetadataDirectory;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;

/** Provide a basic two-column, read-only table model containing key-value
  * pairs for all metadata tags in a File.
  */

class MetadataTableModel extends AbstractTableModel {

    private final static String[] ColumnNames = {"Key", "Value", "ID"};

    private final static int KeyColumn = 0;
    private final static int ValueColumn = 1;
    private final static int IdColumn = 2;

    private ArrayList<KeyValuePair> pairs;

    private boolean showIDs;    // Add a column for key ID numbers

    MetadataTableModel(
        ImageMetadataDirectory dir, boolean filter, boolean sort, boolean showIDs
    ) {
        this.showIDs = showIDs;
        pairs = new ArrayList<>();
        MetadataDirectoryModel dirModel =
            new MetadataDirectoryModel(dir, filter, sort);
        pairs.addAll(dirModel.getPairs());
    }

    public String getColumnName(int col) {
        return ColumnNames[col];
    }

    public int getRowCount() {
        return pairs.size();
    }

    public int getColumnCount() {
        return showIDs ? ColumnNames.length : (ColumnNames.length - 1);
    }

    public Object getValueAt(int row, int col) {
        KeyValuePair pair = pairs.get(row);
        if (col == KeyColumn) {
            return pair.key;
        }
        if (col == ValueColumn) {
            return pair.value;
        }
        if ((col == IdColumn) && (showIDs)) {
            int id = pair.tagID;
            return hexStringOf(id);
        }
        return null;
    }

    public boolean isCellEditable(int row, int col) {
        return false;
    }

    // This method is copied from ImageMetadataDirectory.hexStringOf():
    private static String hexStringOf(int tagID) {
        String hex = Integer.toHexString(tagID).toUpperCase();
        final int length = hex.length();
        if (length < 4) {
            hex = "000".substring(length - 1) + hex;
        }
        return hex;
    }
}
