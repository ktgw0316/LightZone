/*
 * $RCSfile: WritableRandomIterFallback.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:55:45 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.iterator;
import java.awt.Rectangle;
import java.awt.image.Raster;
import java.awt.image.WritableRenderedImage;
import com.lightcrafts.mediax.jai.iterator.WritableRandomIter;

/**
 * @since EA2
 */
public final class WritableRandomIterFallback extends RandomIterFallback
    implements WritableRandomIter {

    WritableRenderedImage wim;

    public WritableRandomIterFallback(WritableRenderedImage im,
                                      Rectangle bounds) {
        super(im, bounds);
        this.wim = im;
    }

    private void makeCurrentWritable(int xLocal, int yLocal) {
        int xIDNew = xTiles[xLocal];
        int yIDNew = yTiles[yLocal];

        if ((xIDNew != xID) || (yIDNew != yID) || (dataBuffer == null)) {
            if (dataBuffer != null) {
                wim.releaseWritableTile(xID, yID);
            }
            xID = xIDNew;
            yID = yIDNew;
            Raster tile = wim.getWritableTile(xID, yID);

            this.dataBuffer = tile.getDataBuffer();
            this.sampleModelTranslateX = tile.getSampleModelTranslateX();
            this.sampleModelTranslateY = tile.getSampleModelTranslateY();
        }
    }

    public void setSample(int x, int y, int b, int s) {
        makeCurrentWritable(x - boundsX, y - boundsY);
        sampleModel.setSample(x - sampleModelTranslateX,
                              y - sampleModelTranslateY,
                              b, s,
                              dataBuffer);
    }

    public void setSample(int x, int y, int b, float s) {
        makeCurrentWritable(x - boundsX, y - boundsY);
        sampleModel.setSample(x - sampleModelTranslateX,
                              y - sampleModelTranslateY,
                              b, s,
                              dataBuffer);
    }

    public void setSample(int x, int y, int b, double s) {
        makeCurrentWritable(x - boundsX, y - boundsY);
        sampleModel.setSample(x - sampleModelTranslateX,
                              y - sampleModelTranslateY,
                              b, s,
                              dataBuffer);
    }

    public void setPixel(int x, int y, int[] iArray) {
        makeCurrentWritable(x - boundsX, y - boundsY);
        sampleModel.setPixel(x - sampleModelTranslateX,
                             y - sampleModelTranslateY,
                             iArray,
                             dataBuffer);
    }

    public void setPixel(int x, int y, float[] fArray) {
        makeCurrentWritable(x - boundsX, y - boundsY);
        sampleModel.setPixel(x - sampleModelTranslateX,
                             y - sampleModelTranslateY,
                             fArray,
                             dataBuffer);
    }

    public void setPixel(int x, int y, double[] dArray) {
        makeCurrentWritable(x - boundsX, y - boundsY);
        sampleModel.setPixel(x - sampleModelTranslateX,
                             y - sampleModelTranslateY,
                             dArray,
                             dataBuffer);
    }

    public void done() {
        if (dataBuffer != null) {
            wim.releaseWritableTile(xID, yID);
        }
        dataBuffer = null;
    }
}
