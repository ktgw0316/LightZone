/*
 * $RCSfile: TileRequest.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:22 $
 * $State: Exp $
 */
package com.lightcrafts.mediax.jai;

import java.awt.Point;

/**
 * Interface representing a <code>TileScheduler</code> request to compute
 * a specific set of tiles for a given image with optional monitoring by
 * <code>TileComputationListener</code>s.
 *
 * @see TileScheduler
 * @see TileComputationListener
 * @see RenderedOp
 * @see OpImage
 *
 * @since JAI 1.1
 */
public interface TileRequest {

    /**
     * Status value indicating that the tile has yet to be processed.
     */
    public static final int TILE_STATUS_PENDING = 0;

    /**
     * Status value indicating that the tile has is being processed.
     */
    public static final int TILE_STATUS_PROCESSING = 1;

    /**
     * Status value indicating that the tile has been computed successfully.
     */
    public static final int TILE_STATUS_COMPUTED = 2;

    /**
     * Status value indicating that the tile computation has been cancelled.
     */
    public static final int TILE_STATUS_CANCELLED = 3;

    /**
     * Status value indicating that the tile computation failed.
     */
    public static final int TILE_STATUS_FAILED = 4;

    /**
     * Returns the image associated with the request.  This is the image
     * which is actually specified to the <code>TileScheduler</code>.  For
     * most <code>PlanarImage</code>s (including <code>OpImage</code>s)
     * this will be the image on which <code>queueTiles()</code> was
     * invoked; for <code>RenderedOp</code> nodes this will be the rendering
     * of the node.
     */
    PlanarImage getImage();

    /**
     * Returns the tile indices of all tiles associated with the request.
     */
    Point[] getTileIndices();

    /**
     * Returns the array of <code>TileComputationListener</code>s specified as
     * monitoring the request.  The returned value should be <code>null</code>
     * if there are no such listeners.
     */
    TileComputationListener[] getTileListeners();

    /**
     * Whether this <code>TileRequest</code> implementation supports the
     * <code>getTileStatus()</code> method.
     */
    boolean isStatusAvailable();

    /**
     * Returns one of the <code>TILE_STATUS_*</code> constants defined in
     * this interface to indicate the status of the specified tile (optional
     * operation).  Implementations for which status is available, i.e.,
     * for which <code>isStatusAvailable()</code> returns <code>true</code>,
     * may but are not required to support all status levels defined by
     * this interface.  The status levels must however be a subset of those
     * herein defined.
     *
     * @param tileX The X index of the tile in the tile array.
     * @param tileY The Y index of the tile in the tile array.
     *
     * @exception UnsupportedOperationException if
     * <code>isStatusAvailable()</code> returns <code>false</code>.
     * @exception IllegalArgumentException if the specified tile is not
     * associated with this request.
     */
    int getTileStatus(int tileX, int tileY);

    /**
     * Issues a request to the <code>TileScheduler</code> which generated
     * this <code>TileRequest</code> to cancel all tiles in the supplied
     * parameter array which are associated with this request.  Any tiles
     * in the array which are not associated with this request will be
     * ignored.  If the parameter is <code>null</code> a request to cancel
     * all tiles in the request will be issued.  This method should merely
     * be a convenience wrapper around the <code>cancelTiles()</code>
     * method of the <code>TileScheduler</code> which created the
     * <code>TileRequest</code>.
     */
    void cancelTiles(Point[] tileIndices);
}
