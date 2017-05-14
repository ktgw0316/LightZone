/* Copyright (C) 2017- Masahiro Kitagawa */

package com.lightcrafts.utils;

import java.util.Arrays;
import java.util.List;

import lombok.Getter;

/**
 * Wrapper for Lensfun C++ library.
 */
public class Lensfun {
    @Getter
    private int[] distModel = {0};

    @Getter
    private float[] distTerms = {0, 0, 0};

    @Getter
    private float[] tcaTerms = {1f, 1f};

    private static List<String> allCameraNames;
    private static List<String> allLensNames;

    public Lensfun() {
        handle = init();
    }

    public Lensfun(String cameraMaker, String cameraModel,
                   String lensMaker, String lensModel,
                   float focal, float aperture) {
        handle = init();
        lensfunTerms(distModel, distTerms, tcaTerms,
                cameraMaker, cameraModel,
                lensMaker, lensModel, focal, aperture);
    }

    @Override
    public void finalize() throws Throwable {
        try {
            super.finalize();
        } finally {
            destroy(handle);
        }
    }

    public synchronized List<String> getAllCameraNames() {
        if (allCameraNames == null) {
            allCameraNames = Arrays.asList(getCameraNames(handle));
        }
        return allCameraNames;
    }

    public synchronized List<String> getAllLensNames() {
        if (allLensNames == null) {
            allLensNames = Arrays.asList(getLensNames(handle));
        }
        return allLensNames;
    }

    public synchronized List<String> getLensNamesFor(
            String cameraMaker, String cameraModel) {
        return Arrays.asList(
                getLensNamesForCamera(handle, cameraMaker, cameraModel));
    }

    public synchronized void initModifier(
            int fullWidth, int fullHeight,
            String cameraMaker, String cameraModel,
            String lensMaker, String lensModel,
            float focal, float aperture) {
        initModifier(handle, fullWidth, fullHeight,
                cameraMaker, cameraModel, lensMaker,
                lensModel, focal, aperture);
    }

    private long handle = 0;

    private native long init();
    private native void destroy(long handle);

    private synchronized static native boolean lensfunTerms(
            int[] ret_distModel, float[] ret_distTerms,
            float[] ret_tcaTerms,
            String cameraMaker, String cameraModel,
            String lensMaker, String lensModel,
            float focal, float aperture);

    private static native String[] getCameraNames(long handle);
    private static native String[] getLensNames(long handle);
    private static native String[] getLensNamesForCamera(
            long handle, String cameraMaker, String cameraModel);

    private native void initModifier(
            long handle, int fullWidth, int fullHeight,
            String cameraMaker, String cameraModel,
            String lensMaker, String lensModel,
            float focal, float aperture);

    private native void applyModifier(
            long handle, int fullWidth, int fullHeight,
            String cameraMaker, String cameraModel,
            String lensMaker, String lensModel,
            float focal, float aperture);
}
