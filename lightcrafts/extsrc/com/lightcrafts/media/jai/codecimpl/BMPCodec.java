/*
 * $RCSfile: BMPCodec.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:55:35 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.codecimpl;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
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
import com.lightcrafts.media.jai.codec.BMPEncodeParam;
import com.lightcrafts.media.jai.codec.SeekableStream;

/**
 * @since EA2
 */
public final class BMPCodec extends ImageCodec {

    public BMPCodec() {}

    public String getFormatName() {
        return "bmp";
    }

    public Class getEncodeParamClass() {
        return com.lightcrafts.media.jai.codec.BMPEncodeParam.class;
    }

    public Class getDecodeParamClass() {
        return Object.class;
    }

    public boolean canEncodeImage(RenderedImage im,
                                  ImageEncodeParam param) {
        SampleModel sampleModel = im.getSampleModel();
        int dataType = sampleModel.getTransferType();
        if (dataType != DataBuffer.TYPE_BYTE &&
            !CodecUtils.isPackedByteImage(im)) {
            return false;
        }

        if (param != null) {
            if (!(param instanceof BMPEncodeParam)) {
                return false;
            }
            BMPEncodeParam BMPParam = (BMPEncodeParam)param;

            int version = BMPParam.getVersion();
            if ((version == BMPEncodeParam.VERSION_2) ||
                (version == BMPEncodeParam.VERSION_4)) {
                return false;
            }
        }

        return true;
    }

    protected ImageEncoder createImageEncoder(OutputStream dst,
                                              ImageEncodeParam param) {
        BMPEncodeParam p = null;
        if (param != null) {
            p = (BMPEncodeParam)param;
        }

        return new BMPImageEncoder(dst, p);
    }

    protected ImageDecoder createImageDecoder(InputStream src,
                                              ImageDecodeParam param) {
        return new BMPImageDecoder(src, null);
    }

    protected ImageDecoder createImageDecoder(File src,
                                              ImageDecodeParam param) 
        throws IOException {
        return new BMPImageDecoder(new FileInputStream(src), null);
    }

    protected ImageDecoder createImageDecoder(SeekableStream src,
                                              ImageDecodeParam param) {
        return new BMPImageDecoder(src, null);
    }

    public int getNumHeaderBytes() {
        return 2;
    }

    public boolean isFormatRecognized(byte[] header) {
        return ((header[0] == 0x42) &&
                (header[1] == 0x4d));
    }
}




