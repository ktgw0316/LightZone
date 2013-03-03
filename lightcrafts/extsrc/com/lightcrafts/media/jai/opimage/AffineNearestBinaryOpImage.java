/*
 * $RCSfile: AffineNearestBinaryOpImage.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.3 $
 * $Date: 2006/07/21 20:41:27 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.opimage;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferUShort;
import java.awt.image.MultiPixelPackedSampleModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import com.lightcrafts.mediax.jai.ImageLayout;
import com.lightcrafts.mediax.jai.Interpolation;
import com.lightcrafts.mediax.jai.JAI;
import com.lightcrafts.mediax.jai.util.Range;
import com.lightcrafts.mediax.jai.BorderExtender;
import java.util.Map;

/**
 * An OpImage subclass that performs nearest-neighbour Affine mapping
 */
final class AffineNearestBinaryOpImage extends AffineNearestOpImage {

    // The background
    private int black = 0;

    // Since this operation deals with packed binary data, we do not need
    // to expand the IndexColorModel
    private static Map configHelper(Map configuration) {
	
	Map config;

	if (configuration == null) {

	    config = new RenderingHints(JAI.KEY_REPLACE_INDEX_COLOR_MODEL,
					Boolean.FALSE);

	} else {
	    
	    config = configuration;

	    if (!(config.containsKey(JAI.KEY_REPLACE_INDEX_COLOR_MODEL))) {
		RenderingHints hints = (RenderingHints)configuration;
		config = (RenderingHints)hints.clone();
		config.put(JAI.KEY_REPLACE_INDEX_COLOR_MODEL, Boolean.FALSE);
	    }
	}

	return config;
    }

    /**
     * Constructs an AffineNearestBinaryOpImage from a RenderedImage source,
     *
     * @param source a RenderedImage.
     * @param layout an ImageLayout optionally containing the tile grid layout,
     *        SampleModel, and ColorModel, or null.
     * @param interp an Interpolation object to use for resampling
     * @param transform the desired AffineTransform.
     */
    public AffineNearestBinaryOpImage(RenderedImage source,
                                       BorderExtender extender,
                                       Map config,
                                       ImageLayout layout,
                                       AffineTransform transform,
                                       Interpolation interp,
                                       double[] backgroundValues) {
        super(source,
              extender,
              configHelper(config),
              layout,
              transform,
              interp,
              backgroundValues);

        // Propagate source's ColorModel and SampleModel but change tile size.
        if (layout != null) {
            colorModel = layout.getColorModel(source);
        } else {
            colorModel = source.getColorModel();
        }
        sampleModel =
            source.getSampleModel().createCompatibleSampleModel(tileWidth,
                                                                tileHeight);

        // Set the background to color to 1 if the color
        // model specifies that as "black", otherwise 0.
	//
        // This cause problem on the background (Bug 4380285): because
        // (1) if the tile does not intersect with the source, zeroed tile(white
        // is created; (2) if the line does not intersect with the source,
        // zeroed-line is created; (3) if the image area does not equal to
        // the whole destination rectangle, some white line will be displayed.
        //
        //if (colorModel.getRGB(1) == 0xff000000) {
        //    black = 1;
        //}
    }

    /**
     * Performs an affine transform on a specified rectangle. The sources are
     * cobbled.
     *
     * @param sources an array of source Rasters, guaranteed to provide all
     *                necessary source data for computing the output.
     * @param dest a WritableRaster tile containing the area to be computed.
     * @param destRect the rectangle within dest to be processed.
     */
    protected void computeRect(Raster[] sources,
                               WritableRaster dest,
                               Rectangle destRect) {
        Raster source = sources[0];

        switch (source.getSampleModel().getDataType()) {
        case DataBuffer.TYPE_BYTE:
            byteLoop(source, dest, destRect);
            break;

        case DataBuffer.TYPE_INT:
            intLoop(source, dest, destRect);
            break;

        case DataBuffer.TYPE_SHORT:
        case DataBuffer.TYPE_USHORT:
            shortLoop(source, dest, destRect);
            break;
        }
    }

    private void byteLoop(Raster source,
                          WritableRaster dest,
                          Rectangle destRect) {
        float src_rect_x1 = source.getMinX();
        float src_rect_y1 = source.getMinY();
        float src_rect_x2 = src_rect_x1 + source.getWidth();
        float src_rect_y2 = src_rect_y1 + source.getHeight();

        MultiPixelPackedSampleModel sourceSM =
            (MultiPixelPackedSampleModel)source.getSampleModel();
        DataBufferByte sourceDB =
            (DataBufferByte)source.getDataBuffer();
        int sourceTransX = source.getSampleModelTranslateX();
        int sourceTransY = source.getSampleModelTranslateY();
        int sourceDataBitOffset = sourceSM.getDataBitOffset();
        int sourceScanlineStride = sourceSM.getScanlineStride();

        MultiPixelPackedSampleModel destSM =
            (MultiPixelPackedSampleModel)dest.getSampleModel();
        DataBufferByte destDB =
            (DataBufferByte)dest.getDataBuffer();
        int destMinX = dest.getMinX();
        int destMinY = dest.getMinY();
        int destTransX = dest.getSampleModelTranslateX();
        int destTransY = dest.getSampleModelTranslateY();
        int destDataBitOffset = destSM.getDataBitOffset();
        int destScanlineStride = destSM.getScanlineStride();

        byte[] sourceData = sourceDB.getData();
        int sourceDBOffset = sourceDB.getOffset();

        byte[] destData = destDB.getData();
        int destDBOffset = destDB.getOffset();

        Point2D dst_pt = new Point2D.Float();
        Point2D src_pt = new Point2D.Float();

        int dst_min_x = destRect.x;
        int dst_min_y = destRect.y;
        int dst_max_x = destRect.x + destRect.width;
        int dst_max_y = destRect.y + destRect.height;

        int incyStride = incy*sourceScanlineStride;
        int incy1Stride = incy1*sourceScanlineStride;

        black = ((int)backgroundValues[0]) & 1;

        for (int y = dst_min_y; y < dst_max_y; y++)  {
            // Backward map the first point in the line
            // The energy is at the (pt_x + 0.5, pt_y + 0.5)
            dst_pt.setLocation((double)dst_min_x + 0.5,
                               (double)y + 0.5);
            mapDestPoint(dst_pt, src_pt);

            // Get the mapped source coordinates
            float s_x = (float)src_pt.getX();
            float s_y = (float)src_pt.getY();

            // Floor to get the integral coordinate
            int s_ix = (int)Math.floor(s_x);
            int s_iy = (int)Math.floor(s_y);

            double fracx = s_x - (double)s_ix;
            double fracy = s_y - (double)s_iy;

            int ifracx = (int) Math.floor(fracx*geom_frac_max);
            int ifracy = (int) Math.floor(fracy*geom_frac_max);

            int start_s_ix = s_ix;
            int start_s_iy = s_iy;
            int start_ifracx = ifracx;
            int start_ifracy = ifracy;

            // Compute clipMinX, clipMinY
            Range clipRange = 
		performScanlineClipping(src_rect_x1, src_rect_y1,
					// Last point in the source is
					// x2 = x1 + width - 1
					// y2 = y1 + height - 1
					src_rect_x2 - 1, src_rect_y2 - 1,
					s_ix, s_iy,
					ifracx, ifracy,
					dst_min_x, dst_max_x,
					0, 0, 0, 0);
            int clipMinX = ((Integer)clipRange.getMinValue()).intValue();
            int clipMaxX = ((Integer)clipRange.getMaxValue()).intValue();

            if(clipMinX > clipMaxX) continue;

            int destYOffset = (y - destTransY)*
                destScanlineStride + destDBOffset;
            int destXOffset = destDataBitOffset + (dst_min_x - destTransX);

            int sourceYOffset = (s_iy - sourceTransY)*
                sourceScanlineStride + sourceDBOffset;
            int sourceXOffset = s_ix - sourceTransX +
                sourceDataBitOffset;

            for (int x = dst_min_x; x < clipMinX; x++) {

                if (setBackground) {
                    int dindex = destYOffset + (destXOffset >> 3);
                    int dshift = 7 - (destXOffset & 7);
                    int delement = destData[dindex];
                    delement |= black << dshift;
                    destData[dindex] = (byte)delement;
                }

                // walk
                if (ifracx < ifracdx1) {
		    /* 
		    // DEBUG
                    s_ix += incx;
		    */
                    ifracx += ifracdx;
                    sourceXOffset += incx;
                } else {
		    /* 
		    // DEBUG
                    s_ix += incx1;
		    */
                    ifracx -= ifracdx1;
                    sourceXOffset += incx1;
                }

                if (ifracy < ifracdy1) {
		    /* 
		    // DEBUG
                    s_iy += incy;
		    */
                    ifracy += ifracdy;
                    sourceYOffset += incyStride;
                } else {
		    /* 
		    // DEBUG
                    s_iy += incy1;
		    */
                    ifracy -= ifracdy1;
                    sourceYOffset += incy1Stride;
                }

                ++destXOffset;
            }

            for (int x = clipMinX; x < clipMaxX; x++) {
                int sindex = sourceYOffset + (sourceXOffset >> 3);
                byte selement = sourceData[sindex];
                int val = (selement >> (7 - (sourceXOffset & 7))) & 1;

                int dindex = destYOffset + (destXOffset >> 3);
                int dshift = 7 - (destXOffset & 7);
                int delement = destData[dindex];
                delement |= val << dshift;
                destData[dindex] = (byte)delement;

                // walk
                if (ifracx < ifracdx1) {
		    /* 
		    // DEBUG
                    s_ix += incx;
		    */
                    ifracx += ifracdx;
                    sourceXOffset += incx;
                } else {
		    /* 
		    // DEBUG
                    s_ix += incx1;
		    */
                    ifracx -= ifracdx1;
                    sourceXOffset += incx1;
                }

                if (ifracy < ifracdy1) {
		    /* 
		    // DEBUG
                    s_iy += incy;
		    */
                    ifracy += ifracdy;
                    sourceYOffset += incyStride;
                } else {
		    /* 
		    // DEBUG
                    s_iy += incy1;
		    */
                    ifracy -= ifracdy1;
                    sourceYOffset += incy1Stride;
                }

                ++destXOffset;
            }

            for (int x = clipMaxX; x < dst_max_x; x++) {

                if (setBackground) {
                    int dindex = destYOffset + (destXOffset >> 3);
                    int dshift = 7 - (destXOffset & 7);
                    int delement = destData[dindex];
                    delement |= black << dshift;
                    destData[dindex] = (byte)delement;
                }

                // walk
                if (ifracx < ifracdx1) {
		    /* 
		    // DEBUG
                    s_ix += incx;
		    */
                    ifracx += ifracdx;
                    sourceXOffset += incx;
                } else {
		    /* 
		    // DEBUG
                    s_ix += incx1;
		    */
                    ifracx -= ifracdx1;
                    sourceXOffset += incx1;
                }

                if (ifracy < ifracdy1) {
		    /* 
		    // DEBUG
                    s_iy += incy;
		    */
                    ifracy += ifracdy;
                    sourceYOffset += incyStride;
                } else {
		    /* 
		    // DEBUG
                    s_iy += incy1;
		    */
                    ifracy -= ifracdy1;
                    sourceYOffset += incy1Stride;
                }

                ++destXOffset;
            }
        }
    }

    private void shortLoop(Raster source,
                           WritableRaster dest,
                           Rectangle destRect) {
        float src_rect_x1 = source.getMinX();
        float src_rect_y1 = source.getMinY();
        float src_rect_x2 = src_rect_x1 + source.getWidth();
        float src_rect_y2 = src_rect_y1 + source.getHeight();

        MultiPixelPackedSampleModel sourceSM =
            (MultiPixelPackedSampleModel)source.getSampleModel();
        DataBufferUShort sourceDB =
            (DataBufferUShort)source.getDataBuffer();
        int sourceTransX = source.getSampleModelTranslateX();
        int sourceTransY = source.getSampleModelTranslateY();
        int sourceDataBitOffset = sourceSM.getDataBitOffset();
        int sourceScanlineStride = sourceSM.getScanlineStride();

        MultiPixelPackedSampleModel destSM =
            (MultiPixelPackedSampleModel)dest.getSampleModel();
        DataBufferUShort destDB =
            (DataBufferUShort)dest.getDataBuffer();
        int destMinX = dest.getMinX();
        int destMinY = dest.getMinY();
        int destTransX = dest.getSampleModelTranslateX();
        int destTransY = dest.getSampleModelTranslateY();
        int destDataBitOffset = destSM.getDataBitOffset();
        int destScanlineStride = destSM.getScanlineStride();

        short[] sourceData = sourceDB.getData();
        int sourceDBOffset = sourceDB.getOffset();

        short[] destData = destDB.getData();
        int destDBOffset = destDB.getOffset();

        Point2D dst_pt = new Point2D.Float();
        Point2D src_pt = new Point2D.Float();

        int dst_min_x = destRect.x;
        int dst_min_y = destRect.y;
        int dst_max_x = destRect.x + destRect.width;
        int dst_max_y = destRect.y + destRect.height;

        int incyStride = incy*sourceScanlineStride;
        int incy1Stride = incy1*sourceScanlineStride;

        black = ((int)backgroundValues[0]) & 1;

        for (int y = dst_min_y; y < dst_max_y; y++)  {
            // Backward map the first point in the line
            // The energy is at the (pt_x + 0.5, pt_y + 0.5)
            dst_pt.setLocation((double)dst_min_x + 0.5,
                               (double)y + 0.5);
            mapDestPoint(dst_pt, src_pt);

            // Get the mapped source coordinates
            float s_x = (float)src_pt.getX();
            float s_y = (float)src_pt.getY();

            // Floor to get the integral coordinate
            int s_ix = (int)Math.floor(s_x);
            int s_iy = (int)Math.floor(s_y);

            double fracx = s_x - (double)s_ix;
            double fracy = s_y - (double)s_iy;

            int ifracx = (int) Math.floor(fracx*geom_frac_max);
            int ifracy = (int) Math.floor(fracy*geom_frac_max);

            int start_s_ix = s_ix;
            int start_s_iy = s_iy;
            int start_ifracx = ifracx;
            int start_ifracy = ifracy;

            // Compute clipMinX, clipMinY
            Range clipRange = 
		performScanlineClipping(src_rect_x1, src_rect_y1,
					// Last point in the source is
					// x2 = x1 + width - 1
					// y2 = y1 + height - 1
					src_rect_x2 - 1, src_rect_y2 - 1,
					s_ix, s_iy,
					ifracx, ifracy,
					dst_min_x, dst_max_x,
					0, 0, 0, 0);
            int clipMinX = ((Integer)clipRange.getMinValue()).intValue();
            int clipMaxX = ((Integer)clipRange.getMaxValue()).intValue();

            if(clipMinX > clipMaxX) continue;

            int destYOffset = (y - destTransY)*
                destScanlineStride + destDBOffset;
            int destXOffset = destDataBitOffset + (dst_min_x - destTransX);

            int sourceYOffset = (s_iy - sourceTransY)*
                sourceScanlineStride + sourceDBOffset;
            int sourceXOffset = s_ix - sourceTransX +
                sourceDataBitOffset;

            for (int x = dst_min_x; x < clipMinX; x++) {

                if (setBackground) {
                    int dindex = destYOffset + (destXOffset >> 4);
                    int dshift = 15 - (destXOffset & 15);
                    int delement = destData[dindex];
                    delement |= black << dshift;
                    destData[dindex] = (short)delement;
                }

                // walk
                if (ifracx < ifracdx1) {
		    /* 
		    // DEBUG
                    s_ix += incx;
		    */
                    ifracx += ifracdx;
                    sourceXOffset += incx;
                } else {
		    /* 
		    // DEBUG
                    s_ix += incx1;
		    */
                    ifracx -= ifracdx1;
                    sourceXOffset += incx1;
                }

                if (ifracy < ifracdy1) {
		    /* 
		    // DEBUG
                    s_iy += incy;
		    */
                    ifracy += ifracdy;
                    sourceYOffset += incyStride;
                } else {
		    /* 
		    // DEBUG
                    s_iy += incy1;
		    */
                    ifracy -= ifracdy1;
                    sourceYOffset += incy1Stride;
                }

                ++destXOffset;
            }

            for (int x = clipMinX; x < clipMaxX; x++) {
                int sindex = sourceYOffset + (sourceXOffset >> 4);
                short selement = sourceData[sindex];
                int val = (selement >> (15 - (sourceXOffset & 15))) & 1;

                int dindex = destYOffset + (destXOffset >> 4);
                int dshift = 15 - (destXOffset & 15);
                int delement = destData[dindex];
                delement |= val << dshift;
                destData[dindex] = (short)delement;

                // walk
                if (ifracx < ifracdx1) {
		    /* 
		    // DEBUG
                    s_ix += incx;
		    */
                    ifracx += ifracdx;
                    sourceXOffset += incx;
                } else {
		    /* 
		    // DEBUG
                    s_ix += incx1;
		    */
                    ifracx -= ifracdx1;
                    sourceXOffset += incx1;
                }

                if (ifracy < ifracdy1) {
		    /* 
		    // DEBUG
                    s_iy += incy;
		    */
                    ifracy += ifracdy;
                    sourceYOffset += incyStride;
                } else {
		    /* 
		    // DEBUG
                    s_iy += incy1;
		    */
                    ifracy -= ifracdy1;
                    sourceYOffset += incy1Stride;
                }

                ++destXOffset;
            }

            for (int x = clipMaxX; x < dst_max_x; x++) {
                if (setBackground) {
                    int dindex = destYOffset + (destXOffset >> 4);
                    int dshift = 15 - (destXOffset & 15);
                    int delement = destData[dindex];
                    delement |= black << dshift;
                    destData[dindex] = (short)delement;
                }

                // walk
                if (ifracx < ifracdx1) {
		    /* 
		    // DEBUG
                    s_ix += incx;
		    */
                    ifracx += ifracdx;
                    sourceXOffset += incx;
                } else {
		    /* 
		    // DEBUG
                    s_ix += incx1;
		    */
                    ifracx -= ifracdx1;
                    sourceXOffset += incx1;
                }

                if (ifracy < ifracdy1) {
		    /* 
		    // DEBUG
                    s_iy += incy;
		    */
                    ifracy += ifracdy;
                    sourceYOffset += incyStride;
                } else {
		    /* 
		    // DEBUG
                    s_iy += incy1;
		    */
                    ifracy -= ifracdy1;
                    sourceYOffset += incy1Stride;
                }

                ++destXOffset;
            }
        }
    }

    private void intLoop(Raster source,
                         WritableRaster dest,
                         Rectangle destRect) {
        float src_rect_x1 = source.getMinX();
        float src_rect_y1 = source.getMinY();
        float src_rect_x2 = src_rect_x1 + source.getWidth();
        float src_rect_y2 = src_rect_y1 + source.getHeight();

        MultiPixelPackedSampleModel sourceSM =
            (MultiPixelPackedSampleModel)source.getSampleModel();
        DataBufferInt sourceDB =
            (DataBufferInt)source.getDataBuffer();
        int sourceTransX = source.getSampleModelTranslateX();
        int sourceTransY = source.getSampleModelTranslateY();
        int sourceDataBitOffset = sourceSM.getDataBitOffset();
        int sourceScanlineStride = sourceSM.getScanlineStride();

        MultiPixelPackedSampleModel destSM =
            (MultiPixelPackedSampleModel)dest.getSampleModel();
        DataBufferInt destDB =
            (DataBufferInt)dest.getDataBuffer();
        int destMinX = dest.getMinX();
        int destMinY = dest.getMinY();
        int destTransX = dest.getSampleModelTranslateX();
        int destTransY = dest.getSampleModelTranslateY();
        int destDataBitOffset = destSM.getDataBitOffset();
        int destScanlineStride = destSM.getScanlineStride();

        int[] sourceData = sourceDB.getData();
        int sourceDBOffset = sourceDB.getOffset();

        int[] destData = destDB.getData();
        int destDBOffset = destDB.getOffset();

        Point2D dst_pt = new Point2D.Float();
        Point2D src_pt = new Point2D.Float();

        int dst_min_x = destRect.x;
        int dst_min_y = destRect.y;
        int dst_max_x = destRect.x + destRect.width;
        int dst_max_y = destRect.y + destRect.height;

        int incyStride = incy*sourceScanlineStride;
        int incy1Stride = incy1*sourceScanlineStride;

        black = ((int)backgroundValues[0]) & 1;

        for (int y = dst_min_y; y < dst_max_y; y++)  {
            // Backward map the first point in the line
            // The energy is at the (pt_x + 0.5, pt_y + 0.5)
            dst_pt.setLocation((double)dst_min_x + 0.5,
                               (double)y + 0.5);
            mapDestPoint(dst_pt, src_pt);

            // Get the mapped source coordinates
            float s_x = (float)src_pt.getX();
            float s_y = (float)src_pt.getY();

            // Floor to get the integral coordinate
            int s_ix = (int)Math.floor(s_x);
            int s_iy = (int)Math.floor(s_y);

            double fracx = s_x - (double)s_ix;
            double fracy = s_y - (double)s_iy;

            int ifracx = (int) Math.floor(fracx*geom_frac_max);
            int ifracy = (int) Math.floor(fracy*geom_frac_max);

            int start_s_ix = s_ix;
            int start_s_iy = s_iy;
            int start_ifracx = ifracx;
            int start_ifracy = ifracy;

            // Compute clipMinX, clipMinY
            Range clipRange = 
		performScanlineClipping(src_rect_x1, src_rect_y1,
					// Last point in the source is
					// x2 = x1 + width - 1
					// y2 = y1 + height - 1
					src_rect_x2 - 1, src_rect_y2 - 1,
					s_ix, s_iy,
					ifracx, ifracy,
					dst_min_x, dst_max_x,
					0, 0, 0, 0);
            int clipMinX = ((Integer)clipRange.getMinValue()).intValue();
            int clipMaxX = ((Integer)clipRange.getMaxValue()).intValue();

            if(clipMinX > clipMaxX) continue;

            int destYOffset = (y - destTransY)*
                destScanlineStride + destDBOffset;
            int destXOffset = destDataBitOffset + (dst_min_x - destTransX);

            int sourceYOffset = (s_iy - sourceTransY)*
                sourceScanlineStride + sourceDBOffset;
            int sourceXOffset = s_ix - sourceTransX +
                sourceDataBitOffset;

            for (int x = dst_min_x; x < clipMinX; x++) {
                if (setBackground) {
                    int dindex = destYOffset + (destXOffset >> 5);
                    int dshift = 31 - (destXOffset & 31);
                    int delement = destData[dindex];
                    delement |= black << dshift;
                    destData[dindex] = (int)delement;
                }

                // walk
                if (ifracx < ifracdx1) {
		    /* 
		    // DEBUG
                    s_ix += incx;
		    */
                    ifracx += ifracdx;
                    sourceXOffset += incx;
                } else {
		    /* 
		    // DEBUG
                    s_ix += incx1;
		    */
                    ifracx -= ifracdx1;
                    sourceXOffset += incx1;
                }

                if (ifracy < ifracdy1) {
		    /* 
		    // DEBUG
                    s_iy += incy;
		    */
                    ifracy += ifracdy;
                    sourceYOffset += incyStride;
                } else {
		    /* 
		    // DEBUG
                    s_iy += incy1;
		    */
                    ifracy -= ifracdy1;
                    sourceYOffset += incy1Stride;
                }

                ++destXOffset;
            }

            for (int x = clipMinX; x < clipMaxX; x++) {
                int sindex = sourceYOffset + (sourceXOffset >> 5);
                int selement = sourceData[sindex];
                int val = (selement >> (31 - (sourceXOffset & 31))) & 1;

                int dindex = destYOffset + (destXOffset >> 5);
                int dshift = 31 - (destXOffset & 31);
                int delement = destData[dindex];
                delement |= val << dshift;
                destData[dindex] = (int)delement;

                // walk
                if (ifracx < ifracdx1) {
		    /* 
		    // DEBUG
                    s_ix += incx;
		    */
                    ifracx += ifracdx;
                    sourceXOffset += incx;
                } else {
		    /* 
		    // DEBUG
                    s_ix += incx1;
		    */
                    ifracx -= ifracdx1;
                    sourceXOffset += incx1;
                }

                if (ifracy < ifracdy1) {
		    /* 
		    // DEBUG
                    s_iy += incy;
		    */
                    ifracy += ifracdy;
                    sourceYOffset += incyStride;
                } else {
		    /* 
		    // DEBUG
                    s_iy += incy1;
		    */
                    ifracy -= ifracdy1;
                    sourceYOffset += incy1Stride;
                }

                ++destXOffset;
            }

            for (int x = clipMaxX; x < dst_max_x; x++) {
                if (setBackground) {
                    int dindex = destYOffset + (destXOffset >> 5);
                    int dshift = 31 - (destXOffset & 31);
                    int delement = destData[dindex];
                    delement |= black << dshift;
                    destData[dindex] = (int)delement;
                }

                // walk
                if (ifracx < ifracdx1) {
		    /* 
		    // DEBUG
                    s_ix += incx;
		    */
                    ifracx += ifracdx;
                    sourceXOffset += incx;
                } else {
		    /* 
		    // DEBUG
                    s_ix += incx1;
		    */
                    ifracx -= ifracdx1;
                    sourceXOffset += incx1;
                }

                if (ifracy < ifracdy1) {
		    /* 
		    // DEBUG
                    s_iy += incy;
		    */
                    ifracy += ifracdy;
                    sourceYOffset += incyStride;
                } else {
		    /* 
		    // DEBUG
                    s_iy += incy1;
		    */
                    ifracy -= ifracdy1;
                    sourceYOffset += incy1Stride;
                }

                ++destXOffset;
            }
        }
    }
}
