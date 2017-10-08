/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.utils;

import javax.media.jai.TileComputationListener;
import javax.media.jai.PlanarImage;
import javax.media.jai.TileRequest;
import java.awt.*;
import java.awt.image.Raster;

/**
 * Created by IntelliJ IDEA.
 * User: fabio
 * Date: Sep 30, 2005
 * Time: 12:27:51 PM
 * To change this template use File | Settings | File Templates.
 */
public class RenderingProgressTask implements TileComputationListener {
    int numTiles = 0;
    int computedTiles = 0;
    PlanarImage theImage = null;
    ProgressIndicator theListener;

    public RenderingProgressTask(ProgressIndicator listener) {
        theListener = listener;
    }

    public synchronized void registerImage(PlanarImage image) {
        if (theImage != null)
            deRegisterImage(theImage);

        Point[] tileIndices = image.getTileIndices(image.getBounds());
        numTiles = tileIndices.length;
        theListener.setMinimum(0);
        theListener.setMaximum(numTiles);
        computedTiles = 0;

        image.addTileComputationListener(this);
        image.queueTiles(tileIndices);
        theImage = image;

        theListener.setMaximum(tileIndices.length);
    }

    public synchronized void deRegisterImage(PlanarImage image) {
        image.removeTileComputationListener(this);
        numTiles = 0;
        theImage = null;
    }

    public boolean done() {
        return computedTiles >= numTiles;
    }

    public synchronized void tileComputed(Object eventSource,
                             TileRequest[] tileRequests,
                             PlanarImage image, int tileX, int tileY,
                             Raster tile) {
        computedTiles++;
        theListener.incrementBy(1);
        if (computedTiles >= numTiles)
            theListener.setIndeterminate(true);
        // System.err.println("RP: computed tile " + tileX + ":" + tileY + ", number " + computedTiles + " of " + numTiles);
        this.notify();
    }

    public synchronized void tileCancelled(Object eventSource,
                              TileRequest[] tileRequests,
                              PlanarImage image, int tileX, int tileY) {
        System.err.println("RP: cancelled tile " + tileX + ":" + tileY);
        this.notify();
    }

    public synchronized void tileComputationFailure(Object eventSource,
                                       TileRequest[] tileRequests,
                                       PlanarImage image, int tileX, int tileY,
                                       final Throwable situation) {
        System.err.println("RP: failed tile " + tileX + ":" + tileY);
        this.notify();
    }

}
