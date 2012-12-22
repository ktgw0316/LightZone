/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.utils;

/*
 * Facets - A Web Application Framework
 * Copyright (c) 2005 Tom Bradford
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 * $Id: SoftValueHashMap.java,v 1.1.1.1 2005/07/07 16:02:39 bradford Exp $
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
public final class SoftValueHashMap extends AbstractMap {

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

    public Set entrySet() {
        throw new UnsupportedOperationException();
    }

    public Object get( Object key ) {
        Object value = null;
        final Reference valueRef = (Reference)m_map.get( key );
        if ( valueRef != null ) {
            value = valueRef.get();
            if ( value == null )
                m_map.remove( key );
        }
        return value;
    }

    public Object put( Object key, Object value ) {
        processQueue();
        final Object newValueRef = new ValueReference( key, value, m_refQueue );
        final Reference oldValueRef = (Reference)m_map.put( key, newValueRef );
        return oldValueRef != null ? oldValueRef.get() : null;
    }

    public Object remove( Object key ) {
        processQueue();
        return m_map.remove( key );
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
    private static final class ValueReference extends SoftReference {
        ValueReference( Object key, Object referrent, ReferenceQueue q ) {
            super( referrent, q );
            m_key = key;
        }
        private final Object m_key;
    }

    /**
     * Process the reference queue: for each {@link ValueReference} whose
     * referrent has been reclaimed, remove that {@link ValueReference}'s key
     * from the map.
     */
    private void processQueue() {
        while ( true ) {
            final ValueReference valueRef = (ValueReference)m_refQueue.poll();
            if ( valueRef == null )
                return;
            m_map.remove( valueRef.m_key );
        }
    }

    private final Map m_map = new HashMap();
    private final ReferenceQueue m_refQueue = new ReferenceQueue();
}
/* vim:set et sw=4 ts=4: */
