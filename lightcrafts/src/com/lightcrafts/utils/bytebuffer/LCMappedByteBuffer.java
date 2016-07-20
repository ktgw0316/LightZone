/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.utils.bytebuffer;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;

/**
 * An <code>LCMappedByteBuffer</code> is-an {@link ArrayByteBuffer} that uses
 * a mapped file for its backing store.  It also implements a {@link #close()}
 * method that properly unmaps the file.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class LCMappedByteBuffer extends ArrayByteBuffer
    implements Closeable {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Construct an <code>LCMappedByteBuffer</code> mapping an entire file
     * read-only.
     *
     * @param file The {@link File} to map read-only.
     */
    public LCMappedByteBuffer( File file ) throws IOException {
        this( file, FileChannel.MapMode.READ_ONLY );
    }

    /**
     * Construct an <code>LCMappedByteBuffer</code> mapping an entire file.
     *
     * @param file The {@link File} to map.
     * @param mode The {@link FileChannel.MapMode} to use.
     */
    public LCMappedByteBuffer( File file, FileChannel.MapMode mode )
        throws IOException
    {
        this( file, 0, file.length(), mode );
    }

    /**
     * Construct an <code>LCMappedByteBuffer</code>.
     *
     * @param position The position within the file at which the mapped region
     * is to start.
     * @param size The size of the region to be mapped.
     * @param mode The {@link FileChannel.MapMode} to use.
     */
    public LCMappedByteBuffer( File file, long position, long size,
                               FileChannel.MapMode mode ) throws IOException {
        super( ByteBufferUtil.map( file, position, size, mode ) );
    }

    /**
     * Closes this <code>LCMapperByteBuffer</code>.
     */
    @Override
    public void close() {
        ByteBufferUtil.clean(getByteBuffer());
    }

    ////////// protected //////////////////////////////////////////////////////

    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }

}
/* vim:set et sw=4 ts=4: */
