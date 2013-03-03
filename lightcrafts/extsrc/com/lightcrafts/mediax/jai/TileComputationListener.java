/*
 * $RCSfile: TileComputationListener.java,v $
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

import java.awt.image.Raster;
import java.util.EventListener;

/**
 * Interface to monitor tiles which have been submitted to the
 * <code>TileScheduler</code> for non-prefetch background processing.
 * The request parameter of each method corresponds to the value
 * returned by the method used to queue the tiles, i.e.,
 * <code>TileScheduler.scheduleTiles()</code> or more commonly
 * <code>PlanarImage.queueTiles()</code>.  The <code>eventSource</code>
 * parameter provides the identity of the emitter of the event.  If the
 * event is emitted by the <code>TileScheduler</code> itself this will
 * be a reference to the <code>TileScheduler</code> object; if it is
 * emitted by a <code>RenderedOp</code> this will be the
 * <code>RenderedOp</code> node.  The <code>image</code> parameter will
 * in all cases be the image actually specified to the
 * <code>TileScheduler</code>.
 *
 * <p> With respect to standard rendered imaging chains consisting of
 * <code>RenderedOp</code> nodes, any <code>TileComputationListener</code>s
 * registered with the <code>RenderedOp</code> itself will receive events
 * from the <code>RenderedOp</code> so that the event source will be the
 * <code>RenderedOp</code>.  If the listener is registered with the
 * rendering of the node, then the event source will likely be the
 * <code>TileScheduler</code> itself.  This is definitely the case if
 * the rendering is either an <code>OpImage</code> or is a
 * <code>PlanarImage</code> which has not overridden <code>queueTiles()</code>.
 *
 * <p> The <code>image</code> parameter passed to any registered listener of
 * a <code>RenderedOp</code> will contain a reference to the rendering of the
 * node rather than to the node itself.
 *
 * <p> For a given <code>TileComputationListener</code> exactly one of the
 * tile status callbacks should be invoked during the life cycle of a given
 * tile in the <code>TileScheduler</code>.
 *
 * @see TileScheduler
 * @see TileRequest
 * @see RenderedOp
 * @see OpImage
 *
 * @since JAI 1.1
 */
public interface TileComputationListener extends EventListener {

    /**
     * To be invoked after each tile is computed.
     *
     * @param eventSource The actual emitter of the tile scheduling event, i.e.,
     *                    the caller of this method.
     * @param requests The relevant tile computation requests as returned
     *                 by the method used to queue the tile.
     * @param image    The image for which tiles are being computed as
     *                 specified to the <code>TileScheduler</code>.
     * @param tileX    The X index of the tile in the tile array.
     * @param tileY    The Y index of the tile in the tile array.
     * @param tile     The computed tile.
     */
    void tileComputed(Object eventSource,
                      TileRequest[] requests,
                      PlanarImage image, int tileX, int tileY,
                      Raster tile);

    /**
     * To be invoked after a tile is cancelled.
     *
     * @param eventSource The actual emitter of the tile scheduling event, i.e.,
     *                    the caller of this method.
     * @param requests The relevant tile computation requests as returned
     *                 by the method used to queue the tile.
     * @param image    The image for which tiles are being computed as
     *                 specified to the <code>TileScheduler</code>.
     * @param tileX    The X index of the tile in the tile array.
     * @param tileY    The Y index of the tile in the tile array.
     */
    void tileCancelled(Object eventSource,
                       TileRequest[] requests,
                       PlanarImage image, int tileX, int tileY);

    /**
     * To be invoked when an exceptional situation prevents computation of
     * a tile.
     *
     * @param eventSource The actual emitter of the tile scheduling event, i.e.,
     *                    the caller of this method.
     * @param requests  The relevant tile computation requests as returned
     *                  by the method used to queue the tile.
     * @param image     The image for which tiles are being computed as
     *                  specified to the <code>TileScheduler</code>.
     * @param tileX     The X index of the tile in the tile array.
     * @param tileY     The Y index of the tile in the tile array.
     * @param situation An object describing the error or exception which
     *                  prevented computation of the tile.
     */
    void tileComputationFailure(Object eventSource,
                                TileRequest[] requests,
                                PlanarImage image, int tileX, int tileY,
                                Throwable situation);
}
