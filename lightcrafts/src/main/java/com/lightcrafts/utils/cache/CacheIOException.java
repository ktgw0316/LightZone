/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.utils.cache;

/**
 * A <code>CacheIOException</code> is-a {@link RuntimeException} that is
 * used to be able to throw an <code>IOException</code> from within methods
 * that aren't declared to throw them.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class CacheIOException extends RuntimeException {

    /**
     * Construct a <code>CacheIOException</code>.
     *
     * @param cause The real <code>IOException</code>.
     */
    public CacheIOException( Exception cause ) {
        super( cause );
    }

}
/* vim:set et sw=4 ts=4: */
