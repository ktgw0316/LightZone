/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.utils.cache;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * A <code>CacheObjectMap</code> is a simplified version of a <code>Map</code>
 * for exclusive use with a {@link Cache}.  Aside from being simpler, some
 * methods throw {@link IOException} since disk access may be involved.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public interface CacheObjectMap {

    /**
     * Removes all objects from the map.
     */
    void clear();

    /**
     * Checks whether the map contains a particular object.
     *
     * @param key The object's key.
     * @return Returns <code>true</code> only if the map contains the object.
     */
    boolean contains( Object key );

    /**
     * Dispose of this <code>CacheObjectMap</code>.
     */
    void dispose();

    /**
     * Gets an object from the map.  Once gotten, the object is removed.
     *
     * @param key The object's key.
     * @param aux An auxiliary object passed through to
     * {@link CacheObjectBroker#decodeFromByteBuffer(ByteBuffer, Object)}.
     * An implementation of {@link CacheObjectBroker} can use this object for
     * any purpose.
     * @return Returns the object having the given key or <code>null</code> if
     * there is no such object.
     */
    Object getOnce( Object key, Object aux ) throws IOException;

    /**
     * Puts an object into the map.
     *
     * @param key The object's key.
     * @param obj The object to put.
     */
    void put( Object key, Object obj ) throws IOException;

    /**
     * Removes an object from the map.
     *
     * @param key The object's key.
     * @return Returns <code>true</code> only if the object was in the map and
     * removed.
     */
    boolean remove( Object key );

    /**
     * Sets the {@link Cache} that is using this <code>CacheObjectMap</code>.
     *
     * @param cache The {@link Cache} to use.
     */
    void setCache( Cache cache );

}
/* vim:set et sw=4 ts=4: */
