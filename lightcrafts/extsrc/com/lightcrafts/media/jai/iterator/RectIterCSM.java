/*
 * $RCSfile: RectIterCSM.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:55:43 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.iterator;
import java.awt.Rectangle;
import java.awt.image.ComponentSampleModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import com.lightcrafts.mediax.jai.PlanarImage;

/**
 */
public abstract class RectIterCSM extends RectIterFallback {

    protected int[] bankIndices;
    protected int scanlineStride;
    protected int pixelStride;
    protected int[] bandOffsets;
    protected int[] DBOffsets;

    protected int offset;
    protected int bandOffset;

    public RectIterCSM(RenderedImage im, Rectangle bounds) {
        super(im, bounds);

        ComponentSampleModel csm = (ComponentSampleModel)sampleModel;
        
        this.scanlineStride = csm.getScanlineStride();
        this.pixelStride = csm.getPixelStride();
        this.bankIndices = csm.getBankIndices();
        int[] bo = csm.getBandOffsets();

        this.bandOffsets = new int[numBands + 1];
        for (int i = 0; i < numBands; i++) {
            bandOffsets[i] = bo[i];
        }
        bandOffsets[numBands] = 0;

        this.DBOffsets = new int[numBands];

        this.offset = (y - sampleModelTranslateY)*scanlineStride +
            (x - sampleModelTranslateX)*pixelStride;
        this.bandOffset = bandOffsets[0];
    }

    protected void dataBufferChanged() {}

    protected void adjustBandOffsets() {
        int[] newDBOffsets = dataBuffer.getOffsets();
        for (int i = 0; i < numBands; i++) {
            int bankNum = bankIndices[i];
            bandOffsets[i] += newDBOffsets[bankNum] - DBOffsets[bankNum];
        }
        this.DBOffsets = newDBOffsets;
    }

    protected void setDataBuffer() {
        Raster tile = im.getTile(tileX, tileY);
        this.dataBuffer = tile.getDataBuffer();
        dataBufferChanged();

        int newSampleModelTranslateX = tile.getSampleModelTranslateX();
        int newSampleModelTranslateY = tile.getSampleModelTranslateY();

        int deltaX = sampleModelTranslateX - newSampleModelTranslateX;
        int deltaY = sampleModelTranslateY - newSampleModelTranslateY;

        offset += deltaY*scanlineStride + deltaX*pixelStride;

        this.sampleModelTranslateX = newSampleModelTranslateX;
        this.sampleModelTranslateY = newSampleModelTranslateY;
    }

    public void startLines() {
        offset += (bounds.y - y)*scanlineStride;
        y = bounds.y;

        tileY = startTileY;
        setTileYBounds();
        setDataBuffer();
    }

    public void nextLine() {
        ++y;
        offset += scanlineStride;
    }

    public void jumpLines(int num) {
        int jumpY = y + num;
        if (jumpY < bounds.y || jumpY > lastY) {
            // Jumped outside the image.
            throw new IndexOutOfBoundsException(JaiI18N.getString("RectIterFallback1"));
        }

        y = jumpY;
        offset += num*scanlineStride;

        if (y < prevYBoundary || y > nextYBoundary) {
            this.tileY = PlanarImage.YToTileY(y,
                                              tileGridYOffset,
                                              tileHeight);
            setTileYBounds();
            setDataBuffer();
        }
    }

    public void startPixels() {
        offset += (bounds.x - x)*pixelStride;
        x = bounds.x;

        tileX = startTileX;
        setTileXBounds();
        setDataBuffer();
    }

    public void nextPixel() {
        ++x;
        offset += pixelStride;
    }

    public void jumpPixels(int num) {
        int jumpX = x + num;
        if (jumpX < bounds.x || jumpX > lastX) {
            // Jumped outside the image.
            throw new IndexOutOfBoundsException(JaiI18N.getString("RectIterFallback0"));
        }

        x = jumpX;
        offset += num*pixelStride;

        if (x < prevXBoundary || x > nextXBoundary) {
            this.tileX = PlanarImage.XToTileX(x,
                                              tileGridXOffset,
                                              tileWidth);
            
            setTileXBounds();
            setDataBuffer();
        }
    }

    public void startBands() {
        b = 0;
        bandOffset = bandOffsets[0];
    }

    public void nextBand() {
        ++b;
        bandOffset = bandOffsets[b];
    }
}
