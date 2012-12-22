/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.types;

import java.nio.ByteBuffer;

/**
 * An <code>OrJPEGSegmentFilter</code> is-a {@link JPEGSegmentFilter} that
 * combines the results of two other {@link JPEGSegmentFilter}s.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public class OrJPEGSegmentFilter implements JPEGSegmentFilter {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Construct an <code>OrJPEGSegmentFilter</code>.
     *
     * @param filter1 The first {@link JPEGSegmentFilter}.
     * @param filter2 The second {@link JPEGSegmentFilter}.
     */
    public OrJPEGSegmentFilter( JPEGSegmentFilter filter1,
                                JPEGSegmentFilter filter2 ) {
        m_filter1 = filter1;
        m_filter2 = filter2;
    }

    /**
     * {@inheritDoc}
     */
    public boolean accept( byte segID, ByteBuffer buf ) {
        return m_filter1.accept( segID, buf ) || m_filter2.accept( segID, buf );
    }

    ////////// private ////////////////////////////////////////////////////////

    private final JPEGSegmentFilter m_filter1, m_filter2;
}
/* vim:set et sw=4 ts=4: */
