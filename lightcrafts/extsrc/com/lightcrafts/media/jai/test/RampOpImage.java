/*
 * $RCSfile: RampOpImage.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.2 $
 * $Date: 2005/02/24 02:07:44 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.test;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.util.Map;
import com.lightcrafts.mediax.jai.ImageLayout;
import com.lightcrafts.mediax.jai.RasterFactory;
import com.lightcrafts.mediax.jai.SourcelessOpImage;

/** Defines a ramp image for testing purpose. */
final class RampOpImage extends SourcelessOpImage {

    public RampOpImage(int minX, int minY, int width, int height, 
                       SampleModel sampleModel,
                       Map configuration, ImageLayout layout) {
        super(layout, configuration, sampleModel, minX, minY, width, height);
    }

    public Raster computeTile(int tileX, int tileY) {
        int orgX = tileXToX(tileX);
        int orgY = tileYToY(tileY);

        WritableRaster dst = RasterFactory.createWritableRaster(
            sampleModel, new Point(orgX, orgY));

        Rectangle rect = new Rectangle(orgX, orgY,
                                       sampleModel.getWidth(),
                                       sampleModel.getHeight());
        rect = rect.intersection(getBounds());

        int numBands = sampleModel.getNumBands();
        int p[] = new int[numBands];

        for (int y = rect.y; y < (rect.y + rect.height); y++) {
            for (int x = rect.x; x < (rect.x + rect.width); x++) {
                int value = Math.max(x & 0xFF, y & 0xFF);
                for (int i = 0; i < numBands; i++) {
                    p[i] = value;
                }
                dst.setPixel(x, y, p);
            }
        }
        return dst;
    }
}
