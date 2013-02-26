/*
 * $RCSfile: StreamSegmentMapper.java,v $
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
 * An interface for use with the <code>SegmentedSeekableStream</code>
 * class.  An instance of the <code>StreamSegmentMapper</code>
 * interface provides the location and length of a segment of a source
 * <code>SeekableStream</code> corresponding to the initial portion of
 * a desired segment of the output stream.
 *
 * <p> As an example, consider a mapping between a source
 * <code>SeekableStream src</code> and a <code>SegmentedSeekableStream
 * dst</code> comprising bytes 100-149 and 200-249 of the source
 * stream.  The <code>dst</code> stream has a reference to an instance
 * <code>mapper</code> of <code>StreamSegmentMapper</code>.
 *
 * <p> A call to <code>dst.seek(0); dst.read(buf, 0, 10)</code> will
 * result in a call to <code>mapper.getStreamSegment(0, 10)</code>,
 * returning a new <code>StreamSegment</code> with a starting
 * position of 100 and a length of 10 (or less).  This indicates that
 * in order to read bytes 0-9 of the segmented stream, bytes 100-109
 * of the source stream should be read.
 *
 * <p> A call to <code>dst.seek(10); int nbytes = dst.read(buf, 0,
 * 100)</code> is somewhat more complex, since it will require data
 * from both segments of <code>src</code>.  The method <code>
 * mapper.getStreamSegment(10, 100)</code> will be called.  This
 * method will return a new <code>StreamSegment</code> with a starting
 * position of 110 and a length of 40 (or less).  The length is
 * limited to 40 since a longer value would result in a read past the
 * end of the first segment.  The read will stop after the first 40
 * bytes and an addition read or reads will be required to obtain the
 * data contained in the second segment.
 *
 * <p><b> This interface is not a committed part of the JAI API.  It may
 * be removed or changed in future releases of JAI.</b>
 */
public interface StreamSegmentMapper {

    /**
     * Returns a <code>StreamSegment</code> object indicating the
     * location of the initial portion of a desired segment in the
     * source stream.  The length of the returned
     * <code>StreamSegment</code> may be smaller than the desired
     * length.
     *
     * @param pos The desired starting position in the
     * <code>SegmentedSeekableStream</code>, as a <code>long</code>.
     * @param length The desired segment length.
     */
    StreamSegment getStreamSegment(long pos, int length);

    /**
     * Sets the values of a <code>StreamSegment</code> object
     * indicating the location of the initial portion of a desired
     * segment in the source stream.  The length of the returned
     * <code>StreamSegment</code> may be smaller than the desired
     * length.
     *
     * @param pos The desired starting position in the
     * <code>SegmentedSeekableStream</code>, as a <code>long</code>.
     * @param length The desired segment length.
     * @param seg A <code>StreamSegment</code> object to be overwritten.
     */
    void getStreamSegment(long pos, int length, StreamSegment seg);
}
