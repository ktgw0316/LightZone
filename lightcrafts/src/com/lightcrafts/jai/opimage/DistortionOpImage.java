/* Copyright (C) 2015 Masahiro Kitagawa */

package com.lightcrafts.jai.opimage;

import com.lightcrafts.mediax.jai.BorderExtender;
import com.lightcrafts.mediax.jai.GeometricOpImage;
import com.lightcrafts.mediax.jai.RasterFormatTag;
import com.lightcrafts.mediax.jai.RasterAccessor;

import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.awt.image.DataBuffer;
import java.awt.*;
import java.util.Map;

public class DistortionOpImage extends GeometricOpImage {

    private final int fullWidth;
    private final int fullHeight;
    private String cameraMaker = "";
    private String cameraModel = "";
    private String lensName = "";
    private float focal = 0f;
    private float aperture = 0f;

    // Coeffs for 5th order polynomial distortion model
    // c.f. http://www.imatest.com/docs/distortion.html
    private float k1 = 0f;
    private float k2 = 0f;
    private float kr = 1f;
    private float kb = 1f;

    public DistortionOpImage(RenderedImage source, Map configuration, BorderExtender extender,
                             float k1, float k2, float kr, float kb) {
        super(vectorize(source), null, configuration, true, extender, null);

        fullWidth  = source.getWidth();
        fullHeight = source.getHeight();
        this.k1 = k1;
        this.k2 = k2;
        this.kr = kr;
        this.kb = kb;
    }

    public DistortionOpImage(RenderedImage source, Map configuration, BorderExtender extender,
                             String cameraMaker, String cameraModel,
                             String lensName, float focal, float aperture) {
        super(vectorize(source), null, configuration, true, extender, null);

        fullWidth  = source.getWidth();
        fullHeight = source.getHeight();
        this.cameraMaker = cameraMaker;
        this.cameraModel = cameraModel;
        this.lensName = lensName;
        this.focal = focal;
        this.aperture = aperture;
    }

    @Override
    protected Rectangle forwardMapRect(Rectangle sourceRect, int sourceIndex) {
        return null;
    }

    @Override
    protected Rectangle backwardMapRect(Rectangle destRect, int sourceIndex) {
        if (sourceIndex != 0)
            return null;

        final float centerX = fullWidth / 2;
        final float centerY = fullHeight / 2;

        final float rx0 = destRect.x - centerX;
        final float ry0 = destRect.y - centerY;
        final float rx1 = rx0 + destRect.width;
        final float ry1 = ry0 + destRect.height;

        final float rMaxSq = (fullWidth * fullWidth + fullHeight * fullHeight) / 4;

        final float smallestMagnitude = Math.min(Math.min(kr, kb), 1);
        final float biggestMagnitude  = Math.max(Math.max(kr, kb), 1);

        // Find minimum of top edge and maximum of bottom edge
        float top    = (int) coeff((rx0 * rx0 + ry0 * ry0) / rMaxSq) * ry0;
        float bottom = (int) coeff((rx0 * rx0 + ry1 * ry1) / rMaxSq) * ry1;
        for (int rx = (int) rx0; rx <= rx1; rx++) {
            float topTmp = coeff((rx * rx + ry0 * ry0) / rMaxSq) * ry0;
            topTmp *= (topTmp < 0) ? biggestMagnitude : smallestMagnitude;
            if (topTmp < top) {
                top = topTmp;
            }
            float bottomTmp = coeff((rx * rx + ry1 * ry1) / rMaxSq) * ry1;
            bottomTmp *= (bottomTmp > 0) ? biggestMagnitude : smallestMagnitude;
            if (bottomTmp > bottom) {
                bottom = bottomTmp;
            }
        }
        final int h = (int) (bottom - top + 1);
        top += centerY;

        // Find minimum of left edge and maximum of right edge
        float left  = (int) coeff((rx0 * rx0 + ry0 * ry0) / rMaxSq) * rx0;
        float right = (int) coeff((rx1 * rx1 + ry0 * ry0) / rMaxSq) * rx1;
        for (int ry = (int) ry0; ry <= ry1; ry++) {
            float leftTmp = coeff((rx0 * rx0 + ry * ry) / rMaxSq) * rx0;
            leftTmp *= (leftTmp < 0) ? biggestMagnitude : smallestMagnitude;
            if (leftTmp < left) {
                left = leftTmp;
            }
            float rightTmp = coeff((rx1 * rx1 + ry * ry) / rMaxSq) * rx1;
            rightTmp *= (rightTmp > 0) ? biggestMagnitude : smallestMagnitude;
            if (rightTmp > right) {
                right = rightTmp;
            }
        }
        final int w = (int) (right - left + 1);
        left += centerX;

        Rectangle rect = new Rectangle((int) left, (int) top, w, h);
        return rect;
    }

    private float coeff(final float radiusSq) {
        // 5th order polynomial distortion model, scaled
        return (1 + k1 * radiusSq + k2 * radiusSq * radiusSq) / (1 + k1 + k2);
    }

    @Override
    protected void computeRect(Raster[] sources,
                               WritableRaster dest,
                               Rectangle destRect) {
        // Retrieve format tags.
        RasterFormatTag[] formatTags = getFormatTags();

        Raster source = sources[0];
        Rectangle srcRect = mapDestRect(destRect, 0);

        RasterAccessor srcAccessor =
                new RasterAccessor(source, srcRect, formatTags[0],
                                   getSourceImage(0).getColorModel());
        RasterAccessor dstAccessor =
                new RasterAccessor(dest, destRect, formatTags[1],
                                   this.getColorModel());

        if (dstAccessor.getDataType() == DataBuffer.TYPE_USHORT) {
            ushortLoop(srcAccessor, dstAccessor);
        }

        // If the RasterAccessor object set up a temporary buffer for the
        // op to write to, tell the RasterAccessor to write that data
        // to the raster no that we're done with it.
        if (dstAccessor.isDataCopy()) {
            dstAccessor.clampDataArrays();
            dstAccessor.copyDataToRaster();
        }
    }

    protected void ushortLoop(RasterAccessor src, RasterAccessor dst) {
        final int dstX = dst.getX();
        final int dstY = dst.getY();
        final int dstWidth = dst.getWidth();
        final int dstHeight = dst.getHeight();

        short dstDataArrays[][] = dst.getShortDataArrays();
        final int dstBandOffsets[] = dst.getBandOffsets();
        final int dstPixelStride = dst.getPixelStride();
        final int dstScanlineStride = dst.getScanlineStride();

        final short srcDataArrays[][] = src.getShortDataArrays();
        final int srcBandOffsets[] = src.getBandOffsets();
        final int srcPixelStride = src.getPixelStride();
        final int srcScanlineStride = src.getScanlineStride();

        final int srcX = src.getX();
        final int srcY = src.getY();
        final int srcWidth = src.getWidth();
        final int srcHeight = src.getHeight();

        short dstData[] = dstDataArrays[0];
        short srcData[] = srcDataArrays[0];

        if (src.getNumBands() == 1) {
            synchronized(this) {
                distortionMono(srcData, dstData,
                               fullWidth, fullHeight,
                               srcX, srcY, srcWidth, srcHeight,
                               dstX, dstY, dstWidth, dstHeight,
                               srcPixelStride, dstPixelStride,
                               srcBandOffsets[0], dstBandOffsets[0],
                               srcScanlineStride, dstScanlineStride, k1, k2);
            }
        }
        else if (src.getNumBands() == 3) {
            if (cameraModel.isEmpty() && lensName.isEmpty()) {
                synchronized(this) {
                    distortionColor(srcData, dstData,
                                    fullWidth, fullHeight,
                                    srcX, srcY, srcWidth, srcHeight,
                                    dstX, dstY, dstWidth, dstHeight,
                                    srcPixelStride, dstPixelStride,
                                    srcBandOffsets[0], srcBandOffsets[1], srcBandOffsets[2],
                                    dstBandOffsets[0], dstBandOffsets[1], dstBandOffsets[2],
                                    srcScanlineStride, dstScanlineStride, k1, k2, kr, kb);
                }
            }
            else {
                System.out.println("camera maker = " + cameraMaker); // DEBUG
                System.out.println("camera model = " + cameraModel); // DEBUG
                System.out.println("lens name    = " + lensName);    // DEBUG
                synchronized(this) {
                    lensfun(srcData, dstData,
                            fullWidth, fullHeight,
                            // srcX, srcY,
                            dstX, dstY, dstWidth, dstHeight,
                            srcPixelStride, dstPixelStride,
                            srcBandOffsets[0], srcBandOffsets[1], srcBandOffsets[2],
                            dstBandOffsets[0], dstBandOffsets[1], dstBandOffsets[2],
                            srcScanlineStride, dstScanlineStride,
                            cameraMaker, cameraModel,
                            lensName, focal, aperture);
                }
            }
        }
    }

    static native void distortionMono(short srcData[], short dstData[],
                                      int fullWidth, int fullHeight,
                                      int srcRectX, int srcRectY,
                                      int srcRectWidth, int srcRectHeight,
                                      int dstRectX, int dstRectY,
                                      int dstRectWidth, int dstRectHeight,
                                      int srcPixelStride, int dstPixelStride,
                                      int srcOffset, int dstOffset,
                                      int srcLineStride, int dstLineStride,
                                      float k1, float k2);

    static native void distortionColor(short srcData[], short dstData[],
                                       int fullWidth, int fullHeight,
                                       int srcRectX, int srcRectY,
                                       int srcRectWidth, int srcRectHeight,
                                       int dstRectX, int dstRectY,
                                       int dstRectWidth, int dstRectHeight,
                                       int srcPixelStride, int dstPixelStride,
                                       int srcROffset, int srcGOffset, int srcBOffset,
                                       int dstROffset, int dstGOffset, int dstBOffset,
                                       int srcLineStride, int dstLineStride,
                                       float k1, float k2, float kr, float kb);

    static native void lensfun(short srcData[], short dstData[],
                               int fullWidth, int fullHeight,
                               int rectX, int rectY, int rectWidth, int rectHeight,
                               int srcPixelStride, int dstPixelStride,
                               int srcROffset, int srcGOffset, int srcBOffset,
                               int dstROffset, int dstGOffset, int dstBOffset,
                               int srcLineStride, int dstLineStride,
                               String cameraMaker, String cameraModel,
                               String lensName, float focal, float aperture);
}
