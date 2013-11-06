/*
 * $RCSfile: AffineBicubicOpImage.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:12 $
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
// import com.lightcrafts.media.jai.test.OpImageTester;

/**
 * An OpImage subclass that performs bicubic Affine mapping
 */
final class AffineBicubicOpImage extends AffineOpImage {

    /**
     * Constructs an AffineBicubicOpImage from a RenderedImage source,
     *
     * @param source a RenderedImage.
     * @param extender a BorderExtender, or null.
     * @param layout an ImageLayout optionally containing the tile grid layout,
     *        SampleModel, and ColorModel, or null.
     * @param interp an Interpolation object to use for resampling
     * @param transform the desired AffineTransform.
     */
    public AffineBicubicOpImage(RenderedImage source,
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
        float float_fracx, float_fracy;
        float frac_xx, frac_yy;

        int s_ix, s_iy;
        int p_x, p_y;

        int p__, p0_, p1_, p2_;
        int p_0, p00, p01, p02;
        int p_1, p10, p11, p12;
        int p_2, p20, p21, p22;

        int s__, s0_, s1_, s2_;
        int s_0, s00, s01, s02;
        int s_1, s10, s11, s12;
        int s_2, s20, s21, s22;

        float s0, s1, s_, s2;
        float q_, q0, q1, q2;
        float s, q;
        int result;

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

            //
            // Get the 16 neighbouring positions of the
            // coordinate in question (p00).
            //
            //       p__  p0_  p1_  p2_
            //       p_0  p00  p10  p20
            //       p_1  p01  p11  p21
            //       p_2  p02  p12  p22
            //
            p__ = p_x + p_y - srcScanlineStride - srcPixelStride;
            p0_ = p__ + srcPixelStride;
            p1_ = p0_ + srcPixelStride;
            p2_ = p1_ + srcPixelStride;
            p_0 = p__ + srcScanlineStride;
            p00 = p_0 + srcPixelStride;
            p10 = p00 + srcPixelStride;
            p20 = p10 + srcPixelStride;
            p_1 = p_0 + srcScanlineStride;
            p01 = p_1 + srcPixelStride;
            p11 = p01 + srcPixelStride;
            p21 = p11 + srcPixelStride;
            p_2 = p_1 + srcScanlineStride;
            p02 = p_2 + srcPixelStride;
            p12 = p02 + srcPixelStride;
            p22 = p12 + srcPixelStride;

            for (int x = dst_min_x; x < dst_max_x; x++) {
                //
                // Check against the source rectangle
                //

                if ((s_ix >= src_rect_x1 + 1) &&
                    (s_ix < (src_rect_x2 - 2)) &&
                    (s_iy >= (src_rect_y1 + 1)) &&
                    (s_iy < (src_rect_y2 - 2))) {
                    for (int k2=0; k2 < dst_num_bands; k2++) {
                        //
                        // Get the pixels
                        //
                        byte tmp_row[];
                        int tmp_col;

                        tmp_row = srcDataArrays[k2];
                        tmp_col = bandOffsets[k2];

                        s__ = tmp_row[p__ + tmp_col] & 0xff;
                        s0_ = tmp_row[p0_ + tmp_col] & 0xff;
                        s1_ = tmp_row[p1_ + tmp_col] & 0xff;
                        s2_ = tmp_row[p2_ + tmp_col] & 0xff;
                        s_0 = tmp_row[p_0 + tmp_col] & 0xff;
                        s00 = tmp_row[p00 + tmp_col] & 0xff;
                        s10 = tmp_row[p10 + tmp_col] & 0xff;
                        s20 = tmp_row[p20 + tmp_col] & 0xff;
                        s_1 = tmp_row[p_1 + tmp_col] & 0xff;
                        s01 = tmp_row[p01 + tmp_col] & 0xff;
                        s11 = tmp_row[p11 + tmp_col] & 0xff;
                        s21 = tmp_row[p21 + tmp_col] & 0xff;
                        s_2 = tmp_row[p_2 + tmp_col] & 0xff;
                        s02 = tmp_row[p02 + tmp_col] & 0xff;
                        s12 = tmp_row[p12 + tmp_col] & 0xff;
                        s22 = tmp_row[p22 + tmp_col] & 0xff;

                        // Get the new frac values
                        float_fracx = fracx;
                        float_fracy = fracy;
                        frac_xx = float_fracx * (1.0F - float_fracx);
                        frac_yy = float_fracy * (1.0F - float_fracy);

                        s0 = s00 + ((s10 - s00) * float_fracx);
                        s1 = s01 + ((s11 - s01) * float_fracx);
                        s_ = s0_ + ((s1_ - s0_) * float_fracx);
                        s2 = s02 + ((s12 - s02) * float_fracx);

                        q_ = (s1_ + s__) +
                            (((s2_ + s0_) - (s1_ + s__)) * float_fracx);
                        q0 = (s10 + s_0) +
                            (((s20 + s00) - (s10 + s_0)) * float_fracx);
                        q1 = (s11 + s_1) +
                            ((s21 + s01) - (s11 + s_1)) * float_fracx;
                        q2 = (s12 + s_2) +
                            (((s22 + s02) - (s12 + s_2)) * float_fracx);

                        q_ = s_ - q_ / 2.0F;
                        q0 = s0 - q0 / 2.0F;
                        q1 = s1 - q1 / 2.0F;
                        q2 = s2 - q2 / 2.0F;

                        s_ += (q_ * frac_xx);
                        s0 += (q0 * frac_xx);
                        s1 += (q1 * frac_xx);
                        s2 += (q2 * frac_xx);

                        s = s0 + ((s1 - s0) * float_fracy);
                        q = (s1 + s_) +
                            (((s2 + s0) - (s1 + s_)) * float_fracy);

                        q = s - q / 2.0F;

                        s += (q * frac_yy);

                        // Round
                        if (s < 0.5F) {
                           result = 0;
                        } else if (s > 254.5F) {
                            result = 255;
                        } else {
                            result = (int) (s + 0.5F);
                        }

                        // write the result
                        dstDataArrays[k2]
                            [dstPixelOffset+dstBandOffsets[k2]] =
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

                //
                // Get the 16 neighbouring positions of the
                // coordinate in question (p00).
                //
                //       p__  p0_  p1_  p2_
                //       p_0  p00  p10  p20
                //       p_1  p01  p11  p21
                //       p_2  p02  p12  p22
                //
                p__ = p_x + p_y - srcScanlineStride - srcPixelStride;
                p0_ = p__ + srcPixelStride;
                p1_ = p0_ + srcPixelStride;
                p2_ = p1_ + srcPixelStride;
                p_0 = p__ + srcScanlineStride;
                p00 = p_0 + srcPixelStride;
                p10 = p00 + srcPixelStride;
                p20 = p10 + srcPixelStride;
                p_1 = p_0 + srcScanlineStride;
                p01 = p_1 + srcPixelStride;
                p11 = p01 + srcPixelStride;
                p21 = p11 + srcPixelStride;
                p_2 = p_1 + srcScanlineStride;
                p02 = p_2 + srcPixelStride;
                p12 = p02 + srcPixelStride;
                p22 = p12 + srcPixelStride;

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
        float float_fracx, float_fracy;
        float frac_xx, frac_yy;

        int s_ix, s_iy;
        int p_x, p_y;

        int p__, p0_, p1_, p2_;
        int p_0, p00, p01, p02;
        int p_1, p10, p11, p12;
        int p_2, p20, p21, p22;

        int s__, s0_, s1_, s2_;
        int s_0, s00, s01, s02;
        int s_1, s10, s11, s12;
        int s_2, s20, s21, s22;

        float s0, s1, s_, s2;
        float q_, q0, q1, q2;
        float s, q;
        int result;

        int dstPixelOffset;
        int dstOffset = 0;

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

            //
            // Get the 16 neighbouring positions of the
            // coordinate in question (p00).
            //
            //       p__  p0_  p1_  p2_
            //       p_0  p00  p10  p20
            //       p_1  p01  p11  p21
            //       p_2  p02  p12  p22
            //
            p__ = p_x + p_y - srcScanlineStride - srcPixelStride;
            p0_ = p__ + srcPixelStride;
            p1_ = p0_ + srcPixelStride;
            p2_ = p1_ + srcPixelStride;
            p_0 = p__ + srcScanlineStride;
            p00 = p_0 + srcPixelStride;
            p10 = p00 + srcPixelStride;
            p20 = p10 + srcPixelStride;
            p_1 = p_0 + srcScanlineStride;
            p01 = p_1 + srcPixelStride;
            p11 = p01 + srcPixelStride;
            p21 = p11 + srcPixelStride;
            p_2 = p_1 + srcScanlineStride;
            p02 = p_2 + srcPixelStride;
            p12 = p02 + srcPixelStride;
            p22 = p12 + srcPixelStride;

            for (int x = dst_min_x; x < dst_max_x; x++) {
                //
                // Check against the source rectangle
                //
                if ((s_ix >= (src_rect_x1 + 1)) &&
                    (s_ix < (src_rect_x2 - 2)) &&
                    (s_iy >= (src_rect_y1 + 1)) &&
                    (s_iy < (src_rect_y2 - 2))) {
                    for (int k2=0; k2 < dst_num_bands; k2++) {
                        //
                        // Get the pixels
                        //
                        int tmp_row[];
                        int tmp_col;

                        tmp_row = srcDataArrays[k2];
                        tmp_col = bandOffsets[k2];

                        s__ = tmp_row[p__ + tmp_col];
                        s0_ = tmp_row[p0_ + tmp_col];
                        s1_ = tmp_row[p1_ + tmp_col];
                        s2_ = tmp_row[p2_ + tmp_col];
                        s_0 = tmp_row[p_0 + tmp_col];
                        s00 = tmp_row[p00 + tmp_col];
                        s10 = tmp_row[p10 + tmp_col];
                        s20 = tmp_row[p20 + tmp_col];
                        s_1 = tmp_row[p_1 + tmp_col];
                        s01 = tmp_row[p01 + tmp_col];
                        s11 = tmp_row[p11 + tmp_col];
                        s21 = tmp_row[p21 + tmp_col];
                        s_2 = tmp_row[p_2 + tmp_col];
                        s02 = tmp_row[p02 + tmp_col];
                        s12 = tmp_row[p12 + tmp_col];
                        s22 = tmp_row[p22 + tmp_col];

                        // Get the new frac values
                        float_fracx = fracx;
                        float_fracy = fracy;
                        frac_xx = float_fracx * (1.0F - float_fracx);
                        frac_yy = float_fracy * (1.0F - float_fracy);

                        s0 = s00 + ((s10 - s00) * float_fracx);
                        s1 = s01 + ((s11 - s01) * float_fracx);
                        s_ = s0_ + ((s1_ - s0_) * float_fracx);
                        s2 = s02 + ((s12 - s02) * float_fracx);

                        q_ = (s1_ + s__) +
                            (((s2_ + s0_) - (s1_ + s__)) * float_fracx);
                        q0 = (s10 + s_0) +
                            (((s20 + s00) - (s10 + s_0)) * float_fracx);
                        q1 = (s11 + s_1) +
                            ((s21 + s01) - (s11 + s_1)) * float_fracx;
                        q2 = (s12 + s_2) +
                            (((s22 + s02) - (s12 + s_2)) * float_fracx);

                        q_ = s_ - q_ / 2.0F;
                        q0 = s0 - q0 / 2.0F;
                        q1 = s1 - q1 / 2.0F;
                        q2 = s2 - q2 / 2.0F;

                        s_ += (q_ * frac_xx);
                        s0 += (q0 * frac_xx);
                        s1 += (q1 * frac_xx);
                        s2 += (q2 * frac_xx);

                        s = s0 + ((s1 - s0) * float_fracy);
                        q = (s1 + s_) +
                            (((s2 + s0) - (s1 + s_)) * float_fracy);

                        q = s - q / 2.0F;

                        s += (q * frac_yy);

                        // Round the result
                        if (s < (float)(Integer.MIN_VALUE)) {
                           result = Integer.MIN_VALUE;
                        } else if (s > (float)(Integer.MAX_VALUE)) {
                            result = Integer.MAX_VALUE;
                        } else if (s > 0.0) {
                            result = (int) (s + 0.5F);
                        } else {
                            result = (int) (s - 0.5F);
                        }

                        // write the result
                        dstDataArrays[k2]
                            [dstPixelOffset+dstBandOffsets[k2]] = result;
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

                //
                // Get the 16 neighbouring positions of the
                // coordinate in question (p00).
                //
                //       p__  p0_  p1_  p2_
                //       p_0  p00  p10  p20
                //       p_1  p01  p11  p21
                //       p_2  p02  p12  p22
                //
                p__ = p_x + p_y - srcScanlineStride - srcPixelStride;
                p0_ = p__ + srcPixelStride;
                p1_ = p0_ + srcPixelStride;
                p2_ = p1_ + srcPixelStride;
                p_0 = p__ + srcScanlineStride;
                p00 = p_0 + srcPixelStride;
                p10 = p00 + srcPixelStride;
                p20 = p10 + srcPixelStride;
                p_1 = p_0 + srcScanlineStride;
                p01 = p_1 + srcPixelStride;
                p11 = p01 + srcPixelStride;
                p21 = p11 + srcPixelStride;
                p_2 = p_1 + srcScanlineStride;
                p02 = p_2 + srcPixelStride;
                p12 = p02 + srcPixelStride;
                p22 = p12 + srcPixelStride;

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
        float float_fracx, float_fracy;
        float frac_xx, frac_yy;

        int s_ix, s_iy;
        int p_x, p_y;

        int p__, p0_, p1_, p2_;
        int p_0, p00, p01, p02;
        int p_1, p10, p11, p12;
        int p_2, p20, p21, p22;

        short s__, s0_, s1_, s2_;
        short s_0, s00, s01, s02;
        short s_1, s10, s11, s12;
        short s_2, s20, s21, s22;

        float s0, s1, s_, s2;
        float q_, q0, q1, q2;
        float s, q;

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

            //
            // Get the 16 neighbouring positions of the
            // coordinate in question (p00).
            //
            //       p__  p0_  p1_  p2_
            //       p_0  p00  p10  p20
            //       p_1  p01  p11  p21
            //       p_2  p02  p12  p22
            //
            p__ = p_x + p_y - srcScanlineStride - srcPixelStride;
            p0_ = p__ + srcPixelStride;
            p1_ = p0_ + srcPixelStride;
            p2_ = p1_ + srcPixelStride;
            p_0 = p__ + srcScanlineStride;
            p00 = p_0 + srcPixelStride;
            p10 = p00 + srcPixelStride;
            p20 = p10 + srcPixelStride;
            p_1 = p_0 + srcScanlineStride;
            p01 = p_1 + srcPixelStride;
            p11 = p01 + srcPixelStride;
            p21 = p11 + srcPixelStride;
            p_2 = p_1 + srcScanlineStride;
            p02 = p_2 + srcPixelStride;
            p12 = p02 + srcPixelStride;
            p22 = p12 + srcPixelStride;

            for (int x = dst_min_x; x < dst_max_x; x++) {
                //
                // Check against the source rectangle
                //
                if ((s_ix >= (src_rect_x1 + 1)) &&
                    (s_ix < (src_rect_x2 - 2)) &&
                    (s_iy >= (src_rect_y1 + 1)) &&
                    (s_iy < (src_rect_y2 - 2))) {
                    for (int k2=0; k2 < dst_num_bands; k2++) {
                        //
                        // Get the pixels
                        //
                        short tmp_row[];
                        int tmp_col;

                        tmp_row = srcDataArrays[k2];
                        tmp_col = bandOffsets[k2];

                        s__ = tmp_row[p__ + tmp_col];
                        s0_ = tmp_row[p0_ + tmp_col];
                        s1_ = tmp_row[p1_ + tmp_col];
                        s2_ = tmp_row[p2_ + tmp_col];
                        s_0 = tmp_row[p_0 + tmp_col];
                        s00 = tmp_row[p00 + tmp_col];
                        s10 = tmp_row[p10 + tmp_col];
                        s20 = tmp_row[p20 + tmp_col];
                        s_1 = tmp_row[p_1 + tmp_col];
                        s01 = tmp_row[p01 + tmp_col];
                        s11 = tmp_row[p11 + tmp_col];
                        s21 = tmp_row[p21 + tmp_col];
                        s_2 = tmp_row[p_2 + tmp_col];
                        s02 = tmp_row[p02 + tmp_col];
                        s12 = tmp_row[p12 + tmp_col];
                        s22 = tmp_row[p22 + tmp_col];

                        // Get the new frac values
                        float_fracx = fracx;
                        float_fracy = fracy;
                        frac_xx = float_fracx * (1.0F - float_fracx);
                        frac_yy = float_fracy * (1.0F - float_fracy);

                        s0 = s00 + ((s10 - s00) * float_fracx);
                        s1 = s01 + ((s11 - s01) * float_fracx);
                        s_ = s0_ + ((s1_ - s0_) * float_fracx);
                        s2 = s02 + ((s12 - s02) * float_fracx);

                        q_ = (s1_ + s__) +
                            (((s2_ + s0_) - (s1_ + s__)) * float_fracx);
                        q0 = (s10 + s_0) +
                            (((s20 + s00) - (s10 + s_0)) * float_fracx);
                        q1 = (s11 + s_1) +
                            ((s21 + s01) - (s11 + s_1)) * float_fracx;
                        q2 = (s12 + s_2) +
                            (((s22 + s02) - (s12 + s_2)) * float_fracx);

                        q_ = s_ - q_ / 2.0F;
                        q0 = s0 - q0 / 2.0F;
                        q1 = s1 - q1 / 2.0F;
                        q2 = s2 - q2 / 2.0F;

                        s_ += (q_ * frac_xx);
                        s0 += (q0 * frac_xx);
                        s1 += (q1 * frac_xx);
                        s2 += (q2 * frac_xx);

                        s = s0 + ((s1 - s0) * float_fracy);
                        q = (s1 + s_) +
                            (((s2 + s0) - (s1 + s_)) * float_fracy);

                        q = s - q / 2.0F;

                        s += (q * frac_yy);

                        // Round the result
                        if (s < (float)Short.MIN_VALUE) {
                           result = Short.MIN_VALUE;
                        } else if (s > (float)Short.MAX_VALUE) {
                            result = Short.MAX_VALUE;
                        } else if (s > 0.0) {
                            result = (short) (s + 0.5F);
                        } else {
                            result = (short) (s - 0.5F);
                        }

                        // write the result
                        dstDataArrays[k2]
                            [dstPixelOffset+dstBandOffsets[k2]] = result;
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

                //
                // Get the 16 neighbouring positions of the
                // coordinate in question (p00).
                //
                //       p__  p0_  p1_  p2_
                //       p_0  p00  p10  p20
                //       p_1  p01  p11  p21
                //       p_2  p02  p12  p22
                //
                p__ = p_x + p_y - srcScanlineStride - srcPixelStride;
                p0_ = p__ + srcPixelStride;
                p1_ = p0_ + srcPixelStride;
                p2_ = p1_ + srcPixelStride;
                p_0 = p__ + srcScanlineStride;
                p00 = p_0 + srcPixelStride;
                p10 = p00 + srcPixelStride;
                p20 = p10 + srcPixelStride;
                p_1 = p_0 + srcScanlineStride;
                p01 = p_1 + srcPixelStride;
                p11 = p01 + srcPixelStride;
                p21 = p11 + srcPixelStride;
                p_2 = p_1 + srcScanlineStride;
                p02 = p_2 + srcPixelStride;
                p12 = p02 + srcPixelStride;
                p22 = p12 + srcPixelStride;

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
        float float_fracx, float_fracy;
        float frac_xx, frac_yy;

        int s_ix, s_iy;
        int p_x, p_y;

        int p__, p0_, p1_, p2_;
        int p_0, p00, p01, p02;
        int p_1, p10, p11, p12;
        int p_2, p20, p21, p22;

        int s__, s0_, s1_, s2_;
        int s_0, s00, s01, s02;
        int s_1, s10, s11, s12;
        int s_2, s20, s21, s22;

        float s0, s1, s_, s2;
        float q_, q0, q1, q2;
        float s, q;

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

            //
            // Get the 16 neighbouring positions of the
            // coordinate in question (p00).
            //
            //       p__  p0_  p1_  p2_
            //       p_0  p00  p10  p20
            //       p_1  p01  p11  p21
            //       p_2  p02  p12  p22
            //
            p__ = p_x + p_y - srcScanlineStride - srcPixelStride;
            p0_ = p__ + srcPixelStride;
            p1_ = p0_ + srcPixelStride;
            p2_ = p1_ + srcPixelStride;
            p_0 = p__ + srcScanlineStride;
            p00 = p_0 + srcPixelStride;
            p10 = p00 + srcPixelStride;
            p20 = p10 + srcPixelStride;
            p_1 = p_0 + srcScanlineStride;
            p01 = p_1 + srcPixelStride;
            p11 = p01 + srcPixelStride;
            p21 = p11 + srcPixelStride;
            p_2 = p_1 + srcScanlineStride;
            p02 = p_2 + srcPixelStride;
            p12 = p02 + srcPixelStride;
            p22 = p12 + srcPixelStride;

            for (int x = dst_min_x; x < dst_max_x; x++) {
                //
                // Check against the source rectangle
                //
                if ((s_ix >= (src_rect_x1 + 1)) &&
                    (s_ix < (src_rect_x2 - 2)) &&
                    (s_iy >= (src_rect_y1 + 1)) &&
                    (s_iy < (src_rect_y2 - 2))) {
                    for (int k2=0; k2 < dst_num_bands; k2++) {
                        //
                        // Get the pixels
                        //
                        short tmp_row[];
                        int tmp_col;

                        tmp_row = srcDataArrays[k2];
                        tmp_col = bandOffsets[k2];

                        s__ = tmp_row[p__ + tmp_col] & 0xffff;
                        s0_ = tmp_row[p0_ + tmp_col] & 0xffff;
                        s1_ = tmp_row[p1_ + tmp_col] & 0xffff;
                        s2_ = tmp_row[p2_ + tmp_col] & 0xffff;
                        s_0 = tmp_row[p_0 + tmp_col] & 0xffff;
                        s00 = tmp_row[p00 + tmp_col] & 0xffff;
                        s10 = tmp_row[p10 + tmp_col] & 0xffff;
                        s20 = tmp_row[p20 + tmp_col] & 0xffff;
                        s_1 = tmp_row[p_1 + tmp_col] & 0xffff;
                        s01 = tmp_row[p01 + tmp_col] & 0xffff;
                        s11 = tmp_row[p11 + tmp_col] & 0xffff;
                        s21 = tmp_row[p21 + tmp_col] & 0xffff;
                        s_2 = tmp_row[p_2 + tmp_col] & 0xffff;
                        s02 = tmp_row[p02 + tmp_col] & 0xffff;
                        s12 = tmp_row[p12 + tmp_col] & 0xffff;
                        s22 = tmp_row[p22 + tmp_col] & 0xffff;

                        // Get the new frac values
                        float_fracx = fracx;
                        float_fracy = fracy;
                        frac_xx = float_fracx * (1.0F - float_fracx);
                        frac_yy = float_fracy * (1.0F - float_fracy);

                        s0 = s00 + ((s10 - s00) * float_fracx);
                        s1 = s01 + ((s11 - s01) * float_fracx);
                        s_ = s0_ + ((s1_ - s0_) * float_fracx);
                        s2 = s02 + ((s12 - s02) * float_fracx);

                        q_ = (s1_ + s__) +
                            (((s2_ + s0_) - (s1_ + s__)) * float_fracx);
                        q0 = (s10 + s_0) +
                            (((s20 + s00) - (s10 + s_0)) * float_fracx);
                        q1 = (s11 + s_1) +
                            ((s21 + s01) - (s11 + s_1)) * float_fracx;
                        q2 = (s12 + s_2) +
                            (((s22 + s02) - (s12 + s_2)) * float_fracx);

                        q_ = s_ - q_ / 2.0F;
                        q0 = s0 - q0 / 2.0F;
                        q1 = s1 - q1 / 2.0F;
                        q2 = s2 - q2 / 2.0F;

                        s_ += (q_ * frac_xx);
                        s0 += (q0 * frac_xx);
                        s1 += (q1 * frac_xx);
                        s2 += (q2 * frac_xx);

                        s = s0 + ((s1 - s0) * float_fracy);
                        q = (s1 + s_) +
                            (((s2 + s0) - (s1 + s_)) * float_fracy);


                        q = s - q / 2.0F;

                        s += (q * frac_yy);

                        // Round
                        if (s < 0.0) {
                           result = 0;
                        } else if (s > (float) USHORT_MAX) {
                            result = USHORT_MAX;
                        } else {
                            result = (int) (s + 0.5F);
                        }

                        // write the result
                        dstDataArrays[k2]
                            [dstPixelOffset+dstBandOffsets[k2]] =
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

                //
                // Get the 16 neighbouring positions of the
                // coordinate in question (p00).
                //
                //       p__  p0_  p1_  p2_
                //       p_0  p00  p10  p20
                //       p_1  p01  p11  p21
                //       p_2  p02  p12  p22
                //
                p__ = p_x + p_y - srcScanlineStride - srcPixelStride;
                p0_ = p__ + srcPixelStride;
                p1_ = p0_ + srcPixelStride;
                p2_ = p1_ + srcPixelStride;
                p_0 = p__ + srcScanlineStride;
                p00 = p_0 + srcPixelStride;
                p10 = p00 + srcPixelStride;
                p20 = p10 + srcPixelStride;
                p_1 = p_0 + srcScanlineStride;
                p01 = p_1 + srcPixelStride;
                p11 = p01 + srcPixelStride;
                p21 = p11 + srcPixelStride;
                p_2 = p_1 + srcScanlineStride;
                p02 = p_2 + srcPixelStride;
                p12 = p02 + srcPixelStride;
                p22 = p12 + srcPixelStride;

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
        float float_fracx, float_fracy;
        float frac_xx, frac_yy;

        int s_ix, s_iy;
        int p_x, p_y;

        int p__, p0_, p1_, p2_;
        int p_0, p00, p01, p02;
        int p_1, p10, p11, p12;
        int p_2, p20, p21, p22;

        float s__, s0_, s1_, s2_;
        float s_0, s00, s01, s02;
        float s_1, s10, s11, s12;
        float s_2, s20, s21, s22;

        float s0, s1, s_, s2;
        float q_, q0, q1, q2;
        float s, q;

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

            //
            // Get the 16 neighbouring positions of the
            // coordinate in question (p00).
            //
            //       p__  p0_  p1_  p2_
            //       p_0  p00  p10  p20
            //       p_1  p01  p11  p21
            //       p_2  p02  p12  p22
            //
            p__ = p_x + p_y - srcScanlineStride - srcPixelStride;
            p0_ = p__ + srcPixelStride;
            p1_ = p0_ + srcPixelStride;
            p2_ = p1_ + srcPixelStride;
            p_0 = p__ + srcScanlineStride;
            p00 = p_0 + srcPixelStride;
            p10 = p00 + srcPixelStride;
            p20 = p10 + srcPixelStride;
            p_1 = p_0 + srcScanlineStride;
            p01 = p_1 + srcPixelStride;
            p11 = p01 + srcPixelStride;
            p21 = p11 + srcPixelStride;
            p_2 = p_1 + srcScanlineStride;
            p02 = p_2 + srcPixelStride;
            p12 = p02 + srcPixelStride;
            p22 = p12 + srcPixelStride;

            for (int x = dst_min_x; x < dst_max_x; x++) {
                //
                // Check against the source rectangle
                //
                if ((s_ix >= (src_rect_x1 + 1)) &&
                    (s_ix < (src_rect_x2 - 2)) &&
                    (s_iy >= (src_rect_y1 + 1)) &&
                    (s_iy < (src_rect_y2 - 2))) {
                    for (int k2=0; k2 < dst_num_bands; k2++) {
                        //
                        // Get the pixels
                        //
                        float tmp_row[];
                        int tmp_col;

                        tmp_row = srcDataArrays[k2];
                        tmp_col = bandOffsets[k2];

                        s__ = tmp_row[p__ + tmp_col];
                        s0_ = tmp_row[p0_ + tmp_col];
                        s1_ = tmp_row[p1_ + tmp_col];
                        s2_ = tmp_row[p2_ + tmp_col];
                        s_0 = tmp_row[p_0 + tmp_col];
                        s00 = tmp_row[p00 + tmp_col];
                        s10 = tmp_row[p10 + tmp_col];
                        s20 = tmp_row[p20 + tmp_col];
                        s_1 = tmp_row[p_1 + tmp_col];
                        s01 = tmp_row[p01 + tmp_col];
                        s11 = tmp_row[p11 + tmp_col];
                        s21 = tmp_row[p21 + tmp_col];
                        s_2 = tmp_row[p_2 + tmp_col];
                        s02 = tmp_row[p02 + tmp_col];
                        s12 = tmp_row[p12 + tmp_col];
                        s22 = tmp_row[p22 + tmp_col];

                        // Get the new frac values
                        float_fracx = fracx;
                        float_fracy = fracy;
                        frac_xx = float_fracx * (1.0F - float_fracx);
                        frac_yy = float_fracy * (1.0F - float_fracy);

                        s0 = s00 + ((s10 - s00) * float_fracx);
                        s1 = s01 + ((s11 - s01) * float_fracx);
                        s_ = s0_ + ((s1_ - s0_) * float_fracx);
                        s2 = s02 + ((s12 - s02) * float_fracx);

                        q_ = (s1_ + s__) +
                            (((s2_ + s0_) - (s1_ + s__)) * float_fracx);
                        q0 = (s10 + s_0) +
                            (((s20 + s00) - (s10 + s_0)) * float_fracx);
                        q1 = (s11 + s_1) +
                            ((s21 + s01) - (s11 + s_1)) * float_fracx;
                        q2 = (s12 + s_2) +
                            (((s22 + s02) - (s12 + s_2)) * float_fracx);

                        q_ = s_ - q_ / 2.0F;
                        q0 = s0 - q0 / 2.0F;
                        q1 = s1 - q1 / 2.0F;
                        q2 = s2 - q2 / 2.0F;

                        s_ += (q_ * frac_xx);
                        s0 += (q0 * frac_xx);
                        s1 += (q1 * frac_xx);
                        s2 += (q2 * frac_xx);

                        s = s0 + ((s1 - s0) * float_fracy);
                        q = (s1 + s_) +
                            (((s2 + s0) - (s1 + s_)) * float_fracy);


                        q = s - q / 2.0F;

                        s += (q * frac_yy);

                        // write the result
                        dstDataArrays[k2]
                            [dstPixelOffset+dstBandOffsets[k2]] = s;
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

                //
                // Get the 16 neighbouring positions of the
                // coordinate in question (p00).
                //
                //       p__  p0_  p1_  p2_
                //       p_0  p00  p10  p20
                //       p_1  p01  p11  p21
                //       p_2  p02  p12  p22
                //
                p__ = p_x + p_y - srcScanlineStride - srcPixelStride;
                p0_ = p__ + srcPixelStride;
                p1_ = p0_ + srcPixelStride;
                p2_ = p1_ + srcPixelStride;
                p_0 = p__ + srcScanlineStride;
                p00 = p_0 + srcPixelStride;
                p10 = p00 + srcPixelStride;
                p20 = p10 + srcPixelStride;
                p_1 = p_0 + srcScanlineStride;
                p01 = p_1 + srcPixelStride;
                p11 = p01 + srcPixelStride;
                p21 = p11 + srcPixelStride;
                p_2 = p_1 + srcScanlineStride;
                p02 = p_2 + srcPixelStride;
                p12 = p02 + srcPixelStride;
                p22 = p12 + srcPixelStride;

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
        double float_fracx, float_fracy;
        double frac_xx, frac_yy;

        int s_ix, s_iy;
        int p_x, p_y;

        int p__, p0_, p1_, p2_;
        int p_0, p00, p01, p02;
        int p_1, p10, p11, p12;
        int p_2, p20, p21, p22;

        double s__, s0_, s1_, s2_;
        double s_0, s00, s01, s02;
        double s_1, s10, s11, s12;
        double s_2, s20, s21, s22;

        double s0, s1, s_, s2;
        double q_, q0, q1, q2;
        double s, q;

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

            //
            // Get the 16 neighbouring positions of the
            // coordinate in question (p00).
            //
            //       p__  p0_  p1_  p2_
            //       p_0  p00  p10  p20
            //       p_1  p01  p11  p21
            //       p_2  p02  p12  p22
            //
            p__ = p_x + p_y - srcScanlineStride - srcPixelStride;
            p0_ = p__ + srcPixelStride;
            p1_ = p0_ + srcPixelStride;
            p2_ = p1_ + srcPixelStride;
            p_0 = p__ + srcScanlineStride;
            p00 = p_0 + srcPixelStride;
            p10 = p00 + srcPixelStride;
            p20 = p10 + srcPixelStride;
            p_1 = p_0 + srcScanlineStride;
            p01 = p_1 + srcPixelStride;
            p11 = p01 + srcPixelStride;
            p21 = p11 + srcPixelStride;
            p_2 = p_1 + srcScanlineStride;
            p02 = p_2 + srcPixelStride;
            p12 = p02 + srcPixelStride;
            p22 = p12 + srcPixelStride;

            for (int x = dst_min_x; x < dst_max_x; x++) {
                //
                // Check against the source rectangle
                //
                if ((s_ix >= (src_rect_x1 + 1)) &&
                    (s_ix < (src_rect_x2 - 2)) &&
                    (s_iy >= (src_rect_y1 + 1)) &&
                    (s_iy < (src_rect_y2 - 2))) {
                    for (int k2=0; k2 < dst_num_bands; k2++) {
                        //
                        // Get the pixels
                        //
                        double tmp_row[];
                        int tmp_col;

                        tmp_row = srcDataArrays[k2];
                        tmp_col = bandOffsets[k2];

                        s__ = tmp_row[p__ + tmp_col];
                        s0_ = tmp_row[p0_ + tmp_col];
                        s1_ = tmp_row[p1_ + tmp_col];
                        s2_ = tmp_row[p2_ + tmp_col];
                        s_0 = tmp_row[p_0 + tmp_col];
                        s00 = tmp_row[p00 + tmp_col];
                        s10 = tmp_row[p10 + tmp_col];
                        s20 = tmp_row[p20 + tmp_col];
                        s_1 = tmp_row[p_1 + tmp_col];
                        s01 = tmp_row[p01 + tmp_col];
                        s11 = tmp_row[p11 + tmp_col];
                        s21 = tmp_row[p21 + tmp_col];
                        s_2 = tmp_row[p_2 + tmp_col];
                        s02 = tmp_row[p02 + tmp_col];
                        s12 = tmp_row[p12 + tmp_col];
                        s22 = tmp_row[p22 + tmp_col];

                        // Get the new frac values
                        float_fracx = fracx;
                        float_fracy = fracy;
                        frac_xx = float_fracx * (1.0F - float_fracx);
                        frac_yy = float_fracy * (1.0F - float_fracy);

                        s0 = s00 + ((s10 - s00) * float_fracx);
                        s1 = s01 + ((s11 - s01) * float_fracx);
                        s_ = s0_ + ((s1_ - s0_) * float_fracx);
                        s2 = s02 + ((s12 - s02) * float_fracx);

                        q_ = (s1_ + s__) +
                            (((s2_ + s0_) - (s1_ + s__)) * float_fracx);
                        q0 = (s10 + s_0) +
                            (((s20 + s00) - (s10 + s_0)) * float_fracx);
                        q1 = (s11 + s_1) +
                            ((s21 + s01) - (s11 + s_1)) * float_fracx;
                        q2 = (s12 + s_2) +
                            (((s22 + s02) - (s12 + s_2)) * float_fracx);

                        q_ = s_ - q_ / 2.0F;
                        q0 = s0 - q0 / 2.0F;
                        q1 = s1 - q1 / 2.0F;
                        q2 = s2 - q2 / 2.0F;

                        s_ += (q_ * frac_xx);
                        s0 += (q0 * frac_xx);
                        s1 += (q1 * frac_xx);
                        s2 += (q2 * frac_xx);

                        s = s0 + ((s1 - s0) * float_fracy);
                        q = (s1 + s_) +
                            (((s2 + s0) - (s1 + s_)) * float_fracy);

                        q = s - q / 2.0F;

                        s += (q * frac_yy);

                        // write the result
                        dstDataArrays[k2]
                            [dstPixelOffset+dstBandOffsets[k2]] = s;
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

                //
                // Get the 16 neighbouring positions of the
                // coordinate in question (p00).
                //
                //       p__  p0_  p1_  p2_
                //       p_0  p00  p10  p20
                //       p_1  p01  p11  p21
                //       p_2  p02  p12  p22
                //
                p__ = p_x + p_y - srcScanlineStride - srcPixelStride;
                p0_ = p__ + srcPixelStride;
                p1_ = p0_ + srcPixelStride;
                p2_ = p1_ + srcPixelStride;
                p_0 = p__ + srcScanlineStride;
                p00 = p_0 + srcPixelStride;
                p10 = p00 + srcPixelStride;
                p20 = p10 + srcPixelStride;
                p_1 = p_0 + srcScanlineStride;
                p01 = p_1 + srcPixelStride;
                p11 = p01 + srcPixelStride;
                p21 = p11 + srcPixelStride;
                p_2 = p_1 + srcScanlineStride;
                p02 = p_2 + srcPixelStride;
                p12 = p02 + srcPixelStride;
                p22 = p12 + srcPixelStride;

                dstPixelOffset += dstPixelStride;
            }

            dstOffset += dstScanlineStride;
        }
    }

//     public static OpImage createTestImage(OpImageTester oit) {
// 	Interpolation interp = new InterpolationBicubic(8);
//         AffineTransform tr = new AffineTransform(0.707107,
//                                                  -0.707106,
//                                                  0.707106,
//                                                  0.707107,
//                                                  0.0,
//                                                  0.0);

//         return new AffineBicubicOpImage(oit.getSource(), null, null,
//                                         new ImageLayout(oit.getSource()),
//                                         tr,
//                                         interp);
//     }

//     // Calls a method on OpImage that uses introspection, to make this
//     // class, discover it's createTestImage() call, call it and then
//     // benchmark the performance of the created OpImage chain.
//     public static void main(String args[]) {
//         String classname = "com.lightcrafts.media.jai.opimage.AffineBicubicOpImage";
//         OpImageTester.performDiagnostics(classname, args);
//     }
}
