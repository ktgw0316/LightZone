/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.browser.view;

import com.lightcrafts.ui.browser.model.ImageDatum;

import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Collection;

/**
 * A Transferable for exporting ImageDatums as lists of files and as
 * path strings. 
 */
class ImageDatumTransferable implements Transferable {

    private List<ImageDatum> datums;

    ImageDatumTransferable(Collection<ImageDatum> datums) {
        this.datums = new LinkedList<ImageDatum>(datums);
    }

    public DataFlavor[] getTransferDataFlavors() {
        return new DataFlavor[] {
            DataFlavor.javaFileListFlavor, DataFlavor.stringFlavor
        };
    }

    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return DataFlavor.javaFileListFlavor.equals(flavor) ||
               DataFlavor.stringFlavor.equals(flavor);
    }

    public Object getTransferData(DataFlavor flavor)
        throws UnsupportedFlavorException, IOException
    {
        List<File> files = new LinkedList<File>();
        for (ImageDatum datum : datums) {
            File file = datum.getFile();
            files.add(file);
        }
        if (DataFlavor.javaFileListFlavor.equals(flavor)) {
            return files;
        }
        if (DataFlavor.stringFlavor.equals(flavor)) {
            StringBuffer buffer = new StringBuffer();
            for (File file : files) {
                String path = file.getAbsolutePath();
                buffer.append(path);
                buffer.append(" ");
            }
            return buffer.toString();
        }
        throw new UnsupportedFlavorException(flavor);
    }

    List<ImageDatum> getImageDatums() {
        return new LinkedList<ImageDatum>(datums);
    }
}
