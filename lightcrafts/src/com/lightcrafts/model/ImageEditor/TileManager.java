/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2022-     Masahiro Kitagawa */

package com.lightcrafts.model.ImageEditor;

import javax.media.jai.PlanarImage;
import javax.media.jai.TileComputationListener;
import javax.media.jai.TileRequest;
import java.awt.*;
import java.awt.image.Raster;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.function.Predicate.not;

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
    private final TileRequest tileRequest;
    private int pendingTiles;
    private final Set<Point> tiles;
    private final Set<Point> handledTiles = new HashSet<>();
    private boolean cancelled = false;

    PaintRequest(PlanarImage image, int epoch, Set<Point> tileIndices, boolean syncronous, boolean prefetch,
                 TileHandler handler) {
        this.image = image;
        this.epoch = epoch;
        this.tiles = Set.copyOf(tileIndices);
        this.synchronous = syncronous;
        this.prefetch = prefetch;
        this.tileHandler = handler;

        final int size = tileIndices.size();
        this.pendingTiles = size;
        this.tileRequest = image.queueTiles(tileIndices.toArray(new Point[size]));
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
    private final List<PaintRequest> requests = new LinkedList<>();
    private PaintRequest prefetchRequest = null;

    private PaintRequest cancelRequest(PaintRequest request) {
        request.cancel();
        if (prefetchRequest == request)
            prefetchRequest = null;
        return request;
    }

    private void cancelPrefetch() {
        if (prefetchRequest == null)
            return;

        final var canceled = requests.stream()
                .filter(pr -> pr == prefetchRequest)
                .map(this::cancelRequest)
                .collect(Collectors.toList());
        requests.removeAll(canceled);

        prefetchRequest = null;
    }

    private void handleTile(TileRequest tileRequest, int tileX, int tileY) {
        final var removed = requests.stream()
                .filter(pr -> pr.getTileRequest() == tileRequest)
                .filter(pr -> pr.handleTile(tileX, tileY))
                .findFirst()
                .filter(pr -> pr.getPendingTiles() == 0);
        removed.ifPresent(requests::remove);
    }

    // Public interface, synchronized

    public synchronized void cancelTiles(PlanarImage image, int epoch) {
        final var canceled = requests.stream()
                .filter(pr -> pr.image == image)
                .filter(pr -> pr.epoch == epoch)
                .map(this::cancelRequest)
                .collect(Collectors.toList());
        requests.removeAll(canceled);
    }

    public synchronized int pendingTiles(PlanarImage image, int epoch) {
        return requests.stream()
                .filter(pr -> pr.image == image)
                .filter(pr -> pr.epoch == epoch)
                .mapToInt(PaintRequest::getPendingTiles)
                .sum();
    }

    public synchronized int queueTiles(PlanarImage image, int epoch, List<Point> tiles, boolean syncronous,
                                       boolean prefetch, TileHandler handler) {
        cancelPrefetch();

        /*
            If this is not a new image, see if we can prune the tile list:
            for all tiles in the list see if there is any corresponding
            request already enqueued. This is necessary since AWT can (and often
            does) enqueue the same repaint several times
         */

        final var paintRequests = requests.stream()
                .filter(not(PaintRequest::isCancelled))
                .filter(pr -> pr.image == image)
                .filter(pr -> pr.epoch == epoch)
                .collect(Collectors.toList());
        tiles.removeIf(t -> paintRequests.stream().anyMatch(pr -> pr.hasTile(t)));

        if (!tiles.isEmpty()) {
            PaintRequest pr = new PaintRequest(image, epoch, Set.copyOf(tiles), syncronous, prefetch, handler);
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
