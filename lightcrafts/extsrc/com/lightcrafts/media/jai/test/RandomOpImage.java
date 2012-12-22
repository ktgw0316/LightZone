/*
 * $RCSfile: RandomOpImage.java,v $
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
import java.awt.Frame;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.util.Map;
import com.lightcrafts.mediax.jai.ImageLayout;
import com.lightcrafts.mediax.jai.PlanarImage;
import com.lightcrafts.mediax.jai.RasterFactory;
import com.lightcrafts.mediax.jai.SourcelessOpImage;
import com.lightcrafts.mediax.jai.widget.ScrollingImagePanel;

/** Defines an OpImage with random pixel values for testing purposes. */
final class RandomOpImage extends SourcelessOpImage {

    private int maxValue;
    private int transtype;

    public RandomOpImage(int minX, int minY,
                         int width, int height,
                         SampleModel sampleModel,
                         Map configuration, ImageLayout layout) {
        super(layout, configuration, sampleModel, minX, minY, width, height);

        switch (this.transtype = sampleModel.getTransferType()) {
        case DataBuffer.TYPE_BYTE:
            maxValue = 255;
            break;
        case DataBuffer.TYPE_USHORT:
            maxValue = 65535;
            break;
        case DataBuffer.TYPE_SHORT:
            maxValue = Short.MAX_VALUE;
            break;
        case DataBuffer.TYPE_INT:
            maxValue = Integer.MAX_VALUE;
            break;
        }

        /*
         * Fill in all pixel values so that when other OpImages uses this
         * one for performance calculation it doesn't take away time.
         */
        for (int y = getMinTileY(); y <= getMaxTileY(); y++) {
            for (int x = getMinTileX(); x <= getMaxTileX(); x++) {
                getTile(x, y);
            }
        }
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
                for (int i = 0; i < numBands; i++) {
                    switch ( transtype ) {
                    case DataBuffer.TYPE_BYTE:
                    case DataBuffer.TYPE_USHORT:
                    // For unsigned data, limint the random number to [0, 1]
                    // and the result to [0, MAX_VALUE];
                        p[i] = (int)(maxValue * Math.random());
                        break;
                    default:
                    // For signed data, limint the random number to [-1, 1]
                    // and the result to [MIN_VALUE, MAX_VALUE];
                        p[i] = (int)((maxValue+1.0F) *
                                 (Math.random() - 0.5F) * 2.0F);
                    }
                }
                dst.setPixel(x, y, p);
            }
        }
        return dst;
    }

    public static void main (String args[]) {
        ImageLayout layout = new ImageLayout();
        layout.setTileWidth(64);
        layout.setTileHeight(64);
        layout.setColorModel(OpImageTester.createComponentColorModel());

        PlanarImage image =
            new RandomOpImage(0, 0, 100, 100,
                              RasterFactory.createPixelInterleavedSampleModel(
                                              DataBuffer.TYPE_BYTE, 64, 64, 3),
                              null, layout);

        ScrollingImagePanel panel = new ScrollingImagePanel(image, 120, 120);

        Frame window = new Frame("JAI RandomOpImage Test");
        window.add(panel);
        window.pack();
        window.show();
    }
}
