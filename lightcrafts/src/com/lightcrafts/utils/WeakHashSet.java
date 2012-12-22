/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.utils;

import java.util.*;

/**
 * A <code>WeakHashSet</code> is just like {@link WeakHashMap} except it's a
 * {@link Set}.
 */
public class WeakHashSet<E> extends AbstractSet<E> {

    public WeakHashSet() {
        m_map = new WeakHashMap<E,Boolean>();
    }

    public WeakHashSet( Collection<E> c ) {
        m_map = new WeakHashMap<E,Boolean>(
            Math.max( (int)(c.size() / .75F) + 1, 16 )
        );
        //noinspection OverridableMethodCallInConstructor
        addAll( c );
    }

    public WeakHashSet( int initialCapacity ) {
        m_map = new WeakHashMap<E,Boolean>( initialCapacity );
    }

    public WeakHashSet( int initialCapacity, float loadFactor ) {
        m_map = new WeakHashMap<E,Boolean>( initialCapacity, loadFactor );
    }

    public boolean add( E o ) {
        return m_map.put( o, Boolean.TRUE ) == null;
    }

    public void clear() {
        m_map.clear();
    }

    public boolean contains( Object o ) {
        //noinspection SuspiciousMethodCalls
        return m_map.containsKey( o );
    }

    public boolean isEmpty() {
        return m_map.isEmpty();
    }

    public Iterator<E> iterator() {
        return m_map.keySet().iterator();
    }

    public boolean remove( Object o ) {
        //noinspection SuspiciousMethodCalls
        return m_map.remove( o ) == Boolean.TRUE;
    }

    public int size() {
        return m_map.size();
    }

    ////////// private ////////////////////////////////////////////////////////

    private final WeakHashMap<E,Boolean> m_map;
}
/* vim:set et sw=4 ts=4: */
