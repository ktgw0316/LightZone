/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2018-     Masahiro Kitagawa */

package com.lightcrafts.image.libs;

import java.lang.ref.Cleaner;
import java.lang.foreign.MemorySegment;

/**
 * An <code>LCTIFFCommon</code> factors out common code for {@link LCTIFFReader} and {@link
 * LCTIFFWriter}.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 * @see <a href="http://www.remotesensing.org/libtiff/">LibTIFF</a>
 */
class LCTIFFCommon implements AutoCloseable {

    static {
        TIFFNative.init();
    }

    protected static final Cleaner cleaner = Cleaner.create();
    protected final NativeState nativeState = new NativeState();
    private final Cleaner.Cleanable cleanable = cleaner.register(this, nativeState);

    /**
     * Dispose of an <code>LCTIFFWriter</code>.  Calling this more than once is guaranteed to be
     * harmless.
     */
    public void dispose() {
        cleanable.clean();
    }

    @Override
    public void close() {
        dispose();
    }

    protected MemorySegment open(String fileName, String mode) {
        return TIFFNative.open(fileName, mode);
    }

    protected void setNativeHandle(MemorySegment handle) {
        nativeState.handle = handle;
    }

    protected MemorySegment nativeHandle() {
        return nativeState.handle;
    }

    protected static final class NativeState implements Runnable {
        private MemorySegment handle = MemorySegment.NULL;

        private synchronized void close() {
            final MemorySegment closing = handle;
            handle = MemorySegment.NULL;
            TIFFNative.close(closing);
        }

        @Override
        public void run() {
            close();
        }
    }
}
/* vim:set et sw=4 ts=4: */
