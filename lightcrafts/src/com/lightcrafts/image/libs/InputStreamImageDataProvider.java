/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.libs;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

/**
 * An <code>InputStreamImageDataProvider</code> is-an {@link LCImageDataProvider} for getting image
 * data from an {@link InputStream}.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class InputStreamImageDataProvider implements LCImageDataProvider {

    private ReadableByteChannel m_channel;

    /**
     * Construct an <code>InputStreamImageDataProvider</code>.
     *
     * @param stream The {@link InputStream} to get image data from.
     */
    public InputStreamImageDataProvider(InputStream stream) {
        m_channel = Channels.newChannel(stream);
    }

    /**
     * Dispose of this <code>InputStreamImageDataProvider</code> and its resources.
     */
    public synchronized void dispose() {
        if (m_channel != null) {
            final ReadableByteChannel temp = m_channel;
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
    public int getImageData(ByteBuffer buf)
            throws IOException, LCImageLibException {
        buf.clear();
        return m_channel.read(buf);
    }

    @Override
    protected void finalize() throws Throwable {
        dispose();
        super.finalize();
    }
}
/* vim:set et sw=4 ts=4: */
