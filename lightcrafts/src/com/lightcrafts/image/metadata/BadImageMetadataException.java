/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata;

import java.io.File;

import com.lightcrafts.image.BadImageFileException;

/**
 * A <code>BadImageMetadataException</code> is-a {@link BadImageFileException}
 * for reporting bad image metadata.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class BadImageMetadataException extends BadImageFileException {

    /**
     * Construct a <code>BadImageMetadataException</code>.
     *
     * @param file The image file containing bad metadata.
     */
    public BadImageMetadataException( File file ) {
        super( file );
    }

    /**
     * Construct a <code>BadImageMetadataException</code>.
     *
     * @param file The image file containing bad metadata.
     * @param message An informational message.
     */
    public BadImageMetadataException( File file, String message ) {
        super( file, message );
    }

    /**
     * Construct a <code>BadImageMetadataException</code>.
     *
     * @param file The image file containing bad metadata.
     * @param cause The original exception.
     */
    public BadImageMetadataException( File file, Throwable cause ) {
        super( file, cause );
    }

    /**
     * Construct a <code>BadImageMetadataException</code>.
     *
     * @param file The image file containing bad metadata.
     * @param message An informational message.
     * @param cause The original exception.
     */
    public BadImageMetadataException( File file, String message,
                                      Throwable cause ) {
        super( file, message, cause );
    }

}
/* vim:set et sw=4 ts=4: */
