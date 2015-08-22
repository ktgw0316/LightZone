/* Copyright (C) 2015 Masahiro Kitagawa */

package com.lightcrafts.jai.opimage;

import com.lightcrafts.mediax.jai.BorderExtender;
import com.lightcrafts.mediax.jai.GeometricOpImage;
import com.lightcrafts.mediax.jai.Interpolation;
import com.lightcrafts.mediax.jai.OpImage;
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

    public DistortionOpImage(RenderedImage sources, Map configuration, BorderExtender extender,
                             float k1, float k2, float kr, float kb) {
        super(OpImage.vectorize(sources), null, configuration, true, extender,
                Interpolation.getInstance(Interpolation.INTERP_BILINEAR));

        fullWidth  = sources.getWidth();
        fullHeight = sources.getHeight();
        this.k1 = k1;
        this.k2 = k2;
        this.kr = kr;
        this.kb = kb;
    }

    public DistortionOpImage(RenderedImage sources, Map configuration, BorderExtender extender,
                             String cameraMaker, String cameraModel,
                             String lensName, float focal, float aperture) {
        super(OpImage.vectorize(sources), null, configuration, true, extender,
                Interpolation.getInstance(Interpolation.INTERP_BILINEAR));

        fullWidth  = sources.getWidth();
        fullHeight = sources.getHeight();
        this.cameraMaker = cameraMaker;
        this.cameraModel = cameraModel;
        this.lensName = lensName;
        this.focal = focal;
        this.aperture = aperture;
    }

    @Override
    protected Rectangle forwardMapRect(Rectangle sourceRect, int sourceIndex) {
        if (sourceIndex != 0)
            return null;
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected Rectangle backwardMapRect(Rectangle destRect, int sourceIndex) {
        if (sourceIndex != 0)
            return null;
        // TODO Auto-generated method stub
        return null;
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

        short dstData[] = dstDataArrays[0];
        short srcData[] = srcDataArrays[0];

        System.out.println("srcPixelStride = " + srcPixelStride); // DEBUG
        System.out.println("srcLineStride  = " + srcScanlineStride); // DEBUG
        System.out.println("dstWidth  = " + dstWidth); // DEBUG
        System.out.println("dstHeight = " + dstHeight); // DEBUG

        if (src.getNumBands() == 1) {
            System.out.println("srcBandOffsets = " + srcBandOffsets[0]); // DEBUG

            synchronized(this) {
                distortionMono(srcData, dstData,
                               fullWidth, fullHeight,
                               dstX, dstY, dstWidth, dstHeight,
                               srcPixelStride, dstPixelStride,
                               srcBandOffsets[0], dstBandOffsets[0],
                               srcScanlineStride, dstScanlineStride, k1, k2);
            }
        }
        else if (src.getNumBands() == 3) {
            System.out.println("srcBandOffsets = " + srcBandOffsets[0]
                    + ", " + srcBandOffsets[1] + ", " + srcBandOffsets[2]); // DEBUG

            if (cameraModel.isEmpty() && lensName.isEmpty()) {
                synchronized(this) {
                    distortionColor(srcData, dstData,
                                    fullWidth, fullHeight,
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
                                      int rectX, int rectY, int rectWidth, int rectHeight,
                                      int srcPixelStride, int dstPixelStride,
                                      int srcOffset, int dstOffset,
                                      int srcLineStride, int dstLineStride,
                                      float k1, float k2);

    static native void distortionColor(short srcData[], short dstData[],
                                       int fullWidth, int fullHeight,
                                       int rectX, int rectY, int rectWidth, int rectHeight,
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
