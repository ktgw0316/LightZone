/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.utils.cache;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Iterator;

import com.lightcrafts.utils.bytebuffer.ByteBufferAllocator;

/**
 * An <code>LRUCacheObjectMap</code> is-a {@link CacheObjectMap} that uses a
 * least-recently-used (LRU) heuristic to determine when object should be
 * cached to disk.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class LRUCacheObjectMap implements CacheObjectMap {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Construct an <code>LRUCacheObjectMap</code>.
     *
     * @param bufAlloc The {@link ByteBufferAllocator} to use.
     * @param maxSize The maximum total amount of memory (in bytes) the
     * in-memory cache may use.
     */
    public LRUCacheObjectMap( ByteBufferAllocator bufAlloc, long maxSize ) {
        //
        // The initial capacity and load factor values below are the values
        // used as defaults for Java library maps.
        //
        this( bufAlloc, maxSize, 16, 0.75F );
    }

    /**
     * Construct an <code>LRUCacheObjectMap</code>.
     *
     * @param bufAlloc The {@link ByteBufferAllocator} to use.
     * @param maxSize The maximum total amount of memory (in bytes) the
     * in-memory cache may use.
     * @param initialCapacity The initial capacity.
     * @param loadFactor The load factor.
     */
    public LRUCacheObjectMap( ByteBufferAllocator bufAlloc, long maxSize,
                              int initialCapacity, float loadFactor ) {
        m_bufAlloc = bufAlloc;
        m_lruMap = new LRUHashMap<Object, ByteBuffer>( initialCapacity, loadFactor );
        m_maxSize = maxSize;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void clear() {
        m_bufAlloc.clear();
        m_lruMap.clear();
    }

    /**
     * {@inheritDoc}
     */
    public synchronized boolean contains( Object key ) {
        return m_lruMap.containsKey( key ) || m_cache.storeContains( key );
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void dispose() {
        if ( m_bufAlloc != null ) {
            m_bufAlloc.dispose();
            m_bufAlloc = null;
        }
        m_lruMap.clear();
    }

    /**
     * {@inheritDoc}
     */
    public Object getOnce( Object key, Object aux ) throws IOException {
        if ( key == null )
            throw new IllegalArgumentException();
        final ByteBuffer buf;
        synchronized ( this ) {
            buf = m_lruMap.remove( key );
        }
        if ( buf != null ) {
            final CacheObjectBroker broker = m_cache.getCacheObjectBroker();
            buf.position( 0 );
            return broker.decodeFromByteBuffer( buf, aux );
        }
        return m_cache.readFromStore( key, aux );
    }

    /**
     * {@inheritDoc}
     */
    public void put( Object key, Object obj ) {
        if ( key == null || obj == null )
            throw new IllegalArgumentException();
        final CacheObjectBroker broker = m_cache.getCacheObjectBroker();
        final int objSize = broker.getEncodedSizeOf( obj );
        while ( true ) {
            final ByteBuffer buf;
            try {
                buf = m_bufAlloc.allocByteBuffer( objSize );
            }
            catch ( OutOfMemoryError e ) {
                if ( spillOneObject() )
                    continue;
                throw e;
            }
            broker.encodeToByteBuffer( buf, obj );
            synchronized ( this ) {
                m_lruMap.put( key, buf );
                m_curSize += objSize;
            }
            break;
        }
    }

    /**
     * {@inheritDoc}
     */
    public synchronized boolean remove( Object key ) {
        if ( key == null )
            throw new IllegalArgumentException();
        final ByteBuffer buf = m_lruMap.remove( key );
        if ( buf != null ) {
            m_curSize -=  buf.limit();
            final boolean freed = m_bufAlloc.freeByteBuffer( buf );
            assert freed;
            return true;
        }
        return m_cache.removeFromStore( key );
    }

    /**
     * {@inheritDoc}
     */
    public void setCache( Cache cache ) {
        m_cache = cache;
    }

    ////////// protected //////////////////////////////////////////////////////

    protected void finalize() throws Throwable {
        dispose();
        super.finalize();
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * An <code>LRUHashMap</code> is-a {@link LinkedHashMap} that overrides
     * {@link #removeEldestEntry(Map.Entry)} to define a policy for removing
     * objects from the map.
     */
    private final class LRUHashMap<K, V extends ByteBuffer> extends LinkedHashMap<K,V> {

        /**
         * Construct an <code>LRUHashMap</code> passing <code>true</code> for
         * the <code>accessOrder</code> parameter so that iterating over the
         * contents of the map will be in least-recently-used order.
         *
         * @param initialCapacity The initial capacity.
         * @param loadFactor The load factor.
         */
        LRUHashMap( int initialCapacity, float loadFactor ) {
            super( initialCapacity, loadFactor, true );
        }

        /**
         * If the current total amount of memory being used by all the objects
         * in the map exceeds the maximum allowed size, remove the eldest
         * objects.
         *
         * @param notUsed The eldest map entry.
         * @return Always returns <code>false</code> since we remove entries
         * ourselves.
         */
        @Override
        protected boolean removeEldestEntry( Map.Entry notUsed ) {
            synchronized ( LRUCacheObjectMap.this ) {
                if ( m_curSize <= m_maxSize )
                    return false;
                for ( Iterator<Map.Entry<K,V>> i = entrySet().iterator();
                      i.hasNext() && m_curSize > m_maxSize; ) {
                    final Map.Entry<K,V> me = i.next();
                    spill( me.getKey(), me.getValue() );
                    i.remove();
                }
            }
            return false;
        }

    }

    /**
     * Spill a {@link ByteBuffer} to the {@link CacheStore}.
     *
     * @param key The object's key.
     * @param buf The {@link ByteBuffer} to spill.
     */
    private void spill( Object key, ByteBuffer buf ) {
        try {
            m_cache.writeToStore( key, buf );
        }
        catch ( IOException e ) {
            throw new CacheIOException( e );
        }
        m_curSize -= buf.limit();
        final boolean freed = m_bufAlloc.freeByteBuffer( buf );
        assert freed;
    }

    /**
     * Spill one map entry.
     *
     * @return Returns <code>true</code> only if a map entry was spilled.
     */
    private synchronized boolean spillOneObject() {
        final Iterator<Map.Entry<Object,ByteBuffer>> i =
            m_lruMap.entrySet().iterator();
        if ( i.hasNext() ) {
            final Map.Entry<Object,ByteBuffer> me = i.next();
            spill( me.getKey(), me.getValue() );
            i.remove();
            return true;
        }
        return false;
    }

    /**
     * The {@link ByteBufferAllocator} in use.
     */
    private ByteBufferAllocator m_bufAlloc;

    /**
     * The {@link Cache} using this <code>LRUHashMap</code>.
     */
    private Cache m_cache;

    /**
     * The current total amount of memory (in bytes) all the objects in the map
     * are using.
     */
    private long m_curSize;

    /**
     * The map of all cached objects.  The key is the object's key and the
     * value is a {@link ByteBuffer} containing its encoded representation.
     */
    private final Map<Object,ByteBuffer> m_lruMap;

    /**
     * The maximum total amount of memory (in bytes) the sum of the sizes of
     * all objects may not exceed.
     */
    private final long m_maxSize;
}
/* vim:set et sw=4 ts=4: */
