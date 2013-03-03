/*
 * $RCSfile: RookIterFallback.java,v $
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
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import com.lightcrafts.mediax.jai.PlanarImage;
import com.lightcrafts.mediax.jai.iterator.RookIter;

/**
 * @since EA2
 */
public class RookIterFallback implements RookIter {

    /** The source image. */
    protected RenderedImage im;

    /** The iterator bounding rectangle. */
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

    /** The tile index of the rightmost column in the iterator's bounds. */ 
    protected int endTileX;

    /** The tile index of the topmost row in the iterator's bounds. */ 
    protected int startTileY;

    /** The tile index of the bottommost row in the iterator's bounds. */ 
    protected int endTileY;

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

    /** The (inclusive) smallest X coordinate of the bounding rectangle. */
    protected int firstX;

    /** The (inclusive) smallest Y coordinate of the bounding rectangle. */
    protected int firstY;

    /** The (inclusive) largest X coordinate of the bounding rectangle. */
    protected int lastX;

    /** The (inclusive) largest Y coordinate of the bounding rectangle. */
    protected int lastY;

    /** The current X pixel position. */
    protected int x;

    /** The current Y pixel position. */
    protected int y;

    /** The current X pixel position relative to the tile start. */
    protected int localX;

    /** The current Y pixel position relative to the tile start. */
    protected int localY;

    /** The current band offset. */
    protected int b;

    /** The DataBuffer of the current tile. */
    protected DataBuffer dataBuffer = null;

    public RookIterFallback(RenderedImage im, Rectangle bounds) {
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
        this.endTileX = PlanarImage.XToTileX(bounds.x + bounds.width - 1,
                                             tileGridXOffset,
                                             tileWidth);
        this.startTileY = PlanarImage.YToTileY(bounds.y,
                                               tileGridYOffset,
                                               tileHeight);
        this.endTileY = PlanarImage.YToTileY(bounds.y + bounds.height - 1,
                                             tileGridYOffset,
                                             tileHeight);

        this.tileX = startTileX;
        this.tileY = startTileY;

        this.firstX = bounds.x;
        this.firstY = bounds.y;
        this.lastX = bounds.x + bounds.width - 1;
        this.lastY = bounds.y + bounds.height - 1;

        x = bounds.x;
        y = bounds.y;
        b = 0;

        setTileXBounds();
        setTileYBounds();
        setDataBuffer();
    }

    private final void setTileXBounds() {
        tileXStart = tileX*tileWidth + tileGridXOffset;
        tileXEnd = tileXStart + tileWidth - 1;
        localX = x - tileXStart;

        prevXBoundary = Math.max(tileXStart, firstX);
        nextXBoundary = Math.min(tileXEnd, lastX);
    }

    private final void setTileYBounds() {
        tileYStart = tileY*tileHeight + tileGridYOffset;
        tileYEnd = tileYStart + tileHeight - 1;
        localY = y - tileYStart;

        prevYBoundary = Math.max(tileYStart, firstY);
        nextYBoundary = Math.min(tileYEnd, lastY);
    }
     
    private final void setDataBuffer() {
        this.dataBuffer = im.getTile(tileX, tileY).getDataBuffer();
    }

    public void startLines() {
        y = firstY;
        localY = y - tileYStart;
        tileY = startTileY;
        setTileYBounds();
        setDataBuffer();
    }

    public void endLines() {
        y = lastY;
        localY = y - tileYStart;
        tileY = endTileY;
        setTileYBounds();
        setDataBuffer();
    }

    public void nextLine() {
        ++y;
        ++localY;
    }

    public void prevLine() {
        --y;
        --localY;
    }

    public void jumpLines(int num) {
        y += num;
        localY += num;

        if (y < tileYStart || y > tileYEnd) {
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
                localY -= tileHeight;
                prevYBoundary = Math.max(tileYStart, firstY);
                nextYBoundary = Math.min(tileYEnd, lastY);

                setDataBuffer();
                return false;
            }
        } else {
            return false;
        }
    }

    public boolean finishedLinesTop() {
        if (y < prevYBoundary) {
            if (y < firstY) {
                return true;
            } else {
                --tileY;
                tileYStart -= tileHeight;
                tileYEnd -= tileHeight;
                localY += tileHeight;
                prevYBoundary = Math.max(tileYStart, firstY);
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

    public boolean prevLineDone() {
        prevLine();
        return finishedLinesTop();
    }

    public void startPixels() {
        x = firstX;
        localX = x - tileXStart;
        tileX = startTileX;
        setTileXBounds();
        setDataBuffer();
    }

    public void endPixels() {
        x = lastX;
        tileX = endTileX;
        setTileXBounds();
        setDataBuffer();
    }

    public void nextPixel() {
        ++x;
        ++localX;
    }

    public void prevPixel() {
        --x;
        --localX;
    }

    public void jumpPixels(int num) {
        x += num;
        localX += num;
        
        if (x < tileXStart || x > tileXEnd) {
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
                localX -= tileWidth;
                prevXBoundary = Math.max(tileXStart, firstX);
                nextXBoundary = Math.min(tileXEnd, lastX);
                
                setDataBuffer();
                return false;
            }
        } else {
            return false;
        }
    }

    public boolean finishedPixelsLeft() {
        if (x < prevXBoundary) {
            if (x < firstX) {
                return true;
            } else {
                --tileX;
                tileXStart -= tileWidth;
                tileXEnd -= tileWidth;
                localX += tileWidth;
                prevXBoundary = Math.max(tileXStart, firstX);
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

    public boolean prevPixelDone() {
        prevPixel();
        return finishedPixelsLeft();
    }

    public void startBands() {
        b = 0;
    }

    public void endBands() {
        b = numBands - 1;
    }

    public void prevBand() {
        --b;
    }

    public void nextBand() {
        ++b;
    }

    public boolean prevBandDone() {
        return (--b) < 0;
    }

    public boolean nextBandDone() {
        return (++b) >= numBands;
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
