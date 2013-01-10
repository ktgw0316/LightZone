/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.jai.opimage;

import javax.media.jai.ImageLayout;
import javax.media.jai.PointOpImage;
import com.lightcrafts.jai.utils.Functions;

import java.awt.image.*;
import java.awt.*;
import java.util.Map;

/**
 * Copyright (C) 2007 Light Crafts, Inc.
 * User: fabio
 * Date: Mar 16, 2007
 * Time: 10:32:08 PM
 */

public class UnlicensedOpImage extends PointOpImage {
    public UnlicensedOpImage(RenderedImage source, Map config) {
        super(source, new ImageLayout(source), config, true);
        permitInPlaceOperation();
    }

    protected void computeRect(Raster[] sources,
                               WritableRaster dest,
                               Rectangle destRect) {
        if (dest != sources[0])
            Functions.copyData(dest, sources[0]);

        WritableRaster raster = dest.createWritableTranslatedChild(0, 0);

        int value = raster.getTransferType() == DataBuffer.TYPE_BYTE ? Byte.MAX_VALUE/2 : Short.MAX_VALUE;

        int pixel[] = new int[3];
        int pixlen = pixel.length;

        for (int i = 0; i < raster.getHeight()-1; i++) {

            raster.getPixel(0, i, pixel);

            for (int j = 0; j < pixlen; j++)
                pixel[j] = value;

            raster.setPixel(0, i, pixel);
        }

        for (int i = 0; i < raster.getHeight()-1; i++) {

            raster.getPixel(i, 0, pixel);

            for (int j = 0; j < pixlen; j++)
                pixel[j] = value;

            raster.setPixel(i, 0, pixel);
        }
    }
}
