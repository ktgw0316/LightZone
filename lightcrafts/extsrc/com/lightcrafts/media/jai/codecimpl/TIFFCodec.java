/*
 * $RCSfile: TIFFCodec.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:55:39 $
 * $State: Exp $
 */

package com.lightcrafts.media.jai.codecimpl;
import java.awt.image.RenderedImage;
import java.io.OutputStream;
import com.lightcrafts.media.jai.codec.ImageCodec;
import com.lightcrafts.media.jai.codec.ImageDecoder;
import com.lightcrafts.media.jai.codec.ImageDecodeParam;
import com.lightcrafts.media.jai.codec.ImageEncoder;
import com.lightcrafts.media.jai.codec.ImageEncodeParam;
import com.lightcrafts.media.jai.codec.SeekableStream;

/**
 * @since EA3
 */
public final class TIFFCodec extends ImageCodec {

    public TIFFCodec() {}

    public String getFormatName() {
        return "tiff";
    }

    public Class getEncodeParamClass() {
        return com.lightcrafts.media.jai.codec.TIFFEncodeParam.class;
    }

    public Class getDecodeParamClass() {
        return com.lightcrafts.media.jai.codec.TIFFDecodeParam.class;
    }

    public boolean canEncodeImage(RenderedImage im,
                                  ImageEncodeParam param) {
        return true;
    }

    protected ImageEncoder createImageEncoder(OutputStream dst,
                                              ImageEncodeParam param) {
        return new TIFFImageEncoder(dst, param);
    }

    protected ImageDecoder createImageDecoder(SeekableStream src,
                                              ImageDecodeParam param) {
        return new TIFFImageDecoder(src, param);
    }

    public int getNumHeaderBytes() {
        return 4;
    }

    public boolean isFormatRecognized(byte[] header) {
        if ((header[0] == 0x49) &&
            (header[1] == 0x49) &&
            (header[2] == 0x2a) &&
            (header[3] == 0x00)) {
            return true;
        }

        if ((header[0] == 0x4d) &&
            (header[1] == 0x4d) &&
            (header[2] == 0x00) &&
            (header[3] == 0x2a)) {
            return true;
        }

        return false;
    }
}
