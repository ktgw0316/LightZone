package com.lightcrafts.utils;

import org.junit.jupiter.api.Test;

import java.awt.Rectangle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LensfunTest {

    @Test
    void loadsCameraAndLensNamesFromTheDatabase() {
        final Lensfun lensfun = newLensfun();
        Lensfun corrected = null;
        try {
            assertFalse(lensfun.getAllCameraNames().isEmpty());
            assertFalse(lensfun.getAllLensNames().isEmpty());

            for (String cameraName : lensfun.getAllCameraNames()) {
                final String[] camera = cameraName.split(": ", 2);
                final var lenses = lensfun.getLensNamesFor(camera[0], camera[1]);
                if (!lenses.isEmpty()) {
                    final String[] lens = lenses.getFirst().split(": ", 2);
                    corrected = Lensfun.updateInstance(
                            camera[0], camera[1], lens[0], lens[1], 35, 2.8f);
                    corrected.updateModifier(32, 32);
                    final Rectangle mapped = corrected.backwardMapRect(new Rectangle(4, 4, 8, 8));
                    assertTrue(mapped.width > 0);
                    assertTrue(mapped.height > 0);
                    return;
                }
            }
            throw new AssertionError("No compatible lens was found for any camera");
        } finally {
            if (corrected != null && corrected != lensfun) {
                corrected.dispose();
            }
            lensfun.dispose();
        }
    }

    @Test
    void mapsRegionsAndAppliesModifierToSixteenBitRgbPixels() {
        final Lensfun lensfun = newLensfun();
        try {
            final int width = 32;
            final int height = 32;
            lensfun.updateModifier(width, height, 0, 0, 0, 0);

            assertEquals(
                    new Rectangle(3, 3, 14, 14),
                    lensfun.backwardMapRect(new Rectangle(4, 4, 8, 8)));

            final int regionWidth = 24;
            final int regionHeight = 24;
            final int sourceLineStride = (width + 4) * 3;
            final short[] source = new short[(height + 4) * sourceLineStride];
            for (int i = 0; i < source.length; i += 3) {
                source[i] = 10000;
                source[i + 1] = 20000;
                source[i + 2] = 30000;
            }
            final short[] destination = new short[regionWidth * regionHeight * 3];

            lensfun.distortionColor(
                    source, destination,
                    0, 0, width, height,
                    4, 4, regionWidth, regionHeight,
                    3, 3,
                    0, 1, 2,
                    0, 1, 2,
                    sourceLineStride, regionWidth * 3);

            for (int i = 0; i < destination.length; i += 3) {
                assertEquals(10000, destination[i] & 0xffff, 8);
                assertEquals(20000, destination[i + 1] & 0xffff, 8);
                assertEquals(30000, destination[i + 2] & 0xffff, 8);
            }
        } finally {
            lensfun.dispose();
        }
    }

    private static Lensfun newLensfun() {
        return Lensfun.updateInstance("", "test camera", "", "", 35, 2.8f);
    }
}
