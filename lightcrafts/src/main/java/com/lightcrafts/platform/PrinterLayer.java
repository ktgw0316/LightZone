/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.platform;

import java.awt.print.PageFormat;
import java.awt.print.PrinterException;

import com.lightcrafts.model.ImageEditor.ImageEditorEngine;
import com.lightcrafts.model.PrintSettings;
import com.lightcrafts.utils.thread.ProgressThread;

public interface PrinterLayer {

    void initialize();

    void dispose();

    void setPageFormat(PageFormat pageFormat);

    PageFormat getPageFormat();

    PageFormat pageDialog(PageFormat format);

    boolean printDialog();

    void setJobName(String name);

    void print( ImageEditorEngine engine, ProgressThread thread,
                PageFormat format, PrintSettings settings )
        throws PrinterException;

    void cancelPrint();
}
/* vim:set et sw=4 ts=4: */
