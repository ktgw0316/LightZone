/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2018-     Masahiro Kitagawa */

package com.lightcrafts.image.libs;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.lang.ref.Cleaner;

/**
 * An <code>LCTIFFCommon</code> factors out common code for {@link LCTIFFReader} and {@link
 * LCTIFFWriter}.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 * @see <a href="http://www.remotesensing.org/libtiff/">LibTIFF</a>
 */
class LCTIFFCommon implements AutoCloseable {

    static {
        System.loadLibrary("LCTIFF");
        init();
    }

    protected static final Cleaner cleaner = Cleaner.create();

    /**
     * This is where the native code stores a pointer to the <code>TIFF</code> native data
     * structure.  Do not touch this from Java.
     */
    long m_nativePtr;

    /**
     * Initializes the native library.
     */
    private static native void init();

    /**
     * This is called from the native code to throw an {@link LCImageLibException}.  Doing it this
     * way is less work than having the native code call the constructor directly.
     *
     * @param msg The error message.
     */
    @SuppressWarnings({"UNUSED_SYMBOL"})
    private static void throwException(String msg)
            throws LCImageLibException {
        throw new LCImageLibException(msg);
    }

    /**
     * Dispose of an <code>LCTIFFWriter</code>.  Calling this more than once is guaranteed to be
     * harmless.
     */
    public void dispose() {
        TIFFClose();
    }

    @Override
    public void close() {
        dispose();
    }

    @Contract(pure = true)
    protected static @NotNull Runnable cleanup(@NotNull LCTIFFCommon tiff) {
        return tiff::dispose;
    }

    /**
     * Closes the TIFF file.
     */
    private native void TIFFClose();
}
/* vim:set et sw=4 ts=4: */
