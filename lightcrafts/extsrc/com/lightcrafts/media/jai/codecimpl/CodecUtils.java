/*
 * $RCSfile: CodecUtils.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.2 $
 * $Date: 2006/08/22 00:12:04 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.codecimpl;

import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.SinglePixelPackedSampleModel;
import java.io.IOException;
import java.lang.reflect.Method;

/**
 * A class for utility functions for codecs.
 */
class CodecUtils {
    /**
     * The <code>initCause()</code> method of <code>IOException</code>
     * which is available from J2SE 1.4 onward.
     */
    static Method ioExceptionInitCause;

    static {
        try {
            Class c = Class.forName("java.io.IOException");
            ioExceptionInitCause =
                c.getMethod("initCause",
                            new Class[] {java.lang.Throwable.class});
        } catch(Exception e) {
            ioExceptionInitCause = null;
        }
    }

    /**
     * Returns <code>true</code> if and only if <code>im</code>
     * has a <code>SinglePixelPackedSampleModel</code> with a
     * sample size of at most 8 bits for each of its bands.
     *
     * @param src The <code>RenderedImage</code> to test.
     * @return Whether the image is byte-packed.
     */
    static final boolean isPackedByteImage(RenderedImage im) {
        SampleModel imageSampleModel = im.getSampleModel();

        if(imageSampleModel instanceof SinglePixelPackedSampleModel) {
            for(int i = 0; i < imageSampleModel.getNumBands(); i++) {
                if(imageSampleModel.getSampleSize(i) > 8) {
                    return false;
                }
            }

            return true;
        }

        return false;
    }

    /**
     * Converts the parameter exception to an <code>IOException</code>.
     */
    static final IOException toIOException(Exception cause) {
        IOException ioe;

        if(cause != null) {
            if(cause instanceof IOException) {
                ioe = (IOException)cause;
            } else if(ioExceptionInitCause != null) {
                ioe = new IOException(cause.getMessage());
                try {
                    ioExceptionInitCause.invoke(ioe, new Object[] {cause});
                } catch(Exception e2) {
                    // Ignore it ...
                }
            } else {
                ioe = new IOException(cause.getClass().getName()+": "+
                                      cause.getMessage());
            }
        } else {
            ioe = new IOException();
        }

        return ioe;
    }
}
