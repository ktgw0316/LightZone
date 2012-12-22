/*
 * $RCSfile: GIFCodec.java,v $
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
import com.lightcrafts.media.jai.codec.SeekableStream;
// import com.lightcrafts.media.jai.codec.GIFEncodeParam;

/**
 * @since EA3
 */
public final class GIFCodec extends ImageCodec {

    public GIFCodec() {}

    public String getFormatName() {
        return "gif";
    }

    public Class getEncodeParamClass() {
        return Object.class;
    }

    public Class getDecodeParamClass() {
        return Object.class;
    }

    public boolean canEncodeImage(RenderedImage im,
                                  ImageEncodeParam param) {
        return false;
    }

    protected ImageEncoder createImageEncoder(OutputStream dst,
                                              ImageEncodeParam param) {
        /*
        GIFEncodeParam p = null;
        if (param != null) {
            p = (GIFEncodeParam)param;
        }

        return new GIFImageEncoder(dst, p);
        */

        return null;
    }

    protected ImageDecoder createImageDecoder(InputStream src,
                                              ImageDecodeParam param) {
        return new GIFImageDecoder(src, param);
    }

    protected ImageDecoder createImageDecoder(File src,
                                              ImageDecodeParam param) 
        throws IOException {
        return new GIFImageDecoder(new FileInputStream(src), null);
    }

    protected ImageDecoder createImageDecoder(SeekableStream src,
                                              ImageDecodeParam param) {
        return new GIFImageDecoder(src, param);
    }


    public int getNumHeaderBytes() {
        return 4;
    }

    public boolean isFormatRecognized(byte[] header) {
        return ((header[0] == 'G') &&
                (header[1] == 'I') &&
                (header[2] == 'F') &&
                (header[3] == '8'));
    }
}
