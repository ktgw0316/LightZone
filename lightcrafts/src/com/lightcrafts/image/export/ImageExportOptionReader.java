/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.export;

import java.io.IOException;

/**
 * An <code>ImageExportOptionReader</code> is used to read
 * {@link ImageExportOption}s.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public interface ImageExportOptionReader {

    /**
     * Read a {@link BooleanExportOption}.
     *
     * @param o The {@link BooleanExportOption} to read.
     */
    void read( BooleanExportOption o ) throws IOException;

    /**
     * Read an {@link IntegerExportOption}.
     *
     * @param o The {@link IntegerExportOption} to read.
     */
    void read( IntegerExportOption o ) throws IOException;

    /**
     * Read a {@link StringExportOption}.
     *
     * @param o The {@link StringExportOption} to read.
     */
    void read( StringExportOption o ) throws IOException;

}
/* vim:set et sw=4 ts=4: */
