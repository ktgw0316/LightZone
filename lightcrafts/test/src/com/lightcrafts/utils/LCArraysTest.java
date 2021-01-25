/* Copyright (C) 2021-     Masahiro Kitagawa */

package com.lightcrafts.utils;

import static org.junit.Assert.assertArrayEquals;

public class LCArraysTest {
    private final byte[] byteArray = {1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0};
    private final int[] intArray = {0x1000000, 0, 0x1000000, 0};
    private final short[] shortArray = {0x100, 0, 0, 0, 0x100, 0, 0, 0};

    @org.junit.Test
    public void testCopyIntArrayToByteArray() {
        final int length = intArray.length * 4;
        var dstByteArray = new byte[length];
        LCArrays.copy(intArray, 0, dstByteArray, 0, length);
        assertArrayEquals(dstByteArray, byteArray);
    }

    @org.junit.Test
    public void testCopyShortArrayToByteArray() {
        final int length = intArray.length * 2;
        var dstByteArray = new byte[length];
        LCArrays.copy(shortArray, 0, dstByteArray, 0, length);
        assertArrayEquals(dstByteArray, byteArray);
    }

    @org.junit.Test
    public void testCopyByteArrayToIntArray() {
        final int length = byteArray.length;
        var dstIntArray = new int[length];
        LCArrays.copy(byteArray, 0, dstIntArray, 0, length);
        assertArrayEquals(dstIntArray, intArray);
    }

    @org.junit.Test
    public void testCopyByteArrayToShortArray() {
        final int length = byteArray.length;
        var dstShortArray = new short[length];
        LCArrays.copy(byteArray, 0, dstShortArray, 0, length);
        assertArrayEquals(dstShortArray, shortArray);
    }

    @org.junit.Test
    public void testResize() {
        assertArrayEquals((short[]) LCArrays.resize(shortArray, 4),
                new short[]{0x100, 0, 0, 0});
        // TODO
    }
}