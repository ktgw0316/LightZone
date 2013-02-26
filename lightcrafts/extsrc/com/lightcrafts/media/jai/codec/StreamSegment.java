/*
 * $RCSfile: StreamSegment.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:55:33 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.codec;

/**
 * A utility class representing a segment within a stream as a
 * <code>long</code> starting position and an <code>int</code>
 * length.
 *
 * <p><b> This class is not a committed part of the JAI API.  It may
 * be removed or changed in future releases of JAI.</b>
 */
public class StreamSegment {
    
    private long startPos = 0L;
    private int segmentLength = 0;

    /**
     * Constructs a <code>StreamSegment</code>.
     * The starting position and length are set to 0.
     */
    public StreamSegment() {}

    /**
     * Constructs a <code>StreamSegment</code> with a
     * given starting position and length.
     */
    public StreamSegment(long startPos, int segmentLength) {
        this.startPos = startPos;
        this.segmentLength = segmentLength;
    }

    /**
     * Returns the starting position of the segment.
     */
    public final long getStartPos() {
        return startPos;
    }

    /**
     * Sets the starting position of the segment.
     */
    public final void setStartPos(long startPos) {
        this.startPos = startPos;
    }

    /**
     * Returns the length of the segment.
     */
    public final int getSegmentLength() {
        return segmentLength;
    }

    /**
     * Sets the length of the segment.
     */
    public final void setSegmentLength(int segmentLength) {
        this.segmentLength = segmentLength;
    }
}
