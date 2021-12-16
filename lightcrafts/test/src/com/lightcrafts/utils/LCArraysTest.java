/* Copyright (C) 2021-     Masahiro Kitagawa */

package com.lightcrafts.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class LCArraysTest {
    private final byte[] byteArray = {1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0};
    private final int[] intArray = {1, 1, 1, 1};
    private final short[] shortArray = {1, 0, 1, 0, 1, 0, 1, 0};

    @Test
    public void testCopyIntArrayToByteArray() {
        final int length = intArray.length * 4;
        var dstByteArray = new byte[length];
        LCArrays.copy(intArray, 0, dstByteArray, 0, length);
        assertThat(dstByteArray).isEqualTo(byteArray);
    }

    @Test
    public void testCopyShortArrayToByteArray() {
        final int length = shortArray.length * 2;
        var dstByteArray = new byte[length];
        LCArrays.copy(shortArray, 0, dstByteArray, 0, length);
        assertThat(dstByteArray).isEqualTo(byteArray);
    }

    @Test
    public void testCopyByteArrayToIntArray() {
        final int length = byteArray.length;
        var dstIntArray = new int[length / 4];
        LCArrays.copy(byteArray, 0, dstIntArray, 0, length);
        assertThat(dstIntArray).isEqualTo(intArray);
    }

    @Test
    public void testCopyByteArrayToShortArray() {
        final int length = byteArray.length;
        var dstShortArray = new short[length / 2];
        LCArrays.copy(byteArray, 0, dstShortArray, 0, length);
        assertThat(dstShortArray).isEqualTo(shortArray);
    }
}
/* vim:set et sw=4 ts=4: */
