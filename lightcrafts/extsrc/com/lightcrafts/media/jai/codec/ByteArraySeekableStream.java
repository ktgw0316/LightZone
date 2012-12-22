/*
 * $RCSfile: ByteArraySeekableStream.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:55:29 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.codec;
import java.io.IOException;

/**
 * A subclass of <code>SeekableStream</code> that takes input from an
 * array of bytes.  Seeking backwards is supported.  The
 * <code>mark()</code> and <code>resest()</code> methods are
 * supported.
 *
 * <p><b> This class is not a committed part of the JAI API.  It may
 * be removed or changed in future releases of JAI.</b>
 */
public class ByteArraySeekableStream extends SeekableStream {

    /** Array holding the source data. */
    private byte[] src;

    /** The starting offset within the array. */
    private int offset;

    /** The length of the valid segment of the array. */
    private int length;
    
    /** The current output position. */
    private int pointer;

    /**
     * Constructs a <code>ByteArraySeekableStream</code> taking
     * input from a given segment of an input <code>byte</code> array.
     */
    public ByteArraySeekableStream(byte[] src, int offset, int length)
        throws IOException {
        this.src = src;
        this.offset = offset;
        this.length = length;
    }

    /**
     * Constructs a <code>ByteArraySeekableStream</code> taking
     * input from an entire input <code>byte</code> array.
     */
    public ByteArraySeekableStream(byte[] src) throws IOException {
        this(src, 0, src.length);
    }

    /**
     * Returns the number of bytes that can be read from this input 
     * stream without blocking. 
     * The value returned is
     * <code>Math.min(offset + length, src.length)&nbsp;- pos</code>, 
     * which is the number of bytes remaining to be read from the input buffer.
     *
     * @return  the number of bytes that can be read from the input stream
     *          without blocking.
     */
    public synchronized int available() {
        ensureOpen();
        return Math.min(offset + length, src.length) - pointer;
    }

    /**
     * Returns <code>true</code> since this object supports seeking
     * backwards.
     */
    public boolean canSeekBackwards() {
        return true;
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
     * Sets the offset, measured from the beginning of this 
     * stream, at which the next read occurs.  Seeking backwards is
     * allowed.
     *
     * @param      pos   the offset position, measured in bytes from the 
     *                   beginning of the stream, at which to set the stream 
     *                   pointer.
     */
    public void seek(long pos) {
        pointer = (int)pos;
    }

    /**
     * Reads the next byte of data from the input array. The value byte is
     * returned as an <code>int</code> in the range <code>0</code> to
     * <code>255</code>. If no byte is available because the end of the stream
     * has been reached, the value <code>-1</code> is returned.
     */
    public int read() {
        if (pointer < length + offset) {
            return (int)(src[pointer++ + offset] & 0xff);
        } else {
            return -1;
        }
    }

    /**
     * Copies up to <code>len</code> bytes of data from the input array into
     * an array of bytes.  An attempt is made to copy as many as
     * <code>len</code> bytes, but a smaller number may be copied, possibly
     * zero. The number of bytes actually copied is returned as an integer.
     *
     * <p> If <code>b</code> is <code>null</code>, a
     * <code>NullPointerException</code> is thrown.
     *
     * <p> If <code>off</code> is negative, or <code>len</code> is negative, or
     * <code>off+len</code> is greater than the length of the array
     * <code>b</code>, then an <code>IndexOutOfBoundsException</code> is
     * thrown.
     *
     * <p> If <code>len</code> is zero, then no bytes are copied and
     * <code>0</code> is returned; otherwise, there is an attempt to copy at
     * least one byte. If no byte is available because the stream is at end of
     * stream, the value <code>-1</code> is returned; otherwise, at least one
     * byte is copied into <code>b</code>.
     *
     * <p> The first byte copied is stored into element
     * <code>b[off]</code>, the next one into <code>b[off+1]</code>,
     * and so on. The number of bytes copied is, at most, equal to
     * <code>len</code>. Let <i>k</i> be the number of bytes actually
     * copied; these bytes will be stored in elements
     * <code>b[off]</code> through
     * <code>b[off+</code><i>k</i><code>-1]</code>, leaving elements
     * <code>b[off+</code><i>k</i><code>]</code> through
     * <code>b[off+len-1]</code> unaffected.
     *
     * <p> In every case, elements <code>b[0]</code> through
     * <code>b[off]</code> and elements <code>b[off+len]</code> through
     * <code>b[b.length-1]</code> are unaffected.
     *
     * @param      b     the buffer into which the data is copied.
     * @param      off   the start offset in array <code>b</code>
     *                   at which the data is written.
     * @param      len   the maximum number of bytes to copy.
     * @return     the total number of bytes read into the buffer, or
     *             <code>-1</code> if there is no more data because the end of
     *             the stream has been reached.
     */
    public int read(byte[] b, int off, int len) {
        if (b == null) {
            throw new NullPointerException();
        }
        if ((off < 0) || (len < 0) || (off + len > b.length)) {
            throw new IndexOutOfBoundsException();
        }
        if (len == 0) {
            return 0;
        }

        int oldPointer = pointer;
        pointer = Math.min(pointer + len, length + offset);

        if (pointer == oldPointer) {
            return -1;
        } else {
            System.arraycopy(src, oldPointer, b, off, pointer - oldPointer);
            return pointer - oldPointer;
        }
    }

    /**
     * Attempts to skip over <code>n</code> bytes of input discarding the 
     * skipped bytes. 
     * <p>
     * 
     * This method may skip over some smaller number of bytes, possibly zero. 
     * This may result from any of a number of conditions; reaching end of 
     * stream before <code>n</code> bytes have been skipped is only one 
     * possibility. This method never throws an <code>EOFException</code>. 
     * The actual number of bytes skipped is returned.  If <code>n</code> 
     * is negative, no bytes are skipped.
     *
     * @param      n   the number of bytes to be skipped.
     * @return     the actual number of bytes skipped.
     */
    public int skipBytes(int n) {
        int oldPointer = pointer;
        pointer = Math.min(pointer + n, length + offset);
        return pointer - oldPointer;
    }

    /** Does nothing. */
    public void close() {
    }

    /** Returns the number of valid bytes in the input array. */
    public long length() {
        return length;
    }

    /** Check to make sure that the stream has not been closed */
    private void ensureOpen() {
        /* This method does nothing for now.  Once we add throws clauses
         * to the I/O methods in this class, it will throw an IOException
         * if the stream has been closed.
         */
    }
}
