/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.types;

import java.nio.ByteBuffer;

/**
 * A <code>NotJPEGSegmentFilter</code> is-a {@link JPEGSegmentFilter} that
 * negates the result of another {@link JPEGSegmentFilter}.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public class NotJPEGSegmentFilter implements JPEGSegmentFilter {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Construct a <code>NotJPEGSegmentFilter</code>.
     *
     * @param filter The {@link JPEGSegmentFilter} to negate.
     */
    public NotJPEGSegmentFilter( JPEGSegmentFilter filter ) {
        m_filter = filter;
    }

    /**
     * {@inheritDoc}
     */
    public boolean accept( byte segID, ByteBuffer buf ) {
        return !m_filter.accept( segID, buf );
    }

    ////////// private ////////////////////////////////////////////////////////

    private final JPEGSegmentFilter m_filter;
}
/* vim:set et sw=4 ts=4: */
