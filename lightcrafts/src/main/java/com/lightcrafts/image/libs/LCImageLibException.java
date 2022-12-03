/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.libs;

import com.lightcrafts.utils.LightCraftsException;

/**
 * An <code>LCImageLibException</code> is-an {@link Exception} for reporting exceptions from Light
 * Crafts image libraries.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class LCImageLibException extends LightCraftsException {

    /**
     * Construct an <code>LCImageLibException</code>.
     *
     * @param cause The original exception.
     */
    public LCImageLibException(Throwable cause) {
        super(cause);
    }

    /**
     * Construct an <code>LCImageLibException</code>.
     *
     * @param message The detail message.
     */
    public LCImageLibException(String message) {
        super(message);
    }
}
/* vim:set et sw=4 ts=4: */
