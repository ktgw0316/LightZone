package com.lightcrafts.utils.raw;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class DCRawInterpolationTest {

    @Test
    void interpolatesGreenAndRedBlueChannelsWithPaddedRows() {
        final int width = 8;
        final int height = 8;
        final int sourceStride = 10;
        final int destinationStride = 10;
        final short[] source = new short[sourceStride * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                source[y * sourceStride + x] = (short) ((x * 37 + y * 101 + 91) % 4096);
            }
        }
        final short[] originalSource = source.clone();
        final short[] destination = new short[destinationStride * height * 3];
        Arrays.fill(destination, (short) 173);

        DCRaw.interpolateGreen(
                source, destination, width, height,
                sourceStride, destinationStride,
                3, 0, 1, 2, 0, 0, 0);
        DCRaw.interpolateRedBlue(
                destination, width, height, destinationStride,
                0, 1, 2, 1, 0, 0, 1);

        assertEquals(-1421294335, Arrays.hashCode(destination));
        assertArrayEquals(originalSource, source);
    }
}
