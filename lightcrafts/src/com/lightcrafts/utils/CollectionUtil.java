/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.utils;

import java.util.HashSet;
import java.util.Set;

/**
 * A <code>CollectionUtil</code> is a set of utility functions for collections.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class CollectionUtil {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Convert the given array into a {@link Set}.  Changes to elements in the
     * set &quot;write through&quot; to the original array.
     *
     * @param array The array to convert or <code>null</code> for none.
     * @return Returns a new {@link Set} or <code>null</code> if
     * <code>null</code> was given for the array argument.
     */
    public static <T> Set<T> asSet( T[] array ) {
        if ( array == null )
            return null;
        final HashSet<T> set = new HashSet<T>();
        for ( T element : array )
            set.add( element );
        return set;
    }

}
/* vim:set et sw=4 ts=4: */
