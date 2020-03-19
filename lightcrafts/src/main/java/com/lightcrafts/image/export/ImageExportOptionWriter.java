/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.export;

import java.io.IOException;

/**
 * An <code>ImageExportOptionWriter</code> is used to write
 * {@link ImageExportOption}s.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public interface ImageExportOptionWriter {

    /**
     * Write a {@link BooleanExportOption}.
     *
     * @param o The {@link BooleanExportOption} to write.
     */
    void write( BooleanExportOption o ) throws IOException;

    /**
     * Write an {@link IntegerExportOption}.
     *
     * @param o The {@link IntegerExportOption} to write.
     */
    void write( IntegerExportOption o ) throws IOException;

    /**
     * Write a {@link StringExportOption}.
     *
     * @param o The {@link StringExportOption} to write.
     */
    void write( StringExportOption o ) throws IOException;

}
/* vim:set et sw=4 ts=4: */
