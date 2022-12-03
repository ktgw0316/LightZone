/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image;

import com.lightcrafts.utils.LightCraftsException;

import java.io.File;

/**
 * A <code>BadImageFileException</code> is-a {@link LightCraftsException} for
 * reporting  bad images.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public class BadImageFileException extends LightCraftsException {

    /**
     * Construct a <code>BadImageFileException</code>.
     *
     * @param cause The original exception.
     */
    public BadImageFileException( Throwable cause ) {
        super( cause );
    }

    /**
     * Construct a <code>BadImageFileException</code>.
     *
     * @param file The bad image file.
     */
    public BadImageFileException( File file ) {
        super( file.getAbsolutePath() );
    }

    /**
     * Construct a <code>BadImageFileException</code>.
     *
     * @param file The bad image file.
     * @param message An informational message.
     */
    public BadImageFileException( File file, String message ) {
        super(
            file.getAbsolutePath() + (message != null ? ": " + message : "")
        );
    }

    /**
     * Construct a <code>BadImageFileException</code>.
     *
     * @param file The bad image file.
     * @param cause The original exception.
     */
    public BadImageFileException( File file, Throwable cause ) {
        super( file.getAbsolutePath(), cause );
    }

    /**
     * Construct a <code>BadImageFileException</code>.
     *
     * @param file The bad image file.
     * @param message An informational message.
     * @param cause The original exception.
     */
    public BadImageFileException( File file, String message, Throwable cause ) {
        super(
            file.getAbsolutePath() + (message != null ? ": " + message : ""),
            cause
        );
    }

}
/* vim:set et sw=4 ts=4: */
