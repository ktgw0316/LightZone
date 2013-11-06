/*
 * $RCSfile: AffineNearestOpImage.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.2 $
 * $Date: 2005/12/20 22:05:47 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.opimage;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import com.lightcrafts.mediax.jai.ImageLayout;
import com.lightcrafts.mediax.jai.Interpolation;
import com.lightcrafts.mediax.jai.RasterAccessor;
import com.lightcrafts.mediax.jai.RasterFormatTag;
import com.lightcrafts.mediax.jai.util.Range;
import java.util.Map;
import com.lightcrafts.mediax.jai.BorderExtender;

/**
 * An OpImage subclass that performs nearest-neighbour Affine mapping
 */
class AffineNearestOpImage extends AffineOpImage {
    /**
     * Constructs an AffineNearestOpImage from a RenderedImage source,
     *
     * @param source a RenderedImage.
     * @param layout an ImageLayout optionally containing the tile grid layout,
     *        SampleModel, and ColorModel, or null.
     * @param interp an Interpolation object to use for resampling
     * @param transform the desired AffineTransform.
     */
    public AffineNearestOpImage(RenderedImage source,
				BorderExtender extender,
                                Map config,
                                ImageLayout layout,
                                AffineTransform transform,
                                Interpolation interp,
                                double[] backgroundValues) {
        super(source,
              extender,
              config,
              layout,
              transform,
              interp,
              backgroundValues);

        // If the source has an IndexColorModel, override the default setting
        // in OpImage. The dest shall have exactly the same SampleModel and
        // ColorModel as the source.
        // Note, in this case, the source should have an integral data type.
        ColorModel srcColorModel = source.getColorModel();
        if (srcColorModel instanceof IndexColorModel) {
             sampleModel = source.getSampleModel().createCompatibleSampleModel(
                                                   tileWidth, tileHeight);
             colorModel = srcColorModel;
        }
    }

    // Scanline clipping stuff

    /**
     * Sets clipMinX, clipMaxX based on s_ix, s_iy, ifracx, ifracy,
     * dst_min_x, and dst_min_y.  Padding factors are added and
     * subtracted from the source bounds as given by
     * src_rect_{x,y}{1,2}.  For example, for nearest-neighbor interpo
     * the padding factors should be set to (0, 0, 0, 0); for
     * bilinear, (0, 1, 0, 1); and for bicubic, (1, 2, 1, 2).
     *
     * <p> The returned Range object will be for the Integer class and
     * will contain extrema equivalent to clipMinX and clipMaxX.
     */
    protected Range performScanlineClipping(float src_rect_x1,
                                            float src_rect_y1,
                                            float src_rect_x2,
                                            float src_rect_y2,
                                            int s_ix, int s_iy,
                                            int ifracx, int ifracy,
                                            int dst_min_x, int dst_max_x,
                                            int lpad, int rpad,
                                            int tpad, int bpad) {
        int clipMinX = dst_min_x;
        int clipMaxX = dst_max_x;

        long xdenom = incx*geom_frac_max + ifracdx;
        if (xdenom != 0) {
            long clipx1 = (long)src_rect_x1 + lpad;
            long clipx2 = (long)src_rect_x2 - rpad;

            long x1 = ((clipx1 - s_ix)*geom_frac_max - ifracx) +
                dst_min_x*xdenom;
            long x2 = ((clipx2 - s_ix)*geom_frac_max - ifracx) +
                dst_min_x*xdenom;

            // Moving backwards, switch roles of left and right edges
            if (xdenom < 0) {
                long tmp = x1;
                x1 = x2;
                x2 = tmp;
            }

            int dx1 = ceilRatio(x1, xdenom);
            clipMinX = Math.max(clipMinX, dx1);

            int dx2 = floorRatio(x2, xdenom) + 1;
            clipMaxX = Math.min(clipMaxX, dx2);
        } else {
            // xdenom == 0, all points have same x coordinate as the first
            if (s_ix < src_rect_x1 || s_ix >= src_rect_x2) {
                clipMinX = clipMaxX = dst_min_x;
                return new Range(Integer.class,
                                 new Integer(clipMinX),
                                 new Integer(clipMaxX));
            }
        }

        long ydenom = incy*geom_frac_max + ifracdy;
        if (ydenom != 0) {
            long clipy1 = (long)src_rect_y1 + tpad;
            long clipy2 = (long)src_rect_y2 - bpad;

            long y1 = ((clipy1 - s_iy)*geom_frac_max - ifracy) +
                dst_min_x*ydenom;
            long y2 = ((clipy2 - s_iy)*geom_frac_max - ifracy) +
                dst_min_x*ydenom;

            // Moving backwards, switch roles of top and bottom edges
            if (ydenom < 0) {
                long tmp = y1;
                y1 = y2;
                y2 = tmp;
            }

            int dx1 = ceilRatio(y1, ydenom);
            clipMinX = Math.max(clipMinX, dx1);

            int dx2 = floorRatio(y2, ydenom) + 1;
            clipMaxX = Math.min(clipMaxX, dx2);
        } else {
            // ydenom == 0, all points have same y coordinate as the first
            if (s_iy < src_rect_y1 || s_iy >= src_rect_y2) {
                clipMinX = clipMaxX = dst_min_x;
            }
        }

        if (clipMinX > dst_max_x)
            clipMinX = dst_max_x;
        if (clipMaxX < dst_min_x)
            clipMaxX = dst_min_x;

        return new Range(Integer.class,
                         new Integer(clipMinX),
                         new Integer(clipMaxX));
    }

    /**
     * Sets s_ix, s_iy, ifracx, ifracy to their values at x == clipMinX
     * from their initial values at x == dst_min_x.
     *
     * <p> The return Point array will contain the updated values of s_ix
     * and s_iy in the first element and those of ifracx and ifracy in the
     * second element.
     */
    protected Point[] advanceToStartOfScanline(int dst_min_x, int clipMinX,
                                               int s_ix, int s_iy,
                                               int ifracx, int ifracy) {
        // Skip output up to clipMinX
        long skip = clipMinX - dst_min_x;
        long dx =
            ((long)ifracx + skip*ifracdx)/geom_frac_max;
        long dy =
            ((long)ifracy + skip*ifracdy)/geom_frac_max;
        s_ix += skip*incx + (int)dx;
        s_iy += skip*incy + (int)dy;

	long lfracx = ifracx + skip*ifracdx;
	if (lfracx >= 0) {
	    ifracx = (int)(lfracx % geom_frac_max);
        } else {
	    ifracx = (int)(-(-lfracx % geom_frac_max));
        }

        long lfracy = ifracy + skip*ifracdy;
        if (lfracy >= 0) {
            ifracy = (int)(lfracy % geom_frac_max);
        } else {
            ifracy = (int)(-(-lfracy % geom_frac_max));
        }

        return new Point[] {new Point(s_ix, s_iy), new Point(ifracx, ifracy)};
    }

    // computeRect() and data type-specific loop methods.

    /**
     * Performs an affine transform on a specified rectangle. The sources are
     * cobbled.
     *
     * @param sources an array of source Rasters, guaranteed to provide all
     *                necessary source data for computing the output.
     * @param dest a WritableRaster tile containing the area to be computed.
     * @param destRect the rectangle within dest to be processed.
     */
    protected void computeRect(Raster [] sources,
                               WritableRaster dest,
                               Rectangle destRect) {
        // Retrieve format tags.
        RasterFormatTag[] formatTags = getFormatTags();

        Raster source = sources[0];

        Rectangle srcRect = source.getBounds();

        int srcRectX = srcRect.x;
        int srcRectY = srcRect.y;

        //
        // Get data for the source rectangle & the destination rectangle
        //
        RasterAccessor srcAccessor =
            new RasterAccessor(source,
                               srcRect,
                               formatTags[0],
                               getSourceImage(0).getColorModel());

        RasterAccessor dstAccessor =
            new RasterAccessor(dest,
                               destRect,
                               formatTags[1],
                               getColorModel());

        switch (dstAccessor.getDataType()) {
        case DataBuffer.TYPE_BYTE:
            int dstNumBands = dstAccessor.getNumBands();
            if (dstNumBands == 1) {
                byteLoop_1band(srcAccessor,
                               destRect,
                               srcRectX,
                               srcRectY,
                               dstAccessor);
            } else if (dstNumBands == 3) {
                byteLoop_3band(srcAccessor,
                               destRect,
                               srcRectX,
                               srcRectY,
                               dstAccessor);
            } else {
                byteLoop(srcAccessor,
                         destRect,
                         srcRectX,
                         srcRectY,
                         dstAccessor);
            }
            break;

        case DataBuffer.TYPE_INT:
            intLoop(srcAccessor,
                    destRect,
                    srcRectX,
                    srcRectY,
                    dstAccessor);
            break;

        case DataBuffer.TYPE_SHORT:
        case DataBuffer.TYPE_USHORT:
            shortLoop(srcAccessor,
                      destRect,
                      srcRectX,
                      srcRectY,
                      dstAccessor);
            break;

        case DataBuffer.TYPE_FLOAT:
            floatLoop(srcAccessor,
                      destRect,
                      srcRectX,
                      srcRectY,
                      dstAccessor);
            break;

        case DataBuffer.TYPE_DOUBLE:
            doubleLoop(srcAccessor,
                       destRect,
                       srcRectX,
                       srcRectY,
                       dstAccessor);
            break;
        }

        //
        // If the RasterAccessor object set up a temporary buffer for the
        // op to write to, tell the RasterAccessor to write that data
        // to the raster, that we're done with it.
        //
         if (dstAccessor.isDataCopy()) {
            dstAccessor.clampDataArrays();
            dstAccessor.copyDataToRaster();
        }
    }

    private void byteLoop(RasterAccessor src,
                          Rectangle destRect,
                          int srcRectX,
                          int srcRectY,
                          RasterAccessor dst) {

        float src_rect_x1 = src.getX();
        float src_rect_y1 = src.getY();
        float src_rect_x2 = src_rect_x1 + src.getWidth();
        float src_rect_y2 = src_rect_y1 + src.getHeight();

        float s_x, s_y;

        int src_x, src_y, src_pos;

        double fracx, fracy;

        int dstPixelOffset;
        int dstOffset = 0;

        Point2D dst_pt = new Point2D.Float();
        Point2D src_pt = new Point2D.Float();

        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();

        byte dstDataArrays[][] = dst.getByteDataArrays();
        int dstBandOffsets[] = dst.getBandOffsets();
        int dstPixelStride = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();

        byte srcDataArrays[][] = src.getByteDataArrays();
        int bandOffsets[] = src.getBandOffsets();
        int srcPixelStride = src.getPixelStride();
        int srcScanlineStride = src.getScanlineStride();

        int dst_num_bands = dst.getNumBands();

        int dst_min_x = destRect.x;
        int dst_min_y = destRect.y;
        int dst_max_x = destRect.x + destRect.width;
        int dst_max_y = destRect.y + destRect.height;

        int incxStride = incx*srcPixelStride;
        int incx1Stride = incx1*srcPixelStride;
        int incyStride = incy*srcScanlineStride;
        int incy1Stride = incy1*srcScanlineStride;

        byte[] backgroundByte = new byte[dst_num_bands];
	for (int i = 0; i < dst_num_bands; i++)
	    backgroundByte[i] = (byte)backgroundValues[i];

        for (int y = dst_min_y; y < dst_max_y; y++)  {
            dstPixelOffset = dstOffset;

            // Backward map the first point in the line
            // The energy is at the (pt_x + 0.5, pt_y + 0.5)
            dst_pt.setLocation((double)dst_min_x + 0.5,
                               (double)y + 0.5);

            mapDestPoint(dst_pt, src_pt);

            // Get the mapped source coordinates
            s_x = (float)src_pt.getX();
            s_y = (float)src_pt.getY();

            // Floor to get the integral coordinate
            int s_ix = (int) Math.floor(s_x);
            int s_iy = (int) Math.floor(s_y);

            fracx = s_x - (double)s_ix;
            fracy = s_y - (double)s_iy;

            int ifracx = (int) Math.floor(fracx * geom_frac_max);
            int ifracy = (int) Math.floor(fracy * geom_frac_max);

            // Compute clipMinX, clipMinY
            Range clipRange = 
		performScanlineClipping(src_rect_x1, src_rect_y1,
					// Last point in the source is
					// x2 = x1 + width - 1
					// y2 = y1 + height - 1
					src_rect_x2, src_rect_y2,
					s_ix, s_iy,
					ifracx, ifracy,
					dst_min_x, dst_max_x,
					0, 0, 0, 0);
            int clipMinX = ((Integer)clipRange.getMinValue()).intValue();
            int clipMaxX = ((Integer)clipRange.getMaxValue()).intValue();

            // Advance s_ix, s_iy, ifracx, ifracy
            Point[] startPts = advanceToStartOfScanline(dst_min_x, clipMinX,
                                                        s_ix, s_iy,
                                                        ifracx, ifracy);
            s_ix = startPts[0].x;
            s_iy = startPts[0].y;
            ifracx = startPts[1].x;
            ifracy = startPts[1].y;

            // Translate to/from SampleModel space & Raster space
            src_pos = (s_iy - srcRectY)*srcScanlineStride +
                (s_ix - srcRectX)*srcPixelStride;

            if (setBackground) {
                for (int x = dst_min_x; x < clipMinX; x++) {
                    for (int k2=0; k2 < dst_num_bands; k2++)
                        dstDataArrays[k2]
                            [dstPixelOffset+dstBandOffsets[k2]] =
                            backgroundByte[k2];
                    dstPixelOffset += dstPixelStride;
                }
            } else             // Advance to first pixel
                dstPixelOffset += (clipMinX - dst_min_x)*dstPixelStride;

            for (int x = clipMinX; x < clipMaxX; x++) {
                for (int k2=0; k2 < dst_num_bands; k2++) {
                    dstDataArrays[k2]
                        [dstPixelOffset+dstBandOffsets[k2]] =
                        srcDataArrays[k2][src_pos + bandOffsets[k2]];
                }

                // walk
                if (ifracx < ifracdx1) {
		    /* 
		    // DEBUG
                    s_ix += incx;
		    */
                    src_pos += incxStride;
                    ifracx += ifracdx;
                } else {
		    /* 
		    // DEBUG
                    s_ix += incx1;
		    */
                    src_pos += incx1Stride;
                    ifracx -= ifracdx1;
                }

                if (ifracy < ifracdy1) {
		    /* 
		    // DEBUG
                    s_iy += incy;
		    */
                    src_pos += incyStride;
                    ifracy += ifracdy;
                } else {
		    /* 
		    // DEBUG
                    s_iy += incy1;
		    */
                    src_pos += incy1Stride;
                    ifracy -= ifracdy1;
                }

                // Go to next pixel
                dstPixelOffset += dstPixelStride;
            }

            if (setBackground && clipMinX <= clipMaxX) {
                for (int x = clipMaxX; x < dst_max_x ; x++) {
                    for (int k2=0; k2 < dst_num_bands; k2++)
                        dstDataArrays[k2]
                            [dstPixelOffset+dstBandOffsets[k2]] =
                            backgroundByte[k2];
                    dstPixelOffset += dstPixelStride;
                }
            }

            // Go to the next line in the destination rectangle
            dstOffset += dstScanlineStride;
        }

    }

    private void byteLoop_1band(RasterAccessor src,
                                Rectangle destRect,
                                int srcRectX,
                                int srcRectY,
                                RasterAccessor dst) {

        float src_rect_x1 = src.getX();
        float src_rect_y1 = src.getY();
        float src_rect_x2 = src_rect_x1 + src.getWidth();
        float src_rect_y2 = src_rect_y1 + src.getHeight();

        float s_x, s_y;

        int src_x, src_y, src_pos;

        double fracx, fracy;

        int dstPixelOffset;
        int dstOffset = 0;

        Point2D dst_pt = new Point2D.Float();
        Point2D src_pt = new Point2D.Float();

        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();

        byte dstDataArrays[][] = dst.getByteDataArrays();
        int dstBandOffsets[] = dst.getBandOffsets();
        int dstPixelStride = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();

        byte[] dstDataArray0 = dstDataArrays[0];
        int dstBandOffset0 = dstBandOffsets[0];

        byte srcDataArrays[][] = src.getByteDataArrays();
        int bandOffsets[] = src.getBandOffsets();
        int srcPixelStride = src.getPixelStride();
        int srcScanlineStride = src.getScanlineStride();

        byte[] srcDataArray0 = srcDataArrays[0];
        int bandOffsets0 = bandOffsets[0];

        int dst_min_x = destRect.x;
        int dst_min_y = destRect.y;
        int dst_max_x = destRect.x + destRect.width;
        int dst_max_y = destRect.y + destRect.height;

        int incxStride = incx*srcPixelStride;
        int incx1Stride = incx1*srcPixelStride;
        int incyStride = incy*srcScanlineStride;
        int incy1Stride = incy1*srcScanlineStride;

	byte backgroundByte = (byte)backgroundValues[0];

        for (int y = dst_min_y; y < dst_max_y; y++)  {
            dstPixelOffset = dstOffset;

            // Backward map the first point in the line
            // The energy is at the (pt_x + 0.5, pt_y + 0.5)
            dst_pt.setLocation((double)dst_min_x + 0.5,
                               (double)y + 0.5);

            mapDestPoint(dst_pt, src_pt);

            // Get the mapped source coordinates
            s_x = (float)src_pt.getX();
            s_y = (float)src_pt.getY();

            // Floor to get the integral coordinate
            int s_ix = (int) Math.floor(s_x);
            int s_iy = (int) Math.floor(s_y);

            fracx = s_x - (double)s_ix;
            fracy = s_y - (double)s_iy;

            int ifracx = (int) Math.floor(fracx * geom_frac_max);
            int ifracy = (int) Math.floor(fracy * geom_frac_max);

            // Compute clipMinX, clipMinY
            Range clipRange = 
		performScanlineClipping(src_rect_x1, src_rect_y1,
					// Last point in the source is
					// x2 = x1 + width - 1
					// y2 = y1 + height - 1
					src_rect_x2, src_rect_y2,
					s_ix, s_iy,
					ifracx, ifracy,
					dst_min_x, dst_max_x,
					0, 0, 0, 0);
            int clipMinX = ((Integer)clipRange.getMinValue()).intValue();
            int clipMaxX = ((Integer)clipRange.getMaxValue()).intValue();

            // Advance s_ix, s_iy, ifracx, ifracy
            Point[] startPts = advanceToStartOfScanline(dst_min_x, clipMinX,
                                                        s_ix, s_iy,
                                                        ifracx, ifracy);
            s_ix = startPts[0].x;
            s_iy = startPts[0].y;
            ifracx = startPts[1].x;
            ifracy = startPts[1].y;

            // Translate to/from SampleModel space & Raster space
            src_pos = (s_iy - srcRectY)*srcScanlineStride +
                (s_ix - srcRectX)*srcPixelStride;

            if (setBackground) {
                for (int x = dst_min_x; x < clipMinX; x++) {
                    dstDataArray0[dstPixelOffset + dstBandOffset0] =
                            backgroundByte;
                    dstPixelOffset += dstPixelStride;
                }
            } else             // Advance to first pixel
                dstPixelOffset += (clipMinX - dst_min_x)*dstPixelStride;

            for (int x = clipMinX; x < clipMaxX; x++) {
                dstDataArray0[dstPixelOffset + dstBandOffset0] =
                    srcDataArray0[src_pos + bandOffsets0];

                // walk
                if (ifracx < ifracdx1) {
		    /* 
		    // DEBUG
                    s_ix += incx;
		    */
                    src_pos += incxStride;
                    ifracx += ifracdx;
                } else {
		    /* 
		    // DEBUG
                    s_ix += incx1;
		    */
                    src_pos += incx1Stride;
                    ifracx -= ifracdx1;
                }

                if (ifracy < ifracdy1) {
		    /* 
		    // DEBUG
                    s_iy += incy;
		    */
                    src_pos += incyStride;
                    ifracy += ifracdy;
                } else {
		    /* 
		    // DEBUG
                    s_iy += incy1;
		    */
                    src_pos += incy1Stride;
                    ifracy -= ifracdy1;
                }

                // Go to next pixel
                dstPixelOffset += dstPixelStride;
            }

            if (setBackground && clipMinX <= clipMaxX) {
                for (int x = clipMaxX; x < dst_max_x; x++) {
                    dstDataArray0[dstPixelOffset + dstBandOffset0] =
                            backgroundByte;
                    dstPixelOffset += dstPixelStride;
                }
            }

            // Go to the next line in the destination rectangle
            dstOffset += dstScanlineStride;
        }
    }

    private void byteLoop_3band(RasterAccessor src,
                                Rectangle destRect,
                                int srcRectX,
                                int srcRectY,
                                RasterAccessor dst) {

        float src_rect_x1 = src.getX();
        float src_rect_y1 = src.getY();
        float src_rect_x2 = src_rect_x1 + src.getWidth();
        float src_rect_y2 = src_rect_y1 + src.getHeight();

        float s_x, s_y;

        int src_x, src_y, src_pos;

        double fracx, fracy;

        int dstPixelOffset;
        int dstOffset = 0;

        Point2D dst_pt = new Point2D.Float();
        Point2D src_pt = new Point2D.Float();

        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();

        byte dstDataArrays[][] = dst.getByteDataArrays();
        int dstBandOffsets[] = dst.getBandOffsets();
        int dstPixelStride = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();

        byte[] dstDataArray0 = dstDataArrays[0];
        byte[] dstDataArray1 = dstDataArrays[1];
        byte[] dstDataArray2 = dstDataArrays[2];

        int dstBandOffset0 = dstBandOffsets[0];
        int dstBandOffset1 = dstBandOffsets[1];
        int dstBandOffset2 = dstBandOffsets[2];

        byte srcDataArrays[][] = src.getByteDataArrays();
        int bandOffsets[] = src.getBandOffsets();
        int srcPixelStride = src.getPixelStride();
        int srcScanlineStride = src.getScanlineStride();

        byte[] srcDataArray0 = srcDataArrays[0];
        byte[] srcDataArray1 = srcDataArrays[1];
        byte[] srcDataArray2 = srcDataArrays[2];

        int bandOffsets0 = bandOffsets[0];
        int bandOffsets1 = bandOffsets[1];
        int bandOffsets2 = bandOffsets[2];

        int dst_min_x = destRect.x;
        int dst_min_y = destRect.y;
        int dst_max_x = destRect.x + destRect.width;
        int dst_max_y = destRect.y + destRect.height;

        int incxStride = incx*srcPixelStride;
        int incx1Stride = incx1*srcPixelStride;
        int incyStride = incy*srcScanlineStride;
        int incy1Stride = incy1*srcScanlineStride;

        byte background0 = (byte)backgroundValues[0];
        byte background1 = (byte)backgroundValues[1];
        byte background2 = (byte)backgroundValues[2];

        for (int y = dst_min_y; y < dst_max_y; y++)  {
            dstPixelOffset = dstOffset;

            // Backward map the first point in the line
            // The energy is at the (pt_x + 0.5, pt_y + 0.5)
            dst_pt.setLocation((double)dst_min_x + 0.5,
                               (double)y + 0.5);

            mapDestPoint(dst_pt, src_pt);

            // Get the mapped source coordinates
            s_x = (float)src_pt.getX();
            s_y = (float)src_pt.getY();

            // Floor to get the integral coordinate
            int s_ix = (int) Math.floor(s_x);
            int s_iy = (int) Math.floor(s_y);

            fracx = s_x - (double)s_ix;
            fracy = s_y - (double)s_iy;

            int ifracx = (int) Math.floor(fracx * geom_frac_max);
            int ifracy = (int) Math.floor(fracy * geom_frac_max);

            // Compute clipMinX, clipMinY
            Range clipRange = 
		performScanlineClipping(src_rect_x1, src_rect_y1,
					// Last point in the source is
					// x2 = x1 + width - 1
					// y2 = y1 + height - 1
					src_rect_x2, src_rect_y2,
					s_ix, s_iy,
					ifracx, ifracy,
					dst_min_x, dst_max_x,
					0, 0, 0, 0);
            int clipMinX = ((Integer)clipRange.getMinValue()).intValue();
            int clipMaxX = ((Integer)clipRange.getMaxValue()).intValue();

            // Advance s_ix, s_iy, ifracx, ifracy
            Point[] startPts = advanceToStartOfScanline(dst_min_x, clipMinX,
                                                        s_ix, s_iy,
                                                        ifracx, ifracy);
            s_ix = startPts[0].x;
            s_iy = startPts[0].y;
            ifracx = startPts[1].x;
            ifracy = startPts[1].y;

            // Translate to/from SampleModel space & Raster space
            src_pos = (s_iy - srcRectY)*srcScanlineStride +
                (s_ix - srcRectX)*srcPixelStride;

            if (setBackground) {
                for (int x = dst_min_x; x < clipMinX; x++) {
                    dstDataArray0[dstPixelOffset + dstBandOffset0] =
                        background0;
                    dstDataArray1[dstPixelOffset + dstBandOffset1] =
                        background1;
                    dstDataArray2[dstPixelOffset + dstBandOffset2] =
                        background2;

                    dstPixelOffset += dstPixelStride;
                }
            } else             // Advance to first pixel
                dstPixelOffset += (clipMinX - dst_min_x)*dstPixelStride;

            for (int x = clipMinX; x < clipMaxX; x++) {
                dstDataArray0[dstPixelOffset + dstBandOffset0] =
                    srcDataArray0[src_pos + bandOffsets0];
                dstDataArray1[dstPixelOffset + dstBandOffset1] =
                    srcDataArray1[src_pos + bandOffsets1];
                dstDataArray2[dstPixelOffset + dstBandOffset2] =
                    srcDataArray2[src_pos + bandOffsets2];

                // walk
                if (ifracx < ifracdx1) {
		    /* 
		    // DEBUG
                    s_ix += incx;
		    */
                    src_pos += incxStride;
                    ifracx += ifracdx;
                } else {
		    /* 
		    // DEBUG
                    s_ix += incx1;
		    */
                    src_pos += incx1Stride;
                    ifracx -= ifracdx1;
                }

                if (ifracy < ifracdy1) {
		    /* 
		    // DEBUG
                    s_iy += incy;
		    */
                    src_pos += incyStride;
                    ifracy += ifracdy;
                } else {
		    /* 
		    // DEBUG
                    s_iy += incy1;
		    */
                    src_pos += incy1Stride;
                    ifracy -= ifracdy1;
                }

                // Go to next pixel
                dstPixelOffset += dstPixelStride;
            }

            if (setBackground && clipMinX <= clipMaxX) {
                for (int x = clipMaxX; x < dst_max_x; x++) {
                    dstDataArray0[dstPixelOffset + dstBandOffset0] =
                        background0;
                    dstDataArray1[dstPixelOffset + dstBandOffset1] =
                        background1;
                    dstDataArray2[dstPixelOffset + dstBandOffset2] =
                        background2;
                    dstPixelOffset += dstPixelStride;
                }
            }

            // Go to the next line in the destination rectangle
            dstOffset += dstScanlineStride;
        }
    }

    private void intLoop(RasterAccessor src,
                         Rectangle destRect,
                         int srcRectX,
                         int srcRectY,
                         RasterAccessor dst) {

        float src_rect_x1 = src.getX();
        float src_rect_y1 = src.getY();
        float src_rect_x2 = src_rect_x1 + src.getWidth();
        float src_rect_y2 = src_rect_y1 + src.getHeight();

        float s_x, s_y;

        int src_x, src_y, src_pos;

        double fracx, fracy;

        int dstPixelOffset;
        int dstOffset = 0;

        Point2D dst_pt = new Point2D.Float();
        Point2D src_pt = new Point2D.Float();

        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();

        int dstDataArrays[][] = dst.getIntDataArrays();
        int dstBandOffsets[] = dst.getBandOffsets();
        int dstPixelStride = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();

        int srcDataArrays[][] = src.getIntDataArrays();
        int bandOffsets[] = src.getBandOffsets();
        int srcPixelStride = src.getPixelStride();
        int srcScanlineStride = src.getScanlineStride();

        int dst_num_bands = dst.getNumBands();

        int dst_min_x = destRect.x;
        int dst_min_y = destRect.y;
        int dst_max_x = destRect.x + destRect.width;
        int dst_max_y = destRect.y + destRect.height;

        int incxStride = incx*srcPixelStride;
        int incx1Stride = incx1*srcPixelStride;
        int incyStride = incy*srcScanlineStride;
        int incy1Stride = incy1*srcScanlineStride;

	int[] backgroundInt = new int[dst_num_bands];
	for (int i = 0; i < dst_num_bands; i++)
	    backgroundInt[i] = (int)backgroundValues[i];

        for (int y = dst_min_y; y < dst_max_y; y++)  {
            dstPixelOffset = dstOffset;

            // Backward map the first point in the line
            // The energy is at the (pt_x + 0.5, pt_y + 0.5)
            dst_pt.setLocation((double)dst_min_x + 0.5,
                               (double)y + 0.5);
            mapDestPoint(dst_pt, src_pt);

            // Get the mapped source coordinates
            s_x = (float)src_pt.getX();
            s_y = (float)src_pt.getY();

            // Floor value to get the integral coordinate
            int s_ix = (int) Math.floor(s_x);
            int s_iy = (int) Math.floor(s_y);

            fracx = s_x - (double)s_ix;
            fracy = s_y - (double)s_iy;

            int ifracx = (int) Math.floor(fracx * geom_frac_max);
            int ifracy = (int) Math.floor(fracy * geom_frac_max);

            // Compute clipMinX, clipMinY
            Range clipRange = 
		performScanlineClipping(src_rect_x1, src_rect_y1,
					// Last point in the source is
					// x2 = x1 + width - 1
					// y2 = y1 + height - 1
					src_rect_x2, src_rect_y2,
					s_ix, s_iy,
					ifracx, ifracy,
					dst_min_x, dst_max_x,
					0, 0, 0, 0);
            int clipMinX = ((Integer)clipRange.getMinValue()).intValue();
            int clipMaxX = ((Integer)clipRange.getMaxValue()).intValue();

            // Advance s_ix, s_iy, ifracx, ifracy
            Point[] startPts = advanceToStartOfScanline(dst_min_x, clipMinX,
                                                        s_ix, s_iy,
                                                        ifracx, ifracy);
            s_ix = startPts[0].x;
            s_iy = startPts[0].y;
            ifracx = startPts[1].x;
            ifracy = startPts[1].y;

            // Translate to/from SampleModel space & Raster space
            src_pos = (s_iy - srcRectY)*srcScanlineStride +
                (s_ix - srcRectX)*srcPixelStride;

            if (setBackground) {
                for (int x = dst_min_x; x < clipMinX; x++) {
                    for (int k2=0; k2 < dst_num_bands; k2++)
                        dstDataArrays[k2]
                            [dstPixelOffset+dstBandOffsets[k2]] =
                            backgroundInt[k2];
                    dstPixelOffset += dstPixelStride;
                }
            } else             // Advance to first pixel
                dstPixelOffset += (clipMinX - dst_min_x)*dstPixelStride;

            for (int x = clipMinX; x < clipMaxX; x++) {
                for (int k2=0; k2 < dst_num_bands; k2++) {
                    dstDataArrays[k2]
                        [dstPixelOffset+dstBandOffsets[k2]] =
                        srcDataArrays[k2][src_pos + bandOffsets[k2]];
                }

                // walk
                if (ifracx < ifracdx1) {
		    /* 
		    // DEBUG
                    s_ix += incx;
		    */
                    src_pos += incxStride;
                    ifracx += ifracdx;
                } else {
		    /* 
		    // DEBUG
                    s_ix += incx1;
		    */
                    src_pos += incx1Stride;
                    ifracx -= ifracdx1;
                }

                if (ifracy < ifracdy1) {
		    /* 
		    // DEBUG
                    s_iy += incy;
		    */
                    src_pos += incyStride;
                    ifracy += ifracdy;
                } else {
		    /* 
		    // DEBUG
                    s_iy += incy1;
		    */
                    src_pos += incy1Stride;
                    ifracy -= ifracdy1;
                }

                dstPixelOffset += dstPixelStride;
            }

            if (setBackground && clipMinX <= clipMaxX) {
                for (int x = clipMaxX; x < dst_max_x ; x++) {
                    for (int k2=0; k2 < dst_num_bands; k2++)
                        dstDataArrays[k2]
                            [dstPixelOffset+dstBandOffsets[k2]] =
                            backgroundInt[k2];
                    dstPixelOffset += dstPixelStride;
                }
            }

            dstOffset += dstScanlineStride;
        }
    }

    private void shortLoop(RasterAccessor src,
                           Rectangle destRect,
                           int srcRectX,
                           int srcRectY,
                           RasterAccessor dst) {

        float src_rect_x1 = src.getX();
        float src_rect_y1 = src.getY();
        float src_rect_x2 = src_rect_x1 + src.getWidth();
        float src_rect_y2 = src_rect_y1 + src.getHeight();

        float s_x, s_y;

        int src_x, src_y, src_pos;

        double fracx, fracy;

        int dstPixelOffset;
        int dstOffset = 0;

        Point2D dst_pt = new Point2D.Float();
        Point2D src_pt = new Point2D.Float();

        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();

        short dstDataArrays[][] = dst.getShortDataArrays();
        int dstBandOffsets[] = dst.getBandOffsets();
        int dstPixelStride = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();

        short srcDataArrays[][] = src.getShortDataArrays();
        int bandOffsets[] = src.getBandOffsets();
        int srcPixelStride = src.getPixelStride();
        int srcScanlineStride = src.getScanlineStride();

        int dst_num_bands = dst.getNumBands();

        int dst_min_x = destRect.x;
        int dst_min_y = destRect.y;
        int dst_max_x = destRect.x + destRect.width;
        int dst_max_y = destRect.y + destRect.height;

        int incxStride = incx*srcPixelStride;
        int incx1Stride = incx1*srcPixelStride;
        int incyStride = incy*srcScanlineStride;
        int incy1Stride = incy1*srcScanlineStride;

	short[] backgroundShort = new short[dst_num_bands];
	for (int i = 0; i < dst_num_bands; i++)
	    backgroundShort[i] = (short)backgroundValues[i];

        for (int y = dst_min_y; y < dst_max_y; y++)  {
            dstPixelOffset = dstOffset;

            // Backward map the first point in the line
            // The energy is at the (pt_x + 0.5, pt_y + 0.5)
            dst_pt.setLocation((double)dst_min_x + 0.5,
                               (double)y + 0.5);
            mapDestPoint(dst_pt, src_pt);

            // Get the mapped source coordinates
            s_x = (float)src_pt.getX();
            s_y = (float)src_pt.getY();

            // Floor value to get the integral coordinate
            int s_ix = (int) Math.floor(s_x);
            int s_iy = (int) Math.floor(s_y);

            fracx = s_x - (double)s_ix;
            fracy = s_y - (double)s_iy;

            int ifracx = (int) Math.floor(fracx * geom_frac_max);
            int ifracy = (int) Math.floor(fracy * geom_frac_max);

            // Compute clipMinX, clipMinY
            Range clipRange = 
		performScanlineClipping(src_rect_x1, src_rect_y1,
					// Last point in the source is
					// x2 = x1 + width - 1
					// y2 = y1 + height - 1
					src_rect_x2, src_rect_y2,
					s_ix, s_iy,
					ifracx, ifracy,
					dst_min_x, dst_max_x,
					0, 0, 0, 0);
            int clipMinX = ((Integer)clipRange.getMinValue()).intValue();
            int clipMaxX = ((Integer)clipRange.getMaxValue()).intValue();

            // Advance s_ix, s_iy, ifracx, ifracy
            Point[] startPts = advanceToStartOfScanline(dst_min_x, clipMinX,
                                                        s_ix, s_iy,
                                                        ifracx, ifracy);
            s_ix = startPts[0].x;
            s_iy = startPts[0].y;
            ifracx = startPts[1].x;
            ifracy = startPts[1].y;

            // Translate to/from SampleModel space & Raster space
            src_pos = (s_iy - srcRectY)*srcScanlineStride +
                (s_ix - srcRectX)*srcPixelStride;

            if (setBackground) {
                for (int x = dst_min_x; x < clipMinX; x++) {
                    for (int k2=0; k2 < dst_num_bands; k2++)
                        dstDataArrays[k2]
                            [dstPixelOffset+dstBandOffsets[k2]] =
                            backgroundShort[k2];
                    dstPixelOffset += dstPixelStride;
                }
            } else             // Advance to first pixel
                dstPixelOffset += (clipMinX - dst_min_x)*dstPixelStride;

            for (int x = clipMinX; x < clipMaxX; x++) {
                for (int k2=0; k2 < dst_num_bands; k2++) {
                    dstDataArrays[k2]
                        [dstPixelOffset+dstBandOffsets[k2]] =
                        srcDataArrays[k2][src_pos + bandOffsets[k2]];
                }

                // walk
                if (ifracx < ifracdx1) {
		    /* 
		    // DEBUG
                    s_ix += incx;
		    */
                    src_pos += incxStride;
                    ifracx += ifracdx;
                } else {
		    /* 
		    // DEBUG
                    s_ix += incx1;
		    */
                    src_pos += incx1Stride;
                    ifracx -= ifracdx1;
                }

                if (ifracy < ifracdy1) {
		    /* 
		    // DEBUG
                    s_iy += incy;
		    */
                    src_pos += incyStride;
                    ifracy += ifracdy;
                } else {
		    /* 
		    // DEBUG
                    s_iy += incy1;
		    */
                    src_pos += incy1Stride;
                    ifracy -= ifracdy1;
                }

                dstPixelOffset += dstPixelStride;
            }

            if (setBackground && clipMinX <= clipMaxX) {
                for (int x = clipMaxX; x < dst_max_x ; x++) {
                    for (int k2=0; k2 < dst_num_bands; k2++)
                        dstDataArrays[k2]
                            [dstPixelOffset+dstBandOffsets[k2]] =
                            backgroundShort[k2];
                    dstPixelOffset += dstPixelStride;
                }
            }

            dstOffset += dstScanlineStride;
        }
    }

    private void floatLoop(RasterAccessor src,
                           Rectangle destRect,
                           int srcRectX,
                           int srcRectY,
                           RasterAccessor dst) {

        float src_rect_x1 = src.getX();
        float src_rect_y1 = src.getY();
        float src_rect_x2 = src_rect_x1 + src.getWidth();
        float src_rect_y2 = src_rect_y1 + src.getHeight();

        float s_x, s_y;

        int src_x, src_y, src_pos;

        double fracx, fracy;

        int dstPixelOffset;
        int dstOffset = 0;

        Point2D dst_pt = new Point2D.Float();
        Point2D src_pt = new Point2D.Float();

        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();

        float dstDataArrays[][] = dst.getFloatDataArrays();
        int dstBandOffsets[] = dst.getBandOffsets();
        int dstPixelStride = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();

        float srcDataArrays[][] = src.getFloatDataArrays();
        int bandOffsets[] = src.getBandOffsets();
        int srcPixelStride = src.getPixelStride();
        int srcScanlineStride = src.getScanlineStride();

        int dst_num_bands = dst.getNumBands();

        int dst_min_x = destRect.x;
        int dst_min_y = destRect.y;
        int dst_max_x = destRect.x + destRect.width;
        int dst_max_y = destRect.y + destRect.height;

        int incxStride = incx*srcPixelStride;
        int incx1Stride = incx1*srcPixelStride;
        int incyStride = incy*srcScanlineStride;
        int incy1Stride = incy1*srcScanlineStride;

	float[] backgroundFloat = new float[dst_num_bands];
	for (int i = 0; i < dst_num_bands; i++)
	    backgroundFloat[i] = (float)backgroundValues[i];

        for (int y = dst_min_y; y < dst_max_y; y++)  {
            dstPixelOffset = dstOffset;

            // Backward map the first point in the line
            // The energy is at the (pt_x + 0.5, pt_y + 0.5)
            dst_pt.setLocation((double)dst_min_x + 0.5,
                               (double)y + 0.5);
            mapDestPoint(dst_pt, src_pt);

            // Get the mapped source coordinates
            s_x = (float)src_pt.getX();
            s_y = (float)src_pt.getY();

            // Floor value to get the integral coordinate
            int s_ix = (int) Math.floor(s_x);
            int s_iy = (int) Math.floor(s_y);

            fracx = s_x - (double)s_ix;
            fracy = s_y - (double)s_iy;

            int ifracx = (int) Math.floor(fracx * geom_frac_max);
            int ifracy = (int) Math.floor(fracy * geom_frac_max);

            // Compute clipMinX, clipMinY
            Range clipRange = 
		performScanlineClipping(src_rect_x1, src_rect_y1,
					// Last point in the source is
					// x2 = x1 + width - 1
					// y2 = y1 + height - 1
					src_rect_x2, src_rect_y2,
					s_ix, s_iy,
					ifracx, ifracy,
					dst_min_x, dst_max_x,
					0, 0, 0, 0);
            int clipMinX = ((Integer)clipRange.getMinValue()).intValue();
            int clipMaxX = ((Integer)clipRange.getMaxValue()).intValue();

            // Advance s_ix, s_iy, ifracx, ifracy
            Point[] startPts = advanceToStartOfScanline(dst_min_x, clipMinX,
                                                        s_ix, s_iy,
                                                        ifracx, ifracy);
            s_ix = startPts[0].x;
            s_iy = startPts[0].y;
            ifracx = startPts[1].x;
            ifracy = startPts[1].y;

            // Translate to/from SampleModel space & Raster space
            src_pos = (s_iy - srcRectY)*srcScanlineStride +
                (s_ix - srcRectX)*srcPixelStride;

            if (setBackground) {
                for (int x = dst_min_x; x < clipMinX; x++) {
                    for (int k2=0; k2 < dst_num_bands; k2++)
                        dstDataArrays[k2]
                            [dstPixelOffset+dstBandOffsets[k2]] =
                            backgroundFloat[k2];
                    dstPixelOffset += dstPixelStride;
                }
            } else             // Advance to first pixel
                dstPixelOffset += (clipMinX - dst_min_x)*dstPixelStride;

            for (int x = clipMinX; x < clipMaxX; x++) {
                for (int k2=0; k2 < dst_num_bands; k2++) {
                    dstDataArrays[k2]
                        [dstPixelOffset+dstBandOffsets[k2]] =
                        srcDataArrays[k2][src_pos + bandOffsets[k2]];
                }

                // walk
                if (ifracx < ifracdx1) {
		    /* 
		    // DEBUG
                    s_ix += incx;
		    */
                    src_pos += incxStride;
                    ifracx += ifracdx;
                } else {
		    /* 
		    // DEBUG
                    s_ix += incx1;
		    */
                    src_pos += incx1Stride;
                    ifracx -= ifracdx1;
                }

                if (ifracy < ifracdy1) {
		    /* 
		    // DEBUG
                    s_iy += incy;
		    */
                    src_pos += incyStride;
                    ifracy += ifracdy;
                } else {
		    /* 
		    // DEBUG
                    s_iy += incy1;
		    */
                    src_pos += incy1Stride;
                    ifracy -= ifracdy1;
                }

                dstPixelOffset += dstPixelStride;
            }

            if (setBackground && clipMinX <= clipMaxX) {
                for (int x = clipMaxX; x < dst_max_x ; x++) {
                    for (int k2=0; k2 < dst_num_bands; k2++)
                        dstDataArrays[k2]
                            [dstPixelOffset+dstBandOffsets[k2]] =
                            backgroundFloat[k2];
                    dstPixelOffset += dstPixelStride;
                }
            }

            dstOffset += dstScanlineStride;
        }
    }

    private void doubleLoop(RasterAccessor src,
                            Rectangle destRect,
                            int srcRectX,
                            int srcRectY,
                            RasterAccessor dst) {

        float src_rect_x1 = src.getX();
        float src_rect_y1 = src.getY();
        float src_rect_x2 = src_rect_x1 + src.getWidth();
        float src_rect_y2 = src_rect_y1 + src.getHeight();

        float s_x, s_y;

        int src_x, src_y, src_pos;

        double fracx, fracy;

        int dstPixelOffset;
        int dstOffset = 0;

        Point2D dst_pt = new Point2D.Float();
        Point2D src_pt = new Point2D.Float();

        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();

        double dstDataArrays[][] = dst.getDoubleDataArrays();
        int dstBandOffsets[] = dst.getBandOffsets();
        int dstPixelStride = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();

        double srcDataArrays[][] = src.getDoubleDataArrays();
        int bandOffsets[] = src.getBandOffsets();
        int srcPixelStride = src.getPixelStride();
        int srcScanlineStride = src.getScanlineStride();

        int dst_num_bands = dst.getNumBands();

        int dst_min_x = destRect.x;
        int dst_min_y = destRect.y;
        int dst_max_x = destRect.x + destRect.width;
        int dst_max_y = destRect.y + destRect.height;

        int incxStride = incx*srcPixelStride;
        int incx1Stride = incx1*srcPixelStride;
        int incyStride = incy*srcScanlineStride;
        int incy1Stride = incy1*srcScanlineStride;

        for (int y = dst_min_y; y < dst_max_y; y++)  {
            dstPixelOffset = dstOffset;

            // Backward map the first point in the line
            // The energy is at the (pt_x + 0.5, pt_y + 0.5)
            dst_pt.setLocation((double)dst_min_x + 0.5,
                               (double)y + 0.5);
            mapDestPoint(dst_pt, src_pt);

            // Get the mapped source coordinates
            s_x = (float)src_pt.getX();
            s_y = (float)src_pt.getY();

            // Floor value to get the integral coordinate
            int s_ix = (int) Math.floor(s_x);
            int s_iy = (int) Math.floor(s_y);

            fracx = s_x - (double)s_ix;
            fracy = s_y - (double)s_iy;

            int ifracx = (int) Math.floor(fracx * geom_frac_max);
            int ifracy = (int) Math.floor(fracy * geom_frac_max);

            // Compute clipMinX, clipMinY
            Range clipRange = 
		performScanlineClipping(src_rect_x1, src_rect_y1,
					src_rect_x2, src_rect_y2,
					s_ix, s_iy,
					ifracx, ifracy,
					dst_min_x, dst_max_x,
					0, 0, 0, 0);
            int clipMinX = ((Integer)clipRange.getMinValue()).intValue();
            int clipMaxX = ((Integer)clipRange.getMaxValue()).intValue();

            // Advance s_ix, s_iy, ifracx, ifracy
            Point[] startPts = advanceToStartOfScanline(dst_min_x, clipMinX,
                                                        s_ix, s_iy,
                                                        ifracx, ifracy);
            s_ix = startPts[0].x;
            s_iy = startPts[0].y;
            ifracx = startPts[1].x;
            ifracy = startPts[1].y;

            // Translate to/from SampleModel space & Raster space
            src_pos = (s_iy - srcRectY)*srcScanlineStride +
                (s_ix - srcRectX)*srcPixelStride;

            if (setBackground) {
                for (int x = dst_min_x; x < clipMinX; x++) {
                    for (int k2=0; k2 < dst_num_bands; k2++)
                        dstDataArrays[k2]
                            [dstPixelOffset+dstBandOffsets[k2]] =
                            backgroundValues[k2];
                    dstPixelOffset += dstPixelStride;
                }
            } else             // Advance to first pixel
                dstPixelOffset += (clipMinX - dst_min_x)*dstPixelStride;

            for (int x = clipMinX; x < clipMaxX; x++) {
                for (int k2=0; k2 < dst_num_bands; k2++) {
                    dstDataArrays[k2]
                        [dstPixelOffset+dstBandOffsets[k2]] =
                        srcDataArrays[k2][src_pos + bandOffsets[k2]];
                }

                // walk
                if (ifracx < ifracdx1) {
		    /* 
		    // DEBUG
                    s_ix += incx;
		    */
                    src_pos += incxStride;
                    ifracx += ifracdx;
                } else {
		    /* 
		    // DEBUG
                    s_ix += incx1;
		    */
                    src_pos += incx1Stride;
                    ifracx -= ifracdx1;
                }

                if (ifracy < ifracdy1) {
		    /* 
		    // DEBUG
                    s_iy += incy;
		    */
                    src_pos += incyStride;
                    ifracy += ifracdy;
                } else {
		    /* 
		    // DEBUG
                    s_iy += incy1;
		    */
                    src_pos += incy1Stride;
                    ifracy -= ifracdy1;
                }

                dstPixelOffset += dstPixelStride;
            }

            if (setBackground && clipMinX <= clipMaxX) {
                for (int x = clipMaxX; x < dst_max_x ; x++) {
                    for (int k2=0; k2 < dst_num_bands; k2++)
                        dstDataArrays[k2]
                            [dstPixelOffset+dstBandOffsets[k2]] =
                            backgroundValues[k2];
                    dstPixelOffset += dstPixelStride;
                }
            }

            dstOffset += dstScanlineStride;
        }
    }
}
