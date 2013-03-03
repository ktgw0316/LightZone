/*
 * $RCSfile: ImageEncoderImpl.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:55:31 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.codec;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.OutputStream;
import com.lightcrafts.media.jai.codecimpl.SingleTileRenderedImage;

/**
 * A partial implementation of the ImageEncoder interface useful for
 * subclassing.
 *
 * <p><b> This class is not a committed part of the JAI API.  It may
 * be removed or changed in future releases of JAI.</b>
 */
public abstract class ImageEncoderImpl implements ImageEncoder {
    
    /** The OutputStream associcted with this ImageEncoder. */
    protected OutputStream output;

    /** The ImageEncodeParam object associcted with this ImageEncoder. */
    protected ImageEncodeParam param;

    /**
     * Constructs an ImageEncoderImpl with a given OutputStream
     * and ImageEncoderParam instance.
     */
    public ImageEncoderImpl(OutputStream output,
                            ImageEncodeParam param) {
        this.output = output;
        this.param = param;
    }

    /**
     * Returns the current parameters as an instance of the
     * ImageEncodeParam interface.  Concrete implementations of this
     * interface will return corresponding concrete implementations of
     * the ImageEncodeParam interface.  For example, a JPEGImageEncoder
     * will return an instance of JPEGEncodeParam.
     */
    public ImageEncodeParam getParam() {
        return param;
    }

    /**
     * Sets the current parameters to an instance of the 
     * ImageEncodeParam interface.  Concrete implementations
     * of ImageEncoder may throw a RuntimeException if the
     * params argument is not an instance of the appropriate
     * subclass or subinterface.  For example, a JPEGImageEncoder
     * will expect param to be an instance of JPEGEncodeParam.
     */
    public void setParam(ImageEncodeParam param) {
        this.param = param;
    }

    /** Returns the OutputStream associated with this ImageEncoder. */
    public OutputStream getOutputStream() {
        return output;
    }
    
    /**
     * Encodes a Raster with a given ColorModel and writes the output
     * to the OutputStream associated with this ImageEncoder.
     */
    public void encode(Raster ras, ColorModel cm) throws IOException {
        RenderedImage im = new SingleTileRenderedImage(ras, cm);
        encode(im);
    }

    /**
     * Encodes a RenderedImage and writes the output to the
     * OutputStream associated with this ImageEncoder.
     */
    public abstract void encode(RenderedImage im) throws IOException;
}
