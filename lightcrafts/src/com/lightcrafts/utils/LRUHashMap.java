/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.utils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * An <code>LRUHashMap&lt;K,V&gt;</code> is-a {@link LinkedHashMap&lt;K,V&gt;}
 * that implements a least-recently-used hash map.
 *
 * @author Fabio Riccardi [fabio@lightcrafts.com]
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public class LRUHashMap<K,V> extends LinkedHashMap<K,V> {

    ////////// public /////////////////////////////////////////////////////////

    public LRUHashMap( int maxEntries ) {
        m_maxEntries = maxEntries;
    }

    public LRUHashMap( int initialCapacity, int maxEntries ) {
        super( initialCapacity );
        m_maxEntries = maxEntries;
    }

    public LRUHashMap( int initialCapacity, float loadFactor,
                       boolean accessOrder, int maxEntries ) {
        super( initialCapacity, loadFactor, accessOrder );
        m_maxEntries = maxEntries;
    }

    ////////// protected //////////////////////////////////////////////////////

    protected boolean removeEldestEntry( Map.Entry<K,V> eldest ) {
        return size() > m_maxEntries;
    }

    ////////// private ////////////////////////////////////////////////////////

    private final int m_maxEntries;
}
/* vim:set et sw=4 ts=4: */
