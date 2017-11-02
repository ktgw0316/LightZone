/* Copyright (C) 2017-     Masahiro Kitagawa */

package com.lightcrafts.model;

import org.junit.Test;

import java.awt.Dimension;
import java.awt.geom.Point2D;

import static org.junit.Assert.*;

public class CropBoundsTest {
    @Test
    public void getDimensionToFit() throws Exception {
        // issue204
        final Dimension D1440x2560 = new Dimension(1440, 2560);
        final Dimension D2560x2560 = new Dimension(2560, 2560);
        final Dimension D3455x3455 = new Dimension(3455, 3455);

        CropBounds crop = new CropBounds(
                new Point2D.Double(1311.9538, 1727.2177),
                1922.4469, 3416.3966, -0.020157604);
        assertEquals(D1440x2560, crop.getDimensionToFit(D1440x2560));
        assertEquals(D1440x2560, crop.getDimensionToFit(D2560x2560));

        crop = new CropBounds(
                new Point2D.Double(2489.3443, 1728.1078),
                3455.0741, 3455.0741, 0);
        assertEquals(D3455x3455, crop.getDimensionToFit(D3455x3455));

        crop = new CropBounds(
                new Point2D.Double(2554.8590, 1728.0603),
                3455.5752, 3455.5752, 0);
        assertEquals(D3455x3455, crop.getDimensionToFit(D3455x3455));
    }
}