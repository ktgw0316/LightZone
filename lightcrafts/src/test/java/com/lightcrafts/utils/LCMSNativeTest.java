package com.lightcrafts.utils;

import org.junit.jupiter.api.Test;

import java.awt.color.ColorSpace;
import java.awt.color.ICC_Profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LCMSNativeTest {

    @Test
    void createsProfilesTransformsPixelsAndReleasesNativeHandles() {
        final long profile =
                LCMSNative.cmsCreateRGBProfile(
                        new double[] {0.3127, 0.3290, 1.0},
                        new double[] {
                            0.64, 0.33, 1.0,
                            0.30, 0.60, 1.0,
                            0.15, 0.06, 1.0
                        },
                        2.2);
        assertNotEquals(0, profile);

        final long labProfile = LCMSNative.cmsCreateLab2Profile();
        assertNotEquals(0, labProfile);

        final byte[] profileData = ICC_Profile.getInstance(ColorSpace.CS_sRGB).getData();
        final long openedProfile =
                LCMSNative.cmsOpenProfileFromMem(profileData, profileData.length);
        assertNotEquals(0, openedProfile);
        assertTrue(LCMSNative.cmsCloseProfile(openedProfile));

        final long transform =
                LCMSNative.cmsCreateTransform(
                        profile, LCMS.TYPE_RGB_8,
                        profile, LCMS.TYPE_RGB_8,
                        LCMS.INTENT_PERCEPTUAL, 0);
        assertNotEquals(0, transform);

        final byte[] inputBytes = {(byte) 32, (byte) 128, (byte) 224};
        final byte[] outputBytes = new byte[inputBytes.length];
        LCMSNative.cmsDoTransform(transform, inputBytes, outputBytes, 1);
        for (int i = 0; i < inputBytes.length; i++) {
            assertEquals(inputBytes[i] & 0xff, outputBytes[i] & 0xff, 2);
        }
        final byte[] inPlaceBytes = inputBytes.clone();
        LCMSNative.cmsDoTransform(transform, inPlaceBytes, inPlaceBytes, 1);
        for (int i = 0; i < inputBytes.length; i++) {
            assertEquals(inputBytes[i] & 0xff, inPlaceBytes[i] & 0xff, 2);
        }

        LCMSNative.cmsDeleteTransform(transform);

        final long shortTransform =
                LCMSNative.cmsCreateTransform(
                        profile, LCMS.TYPE_RGB_16,
                        profile, LCMS.TYPE_RGB_16,
                        LCMS.INTENT_PERCEPTUAL, 0);
        assertNotEquals(0, shortTransform);

        final short[] inputShorts = {8192, (short) 32768, (short) 57344};
        final short[] outputShorts = new short[inputShorts.length];
        LCMSNative.cmsDoTransform(shortTransform, inputShorts, outputShorts, 1);
        for (int i = 0; i < inputShorts.length; i++) {
            assertEquals(inputShorts[i] & 0xffff, outputShorts[i] & 0xffff, 64);
        }

        LCMSNative.cmsDeleteTransform(shortTransform);

        final int doubleFormat = LCMS.TYPE_RGB_DBL | (1 << 22);
        final long doubleTransform =
                LCMSNative.cmsCreateTransform(
                        profile, doubleFormat,
                        profile, doubleFormat,
                        LCMS.INTENT_PERCEPTUAL, 0);
        assertNotEquals(0, doubleTransform);

        final double[] inputDoubles = {0.125, 0.5, 0.875};
        final double[] outputDoubles = new double[inputDoubles.length];
        LCMSNative.cmsDoTransform(doubleTransform, inputDoubles, outputDoubles, 1);
        for (int i = 0; i < inputDoubles.length; i++) {
            assertEquals(inputDoubles[i], outputDoubles[i], 0.001);
        }
        LCMSNative.cmsDeleteTransform(doubleTransform);

        final double[] noOpInput = {0.125, 0.5, 0.875};
        final double[] noOpOutput = new double[noOpInput.length];
        LCMSNative.cmsDoTransform(0, noOpInput, noOpOutput, 1);
        assertEquals(0, noOpOutput[0]);
        assertEquals(0, noOpOutput[1]);
        assertEquals(0, noOpOutput[2]);

        final long proofingTransform =
                LCMSNative.cmsCreateProofingTransform(
                        profile, LCMS.TYPE_RGB_8,
                        profile, LCMS.TYPE_RGB_8,
                        labProfile, LCMS.INTENT_PERCEPTUAL,
                        LCMS.INTENT_PERCEPTUAL, 0);
        assertNotEquals(0, proofingTransform);
        LCMSNative.cmsDeleteTransform(proofingTransform);

        assertTrue(LCMSNative.cmsCloseProfile(profile));
        assertTrue(LCMSNative.cmsCloseProfile(labProfile));
    }
}
