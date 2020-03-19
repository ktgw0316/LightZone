package com.lightcrafts.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PreviewTest {

    @Test
    public void calcZone() {
        final double delta = 0.1;
        assertEquals( 0., Preview.calcZone(  0), delta);
        assertEquals( 1., Preview.calcZone(  1.4 - 1), delta);
        assertEquals( 2., Preview.calcZone(  2   - 1), delta);
        assertEquals( 3., Preview.calcZone(  2.9 - 1), delta);
        assertEquals( 4., Preview.calcZone(  4   - 1), delta);
        assertEquals( 5., Preview.calcZone(  5.8 - 1), delta);
        assertEquals( 6., Preview.calcZone(  8   - 1), delta);
        assertEquals( 7., Preview.calcZone( 11.5 - 1), delta);
        assertEquals( 8., Preview.calcZone( 16 - 1), delta);
        assertEquals( 9., Preview.calcZone( 23 - 1), delta);
        assertEquals(10., Preview.calcZone( 32 - 1), delta);
        assertEquals(11., Preview.calcZone( 46 - 1), delta);
        assertEquals(12., Preview.calcZone( 64 - 1), delta);
        assertEquals(13., Preview.calcZone( 92 - 1), delta);
        assertEquals(14., Preview.calcZone(128 - 1), delta);
        assertEquals(15., Preview.calcZone(184 - 1), delta);
        assertEquals(16., Preview.calcZone(256 - 1), delta);
    }
}