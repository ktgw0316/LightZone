/*
 * $RCSfile: AffineGeneralOpImage.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:13 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.opimage;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.AffineTransform;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import com.lightcrafts.mediax.jai.BorderExtender;
import com.lightcrafts.mediax.jai.ImageLayout;
import com.lightcrafts.mediax.jai.Interpolation;
import com.lightcrafts.mediax.jai.RasterAccessor;
import com.lightcrafts.mediax.jai.RasterFormatTag;
import java.util.Map;

/**
 * An OpImage subclass that performs general Affine mapping
 */
final class AffineGeneralOpImage extends AffineOpImage {

    /* The number of subsampleBits */
    private int subsampleBits;
    private int shiftvalue;

    private int interp_width, interp_height ;
    private int interp_left, interp_top, interp_right, interp_bottom;

    /**
     * Constructs an AffineGeneralOpImage from a RenderedImage source,
     *
     * @param source a RenderedImage.
     * @param extender a BorderExtender, or null.

     * @param layout an ImageLayout optionally containing the tile grid layout,
     *        SampleModel, and ColorModel, or null.
     * @param interp an Interpolation object to use for resampling
     * @param transform the desired AffineTransform.
     */
    public AffineGeneralOpImage(RenderedImage source,
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

        subsampleBits = interp.getSubsampleBitsH();
        shiftvalue = 1 << subsampleBits;

        interp_width = interp.getWidth();
        interp_height = interp.getHeight();
        interp_left = interp.getLeftPadding();
        interp_top = interp.getTopPadding();
        interp_right = interp_width - interp_left - 1;
        interp_bottom = interp_height - interp_top - 1;
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
        // In the first version source Rectangle is the whole source
        // image always.
        //
        // See if we can cache the source to avoid multiple rasteraccesors
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
            byteLoop(srcAccessor,
                     destRect,
                     srcRectX,
                     srcRectY,
                     dstAccessor);
            break;

        case DataBuffer.TYPE_INT:
            intLoop(srcAccessor,
                    destRect,
                    srcRectX,
                    srcRectY,
                    dstAccessor);
            break;

        case DataBuffer.TYPE_SHORT:
            shortLoop(srcAccessor,
                      destRect,
                      srcRectX,
                      srcRectY,
                      dstAccessor);
            break;

        case DataBuffer.TYPE_USHORT:
            ushortLoop(srcAccessor,
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

        // If the RasterAccessor object set up a temporary buffer for the
        // op to write to, tell the RasterAccessor to write that data
        // to the raster, that we're done with it.
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

        float fracx, fracy;

        int s_ix, s_iy;
        int p_x, p_y;

        int s, q;
        int result;

        int samples[][] = new int[interp_height][interp_width];
        int xfrac, yfrac;

        int dstPixelOffset;
        int dstOffset = 0;

        Point2D dst_pt = new Point2D.Float();
        Point2D src_pt = new Point2D.Float();

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

	byte[] backgroundByte = new byte[dst_num_bands];
	for (int i = 0; i < dst_num_bands; i++)
	    backgroundByte[i] = (byte)backgroundValues[i];

        for (int y = dst_min_y; y < dst_max_y ; y++) {
            dstPixelOffset = dstOffset;

            // Backward map the first point in the line
            // The energy is at the (pt_x + 0.5, pt_y + 0.5)
            dst_pt.setLocation((double)dst_min_x + 0.5,
                               (double)y + 0.5);
            mapDestPoint(dst_pt, src_pt);

            // Get the mapped source coordinates
            s_x = (float) src_pt.getX();
            s_y = (float) src_pt.getY();
            // As per definition of bicubic interpolation
            s_x -= 0.5;
            s_y -= 0.5;

            // Floor to get the integral coordinate
            s_ix = (int) Math.floor(s_x);
            s_iy = (int) Math.floor(s_y);

            fracx = s_x - (float) s_ix;
            fracy = s_y - (float) s_iy;

            // Translate to/from SampleModel space & Raster space
            p_x = (s_ix - srcRectX) * srcPixelStride;
            p_y = (s_iy - srcRectY) * srcScanlineStride;

            for (int x = dst_min_x; x < dst_max_x; x++) {
                //
                // Check against the source rectangle
                //

                if ((s_ix >= src_rect_x1 + interp_left) &&
                    (s_ix < (src_rect_x2 - interp_right)) &&
                    (s_iy >= (src_rect_y1 + interp_top)) &&
                    (s_iy < (src_rect_y2 - interp_bottom))) {
                    for (int k=0; k < dst_num_bands; k++) {
                        byte srcData[] = srcDataArrays[k];
                        int tmp = bandOffsets[k];

                        // Get the pixels required for this interpolation
                        int start = interp_left * srcPixelStride +
                            interp_top * srcScanlineStride;
                        start = p_x + p_y - start;
                        int countH = 0, countV = 0;

                        for (int i = 0; i < interp_height; i++) {
                            int startY = start;
                            for (int j = 0; j < interp_width; j++) {
                                samples[countV][countH++] =
                                    srcData[start + tmp] & 0xff;
                                start += srcPixelStride;
                            }
                            countV++;
                            countH = 0;
                            start = startY + srcScanlineStride;
                        }

                        // Get the new frac values
                        xfrac = (int) (fracx * shiftvalue);
                        yfrac = (int) (fracy * shiftvalue);

                        // Do the interpolation
                        s = interp.interpolate(samples, xfrac, yfrac);

                        // Clamp
                        if (s < 0) {
                           result = 0;
                        } else if (s > 255) {
                            result = 255;
                        } else {
                            result = s;
                        }

                        // write the result
                        dstDataArrays[k]
                            [dstPixelOffset+dstBandOffsets[k]] =
                            (byte) (result & 0xff);
                    }
                } else if (setBackground) {
		    for (int k=0; k < dst_num_bands; k++)
			dstDataArrays[k][dstPixelOffset+dstBandOffsets[k]] =
			    backgroundByte[k];
		}

                // walk
                if (fracx < fracdx1) {
                    s_ix += incx;
                    fracx += fracdx;
                } else {
                    s_ix += incx1;
                    fracx -= fracdx1;
                }

                if (fracy < fracdy1) {
                    s_iy += incy;
                    fracy += fracdy;
                } else {
                    s_iy += incy1;
                    fracy -= fracdy1;
                }

                // Translate to/from SampleModel space & Raster space
                p_x = (s_ix - srcRectX) * srcPixelStride;
                p_y = (s_iy - srcRectY) * srcScanlineStride;

                dstPixelOffset += dstPixelStride;
            }

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

        float fracx, fracy;

        int s_ix, s_iy;
        int p_x, p_y;

        int s, q;
        int result;

        int dstPixelOffset;
        int dstOffset = 0;

        int samples[][] = new int[interp_height][interp_width];
        int xfrac, yfrac;

        Point2D dst_pt = new Point2D.Float();
        Point2D src_pt = new Point2D.Float();

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

	int[] backgroundInt = new int[dst_num_bands];
	for (int i = 0; i < dst_num_bands; i++)
	    backgroundInt[i] = (int)backgroundValues[i];

        for (int y = dst_min_y; y < dst_max_y ; y++) {
            dstPixelOffset = dstOffset;

            // Backward map the first point in the line
            // The energy is at the (pt_x + 0.5, pt_y + 0.5)
            dst_pt.setLocation((double)dst_min_x + 0.5,
                               (double)y + 0.5);
            mapDestPoint(dst_pt, src_pt);

            // Get the mapped source coordinates
            s_x = (float) src_pt.getX();
            s_y = (float) src_pt.getY();

            // As per definition of bicubic interpolation
            s_x -= 0.5;
            s_y -= 0.5;

            // Floor to get the integral coordinate
            s_ix = (int) Math.floor(s_x);
            s_iy = (int) Math.floor(s_y);

            fracx = s_x - (float) s_ix;
            fracy = s_y - (float) s_iy;

            // Translate to/from SampleModel space & Raster space
            p_x = (s_ix - srcRectX) * srcPixelStride;
            p_y = (s_iy - srcRectY) * srcScanlineStride;

            for (int x = dst_min_x; x < dst_max_x; x++) {
                //
                // Check against the source rectangle
                //
                if ((s_ix >= (src_rect_x1 + interp_left)) &&
                    (s_ix < (src_rect_x2 - interp_right)) &&
                    (s_iy >= (src_rect_y1 + interp_top)) &&
                    (s_iy < (src_rect_y2 - interp_bottom))) {
                    for (int k=0; k < dst_num_bands; k++) {
                        int srcData[] = srcDataArrays[k];
                        int tmp = bandOffsets[k];

                        // Get the pixels required for this interpolation
                        int start = interp_left * srcPixelStride +
                            interp_top * srcScanlineStride;
                        start = p_x + p_y - start;
                        int countH = 0, countV = 0;

                        for (int i = 0; i < interp_height; i++) {
                            int startY = start;
                            for (int j = 0; j < interp_width; j++) {
                                samples[countV][countH++] =
                                    srcData[start + tmp];
                                start += srcPixelStride;
                            }
                            countV++;
                            countH = 0;
                            start = startY + srcScanlineStride;
                        }

                        // Get the new frac values
                        xfrac = (int) (fracx * shiftvalue);
                        yfrac = (int) (fracy * shiftvalue);

                        // Do the interpolation
                        s = interp.interpolate(samples, xfrac, yfrac);

                        // Clamp
                        if (s < Integer.MIN_VALUE) {
                           result = Integer.MIN_VALUE;
                        } else if (s > Integer.MAX_VALUE) {
                            result = Integer.MAX_VALUE;
                        } else {
                            result = s;
                        }

                        // write the result
                        dstDataArrays[k]
                            [dstPixelOffset+dstBandOffsets[k]] = result;
                    }
                } else if (setBackground) {
		    for (int k=0; k < dst_num_bands; k++)
			dstDataArrays[k][dstPixelOffset+dstBandOffsets[k]] =
			    backgroundInt[k];
		}

                // walk
                if (fracx < fracdx1) {
                    s_ix += incx;
                    fracx += fracdx;
                } else {
                    s_ix += incx1;
                    fracx -= fracdx1;
                }

                if (fracy < fracdy1) {
                    s_iy += incy;
                    fracy += fracdy;
                } else {
                    s_iy += incy1;
                    fracy -= fracdy1;
                }

                // Translate to/from SampleModel space & Raster space
                p_x = (s_ix - srcRectX) * srcPixelStride;
                p_y = (s_iy - srcRectY) * srcScanlineStride;

                dstPixelOffset += dstPixelStride;
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

        float fracx, fracy;

        int s_ix, s_iy;
        int p_x, p_y;

        int s, q;

        int samples[][] = new int[interp_height][interp_width];
        int xfrac, yfrac;

        short result;

        int dstPixelOffset;
        int dstOffset = 0;

        Point2D dst_pt = new Point2D.Float();
        Point2D src_pt = new Point2D.Float();

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

	short[] backgroundShort = new short[dst_num_bands];
	for (int i = 0; i < dst_num_bands; i++)
	    backgroundShort[i] = (short)backgroundValues[i];

        for (int y = dst_min_y; y < dst_max_y ; y++) {

            dstPixelOffset = dstOffset;

            // Backward map the first point in the line
            // The energy is at the (pt_x + 0.5, pt_y + 0.5)
            dst_pt.setLocation((double)dst_min_x + 0.5,
                               (double)y + 0.5);
            mapDestPoint(dst_pt, src_pt);

            // Get the mapped source coordinates
            s_x = (float) src_pt.getX();
            s_y = (float) src_pt.getY();

            // As per definition of bicubic interpolation
            s_x -= 0.5;
            s_y -= 0.5;

            // Floor to get the integral coordinate
            s_ix = (int) Math.floor(s_x);
            s_iy = (int) Math.floor(s_y);

            fracx = s_x - (float) s_ix;
            fracy = s_y - (float) s_iy;

            // Translate to/from SampleModel space & Raster space
            p_x = (s_ix - srcRectX) * srcPixelStride;
            p_y = (s_iy - srcRectY) * srcScanlineStride;

            for (int x = dst_min_x; x < dst_max_x; x++) {
                //
                // Check against the source rectangle
                //
                if ((s_ix >= (src_rect_x1 + interp_left)) &&
                    (s_ix < (src_rect_x2 - interp_right)) &&
                    (s_iy >= (src_rect_y1 + interp_top)) &&
                    (s_iy < (src_rect_y2 - interp_bottom))) {
                    for (int k=0; k < dst_num_bands; k++) {
                        short srcData[] = srcDataArrays[k];
                        int tmp = bandOffsets[k];

                        // Get the pixels required for this interpolation
                        int start = interp_left * srcPixelStride +
                            interp_top * srcScanlineStride;
                        start = p_x + p_y - start;
                        int countH = 0, countV = 0;

                        for (int i = 0; i < interp_height; i++) {
                            int startY = start;
                            for (int j = 0; j < interp_width; j++) {
                                samples[countV][countH++] =
                                    srcData[start + tmp];
                                start += srcPixelStride;
                            }
                            countV++;
                            countH = 0;
                            start = startY + srcScanlineStride;
                        }

                        // Get the new frac values
                        xfrac = (int) (fracx * shiftvalue);
                        yfrac = (int) (fracy * shiftvalue);

                        // Do the interpolation
                        s = interp.interpolate(samples, xfrac, yfrac);

                        // Round the result
                        if (s < Short.MIN_VALUE) {
                           result = Short.MIN_VALUE;
                        } else if (s > Short.MAX_VALUE) {
                            result = Short.MAX_VALUE;
                        } else {
                            result = (short) s;
                        }

                        // write the result
                        dstDataArrays[k]
                            [dstPixelOffset+dstBandOffsets[k]] = result;
                    }
                } else if (setBackground) {
		    for (int k=0; k < dst_num_bands; k++)
			dstDataArrays[k][dstPixelOffset+dstBandOffsets[k]] =
			    backgroundShort[k];
		}

                // walk
                if (fracx < fracdx1) {
                    s_ix += incx;
                    fracx += fracdx;
                } else {
                    s_ix += incx1;
                    fracx -= fracdx1;
                }

                if (fracy < fracdy1) {
                    s_iy += incy;
                    fracy += fracdy;
                } else {
                    s_iy += incy1;
                    fracy -= fracdy1;
                }

                // Translate to/from SampleModel space & Raster space
                p_x = (s_ix - srcRectX) * srcPixelStride;
                p_y = (s_iy - srcRectY) * srcScanlineStride;

                dstPixelOffset += dstPixelStride;
            }

            dstOffset += dstScanlineStride;
        }
    }

    private void ushortLoop(RasterAccessor src,
                            Rectangle destRect,
                            int srcRectX,
                            int srcRectY,
                            RasterAccessor dst) {

        float src_rect_x1 = src.getX();
        float src_rect_y1 = src.getY();
        float src_rect_x2 = src_rect_x1 + src.getWidth();
        float src_rect_y2 = src_rect_y1 + src.getHeight();

        float s_x, s_y;

        float fracx, fracy;

        int s_ix, s_iy;
        int p_x, p_y;

        int s, q;

        int samples[][] = new int[interp_height][interp_width];
        int xfrac, yfrac;

        int result;

        int dstPixelOffset;
        int dstOffset = 0;

        Point2D dst_pt = new Point2D.Float();
        Point2D src_pt = new Point2D.Float();

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

	short[] backgroundUShort = new short[dst_num_bands];
	for (int i = 0; i < dst_num_bands; i++)
	    backgroundUShort[i] = (short)backgroundValues[i];

        for (int y = dst_min_y; y < dst_max_y ; y++) {
            dstPixelOffset = dstOffset;

            // Backward map the first point in the line
            // The energy is at the (pt_x + 0.5, pt_y + 0.5)
            dst_pt.setLocation((double)dst_min_x + 0.5,
                               (double)y + 0.5);
            mapDestPoint(dst_pt, src_pt);

            // Get the mapped source coordinates
            s_x = (float) src_pt.getX();
            s_y = (float) src_pt.getY();

            // As per definition of bicubic interpolation
            s_x -= 0.5;
            s_y -= 0.5;

            // Floor to get the integral coordinate
            s_ix = (int) Math.floor(s_x);
            s_iy = (int) Math.floor(s_y);

            fracx = s_x - (float) s_ix;
            fracy = s_y - (float) s_iy;

            // Translate to/from SampleModel space & Raster space
            p_x = (s_ix - srcRectX) * srcPixelStride;
            p_y = (s_iy - srcRectY) * srcScanlineStride;

            for (int x = dst_min_x; x < dst_max_x; x++) {
                //
                // Check against the source rectangle
                //
                if ((s_ix >= (src_rect_x1 + interp_left)) &&
                    (s_ix < (src_rect_x2 - interp_right)) &&
                    (s_iy >= (src_rect_y1 + interp_top)) &&
                    (s_iy < (src_rect_y2 - interp_bottom))) {
                    for (int k=0; k < dst_num_bands; k++) {
                        short srcData[] = srcDataArrays[k];
                        int tmp = bandOffsets[k];

                        // Get the pixels required for this interpolation
                        int start = interp_left * srcPixelStride +
                            interp_top * srcScanlineStride;
                        start = p_x + p_y - start;
                        int countH = 0, countV = 0;

                        for (int i = 0; i < interp_height; i++) {
                            int startY = start;
                            for (int j = 0; j < interp_width; j++) {
                                samples[countV][countH++] =
                                    srcData[start + tmp] & 0xffff;
                                start += srcPixelStride;
                            }
                            countV++;
                            countH = 0;
                            start = startY + srcScanlineStride;
                        }

                        // Get the new frac values
                        xfrac = (int) (fracx * shiftvalue);
                        yfrac = (int) (fracy * shiftvalue);

                        // Do the interpolation
                        s = interp.interpolate(samples, xfrac, yfrac);

                        // Round
                        if (s < 0) {
                           result = 0;
                        } else if (s > USHORT_MAX) {
                            result = USHORT_MAX;
                        } else {
                            result =  s;
                        }

                        // write the result
                        dstDataArrays[k]
                            [dstPixelOffset+dstBandOffsets[k]] =
                            (short)(result & 0xFFFF);
                    }
                } else if (setBackground) {
		    for (int k=0; k < dst_num_bands; k++)
			dstDataArrays[k][dstPixelOffset+dstBandOffsets[k]] =
			    backgroundUShort[k];
		}

                // walk
                if (fracx < fracdx1) {
                    s_ix += incx;
                    fracx += fracdx;
                } else {
                    s_ix += incx1;
                    fracx -= fracdx1;
                }

                if (fracy < fracdy1) {
                    s_iy += incy;
                    fracy += fracdy;
                } else {
                    s_iy += incy1;
                    fracy -= fracdy1;
                }

                // Translate to/from SampleModel space & Raster space
                p_x = (s_ix - srcRectX) * srcPixelStride;
                p_y = (s_iy - srcRectY) * srcScanlineStride;

                dstPixelOffset += dstPixelStride;
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

        float fracx, fracy;

        int s_ix, s_iy;
        int p_x, p_y;

        float s, q;

        float samples[][] = new float[interp_height][interp_width];
        long xfrac, yfrac;

        int dstPixelOffset;
        int dstOffset = 0;

        Point2D dst_pt = new Point2D.Float();
        Point2D src_pt = new Point2D.Float();

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

	float[] backgroundFloat = new float[dst_num_bands];
	for (int i = 0; i < dst_num_bands; i++)
	    backgroundFloat[i] = (float)backgroundValues[i];

        for (int y = dst_min_y; y < dst_max_y ; y++) {

            dstPixelOffset = dstOffset;

            // Backward map the first point in the line
            // The energy is at the (pt_x + 0.5, pt_y + 0.5)
            dst_pt.setLocation((double)dst_min_x + 0.5,
                               (double)y + 0.5);
            mapDestPoint(dst_pt, src_pt);

            // Get the mapped source coordinates
            s_x = (float) src_pt.getX();
            s_y = (float) src_pt.getY();

            // As per definition of bicubic interpolation
            s_x -= 0.5;
            s_y -= 0.5;

            // Floor to get the integral coordinate
            s_ix = (int) Math.floor(s_x);
            s_iy = (int) Math.floor(s_y);

            fracx = s_x - (float) s_ix;
            fracy = s_y - (float) s_iy;

            // Translate to/from SampleModel space & Raster space
            p_x = (s_ix - srcRectX) * srcPixelStride;
            p_y = (s_iy - srcRectY) * srcScanlineStride;

            for (int x = dst_min_x; x < dst_max_x; x++) {
                //
                // Check against the source rectangle
                //
                if ((s_ix >= (src_rect_x1 + interp_left)) &&
                    (s_ix < (src_rect_x2 - interp_right)) &&
                    (s_iy >= (src_rect_y1 + interp_top)) &&
                    (s_iy < (src_rect_y2 - interp_bottom))) {
                    for (int k=0; k < dst_num_bands; k++) {
                        float srcData[] = srcDataArrays[k];
                        int tmp = bandOffsets[k];

                        // Get the pixels required for this interpolation
                        int start = interp_left * srcPixelStride +
                            interp_top * srcScanlineStride;
                        start = p_x + p_y - start;
                        int countH = 0, countV = 0;

                        for (int i = 0; i < interp_height; i++) {
                            int startY = start;
                            for (int j = 0; j < interp_width; j++) {
                                samples[countV][countH++] =
                                    srcData[start + tmp];
                                start += srcPixelStride;
                            }
                            countV++;
                            countH = 0;
                            start = startY + srcScanlineStride;
                        }

                        // Do the interpolation
                        s = interp.interpolate(samples, fracx, fracy);

                        // write the result
                        dstDataArrays[k]
                            [dstPixelOffset+dstBandOffsets[k]] = s;
                    }
                } else if (setBackground) {
		    for (int k=0; k < dst_num_bands; k++)
			dstDataArrays[k][dstPixelOffset+dstBandOffsets[k]] =
			    backgroundFloat[k];
		}

                // walk
                if (fracx < fracdx1) {
                    s_ix += incx;
                    fracx += fracdx;
                } else {
                    s_ix += incx1;
                    fracx -= fracdx1;
                }

                if (fracy < fracdy1) {
                    s_iy += incy;
                    fracy += fracdy;
                } else {
                    s_iy += incy1;
                    fracy -= fracdy1;
                }

                // Translate to/from SampleModel space & Raster space
                p_x = (s_ix - srcRectX) * srcPixelStride;
                p_y = (s_iy - srcRectY) * srcScanlineStride;

                dstPixelOffset += dstPixelStride;
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

        double s_x, s_y;

        double fracx, fracy;

        int s_ix, s_iy;
        int p_x, p_y;

        double s, q;

        double samples[][] = new double[interp_height][interp_width];
        float xfrac, yfrac;

        int dstPixelOffset;
        int dstOffset = 0;

        Point2D dst_pt = new Point2D.Float();
        Point2D src_pt = new Point2D.Float();

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

        for (int y = dst_min_y; y < dst_max_y ; y++) {

            dstPixelOffset = dstOffset;


            // Backward map the first point in the line
            // The energy is at the (pt_x + 0.5, pt_y + 0.5)
            dst_pt.setLocation((double)dst_min_x + 0.5,
                               (double)y + 0.5);
            mapDestPoint(dst_pt, src_pt);

            // Get the mapped source coordinates
            s_x = (double) src_pt.getX();
            s_y = (double) src_pt.getY();

            // As per definition of bicubic interpolation
            s_x -= 0.5;
            s_y -= 0.5;

            // Floor to get the integral coordinate
            s_ix = (int) Math.floor(s_x);
            s_iy = (int) Math.floor(s_y);

            fracx = s_x - (double) s_ix;
            fracy = s_y - (double) s_iy;

            // Translate to/from SampleModel space & Raster space
            p_x = (s_ix - srcRectX) * srcPixelStride;
            p_y = (s_iy - srcRectY) * srcScanlineStride;

            for (int x = dst_min_x; x < dst_max_x; x++) {
                //
                // Check against the source rectangle
                //
                if ((s_ix >= (src_rect_x1 + interp_left)) &&
                    (s_ix < (src_rect_x2 - interp_right)) &&
                    (s_iy >= (src_rect_y1 + interp_top)) &&
                    (s_iy < (src_rect_y2 - interp_bottom))) {
                    for (int k=0; k < dst_num_bands; k++) {
                        double srcData[] = srcDataArrays[k];
                        int tmp = bandOffsets[k];

                        // Get the pixels required for this interpolation
                        int start = interp_left * srcPixelStride +
                            interp_top * srcScanlineStride;
                        start = p_x + p_y - start;
                        int countH = 0, countV = 0;

                        for (int i = 0; i < interp_height; i++) {
                            int startY = start;
                            for (int j = 0; j < interp_width; j++) {
                                samples[countV][countH++] =
                                    srcData[start + tmp];
                                start += srcPixelStride;
                            }
                            countV++;
                            countH = 0;
                            start = startY + srcScanlineStride;
                        }

                        // Do the interpolation
                        s = interp.interpolate(samples,
                                               (float)fracx,
                                               (float)fracy);

                        // write the result
                        dstDataArrays[k]
                            [dstPixelOffset+dstBandOffsets[k]] = s;
                    }
                } else if (setBackground) {
		    for (int k=0; k < dst_num_bands; k++)
			dstDataArrays[k][dstPixelOffset+dstBandOffsets[k]] =
			    backgroundValues[k];
		}
                // walk
                if (fracx < fracdx1) {
                    s_ix += incx;
                    fracx += fracdx;
                } else {
                    s_ix += incx1;
                    fracx -= fracdx1;
                }

                if (fracy < fracdy1) {
                    s_iy += incy;
                    fracy += fracdy;
                } else {
                    s_iy += incy1;
                    fracy -= fracdy1;
                }

                // Translate to/from SampleModel space & Raster space
                p_x = (s_ix - srcRectX) * srcPixelStride;
                p_y = (s_iy - srcRectY) * srcScanlineStride;

                dstPixelOffset += dstPixelStride;
            }

            dstOffset += dstScanlineStride;
        }
    }
}
