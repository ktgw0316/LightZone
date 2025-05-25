package com.lightcrafts.jai.opimage;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class BilateralFilterOpImageTest {
    private final int width = 50;
    private final int height = 50;
    private final int windowRadius = 2;
    private final int windowSize = 2 * windowRadius + 1;
    private final float scaleR = 0.1f;

    private float[] kernel;
    private short[] srcData;
    private short[] dstData;

    @BeforeAll
    static void setUpBeforeClass() {
        System.loadLibrary("FASTJAI");
    }

    @BeforeEach
    void setUp() {
        kernel = new float[windowSize];
        for (int i = -windowRadius; i <= windowRadius; i++) {
            kernel[windowRadius + i] = (float) (1.0 / (2.0 * 4.0) * i * i + 0.25);
        }

        srcData = new short[width * height];
        dstData = new short[width * height];
    }

    private void verifyAllValues(short expectedValue, String message) {
        for (int y = 0; y < height - 2 * windowRadius; y++) {
            for (int x = 0; x < width - 2 * windowRadius; x++) {
                int index = y * width + x;
                assertEquals(expectedValue & 0xFFFF, dstData[index] & 0xFFFF,
                        "Value at position (" + x + "," + y + ") differs from expected value");
            }
        }
    }

    private void applyFilter() {
        BilateralFilterOpImage.bilateralFilterMonoRLM(
            srcData, dstData,
            windowRadius, windowSize, scaleR, kernel,
            width, height,
            1, 1, // pixel stride
            0, 0, // offset
            width, width // line stride
        );
    }

    @Test
    void bilateralFilterMonoRLM_uniformValues() {
        final short uniformValue = 16384; // near half of 65535
        Arrays.fill(srcData, uniformValue);

        applyFilter();

        verifyAllValues(uniformValue, "");
    }

    @Test
    void bilateralFilterMonoRLM_extremeValues() {
        // Case 1: minimum value (0)
        Arrays.fill(srcData, (short) 0);
        applyFilter();
        verifyAllValues((short) 0, "For minimum value (0), ");

        // Case 2: maximum value (65535)
        Arrays.fill(srcData, (short) 65535);
        applyFilter();
        verifyAllValues((short) 65535, "For maximum value (65535), ");
    }
}