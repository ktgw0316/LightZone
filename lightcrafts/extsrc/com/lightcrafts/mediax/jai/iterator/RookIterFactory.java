/*
 * $RCSfile: RookIterFactory.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:27 $
 * $State: Exp $
 */
package com.lightcrafts.mediax.jai.iterator;
import java.awt.Rectangle;
import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRenderedImage;
// import com.lightcrafts.media.jai.iterator.RookIterCSMByte;
// import com.lightcrafts.media.jai.iterator.RookIterCSMShort;
// import com.lightcrafts.media.jai.iterator.RookIterCSMUShort;
// import com.lightcrafts.media.jai.iterator.RookIterCSMInt;
// import com.lightcrafts.media.jai.iterator.RookIterCSMFloat;
// import com.lightcrafts.media.jai.iterator.RookIterCSMDouble;
import com.lightcrafts.media.jai.iterator.RookIterFallback;
import com.lightcrafts.media.jai.iterator.WrapperRI;
import com.lightcrafts.media.jai.iterator.WrapperWRI;
// import com.lightcrafts.media.jai.iterator.WritableRookIterCSMByte;
// import com.lightcrafts.media.jai.iterator.WritableRookIterCSMShort;
// import com.lightcrafts.media.jai.iterator.WritableRookIterCSMUShort;
// import com.lightcrafts.media.jai.iterator.WritableRookIterCSMInt;
// import com.lightcrafts.media.jai.iterator.WritableRookIterCSMFloat;
// import com.lightcrafts.media.jai.iterator.WritableRookIterCSMDouble;
import com.lightcrafts.media.jai.iterator.WritableRookIterFallback;

/**
 * A factory class to instantiate instances of the RookIter and
 * WritableRookIter interfaces on sources of type Raster,
 * RenderedImage, and WritableRenderedImage.
 *
 * @see RookIter
 * @see WritableRookIter
 */
public class RookIterFactory {

    /** Prevent this class from ever being instantiated. */
    private RookIterFactory() {}

    /**
     * Constructs and returns an instance of RookIter suitable
     * for iterating over the given bounding rectangle within the
     * given RenderedImage source.  If the bounds parameter is null,
     * the entire image will be used.
     *
     * @param im a read-only RenderedImage source.
     * @param bounds the bounding Rectangle for the iterator, or null.
     * @return a RookIter allowing read-only access to the source.
     */
    public static RookIter create(RenderedImage im,
                                  Rectangle bounds) { 
        if (bounds == null) {
            bounds = new Rectangle(im.getMinX(), im.getMinY(),
                                   im.getWidth(), im.getHeight());
        }

        SampleModel sm = im.getSampleModel();
        if (sm instanceof ComponentSampleModel) {
            switch (sm.getDataType()) {
            case DataBuffer.TYPE_BYTE:
                // return new RookIterCSMByte(im, bounds);
            case DataBuffer.TYPE_SHORT:
                // return new RookIterCSMShort(im, bounds);
            case DataBuffer.TYPE_USHORT:
                // return new RookIterCSMUShort(im, bounds);
            case DataBuffer.TYPE_INT:
                // return new RookIterCSMInt(im, bounds);
            case DataBuffer.TYPE_FLOAT:
                // return new RookIterCSMFloat(im, bounds);
            case DataBuffer.TYPE_DOUBLE:
                // return new RookIterCSMDouble(im, bounds);
            }
        }

        return new RookIterFallback(im, bounds);
    }

    /**
     * Constructs and returns an instance of RookIter suitable
     * for iterating over the given bounding rectangle within the
     * given Raster source.  If the bounds parameter is null,
     * the entire Raster will be used.
     *
     * @param ras a read-only Raster source.
     * @param bounds the bounding Rectangle for the iterator, or null.
     * @return a RookIter allowing read-only access to the source.
     */
    public static RookIter create(Raster ras,
                                  Rectangle bounds) {
        RenderedImage im = new WrapperRI(ras);
        return create(im, bounds);
    }

    /**
     * Constructs and returns an instance of WritableRookIter suitable for
     * iterating over the given bounding rectangle within the given
     * WritableRenderedImage source.  If the bounds parameter is null,
     * the entire image will be used.
     *
     * @param im a WritableRenderedImage source.
     * @param bounds the bounding Rectangle for the iterator, or null.
     * @return a WritableRookIter allowing read/write access to the source.
     */
    public static WritableRookIter createWritable(WritableRenderedImage im,
                                                  Rectangle bounds) {
        if (bounds == null) {
            bounds = new Rectangle(im.getMinX(), im.getMinY(),
                                   im.getWidth(), im.getHeight());
        }

        SampleModel sm = im.getSampleModel();
        if (sm instanceof ComponentSampleModel) {
            switch (sm.getDataType()) {
            case DataBuffer.TYPE_BYTE:
                // return new WritableRookIterCSMByte(im, bounds);
            case DataBuffer.TYPE_SHORT:
                // return new WritableRookIterCSMShort(im, bounds);
            case DataBuffer.TYPE_USHORT:
                // return new WritableRookIterCSMUShort(im, bounds);
            case DataBuffer.TYPE_INT:
                // return new WritableRookIterCSMInt(im, bounds);
            case DataBuffer.TYPE_FLOAT:
                // return new WritableRookIterCSMFloat(im, bounds);
            case DataBuffer.TYPE_DOUBLE:
                // return new WritableRookIterCSMDouble(im, bounds);
            }
        }

        return new WritableRookIterFallback(im, bounds);
    }

    /**
     * Constructs and returns an instance of WritableRookIter suitable for
     * iterating over the given bounding rectangle within the given
     * WritableRaster source.  If the bounds parameter is null,
     * the entire Raster will be used.
     *
     * @param ras a WritableRaster source.
     * @param bounds the bounding Rectangle for the iterator, or null.
     * @return a WritableRookIter allowing read/write access to the source.
     */
    public static WritableRookIter createWritable(WritableRaster ras,
                                                  Rectangle bounds) {
        WritableRenderedImage im = new WrapperWRI(ras);
        return createWritable(im, bounds);
    }
}
