/*
 * $RCSfile: SnapshotImage.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.2 $
 * $Date: 2005/05/12 18:24:34 $
 * $State: Exp $
 */
package com.lightcrafts.mediax.jai;
import java.awt.Point;
import java.awt.image.Raster;
import java.awt.image.TileObserver;
import java.awt.image.WritableRaster;
import java.awt.image.WritableRenderedImage;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;

/**
 * A (Raster, X, Y) tuple.
 */
final class TileCopy {

    /** The tile's <code>Raster</code> data. */
    Raster tile;

    /** The tile's column within the image tile grid. */
    int tileX;

    /** The tile's row within the image tile grid. */
    int tileY;

    /**
     * Constructs a TileCopy object given the tile's <code>Raster</code> data
     * and its location in the tile grid.
     *
     * @param tile the <code>Raster</code> containing the tile's data.
     * @param tileX the tile's X position in the tile grid.
     * @param tileY the tile's X position in the tile grid.
     */
    TileCopy(Raster tile, int tileX, int tileY) {
        this.tile = tile;
        this.tileX = tileX;
        this.tileY = tileY;
    }
}


/**
 * A proxy for <code>Snapshot</code> that calls
 * <code>Snapshot.dispose()</code> when finalized.
 * No references to a SnapshotProxy are held internally, only user
 * references.  Thus it will be garbage collected when the last user
 * reference is relinquished.  The <code>Snapshot</code>'s
 * <code>dispose()</code> method
 * is called from <code>SnapshotProxy.finalize()</code>, ensuring that all
 * of the resources held by the <code>Snapshot</code> will become collectable.
 */
final class SnapshotProxy extends PlanarImage {
    /**
     * The parent <code>Snapshot</code> to which we forward
     * <code>getTile()</code> calls.
     */
    Snapshot parent;

    /**
     * Construct a new proxy for a given <code>Snapshot</code>.
     *
     * @param parent the <code>Snapshot</code> to which method calls will
     * be forwarded.
     */
    SnapshotProxy(Snapshot parent) {
        super(new ImageLayout(parent), null, null);
        this.parent = parent;
    }

    /**
     * Forwards a tile request to the parent <code>Snapshot</code>.
     *
     * @param tileX the X index of the tile.
     * @param tileY the Y index of the tile.
     * @return the tile as a <code>Raster</code>.
     */
    public Raster getTile(int tileX, int tileY) {
        return parent.getTile(tileX, tileY);
    }

    /** Disposes of resources held by this proxy. */
    public void dispose() {
        parent.dispose();
    }
}

/**
 * A non-public class that holds a portion of the state associated
 * with a <code>SnapShotImage</code>.  A <code>Snapshot</code> provides the
 * appearance of a <code>PlanarImage</code> with fixed contents.  In order to
 * provide this illusion, however, the <code>Snapshot</code> relies on the
 * fact that it belongs to a linked list of <code>Snapshot</code>s rooted in a
 * particular <code>SnapShotImage</code>; it cannot function independently.
 *
 */
final class Snapshot extends PlanarImage {

    /** The creator of this image. */
    SnapshotImage parent;

    /** The next <code>Snapshot</code> in a doubly-linked list. */
    Snapshot next;

    /** The previous <code>Snapshot</code> in a doubly-linked list. */
    Snapshot prev;

    /** A set of cached TileCopy elements. */
    Hashtable tiles = new Hashtable();

    /** True if <code>dispose()</code> has been called. */
    boolean disposed = false;

    /**
     * Constructs a <code>Snapshot</code> that will provide a synchronous
     * view of a <code>SnapshotImage</code> at a particular moment in time.
     *
     * @param parent a <code>SnapshotImage</code> this image will be viewing.
     */
    Snapshot(SnapshotImage parent) {
        super(new ImageLayout(parent), null, null);
        this.parent = parent;
    }

    /**
     * Returns the version of a tile "seen" by this <code>Snapshot</code>.
     * The tile "seen" is the oldest copy of the tile made after
     * the creation of this <code>Snapshot</code>; it may be held in the
     * tiles <code>Hashtable</code> of this <code>Snapshot</code> or one of
     * its successors.  If no later <code>Snapshot</code> holds a copy of
     * the tile, the current version of the tile from the source image is
     * returned.
     *
     * <p> <code>getTile()</code> is synchronized in order to prevent calls to
     * <code>dispose()</code>, which will cause the list of
     * <code>Snapshot</code>s to change, from occurring at the same time as
     * the walking of the list.
     *
     * @param tileX the X index of the tile.
     * @param tileY the Y index of the tile.
     * @return the tile as a <code>Raster</code>.
     */
    public Raster getTile(int tileX, int tileY) {
        // Make sure dispose() and getTile() are mutually exclusive
        synchronized(parent) {
            // Check local set of tile copies, if not there move
            // forward to the next <code>Snapshot</code>, if last image
            // get the tile from the real source image.

            TileCopy tc = (TileCopy)tiles.get(new Point(tileX, tileY));
            if (tc != null) {
                return tc.tile;
            } else if (next != null) {
                return next.getTile(tileX, tileY);
            } else {
                return parent.getTrueSource().getTile(tileX, tileY);
            }
        }
    }

    /**
     * Sets the next <code>Snapshot</code> in the list to a given
     * <code>Snapshot</code>.
     *
     * @param next the next <code>Snapshot</code> in the list.
     */
    void setNext(Snapshot next) {
        this.next = next;
    }

    /**
     * Sets the previous <code>Snapshot</code> in the list to a given
     * <code>Snapshot</code>.
     *
     * @param prev the previous <code>Snapshot</code> in the list.
     */
    void setPrev(Snapshot prev) {
        this.prev = prev;
    }

    /**
     * Returns true if this <code>Snapshot</code> already stores a version
     * of a specified tile.
     *
     * @param tileX the X index of the tile.
     * @param tileY the Y index of the tile.
     * @return true if this <code>Snapshot</code> holds a copy of the tile.
     */
    boolean hasTile(int tileX, int tileY) {
        TileCopy tc = (TileCopy)tiles.get(new Point(tileX, tileY));
        return tc != null;
    }

    /**
     * Stores a given tile in this <code>Snapshot</code>.  The caller should
     * not attempt to store more than one version of a given tile.
     *
     * @param tile a <code>Raster</code> containing the tile data.
     * @param tileX the tile's column within the image tile grid.
     * @param tileY the tile's row within the image tile grid.
     */
    void addTile(Raster tile, int tileX, int tileY) {
        TileCopy tc = new TileCopy(tile, tileX, tileY);
        tiles.put(new Point(tileX, tileY), tc);
    }

    /** This image will no longer be referenced by the user. */
    public void dispose() {
        // Make sure dispose() and getTile() are mutually exclusive
        synchronized(parent) {
            // Make it idempotent
            if (disposed) {
                return;
            }
            disposed = true;

            // If this is the last Snapshot, inform the parent
            if (parent.getTail() == this) {
                parent.setTail(prev);
            }

            // Remove 'this' from the chain
            if (prev != null) {
                prev.setNext(next);
            }
            if (next != null) {
                next.setPrev(prev);
            }

            // If there is a previous node, push tiles back to it
            if (prev != null) {
                // Push tiles back to the previous Snapshot
                Enumeration enumeration = tiles.elements();
                while (enumeration.hasMoreElements()) {
                    TileCopy tc = (TileCopy)enumeration.nextElement();
                    if (!prev.hasTile(tc.tileX, tc.tileY)) {
                        prev.addTile(tc.tile, tc.tileX, tc.tileY);
                    }
                }
            }

            // Null out links to help the GC
            parent = null;
            next = prev = null;
            tiles = null;
        }
    }
}

/**
 * A class providing an arbitrary number of synchronous views of a
 * possibly changing <code>WritableRenderedImage</code>.
 * <code>SnapshotImage</code> is responsible for stabilizing changing sources
 * in order to allow deferred execution of operations dependent on such
 * sources.
 *
 * <p> Any <code>RenderedImage</code> may be used as the source of a
 * <code>SnapshotImage</code>; if it is a <code>WritableRenderedImage</code>,
 * the <code>SnapshotImage</code> will register itself as a
 * <code>TileObserver</code> and make copies of tiles that are about to change.
 * Multiple versions of each tile are maintained internally, as long as they
 * are in demand.  <code>SnapshotImage</code> is able to track demand and
 * should be able to simply forward requests for tiles to the source most
 * of the time, without the need to make a copy.
 *
 * <p> When used as a source, calls to getTile will simply be passed
 * along to the source.  In other words, <code>SnapshotImage</code> is
 * completely transparent.  However, by calling <code>createSnapshot()</code>
 * an instance of a non-public <code>PlanarImage</code> subclass (called
 * <code>Snapshot</code> in this implementation) will be created and returned.
 * This image will always return tile data with contents as of the time of its
 * construction.
 *
 * <p> When a particular <code>Snapshot</code> is no longer needed, its
 * <code>dispose()</code> method may be called.    The <code>dispose()</code>
 * method will be called automatically when the <code>Snapshot</code> is
 * finalized by the garbage collector.  Disposing of the <code>Snapshot</code>
 * allows tile data held by the <code>Snapshot</code> that is not needed by
 * any other <code>Snapshot</code> to be disposed of as well.
 *
 * <p> This implementation of <code>SnapshotImage</code> makes use of a
 * doubly-linked list of <code>Snapshot</code> objects.  A new
 * <code>Snapshot</code> is added to the tail of the list whenever
 * <code>createSnapshot()</code> is called.  Each <code>Snapshot</code>
 * has a cache containing copies of any tiles that were writable at the
 * time of its construction, as well as any tiles that become writable
 * between the time of its construction and the construction of the next
 * <code>Snapshot</code>.
 *
 * <p> When asked for a tile, a <code>Snapshot</code> checks its local cache
 * and returns its version of the tile if one is found.  Otherwise, it
 * forwards the request onto its successor.  This process continues
 * until the latest <code>Snapshot</code> is reached; if it does not contain
 * a copy of the tile, the tile is requested from the real source image.
 *
 * <p> When a <code>Snapshot</code> is no longer needed, its
 * <code>dispose()</code> method attempts to push the contents of its tile
 * cache back to the previous <code>Snapshot</code> in the linked list.  If
 * that image possesses a version of the same tile, the tile is not pushed
 * back and may be discarded.
 *
 * @see java.awt.image.RenderedImage
 * @see java.awt.image.TileObserver
 * @see java.awt.image.WritableRenderedImage
 * @see PlanarImage
 *
 */
public class SnapshotImage extends PlanarImage implements TileObserver {

    /** The real image source. */
    private PlanarImage source;

    /** The last entry in the list of <code>Snapshot</code>, initially null. */
    private Snapshot tail = null;

    /** The set of active tiles, represented as a HashSet of Points. */
    private HashSet activeTiles = new HashSet();

    /**
     * Constructs a <code>SnapshotImage</code> from a <code>PlanarImage</code>
     * source.
     *
     * @param source a <code>PlanarImage</code> source.
     * @throws IllegalArgumentException if source is null.
     */
    public SnapshotImage(PlanarImage source) {
        super(new ImageLayout(source), null, null);

        // Record the source image
        this.source = source;
        //  Set image parameters to match the source

        //  Determine which tiles of the source image are writable
        if (source instanceof WritableRenderedImage) {
            WritableRenderedImage wri = (WritableRenderedImage)source;
            wri.addTileObserver(this);

            Point[] pts = wri.getWritableTileIndices();
            if (pts != null) {
                int num = pts.length;
                for (int i = 0; i < num; i++) {
                    //  Add these tiles to the active list
                    Point p = pts[i];
                    activeTiles.add(new Point(p.x, p.y));
               }
            }
        }
    }

    /**
     * Returns the <code>PlanarImage</code> source of this
     * <code>SnapshotImage</code>.
     *
     * @return a <code>PlanarImage</code> that is the source of data for this
     * image.
     */
    protected PlanarImage getTrueSource() {
        return source;
    }

    /**
     * Sets the reference to the most current <code>Snapshot</code> to a given
     * <code>Snapshot</code>.
     *
     * @param tail a reference to the new most current <code>Snapshot</code>.
     */
    void setTail(Snapshot tail) {
        this.tail = tail;
    }

    /**
     * Returns a reference to the most current <code>Snapshot</code>.
     *
     * @return the <code>Snapshot</code> at the tail end of the list.
     */
    Snapshot getTail() {
        return tail;
    }

    /**
     * Creates and returns a <code>Raster</code> copy of a given source tile.
     *
     * @param tileX the X index of the tile.
     * @param tileY the Y index of the tile.
     * @return a newly-constructed <code>Raster</code> containing a copy
     *         of the tile data.
     */
    private Raster createTileCopy(int tileX, int tileY) {
        int x = tileXToX(tileX);
        int y = tileYToY(tileY);
        Point p = new Point(x, y);

        WritableRaster tile =
            RasterFactory.createWritableRaster(sampleModel, p);
        source.copyData(tile);
        return tile;
    }

    /**
     * Creates a snapshot of this image.  This snapshot may be used
     * indefinitely, and will always appear to have the pixel data that
     * this image has currently.  The snapshot is semantically a copy
     * of this image but may be implemented in a more efficient manner.
     * Multiple snapshots taken at different times may share tiles that
     * have not changed, and tiles that are currently static in this
     * image's source do not need to be copied at all.
     *
     * @return a <code>PlanarImage</code> snapshot.
     */
    public PlanarImage createSnapshot() {
        if (source instanceof WritableRenderedImage) {
            // Create a new Snapshot
            Snapshot snap = new Snapshot(this);

            // For each active tile:
            Iterator iter = activeTiles.iterator();
            while (iter.hasNext()) {
                Point p = (Point)iter.next();

                // Make a copy and store it in the Snapshot
                Raster tile = createTileCopy(p.x, p.y);
                snap.addTile(tile, p.x, p.y);
            }

            // Add the new Snapshot to the list of snapshots
            if (tail == null) {
                tail = snap;
            } else {
                tail.setNext(snap);
                snap.setPrev(tail);
                tail = snap;
            }

            // Create a proxy and return it
            return new SnapshotProxy(snap);
        } else {
            return source;
        }
    }

    /**
     * Receives the information that a tile is either about to become
     * writable, or is about to become no longer writable.
     *
     * @param source the <code>WritableRenderedImage</code> for which we
     *               are an observer.
     * @param tileX the X index of the tile.
     * @param tileY the Y index of the tile.
     * @param willBeWritable true if the tile is becoming writable.
     */
    public void tileUpdate(WritableRenderedImage source,
                           int tileX, int tileY,
                           boolean willBeWritable) {
        if (willBeWritable) {
            // If the last Snapshot doesn't have the tile, copy it
            if ((tail != null) && (!tail.hasTile(tileX, tileY))) {
                tail.addTile(createTileCopy(tileX, tileY), tileX, tileY);
            }
            // Add the tile to the active list
            activeTiles.add(new Point(tileX, tileY));
        } else {
            // Remove the tile from the active list
            activeTiles.remove(new Point(tileX, tileY));
        }
    }

    /**
     * Returns a non-snapshotted tile from the source.
     *
     * @param tileX the X index of the tile.
     * @param tileY the Y index of the tile.
     * @return the tile as a <code>Raster</code>.
     */
    public Raster getTile(int tileX, int tileY) {
        //  Return the current source tile (X, Y)
        return source.getTile(tileX, tileY);
    }
}
