/*
 * $RCSfile: SegmentedSeekableStream.java,v $
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
import java.io.IOException;


/**
 * An implementation of the <code>StreamSegmentMapper</code> interface
 * that requires an explicit list of the starting locations and
 * lengths of the source segments.
 */
class StreamSegmentMapperImpl implements StreamSegmentMapper {

    private long[] segmentPositions;

    private int[] segmentLengths;

    public StreamSegmentMapperImpl(long[] segmentPositions,
                                   int[] segmentLengths) {
        this.segmentPositions = (long[])segmentPositions.clone();
        this.segmentLengths = (int[])segmentLengths.clone();
    }

    public StreamSegment getStreamSegment(long position, int length) {
        int numSegments = segmentLengths.length;
        for (int i = 0; i < numSegments; i++) {
            int len = segmentLengths[i];
            if (position < len) {
                return new StreamSegment(segmentPositions[i] + position,
                                         Math.min(len - (int)position,
                                                  length));
            }
            position -= len;
        }

        return null;
    }

    public void getStreamSegment(long position, int length,
                                 StreamSegment seg) {
        int numSegments = segmentLengths.length;
        for (int i = 0; i < numSegments; i++) {
            int len = segmentLengths[i];
            if (position < len) {
                seg.setStartPos(segmentPositions[i] + position);
                seg.setSegmentLength(Math.min(len - (int)position, length));
                return;
            }
            position -= len;
        }

        seg.setStartPos(-1);
        seg.setSegmentLength(-1);
        return;
    }
}

/**
 * An implementation of the <code>StreamSegmentMapper</code> interface
 * for segments of equal length.
 */
class SectorStreamSegmentMapper implements StreamSegmentMapper {

    long[] segmentPositions;
    int segmentLength;
    int totalLength;
    int lastSegmentLength;

    public SectorStreamSegmentMapper(long[] segmentPositions,
                                     int segmentLength,
                                     int totalLength) {
        this.segmentPositions = (long[])segmentPositions.clone();
        this.segmentLength = segmentLength;
        this.totalLength = totalLength;
        this.lastSegmentLength = totalLength -
            (segmentPositions.length - 1)*segmentLength;
    }

    public StreamSegment getStreamSegment(long position, int length) {
        int index = (int) (position/segmentLength);

        // Compute segment length
        int len = (index == segmentPositions.length - 1) ?
            lastSegmentLength : segmentLength;

        // Compute position within the segment
        position -= index*segmentLength;

        // Compute maximum legal length
        len -= position;
        if (len > length) {
            len = length;
        }
        return new StreamSegment(segmentPositions[index] + position, len);
    }

    public void getStreamSegment(long position, int length,
                                 StreamSegment seg) {
        int index = (int) (position/segmentLength);

        // Compute segment length
        int len = (index == segmentPositions.length - 1) ?
            lastSegmentLength : segmentLength;

        // Compute position within the segment
        position -= index*segmentLength;

        // Compute maximum legal length
        len -= position;
        if (len > length) {
            len = length;
        }

        seg.setStartPos(segmentPositions[index] + position);
        seg.setSegmentLength(len);
    }
}

/**
 * A <code>SegmentedSeekableStream</code> provides a view of a
 * subset of another <code>SeekableStream</code> consiting of a series
 * of segments with given starting positions in the source stream and
 * lengths.  The resulting stream behaves like an ordinary
 * <code>SeekableStream</code>.
 *
 * <p> For example, given a <code>SeekableStream</code> containing
 * data in a format consisting of a number of sub-streams stored in
 * non-contiguous sectors indexed by a directory, it is possible to
 * construct a set of <code>SegmentedSeekableStream</code>s, one for
 * each sub-stream, that each provide a view of the sectors comprising
 * a particular stream by providing the positions and lengths of the
 * stream's sectors as indicated by the directory.  The complex
 * multi-stream structure of the original stream may be ignored by
 * users of the <code>SegmentedSeekableStream</code>, who see a
 * separate <code>SeekableStream</code> for each sub-stream and do not
 * need to understand the directory structure at all.
 *
 * <p> For further efficiency, a directory structure such as in the
 * example described above need not be fully parsed in order to build
 * a <code>SegmentedSeekableStream</code>.  Instead, the
 * <code>StreamSegmentMapper</code> interface allows the association
 * between a desired region of the output and an input segment to be
 * provided dynamically.  This mapping might be computed by reading
 * from a directory in piecemeal fashion in order to avoid consuming
 * memory resources.
 *
 * <p> It is the responsibility of the user of this class to determine
 * whether backwards seeking should be enabled.  If the source stream
 * supports only forward seeking, backwards seeking must be disabled
 * and the <code>StreamSegmentMapper</code> must be monotone; that is,
 * forward motion in the destination must always result in forward
 * motion within the source.  If the source stream supports backwards
 * seeking, there are no restrictions on the
 * <code>StreamSegmentMapper</code> and backwards seeking may always
 * be enabled for the <code>SegmentedSeekableStream</code>.
 *
 * <p><b> This class is not a committed part of the JAI API.  It may
 * be removed or changed in future releases of JAI.</b>
 */
public class SegmentedSeekableStream extends SeekableStream {

    private SeekableStream stream;
    private StreamSegmentMapper mapper;
    private long pointer = 0;
    private boolean canSeekBackwards;
    
    /**
     * Constructs a <code>SegmentedSeekableStream</code>
     * given a <code>SeekableStream</code> as input,
     * an instance of <code>StreamSegmentMapper</code>,
     * and a <code>boolean</code> indicating whether the
     * output <code>SegmentedSeekableStream</code> should
     * support seeking backwards.  If <code>canSeekBackwards</code>
     * is <code>true</code>, the source stream must itself
     * support seeking backwards.
     *
     * @param stream A source <code>SeekableStream</code>
     * @param mapper An instance of the <code>StreamSegmentMapper</code>
     *        interface.
     * @param canSeekBackwards <code>true</code> if the ability to
     *        seek backwards is desired.   
     */
    public SegmentedSeekableStream(SeekableStream stream,
                                   StreamSegmentMapper mapper,
                                   boolean canSeekBackwards) {
        this.stream = stream;
        this.mapper = mapper;
        this.canSeekBackwards = canSeekBackwards;
 
        if (canSeekBackwards && !stream.canSeekBackwards()) {
            throw new IllegalArgumentException(JaiI18N.getString("SegmentedSeekableStream0"));
        }
    }

    /**
     * Constructs a <code>SegmentedSeekableStream</code> given a
     * <code>SeekableStream</code> as input, a list of the starting
     * positions and lengths of the segments of the source stream, and
     * a <code>boolean</code> indicating whether the output
     * <code>SegmentedSeekableStream</code> should support seeking
     * backwards.  If <code>canSeekBakckwards</code> is
     * <code>true</code>, the source stream must itself support
     * seeking backwards.
     *
     * @param stream A source <code>SeekableStream</code>
     * @param segmentPositions An array of <code>long</code>s 
     *        giving the starting positions of the segments in the
     *        source stream.
     * @param segmentLengths  An array of <code>int</code>s 
     *        giving the lengths of segments in the source stream.
     * @param canSeekBackwards <code>true</code> if the ability to
     *        seek backwards is desired.
     */
    public SegmentedSeekableStream(SeekableStream stream,
                                   long[] segmentPositions,
                                   int[] segmentLengths,
                                   boolean canSeekBackwards) {
        this(stream,
            new StreamSegmentMapperImpl(segmentPositions, segmentLengths),
            canSeekBackwards);
    }

    /**
     * Constructs a <code>SegmentedSeekableStream</code> given a
     * <code>SeekableStream</code> as input, a list of the starting
     * positions of the segments of the source stream, the common
     * length of each segment, the total length of the segments and
     * a <code>boolean</code> indicating whether the output
     * <code>SegmentedSeekableStream</code> should support seeking
     * backwards.  If <code>canSeekBakckwards</code> is
     * <code>true</code>, the source stream must itself support
     * seeking backwards.
     *
     * <p> This constructor is useful for selecting substreams
     *     of sector-oriented file formats in which each segment
     *     of the substream (except possibly the final segment)
     *     occupies a fixed-length sector.
     *
     * @param stream A source <code>SeekableStream</code>
     * @param segmentPositions An array of <code>long</code>s 
     *        giving the starting positions of the segments in the
     *        source stream.
     * @param segmentLength  The common length of each segment.
     * @param totalLength  The total length of the source segments.
     * @param canSeekBackwards <code>true</code> if the ability to
     *        seek backwards is desired.
     */
    public SegmentedSeekableStream(SeekableStream stream,
                                   long[] segmentPositions,
                                   int segmentLength,
                                   int totalLength,
                                   boolean canSeekBackwards) {
        this(stream,
             new SectorStreamSegmentMapper(segmentPositions,
                                           segmentLength,
                                           totalLength),
             canSeekBackwards);
    }

    /**
     * Returns the current offset in this stream.
     *
     * @return     the offset from the beginning of the stream, in bytes,
     *             at which the next read occurs.
     */
    public long getFilePointer() {
        return (long)pointer;
    }

    /**
     * Returns <code>true</code> if seeking backwards is supported.
     * Support is determined by the value of the
     * <code>canSeekBackwards</code> parameter at construction time.
     */
    public boolean canSeekBackwards() {
        return canSeekBackwards;
    }

    /**
     * Sets the offset, measured from the beginning of this 
     * stream, at which the next read occurs.
     *
     * <p> If <code>canSeekBackwards()</code> returns <code>false</code>,
     * then setting <code>pos</code> to an offset smaller than
     * the current value of <code>getFilePointer()</code> will have
     * no effect.
     *
     * @param      pos   the offset position, measured in bytes from the 
     *                   beginning of the stream, at which to set the stream 
     *                   pointer.
     * @exception  IOException  if <code>pos</code> is less than 
     *                          <code>0</code> or if an I/O error occurs.
     */
    public void seek(long pos) throws IOException {
        if (pos < 0) {
            throw new IOException();
        }
        pointer = pos;
    }

    private StreamSegment streamSegment = new StreamSegment();
    
    /**
     * Reads the next byte of data from the input stream. The value byte is
     * returned as an <code>int</code> in the range <code>0</code> to
     * <code>255</code>. If no byte is available because the end of the stream
     * has been reached, the value <code>-1</code> is returned. This method
     * blocks until input data is available, the end of the stream is detected,
     * or an exception is thrown.
     *
     * @return     the next byte of data, or <code>-1</code> if the end of the
     *             stream is reached.
     * @exception  IOException  if an I/O error occurs.
     */
    public int read() throws IOException {
        mapper.getStreamSegment(pointer, 1, streamSegment);
        stream.seek(streamSegment.getStartPos());

        int val = stream.read();
        ++pointer;
        return val;
    }

    /**
     * Reads up to <code>len</code> bytes of data from the input stream into
     * an array of bytes.  An attempt is made to read as many as
     * <code>len</code> bytes, but a smaller number may be read, possibly
     * zero. The number of bytes actually read is returned as an integer.
     *
     * <p> This method blocks until input data is available, end of stream is
     * detected, or an exception is thrown.
     *
     * <p> If <code>b</code> is <code>null</code>, a
     * <code>NullPointerException</code> is thrown.
     *
     * <p> If <code>off</code> is negative, or <code>len</code> is negative, or
     * <code>off+len</code> is greater than the length of the array
     * <code>b</code>, then an <code>IndexOutOfBoundsException</code> is
     * thrown.
     *
     * <p> If <code>len</code> is zero, then no bytes are read and
     * <code>0</code> is returned; otherwise, there is an attempt to read at
     * least one byte. If no byte is available because the stream is at end of
     * stream, the value <code>-1</code> is returned; otherwise, at least one
     * byte is read and stored into <code>b</code>.
     *
     * <p> The first byte read is stored into element <code>b[off]</code>, the
     * next one into <code>b[off+1]</code>, and so on. The number of bytes read
     * is, at most, equal to <code>len</code>. Let <i>k</i> be the number of
     * bytes actually read; these bytes will be stored in elements
     * <code>b[off]</code> through <code>b[off+</code><i>k</i><code>-1]</code>,
     * leaving elements <code>b[off+</code><i>k</i><code>]</code> through
     * <code>b[off+len-1]</code> unaffected.
     *
     * <p> In every case, elements <code>b[0]</code> through
     * <code>b[off]</code> and elements <code>b[off+len]</code> through
     * <code>b[b.length-1]</code> are unaffected.
     *
     * <p> If the first byte cannot be read for any reason other than end of
     * stream, then an <code>IOException</code> is thrown. In particular, an
     * <code>IOException</code> is thrown if the input stream has been closed.
     *
     * @param      b     the buffer into which the data is read.
     * @param      off   the start offset in array <code>b</code>
     *                   at which the data is written.
     * @param      len   the maximum number of bytes to read.
     * @return     the total number of bytes read into the buffer, or
     *             <code>-1</code> if there is no more data because the end of
     *             the stream has been reached.
     * @exception  IOException  if an I/O error occurs.
     */
    public int read(byte[] b, int off, int len) throws IOException {
        if (b == null) {
            throw new NullPointerException();
        }
        if ((off < 0) || (len < 0) || (off + len > b.length)) {
            throw new IndexOutOfBoundsException();
        }
        if (len == 0) {
            return 0;
        }

        mapper.getStreamSegment(pointer, len, streamSegment);
        stream.seek(streamSegment.getStartPos());

        int nbytes = stream.read(b, off, streamSegment.getSegmentLength());
        pointer += nbytes;
        return nbytes;
    }
}
