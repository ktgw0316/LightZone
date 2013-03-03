/*
 * $RCSfile: RectIterFallback.java,v $
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
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import com.lightcrafts.mediax.jai.PlanarImage;
import com.lightcrafts.mediax.jai.iterator.RectIter;

/**
 * @since EA2
 */
public class RectIterFallback implements RectIter {

    /** The source image. */
    protected RenderedImage im;

    /** The iterator bouning rectangle. */
    protected Rectangle bounds;

    /** The SampleModel for the tiles of the source image. */
    protected SampleModel sampleModel;

    /** The number of bands of the source image. */
    protected int numBands;

    /** The width of tiles in the source image. */
    protected int tileWidth;

    /** The height of tiles in the source image. */
    protected int tileHeight;

    /** The X offset of the source image tile grid. */
    protected int tileGridXOffset;

    /** The Y offset of the source image tile grid. */
    protected int tileGridYOffset;

    /** The tile index of the leftmost column in the iterator's bounds. */ 
    protected int startTileX;

    /** The tile index of the topmost row in the iterator's bounds. */ 
    protected int startTileY;

    /** The (inclusive) smallest X coordinate of the current tile. */
    protected int tileXStart;

    /** The (inclusive) largest X coordinate of the current tile. */
    protected int tileXEnd;

    /** The (inclusive) smallest Y coordinate of the current tile. */
    protected int tileYStart;

    /** The (inclusive) largest Y coordinate of the current tile. */
    protected int tileYEnd;

    /** The next leftwards tile or image boundary. */
    protected int prevXBoundary;

    /** The next rightwards tile or image boundary. */
    protected int nextXBoundary;

    /** The next upwards tile or image boundary. */
    protected int prevYBoundary;

    /** The next downwards tile or image boundary. */
    protected int nextYBoundary;

    /** The X index of the current tile. */
    protected int tileX;

    /** The Y index of the current tile. */
    protected int tileY;

    /** The (inclusive) largest X coordinate of the bounding rectangle. */
    protected int lastX;

    /** The (inclusive) largest Y coordinate of the bounding rectangle. */
    protected int lastY;

    /** The current X pixel position. */
    protected int x;

    /** The current Y pixel position. */
    protected int y;

    /** The current X pixel position minus the sample model translation . */
    protected int localX;

    /** The current Y pixel position minus the sample model translation . */
    protected int localY;

    /** The X translation factor of the SampleModel of the current tile. */
    protected int sampleModelTranslateX = 0;

    /** The X translation factor of the SampleModel of the current tile. */
    protected int sampleModelTranslateY = 0;

    /** The current band offset. */
    protected int b;

    /** The DataBuffer of the current tile. */
    protected DataBuffer dataBuffer = null;

    public RectIterFallback(RenderedImage im, Rectangle bounds) {
        this.im = im;
        this.bounds = bounds;

        this.sampleModel = im.getSampleModel();
        this.numBands = sampleModel.getNumBands();

        this.tileGridXOffset = im.getTileGridXOffset();
        this.tileGridYOffset = im.getTileGridYOffset();
        this.tileWidth = im.getTileWidth();
        this.tileHeight = im.getTileHeight();

        this.startTileX = PlanarImage.XToTileX(bounds.x,
                                               tileGridXOffset,
                                               tileWidth);
        this.startTileY = PlanarImage.YToTileY(bounds.y,
                                               tileGridYOffset,
                                               tileHeight);

        this.tileX = startTileX;
        this.tileY = startTileY;

        this.lastX = bounds.x + bounds.width - 1;
        this.lastY = bounds.y + bounds.height - 1;

        localX = x = bounds.x;
        localY = y = bounds.y;
        b = 0;

        setTileXBounds();
        setTileYBounds();
        setDataBuffer();
    }

    protected final void setTileXBounds() {
        tileXStart = tileX*tileWidth + tileGridXOffset;
        tileXEnd = tileXStart + tileWidth - 1;

        prevXBoundary = Math.max(tileXStart, bounds.x);
        nextXBoundary = Math.min(tileXEnd, lastX);
    }

    protected final void setTileYBounds() {
        tileYStart = tileY*tileHeight + tileGridYOffset;
        tileYEnd = tileYStart + tileHeight - 1;

        prevYBoundary = Math.max(tileYStart, bounds.y);
        nextYBoundary = Math.min(tileYEnd, lastY);
    }
     
    protected void setDataBuffer() {
        Raster tile = im.getTile(tileX, tileY);
        this.dataBuffer = tile.getDataBuffer();

        int newSampleModelTranslateX = tile.getSampleModelTranslateX();
        int newSampleModelTranslateY = tile.getSampleModelTranslateY();
        localX += sampleModelTranslateX - newSampleModelTranslateX;
        localY += sampleModelTranslateY - newSampleModelTranslateY;

        this.sampleModelTranslateX = newSampleModelTranslateX;
        this.sampleModelTranslateY = newSampleModelTranslateY;
    }

    public void startLines() {
        y = bounds.y;
        localY = y - sampleModelTranslateY;
        tileY = startTileY;
        setTileYBounds();
        setDataBuffer();
    }

    public void nextLine() {
        ++y;
        ++localY;
    }

    public void jumpLines(int num) {
        int jumpY = y + num;
        if (jumpY < bounds.y || jumpY > lastY) {
            // Jumped outside the image.
            throw new IndexOutOfBoundsException(JaiI18N.getString("RectIterFallback1"));
        }

        y = jumpY;
        localY += num;

        if (y < prevYBoundary || y > nextYBoundary) {
            this.tileY = PlanarImage.YToTileY(y,
                                              tileGridYOffset,
                                              tileHeight);
            setTileYBounds();
            setDataBuffer();
        }
    }

    public boolean finishedLines() {
        if (y > nextYBoundary) {
            if (y > lastY) {
                return true;
            } else {
                ++tileY;
                tileYStart += tileHeight;
                tileYEnd += tileHeight;
                prevYBoundary = Math.max(tileYStart, bounds.y);
                nextYBoundary = Math.min(tileYEnd, lastY);

                setDataBuffer();
                return false;
            }
        } else {
            return false;
        }
    }

    public boolean nextLineDone() {
        nextLine();
        return finishedLines();
    }

    public void startPixels() {
        x = bounds.x;
        localX = x - sampleModelTranslateX;
        tileX = startTileX;
        setTileXBounds();
        setDataBuffer();
    }

    public void nextPixel() {
        ++x;
        ++localX;
    }

    public void jumpPixels(int num) {
        int jumpX = x + num;
        if (jumpX < bounds.x || jumpX > lastX) {
            // Jumped outside the image.
            throw new IndexOutOfBoundsException(JaiI18N.getString("RectIterFallback0"));
        }

        x = jumpX;
        localX += num;

        if (x < prevXBoundary || x > nextXBoundary) {
            this.tileX = PlanarImage.XToTileX(x,
                                              tileGridXOffset,
                                              tileWidth);
            setTileXBounds();
            setDataBuffer();
        }
    }

    public boolean finishedPixels() {
        if (x > nextXBoundary) {
            if (x > lastX) {
                return true;
            } else {
                ++tileX;
                tileXStart += tileWidth;
                tileXEnd += tileWidth;
                prevXBoundary = Math.max(tileXStart, bounds.x);
                nextXBoundary = Math.min(tileXEnd, lastX);
                setDataBuffer();
                return false;
            }
        } else {
            return false;
        }
    }

    public boolean nextPixelDone() {
        nextPixel();
        return finishedPixels();
    }

    public void startBands() {
        b = 0;
    }

    public void nextBand() {
        ++b;
    }

    public boolean nextBandDone() {
        nextBand();
        return finishedBands();
    }

    public boolean finishedBands() {
        return b >= numBands;
    }

    public int getSample() {
        return sampleModel.getSample(localX, localY, b, dataBuffer);
    }

    public int getSample(int b) {
        return sampleModel.getSample(localX, localY, b, dataBuffer);
    }

    public float getSampleFloat() {
        return sampleModel.getSampleFloat(localX, localY, b, dataBuffer);
    }

    public float getSampleFloat(int b) {
        return sampleModel.getSampleFloat(localX, localY, b, dataBuffer);
    }

    public double getSampleDouble() {
        return sampleModel.getSampleDouble(localX, localY, b, dataBuffer);
    }

    public double getSampleDouble(int b) {
        return sampleModel.getSampleDouble(localX, localY, b, dataBuffer);
    }

    public int[] getPixel(int[] iArray) {
        return sampleModel.getPixel(localX, localY, iArray, dataBuffer);
    }

    public float[] getPixel(float[] fArray) {
        return sampleModel.getPixel(localX, localY, fArray, dataBuffer);
    }

    public double[] getPixel(double[] dArray) {
        return sampleModel.getPixel(localX, localY, dArray, dataBuffer);
    }
}
