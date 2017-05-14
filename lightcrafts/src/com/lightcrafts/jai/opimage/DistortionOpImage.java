/* Copyright (C) 2015- Masahiro Kitagawa */

package com.lightcrafts.jai.opimage;

import com.lightcrafts.mediax.jai.BorderExtender;
import com.lightcrafts.mediax.jai.GeometricOpImage;
import com.lightcrafts.mediax.jai.RasterFormatTag;
import com.lightcrafts.mediax.jai.RasterAccessor;
import com.lightcrafts.utils.Lensfun;

import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.awt.image.DataBuffer;
import java.awt.*;
import java.awt.geom.Point2D;
import java.util.Map;

import lombok.val;

public class DistortionOpImage extends GeometricOpImage {

    private final int fullWidth;
    private final int fullHeight;
    private final Point2D center;
    private final boolean shouldUseLensfun;
    private String cameraMaker = "";
    private String cameraModel = "";
    private String lensMaker = "";
    private String lensModel = "";
    private float focal = 0f;
    private float aperture = 0f;

    private static float[] distTerms = {0, 0, 0};
    private static float[] tcaTerms = {1f, 1f};

    private interface DistModel {
        float coeff(final float radiusSq);
    }

    private enum DistModelImpl implements DistModel {
        // c.f. http://lensfun.sourceforge.net/manual/group__Lens.html#gaa505e04666a189274ba66316697e308e
        DIST_MODEL_NONE {
            @Override
            public float coeff(final float radiusSq) {
                return 1f;
            }
        },
        DIST_MODEL_POLY3 {
            @Override
            public float coeff(final float radiusSq) {
                float k1 = distTerms[0];
                return 1f - k1 + k1 * radiusSq;
            }
        },
        DIST_MODEL_POLY5 {
            @Override
            public float coeff(final float radiusSq) {
                float k1 = distTerms[0];
                float k2 = distTerms[1];
                return 1f + radiusSq * (k1 + radiusSq * k2);
                // = 1 + k1 * r^2 + k2 * r^4;
            }
        },
        DIST_MODEL_PTLENS {
            @Override
            public float coeff(final float radiusSq) {
                float k1 = distTerms[0];
                float k2 = distTerms[1];
                float k3 = distTerms[2];
                final float radius = (float) Math.sqrt(radiusSq);
                return (k1 * radius + k2) * radiusSq + k3 * radius + 1f - k1 - k2 - k3;
                // = k1 * r^3 + k2 * r^2 + k3 * r + 1 - k1 - k2 - k3;
            }
        },
        // c.f. http://download.macromedia.com/pub/labs/lensprofile_creator/lensprofile_creator_cameramodel.pdf
        DIST_MODEL_ADOBE_RECTILINEAR {
            @Override
            public float coeff(final float radiusSq) {
                float k1 = distTerms[0];
                float k2 = distTerms[1];
                float k3 = distTerms[2];
                // NOTE: We ignore k4 and k5 terms
                return 1f + radiusSq * (k1 + radiusSq * (k2 + radiusSq * k3));
                // = 1 + k1 * r^2 + k2 * r^4 + k3 * r^6;
            }
        },
        DIST_MODEL_LIGHTZONE {
            @Override
            public float coeff(final float radiusSq) {
                float k1 = distTerms[0];
                float k2 = distTerms[1];
                return (1f + radiusSq * (k1 + radiusSq * k2)) / (1f + k1 + k2);
                // = POLY5 / (1 + k1 + k2)
            }
        }
    }

    private DistModelImpl distModel = DistModelImpl.DIST_MODEL_LIGHTZONE;

    public DistortionOpImage(RenderedImage source, Map configuration, BorderExtender extender,
                             int fullWidth, int fullHeight, Point2D center,
                             float k1, float k2, float kr, float kb) {
        super(vectorize(source), null, configuration, true, extender, null);

        shouldUseLensfun = false;

        this.fullWidth  = fullWidth;
        this.fullHeight = fullHeight;
        this.center = center;

        distTerms[0] = k1;
        distTerms[1] = k2;
        tcaTerms[0] = kr;
        tcaTerms[1] = kb;

        // distModel = DistModelImpl.DIST_MODEL_POLY5;
        distModel = DistModelImpl.DIST_MODEL_LIGHTZONE;
    }

    public DistortionOpImage(RenderedImage source, Map configuration, BorderExtender extender,
                             int fullWidth, int fullHeight, Point2D center,
                             String cameraMaker, String cameraModel,
                             String lensMaker, String lensModel,
                             float focal, float aperture) {
        super(vectorize(source), null, configuration, true, extender, null);

        shouldUseLensfun = true;

        this.fullWidth  = fullWidth;
        this.fullHeight = fullHeight;
        this.center = center;

        this.cameraMaker = cameraMaker;
        this.cameraModel = cameraModel;
        this.lensMaker = lensMaker;
        this.lensModel = lensModel;
        this.focal = focal;
        this.aperture = aperture;

        if (!cameraModel.isEmpty() || !lensModel.isEmpty()) {
            System.out.println("camera maker = " + cameraMaker); // DEBUG
            System.out.println("camera model = " + cameraModel); // DEBUG
            System.out.println("lens maker   = " + lensMaker);   // DEBUG
            System.out.println("lens model   = " + lensModel);   // DEBUG
            System.out.println("focal length = " + focal);       // DEBUG
            System.out.println("aperture     = " + aperture);    // DEBUG

            /*
            val lensfun = new Lensfun(cameraMaker, cameraModel,
                    lensMaker, lensModel, focal, aperture);
            distTerms = lensfun.getDistTerms();
            tcaTerms  = lensfun.getTcaTerms();
            val model = lensfun.getDistModel();
            distModel = DistModelImpl.values()[model[0]];
            */
        }
        else {
            distModel = DistModelImpl.DIST_MODEL_NONE;
        }
        System.out.println("distortion model = " + distModel.name()); // DEBUG
    }

    @Override
    protected Rectangle forwardMapRect(Rectangle sourceRect, int sourceIndex) {
        return null;
    }

    @Override
    protected Rectangle backwardMapRect(Rectangle destRect, int sourceIndex) {
        if (sourceIndex != 0)
            return null;

        if (shouldUseLensfun) {
            final int[] srcRect;
            synchronized(this) {
                srcRect = backwardMapRectLF(fullWidth, fullHeight,
                        (int)center.getX(), (int)center.getY(),
                        destRect.x, destRect.y, destRect.width, destRect.height,
                        cameraMaker, cameraModel, lensMaker, lensModel, focal, aperture);
            }
            return new Rectangle(srcRect[0], srcRect[1], srcRect[2], srcRect[3]);
        }

        final float centerX = (float) center.getX();
        final float centerY = (float) center.getY();

        final float rx0 = destRect.x - centerX;
        final float ry0 = destRect.y - centerY;
        final float rx1 = rx0 + destRect.width;
        final float ry1 = ry0 + destRect.height;

        final float rMaxSq = (fullWidth * fullWidth + fullHeight * fullHeight) / 4;

        final float kr = tcaTerms[0];
        final float kb = tcaTerms[1];
        final float smallestMagnitude = Math.min(Math.min(kr, kb), 1);
        final float biggestMagnitude  = Math.max(Math.max(kr, kb), 1);

        // Find minimum of top edge and maximum of bottom edge
        float top    = (int) distModel.coeff((rx0 * rx0 + ry0 * ry0) / rMaxSq) * ry0;
        float bottom = (int) distModel.coeff((rx0 * rx0 + ry1 * ry1) / rMaxSq) * ry1;
        for (int rx = (int) rx0; rx <= rx1; rx++) {
            float topTmp = distModel.coeff((rx * rx + ry0 * ry0) / rMaxSq) * ry0;
            topTmp *= (topTmp < 0) ? biggestMagnitude : smallestMagnitude;
            if (topTmp < top) {
                top = topTmp;
            }
            float bottomTmp = distModel.coeff((rx * rx + ry1 * ry1) / rMaxSq) * ry1;
            bottomTmp *= (bottomTmp > 0) ? biggestMagnitude : smallestMagnitude;
            if (bottomTmp > bottom) {
                bottom = bottomTmp;
            }
        }
        final int h = (int) (bottom - top + 1);
        top += centerY;

        // Find minimum of left edge and maximum of right edge
        float left  = (int) distModel.coeff((rx0 * rx0 + ry0 * ry0) / rMaxSq) * rx0;
        float right = (int) distModel.coeff((rx1 * rx1 + ry0 * ry0) / rMaxSq) * rx1;
        for (int ry = (int) ry0; ry <= ry1; ry++) {
            float leftTmp = distModel.coeff((rx0 * rx0 + ry * ry) / rMaxSq) * rx0;
            leftTmp *= (leftTmp < 0) ? biggestMagnitude : smallestMagnitude;
            if (leftTmp < left) {
                left = leftTmp;
            }
            float rightTmp = distModel.coeff((rx1 * rx1 + ry * ry) / rMaxSq) * rx1;
            rightTmp *= (rightTmp > 0) ? biggestMagnitude : smallestMagnitude;
            if (rightTmp > right) {
                right = rightTmp;
            }
        }
        final int w = (int) (right - left + 1);
        left += centerX;

        return new Rectangle((int) left, (int) top, w, h);
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
                               (int)center.getX(), (int)center.getY(),
                               srcX, srcY, srcWidth, srcHeight,
                               dstX, dstY, dstWidth, dstHeight,
                               srcPixelStride, dstPixelStride,
                               srcBandOffsets[0], dstBandOffsets[0],
                               srcScanlineStride, dstScanlineStride,
                               distModel.ordinal(), distTerms);
            }
        }
        else if (src.getNumBands() == 3) {
            if (shouldUseLensfun) {
                synchronized(this) {
                    distortionColorLF(srcData, dstData,
                            fullWidth, fullHeight,
                            (int)center.getX(), (int)center.getY(),
                            srcX, srcY, srcWidth, srcHeight,
                            dstX, dstY, dstWidth, dstHeight,
                            srcPixelStride, dstPixelStride,
                            srcBandOffsets[0], srcBandOffsets[1], srcBandOffsets[2],
                            dstBandOffsets[0], dstBandOffsets[1], dstBandOffsets[2],
                            srcScanlineStride, dstScanlineStride,
                            cameraMaker, cameraModel, lensMaker, lensModel, focal, aperture);
                }
            }
            else {
                synchronized (this) {
                distortionColor(srcData, dstData,
                        fullWidth, fullHeight,
                        (int)center.getX(), (int)center.getY(),
                        srcX, srcY, srcWidth, srcHeight,
                        dstX, dstY, dstWidth, dstHeight,
                        srcPixelStride, dstPixelStride,
                        srcBandOffsets[0], srcBandOffsets[1], srcBandOffsets[2],
                        dstBandOffsets[0], dstBandOffsets[1], dstBandOffsets[2],
                        srcScanlineStride, dstScanlineStride,
                        distModel.ordinal(), distTerms, tcaTerms);
                }
            }
        }
    }

    static native void distortionMono(short srcData[], short dstData[],
                                      int fullWidth, int fullHeight,
                                      int centerX, int centerY,
                                      int srcRectX, int srcRectY,
                                      int srcRectWidth, int srcRectHeight,
                                      int dstRectX, int dstRectY,
                                      int dstRectWidth, int dstRectHeight,
                                      int srcPixelStride, int dstPixelStride,
                                      int srcOffset, int dstOffset,
                                      int srcLineStride, int dstLineStride,
                                      int distModel, float[] distTerms);

    static native void distortionColor(short srcData[], short dstData[],
                                       int fullWidth, int fullHeight,
                                       int centerX, int centerY,
                                       int srcRectX, int srcRectY,
                                       int srcRectWidth, int srcRectHeight,
                                       int dstRectX, int dstRectY,
                                       int dstRectWidth, int dstRectHeight,
                                       int srcPixelStride, int dstPixelStride,
                                       int srcROffset, int srcGOffset, int srcBOffset,
                                       int dstROffset, int dstGOffset, int dstBOffset,
                                       int srcLineStride, int dstLineStride,
                                       int distModel, float[] distTerms,
                                       float[] tcaTerms);

    static native void distortionColorLF(short srcData[], short dstData[],
                                         int fullWidth, int fullHeight,
                                         int centerX, int centerY,
                                         int srcRectX, int srcRectY,
                                         int srcRectWidth, int srcRectHeight,
                                         int dstRectX, int dstRectY,
                                         int dstRectWidth, int dstRectHeight,
                                         int srcPixelStride, int dstPixelStride,
                                         int srcROffset, int srcGOffset, int srcBOffset,
                                         int dstROffset, int dstGOffset, int dstBOffset,
                                         int srcLineStride, int dstLineStride,
                                         String cameraMaker, String cameraModel,
                                         String lensMaker, String lensModel,
                                         float focal, float aperture);

    static native int[] backwardMapRectLF(int fullWidth, int fullHeight,
                                         int centerX, int centerY,
                                         int dstRectX, int dstRectY,
                                         int dstRectWidth, int dstRectHeight,
                                         String cameraMaker, String cameraModel,
                                         String lensMaker, String lensModel,
                                         float focal, float aperture);
}
