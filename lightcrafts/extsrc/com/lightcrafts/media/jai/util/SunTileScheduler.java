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
package com.lightcrafts.media.jai.util;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.image.Raster;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import com.lightcrafts.mediax.jai.OpImage;
import com.lightcrafts.mediax.jai.PlanarImage;
import com.lightcrafts.mediax.jai.TileCache;
import com.lightcrafts.mediax.jai.TileComputationListener;
import com.lightcrafts.mediax.jai.TileRequest;
import com.lightcrafts.mediax.jai.TileScheduler;
import com.lightcrafts.mediax.jai.util.ImagingException;
import com.lightcrafts.mediax.jai.util.ImagingListener;
import com.lightcrafts.media.jai.util.ImageUtil;

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
    final List indices;
    final Set listeners;

    final Hashtable tileStatus;

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
                listeners = new HashSet(numListeners);
                for(int i = 0; i < numListeners; i++) {
                    listeners.add(tileListeners[i]);
                }
            } else {
                listeners = null;
            }
        } else {
            listeners = null;
        }

        // Initialize status table.
        tileStatus = new Hashtable(tileIndices.length);
    }

    // --- TileRequest implementation ---

    public PlanarImage getImage() {
        return image;
    }

    public Point[] getTileIndices() {
        return (Point[])indices.toArray(new Point[0]);
    }

    public TileComputationListener[] getTileListeners() {
        return (TileComputationListener[])
            listeners.toArray(new TileComputationListener[0]);
    }

    public boolean isStatusAvailable() {
        return true;
    }

    public int getTileStatus(int tileX, int tileY) {
        Point p = new Point(tileX, tileY);

        int status;
        if(tileStatus.containsKey(p)) {
            status = ((Integer)tileStatus.get(p)).intValue();
        } else {
            status = TileRequest.TILE_STATUS_PENDING;
        }

        return status;
    }

    public void cancelTiles(Point[] tileIndices) {
        // Forward the call to the scheduler.
        scheduler.cancelTiles(this, tileIndices);
    }
}

/** A job to put in a job queue. */
interface Job {
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
final class RequestJob implements Job {

    final SunTileScheduler scheduler; // the TileScheduler

    final PlanarImage owner;	 // the image this tile belongs to
    final int tileX;		 // tile's X index
    final int tileY;		 // tile's Y index
    final Raster[] tiles;        // the computed tiles
    final int offset;            // offset into arrays

    boolean done = false;        // flag indicating completion status
    Exception exception = null;	 // Any exception that might have occured
				 // during computeTile

    /** Constructor. */
    RequestJob(SunTileScheduler scheduler,
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
        List reqList;
        synchronized(scheduler.tileRequests) {
            // Initialize the tile ID.
            Object tileID = SunTileScheduler.tileKey(owner, tileX, tileY);

            // Remove the List of Requests from the request Map.
            reqList = (List)scheduler.tileRequests.remove(tileID);

            // Remove the tile Job from the job Map.
            scheduler.tileJobs.remove(tileID);
        }

        // Check whether reqList is valid in case job was cancelled while
        // blocking on the tileRequests Map above.
        // XXX Do not need empty check in next line?
        if(reqList != null && !reqList.isEmpty()) {
            // Update tile status to "processing".
            Point p = new Point(tileX, tileY);
            Integer tileStatus = new Integer(TileRequest.TILE_STATUS_PROCESSING);
            Iterator reqIter = reqList.iterator();
            while(reqIter.hasNext()) {
                Request r = (Request)reqIter.next();
                r.tileStatus.put(p, tileStatus);
            }

            try {
                tiles[offset] = owner.getTile(tileX, tileY);
            } catch (Exception e) {
                exception = e;
            } finally {
                // Extract the Set of all TileComputationListeners.
                int numReq = reqList.size();
                Set listeners = SunTileScheduler.getListeners(reqList);

                // XXX Do not need empty check in next line.
                if(listeners != null && !listeners.isEmpty()) {
                    // Get TileRequests as an array for later use.
                    TileRequest[] requests =
                        (TileRequest[])reqList.toArray(new TileRequest[0]);

                    // Update tile status as needed.
                    tileStatus = new Integer(exception == null ?
                                             TileRequest.TILE_STATUS_COMPUTED :
                                             TileRequest.TILE_STATUS_FAILED);
                    for(int i = 0; i < numReq; i++) {
                        ((Request)requests[i]).tileStatus.put(p, tileStatus);
                    }

                    // Create an Iterator over the listeners.
                    Iterator iter = listeners.iterator();

                    // Notify listeners.
                    if(exception == null) {
                        // Tile computation successful.
                        while(iter.hasNext()) {
                            TileComputationListener listener =
                                (TileComputationListener)iter.next();
                            listener.tileComputed(scheduler, requests,
                                                  owner, tileX, tileY,
                                                  tiles[offset]);
                        }
                    } else {
                        // Tile computation unsuccessful.
                        while(iter.hasNext()) {
                            TileComputationListener listener =
                                (TileComputationListener)iter.next();
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
        return false;
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
}

/**
 * A <code>Job</code> which computes one or more tiles at a time for either
 * a prefetch job or a blocking job.
 */
final class TileJob implements Job {

    final SunTileScheduler scheduler; // the TileScheduler

    final boolean isBlocking;  // whether the job is blocking
    final PlanarImage owner;   // the image this tile belongs to
    final Point[] tileIndices; // the tile indices
    final Raster[] tiles;      // the computed tiles
    final int offset;          // offset into arrays
    final int numTiles;        // number of elements to use in indices array

    boolean done = false;       // flag indicating completion status
    Exception exception = null;	// The first exception that might have
				// occured during computeTile

    /** Constructor. */
    TileJob(SunTileScheduler scheduler, boolean isBlocking,
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
}

/**
 * Worker thread that takes jobs from the tile computation queue and does
 * the actual computation.
 */
class WorkerThread extends Thread {

    /** <code>Object</code> indicating the the thread should exit. */
    public static final Object TERMINATE = new Object();

    /** The scheduler that spawned this thread. */
    SunTileScheduler scheduler;

    /** Whether this is a prefetch thread. */
    boolean isPrefetch;

    /** Constructor. */
    public WorkerThread(ThreadGroup group,
			SunTileScheduler scheduler,
			boolean isPrefetch) {
	super(group, group.getName() + group.activeCount());
        this.scheduler = scheduler;
        this.isPrefetch = isPrefetch;

        setDaemon(true);
        start();
    }

    /** Does the tile computation. */
    public void run() {
        LinkedList jobQueue = scheduler.getQueue(isPrefetch);

        while(true) {
            Object dequeuedObject = null;

            // Check the job queue.
            synchronized(jobQueue) {
                if(jobQueue.size() > 0) {
                    // Remove the first job.
                    dequeuedObject = jobQueue.removeFirst();
                } else {
                    try {
                        // Wait for a notify() on the queue.
                        jobQueue.wait();
                        continue;
                    } catch(InterruptedException ie) {
                        // Ignore: should never happen.
                    }
                }
            }
	    
            if(dequeuedObject == TERMINATE || 
		getThreadGroup() == null || getThreadGroup().isDestroyed()) {
                // Remove WorkerThread from appropriate Vector.
                Vector threads;
                synchronized(threads = scheduler.getWorkers(isPrefetch)) {
                    threads.remove(this);
                }

                // Exit the thread.
                return;
            }

            Job job = (Job)dequeuedObject;

            // Execute tile job.
            if (job != null) {
		job.compute();

		// Notify the scheduler only if the Job is blocking.
		if(job.isBlocking()) {
		    synchronized(scheduler) {
			scheduler.notify();
		    }
		}
	    }
        } // infinite loop
    }
}

/**
 * This is Sun Microsystems' reference implementation of the
 * <code>com.lightcrafts.mediax.jai.TileScheduler</code> interface.  It provides
 * a mechanism for scheduling tile calculation.  Multi-threading is
 * used whenever possible.
 *
 * @see com.lightcrafts.mediax.jai.TileScheduler
 */
public final class SunTileScheduler implements TileScheduler {

    /** The default number of worker threads. */
    private static final int NUM_THREADS_DEFAULT = 2;

    /** The default number of worker threads. */
    private static final int NUM_PREFETCH_THREADS_DEFAULT = 1;

    /** The instance counter.  It is used to compose the name of the
     *  ThreadGroup.
     */
    private static int numInstances = 0;

    /** The tile schedular name.  It is used to compose the name of the
     *  ThreadGroup.
     */
    private static String name = JaiI18N.getString("SunTileSchedulerName");

    /** The root ThreadGroup, which holds two sub-groups:
     * the ThreadGroup for the standard jobs, and the ThreadGroup for
     * the prefetch jobs.
     */
    private ThreadGroup rootGroup;

    /** The ThreadGroup contains all the standard jobs. */
    private ThreadGroup standardGroup;

    /** The ThreadGroup contains all the prefetch jobs. */
    private ThreadGroup prefetchGroup;

    /** The worker thread parallelism. */
    private int parallelism = NUM_THREADS_DEFAULT;

    /** The processing thread parallelism. */
    private int prefetchParallelism = NUM_PREFETCH_THREADS_DEFAULT;

    /** The worker thread priority. */
    private int priority = Thread.NORM_PRIORITY;

    /** The prefetch thread priority. */
    private int prefetchPriority = Thread.MIN_PRIORITY;

    /** A job queue for tiles waiting to be computed by the worker threads. */
    private LinkedList queue = null;

    /** A job queue for tiles waiting to be computed by prefetch workers. */
    private LinkedList prefetchQueue = null;

    /**
     * A <code>Vector</code> of <code>WorkerThread</code>s that persist
     * to do the actual tile computation for normal processing.  This
     * variable should never be set to <code>null</code>.
     */
    private Vector workers = new Vector();

    /**
     * A <code>Vector</code> of <code>WorkerThread</code>s that persist
     * to do the actual tile computation for prefetch processing.  This
     * variable should never be set to <code>null</code>.
     */
    private Vector prefetchWorkers = new Vector();

    /**
     * The effective number of worker threads; may differ from
     * <code>workers.size()</code> due to latency.  This value should
     * equal the size of <code>workers</code> less the number of
     * <code>WorkerThread.TERMINATE</code>s in <code>queue</code>.
     */
    private int numWorkerThreads = 0;

    /**
     * The effective number of prefetch worker threads; may differ from
     * <code>prefetchWorkers.size()</code> due to latency.  This value should
     * equal the size of <code>prefetchWorkers</code> less the number of
     * <code>WorkerThread.TERMINATE</code>s in <code>prefetchQueue</code>.
     */
    private int numPrefetchThreads = 0;

    /**
     * <code>Map</code> of tiles currently being computed.  The key is
     * created from the image and tile indices by the <code>tileKey()</code>
     * method.  Each key is mapped to an <code>Object[1]</code> which may
     * contain <code>null</code>, a <code>Raster</code>, or an indefinite
     * <code>Object</code> which represent, respectively, that the tile is
     * being computed, the tile itself, and that the tile computation failed.
     */
    private Map tilesInProgress = new HashMap();

    /**
     * <code>Map</code> of tiles to <code>Request</code>s.  The key is
     * created from the image and tile indices by the <code>tileKey()</code>
     * method.  Each key is mapped to a <code>List</code> of
     * <code>Request</code> for the tile.  If there is no mapping for the
     * tile, then there are no current requests.  If a mapping exists, it
     * should always be non-null and the <code>List</code> value should
     * have size of at least unity.
     */
    Map tileRequests = new HashMap();

    /**
     * <code>Map</code> of tiles to <code>Job</code>s.The key is
     * created from the image and tile indices by the <code>tileKey()</code>
     * method.  Each key is mapped to a <code>Job</code> for the tile.  If
     * there is no mapping for the tile, then there is no enqueued
     * <code>RequestJob</code>.
     */
    Map tileJobs = new HashMap();


    /** The name of this instance. */
    private String nameOfThisInstance;

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
    static Set getListeners(List reqList) {
        // Extract the Set of all TileComputationListeners.
        int numReq = reqList.size();
        HashSet listeners = null;
        for(int j = 0; j < numReq; j++) {
            Request req = (Request)reqList.get(j);
            // XXX Do not need empty check in next line.
            if(req.listeners != null && !req.listeners.isEmpty()) {
                if(listeners == null) {
                    listeners = new HashSet();
                }
                listeners.addAll(req.listeners);
            }
        }

        return listeners;
    }

    /**
     * Converts the supplied <code>Exception</code>'s stack trace
     * to a <code>String</code>.
     */
    private static String getStackTraceString(Throwable e) {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(byteStream);
        e.printStackTrace(printStream);
        printStream.flush();
        String stackTraceString = byteStream.toString();
        printStream.close();
        return stackTraceString;
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
    public SunTileScheduler(int parallelism, int priority,
                            int prefetchParallelism, int prefetchPriority) {
        // Create queues and set parallelism and priority to default values.
        this();

        setParallelism(parallelism);
        setPriority(priority);
        setPrefetchParallelism(prefetchParallelism);
        setPrefetchPriority(prefetchPriority);
    }

    /**
     * Constructor.  Processing and prefetch queues are created and all
     * parallelism and priority values are set to default values.
     */
    public SunTileScheduler() {
        queue = new LinkedList();
        prefetchQueue = new LinkedList();

	nameOfThisInstance = name + numInstances;
	rootGroup = new ThreadGroup(nameOfThisInstance);
	rootGroup.setDaemon(true);

	standardGroup = new ThreadGroup(rootGroup,
	                                nameOfThisInstance + "Standard");
	standardGroup.setDaemon(true);

	prefetchGroup = new ThreadGroup(rootGroup,
	                                nameOfThisInstance + "Prefetch");
	prefetchGroup.setDaemon(true);

	numInstances++;
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
                Point p = tileIndices[j];

                try {
                    tiles[j] = owner.getTile(p.x, p.y);
                } catch (Exception e) {
                    exception = e;

                    // Abort the remaining tiles in the job.
                    break;
                }
            }
        } else { // listeners present
            Request[] reqs = new Request[] {request};
            for(int i = 0; i < numTiles; i++, j++) {
                Point p = tileIndices[j];

                // Update tile status to "processing".
                Integer tileStatus =
                    new Integer(TileRequest.TILE_STATUS_PROCESSING);
                request.tileStatus.put(p, tileStatus);

                try {
                    tiles[j] = owner.getTile(p.x, p.y);
                    Iterator iter = request.listeners.iterator();
                    while(iter.hasNext()) {
                        // Update tile status to "computed".
                        tileStatus =
                            new Integer(TileRequest.TILE_STATUS_COMPUTED);
                        request.tileStatus.put(p, tileStatus);

                        TileComputationListener listener =
                            (TileComputationListener)iter.next();
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
                /* XXX
                try {
                    List reqList;
                    synchronized(tileRequests) {
                        Long tileID = tileKey(owner, p.x, p.y);
                        reqList = (List)tileRequests.remove(tileID);
                        tileJobs.remove(tileID);
                    }
                    if(reqList != null) {
                        tiles[j] = owner.getTile(p.x, p.y);
                        TileRequest[] reqs =
                            (TileRequest[])reqList.toArray(new TileRequest[0]);
                        Set listeners = getListeners(reqList);
                        if(listeners != null) {
                            Iterator iter = listeners.iterator();
                            while(iter.hasNext()) {
                                TileComputationListener listener =
                                    (TileComputationListener)iter.next();
                                listener.tileComputed(this,
                                                      reqs,
                                                      owner,
                                                      p.x, p.y,
                                                      tiles[j]);
                            }
                        }
                    }
                } catch (Exception e) {
                    exception = e;

                    // Abort the remaining tiles in the job.
                    break;
                }
                */
            }
        }

        // If an exception occured, notify listeners that all remaining
        // tiles in the job have failed.
        if(exception != null && request != null && request.listeners != null) {
            int lastOffset = j;
            int numFailed = numTiles - (lastOffset - offset);

            // Mark all tiles starting with the one which generated the
            // Exception as "failed".
            for(int i = 0, k = lastOffset; i < numFailed; i++) {
                Integer tileStatus =
                    new Integer(TileRequest.TILE_STATUS_FAILED);
                request.tileStatus.put(tileIndices[k++], tileStatus);
            }

            // Notify listeners.
            Request[] reqs = new Request[] {request};
            for(int i = 0, k = lastOffset; i < numFailed; i++) {
                Point p = tileIndices[k++];
                Iterator iter = request.listeners.iterator();
                while(iter.hasNext()) {
                    TileComputationListener listener =
                        (TileComputationListener)iter.next();
                    listener.tileComputationFailure(this, reqs,
                                                    owner, p.x, p.y,
                                                    exception);
                }
            }
        }

        /* XXX
        if(exception != null) {
            int numFailed = numTiles - (j - offset);
            for(int i = 0; i < numFailed; i++) {
                Point p = tileIndices[j++];
                Long tileID = tileKey(owner, p.x, p.y);
                List reqList = (List)tileRequests.remove(tileID);
                tileJobs.remove(tileID);
                if(reqList != null) {
                    TileRequest[] reqs =
                        (TileRequest[])reqList.toArray(new TileRequest[0]);
                    Set listeners = getListeners(reqList);
                    if(listeners != null) {
                        Iterator iter = listeners.iterator();
                        while(iter.hasNext()) {
                            TileComputationListener listener =
                                (TileComputationListener)iter.next();
                            listener.tileComputationFailure(this, reqs,
                                                            owner, p.x, p.y,
                                                            exception);
                        }
                    }
                }
            }
        }
        */

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
    public Raster scheduleTile(OpImage owner,
                               int tileX,
                               int tileY) {
        if (owner == null) {
            throw new IllegalArgumentException(JaiI18N.getString("SunTileScheduler1"));
        }

        // Eventual tile to be returned.
        Raster tile = null;

        // Get the tile's unique ID.
        Object tileID = tileKey(owner, tileX, tileY);

        // Set the computation flag and initialize or retrieve the tile cache.
        boolean computeTile = false;
        Object[] cache = null;
        synchronized(tilesInProgress) {
            if(computeTile = !tilesInProgress.containsKey(tileID)) {
                // Computing: add tile ID to the map.
                tilesInProgress.put(tileID, cache = new Object[1]);
            } else {
                // Waiting: get tile cache from the Map.
                cache = (Object[])tilesInProgress.get(tileID);
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
                } else if(e instanceof RuntimeException) {
                    sendExceptionToListener(JaiI18N.getString("SunTileScheduler6"), e);
//                    throw (RuntimeException)e;
                } else {
                    String message = JaiI18N.getString("SunTileScheduler6");
                    sendExceptionToListener(message,
                                            new ImagingException(message, e));
/*
                    throw new RuntimeException(e.getMessage()+"\n"+
                                               getStackTraceString(e));
*/
                }
            } finally {
                synchronized(cache) {
                    // Always set the cached tile to a non-null value.
                    cache[0] = tile != null ? tile : new Object();

                    // Notify the thread(s).
                    cache.notifyAll();

                    synchronized(tilesInProgress) {
                        // Remove the tile ID from the Map.
                        tilesInProgress.remove(tileID);
                    }
                }
            }
        } else {
            synchronized(cache) {
                // Check the cache: a null value indicates computation is
                // still in progress.
                if(cache[0] == null) {
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
                    throw new RuntimeException(JaiI18N.getString("SunTileScheduler5"));
                }
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
     * @param listener A <code>TileComputationListener</code> of the
     *        processing.  May be <code>null</code>.
     *
     * @return The computed tiles.  This value is meaningless if
     *         <code>listener</code> is non-<code>null</code>.
     */
    // The allowable arguments are constained as follows:
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
        Raster[] tiles = new Raster[numTiles];
        Object returnValue = tiles;

        int numThreads = 0;
        Job[] jobs = null;
        int numJobs = 0;

        synchronized(getWorkers(isPrefetch)) {
            numThreads = getNumThreads(isPrefetch);

            if(numThreads > 0) { // worker threads exist
                if(numTiles <= numThreads ||       // no more tiles than threads
                   (!isBlocking && !isPrefetch)) { // non-blocking, non-prefetch

                    jobs = new Job[numTiles];

                    if(!isBlocking && !isPrefetch) {
                        Request request =
                            new Request(this, owner, tileIndices, listeners);

                        // Override return value.
                        returnValue = request;

                        // Queue all tiles as single-tile jobs.
                        while(numJobs < numTiles) {
                            Point p = tileIndices[numJobs];

                            Object tileID = tileKey(owner, p.x, p.y);

                            synchronized(tileRequests) {
                                List reqList = null;
                                if(tileRequests.containsKey(tileID)) {
                                    // This tile is already queued in a
                                    // non-blocking, non-prefetch job.
                                    reqList = (List)tileRequests.get(tileID);
                                    reqList.add(request);
                                    numTiles--;
                                } else {
                                    // This tile has not yet been queued.
                                    reqList = new ArrayList();
                                    reqList.add(request);
                                    tileRequests.put(tileID, reqList);

                                    jobs[numJobs] =
                                        new RequestJob(this,
                                                       owner, p.x, p.y,
                                                       tiles, numJobs);

                                    tileJobs.put(tileID, jobs[numJobs]);

                                    addJob(jobs[numJobs++], false);
                                }
                            }
                        }
                    } else { // numTiles <= numThreads
                        while(numJobs < numTiles) {
                            jobs[numJobs] = new TileJob(this,
                                                        isBlocking,
                                                        owner,
                                                        tileIndices,
                                                        tiles,
                                                        numJobs,
                                                        1);
                            addJob(jobs[numJobs++], isPrefetch);
                        }
                    }
                } else { // more tiles than worker threads
                    // Set the fraction of unqueued tiles to be processed by
                    // each worker thread.
                    float frac = 1.0F/(2.0F*numThreads);

                    // Set the minimum number of tiles each thread may process.
                    // If there is only one thread this will equal the total
                    // number of tiles.
                    int minTilesPerThread = numThreads == 1 ? numTiles :
                        Math.min(Math.max(1, (int)(frac*numTiles/2.0F + 0.5F)),
                                 numTiles);

                    // Allocate the maximum possible number of multi-tile jobs.
                    // This will be larger than the actual number of jobs but
                    // a more precise calcuation is not possible and a dynamic
                    // storage object such as a Collection would not be useful
                    // since as calculated maxNumJobs = 4*numThreads if the
                    // preceeding values of "frac" and "minTilesPerThread" are
                    // 1/(2*numThreads) and frac*numTiles/2, respectively.
                    int maxNumJobs = numThreads == 1 ? 1 :
                        (int)((float)numTiles/(float)minTilesPerThread+0.5F);
                    jobs = new TileJob[maxNumJobs];

                    // Set the number of enqueued tiles and the number left.
                    int numTilesQueued = 0;
                    int numTilesLeft = numTiles - numTilesQueued;

                    // Assign a number of tiles to each thread determined by
                    // the number of remaining tiles, the fraction of remaining
                    // tiles to be processed and the minimum chunk size.
                    while(numTilesLeft > 0) {
                        // Set the number of tiles to the pre-calculated
                        // fraction of tiles yet to be computed.
                        int numTilesInThread = (int)(frac*numTilesLeft + 0.5F);

                        // Ensure that the number to be processed is at
                        // least the minimum chunk size.
                        if(numTilesInThread < minTilesPerThread) {
                            numTilesInThread = minTilesPerThread;
                        }

                        // Clamp number of tiles in thread to number unqueued.
                        if(numTilesInThread > numTilesLeft) {
                            numTilesInThread = numTilesLeft;
                        }

                        // Decrement the count of remaining tiles. Note that
                        // this value will be non-negative due to the clamping
                        // above.
                        numTilesLeft -= numTilesInThread;

                        // If the number left is smaller than the minimum chunk
                        // size then process these tiles in the current job.
                        if(numTilesLeft < minTilesPerThread) {
                            numTilesInThread += numTilesLeft;
                            numTilesLeft = 0;
                        }

                        // Create a job to process the number of tiles needed.
                        jobs[numJobs] = new TileJob(this,
                                                    isBlocking,
                                                    owner,
                                                    tileIndices,
                                                    tiles,
                                                    numTilesQueued,
                                                    numTilesInThread);

                        // Queue the job and increment the job count.
                        addJob(jobs[numJobs++], isPrefetch);

                        // Increment the count of tiles queued.
                        numTilesQueued += numTilesInThread;
                    }
                } // SingleTile vs. MultiTile Jobs
            } // numThreads > 0
        } // end synchronized block

        if(numThreads != 0) {
            // If blocking, wait until all tiles have been computed.
            // There is no 'else' block for non-blocking as in that
            // case we just want to continue.
            if(isBlocking) {
                LinkedList jobQueue = getQueue(isPrefetch);

                for (int i = 0; i < numJobs; i++) {
                    synchronized(this) {
                        while (jobs[i].notDone()) {
                            try {
                                wait();
                            } catch(InterruptedException ie) {
                                // Ignore: should never happen.
                            }
                        }
                    }

                    // XXX: should we re-throw the exception or
                    //      should we reschedule this job ?? krishnag
                    Exception e = jobs[i].getException();

                    if (e != null) {
                        // Throw a RuntimeException with the Exception's
                        // message concatenated with the stack trace.
                        String message = JaiI18N.getString("SunTileScheduler7");
                        sendExceptionToListener(message,
                                                new ImagingException(message, e));
/*
                        throw new RuntimeException(e.getMessage()+"\n"+
                                                   getStackTraceString(e));
*/
                    }
                }
            }
        } else { // numThreads == 0
            Request request = null;
            if(!isBlocking && !isPrefetch) {
                request = new Request(this, owner, tileIndices, listeners);
                returnValue = request;
            }

            // no workers; sequentially compute tiles in main thread
            Exception e = compute(owner, tileIndices, tiles, 0, numTiles,
                                  request);

            // Throw a RuntimeException with the Exception's
            // message concatenated with the stack trace.
            if(e != null) {
                String message = JaiI18N.getString("SunTileScheduler7");
                sendExceptionToListener(message,
                                        new ImagingException(message, e));
/*
                throw new RuntimeException(e.getMessage()+"\n"+
                                           getStackTraceString(e));
*/
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
    public Raster[] scheduleTiles(OpImage owner,
                                  Point tileIndices[]) {
        if (owner == null || tileIndices == null) {
            throw new IllegalArgumentException(JaiI18N.getString("SunTileScheduler0"));
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
    public TileRequest scheduleTiles(PlanarImage target, Point[] tileIndices,
                                     TileComputationListener[] tileListeners) {
        if (target == null || tileIndices == null) {
            throw new IllegalArgumentException(JaiI18N.getString("SunTileScheduler4"));
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
    public void cancelTiles(TileRequest request, Point[] tileIndices) {
        if(request == null) {
            throw new IllegalArgumentException(JaiI18N.getString("SunTileScheduler3"));
        }

        Request req = (Request)request;
        synchronized(tileRequests) {
            // Save the list of all tile indices in this request.
            List reqIndexList = req.indices;

            // Initialize the set of tile indices to cancel.
            Point[] indices;
            if(tileIndices != null && tileIndices.length > 0) {
                // Create a Set from the supplied indices.
                List tileIndexList = Arrays.asList(tileIndices);

                // Retain only indices which were actually in the request.
                tileIndexList.retainAll(reqIndexList);

                indices = (Point[])tileIndexList.toArray(new Point[0]);
            } else {
                indices = (Point[])reqIndexList.toArray(new Point[0]);
            }

            // Cache the count.
            int numTiles = indices.length;

            // Cache status value.
            Integer tileStatus = new Integer(TileRequest.TILE_STATUS_CANCELLED);

            // Loop over tile indices to be cancelled.
            for(int i = 0; i < numTiles; i++) {
                Point p = indices[i];

                // Get the tile's ID.
                Object tileID = tileKey(req.image, p.x, p.y);

                // Get the list of requests for this tile.
                List reqList = (List)tileRequests.get(tileID);

                // If there are none, proceed to next index.
                if(reqList == null) {
                    continue;
                }

                // Remove this Request from the Request List for this tile.
                reqList.remove(req);

                // If the request list is now empty, dequeue the job and
                // remove the tile from the hashes.
                if(reqList.isEmpty()) {
                    synchronized(queue) {
                        Object job = tileJobs.remove(tileID);
                        if(job != null) {
                            queue.remove(job);
                        }
                    }
                    tileRequests.remove(tileID);
                }

                // Update tile status to "cancelled".
                req.tileStatus.put(p, tileStatus);

                // Notify any listeners.
                if(req.listeners != null) {
                    TileRequest[] reqArray = new TileRequest[] {req};
                    Iterator iter = req.listeners.iterator();
                    while(iter.hasNext()) {
                        TileComputationListener listener =
                            (TileComputationListener)iter.next();
                        listener.tileCancelled(this, reqArray,
                                               req.image, p.x, p.y);
                    }
                }
            }
        }
    }

    /**
     * Prefetchs a list of tiles of an image.
     *
     * @param owner  The image the tiles belong to.
     * @param tileIndices  An array of tile X and Y indices.
     */
    public void prefetchTiles(PlanarImage owner,
                              Point[] tileIndices) {
        if (owner == null || tileIndices == null) {
            throw new IllegalArgumentException(JaiI18N.getString("SunTileScheduler0"));
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
     * In JAI the parallelism defaults to a value of 2 unless explicity set
     * by the application.
     *
     * @param parallelism The suggested degree of parallelism.
     * @throws IllegalArgumentException if <code>parallelism</code>
     *         is negative.
     */
    public void setParallelism(int parallelism) {
        if (parallelism < 0) {
            throw new IllegalArgumentException(JaiI18N.getString("SunTileScheduler2"));
        }
        this.parallelism = parallelism;
    }

    /**
     * Returns the degree of parallelism of the scheduler.
     */
    public int getParallelism() {
        return parallelism;
    }

    /**
     * Identical to <code>setParallelism()</code> but applies only to
     * <code>prefetchTiles()</code>.
     */
    public void setPrefetchParallelism(int parallelism) {
        if (parallelism < 0) {
            throw new IllegalArgumentException(JaiI18N.getString("SunTileScheduler2"));
        }
        prefetchParallelism = parallelism;
    }

    /**
     * Identical to <code>getParallelism()</code> but applies only to
     * <code>prefetchTiles()</code>.
     */
    public int getPrefetchParallelism() {
        return prefetchParallelism;
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
    public void setPriority(int priority) {
        this.priority = Math.max(Math.min(priority, Thread.MAX_PRIORITY),
                                 Thread.MIN_PRIORITY);
    }

    /**
     * Returns the priority of <code>scheduleTiles()</code> processing.
     */
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
        prefetchPriority = Math.max(Math.min(priority, Thread.MAX_PRIORITY),
                                    Thread.MIN_PRIORITY);
    }

    /**
     * Identical to <code>getPriority()</code> but applies only to
     * <code>prefetchTiles()</code>.
     */
    public int getPrefetchPriority() {
        return prefetchPriority;
    }

    /** Recreate the <code>ThreadGroup</code>is and <code>WorkThread</code>s.
     * This happens in the case of applet: the java plugin will exist after
     * the termination of the applet so that JAI and SunTileScheduler will
     * also exist.  However, the <code>ThreadGroup</code>s are destroyed.
     * Thus, the old workers should be terminated and new i
     * <code>ThreadGroup</code> and workers should be created.
     */
//    private synchronized void createThreadGroup(boolean isPrefetch) {
    private void createThreadGroup(boolean isPrefetch) {
        if (rootGroup == null || rootGroup.isDestroyed()) {
            rootGroup = new ThreadGroup(nameOfThisInstance);
            rootGroup.setDaemon(true);
        }

	if (isPrefetch && 
	    (prefetchGroup == null || prefetchGroup.isDestroyed())) {
            prefetchGroup = new ThreadGroup(rootGroup,
                                        nameOfThisInstance + "Prefetch");
            prefetchGroup.setDaemon(true);
	}

	if (!isPrefetch &&
	    (standardGroup == null || standardGroup.isDestroyed())) {
            standardGroup = new ThreadGroup(rootGroup,
                                        nameOfThisInstance + "Standard");
            standardGroup.setDaemon(true);
	}

	Vector thr = getWorkers(isPrefetch);
        int size = thr.size();

        for(int i = size - 1; i >= 0; i--) {
            Thread t = (Thread)thr.get(i);
	    if (!t.isAlive())
		thr.remove(t);
	}

	if (isPrefetch)
	    numPrefetchThreads = thr.size();
	else
	    numWorkerThreads = thr.size();

    }

    /**
     * Returns the effective number of threads of the specified type.
     * This method also updates the number and priority of threads of
     * the specified type according to the global settings. This method
     * may add <code>WorkerThread.TERMINATE</code>s to the appropriate
     * queue if there are too many effective threads.
     */
    private int getNumThreads(boolean isPrefetch) {
	createThreadGroup(isPrefetch);

        // Local variables.
        Vector thr = getWorkers(isPrefetch);
        int nthr;
        int prll;
        int prty;

        // Set local variables depending on the thread type.
        if(isPrefetch) {
            nthr = numPrefetchThreads;
            prll = prefetchParallelism;
            prty = prefetchPriority;
        } else {
            nthr = numWorkerThreads;
            prll = parallelism;
            prty = priority;
        }

        // Update priority if it has changed.
        if(nthr > 0 &&
           ((Thread)thr.get(0)).getPriority() != prty) {
            int size = thr.size();
            for(int i = 0; i < size; i++) {
                Thread t = (Thread)thr.get(i);
                if (t != null && t.getThreadGroup() != null) {
		    t.setPriority(prty);
		}
            }
        }

        if(nthr < prll) {
            // Not enough processing threads.
            // Add more threads at current priority.
            while(nthr < prll) {
                Thread t =
		    new WorkerThread(isPrefetch ? prefetchGroup : standardGroup,
				     this, isPrefetch);

                t.setPriority(prty);
                thr.add(t);
                nthr++;
            }
        } else {
            // Too many processing threads: queue WorkerThread.TERMINATEs.
            // WorkerThread will remove itself later from the appropriate
            // Vector.
            while(nthr > prll) {
                addJob(WorkerThread.TERMINATE, isPrefetch);
                nthr--;
            }
        }

        // Update the number of effective threads.
        if(isPrefetch) {
            numPrefetchThreads = nthr;
        } else {
            numWorkerThreads = nthr;
        }

        return nthr;
    }

    /** Returns the appropriate worker list. */
    Vector getWorkers(boolean isPrefetch) {
        return isPrefetch ? workers : prefetchWorkers;
    }

    /** Returns the appropriate queue. */
    LinkedList getQueue(boolean isPrefetch) {
        return isPrefetch ? prefetchQueue : queue;
    }

    /** Append a job to the appropriate queue. */
    private void addJob(Object job, boolean isPrefetch) {
        if(job == null ||
           (job != WorkerThread.TERMINATE && !(job instanceof Job))) {
            // Programming error: deliberately no message.
            throw new IllegalArgumentException();
        }

        LinkedList jobQueue;
        synchronized(jobQueue = getQueue(isPrefetch)) {
            if(isPrefetch ||
               jobQueue.isEmpty() ||
               job instanceof RequestJob) {
                // Append job to queue.
                jobQueue.addLast(job);
            } else {
                // If the queue is non-empty or the job is a TileJob
                // insert the job after the last TileJob in the queue.
                boolean inserted = false;
                for(int idx = jobQueue.size() - 1; idx >= 0; idx--) {
                    if(jobQueue.get(idx) instanceof TileJob) {
                        jobQueue.add(idx+1, job);
                        inserted = true;
                        break;
                    }
                }
                if(!inserted) {
                    jobQueue.addFirst(job);
                }
            }
            jobQueue.notify();
        }
    }

    /** Queue WorkerThread.TERMINATEs to all workers. */
    protected void finalize() throws Throwable {
        terminateAll(false);
        terminateAll(true);
        super.finalize();
    }

    /** Queue WorkerThread.TERMINATEs to all appropriate workers. */
    private void terminateAll(boolean isPrefetch) {
        synchronized(getWorkers(isPrefetch)) {
            int numThreads = isPrefetch ?
                numPrefetchThreads : numWorkerThreads;
            for(int i = 0; i < numThreads; i++) {
                addJob(WorkerThread.TERMINATE, isPrefetch);
                if(isPrefetch) {
                    numPrefetchThreads--;
                } else {
                    numWorkerThreads--;
                }
            }
        }
    }

    void sendExceptionToListener(String message, Throwable e) {
        ImagingListener listener =
            ImageUtil.getImagingListener((RenderingHints)null);
        listener.errorOccurred(message, e, this, false);
    }
}
