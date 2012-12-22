/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.utils.cache;

/**
 * A <code>CacheBlock</code> contains information about a contiguous block in
 * the cache: its position and size.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class CacheBlock {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Construct a <code>CacheBlock</code>.
     *
     * @param pos The block's position.
     * @param size The block's size.
     */
    public CacheBlock( long pos, int size ) {
        m_pos = pos;
        m_size = size;
    }

    /**
     * Gets the block's position.
     * @return Returns said position.
     */
    public long getPosition() {
        return m_pos;
    }

    /**
     * Gets the block's size.
     * @return Returns said size.
     */
    public int getSize() {
        return m_size;
    }

    /**
     * Set's the block's position.
     * @param newPos The new position.
     */
    public void setPosition( long newPos ) {
        m_pos = newPos;
    }

    /**
     * Set's the block's size.
     * @param newSize The new size.
     */
    public void setSize( int newSize ) {
        m_size = newSize;
    }

    ////////// private ////////////////////////////////////////////////////////

    private long m_pos;
    private int m_size;
}
/* vim:set et sw=4 ts=4: */
