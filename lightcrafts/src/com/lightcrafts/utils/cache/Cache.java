/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.utils.cache;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * A <code>Cache</code> is like a {@link Map} but with two differences:
 *  <ul>
 *    <li>More objects than can fit into memory can be cached.
 *    <li>Unlike a map, once you get an object, it's no longer in the cache.
 *  </ul>
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class Cache {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Construct a <code>Cache</code>.
     *
     * @param objBroker The {@link CacheObjectBroker} to use.
     * @param objMap The {@link CacheObjectMap} to use.
     * @param store The {@link CacheStore} to use.
     * @param freeBlockMgr The {@link FreeBlockManager} to use.
     */
    public Cache( CacheObjectBroker objBroker, CacheObjectMap objMap,
                  CacheStore store, FreeBlockManager freeBlockMgr ) {
        m_blockMap = new HashMap<Object,CacheBlock>();
        m_freeBlockMgr = freeBlockMgr;
        m_objBroker = objBroker;
        m_objMap = objMap;
        m_objMap.setCache( this );
        m_store = store;
    }

    /**
     * Removes all objects from the cache.
     */
    public synchronized void clear() throws IOException {
        m_blockMap.clear();
        m_freeBlockMgr.clear();
        m_objMap.clear();
        m_store.clear();
    }

    /**
     * Checks whether the cache contains a particular object.
     *
     * @param key The object's key.
     * @return Returns <code>true</code> only if the object is in the cache.
     */
    public synchronized boolean contains( Object key ) {
        return m_objMap.contains( key );
    }

    /**
     * Disposes of this <code>Cache</code>.
     */
    public synchronized void dispose() throws IOException {
        m_objMap.dispose();
        m_store.dispose();
    }

    /**
     * Gets an object from the cache.  Once gotten, the object is removed.
     *
     * @param key The object's key.
     * @param aux An auxiliary object passed through to
     * {@link CacheObjectBroker#decodeFromByteBuffer(ByteBuffer, Object)}.
     * An implementation of {@link CacheObjectBroker} can use this object for
     * any purpose.
     * @return Returns the relevant object or <code>null</code> if there is no
     * object in the cache with the given key.
     */
    public Object getOnce( Object key, Object aux ) throws IOException {
        return m_objMap.getOnce( key, aux );
    }

    /**
     * Gets the {@link CacheObjectBroker} in use by the <code>Cache</code>.
     *
     * @return Returns said {@link CacheObjectBroker}.
     */
    public CacheObjectBroker getCacheObjectBroker() {
        return m_objBroker;
    }

    /**
     * Gets the {@link CacheObjectMap} in use by the <code>Cache</code>.
     *
     * @return Returns said {@link CacheObjectMap}.
     */
    public CacheObjectMap getCacheObjectMap() {
        return m_objMap;
    }

    /**
     * Gets the {@link CacheStore} in use.
     *
     * @return Returns said {@link CacheStore}.
     */
    public CacheStore getCacheStore() {
        return m_store;
    }

    /**
     * Gets the {@link FreeBlockManager} in use by the <code>Cache</code>.
     *
     * @return Returns said {@link CacheObjectMap}.
     */
    public FreeBlockManager getFreeBlockManager() {
        return m_freeBlockMgr;
    }

    /**
     * Puts an object into the cache.
     *
     * @param key The object's key.
     * @param obj The object to put.
     */
    public void put( Object key, Object obj ) throws IOException {
        m_objMap.put( key, obj );
    }

    /**
     * Removes an object from the cache.
     *
     * @param key The object's key.
     * @return Returns <code>true</code> only if the object was in the cache
     * and removed.
     */
    public boolean remove( Object key ) {
        return m_objMap.remove( key );
    }

    /**
     * Reads an object from the {@link CacheStore} being used.  This method is
     * meant to be called only by methods of classes that implement the
     * {@link CacheObjectMap} interface.
     *
     * @param key The object's key.
     * @param aux An auxiliary object passed through to
     * {@link CacheObjectBroker#decodeFromByteBuffer(ByteBuffer,Object)}.
     * An implementation of {@link CacheObjectBroker} can use this object for
     * any purpose.
     * @return Returns the read object.
     */
    public synchronized Object readFromStore( Object key, Object aux )
        throws IOException
    {
        final CacheBlock block = m_blockMap.remove( key );
        if ( block == null )
            return null;
        final ByteBuffer buf = m_store.getByteBuffer( block.getSize() );
        m_store.readFromStore( block.getPosition(), buf );
        final Object obj = m_objBroker.decodeFromByteBuffer( buf, aux );
        m_freeBlockMgr.freeBlock( block );
        return obj;
    }

    /**
     * Removes an object from the {@link CacheStore} being used.  This method
     * is meant to be called only by methods of classes that implement the
     * {@link CacheObjectMap} interface.
     *
     * @param key The object's key.
     * @return Returns <code>true</code> only if the object was removed.
     */
    public synchronized boolean removeFromStore( Object key ) {
        final CacheBlock block = m_blockMap.remove( key );
        if ( block != null ) {
            m_freeBlockMgr.freeBlock( block );
            return true;
        }
        return false;
    }

    /**
     * Checks whether the {@link CacheStore} contains a particular object.
     *
     * @param key The object's key.
     * @return Returns <code>true</code> only if the {@link CacheStore}
     * contains an object having the given key.
     */
    public synchronized boolean storeContains( Object key ) {
        return m_blockMap.containsKey( key );
    }

    /**
     * Writes and object to the {@link CacheStore} being used.  This method is
     * meant to be called only by methods of classes that implement the
     * {@link CacheObjectMap} interface.
     *
     * @param key The object's key.
     * @param buf The encoded object to write.
     */
    public synchronized void writeToStore( Object key, ByteBuffer buf )
        throws IOException
    {
        final int objSize = buf.limit();
        CacheBlock block = m_freeBlockMgr.findBlockOfSize( objSize );
        if ( block == null ) {
            //
            // There are no free blocks available: create a new one at the end
            // of the store.
            //
            // The calls to getSize() and writeToStore() must be together in a
            // synchronized block.
            //
            synchronized ( m_store ) {
                block = new CacheBlock( m_store.getSize(), objSize );
                m_store.writeToStore( block.getPosition(), buf );
            }
        } else
            m_store.writeToStore( block.getPosition(), buf );

        m_blockMap.put( key, block );
    }

    ////////// protected //////////////////////////////////////////////////////

    /**
     * Finalize a <code>Cache</code>.
     */
    protected void finalize() throws Throwable {
        super.finalize();
        dispose();
    }

    ////////// private ////////////////////////////////////////////////////////


    /**
     * A mapping for those objects that have been cached.  The key is the
     * object's key and the value is the relevant {@link CacheBlock}.
     */
    private final Map<Object,CacheBlock> m_blockMap;

    /**
     * The {@link FreeBlockManager} to use.
     */
    private final FreeBlockManager m_freeBlockMgr;

    /**
     * The {@link CacheObjectBroker} to use.
     */
    private final CacheObjectBroker m_objBroker;

    /**
     * The {@link CacheObjectMap} to use.
     */
    private final CacheObjectMap m_objMap;

    /**
     * The {@link CacheStore} to use.
     */
    private final CacheStore m_store;
}
/* vim:set et sw=4 ts=4: */
