/* Copyright (C) 2017- Masahiro Kitagawa */

package com.lightcrafts.utils;

import com.lightcrafts.platform.Platform;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

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

    private static String pathName = "";
    static {
        if (!Platform.isLinux()) {
            final String baseDirName = System.getProperty("lensfun.dir");
            try {
                pathName = Files.find(Paths.get(baseDirName), 1,
                        (path, attr) -> attr.isDirectory() && path.getFileName().toString().startsWith("version_"))
                        .findFirst()
                        .orElse(Path.of(""))
                        .toString();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    private long _handle = init(pathName);

    @Getter (lazy = true)
    private final List<String> allCameraNames = Arrays.asList(getCameraNames(_handle));

    @Getter (lazy = true)
    private final List<String> allLensNames = Arrays.asList(getLensNames(_handle));

    public static Lensfun updateInstance(
            String cameraMaker, String cameraModel,
            String lensMaker, String lensModel,
            float focal, float aperture) {
        // We don't check cameraMaker
        if (instance == null
                || lensMaker.isEmpty()
                || !cameraModel.equals(instance.cameraModel)
                || !lensModel.equals(instance.lensModel)
                || instance.focal != focal
                || instance.aperture != aperture) {
            instance = new Lensfun(cameraMaker, cameraModel,
                    lensMaker, lensModel, focal, aperture);
        }
        return instance;
    }

    public void dispose() {
        destroy(_handle);
        instance = null;
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

    private native long init(String path);
    private native void destroy(long handle);

    private native String[] getCameraNames(long lfHandle);
    private native String[] getLensNames(long lfHandle);
    private native String[] getLensNamesForCamera(
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
            short[] srcData, short[] dstData,
            int srcRectX, int srcRectY, int srcRectWidth, int srcRectHeight,
            int dstRectX, int dstRectY, int dstRectWidth, int dstRectHeight,
            int srcPixelStride, int dstPixelStride,
            int srcROffset, int srcGOffset, int srcBOffset,
            int dstROffset, int dstGOffset, int dstBOffset,
            int srcLineStride, int dstLineStride) {
        distortionColor(
                _handle,
                srcData, dstData,
                srcRectX, srcRectY, srcRectWidth, srcRectHeight,
                dstRectX, dstRectY, dstRectWidth, dstRectHeight,
                srcPixelStride, dstPixelStride,
                srcROffset, srcGOffset, srcBOffset,
                dstROffset, dstGOffset, dstBOffset,
                srcLineStride, dstLineStride);
    }

    public Rectangle backwardMapRect(Rectangle destRect) {
        final int[] srcRect = backwardMapRect(_handle,
                destRect.x, destRect.y, destRect.width, destRect.height);
        return new Rectangle(srcRect[0], srcRect[1], srcRect[2], srcRect[3]);
    }

    private native void distortionColor(
            long lfHandle,
            short[] srcData, short[] dstData,
            int srcRectX, int srcRectY, int srcRectWidth, int srcRectHeight,
            int dstRectX, int dstRectY, int dstRectWidth, int dstRectHeight,
            int srcPixelStride, int dstPixelStride,
            int srcROffset, int srcGOffset, int srcBOffset,
            int dstROffset, int dstGOffset, int dstBOffset,
            int srcLineStride, int dstLineStride);

    private native int[] backwardMapRect(
            long lfHandle,
            int dstRectX, int dstRectY, int dstRectWidth, int dstRectHeight);
}
