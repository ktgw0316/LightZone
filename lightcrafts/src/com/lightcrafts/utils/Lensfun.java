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

    public Lensfun(String cameraMaker, String cameraModel,
                   String lensMaker, String lensModel,
                   float focal, float aperture) {
        lensfunTerms(distModel, distTerms, tcaTerms,
                cameraMaker, cameraModel,
                lensMaker, lensModel, focal, aperture);
    }

    public synchronized static List<String> getAllCameraNames() {
        if (allCameraNames == null) {
            allCameraNames = Arrays.asList(getCameraNames());
        }
        return allCameraNames;
    }

    public synchronized static List<String> getAllLensNames() {
        if (allLensNames == null) {
            allLensNames = Arrays.asList(getLensNames());
        }
        return allLensNames;
    }

    public synchronized static List<String> getLensNamesFor(
            String cameraMaker, String cameraModel) {
        return Arrays.asList(
                getLensNamesForCamera(cameraMaker, cameraModel));
    }

    private synchronized static native boolean lensfunTerms(
            int[] ret_distModel, float[] ret_distTerms,
            float[] ret_tcaTerms,
            String cameraMaker, String cameraModel,
            String lensMaker, String lensModel,
            float focal, float aperture);

    private static native String[] getCameraNames();
    private static native String[] getLensNames();
    private static native String[] getLensNamesForCamera(
            String cameraMaker, String cameraModel);
}
