/*
 * $RCSfile: CheckerboardOpImage.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.2 $
 * $Date: 2005/02/24 02:07:43 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.test;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.util.Map;
import com.lightcrafts.mediax.jai.ImageLayout;
import com.lightcrafts.mediax.jai.SourcelessOpImage;
import com.lightcrafts.mediax.jai.RasterFactory;

/** Defines a checkerboard image for testing purpose. */
final class CheckerboardOpImage extends SourcelessOpImage {

    private int increment;

    private int checkerSize;

    private int numColors;

    /** Defines a checkerboard image of several grey shades. */
    public CheckerboardOpImage(int minX, int minY,
                               int width, int height,
                               SampleModel sampleModel,
                               Map configuration,
                               ImageLayout layout,
                               int checkerSize,
                               int numColors) {
        super(layout, configuration, sampleModel, minX, minY, width, height);

        if (numColors < 2) {
           numColors = 2;
        }
        this.checkerSize = checkerSize;
        this.numColors = numColors;

        switch (sampleModel.getTransferType()) {
        case DataBuffer.TYPE_BYTE:
            increment = 255 / (numColors - 1);
            break;
        case DataBuffer.TYPE_USHORT:
            increment = 65535 / (numColors - 1);
            break;
        case DataBuffer.TYPE_SHORT:
            increment = Short.MAX_VALUE / (numColors - 1);
            break;
        case DataBuffer.TYPE_INT:
            increment = Integer.MAX_VALUE / (numColors - 1);
            break;
        }
    }

    public Raster computeTile(int tileX, int tileY) {
        int orgX = tileXToX(tileX);
        int orgY = tileYToY(tileY);

        WritableRaster dst = 
            RasterFactory.createWritableRaster(
            sampleModel, new Point(orgX, orgY));

        Rectangle rect = new Rectangle(orgX, orgY,
                                       sampleModel.getWidth(),
                                       sampleModel.getHeight());
        rect = rect.intersection(getBounds());

        int numBands = sampleModel.getNumBands();
        int p[] = new int[numBands];

        for (int y = rect.y; y < (rect.y + rect.height); y++) {
            for (int x = rect.x; x < (rect.x + rect.width); x++) {
                int value = getPixelValue(x, y);
                for (int i = 0; i < numBands; i++) {
                    p[i] = value;
                }
                dst.setPixel(x, y, p);
            }
        }
        return dst;
    }

    private int getPixelValue(int x, int y) {
        int squareX = x / checkerSize;
        int squareY = y / checkerSize;
        return (increment * ((squareX + squareY) % numColors));
    }
}
