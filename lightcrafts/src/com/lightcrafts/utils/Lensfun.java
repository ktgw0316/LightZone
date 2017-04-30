/* Copyright (C) 2017- Masahiro Kitagawa */

package com.lightcrafts.utils;

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

    public Lensfun(String cameraMaker, String cameraModel,
                   String lensName, float focal, float aperture) {
        lensfunTerms(distModel, distTerms, tcaTerms,
                cameraMaker, cameraModel, lensName, focal, aperture);
    }

    synchronized static native boolean lensfunTerms(int[] ret_distModel, float[] ret_distTerms,
                                       float[] ret_tcaTerms,
                                       String cameraMaker, String cameraModel,
                                       String lensName, float focal, float aperture);
}
