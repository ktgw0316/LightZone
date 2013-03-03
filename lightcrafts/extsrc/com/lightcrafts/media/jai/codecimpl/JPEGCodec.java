/*
 * $RCSfile: JPEGCodec.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:55:36 $
 * $State: Exp $
 */

package com.lightcrafts.media.jai.codecimpl;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import com.lightcrafts.media.jai.codec.ImageCodec;
import com.lightcrafts.media.jai.codec.ImageDecoder;
import com.lightcrafts.media.jai.codec.ImageDecodeParam;
import com.lightcrafts.media.jai.codec.ImageEncoder;
import com.lightcrafts.media.jai.codec.ImageEncodeParam;
import com.lightcrafts.media.jai.codec.JPEGEncodeParam;
import com.lightcrafts.media.jai.codec.SeekableStream;

/**
 * @since EA2
 */
public final class JPEGCodec extends ImageCodec {

    public JPEGCodec() {}

    public String getFormatName() {
        return "jpeg";
    }

    public Class getEncodeParamClass() {
        return com.lightcrafts.media.jai.codec.JPEGEncodeParam.class;
    }

    public Class getDecodeParamClass() {
        return com.lightcrafts.media.jai.codec.JPEGDecodeParam.class;
    }

    public boolean canEncodeImage(RenderedImage im,
                                  ImageEncodeParam param) {
        return true;
    }

    protected ImageEncoder createImageEncoder(OutputStream dst,
                                              ImageEncodeParam param) {
        JPEGEncodeParam p = null;
        if (param != null) {
            p = (JPEGEncodeParam)param;
        }

        return new JPEGImageEncoder(dst, p);
    }

    protected ImageDecoder createImageDecoder(InputStream src,
                                              ImageDecodeParam param) {
        return new JPEGImageDecoder(src, param);
    }

    protected ImageDecoder createImageDecoder(File src,
                                              ImageDecodeParam param) 
        throws IOException {
        return new JPEGImageDecoder(new FileInputStream(src), param);
    }

    protected ImageDecoder createImageDecoder(SeekableStream src,
                                              ImageDecodeParam param) {
        return new JPEGImageDecoder(src, param);
    }

    public int getNumHeaderBytes() {
        return 3;
    }

    public boolean isFormatRecognized(byte[] header) {
        return ((header[0] == (byte)0xff) &&
                (header[1] == (byte)0xd8) &&
                (header[2] == (byte)0xff));
    }
}
