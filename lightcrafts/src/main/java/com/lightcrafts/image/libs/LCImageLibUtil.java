/* Copyright (C) 2018-     Masahiro Kitagawa */

package com.lightcrafts.image.libs;

import java.awt.image.DataBufferByte;
import java.util.Arrays;
import java.util.stream.IntStream;

class LCImageLibUtil {

    static void invert(DataBufferByte db) {
        final byte[] data = db.getData();
        IntStream.range(0, data.length).parallel().forEach(i -> data[i] = (byte) ~data[i]);
    }

    static int[] bandOffset(int bands) {
        return IntStream.range(0, bands).toArray();
    }

    static int min(int... numbers) {
        return Arrays.stream(numbers).min().orElse(numbers[0]);
    }
}
