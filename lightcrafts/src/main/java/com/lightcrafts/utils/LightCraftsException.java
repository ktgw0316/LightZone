/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.utils;

/**
 * A <code>LightCraftsException</code> is-an {@link Exception} that serves as
 * the base class for all Light Crafts' exceptions.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public abstract class LightCraftsException extends Exception {

    /**
     * Construct a <code>LightCraftsException</code>.
     *
     * @param message The detail message.
     */
    protected LightCraftsException( String message ) {
        super( message );
    }

    /**
     * Construct a <code>LightCraftsException</code>.
     *
     * @param cause The original exception.
     */
    protected LightCraftsException( Throwable cause ) {
        super( cause );
    }

    /**
     * Construct a <code>LightCraftsException</code>.
     *
     * @param message The detail message.
     * @param cause The original exception.
     */
    protected LightCraftsException( String message, Throwable cause ) {
        super( message, cause );
    }

}
/* vim:set et sw=4 ts=4: */
