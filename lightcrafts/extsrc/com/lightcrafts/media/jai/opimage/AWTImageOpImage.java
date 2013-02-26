/*
 * $RCSfile: AWTImageOpImage.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:10 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.opimage;
import java.awt.Canvas;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;
import java.awt.image.ImageObserver;
import java.awt.image.PixelGrabber;
import java.awt.image.Raster;
import java.awt.image.SinglePixelPackedSampleModel;
import java.awt.image.WritableRaster;
import java.util.Map;
import com.lightcrafts.mediax.jai.ImageLayout;
import com.lightcrafts.mediax.jai.PlanarImage;
import com.lightcrafts.mediax.jai.RasterAccessor;
import com.lightcrafts.mediax.jai.RasterFactory;
import com.lightcrafts.mediax.jai.SourcelessOpImage;
import com.lightcrafts.mediax.jai.RasterFormatTag;

/**
 * An <code>OpImage</code> implementing the "AWTImage" operation as
 * described in <code>com.lightcrafts.mediax.jai.operator.AWTImageDescriptor</code>.
 * It takes a regular java.awt.Image and converts it into a
 * com.lightcrafts.mediax.jai.PlanarImage.
 *
 * <p> The layout of the PlanarImage may be specified using the
 * <code>ImageLayout</code> parameter at construction.  The image
 * bounds (minX, minY, width, height), <code>SampleModel</code>, and
 * <code>ColorModel</code>, if supplied, are ignored.  The tile grid
 * offsets will be ignored if neither of the tile dimensions are
 * supplied or equal the respective image dimensions.
 *
 * <p> The image origin is forced to (0,&nbsp0) and the width and height
 * to the width and height, respectively, of the AWT image parameter.
 * If a tile dimension is not set it defaults to the corresponding
 * image dimension.  If neither tile dimension is set or both equal the
 * corresponding image dimensions, the tile grid offsets default to the
 * image origin.
 *
 * <p> The <code>SampleModel</code> is forced to a
 * <code>SinglePixelPackedSampleModel</code> if the tile dimensions equal
 * the image dimensions, otherwise it is forced to a
 * <code>PixelInterleavedSampleModel</code>.  In either case the
 * <code>ColorModel</code> is set using
 * <code>PlanarImage.createColorModel()</code>.
 *
 * @see com.lightcrafts.mediax.jai.operator.AWTImageDescriptor
 * @see AWTImageRIF
 *
 */
final class AWTImageOpImage extends SourcelessOpImage {

    /* The entire image's pixel values. */
    private int[] pixels;

    /* RasterFormatTag for dest sampleModels */
    private RasterFormatTag rasterFormatTag = null;

    private static final ImageLayout layoutHelper (ImageLayout layout,
                                                   Image image) {
        /* Determine image width and height using MediaTracker. */
        MediaTracker tracker = new MediaTracker(new Canvas());
        tracker.addImage(image, 0);
        try {
            tracker.waitForID(0);
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException(JaiI18N.getString("AWTImageOpImage0"));
        }
	if (tracker.isErrorID(0)) {	// not standard file format
            throw new RuntimeException(JaiI18N.getString("AWTImageOpImage1"));
        }
        tracker.removeImage(image);

        // Create layout if none supplied.
        if(layout == null) layout = new ImageLayout();

        // Override minX, minY, width, height
        layout.setMinX(0);
        layout.setMinY(0);
        layout.setWidth(image.getWidth(null));
        layout.setHeight(image.getHeight(null));

        // Override tileWidth, tileHeight if not supplied in layout
        if (!layout.isValid(ImageLayout.TILE_WIDTH_MASK)) {
            layout.setTileWidth(layout.getWidth(null));
        }
        if (!layout.isValid(ImageLayout.TILE_HEIGHT_MASK)) {
            layout.setTileHeight(layout.getHeight(null));
        }

        // Override sampleModel
        // TODO: what if bands != 3?
        if(layout.getTileWidth(null) == layout.getWidth(null) &&
           layout.getTileHeight(null) == layout.getHeight(null)) {
            // Override tile grid offsets so we have a single tile.
            layout.setTileGridXOffset(layout.getMinX(null));
            layout.setTileGridYOffset(layout.getMinY(null));

            int[] bitMasks = new int[] {0x00ff0000, 0x0000ff00, 0x000000ff};
            layout.setSampleModel(
                new SinglePixelPackedSampleModel(DataBuffer.TYPE_INT,
                                                 layout.getWidth(null),
                                                 layout.getHeight(null),
                                                 bitMasks));
        } else {
            layout.setSampleModel(RasterFactory.createPixelInterleavedSampleModel(
                              DataBuffer.TYPE_BYTE,
                              layout.getTileWidth(null),
                              layout.getTileHeight(null),
                              3));
        }

	layout.setColorModel(PlanarImage.createColorModel(layout.getSampleModel(null)));

        return layout;
    }

    /**
     * Constructs an AWTImageOpImage.
     *
     * @param layout  Image layout.
     * @param image   The AWT image.
     */
    public AWTImageOpImage(Map config,
                           ImageLayout layout,
                           Image image) {
        // We don't know the width, height, and sample model yet
        super(layout = layoutHelper(layout, image), config,
              layout.getSampleModel(null),
              layout.getMinX(null), layout.getMinY(null),
              layout.getWidth(null), layout.getHeight(null));

        // Set the format tag if and only if we will use the RasterAccessor.
        if(getTileWidth() != getWidth() || getTileHeight() != getHeight()) {
            rasterFormatTag =
                new RasterFormatTag(getSampleModel(),
                                    RasterAccessor.TAG_BYTE_UNCOPIED);
        }

        // Grab the entire image
        this.pixels = new int[width * height];
        PixelGrabber grabber = new PixelGrabber(image, 0, 0, width, height,
                                                pixels, 0, width);
        try {
            if (!grabber.grabPixels()) {
                if ((grabber.getStatus() & ImageObserver.ABORT) != 0) {
                    throw new RuntimeException(JaiI18N.getString("AWTImageOpImage2"));
		} else {
                    throw new RuntimeException(grabber.getStatus() + JaiI18N.getString("AWTImageOpImage3"));
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException(JaiI18N.getString("AWTImageOpImage4"));
        }
    }

    public Raster computeTile(int tileX, int tileY) {
        if(getTileWidth() == getWidth() && getTileHeight() == getHeight()) {
            DataBuffer dataBuffer = new DataBufferInt(pixels, pixels.length);
            return Raster.createWritableRaster(getSampleModel(),
                                               dataBuffer,
                                               new Point(tileXToX(tileX),
                                                         tileYToY(tileY)));
        }

        return super.computeTile(tileX, tileY);
    }

    protected void computeRect(PlanarImage[] sources,
                               WritableRaster dest,
                               Rectangle destRect) {
        RasterAccessor dst = new RasterAccessor(dest, destRect,
                                 rasterFormatTag,null);

        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();

        int lineStride = dst.getScanlineStride();
        int pixelStride = dst.getPixelStride();

        int lineOffset0 = dst.getBandOffset(0);
        int lineOffset1 = dst.getBandOffset(1);
        int lineOffset2 = dst.getBandOffset(2);

        byte[] data = dst.getByteDataArray(0);

        int offset = (destRect.y - minY) * width + (destRect.x - minX);

        for (int h = 0; h < dheight; h++) {
            int pixelOffset0 = lineOffset0;
            int pixelOffset1 = lineOffset1;
            int pixelOffset2 = lineOffset2;

            lineOffset0 += lineStride;
            lineOffset1 += lineStride;
            lineOffset2 += lineStride;

            int i = offset;
            offset += width;

            for (int w = 0; w < dwidth; w++) {
                data[pixelOffset0] = (byte)((pixels[i] >> 16 ) & 0xFF);
                data[pixelOffset1] = (byte)((pixels[i] >> 8 ) & 0xFF);
                data[pixelOffset2] = (byte)(pixels[i] & 0xFF);

                pixelOffset0 += pixelStride;
                pixelOffset1 += pixelStride;
                pixelOffset2 += pixelStride;
                i++;
            }
        }
    }
}
