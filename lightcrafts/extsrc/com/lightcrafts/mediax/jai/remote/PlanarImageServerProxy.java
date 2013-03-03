/*
 * $RCSfile: PlanarImageServerProxy.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:51 $
 * $State: Exp $
 */package com.lightcrafts.mediax.jai.remote;

import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.ColorModel;
import java.awt.image.renderable.ParameterBlock;
import java.util.Vector;
import com.lightcrafts.mediax.jai.ImageLayout;
import com.lightcrafts.mediax.jai.JAI;
import com.lightcrafts.mediax.jai.OperationRegistry;
import com.lightcrafts.mediax.jai.PlanarImage;
import com.lightcrafts.mediax.jai.TileCache;
import com.lightcrafts.mediax.jai.util.ImagingListener;

/**
 * A subclass of <code>PlanarImage</code> which represents an image on a
 * remote server machine. This class is also an implementation of the
 * <code>RemoteRenderedImage</code> interface. This class allows processing
 * to occur on the remote server machine.
 *
 * <p>Conceptually this class is like a No-op, all it provides is a mechanism
 * allowing the processing to occur on a server. Note that this class does
 * not mandate that the client-server communication rely on any particular
 * wire protocol or communication protocol. A subclass can choose any wire
 * or communication protocol to communicate with its server. This is
 * accomplished by having the subclass implement the methods declared to
 * be abstract in this class. All functionality in this class is then
 * implemented in terms of these abstract methods.
 *
 * <p>Network errors (detected via throws of
 * <code>RemoteImagingException</code>) are dealt with through the use of
 * retry intervals and retries. Retries refers to the maximum number of
 * times a remote operation will be retried. The retry interval refers to
 * the amount of time in milliseconds between two consecutive retries. If
 * errors are encountered at each retry and the number of specified retries
 * has been exhausted, a <code>RemoteImagingException</code> will be thrown.
 * Time outs (When the amount of time taken to get a response or
 * the result of an operation from the remote machine exceeds a limit) are
 * not dealt with, and must be taken care of by the network
 * imaging protocol implementation. The implementation must be responsible
 * for monitoring time outs, but on encountering one can deal with it by
 * throwing a <code>RemoteImagingException</code>, which will then be dealt
 * with using retries and retry intervals.
 *
 * <p> The resultant image layout is computed and provided by the concrete
 * subclass by implementing the abstract method <code>getImageLayout</code>.
 * All the accessor methods dealing with the layout variables namely
 * <code>getMinX()</code>, <code>getMinY()</code>, <code>getWidth()</code>,
 * <code>getHeight()</code>, <code>getMaxX()</code>, <code>getMaxY()</code>,
 * <code>getTileWidth()</code>, <code>getTileHeight()</code>,
 * <code>getTileGridXOffset()</code>, <code>getTileGridYOffset()</code>,
 * <code>getColorModel()</code> and <code>getSampleModel()</code> are
 * implemented in terms of the <code>getImageLayout()</code> method. The
 * implementation of these methods uses retries and retry intervals to
 * deal with Network errors, such that the subclass implementing
 * <code>getImageLayout()</code> does not need to worry about Network errors
 * except to signal them by throwing a <code>RemoteImagingException</code>.
 * The same applies to the other abstract methods implemented by sub-classes
 * namely <code>getRemoteProperty()</code>,
 * <code>getRemotePropertyNames()</code> and <code>computeTile()</code>.
 *
 * <p> The <code>getTile</code> method (abstract in this class' superclass),
 * is implemented in terms of the <code>computeTile</code> method. It provides
 * the additional functionality of caching the tiles on the client, as well
 * as that of dealing with Network errors as mentioned above.
 *
 * @see RemoteImagingException
 *
 * @since JAI 1.1
 */
public abstract class PlanarImageServerProxy extends PlanarImage implements RemoteRenderedImage {

    /** Time in milliseconds between retries. */
    protected int retryInterval;

    /** The number of retries. */
    protected int numRetries;

    /** A reference to a centralized TileCache object. */
    protected transient TileCache cache;

    /**
     * Metric used to produce an ordered list of tiles.  This determines
     * which tiles are removed from the cache first if a memory control
     * operation is required.
     */
    protected Object tileCacheMetric;

    /** A reference to the OperationRegistry object. */
    protected transient OperationRegistry registry;

    /** The String representing the remote server machine. */
    protected String serverName;

    /** The name of the protocol to be used for remote communication. */
    protected String protocolName;

    /** The name of the operation to be performed remotely. */
    protected String operationName;

    /** The sources and/or arguments to the operation. */
    protected ParameterBlock paramBlock;

    /** The RenderingHints for the operation. */
    protected RenderingHints hints;

    // The layout of the image
    private ImageLayout layout = null;

    /** The preferences to be utilized in the negotiation. */
    protected NegotiableCapabilitySet preferences;

    /**
     * The set of properties agreed upon after the negotiation process
     * between the client and the server has been completed.
     */
    protected NegotiableCapabilitySet negotiated;

    /** The server capabilities. */
    NegotiableCapabilitySet serverCapabilities;

    /** The client capabilities. */
    NegotiableCapabilitySet clientCapabilities;

    /**
     * Checks that all the layout parameters have been specified.
     *
     * @throws IllegalArgumentException if layout is null.
     * @throws Error if all the layout fields are not initialized.
     */
    private static void checkLayout(ImageLayout layout) {

	if (layout == null) {
	    throw new IllegalArgumentException("layout is null.");
	}

	if (layout.getValidMask() != 0x3ff) {
 	    throw new Error(JaiI18N.getString("PlanarImageServerProxy3"));
	}
    }

    /**
     * Constructs a <code>PlanarImageServerProxy</code> using the specified
     * name of the server to perform the specified operation on, using the
     * sources and parameters specified by the supplied
     * <code>ParameterBlock</code> and supplied <code>RenderingHints</code>.
     * If hints relating to the <code>OperationRegistry</code>,
     * <code>TileCache</code>, retry interval, number of retries,
     * tile caching metric or negotiation preferences are included in the
     * specified <code>RenderingHints</code> object, they will be honored.
     *
     * <p> An <code>IllegalArgumentException</code> may
     * be thrown by the protocol specific classes at a later point, if
     * null is provided as the serverName argument and null is not
     * considered a valid server name by the specified protocol.
     *
     * @param serverName    The <code>String</code> identifying the remote
     *                      server machine.
     * @param protocolName  The name of the remote imaging protocol.
     * @param operationName The name of the operation.
     * @param paramBlock    The source(s) and/or parameter(s) for the operation.
     * @param hints         The hints for the operation.
     *
     * @throws IllegalArgumentException if operationName is null.
     */
    public PlanarImageServerProxy(String serverName,
				  String protocolName,
				  String operationName,
				  ParameterBlock paramBlock,
				  RenderingHints hints) {

	// To initialize property and event management stuff, as done by
	// superclass PlanarImage constructor.
	super (null, null, null);

	if (operationName == null) {
	    throw new IllegalArgumentException(
				JaiI18N.getString("PlanarImageServerProxy1"));
	}

	this.serverName = serverName;
	this.protocolName = protocolName;
	this.operationName = operationName;
	this.paramBlock = paramBlock;
	this.hints = hints;

	if (hints == null) {
	    // If there are no hints specified, use default values
	    registry = JAI.getDefaultInstance().getOperationRegistry();
	    cache = JAI.getDefaultInstance().getTileCache();
	    retryInterval = RemoteJAI.DEFAULT_RETRY_INTERVAL;
	    numRetries = RemoteJAI.DEFAULT_NUM_RETRIES;

	    // Do negotiation even though there are no preferences, so that
	    // negotiation takes place between the client and the server.
	    setNegotiationPreferences(null);
	} else {

	    registry =
		(OperationRegistry)hints.get(JAI.KEY_OPERATION_REGISTRY);
	    if (registry == null) {
		registry = JAI.getDefaultInstance().getOperationRegistry();
	    }

	    cache = (TileCache)hints.get(JAI.KEY_TILE_CACHE);
	    if (cache == null) {
		cache = JAI.getDefaultInstance().getTileCache();
	    }

	    Integer integer = (Integer)hints.get(JAI.KEY_RETRY_INTERVAL);
	    if (integer == null) {
		retryInterval = RemoteJAI.DEFAULT_RETRY_INTERVAL;
	    } else {
		retryInterval = integer.intValue();
	    }

	    integer = (Integer)hints.get(JAI.KEY_NUM_RETRIES);
	    if (integer == null) {
		numRetries = RemoteJAI.DEFAULT_NUM_RETRIES;
	    } else {
		numRetries = integer.intValue();
	    }

	    tileCacheMetric = (Object)hints.get(JAI.KEY_TILE_CACHE_METRIC);

	    // Cause negotiation to take place.
	    setNegotiationPreferences((NegotiableCapabilitySet)
				   hints.get(JAI.KEY_NEGOTIATION_PREFERENCES));
	}

	if (paramBlock != null) {
	    setSources(paramBlock.getSources());
	}
     }

    /**
     * Returns the <code>String</code> that identifies the server.
     */
    public String getServerName() {
	return serverName;
    }

    /**
     * Returns the <code>String</code> that identifies the remote imaging
     * protocol.
     */
    public String getProtocolName() {
	return protocolName;
    }

    /**
     * Returns the operation name as a <code>String</code>.
     */
    public String getOperationName() {
	return operationName;
    }

    /**
     * Returns the <code>ParameterBlock</code> that specifies the
     * sources and parameters for the operation to be performed by
     * this <code>PlanarImageServerProxy</code>.
     */
    public ParameterBlock getParameterBlock() {
	return paramBlock;
    }

    /**
     * Returns the <code>RenderingHints</code> associated with the
     * operation to be performed by this <code>PlanarImageServerProxy</code>.
     */
    public RenderingHints getRenderingHints() {
	return hints;
    }

    /**
     * Returns the tile cache object of this image by reference.
     * If this image does not have a tile cache, this method returns
     * <code>null</code>.
     */
    public TileCache getTileCache() {
        return cache;
    }

    /**
     * Sets the tile cache object of this image.  A <code>null</code>
     * input indicates that this image should have no tile cache and
     * subsequently computed tiles will not be cached.
     *
     * <p> The existing cache object is informed to release all the
     * currently cached tiles of this image.
     *
     * @param cache  A cache object to be used for caching this image's
     *        tiles, or <code>null</code> if no tile caching is desired.
     */
    public void setTileCache(TileCache cache) {
        if (this.cache != null) {
            this.cache.removeTiles(this);
        }
        this.cache = cache;
    }

    /**
     * Returns the <code>tileCacheMetric</code> instance variable by reference.
     */
    public Object getTileCacheMetric() {
        return tileCacheMetric;
    }

    /**
     * Get the layout of the image. This could be implemented by either
     * asking the server to specify the layout, or have the client compute
     * the image layout. The <code>ImageLayout</code> object returned must
     * have all its fields initialized, else an <code>Error</code> will be
     * thrown. Network errors encountered should be signalled
     * by throwing a <code>RemoteImagingException</code>.
     *
     * @throws RemoteImagingException if an error condition during remote
     *         image processing occurs
     * @throws Error if all the fields in the ImageLayout are not initialized.
     */
    public abstract ImageLayout getImageLayout() throws RemoteImagingException;

    /**
     * Returns a property from the property set generated on the remote
     * server machine. Network errors encountered should be signalled
     * by throwing a <code>RemoteImagingException</code>. If the property
     * name is not recognized, java.awt.Image.UndefinedProperty will be
     * returned.
     *
     * @throws RemoteImagingException if an error condition during remote
     *         image processing occurs
     * @throws IllegalArgumentException if name is null.
     */
    public abstract Object getRemoteProperty(String name)
	throws RemoteImagingException;

    /**
     * Returns a list of names recognized by the <code>getRemoteProperty</code>
     * method. Network errors encountered should be signalled by
     * throwing a <code>RemoteImagingException</code>.
     *
     * @throws RemoteImagingException if an error condition during remote
     *         image processing occurs
     */
    public abstract String[] getRemotePropertyNames()
	throws RemoteImagingException;

    /**
     * Returns a conservative estimate of the destination region that
     * can potentially be affected by the pixels of a rectangle of a
     * given source. This can be implemented by either asking the server
     * to compute the destination region, or by having the client compute
     * the destination region. Network errors encountered should be
     * signalled by throwing a <code>RemoteImagingException</code>.
     *
     * @param sourceRect  The <code>Rectangle</code> in source coordinates.
     * @param sourceIndex  The index of the source image.
     *
     * @return A <code>Rectangle</code> indicating the potentially
     *         affected destination region, or <code>null</code> if
     *         the region is unknown.
     *
     * @throws IllegalArgumentException  If the source index is
     *         negative or greater than that of the last source.
     * @throws IllegalArgumentException  If <code>sourceRect</code> is
     *         <code>null</code>.
     */
    public abstract Rectangle mapSourceRect(Rectangle sourceRect,
                                            int sourceIndex)
	throws RemoteImagingException;

    /**
     * Returns a conservative estimate of the region of a specified
     * source that is required in order to compute the pixels of a
     * given destination rectangle. Either the server or the client can
     * compute the source region to implement this method. Network errors
     * encountered should be signalled by throwing a
     * <code>RemoteImagingException</code>.
     *
     * @param destRect  The <code>Rectangle</code> in destination coordinates.
     * @param sourceIndex  The index of the source image.
     *
     * @return A <code>Rectangle</code> indicating the required source region.
     *
     * @throws IllegalArgumentException  If the source index is
     *         negative or greater than that of the last source.
     * @throws IllegalArgumentException  If <code>destRect</code> is
     *         <code>null</code>.
     */
    public abstract Rectangle mapDestRect(Rectangle destRect,
                                          int sourceIndex)
	throws RemoteImagingException;

    /**
     * Returns tile (tileX, tileY) as computed on the remote server machine.
     * Note that tileX and tileY are indices into the tile array, not pixel
     * locations. The <code>Raster</code> that is returned is a copy.
     * Network errors encountered should be signalled by throwing a
     * <code>RemoteImagingException</code>.
     *
     * <p> Subclasses must implement this method to return a
     * non-<code>null</code> value for all tile indices between
     * <code>getMinTile{X,Y}</code> and <code>getMaxTile{X,Y}</code>,
     * inclusive.  Tile indices outside of this region should result
     * in a return value of <code>null</code>.
     *
     * @param tileX the X index of the requested tile in the tile array.
     * @param tileY the Y index of the requested tile in the tile array.
     * @throws RemoteImagingException if an error condition during remote
     *         image processing occurs
     */
    public abstract Raster computeTile(int tileX, int tileY)
	throws RemoteImagingException;

    /**
     * Returns the amount of time between retries in milliseconds.
     */
    public int getRetryInterval() {
	return retryInterval;
    }

    /**
     * Sets the amount of time between retries in milliseconds.
     *
     * @param retryInterval The amount of time (in milliseconds) to wait
     *                      between retries.
     * @throws IllegalArgumentException if retryInterval is negative.
     */
    public void setRetryInterval(int retryInterval) {
	if (retryInterval < 0) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic3"));
        }
	this.retryInterval = retryInterval;
    }

    /**
     * Returns the number of retries.
     */
    public int getNumRetries() {
	return numRetries;
    }

    /**
     * Sets the number of retries.
     *
     * @param numRetries The number of times an operation should be retried
     *                   in case of a network error.
     * @throws IllegalArgumentException if numRetries is negative.
     */
    public void setNumRetries(int numRetries) {
        if (numRetries < 0) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic4"));
        }
	this.numRetries = numRetries;
    }

    /**
     * Overrides the method in <code>PlanarImage</code> to return the X
     * coordinate of the leftmost column of the remote image.
     */
    public int getMinX() {
	requestLayout();
	return minX;
    }

    /**
     * Overrides the method in <code>PlanarImage</code> to return the X
     * coordinate of the column immediately to the right of the rightmost
     * column of the remote image.
     */
    public int getMaxX() {
	requestLayout();
	return minX + width;
    }

    /**
     * Overrides the method in <code>PlanarImage</code> to return the Y
     * coordinate of the uppermost row of the remote image.
     */
    public int getMinY() {
	requestLayout();
	return minY;
    }

    /**
     * Overrides the method in <code>PlanarImage</code> to return the Y
     * coordinate of the row immediately below the bottom row of the
     * remote image.
     */
    public int getMaxY() {
	requestLayout();
	return minY + height;
    }

    /**
     * Overrides the method in <code>PlanarImage</code> to return the width
     * of the remote image.
     */
    public int getWidth() {
	requestLayout();
	return width;
    }

    /**
     * Overrides the method in <code>PlanarImage</code> to return the height
     * of the remote image.
     */
    public int getHeight() {
	requestLayout();
	return height;
    }

    /**
     * Overrides the method in <code>PlanarImage</code> to return the width
     * of a tile remotely.
     */
    public int getTileWidth() {
	requestLayout();
	return tileWidth;
    }

    /**
     * Overrides the method in <code>PlanarImage</code> to return the height
     * of a tile remotely.
     */
    public int getTileHeight() {
	requestLayout();
	return tileHeight;
    }

    /**
     * Overrides the method in <code>PlanarImage</code> to return the X
     * coordinate of the upper-left pixel of tile (0, 0) remotely.
     */
    public int getTileGridXOffset() {
	requestLayout();
	return tileGridXOffset;
    }

    /**
     * Overrides the method in <code>PlanarImage</code> to return the Y
     * coordinate of the upper-left pixel of tile (0, 0) remotely.
     */
    public int getTileGridYOffset() {
	requestLayout();
	return tileGridYOffset;
    }

    /**
     * Overrides the method in <code>PlanarImage</code> to return the
     * <code>SampleModel</code> of the remote image.
     */
    public SampleModel getSampleModel() {
        requestLayout();
	return sampleModel;
    }

    /**
     * Overrides the method in <code>PlanarImage</code> to return the
     * <code>ColorModel</code> of the remote image.
     */
    public ColorModel getColorModel() {
	requestLayout();
	return colorModel;
    }

    /**
     * Cause the subclass' <code>getImageLayout</code> method to be called,
     * caching the <code>ImageLayout</code> object locally.
     *
     * @throws IllegalArgumentException if getImageLayout returns null.
     * @throws Error if all the fields of the layout are not initialized.
     * @throws RemoteImagingException if the limit of retries is exceeded.
     */
    private ImageLayout requestLayout() {

	if (layout != null)
	    return layout;

	Exception rieSave = null;
        int count = 0;
        while (count++ < numRetries) {
            try {
		layout = getImageLayout();
		// Check that all the fields are initialized
		checkLayout(layout);

		// Set all the super class variables
		minX = layout.getMinX(null);
		minY = layout.getMinY(null);
		width = layout.getWidth(null);
		height = layout.getHeight(null);
		tileWidth = layout.getTileWidth(null);
		tileHeight = layout.getTileHeight(null);
		tileGridXOffset = layout.getTileGridXOffset(null);
		tileGridYOffset = layout.getTileGridYOffset(null);
		sampleModel = layout.getSampleModel(null);
		colorModel = layout.getColorModel(null);
		break;
            } catch (RemoteImagingException e) {
                System.err.println(
				JaiI18N.getString("PlanarImageServerProxy0"));
		rieSave = e;
		// Sleep for retryInterval milliseconds before retrying.
                try {
                    Thread.sleep(retryInterval);
                } catch (InterruptedException f) {
                }
            }
        }

	if (layout == null) {
            sendExceptionToListener(rieSave);
	}

	return layout;
    }

    /**
     * Gets a property from the property set of this image.  If the
     * property name is not recognized,
     * <code>java.awt.Image.UndefinedProperty</code> will be returned.
     * The property to be returned is first looked for in the set of
     * locally cached properties. If not found, the
     * <code>getRemoteProperty</code> method is called to retrieve the
     * property. Network errors that might be encountered during the
     * <code>getRemoteProperty</code> call are dealt with by retries and
     * retry intervals.
     *
     * @param name the name of the property to get, as a <code>String</code>.
     * @return a reference to the property <code>Object</code>, or the value
     * <code>java.awt.Image.UndefinedProperty</code>.
     *
     * @throws RemoteImagingException if the limit of retries is exceeded.
     */
    public Object getProperty(String name) {

	// Try to get property locally.
        Object property = super.getProperty(name);

        if (property == null || property == Image.UndefinedProperty) {

	    Exception rieSave = null;
	    int count=0;
	    while(count++ < numRetries) {
		try {
		    property = getRemoteProperty(name);
		    if(property != Image.UndefinedProperty) {
			setProperty(name, property); // Cache property locally
		    }
		    return property;
		} catch (RemoteImagingException rie) {
		    System.err.println(
				JaiI18N.getString("PlanarImageServerProxy0"));
		    rieSave = rie;
		    try {
			Thread.sleep(retryInterval);
		    } catch (InterruptedException ie) {
		    }
		}
	    }

            sendExceptionToListener(rieSave);
            return property;
	} else {
	    return property;
	}
    }

    /**
     * Returns a list of property names that are recognized by this image
     * or <code>null</code> if none are recognized. The list of recognized
     * property names consists of the locally cached property names
     * (retrieved via <code>super.getPropertyNames</code>) as well as
     * those that might be generated by the operations performed on the
     * remote server machine (retrieved via
     * <code>getRemotePropertyNames</code>). Network errors that might be
     * encountered during the <code>getRemotePropertyNames</code> method
     * are dealt with by retries and retry intervals.
     *
     * @return an array of <code>String</code>s containing valid
     *         property names.
     *
     * @throws RemoteImagingException if the limit of retries is exceeded.
     */
    public String[] getPropertyNames() {

	// Retrieve local property names.
        String[] localPropertyNames = super.getPropertyNames();

	Vector names = new Vector();
        // Put local names in a Vector, if any
	if (localPropertyNames != null) {
	    for(int i = 0; i < localPropertyNames.length; i++) {
		names.add(localPropertyNames[i]);
	    }
	}

	int count=0;
        String[] remotePropertyNames = null;
	Exception rieSave = null;

	while(count++ < numRetries) {
	    try {
		remotePropertyNames = getRemotePropertyNames();
		break;
	    } catch (RemoteImagingException rie) {
                System.err.println(
				JaiI18N.getString("PlanarImageServerProxy0"));
		rieSave = rie;
		try {
		    Thread.sleep(retryInterval);
		} catch (InterruptedException ie) {
		}
	    }
	}

	if (count > numRetries) {
            sendExceptionToListener(rieSave);
	}

	// Put the remote names, if any, in the Vector.
        if(remotePropertyNames != null) {
            for(int i = 0; i < remotePropertyNames.length; i++) {
                if(!names.contains(remotePropertyNames[i])) {
                    names.add(remotePropertyNames[i]);
                }
            }
        }

        // Set the return value from the vector.
        String propertyNames[] = names.size() == 0 ?
            null : (String[])names.toArray(new String[names.size()]);

        return propertyNames;
    }

    /**
     * Returns the tile (tileX, tileY). Note the tileX and tileY are indices
     * into the tile array and not pixel positions. This method is
     * implemented in terms of the <code>computeTile</code> method. This
     * method deals with Network errors (recognized as
     * <code>RemoteImagingExceptions</code>) through retries and retry
     * intervals. This method also performs caching of tiles, so that
     * an already computed tile does not need to be re-computed.
     *
     * @param tileX the X index of the tile.
     * @param tileY the Y index of the tile.
     * @throws RemoteImagingException if limit of retries is exceeded.
     */
    public Raster getTile(int tileX, int tileY) {

	Raster tile = null;

	// Make sure the requested tile is inside this image's boundary.
        if (tileX >= getMinTileX() && tileX <= getMaxTileX() &&
            tileY >= getMinTileY() && tileY <= getMaxTileY()) {

	    // Check if tile is available in the cache.
	    tile = cache != null ? cache.getTile(this, tileX, tileY) : null;

            if (tile == null) {         // tile not in cache
                // Ask the subclass for the tile
		int count = 0;
		Exception rieSave = null;
		while (count++ < numRetries) {
		    try {
			tile = computeTile(tileX, tileY);
			break;
		    } catch (RemoteImagingException rie) {
			System.err.println(
				JaiI18N.getString("PlanarImageServerProxy0"));
			rieSave = rie;
			try {
			    Thread.sleep(retryInterval);
			} catch (InterruptedException ie) {

			}
		    }
		}

		if (count > numRetries) {
                    sendExceptionToListener(rieSave);
		}

                // Cache the result tile.
		if (cache != null) {
		    cache.add(this, tileX, tileY, tile, tileCacheMetric);
		}
            }
        }

        return tile;
    }

    /**
     * Uncaches all the tiles when this image is garbage collected.
     */
    protected void finalize() throws Throwable {
        if (cache != null) {
            cache.removeTiles(this);
        }
        super.finalize();
    }

    //
    // NEGOTIATION RELATED METHODS
    //

    /**
     * Returns the current negotiation preferences or null, if none were
     * set previously.
     */
    public NegotiableCapabilitySet getNegotiationPreferences() {
	return preferences;
    }

    /**
     * Sets the preferences to be used in the client-server
     * communication. These preferences are utilized in the negotiation
     * process. Note that preferences for more than one category can be
     * specified using this method. Also each preference can be a list
     * of values in decreasing order of preference, each value specified
     * as a <code>NegotiableCapability</code>. The
     * <code>NegotiableCapability</code> first (for a particular category)
     * in this list is given highest priority in the negotiation process
     * (for that category).
     *
     * <p> It may be noted that this method allows for multiple negotiation
     * cycles. Everytime this method is called, new preferences are
     * specified for the negotiation, which takes place anew to produce
     * a new set of negotiated resultant values to be used in the
     * remote communication. If the subclass wants to ignore the
     * negotiation preferences newly set, this method can be overridden to
     * do so.
     *
     * @param preferences The preferences to be used in the negotiation
     * process.
     */
    public void setNegotiationPreferences(NegotiableCapabilitySet preferences)
    {
	this.preferences = preferences;

	// Every time new preferences are set, invalidate old Negotiation
	// results and do the negotiation again.
	negotiated = null;

	getNegotiatedValues();
    }

    /**
     * Returns the results of the negotiation process. This will return null
     * if no negotiation preferences were set, and no negotiation was
     * performed, or if the negotiation failed.
     */
    public synchronized NegotiableCapabilitySet getNegotiatedValues()
	throws RemoteImagingException {

	// If negotiation was not performed before, or if new preferences
	// have invalidated the old negotiated results.
	if (negotiated == null) {

	    // Initialize the clientCapabilities and serverCapabilities
	    // variables
	    getCapabilities();

	    // Do the negotiation
	    negotiated = RemoteJAI.negotiate(preferences,
					     serverCapabilities,
					     clientCapabilities);

	    // Inform the server of the negotiated values.
	    setServerNegotiatedValues(negotiated);
	}

	return negotiated;
    }

    /**
     * Returns the results of the negotiation process for the given category.
     * This will return null if no negotiation preferences were set, and
     * no negotiation was performed, or if the negotiation failed.
     *
     * @param category The category to return the negotiated results for.
     * @throws IllegalArgumentException if category is null.
     */
    public NegotiableCapability getNegotiatedValue(String category)
	throws RemoteImagingException {

	// We do not need to check for category being null, since that
	// check will be made by the methods called from within this method.

	// If negotiation was not performed before, or if new preferences
	// have invalidated the old negotiated results.
	if (negotiated == null) {

	    getCapabilities();

	    // Do the negotiation
	    return RemoteJAI.negotiate(preferences,
				       serverCapabilities,
				       clientCapabilities,
				       category);
	} else {
	    // If negotiated is not null, then the negotiated results are
	    // current and the result for the given category can just be
	    // extracted from there and returned.
	    return negotiated.getNegotiatedValue(category);
	}
    }

    // Get the client and server capabilities
    private void getCapabilities() {

	String mode = "remoteRendered";
	if (serverCapabilities == null) {

	    RemoteDescriptor desc =
		(RemoteDescriptor)registry.getDescriptor(mode, protocolName);

	    int count = 0;
	    Exception rieSave = null;
	    while (count++ < numRetries) {
		try {
		    serverCapabilities =
			desc.getServerCapabilities(serverName);
		    break;
		} catch (RemoteImagingException rie) {
		    System.err.println(
				JaiI18N.getString("PlanarImageServerProxy0"));
		    rieSave = rie;
		    try {
			Thread.sleep(retryInterval);
		    } catch (InterruptedException ie) {
		    }
		}
	    }

	    if (count > numRetries) {
                sendExceptionToListener(rieSave);
	    }
	}

	if (clientCapabilities == null) {
	    RemoteRIF rrif =
		(RemoteRIF)registry.getFactory(mode, protocolName);

	    clientCapabilities = rrif.getClientCapabilities();
	}
    }

    void sendExceptionToListener(Exception e) {
        ImagingListener listener = null;
        if (hints != null)
            listener = (ImagingListener)hints.get(JAI.KEY_IMAGING_LISTENER);
        else
            listener = JAI.getDefaultInstance().getImagingListener();
        String message = JaiI18N.getString("PlanarImageServerProxy2");
        listener.errorOccurred(message,
                               new RemoteImagingException(message, e),
                               this, false);
    }
}
