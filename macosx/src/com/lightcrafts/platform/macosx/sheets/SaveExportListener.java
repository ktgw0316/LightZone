/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.platform.macosx.sheets;

import com.lightcrafts.image.export.ImageExportOptions;

/**
 * An <code>SaveExportListener</code> listens for events from native code that
 * has displayed a Mac&nbsp;OS&nbsp;X save/export &quot;sheet&quot; to the
 * user.  It's based on sample code provided by Apple Computer.
 * <p>
 * @see <a href="http://developer.apple.com/samplecode/JSheets/">JSheets
 * sample code</a>.
 */
interface SaveExportListener {

    /**
     * This is called when the user clicks Cancel.
     */
    void sheetCancelled();

    /**
     * This is called when the user clicks OK.
     *
     * @param filename The name of the file specified by the user.
     * @param exportOptions The user-specified {@link ImageExportOptions} or
     * <code>null</code> if none.
     */
    void sheetOK( String filename, ImageExportOptions exportOptions );

}
/* vim:set et sw=4 ts=4: */
