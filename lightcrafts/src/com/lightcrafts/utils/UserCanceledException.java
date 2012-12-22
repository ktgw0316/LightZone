/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.utils;

/**
 * A <code>UserCanceledException</code> is-a {@link LightCraftsException} that
 * used to signal when the user manually cancels an operation, e.g., opening a
 * large image.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public class UserCanceledException extends LightCraftsException {

    /**
     * Construct a <code>UserCanceledException</code>.
     */
    public UserCanceledException() {
        super( "User canceled." );
    }

    /**
     * Construct a <code>UserCanceledException</code>.
     *
     * @param message The detail message.
     */
    public UserCanceledException( String message ) {
        super( message );
    }

    /**
     * Construct a <code>UserCanceledException</code>.
     *
     * @param cause The original exception.
     */
    public UserCanceledException( Throwable cause ) {
        super( cause );
    }

    /**
     * Construct a <code>UserCanceledException</code>.
     *
     * @param message The detail message.
     * @param cause The original exception.
     */
    public UserCanceledException( String message, Throwable cause ) {
        super( message, cause );
    }

}
/* vim:set et sw=4 ts=4: */
