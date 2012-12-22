/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image;

/**
 * An <code>UnsupportedColorProfileException</code> is-a
 * {@link ColorProfileException} for reporting unsupported ICC profiles.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public class UnsupportedColorProfileException extends ColorProfileException {

    /**
     * Construct an <code>UnsupportedColorProfileException</code>.
     *
     * @param message The detail message.
     */
    public UnsupportedColorProfileException( String message ) {
        super( message );
    }

}
/* vim:set et sw=4 ts=4: */
