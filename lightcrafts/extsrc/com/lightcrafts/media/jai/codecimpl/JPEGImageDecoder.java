/*
 * $RCSfile: JPEGImageDecoder.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.3 $
 * $Date: 2006/08/22 00:12:04 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.codecimpl;
import java.awt.image.BufferedImage;
import java.awt.image.ComponentSampleModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.FilterInputStream;
import java.io.InputStream;
import java.io.IOException;
import com.lightcrafts.media.jai.codec.ImageDecoderImpl;
import com.lightcrafts.media.jai.codec.ImageDecodeParam;
import com.lightcrafts.media.jai.codec.JPEGDecodeParam;
import com.lightcrafts.media.jai.codecimpl.ImagingListenerProxy;
import com.lightcrafts.media.jai.codecimpl.util.ImagingException;
import com.sun.image.codec.jpeg.ImageFormatException;

/**
 * @since EA2
 */
public class JPEGImageDecoder extends ImageDecoderImpl {

    public JPEGImageDecoder(InputStream input,
                            ImageDecodeParam param) {
        super(input, param);
    }

    public RenderedImage decodeAsRenderedImage(int page) throws IOException {
        if (page != 0) {
            throw new IOException(JaiI18N.getString("JPEGImageDecoder0"));
        }
        try {
            return new JPEGImage(input, param);
        } catch(Exception e) {
            throw CodecUtils.toIOException(e);
        }
    }
}

/**
 * FilterInputStream subclass which does not support mark/reset.
 * Used to work around a failure of com.sun.image.codec.jpeg.JPEGImageDecoder
 * in which decodeAsBufferedImage() blocks in reset() if a corrupted JPEG
 * image is encountered.
 */
class NoMarkStream extends FilterInputStream {
    NoMarkStream(InputStream in) {
        super(in);
    }

    /**
     * Disallow mark/reset.
     */
    public boolean markSupported() {
        return false;
    }

    /**
     * Disallow close() from closing the stream passed in.
     */
    public final void close() throws IOException {
        // Deliberately do nothing.
    }
}

class JPEGImage extends SimpleRenderedImage {

    /**
     * Mutex for the entire class to circumvent thread unsafety of
     * com.sun.image.codec.jpeg.JPEGImageDecoder implementation.
     */
    private static final Object LOCK = new Object();

    private Raster theTile = null;

    /**
     * Construct a JPEGmage.
     *
     * @param stream The JPEG InputStream.
     * @param param The decoding parameters.
     */
    public JPEGImage(InputStream stream, ImageDecodeParam param) {
        // If the supplied InputStream supports mark/reset wrap it so
        // it does not.
        if(stream.markSupported()) {
            stream = new NoMarkStream(stream);
        }

        // Lock the entire class to work around lack of thread safety
        // in com.sun.image.codec.jpeg.JPEGImageDecoder implementation.
        BufferedImage image = null;
        synchronized(LOCK) {
            com.sun.image.codec.jpeg.JPEGImageDecoder decoder =
                com.sun.image.codec.jpeg.JPEGCodec.createJPEGDecoder(stream);
            try {
                // decodeAsBufferedImage performs default color conversions
                image = decoder.decodeAsBufferedImage();
            } catch (ImageFormatException e) {
                String message = JaiI18N.getString("JPEGImageDecoder1");
                sendExceptionToListener(message, (Exception)e);
//                throw new RuntimeException(JaiI18N.getString("JPEGImageDecoder1"));
            } catch (IOException e) {
                String message = JaiI18N.getString("JPEGImageDecoder1");
                sendExceptionToListener(message, (Exception)e);
//                throw new RuntimeException(JaiI18N.getString("JPEGImageDecoder2"));
            }
        }

        minX = 0;
        minY = 0;
        tileWidth = width = image.getWidth();
        tileHeight = height = image.getHeight();

        // Force image to have a ComponentSampleModel if it does not have one
        // and the ImageDecodeParam is either null or is a JPEGDecodeParam
        // with 'decodeToCSM' set to 'true'.
        if ((param == null ||
             (param instanceof JPEGDecodeParam &&
              ((JPEGDecodeParam)param).getDecodeToCSM())) &&
            !(image.getSampleModel() instanceof ComponentSampleModel)) {

            int type = -1;
            int numBands = image.getSampleModel().getNumBands();
            if (numBands == 1) {
                type = BufferedImage.TYPE_BYTE_GRAY;
            } else if (numBands == 3) {
                type = BufferedImage.TYPE_3BYTE_BGR;
            } else if (numBands == 4) {
                type = BufferedImage.TYPE_4BYTE_ABGR;
            } else {
                throw new RuntimeException(JaiI18N.getString("JPEGImageDecoder3"));
            }

            BufferedImage bi = new BufferedImage(width, height, type);
            bi.getWritableTile(0, 0).setRect(image.getWritableTile(0, 0));
            bi.releaseWritableTile(0, 0);
            image = bi;
        }

        sampleModel = image.getSampleModel();
        colorModel = image.getColorModel();

        theTile = image.getWritableTile(0, 0);
    }

    public synchronized Raster getTile(int tileX, int tileY) {
        if (tileX != 0 || tileY != 0) {
            throw new IllegalArgumentException(JaiI18N.getString("JPEGImageDecoder4"));
        }

        return theTile;
    }

    public void dispose() {
        theTile = null;
    }

    private void sendExceptionToListener(String message, Exception e) {
        ImagingListenerProxy.errorOccurred(message, new ImagingException(message, e),
                               this, false);
    }
}
