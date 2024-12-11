/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2024-     Masahiro Kitagawa */

package com.lightcrafts.image.libs;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.Cleaner;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

/**
 * An <code>InputStreamImageDataProvider</code> is-an {@link LCImageDataProvider} for getting image
 * data from an {@link InputStream}.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class InputStreamImageDataProvider implements AutoCloseable, LCImageDataProvider {

    private final ReadableByteChannel m_channel;

    private static final Cleaner cleaner = Cleaner.create();
    private final Cleaner.Cleanable cleanable;

    /**
     * Construct an <code>InputStreamImageDataProvider</code>.
     *
     * @param stream The {@link InputStream} to get image data from.
     */
    public InputStreamImageDataProvider(InputStream stream) {
        m_channel = Channels.newChannel(stream);
        cleanable = cleaner.register(this, cleanup(this.m_channel));
    }

    /**
     * Dispose of this <code>InputStreamImageDataProvider</code> and its resources.
     */
    @Override
    public void close() {
        cleanable.clean();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getImageData(@NotNull ByteBuffer buf) throws IOException {
        buf.clear();
        return m_channel.read(buf);
    }

    @Contract(pure = true)
    private static @NotNull Runnable cleanup(@Nullable ReadableByteChannel channel) {
        return () -> {
            if (channel == null) return;
            try {
                channel.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        };
    }
}
/* vim:set et sw=4 ts=4: */
