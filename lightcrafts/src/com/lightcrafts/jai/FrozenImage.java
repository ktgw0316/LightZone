/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.jai;

import javax.media.jai.PlanarImage;
import javax.media.jai.ImageLayout;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.*;

public class FrozenImage extends PlanarImage {
    static ImageLayout getsRGBImageLayout(RenderedImage image) {
        ImageLayout layout = new ImageLayout(image);
        layout.setColorModel(new ComponentColorModel(JAIContext.sRGBColorSpace, false, false,
                                                     Transparency.OPAQUE, DataBuffer.TYPE_BYTE));
        return layout;
    }

    private Raster raster[][];

    public FrozenImage(RenderedImage image) {
        this(image, false);
    }

    public FrozenImage(RenderedImage image, boolean castTosRGB) {
        super(castTosRGB ? getsRGBImageLayout(image) : new ImageLayout(image), null, null);
        raster = new Raster[getNumXTiles()][getNumYTiles()];
        for (int x = 0; x < getNumXTiles(); x++)
            for (int y = 0; y < getNumYTiles(); y++)
                raster[x][y] = image.getTile(x, y);
    }

    public Raster getTile(int tileX, int tileY) {
        return raster[tileX][tileY];
    }
}


