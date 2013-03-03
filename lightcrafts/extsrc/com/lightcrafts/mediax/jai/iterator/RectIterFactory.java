/*
 * $RCSfile: RectIterFactory.java,v $
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
import com.lightcrafts.media.jai.iterator.RectIterCSMByte;
// import com.lightcrafts.media.jai.iterator.RectIterCSMShort;
// import com.lightcrafts.media.jai.iterator.RectIterCSMUShort;
// import com.lightcrafts.media.jai.iterator.RectIterCSMInt;
import com.lightcrafts.media.jai.iterator.RectIterCSMFloat;
// import com.lightcrafts.media.jai.iterator.RectIterCSMDouble;
import com.lightcrafts.media.jai.iterator.RectIterFallback;
import com.lightcrafts.media.jai.iterator.WrapperRI;
import com.lightcrafts.media.jai.iterator.WrapperWRI;
import com.lightcrafts.media.jai.iterator.WritableRectIterCSMByte;
// import com.lightcrafts.media.jai.iterator.WritableRectIterCSMShort;
// import com.lightcrafts.media.jai.iterator.WritableRectIterCSMUShort;
// import com.lightcrafts.media.jai.iterator.WritableRectIterCSMInt;
import com.lightcrafts.media.jai.iterator.WritableRectIterCSMFloat;
// import com.lightcrafts.media.jai.iterator.WritableRectIterCSMDouble;
import com.lightcrafts.media.jai.iterator.WritableRectIterFallback;

/**
 * A factory class to instantiate instances of the RectIter and
 * WritableRectIter interfaces on sources of type Raster,
 * RenderedImage, and WritableRenderedImage.
 *
 * @see RectIter
 * @see WritableRectIter
 */
public class RectIterFactory {

    /** Prevent this class from ever being instantiated. */
    private RectIterFactory() {}

    /**
     * Constructs and returns an instance of RectIter suitable
     * for iterating over the given bounding rectangle within the
     * given RenderedImage source.  If the bounds parameter is null,
     * the entire image will be used.
     *
     * @param im a read-only RenderedImage source.
     * @param bounds the bounding Rectangle for the iterator, or null.
     * @return a RectIter allowing read-only access to the source.
     */
    public static RectIter create(RenderedImage im,
                                  Rectangle bounds) {
        if (bounds == null) {
            bounds = new Rectangle(im.getMinX(), im.getMinY(),
                                   im.getWidth(), im.getHeight());
        }

        SampleModel sm = im.getSampleModel();
        if (sm instanceof ComponentSampleModel) {
            switch (sm.getDataType()) {
            case DataBuffer.TYPE_BYTE:
                return new RectIterCSMByte(im, bounds);
            case DataBuffer.TYPE_SHORT:
                // return new RectIterCSMShort(im, bounds);
		break;
            case DataBuffer.TYPE_USHORT:
                // return new RectIterCSMUShort(im, bounds);
		break;
            case DataBuffer.TYPE_INT:
                // return new RectIterCSMInt(im, bounds);
		break;
            case DataBuffer.TYPE_FLOAT:
                return new RectIterCSMFloat(im, bounds);
            case DataBuffer.TYPE_DOUBLE:
                // return new RectIterCSMDouble(im, bounds);
		break;
            }
        }

        return new RectIterFallback(im, bounds);
    }

    /**
     * Constructs and returns an instance of RectIter suitable
     * for iterating over the given bounding rectangle within the
     * given Raster source.  If the bounds parameter is null,
     * the entire Raster will be used.
     *
     * @param ras a read-only Raster source.
     * @param bounds the bounding Rectangle for the iterator, or null.
     * @return a RectIter allowing read-only access to the source.
     */
    public static RectIter create(Raster ras,
                                  Rectangle bounds) {
        RenderedImage im = new WrapperRI(ras);
        return create(im, bounds);
    }

    /**
     * Constructs and returns an instance of WritableRectIter suitable for
     * iterating over the given bounding rectangle within the given
     * WritableRenderedImage source.  If the bounds parameter is null,
     * the entire image will be used.
     *
     * @param im a WritableRenderedImage source.
     * @param bounds the bounding Rectangle for the iterator, or null.
     * @return a WritableRectIter allowing read/write access to the source.
     */
    public static WritableRectIter createWritable(WritableRenderedImage im,
                                                  Rectangle bounds) {
        if (bounds == null) {
            bounds = new Rectangle(im.getMinX(), im.getMinY(),
                                   im.getWidth(), im.getHeight());
        }

        SampleModel sm = im.getSampleModel();
        if (sm instanceof ComponentSampleModel) {
            switch (sm.getDataType()) {
            case DataBuffer.TYPE_BYTE:
                return new WritableRectIterCSMByte(im, bounds);
            case DataBuffer.TYPE_SHORT:
                // return new WritableRectIterCSMShort(im, bounds);
		break;
            case DataBuffer.TYPE_USHORT:
                // return new WritableRectIterCSMUShort(im, bounds);
		break;
            case DataBuffer.TYPE_INT:
                // return new WritableRectIterCSMInt(im, bounds);
		break;
            case DataBuffer.TYPE_FLOAT:
                return new WritableRectIterCSMFloat(im, bounds);
            case DataBuffer.TYPE_DOUBLE:
                // return new WritableRectIterCSMDouble(im, bounds);
		break;
            }
        }

        return new WritableRectIterFallback(im, bounds);
    }

    /**
     * Constructs and returns an instance of WritableRectIter suitable for
     * iterating over the given bounding rectangle within the given
     * WritableRaster source.  If the bounds parameter is null,
     * the entire Raster will be used.
     *
     * @param ras a WritableRaster source.
     * @param bounds the bounding Rectangle for the iterator, or null.
     * @return a WritableRectIter allowing read/write access to the source.
     */
    public static WritableRectIter createWritable(WritableRaster ras,
                                                  Rectangle bounds) {
        WritableRenderedImage im = new WrapperWRI(ras);
        return createWritable(im, bounds);
    }
}
