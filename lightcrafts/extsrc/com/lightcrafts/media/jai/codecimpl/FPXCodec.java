/*
 * $RCSfile: FPXCodec.java,v $
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
import java.io.OutputStream;
import com.lightcrafts.media.jai.codec.ImageCodec;
import com.lightcrafts.media.jai.codec.ImageDecoder;
import com.lightcrafts.media.jai.codec.ImageDecodeParam;
import com.lightcrafts.media.jai.codec.ImageEncoder;
import com.lightcrafts.media.jai.codec.ImageEncodeParam;
import com.lightcrafts.media.jai.codec.SeekableStream;
// import com.lightcrafts.media.jai.codec.FPXEncodeParam;

/**
 * @since EA3
 */
public final class FPXCodec extends ImageCodec {

    public FPXCodec() {}

    public String getFormatName() {
        return "fpx";
    }

    public Class getEncodeParamClass() {
        return null;
    }

    public Class getDecodeParamClass() {
        return com.lightcrafts.media.jai.codec.FPXDecodeParam.class;
    }

    public boolean canEncodeImage(RenderedImage im,
                                  ImageEncodeParam param) {
        return false;
    }

    protected ImageEncoder createImageEncoder(OutputStream dst,
                                              ImageEncodeParam param) {
        throw new RuntimeException(JaiI18N.getString("FPXCodec0"));
    }

    protected ImageDecoder createImageDecoder(SeekableStream src,
                                              ImageDecodeParam param) {
        return new FPXImageDecoder(src, param);
    }

    public int getNumHeaderBytes() {
         return 8;
    }

    public boolean isFormatRecognized(byte[] header) {
        return ((header[0] == (byte)0xd0) &&
                (header[1] == (byte)0xcf) &&
                (header[2] == (byte)0x11) &&
                (header[3] == (byte)0xe0) &&
                (header[4] == (byte)0xa1) &&
                (header[5] == (byte)0xb1) &&
                (header[6] == (byte)0x1a) &&
                (header[7] == (byte)0xe1));
    }

}
