/*
 * $RCSfile: ImageDecoder.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:55:30 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.codec;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.IOException;

/**
 * An interface describing objects that transform an InputStream into a
 * BufferedImage or Raster.
 *
 * <p><b> This interface is not a committed part of the JAI API.  It may
 * be removed or changed in future releases of JAI.</b>
 */
public interface ImageDecoder {

    /**
     * Returns the current parameters as an instance of the
     * ImageDecodeParam interface.  Concrete implementations of this
     * interface will return corresponding concrete implementations of
     * the ImageDecodeParam interface.  For example, a JPEGImageDecoder
     * will return an instance of JPEGDecodeParam.
     */
    ImageDecodeParam getParam();

    /**
     * Sets the current parameters to an instance of the 
     * ImageDecodeParam interface.  Concrete implementations
     * of ImageDecoder may throw a RuntimeException if the
     * param argument is not an instance of the appropriate
     * subclass or subinterface.  For example, a JPEGImageDecoder
     * will expect param to be an instance of JPEGDecodeParam.
     */
    void setParam(ImageDecodeParam param);

    /** Returns the SeekableStream associated with this ImageDecoder. */
    SeekableStream getInputStream();

    /** Returns the number of pages present in the current stream. */
    int getNumPages() throws IOException;

    /**
     * Returns a Raster that contains the decoded contents of the
     * SeekableStream associated with this ImageDecoder.  Only
     * the first page of a multi-page image is decoded.
     */
    Raster decodeAsRaster() throws IOException;

    /**
     * Returns a Raster that contains the decoded contents of the
     * SeekableStream associated with this ImageDecoder.
     * The given page of a multi-page image is decoded.  If
     * the page does not exist, an IOException will be thrown.
     * Page numbering begins at zero.
     *
     * @param page The page to be decoded.
     */
    Raster decodeAsRaster(int page) throws IOException;

    /**
     * Returns a RenderedImage that contains the decoded contents of the
     * SeekableStream associated with this ImageDecoder.  Only
     * the first page of a multi-page image is decoded.
     */
    RenderedImage decodeAsRenderedImage() throws IOException;

    /**
     * Returns a RenderedImage that contains the decoded contents of the
     * SeekableStream associated with this ImageDecoder.
     * The given page of a multi-page image is decoded.  If
     * the page does not exist, an IOException will be thrown.
     * Page numbering begins at zero.
     *
     * @param page The page to be decoded.
     */
    RenderedImage decodeAsRenderedImage(int page) throws IOException;
}
