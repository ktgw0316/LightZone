/* Copyright (C) 2016 Masahiro Kitagawa */

package com.lightcrafts.utils;

public class LCMSNative {

// Native LCMS API Functions
// NOTE: LCMS doesn't seem to be properly reentrant, make all native calls synchronized

    synchronized native static long cmsCreateProofingTransform(long inputProfile, int inputFormat,
                                                               long outputProfile, int outputFormat,
                                                               long proofingProfile,
                                                               int Intent, int ProofingIntent, int dwFlags);

    protected synchronized native static long cmsCreateLab2Profile();

    protected synchronized native static long cmsOpenProfileFromMem(byte[] data, int size);

    protected synchronized native static long cmsCreateRGBProfile(double[] WhitePoint,
                                                                  double[] Primaries,
                                                                  double gamma);

    protected synchronized native static boolean cmsCloseProfile(long hProfile);

    synchronized native static long cmsCreateTransform(long inputProfile, int inputFormat,
                                                       long outputProfile, int outputFormat,
                                                       int intent, int flags);

    synchronized native static void cmsDeleteTransform(long hTransform);

    native static void cmsDoTransform(long hTransform, byte[] InputBuffer, byte[] OutputBuffer, int size);

    native static void cmsDoTransform(long hTransform, short[] InputBuffer, short[] OutputBuffer, int size);

    native static void cmsDoTransform(long hTransform, double[] InputBuffer, double[] OutputBuffer, int size);

    static {
        System.loadLibrary("LCLCMS");
    }

}
