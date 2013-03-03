/*
 * $RCSfile: SeekableOutputStream.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:55:32 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.codec;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;

/**
 * An <code>OutputStream</code> which can seek to an arbitrary offset.
 */
public class SeekableOutputStream extends OutputStream {

    private RandomAccessFile file;

    /**
     * Constructs a <code>SeekableOutputStream</code> from a
     * <code>RandomAccessFile</code>.  Unless otherwise indicated,
     * all method invocations are fowarded to the underlying
     * <code>RandomAccessFile</code>.
     *
     * @param file The <code>RandomAccessFile</code> to which calls
     *             will be forwarded.
     * @exception IllegalArgumentException if <code>file</code> is
     *            <code>null</code>.
     */
    public SeekableOutputStream(RandomAccessFile file) {
        if(file == null) {
            throw new IllegalArgumentException(JaiI18N.getString("SeekableOutputStream0"));
        }
        this.file = file;
    }

    public void write(int b) throws IOException {
        file.write(b);
    }

    public void write(byte b[]) throws IOException {
        file.write(b);
    }

    public void write(byte b[], int off, int len) throws IOException {
        file.write(b, off, len);
    }

    /**
     * Invokes <code>getFD().sync()</code> on the underlying
     * <code>RandomAccessFile</code>.
     */
    public void flush() throws IOException {
	// Fix: 4636212.  When this FIleDescriptor is not valid, do nothing.
	FileDescriptor fd = file.getFD();
        if(fd.valid()) 
	    fd.sync();
    }

    public void close() throws IOException {
        file.close();
    }

    public long getFilePointer() throws IOException {
        return file.getFilePointer();
    }

    public void seek(long pos) throws IOException {
        file.seek(pos);
    }
}
