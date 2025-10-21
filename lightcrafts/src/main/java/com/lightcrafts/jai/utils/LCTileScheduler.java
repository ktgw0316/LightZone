/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2023-     Masahiro Kitagawa */

package com.lightcrafts.jai.utils;

/*
 * $RCSfile: SunTileScheduler.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:02 $
 * $State: Exp $
 */

import org.eclipse.imagen.media.util.ImageUtil;
import org.jetbrains.annotations.NotNull;

import org.eclipse.imagen.*;
import org.eclipse.imagen.util.ImagingException;
import org.eclipse.imagen.util.ImagingListener;
import java.awt.*;
import java.awt.image.Raster;
import java.lang.ref.Cleaner;
import java.math.BigInteger;
import java.util.List;
import java.util.*;
import java.util.concurrent.*;

import static java.lang.Thread.MAX_PRIORITY;
import static java.lang.Thread.MIN_PRIORITY;
import static java.lang.Thread.NORM_PRIORITY;

/**
 * A class representing a request for non-prefetch background computation
 * of tiles.  The object stores the image, the indices of all tiles being
 * requested, and references to all listeners associated with the request.
 *
 * <code>TileRequest</code> methods are not commented.
 */
class Request implements TileRequest {

    private final TileScheduler scheduler;

    final PlanarImage image;
    final List<Point> indices;
    final Set<TileComputationListener> listeners;

    final Hashtable<Point, Integer> tileStatus;

    /**
     * Constructs a <code>Request</code>.
     *
     * @param scheduler The scheduler processing this request.
     * @param image The image for which tiles are being computed.
     * @param tileIndices The indices of the tiles to be computed.
     * @param tileListeners The listeners to be notified of tile
     *        computation, cancellation, or failure.
     *
     * @exception IllegalArgumentException if <code>scheduler</code>,
     *            <code>image</code>, or <code>tileIndices</code> is
     *            <code>null</code> or if <code>tileIndices</code> is
     *            zero-length.
     */
    Request(TileScheduler scheduler,
            PlanarImage image,
            Point[] tileIndices,
            TileComputationListener[] tileListeners) {

        // Save a reference to the scheduler.
        if(scheduler == null) {
            throw new IllegalArgumentException(); // Internal error - no message.
        }
        this.scheduler = scheduler;

        // Save a reference to the image.
        if(image == null) {
            throw new IllegalArgumentException(); // Internal error - no message.
        }
        this.image = image;

        // Ensure there is at least one tile in the request.
        if(tileIndices == null || tileIndices.length == 0) {
            // If this happens it is an internal programming error.
            throw new IllegalArgumentException(); // Internal error - no message.
        }

        // Save the tile indices.
        indices = Arrays.asList(tileIndices);

        // Save references to the listeners, if any.
        if(tileListeners != null) {
            int numListeners = tileListeners.length;
            if(numListeners > 0) {
                listeners = new HashSet<>(numListeners);
                Collections.addAll(listeners, tileListeners);
            } else {
                listeners = null;
            }
        } else {
            listeners = null;
        }

        // Initialize status table.
        tileStatus = new Hashtable<>(tileIndices.length);
    }

    // --- TileRequest implementation ---

    public PlanarImage getImage() {
        return image;
    }

    public Point[] getTileIndices() {
        return indices.toArray(new Point[0]);
    }

    public TileComputationListener[] getTileListeners() {
        return listeners.toArray(new TileComputationListener[0]);
    }

    public boolean isStatusAvailable() {
        return true;
    }

    public int getTileStatus(int tileX, int tileY) {
        Point p = new Point(tileX, tileY);
        return tileStatus.getOrDefault(p, TileRequest.TILE_STATUS_PENDING);
    }

    public void cancelTiles(Point[] tileIndices) {
        // Forward the call to the scheduler.
        scheduler.cancelTiles(this, tileIndices);
    }
}

/** A job to put in a job queue. */
sealed interface Job permits RequestJob, TileJob {
    /** Computes the job required. */
    void compute();

    /** Returns <code>true</code> if the job is not done. */
    boolean notDone();

    /** Returns the image for which tiles are being computed. */
    PlanarImage getOwner();

    /**
     * Returns <code>true</code> if and only if the job should block the
     * thread which processes it.  In this case the scheduler and the
     * processing thread must communicate using <code>wait()</code> and
     * <code>notify()</code>.
     */
    boolean isBlocking();

    /** Returns the first exception encountered or <code>null</code>. */
    Exception getException();
}

/**
 * A <code>Job</code> which computes a single tile at a time for a
 * non-prefetch background job queued by the version of scheduleTiles()
 * which returns a <code>TileRequest</code>.  This <code>Job</code>
 * notifies all <code>TileComputationListener</code>s of all
 * <code>TileRequest</code>s with which this tile is associated of
 * whether the tile was computed or the computation failed.
 */
final class RequestJob implements Job, Comparable<Job>, Runnable {

    final LCTileScheduler scheduler; // the TileScheduler

    final PlanarImage owner;     // the image this tile belongs to
    final int tileX;             // tile's X index
    final int tileY;             // tile's Y index
    final Raster[] tiles;        // the computed tiles
    final int offset;            // offset into arrays

    boolean done = false;        // flag indicating completion status
    Exception exception = null;  // Any exception that might have occurred
                                 // during computeTile

    /** Constructor. */
    RequestJob(LCTileScheduler scheduler,
               PlanarImage owner, int tileX, int tileY,
               Raster[] tiles, int offset) {
        this.scheduler = scheduler;
        this.owner = owner;
        this.tileX = tileX;
        this.tileY = tileY;
        this.tiles = tiles;
        this.offset = offset;
    }

    /**
     * Tile computation. Does the actual call to getTile().
     */
    public void compute() {
        // Get the Request List.
        List<Request> reqList;
        synchronized(scheduler.tileRequests) {
            // Initialize the tile ID.
            Object tileID = LCTileScheduler.tileKey(owner, tileX, tileY);

            // Remove the List of Requests from the request Map.
            reqList = scheduler.tileRequests.remove(tileID);

            // Remove the tile Job from the job Map.
            scheduler.tileJobs.remove(tileID);
        }

        // Check whether reqList is valid in case job was cancelled while
        // blocking on the tileRequests Map above.
        // XXX Do not need empty check in next line?
        if(reqList != null && !reqList.isEmpty()) {
            // Update tile status to "processing".
            Point p = new Point(tileX, tileY);
            Integer tileStatus = TileRequest.TILE_STATUS_PROCESSING;
            for (Request r : reqList) {
                r.tileStatus.put(p, tileStatus);
            }

            try {
                tiles[offset] = owner.getTile(tileX, tileY);
            } catch (Exception e) {
                exception = e;
            } catch (Error e) {
                exception = new Exception(e);
            } finally {
                // Extract the Set of all TileComputationListeners.
                Set<TileComputationListener> listeners = LCTileScheduler.getListeners(reqList);

                // XXX Do not need empty check in next line.
                if(listeners != null && !listeners.isEmpty()) {
                    // Get TileRequests as an array for later use.
                    TileRequest[] requests = reqList.toArray(new TileRequest[0]);

                    // Update tile status as needed.
                    tileStatus = exception == null ?
                                 TileRequest.TILE_STATUS_COMPUTED :
                                 TileRequest.TILE_STATUS_FAILED;
                    for (TileRequest r : requests) {
                        ((Request)r).tileStatus.put(p, tileStatus);
                    }

                    // Notify listeners.
                    if(exception == null) {
                        // Tile computation successful.
                        for (TileComputationListener listener : listeners) {
                            listener.tileComputed(scheduler, requests,
                                                  owner, tileX, tileY,
                                                  tiles[offset]);
                        }
                    } else {
                        // Tile computation unsuccessful.
                        for (TileComputationListener listener : listeners) {
                            listener.tileComputationFailure(scheduler, requests,
                                                            owner, tileX, tileY,
                                                            exception);
                        }
                    }
                }
            }
        }

        // Set the flag indicating job completion.
        done = true;
    }

    /**
     * Returns <code>true</code> if the job is not done; that is,
     * the tile is not computed and no exceptions have occurred.
     */
    public boolean notDone() {
        return !done;
    }

    /** Returns the image for which the tile is being computed. */
    public PlanarImage getOwner() {
        return owner;
    }

    /** Always returns <code>true</code>. */
    public boolean isBlocking() {
        // Big Change: this should prevent enqueueing of new tiles while an image is being processed
        return true;
    }

    /** Returns any encountered exception or <code>null</code>. */
    public Exception getException() {
        return exception;
    }

    /** Returns a string representation of the class object. */
    public String toString() {
        String tString = "null";
        if (tiles[offset] != null) {
            tString = tiles[offset].toString();
        }
        return getClass().getName() + "@" + Integer.toHexString(hashCode()) +
               ": owner = " + owner.toString() +
               " tileX = " + Integer.toString(tileX) +
               " tileY = " + Integer.toString(tileY) +
               " tile = " + tString;
    }

    @Override
    public int compareTo(@NotNull Job o) {
        if (o instanceof TileJob) {
            return 1;
        }
        return 0;
    }

    @Override
    public void run() {
        compute();

        synchronized (scheduler) {
            scheduler.notify();
        }
    }
}

/**
 * A <code>Job</code> which computes one or more tiles at a time for either
 * a prefetch job or a blocking job.
 */
final class TileJob implements Job, Comparable<Job>, Runnable {

    final LCTileScheduler scheduler; // the TileScheduler

    final boolean isBlocking;  // whether the job is blocking
    final PlanarImage owner;   // the image this tile belongs to
    final Point[] tileIndices; // the tile indices
    final Raster[] tiles;      // the computed tiles
    final int offset;          // offset into arrays
    final int numTiles;        // number of elements to use in indices array

    boolean done = false;       // flag indicating completion status
    Exception exception = null; // The first exception that might have
                                // occurred during computeTile

    /** Constructor. */
    TileJob(LCTileScheduler scheduler, boolean isBlocking,
            PlanarImage owner, Point[] tileIndices,
            Raster[] tiles, int offset, int numTiles) {
        this.scheduler = scheduler;
        this.isBlocking = isBlocking;
        this.owner = owner;
        this.tileIndices = tileIndices;
        this.tiles = tiles;
        this.offset = offset;
        this.numTiles = numTiles;
    }

    /**
     * Tile computation. Does the actual calls to getTile().
     */
    public void compute() {
        exception = scheduler.compute(owner, tileIndices, tiles,
                                      offset, numTiles, null);
        done = true;
    }

    /**
     * Returns <code>true</code> if the job is not done; that is,
     * the tile is not computed and no exceptions have occurred.
     */
    public boolean notDone() {
        return !done;
    }

    /** Returns the image for which tiles are being computed. */
    public PlanarImage getOwner() {
        return owner;
    }

    /** Returns <code>true</code> if and only if there is a listener. */
    public boolean isBlocking() {
        return isBlocking;
    }

    /** Returns any encountered exception or <code>null</code>. */
    public Exception getException() {
        return exception;
    }

    @Override
    public int compareTo(@NotNull Job o) {
        if (o instanceof RequestJob) {
            return -1;
        }
        return 0;
    }

    @Override
    public void run() {
        compute();

        // Notify the scheduler only if the Job is blocking.
        if (isBlocking) {
            synchronized (scheduler) {
                scheduler.notify();
            }
        }
    }
}

/**
 * This is Sun Microsystems' reference implementation of the
 * <code>org.eclipse.imagen.TileScheduler</code> interface.  It provides
 * a mechanism for scheduling tile calculation.  Multi-threading is
 * used whenever possible.
 *
 * @see org.eclipse.imagen.TileScheduler
 */
public final class LCTileScheduler implements TileScheduler {

    /** The default number of worker threads. */
    private static final int NUM_THREADS_DEFAULT = 2;

    /** The default number of prefetch threads. */
    private static final int NUM_PREFETCH_THREADS_DEFAULT = 1;

    @NotNull
    private final ThreadPoolExecutor executor;

    @NotNull
    private final ThreadPoolExecutor prefetchExecutor;

    private static final Cleaner cleaner = Cleaner.create();

    /** The worker thread priority. */
    private int priority = NORM_PRIORITY;

    /** The prefetch thread priority. */
    private int prefetchPriority = MIN_PRIORITY;

    /**
     * <code>Map</code> of tiles currently being computed.  The key is
     * created from the image and tile indices by the <code>tileKey()</code>
     * method.  Each key is mapped to an <code>Object[1]</code> which may
     * contain <code>null</code>, a <code>Raster</code>, or an indefinite
     * <code>Object</code> which represent, respectively, that the tile is
     * being computed, the tile itself, and that the tile computation failed.
     */
    private final Map<Object, Object[]> tilesInProgress = new HashMap<>();

    /**
     * <code>Map</code> of tiles to <code>Request</code>s.  The key is
     * created from the image and tile indices by the <code>tileKey()</code>
     * method.  Each key is mapped to a <code>List</code> of
     * <code>Request</code> for the tile.  If there is no mapping for the
     * tile, then there are no current requests.  If a mapping exists, it
     * should always be non-null and the <code>List</code> value should
     * have size of at least unity.
     */
    final Map<Object, List<Request>> tileRequests = new HashMap<>();

    /**
     * <code>Map</code> of tiles to <code>Job</code>s.The key is
     * created from the image and tile indices by the <code>tileKey()</code>
     * method.  Each key is mapped to a <code>Job</code> for the tile.  If
     * there is no mapping for the tile, then there is no enqueued
     * <code>RequestJob</code>.
     */
    Map<Object, Job> tileJobs = new HashMap<>();

    /**
     * Returns the hash table "key" as a <code>Object</code> for this
     * tile.  For <code>PlanarImage</code> and
     * <code>SerializableRenderedImage</code>, the key is generated by
     * the method <code>ImageUtilgenerateID(Object) </code>.  For the
     * other cases, a <code>Long</code> object is returned.
     * The upper 32 bits for this <code>Long</code> is the tile owner's
     * hash code, and the lower 32 bits is the tile's index.
     */
    static Object tileKey(PlanarImage owner, int tileX, int tileY) {
        long idx = tileY * (long)owner.getNumXTiles() + tileX;

        BigInteger imageID = (BigInteger)owner.getImageID();
        byte[] buf = imageID.toByteArray();
        int length = buf.length;
        byte[] buf1 = new byte[length + 8];
        System.arraycopy(buf, 0, buf1, 0, length);
        for (int i = 7, j = 0; i >= 0; i--, j += 8)
           buf1[length++] = (byte)(idx >> j);
        return new BigInteger(buf1);
    }

    /**
     * Returns all <code>TileComputationListener</code>s for the supplied
     * <code>List</code> of <code>Request</code>s.
     */
    static Set<TileComputationListener> getListeners(List<Request> reqList) {
        // Extract the Set of all TileComputationListeners.
        HashSet<TileComputationListener> listeners = null;
        for (Request req : reqList) {
            // XXX Do not need empty check in next line.
            if (req.listeners != null && !req.listeners.isEmpty()) {
                if (listeners == null) {
                    listeners = new HashSet<>();
                }
                listeners.addAll(req.listeners);
            }
        }

        return listeners;
    }

    /**
     * Creates a <code>ThreadFactory</code> with the specified priority.
     */
    private ThreadFactory threadFactory(int priority) {
        return r -> {
            Thread thread = new Thread(r);
            thread.setPriority(priority);
            return thread;
        };
    }

    /**
     * Constructor.
     *
     * @param parallelism  The number of worker threads to do tile computation.
     *        If this number is less than 1, no multi-threading is used.
     * @param priority  The priority of worker threads.
     * @param prefetchParallelism  The number of threads to do prefetching.
     *        If this number is less than 1, no multi-threading is used.
     * @param prefetchPriority  The priority of prefetch threads.
     */
    public LCTileScheduler(int parallelism, int priority,
                           int prefetchParallelism, int prefetchPriority) {
        // Create queues and set parallelism and priority to default values.
        executor = new ThreadPoolExecutor(
                parallelism, parallelism,
                0L, TimeUnit.MICROSECONDS, new PriorityBlockingQueue<>(),
                threadFactory(priority));
        prefetchExecutor = new ThreadPoolExecutor(
                prefetchParallelism, prefetchParallelism,
                0L, TimeUnit.MICROSECONDS, new LinkedBlockingQueue<>(),
                threadFactory(prefetchPriority));
        setPriority(priority);
        setPrefetchPriority(prefetchPriority);

        cleaner.register(this, terminate(prefetchExecutor, executor));
    }

    /**
     * Constructor.  Processing and prefetch queues are created and all
     * parallelism and priority values are set to default values.
     */
    public LCTileScheduler() {
        this(NUM_THREADS_DEFAULT, NORM_PRIORITY, NUM_PREFETCH_THREADS_DEFAULT, MIN_PRIORITY);
    }

    /**
     * Constructor.
     *
     * @param parallelism  The number of worker threads to do tile computation.
     *        If this number is less than 1, no multi-threading is used.
     * @param priority  The priority of worker threads.
     */
    public LCTileScheduler(int parallelism, int priority) {
        this(parallelism, priority, parallelism, priority);
    }

    /**
     * Tile computation. Does the actual calls to getTile().
     */
    Exception compute(PlanarImage owner, Point[] tileIndices,
                      Raster[] tiles, int offset, int numTiles,
                      Request request) {
        Exception exception = null;

        int j = offset;
        if(request == null || request.listeners == null) {
            for(int i = 0; i < numTiles; i++, j++) {
                final Point p = tileIndices[j];

                try {
                    tiles[j] = owner.getTile(p.x, p.y);
                } catch (Exception e) {
                    exception = e;

                    // Abort the remaining tiles in the job.
                    break;
                }
            }
        } else { // listeners present
            final Request[] reqs = new Request[] {request};
            for(int i = 0; i < numTiles; i++, j++) {
                final Point p = tileIndices[j];

                // Update tile status to "processing".
                int tileStatus = TileRequest.TILE_STATUS_PROCESSING;
                request.tileStatus.put(p, tileStatus);

                try {
                    tiles[j] = owner.getTile(p.x, p.y);
                    for (TileComputationListener listener : request.listeners) {
                        // Update tile status to "computed".
                        tileStatus = TileRequest.TILE_STATUS_COMPUTED;
                        request.tileStatus.put(p, tileStatus);

                        listener.tileComputed(this,
                                              reqs,
                                              owner,
                                              p.x, p.y,
                                              tiles[j]);
                    }
                } catch (Exception e) {
                    exception = e;

                    // Abort the remaining tiles in the job.
                    break;
                }
            }
        }

        // If an exception occurred, notify listeners that all remaining
        // tiles in the job have failed.
        if(exception != null && request != null && request.listeners != null) {
            final int lastOffset = j;
            final int numFailed = numTiles - (lastOffset - offset);

            // Mark all tiles starting with the one which generated the
            // Exception as "failed".
            for(int i = 0, k = lastOffset; i < numFailed; i++) {
                Integer tileStatus = TileRequest.TILE_STATUS_FAILED;
                request.tileStatus.put(tileIndices[k++], tileStatus);
            }

            // Notify listeners.
            Request[] reqs = new Request[] {request};
            for(int i = 0, k = lastOffset; i < numFailed; i++) {
                Point p = tileIndices[k++];
                for (TileComputationListener listener : request.listeners) {
                    listener.tileComputationFailure(this, reqs,
                                                    owner, p.x, p.y,
                                                    exception);
                }
            }
        }

        return exception;
    }

    /**
     * Schedules a single tile for computation.
     *
     * @param owner  The image the tiles belong to.
     * @param tileX  The tile's X index.
     * @param tileY  The tile's Y index.
     *
     * @exception IllegalArgumentException if <code>owner</code> is
     * <code>null</code>.
     *
     * @return  The computed tile
     */
    //
    // This method blocks on the 'tilesInProgress' Map to avoid simultaneous
    // computation of the same tile in two or more different threads. The idea
    // is to release the resources of all but one thread so that the computation
    // occurs more quickly. The synchronization variable is an Object[] of length
    // unity. The computed tile is passed from the computing thread to the
    // waiting threads via the contents of this Object[]. Thus this method does
    // not depend on the TileCache to transfer the data.
    //
    @Override
    public Raster scheduleTile(OpImage owner,
                               int tileX,
                               int tileY) {
        if (owner == null) {
            throw new IllegalArgumentException("Null owner");
        }

        // Eventual tile to be returned.
        Raster tile = null;

        // Get the tile's unique ID.
        final Object tileID = tileKey(owner, tileX, tileY);

        // Set the computation flag and initialize or retrieve the tile cache.
        final boolean computeTile;
        final Object[] cache;
        synchronized(tilesInProgress) {
            computeTile = !tilesInProgress.containsKey(tileID);
            if(computeTile) {
                // Computing: add tile ID to the map.
                cache = new Object[1];
                tilesInProgress.put(tileID, cache);
            } else {
                // Waiting: get tile cache from the Map.
                cache = tilesInProgress.get(tileID);
            }
        }

        if(computeTile) {
            try {
                try {
                    // Attempt to compute the tile.
                    tile = owner.computeTile(tileX, tileY);
                } catch (OutOfMemoryError e) {
                    // Free some space in cache
                    TileCache tileCache = owner.getTileCache();
                    if(tileCache != null) {
                        tileCache.removeTiles(owner);
                    }
                    try {
                        // Re-attempt to compute the tile.
                        tile = owner.computeTile(tileX, tileY);
                    } catch (OutOfMemoryError e1) {
                        // Empty the cache
                        if(tileCache != null) {
                            tileCache.flush();
                        }
                    }

                    // Re-attempt to compute the tile.
                    tile = owner.computeTile(tileX, tileY);
                }
            } catch(Throwable e) {
                // Re-throw the Error or Exception.
                if(e instanceof Error) {
                    throw (Error)e;
                } else {
                    sendExceptionToListener("RuntimeException", e);
                }
            } finally {
                // Always set the cached tile to a non-null value.
                cache[0] = tile != null ? tile : new Object();

                // Notify the thread(s).
//                cache.notifyAll();

                // Remove the tile ID from the Map.
                tilesInProgress.remove(tileID);
            }
        } else {
            // Check the cache: a null value indicates computation is
            // still in progress.
            while (cache[0] == null) {
                // Wait for the computation to complete.
                try {
                    cache.wait(); // XXX Should there be a timeout?
                } catch(Exception e) {
                    // XXX What response here?
                }
            }

            // Set the result only if cache contains a Raster.
            if(cache[0] instanceof Raster) {
                tile = (Raster)cache[0];
            } else {
                throw new RuntimeException("Not a Raster instance?");
            }
        }

        return tile;
    }

    /**
     * General purpose method for job creation and queueing.  Note that
     * the returned value should be ignored if the <code>listener</code>
     * parameter is non-<code>null</code>.
     *
     * @param owner The image for which tile computation jobs will be queued.
     * @param tileIndices The indices of the tiles to be computed.
     * @param isPrefetch Whether the operation is a prefetch.
     * @param listeners A <code>TileComputationListener</code> of the
     *        processing.  May be <code>null</code>.
     *
     * @return The computed tiles.  This value is meaningless if
     *         <code>listener</code> is non-<code>null</code>.
     */
    // The allowable arguments are constrained as follows:
    // A) owner and tileIndices non-null.
    // B) (isBlocking,isPrefetch) in {(true,false),(false,false),(false,true)}
    // C) listeners != null <=> (isBlocking,isPrefetch) == (false,false)
    // The returned value is one of:
    // Raster[] <=> (isBlocking,isPrefetch) == (true,false)
    // Integer <=> (isBlocking,isPrefetch) == (false,false)
    // (Raster[])null <=> (isBlocking,isPrefetch) == (false,true)
    private Object scheduleJob(PlanarImage owner,
                               Point[] tileIndices,
                               boolean isBlocking,
                               boolean isPrefetch,
                               TileComputationListener[] listeners) {
        if(owner == null || tileIndices == null) {
            // null parameters
            throw new IllegalArgumentException(); // coding error - no message
        } else if((isBlocking || isPrefetch) && listeners != null) {
            // listeners for blocking or prefetch job
            throw new IllegalArgumentException(); // coding error - no message
        } else if(isBlocking && isPrefetch) {
            throw new IllegalArgumentException(); // coding error - no message
        }

        int numTiles = tileIndices.length;
        final Raster[] tiles = new Raster[numTiles];

        final var jobExecutor = isPrefetch ? prefetchExecutor : executor;
        final int numThreads = jobExecutor.getPoolSize();
        assert numThreads > 0;

        final Job[] jobs = new Job[numTiles];

        Object returnValue = tiles;

        if(!isBlocking && !isPrefetch) {
            final var request = new Request(this, owner, tileIndices, listeners);

            // Override return value.
            returnValue = request;

            // Queue all tiles as single-tile jobs.
            for (int i = 0; i < numTiles; i++) {
                final Point p = tileIndices[i];
                final Object tileID = tileKey(owner, p.x, p.y);

                synchronized(tileRequests) {
                    List<Request> reqList = tileRequests.get(tileID);
                    final boolean isAlreadyQueued = reqList != null;
                    if (isAlreadyQueued) {
                        reqList.add(request);
                        numTiles--;
                    } else {
                        reqList = new ArrayList<>();
                        reqList.add(request);
                        tileRequests.put(tileID, reqList);

                        jobs[i] = new RequestJob(this, owner,
                                p.x, p.y, tiles, i);
                        tileJobs.put(tileID, jobs[i]);
                        addJob(jobs[i], false);
                    }
                }
            }
        } else {
            for (int i = 0; i < numTiles; i++) {
                jobs[i] = new TileJob(this, isBlocking, owner,
                        tileIndices, tiles, i, 1);
                addJob(jobs[i], isPrefetch);
            }
        }

        // If blocking, wait until all tiles have been computed.
        // There is no 'else' block for non-blocking as in that
        // case we just want to continue.
        if(isBlocking) {
            for (final var job : jobs) {
                synchronized(this) {
                    while (job.notDone()) {
                        try {
                            wait();
                        } catch(InterruptedException ie) {
                            // Ignore: should never happen.
                        }
                    }
                }

                // XXX: should we re-throw the exception or
                //      should we reschedule this job ?? krishnag
                Exception e = job.getException();

                if (e != null) {
                    // Throw a RuntimeException with the Exception's
                    // message concatenated with the stack trace.
                    String message = "Exception while scheduling tiles: ";
                    sendExceptionToListener(message,
                            new ImagingException(message, e));
                }
            }
        }

        return returnValue;
    }

    /**
     * Schedules multiple tiles of an image for computation.
     *
     * @param owner  The image the tiles belong to.
     * @param tileIndices  An array of tile X and Y indices.
     *
     * @return  An array of computed tiles.
     */
    @Override
    public Raster[] scheduleTiles(OpImage owner,
                                  Point[] tileIndices) {
        if (owner == null || tileIndices == null) {
            throw new IllegalArgumentException("Null owner or TileIndices");
        }
        return (Raster[])scheduleJob(owner, tileIndices, true, false, null);
    }

    /**
     * Schedule a list of tiles for computation.  The supplied listeners
     * will be notified after each tile has been computed.  This
     * method ideally should be non-blocking. If the <code>TileScheduler</code>
     * implementation uses multithreading, it is at the discretion of the
     * implementation which thread invokes the
     * <code>TileComputationListener</code> methods.
     */
    @Override
    public TileRequest scheduleTiles(PlanarImage target, Point[] tileIndices,
                                     TileComputationListener[] tileListeners) {
        if (target == null || tileIndices == null) {
            throw new IllegalArgumentException("Null owner or TileIndices");
        }
        return (TileRequest)scheduleJob(target, tileIndices, false, false,
                                        tileListeners);
    }

    /**
     * Issues an advisory cancellation request to the
     * <code>TileScheduler</code> stating that the indicated tiles of the
     * specified image should not be processed.  The handling of this request
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
     */
    @Override
    public void cancelTiles(TileRequest request, Point[] tileIndices) {
        if(request == null) {
            throw new IllegalArgumentException("Null TileRequest");
        }

        Request req = (Request)request;
        synchronized(tileRequests) {
            // Save the list of all tile indices in this request.
            List<Point> reqIndexList = req.indices;

            // Initialize the set of tile indices to cancel.
            Point[] indices;
            if(tileIndices != null && tileIndices.length > 0) {
                // Create a Set from the supplied indices.
                List<Point> tileIndexList = Arrays.asList(tileIndices);

                // Retain only indices which were actually in the request.
                tileIndexList.retainAll(reqIndexList);

                indices = tileIndexList.toArray(new Point[0]);
            } else {
                indices = reqIndexList.toArray(new Point[0]);
            }

            // Cache status value.
            Integer tileStatus = TileRequest.TILE_STATUS_CANCELLED;

            // Loop over tile indices to be cancelled.
            for (Point p : indices) {
                // Get the tile's ID.
                Object tileID = tileKey(req.image, p.x, p.y);

                // Get the list of requests for this tile.
                List<Request> reqList = tileRequests.get(tileID);

                // If there are none, proceed to next index.
                if(reqList == null) {
                    continue;
                }

                // Remove this Request from the Request List for this tile.
                reqList.remove(req);

                // If the request list is now empty, dequeue the job and
                // remove the tile from the hashes.
                if(reqList.isEmpty()) {
                    Object job = tileJobs.remove(tileID);
                    if(job != null) {
                        executor.remove((Runnable) job);
                    }
                    tileRequests.remove(tileID);
                }

                // Update tile status to "cancelled".
                req.tileStatus.put(p, tileStatus);

                // Notify any listeners.
                if(req.listeners != null) {
                    TileRequest[] reqArray = new TileRequest[]{req};
                    for (TileComputationListener listener : req.listeners) {
                        listener.tileCancelled(this, reqArray,
                                req.image, p.x, p.y);
                    }
                }
            }
        }
    }

    /**
     * Prefetches a list of tiles of an image.
     *
     * @param owner  The image the tiles belong to.
     * @param tileIndices  An array of tile X and Y indices.
     */
    @Override
    public void prefetchTiles(PlanarImage owner,
                              Point[] tileIndices) {
        if(owner == null || tileIndices == null) {
            throw new IllegalArgumentException("Null owner or TileIndices");
        }
        scheduleJob(owner, tileIndices, false, true, null);
    }

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
     * In JAI the parallelism defaults to a value of 2 unless explicitly set
     * by the application.
     *
     * @param parallelism The suggested degree of parallelism.
     * @throws IllegalArgumentException if <code>parallelism</code>
     *         is negative.
     */
    @Override
    public void setParallelism(int parallelism) {
        if (parallelism < 0) {
            throw new IllegalArgumentException("Negative Parallelism?");
        }
        // Create queues and set parallelism and priority to default values.
        executor.setCorePoolSize(parallelism);
        executor.setMaximumPoolSize(parallelism);
    }

    /**
     * Returns the degree of parallelism of the scheduler.
     */
    @Override
    public int getParallelism() {
        return executor.getPoolSize();
    }

    /**
     * Identical to <code>setParallelism()</code> but applies only to
     * <code>prefetchTiles()</code>.
     */
    @Override
    public void setPrefetchParallelism(int parallelism) {
        if (parallelism < 0) {
            throw new IllegalArgumentException("Negative Parallelism?");
        }
        prefetchExecutor.setCorePoolSize(parallelism);
        prefetchExecutor.setMaximumPoolSize(parallelism);
    }

    /**
     * Identical to <code>getParallelism()</code> but applies only to
     * <code>prefetchTiles()</code>.
     */
    @Override
    public int getPrefetchParallelism() {
        return prefetchExecutor.getPoolSize();
    }

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
     */
    @Override
    public void setPriority(int priority) {
        this.priority = Math.max(Math.min(priority, MAX_PRIORITY),
                                 MIN_PRIORITY);
    }

    /**
     * Returns the priority of <code>scheduleTiles()</code> processing.
     */
    @Override
    public int getPriority() {
        return priority;
    }

    /**
     * Identical to <code>setPriority()</code> but applies only to
     * <code>prefetchTiles()</code>.
     *
     * <p> In the Sun Microsystems reference implementation of
     * <code>TileScheduler</code>, this method sets the priority of any threads
     * spawned to prefetch tiles.  Its initial value is
     * <code>Thread.MIN_PRIORITY</code>.
     */
    public void setPrefetchPriority(int priority) {
        prefetchPriority = Math.max(Math.min(priority, MAX_PRIORITY), MIN_PRIORITY);
    }

    /**
     * Identical to <code>getPriority()</code> but applies only to
     * <code>prefetchTiles()</code>.
     */
    @Override
    public int getPrefetchPriority() {
        return prefetchPriority;
    }

    /** Append a job to the appropriate queue. */
    private void addJob(Job job, boolean isPrefetch) {
        if (job == null) {
            // Programming error: deliberately no message.
            throw new IllegalArgumentException();
        }

        if (isPrefetch) {
            prefetchExecutor.execute((Runnable) job);
        } else {
            executor.execute((Runnable) job);
        }
    }

    /** Queue WorkerThread.TERMINATEs to all workers. */
    private static @NotNull Runnable terminate(ThreadPoolExecutor... executors) {
        return () -> Arrays.stream(executors).forEach(ThreadPoolExecutor::shutdownNow);
    }

    void sendExceptionToListener(String message, Throwable e) {
        ImagingListener listener =
            ImageUtil.getImagingListener((RenderingHints)null);
        listener.errorOccurred(message, e, this, false);
    }
}
