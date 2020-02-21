/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.model.ImageEditor;

import javax.media.jai.TileComputationListener;
import javax.media.jai.TileRequest;
import javax.media.jai.PlanarImage;
import java.awt.image.Raster;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: fabio
 * Date: Mar 28, 2005
 * Time: 12:56:24 PM
 * To change this template use File | Settings | File Templates.
 */

class PaintRequest implements PaintContext {
    final PlanarImage image;
    final int epoch;
    private final boolean synchronous;
    private final boolean prefetch;
    private final TileHandler tileHandler;
    private TileRequest tileRequest = null;
    private int pendingTiles;
    private Set<Point> tiles = new HashSet<Point>();
    private Set<Point> handledTiles = new HashSet<Point>();
    private boolean cancelled = false;

    PaintRequest(PlanarImage image, int epoch, Point[] tileIndices, boolean syncronous, boolean prefetch, TileHandler handler) {
        this.image = image;
        this.epoch = epoch;
        this.synchronous = syncronous;
        this.prefetch = prefetch;
        this.tileHandler = handler;
        this.pendingTiles = tileIndices.length;

        this.tileRequest = image.queueTiles(tileIndices);
        for (Point tileIndice : tileIndices)
            tiles.add(new Point(tileIndice.x, tileIndice.y));
    }

    @Override
    public boolean isPrefetch() {
        return prefetch;
    }

    @Override
    public boolean isSynchronous() {
        return synchronous;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public PlanarImage getImage() {
        return image;
    }

    void cancel() {
        assert !cancelled;
        cancelled = true;
        image.cancelTiles(tileRequest, null);
    }

    TileRequest getTileRequest() {
        assert tileRequest != null;
        return tileRequest;
    }

    int getPendingTiles() {
        return pendingTiles;
    }

    boolean hasTile(Point tile) {
        return tiles.contains(tile);
    }

    boolean handleTile(int tileX, int tileY) {
        Point thisTile = new Point(tileX, tileY);
        if (!cancelled && tiles.contains(thisTile) && !handledTiles.contains(thisTile)) {
            tileHandler.handle(tileX, tileY, this);
            handledTiles.add(thisTile);
            pendingTiles--;
            return true;
        }
        return false;
    }
}

public class TileManager implements TileComputationListener {
    private final List<PaintRequest> requests = new LinkedList<PaintRequest>();
    private PaintRequest prefetchRequest = null;

    private void cancelRequest(PaintRequest request) {
        request.cancel();
        if (prefetchRequest == request)
            prefetchRequest = null;
    }

    private void cancelPrefetch() {
        if (prefetchRequest == null)
            return;

        Iterator it = requests.iterator();
        while (it.hasNext()) {
            PaintRequest pr = (PaintRequest) it.next();
            if (pr == prefetchRequest) {
                it.remove();
                cancelRequest(pr);
            }
        }
        prefetchRequest = null;
    }

    private void handleTile(TileRequest tileRequest, int tileX, int tileY) {
        Iterator it = requests.iterator();
        while (it.hasNext()) {
            PaintRequest pr = (PaintRequest) it.next();
            if (pr.getTileRequest() == tileRequest && pr.handleTile(tileX, tileY)) {
                if (pr.getPendingTiles() == 0)
                    it.remove();
                return;
            }
        }
    }

    // Public interface, synchronized

    public synchronized void cancelTiles(PlanarImage image, int epoch) {
        Iterator it = requests.iterator();
        while (it.hasNext()) {
            PaintRequest pr = (PaintRequest) it.next();
            if (pr.image == image && pr.epoch == epoch) {
                it.remove();
                cancelRequest(pr);
            }
        }
    }

    public synchronized int pendingTiles(PlanarImage image, int epoch) {
        int pendingTiles = 0;
        for (final PaintRequest pr : requests) {
            if (pr.image == image && pr.epoch == epoch)
                pendingTiles += pr.getPendingTiles();
        }
        return pendingTiles;
    }

    public synchronized int queueTiles(PlanarImage image, int epoch, List<Point> tiles, boolean syncronous, boolean prefetch, TileHandler handler) {
        cancelPrefetch();

        /*
            If this is not a new image, see if we can prune the tile list:
            for all tiles in the list see if there is any corresponding
            request already enqueued. This is necessary since AWT can (and often
            does) enqueue the same repaint several times
         */

        Iterator dtit = tiles.iterator();

        next_tile:
        while (dtit.hasNext()) {
            Point tile = (Point) dtit.next();

            for (final PaintRequest pr : requests) {
                if (!pr.isCancelled() && pr.image == image && pr.epoch == epoch && pr.hasTile(tile)) {
                    dtit.remove();
                    continue next_tile;
                }
            }
        }

        if (!tiles.isEmpty()) {
            Point[] tileIndices = new Point[tiles.size()];

            int i = 0;
            for (final Point p : tiles)
                tileIndices[i++] = p;

            PaintRequest pr = new PaintRequest(image, epoch, tileIndices, syncronous, prefetch, handler);
            requests.add(pr);
            if (prefetch)
                prefetchRequest = pr;
        }

        return tiles.size();
    }

    /*
        TileComputationListener implementation, called from the TileScheduler, synchronized
     */

    @Override
    public synchronized void tileComputed(Object eventSource,
                             TileRequest[] tileRequests,
                             PlanarImage image, int tileX, int tileY,
                             Raster tile) {
        // turns out that the current request is always the first in the array
        handleTile(tileRequests[0], tileX, tileY);
    }

    @Override
    public synchronized void tileCancelled(Object eventSource,
                              TileRequest[] tileRequests,
                              PlanarImage image, int tileX, int tileY) {
        // nothing to do here
        // System.err.println("cancelled tile " + tileX + ":" + tileY);
    }

    @Override
    public synchronized void tileComputationFailure(Object eventSource,
                                       TileRequest[] tileRequests,
                                       PlanarImage image, int tileX, int tileY,
                                       final Throwable situation) {
        System.err.println("failed tile " + tileX + ":" + tileY);
        situation.printStackTrace();
        // TODO: eventually this should go away...
        /*
        EventQueue.invokeLater(
            new Runnable() {
                public void run() {
                    throw new RuntimeException(
                        "Tile Computation Failure: " + situation.getMessage(),
                        situation
                    );
                }
            }
        );
        */
    }
}
