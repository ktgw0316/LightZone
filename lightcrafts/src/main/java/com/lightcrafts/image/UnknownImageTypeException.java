/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image;

import com.lightcrafts.utils.LightCraftsException;

/**
 * A <code>UnknownImageTypeException</code> is-a {@link LightCraftsException}
 * for reporting unknown image file types.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public class UnknownImageTypeException extends LightCraftsException {

    /**
     * Construct a <code>UnknownImageTypeException</code>.
     *
     * @param msg The message (should probably be the file name).
     */
    public UnknownImageTypeException( String msg ) {
        super( msg );
    }

}
/* vim:set et sw=4 ts=4: */
