/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2018-     Masahiro Kitagawa */

package com.lightcrafts.image.libs;

/**
 * An <code>LCTIFFCommon</code> factors out common code for {@link LCTIFFReader} and {@link
 * LCTIFFWriter}.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 * @see <a href="http://www.remotesensing.org/libtiff/">LibTIFF</a>
 */
class LCTIFFCommon {

    static {
        System.loadLibrary("LCTIFF");
        init();
    }

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

    /**
     * Finalize this class by calling {@link #dispose()}.
     */
    protected void finalize() throws Throwable {
        super.finalize();
        dispose();
    }

    /**
     * Closes the TIFF file.
     */
    private native void TIFFClose();
}
/* vim:set et sw=4 ts=4: */
