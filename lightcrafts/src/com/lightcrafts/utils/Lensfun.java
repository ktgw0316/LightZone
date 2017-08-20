/* Copyright (C) 2017- Masahiro Kitagawa */

package com.lightcrafts.utils;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.Arrays;
import java.util.List;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Wrapper for Lensfun C++ library.
 */
@RequiredArgsConstructor (access = AccessLevel.PRIVATE)
public class Lensfun {
    private static Lensfun instance; // singleton

    private final String cameraMaker;
    private final String cameraModel;
    private final String lensMaker;
    private final String lensModel;
    private final float focal;
    private final float aperture;

    private int _fullWidth;
    private int _fullHeight;

    @Getter (lazy = true)
    private static final List<String> allCameraNames = Arrays.asList(getCameraNames());;

    @Getter (lazy = true)
    private static final List<String> allLensNames = Arrays.asList(getLensNames());;

    public static Lensfun updateInstance(
            String cameraMaker, String cameraModel,
            String lensMaker, String lensModel,
            float focal, float aperture) {
        // We don't check cameraMaker and lensMaker
        if (instance == null
                || !cameraModel.equals(instance.cameraModel)
                || !lensModel.equals(instance.lensModel)
                || instance.focal != focal
                || instance.aperture != aperture) {
            instance = new Lensfun(cameraMaker, cameraModel,
                    lensMaker, lensModel, focal, aperture);
        }
        return instance;
    }

    @Override
    public void finalize() throws Throwable {
        try {
            super.finalize();
        } finally {
            destroy(_handle);
        }
    }

    public synchronized List<String> getLensNamesFor(
            String cameraMaker, String cameraModel) {
        return Arrays.asList(
                getLensNamesForCamera(_handle, cameraMaker, cameraModel));
    }

    public synchronized Lensfun updateModifier(int fullWidth, int fullHeight) {
        if (_fullWidth != fullWidth || _fullHeight != fullHeight ) {
            _fullWidth = fullWidth;
            _fullHeight = fullHeight;
            initModifier(_handle, fullWidth, fullHeight,
                    cameraMaker, cameraModel, lensMaker, lensModel,
                    focal, aperture);
        }
        return instance;
    }

    public synchronized Lensfun updateModifier(int fullWidth, int fullHeight,
            float k1, float k2, float kr, float kb) {
        _fullWidth = fullWidth;
        _fullHeight = fullHeight;
        initModifierWithPoly5Lens(_handle, fullWidth, fullHeight,
                k1, k2, kr, kb, focal, aperture);
        return instance;
    }

    private long _handle = init();

    private native long init();
    private native void destroy(long handle);

    private static native String[] getCameraNames();
    private static native String[] getLensNames();
    private static native String[] getLensNamesForCamera(
            long lfHandle, String cameraMaker, String cameraModel);

    private native void initModifier(long lfHandle, int fullWidth, int fullHeight,
                                     String cameraMaker, String cameraModel,
                                     String lensMaker, String lensModel,
                                     float focal, float aperture);

    private native void initModifierWithPoly5Lens(long lfHandle,
                                                  int fullWidth, int fullHeight,
                                                  float k1, float k2, float kr, float kb,
                                                  float focal, float aperture);

    //
    // Used by DistortionOpImage
    //

    public void distortionColor(
            short srcData[], short dstData[],
            int centerX, int centerY,
            int srcRectX, int srcRectY, int srcRectWidth, int srcRectHeight,
            int dstRectX, int dstRectY, int dstRectWidth, int dstRectHeight,
            int srcPixelStride, int dstPixelStride,
            int srcROffset, int srcGOffset, int srcBOffset,
            int dstROffset, int dstGOffset, int dstBOffset,
            int srcLineStride, int dstLineStride) {
        distortionColor(
                _handle,
                srcData, dstData,
                centerX, centerY,
                srcRectX, srcRectY, srcRectWidth, srcRectHeight,
                dstRectX, dstRectY, dstRectWidth, dstRectHeight,
                srcPixelStride, dstPixelStride,
                srcROffset, srcGOffset, srcBOffset,
                dstROffset, dstGOffset, dstBOffset,
                srcLineStride, dstLineStride);
    }

    public Rectangle backwardMapRect(Point2D center, Rectangle destRect) {
        final int[] srcRect = backwardMapRect(_handle,
                (int)center.getX(), (int)center.getY(),
                destRect.x, destRect.y, destRect.width, destRect.height);
        return new Rectangle(srcRect[0], srcRect[1], srcRect[2], srcRect[3]);
    }

    private native void distortionColor(
            long lfHandle,
            short[] srcData, short[] dstData,
            int centerX, int centerY,
            int srcRectX, int srcRectY, int srcRectWidth, int srcRectHeight,
            int dstRectX, int dstRectY, int dstRectWidth, int dstRectHeight,
            int srcPixelStride, int dstPixelStride,
            int srcROffset, int srcGOffset, int srcBOffset,
            int dstROffset, int dstGOffset, int dstBOffset,
            int srcLineStride, int dstLineStride);

    private native int[] backwardMapRect(
            long lfHandle,
            int centerX, int centerY,
            int dstRectX, int dstRectY, int dstRectWidth, int dstRectHeight);
}
