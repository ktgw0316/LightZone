/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.libs;

/**
 * An <code>LCTIFFCommon</code> factors out common code for
 * {@link LCTIFFReader} and {@link LCTIFFWriter}.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 * @see <a href="http://www.remotesensing.org/libtiff/">LibTIFF</a>
 */
class LCTIFFCommon {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Dispose of an <code>LCTIFFWriter</code>.  Calling this more than once is
     * guaranteed to be harmless.
     */
    public void dispose() {
        TIFFClose();
    }

    ////////// protected //////////////////////////////////////////////////////

    /**
     * Finalize this class by calling {@link #dispose()}.
     */
    protected void finalize() throws Throwable {
        super.finalize();
        dispose();
    }

    /**
     * This is where the native code stores a pointer to the <code>TIFF</code>
     * native data structure.  Do not touch this from Java.
     */
    protected long m_nativePtr;

    ////////// private ////////////////////////////////////////////////////////

    /**
     * Initializes the native library.
     */
    private static native void init();

    /**
     * Closes the TIFF file.
     */
    private native void TIFFClose();

    /**
     * This is called from the native code to throw an
     * {@link LCImageLibException}.  Doing it this way is less work than having
     * the native code call the constructor directly.
     *
     * @param msg The error message.
     */
    private static void throwException( String msg )
        throws LCImageLibException
    {
        throw new LCImageLibException( msg );
    }

    static {
        System.loadLibrary( "LCTIFF" );
        init();
    }
}
/* vim:set et sw=4 ts=4: */
