/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.utils.cache;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * A <code>WriteThroughCacheObjectMap</code> is-a {@link CacheObjectMap} that
 * always writes the objects to disk immediately.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class WriteThroughCacheObjectMap implements CacheObjectMap {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * {@inheritDoc}
     */
    public void clear() {
        // nothing to do
    }

    /**
     * {@inheritDoc}
     */
    public boolean contains( Object key ) {
        return m_cache.storeContains( key );
    }

    /**
     * {@inheritDoc}
     */
    public void dispose() {
        // nothing to do
    }

    /**
     * Gets an object from the map (from the disk).
     *
     * @param key The object's key.
     * @param aux An auxiliary object.
     * @return Returns the object having the given key or <code>null</code> if
     * there is no such object.
     */
    public Object getOnce( Object key, Object aux ) throws IOException {
        if ( key == null )
            throw new IllegalArgumentException();
        return m_cache.readFromStore( key, aux );
    }

    /**
     * Puts an object into the map (and onto disk).
     *
     * @param key The object's key.
     * @param obj The object to put.
     */
    public void put( Object key, Object obj ) throws IOException {
        if ( key == null || obj == null )
            throw new IllegalArgumentException();
        final CacheObjectBroker broker = m_cache.getCacheObjectBroker();
        final CacheStore store = m_cache.getCacheStore();
        final int objSize = broker.getEncodedSizeOf( obj );
        final ByteBuffer buf = store.getByteBuffer( objSize );
        broker.encodeToByteBuffer( buf, obj );
        m_cache.writeToStore( key, buf );
    }

    /**
     * Removes an object from the map (and disk).
     *
     * @param key The object's key.
     * @return Returns <code>true</code> only if the object was in the map and
     * removed.
     */
    public boolean remove( Object key ) {
        if ( key == null )
            throw new IllegalArgumentException();
        return m_cache.removeFromStore( key );
    }

    /**
     * {@inheritDoc}
     */
    public void setCache( Cache cache ) {
        m_cache = cache;
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * The {@link Cache} using this
     * <code>WriteThroughCacheObjectMap</code>.
     */
    private Cache m_cache;
}
/* vim:set et sw=4 ts=4: */
