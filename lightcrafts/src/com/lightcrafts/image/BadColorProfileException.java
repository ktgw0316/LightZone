/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image;

/**
 * A <code>BadColorProfileException</code> is-a {@link ColorProfileException}
 * for reporting bad ICC profiles.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public class BadColorProfileException extends ColorProfileException {

    /**
     * Construct a <code>BadColorProfileException</code>.
     *
     * @param message The detail message.
     */
    public BadColorProfileException( String message ) {
        super( message );
    }

}
/* vim:set et sw=4 ts=4: */
