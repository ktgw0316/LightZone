/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image;

import com.lightcrafts.utils.LightCraftsException;

/**
 * A <code>ColorProfileException</code> is-a {@link LightCraftsException} for
 * reporting problems with color profiles.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public abstract class ColorProfileException extends LightCraftsException {

    /**
     * Construct a <code>ColorProfileException</code>.
     *
     * @param message The detail message.
     */
    protected ColorProfileException( String message ) {
        super( message );
    }

}
/* vim:set et sw=4 ts=4: */
