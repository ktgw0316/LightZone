package com.lightcrafts.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SegmentTest {

    @Test
    void segmentsGrayscaleImageWithoutChangingItsDimensionsOrConstantRegions() {
        final int width = 32;
        final int height = 24;
        final byte[] image = new byte[width * height];
        java.util.Arrays.fill(image, (byte) 96);
        final byte[] original = image.clone();

        final byte[] result = Segment.segmentImage(image, 1, height, width);

        assertEquals(image.length, result.length);
        assertArrayEquals(original, image);
        assertArrayEquals(image, result);
    }

    @Test
    void segmentsRgbImageAndPreservesUniformColor() {
        final int width = 32;
        final int height = 24;
        final byte[] image = new byte[width * height * 3];
        for (int i = 0; i < image.length; i += 3) {
            image[i] = (byte) 210;
            image[i + 1] = 72;
            image[i + 2] = 38;
        }
        final byte[] original = image.clone();

        final byte[] result = Segment.segmentImage(image, 3, height, width);

        assertEquals(image.length, result.length);
        assertArrayEquals(original, image);
        assertArrayEquals(original, result);
    }

    @Test
    void rejectsInvalidImageDimensionsAndChannelCounts() {
        assertThrows(IllegalArgumentException.class,
                () -> Segment.segmentImage(new byte[0], 1, 0, 1));
        assertThrows(IllegalArgumentException.class,
                () -> Segment.segmentImage(new byte[0], 2, 1, 1));
        assertThrows(IllegalArgumentException.class,
                () -> Segment.segmentImage(new byte[1], 3, 1, 1));
        assertThrows(IllegalArgumentException.class,
                () -> Segment.segmentImage(new byte[0], 3, Integer.MAX_VALUE, Integer.MAX_VALUE));
    }
}
