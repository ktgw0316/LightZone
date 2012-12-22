/*
 * $RCSfile: TileScheduler.java,v $
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
import java.awt.Point;


/**
 * A class implementing a mechanism for scheduling tile calculation.
 * In various implementations tile computation may make use of multithreading
 * and multiple simultaneous network connections for improved performance.
 *
 * <p> If multithreading is used then the implementation of the interface
 * must be thread-safe.  In particular it must be possible to invoke any of
 * the tile scheduling methods on the same image simultaneously from different
 * threads and obtain the same results as if all invocations had been from
 * the same thread.
 *
 * <p> Errors and exceptions which occur within the scheduler and which prevent
 * tile computation will be thrown via the usual mechanism for all blocking
 * methods, i.e., those which perform the computations while the invoking
 * thread blocks.  Failure conditions encountered in computations effected
 * via non-blocking methods will be indicated by notifying any listeners.
 * In neither case is it expected that the tiles will be re-scheduled for
 * computation this instead being left to the application.
 */
public interface TileScheduler {

    /** 
     * Schedules a tile for computation.  Called by
     * <code>OpImage.getTile()</code>, this method makes
     * <code>OpImage.computeTile()</code> calls to calculate
     * the destination tile.  This will provoke the computation
     * of any required source tiles as well.
     *
     * @param target An <code>OpImage</code> whose tile is to be computed.
     * @param tileX The X index of the tile to be computed.
     * @param tileY The Y index of the tile to be computed.
     * @return A <code>Raster</code> containing the contents of the tile.
     *
     * @throws IllegalArgumentException if <code>target</code> is
     *         <code>null</code>.
     */
    Raster scheduleTile(OpImage target, int tileX, int tileY);
 
    /** 
     * Schedules a list of tiles for computation.  Called by 
     * <code>OpImage.getTiles</code>, this method makes
     * <code>OpImage.computeTile()</code> calls to calculate
     * the destination tiles.  This will provoke the computation
     * of any required source tiles as well.
     *
     * @param target An <code>OpImage</code> whose tiles are to be computed.
     * @param tileIndices A list of tile indices indicating which tiles
     *        to schedule for computation.
     * @return An array of <code>Raster</code>s containing a computed
     *         raster for every tile index passed in.
     *
     * @throws IllegalArgumentException if <code>target</code> or
     *         <code>tileIndices</code> is <code>null</code>.
     */
    Raster[] scheduleTiles(OpImage target, Point tileIndices[]);

    /**
     * Schedule a list of tiles for computation.  The supplied listeners
     * will be notified of the status of each tile, i.e., when each tile
     * is computed, cancelled, or encounters an error.  This
     * method ideally should be non-blocking. If the <code>TileScheduler</code>
     * implementation uses multithreading, it is at the discretion of the
     * implementation which thread invokes the
     * <code>TileComputationListener</code> methods.  The event source
     * parameter passed to each listener will be the <code>TileScheduler</code>
     * itself and the image parameter will be the specified target image.
     *
     * <p> In the Sun Microsystems reference implementation of
     * <code>TileScheduler</code> the <code>TileComputationListener</code>
     * methods are invoked by the thread which performs the actual
     * tile computation.  This will be the primary thread if the
     * parallelism is zero, or a worker thread if it is positive.
     *
     * @param target A <code>PlanarImage</code> whose tiles are to be computed.
     * @param tileIndices A list of tile indices indicating which tiles
     *        to schedule for computation.
     * @param tileListeners <code>TileComputationListener</code>s to be
     *        informed of tile computation status; may be <code>null</code>.
     * @return The <code>TileRequest</code> for this set of tiles.
     * @throws IllegalArgumentException if <code>target</code> or
     *         <code>tileIndices</code> is <code>null</code>.
     *
     * @since JAI 1.1
     */
    TileRequest scheduleTiles(PlanarImage target, Point[] tileIndices,
                              TileComputationListener[] tileListeners);

    /**
     * Issues an advisory cancellation request to the
     * <code>TileScheduler</code> stating that the indicated tiles of the
     * specified request should not be processed.  The handling of cancellation
     * is at the discretion of the scheduler which may cancel tile processing
     * in progress and remove tiles from its internal queue, remove tiles from
     * the queue but not terminate current processing, or simply do nothing.
     *
     * <p> In the Sun Microsystems reference implementation of
     * <code>TileScheduler</code> the second tile cancellation option is
     * implemented, i.e., tiles are removed from the internal queue but
     * computation already in progress is not terminated.  If there is at
     * least one worker thread this method should be non-blocking.  Any tiles
     * allowed to complete computation subsequent to this call are complete
     * and will be treated as if they had not been cancelled, e.g., with
     * respect to caching, notification of registered listeners, etc.
     * Furthermore, cancelling a tile request in no way invalidates the tile
     * as a candidate for future recomputation.
     *
     * @param request The request for which tiles are to be cancelled.
     * @param tileIndices The tiles to be cancelled; may be <code>null</code>.
     *        Any tiles not actually in the <code>TileRequest</code> will be
     *        ignored.
     *
     * @throws IllegalArgumentException if <code>request</code> is
     *         <code>null</code>.
     *
     * @since JAI 1.1
     */
    void cancelTiles(TileRequest request, Point[] tileIndices);

    /**
     * Hints to the <code>TileScheduler</code> that the specified tiles from
     * the given <code>PlanarImage</code> might be needed in the near future.
     * Some <code>TileScheduler</code> implementations may spawn a low
     * priority thread to compute the tiles while others may ignore the hint.
     *
     * @param target The <code>OpImage</code> from which to prefetch tiles. 
     * @param tileIndices A list of tile indices indicating which tiles
     *        to prefetch.
     *
     * @throws IllegalArgumentException if <code>target</code> or
     *         <code>tileIndices</code> is <code>null</code>.
     */
    void prefetchTiles(PlanarImage target, Point[] tileIndices);

    /**
     * Suggests to the scheduler the degree of parallelism to use in
     * processing invocations of <code>scheduleTiles()</code>.  For
     * example, this might set the number of threads to spawn.  It is
     * legal to implement this method as a no-op.
     *
     * <p> In the Sun Microsystems reference implementation of TileScheduler
     * this method sets the number of worker threads actually used for tile
     * computation.  Ideally this number should equal the number of processors
     * actually available on the system.  It is the responsibility of the
     * application to set this value as the number of processors is not
     * available via the virtual machine.  A parallelism value of zero
     * indicates that all tile computation will be effected in the primary
     * thread.  A parallelism value of <i>N</i> indicates that there will be
     * <i>N</i> worker threads in addition to the primary scheduler thread.
     * In JAI the parallelism defaults to a value of 2 unless explicity set
     * by the application.
     *
     * @param parallelism The suggested degree of parallelism.
     * @throws IllegalArgumentException if <code>parallelism</code>
     *         is negative.
     *
     * @since JAI 1.1
     */
    void setParallelism(int parallelism);

    /**
     * Returns the degree of parallelism of the scheduler.
     *
     * @since JAI 1.1
     */
    int getParallelism();

    /**
     * Identical to <code>setParallelism()</code> but applies only to
     * <code>prefetchTiles()</code>.
     *
     * @since JAI 1.1
     */
    void setPrefetchParallelism(int parallelism);

    /**
     * Identical to <code>getParallelism()</code> but applies only to
     * <code>prefetchTiles()</code>.
     *
     * @since JAI 1.1
     */
    int getPrefetchParallelism();

    /**
     * Suggests to the scheduler the priority to assign to processing
     * effected by <code>scheduleTiles()</code>.  For example, this might
     * set thread priority.  Values outside of the accepted priority range
     * will be clamped to the nearest extremum.  An implementation may clamp
     * the prefetch priority to less than the scheduling priority.  It is
     * legal to implement this method as a no-op.
     *
     * <p> In the Sun Microsystems reference implementation of TileScheduler
     * this method sets the priority of the worker threads used for tile
     * computation.  Its initial value is <code>Thread.NORM_PRIORITY</code>.
     *
     * @param priority The suggested priority.
     *
     * @since JAI 1.1
     */
    void setPriority(int priority);

    /**
     * Returns the priority of <code>scheduleTiles()</code> processing.
     *
     * @since JAI 1.1
     */
    int getPriority();

    /**
     * Identical to <code>setPriority()</code> but applies only to
     * <code>prefetchTiles()</code>.
     *
     * <p> In the Sun Microsystems reference implementation of
     * <code>TileScheduler</code>, this method sets the priority of any threads
     * spawned to prefetch tiles.  Its initial value is
     * <code>Thread.MIN_PRIORITY</code>.
     *
     * @since JAI 1.1
     */
    void setPrefetchPriority(int priority);

    /**
     * Identical to <code>getPriority()</code> but applies only to
     * <code>prefetchTiles()</code>.
     *
     * @since JAI 1.1
     */
    int getPrefetchPriority();
}

