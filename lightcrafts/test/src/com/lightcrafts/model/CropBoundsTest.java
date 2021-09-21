/* Copyright (C) 2017-     Masahiro Kitagawa */

package com.lightcrafts.model;

import org.junit.jupiter.api.Test;

import java.awt.Dimension;
import java.awt.geom.Point2D;

import static org.assertj.core.api.Assertions.assertThat;

public class CropBoundsTest {
    @Test
    public void createInvertedAspect() {
        final CropBounds rectangle = new CropBounds(
                new Point2D.Float(50f, 50f), 200, 100, 0.5);
        final CropBounds rectangleInv1 = rectangle.createInvertedAspect();
        final CropBounds rectangleInv2 = rectangleInv1.createInvertedAspect();
        final CropBounds rectangleInv3 = rectangleInv2.createInvertedAspect();
        assertThat(rectangle).isEqualTo(rectangleInv2);
        assertThat(rectangleInv1).isEqualTo(rectangleInv3);

        final CropBounds angleOnly = new CropBounds(0.5);
        final CropBounds angleOnlyInv1 = angleOnly.createInvertedAspect();
        final CropBounds angleOnlyInv2 = angleOnlyInv1.createInvertedAspect();
        final CropBounds angleOnlyInv3 = angleOnlyInv2.createInvertedAspect();
        assertThat(angleOnly).isEqualTo(angleOnlyInv2);
        assertThat(angleOnlyInv1).isEqualTo(angleOnlyInv3);

        assertThat(angleOnly).isEqualTo(angleOnlyInv1); // TODO: Should we accept this?
    }

    @Test
    public void getDimensionToFit() {
        // issue204
        final Dimension D1440x2560 = new Dimension(1440, 2560);
        final Dimension D2560x2560 = new Dimension(2560, 2560);
        final Dimension D3455x3455 = new Dimension(3455, 3455);

        CropBounds crop = new CropBounds(
                new Point2D.Double(1311.9538, 1727.2177),
                1922.4469, 3416.3966, -0.020157604);
        assertThat(D1440x2560).isEqualTo(crop.getDimensionToFit(D1440x2560));
        assertThat(D1440x2560).isEqualTo(crop.getDimensionToFit(D2560x2560));

        crop = new CropBounds(
                new Point2D.Double(2489.3443, 1728.1078),
                3455.0741, 3455.0741, 0);
        assertThat(D3455x3455).isEqualTo(crop.getDimensionToFit(D3455x3455));

        crop = new CropBounds(
                new Point2D.Double(2554.8590, 1728.0603),
                3455.5752, 3455.5752, 0);
        assertThat(D3455x3455).isEqualTo(crop.getDimensionToFit(D3455x3455));

        crop = new CropBounds(
                new Point2D.Double(0, 0), 0, 0, 0);
        assertThat(D3455x3455).isEqualTo(crop.getDimensionToFit(D3455x3455));
    }
}