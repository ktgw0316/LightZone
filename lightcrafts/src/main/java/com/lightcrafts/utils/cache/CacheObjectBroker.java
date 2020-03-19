/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.utils.cache;

import java.nio.ByteBuffer;

/**
 * A <code>CacheObjectBroker</code> knows how to deal with the actual objects
 * being written to and read from a cache, i.e., it knows how to convert a
 * given object to a stream of bytes and vice versa.  It also knows how to
 * determine the size of an object.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public interface CacheObjectBroker {

    /**
     * Decodes an object.
     *
     * @param buf The {@link ByteBuffer} to decode the object from.
     * @param aux An auxiliary object passed from
     * {@link Cache#getOnce(Object,Object)}.  An implementation of
     * {@link CacheObjectBroker} can use this object for any purpose.
     * @return Returns the object read from the cache.
     */
    Object decodeFromByteBuffer( ByteBuffer buf, Object aux );

    /**
     * Encodes an object.
     *
     * @param buf The {@link ByteBuffer} to encode the object to.
     * @param obj The object to encode.
     */
    void encodeToByteBuffer( ByteBuffer buf, Object obj );

    /**
     * Gets the encoded size of an object.
     *
     * @param obj The object to get the encoded size of.
     * @return Returns said size (in bytes).
     */
    int getEncodedSizeOf( Object obj );

}
/* vim:set et sw=4 ts=4: */
