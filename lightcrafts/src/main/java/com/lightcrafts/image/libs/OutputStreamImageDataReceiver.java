/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2024-     Masahiro Kitagawa */

package com.lightcrafts.image.libs;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.ref.Cleaner;
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
        implements AutoCloseable, LCImageDataReceiver {

    /**
     * Construct an <code>OutputStreamImageDataReceiver</code>.
     *
     * @param stream The {@link OutputStream} to put image data to.
     */
    public OutputStreamImageDataReceiver(OutputStream stream) {
        m_channel = Channels.newChannel(stream);
        cleanable = cleaner.register(this, cleanup(this.m_channel));
    }

    /**
     * Dispose of this <code>OutputStreamImageDataReceiver</code> and its resources.
     */
    @Contract(pure = true)
    private static @NotNull Runnable cleanup(@NotNull WritableByteChannel channel) {
        return () -> {
            try {
                channel.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int putImageData(@NotNull ByteBuffer buf) throws IOException {
        buf.rewind();
        return m_channel.write(buf);
    }

    @Override
    public void close() {
        cleanable.clean();
    }

    private final WritableByteChannel m_channel;

    private static final Cleaner cleaner = Cleaner.create();
    private final Cleaner.Cleanable cleanable;
}
/* vim:set et sw=4 ts=4: */
