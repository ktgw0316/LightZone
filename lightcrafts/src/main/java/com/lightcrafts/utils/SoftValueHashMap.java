/* Copyright (C) 2014-2015 Masahiro Kitagawa */
/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.utils;

/*
 * This is a refactored version based on
 * Facets Web Application Framework by Tom Bradford, 2005
 * http://sourceforge.net/projects/facets/
 */

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * SoftValueHashMap is a volatile cache that utilizes SoftReferences of the
 * value itself rather than the key.  The benefit over a WeakHashMap
 * is that a WeakHashMap uses WeakReferences for keys.  Since keys are
 * often Strings, and Strings are often interned, there is the potential
 * that an entry will never be garbage collected.  Also, WeakReferences
 * are garbage collected too frequently for the type of volatile caching
 * for which SoftValueHashMap is intended.
 */
public final class SoftValueHashMap<K,V> extends AbstractMap<K,V> {

    ////////// public /////////////////////////////////////////////////////////

    public SoftValueHashMap() {
    }

    public void clear() {
        processQueue();
        m_map.clear();
    }

    public boolean containsKey( Object key ) {
        processQueue();
        return m_map.containsKey( key );
    }

    public Set<java.util.Map.Entry<K, V>> entrySet() {
        throw new UnsupportedOperationException();
    }

    public V get( Object key ) {
        V value = null;
        final Reference<V> valueRef = m_map.get( key );
        if ( valueRef != null ) {
            value = valueRef.get();
            if ( value == null )
                m_map.remove( key );
        }
        return value;
    }

    public V put( K key, V value ) {
        processQueue();
        final ValueReference<V> newValueRef = new ValueReference<V>( key, value, m_refQueue );
        final ValueReference<V> oldValueRef = m_map.put( key, newValueRef );
        return oldValueRef != null ? oldValueRef.get() : null;
    }

    public V remove( Object key ) {
        processQueue();
        ValueReference<V> result = m_map.remove( key );
        return result != null ? result.get() : null;
    }

    public int size() {
        processQueue();
        return m_map.size();
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * A <code>ValueReference</code> is-a {@link SoftReference} that also
     * contains a strong reference to the key for the value.  The key is used
     * to remove the key from the map when the value is reclaimed.
     */
    private static final class ValueReference<V> extends SoftReference<V> {
        ValueReference( Object key, V referrent, ReferenceQueue<V> q ) {
            super( referrent, q );
            m_key = key;
        }
        private final Object m_key;
    }

    /**
     * Process the reference queue: for each {@link ValueReference} whose
     * referent has been reclaimed, remove that {@link ValueReference}'s key
     * from the map.
     */
    @SuppressWarnings("unchecked")
    private void processQueue() {
        ValueReference<V> valueRef;
        while ( (valueRef = (ValueReference<V>) m_refQueue.poll()) != null )
            m_map.remove( valueRef.m_key );
    }

    private final Map<K, ValueReference<V>> m_map = new HashMap<K, ValueReference<V>>();
    private final ReferenceQueue<V> m_refQueue = new ReferenceQueue<V>();
}
/* vim:set et sw=4 ts=4: */
