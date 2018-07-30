/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.libs;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;

/**
 * An <code>OutputStreamImageDataReceiver</code> is-an {@link LCImageDataReceiver} for putting image
 * data to an {@link OutputStream}.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class OutputStreamImageDataReceiver
        implements LCImageDataReceiver {

    /**
     * Construct an <code>OutputStreamImageDataReceiver</code>.
     *
     * @param stream The {@link OutputStream} to put image data to.
     */
    public OutputStreamImageDataReceiver(OutputStream stream) {
        m_channel = Channels.newChannel(stream);
    }

    /**
     * Dispose of this <code>OutputStreamImageDataReceiver</code> and its resources.
     */
    public synchronized void dispose() {
        if (m_channel != null) {
            final WritableByteChannel temp = m_channel;
            m_channel = null;
            try {
                temp.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int putImageData(ByteBuffer buf)
            throws IOException, LCImageLibException {
        buf.rewind();
        return m_channel.write(buf);
    }

    @Override
    protected void finalize() throws Throwable {
        dispose();
        super.finalize();
    }

    private WritableByteChannel m_channel;
}
/* vim:set et sw=4 ts=4: */
