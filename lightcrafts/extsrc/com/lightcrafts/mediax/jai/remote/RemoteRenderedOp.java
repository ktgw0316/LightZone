/*
 * $RCSfile: RemoteRenderedOp.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.2 $
 * $Date: 2006/06/16 22:52:05 $
 * $State: Exp $
 */package com.lightcrafts.mediax.jai.remote;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.Vector;
import java.text.MessageFormat;
import com.lightcrafts.mediax.jai.CollectionChangeEvent;
import com.lightcrafts.mediax.jai.CollectionOp;
import com.lightcrafts.mediax.jai.JAI;
import com.lightcrafts.mediax.jai.OperationRegistry;
import com.lightcrafts.mediax.jai.PlanarImage;
import com.lightcrafts.mediax.jai.PropertyChangeEventJAI;
import com.lightcrafts.mediax.jai.PropertySourceChangeEvent;
import com.lightcrafts.mediax.jai.RegistryMode;
import com.lightcrafts.mediax.jai.RenderedOp;
import com.lightcrafts.mediax.jai.RenderingChangeEvent;
import com.lightcrafts.mediax.jai.TileCache;
import com.lightcrafts.mediax.jai.registry.RemoteRIFRegistry;
import com.lightcrafts.mediax.jai.util.ImagingException;
import com.lightcrafts.mediax.jai.util.ImagingListener;
import com.lightcrafts.media.jai.util.ImageUtil;

/**
 * A node in a remote rendered imaging chain.  This class is a concrete
 * implementation of the <code>RemoteRenderedImage</code> interface. A
 * <code>RemoteRenderedOp</code> stores a protocol name (as a
 * <code>String</code>), a server name (as a <code>String</code>), an
 * operation name (as a <code>String</code>), a
 * <code>ParameterBlock</code> containing sources and miscellaneous
 * parameters, and a <code>RenderingHints</code> containing rendering
 * hints.  A set of nodes may be joined together via the source
 * <code>Vector</code>s within their <code>ParameterBlock</code>s to
 * form a <u>d</u>irected <u>a</u>cyclic <u>g</u>raph (DAG). The topology
 * i.e., connectivity of the graph may be altered by changing the
 * <code>ParameterBlock</code>s; the operation name, parameters, and
 * rendering hints may also be changed.
 *
 * <p> Such chains represent and handle operations that are being
 * performed remotely. They convey the structure of an imaging
 * chain in a compact representation and can be used to influence the
 * remote imaging process (through the use of retry interval, retries and
 * negotiation preferences).
 *
 * <p> <code>RemoteRenderedOp</code>s are a client side representation of
 * the chain of operations taking place on the server.
 *
 * <p> The translation between <code>RemoteRenderedOp</code> chains and
 * <code>RemoteRenderedImage</code> (usually
 * <code>PlanarImageServerProxy</code>) chains makes use of two levels of
 * indirection provided by the <code>OperationRegistry</code> and
 * <code>RemoteRIF</code> facilities.  First, the
 * local <code>OperationRegistry</code> is used to map the protocol
 * name into a <code>RemoteRIF</code>.  This <code>RemoteRIF</code> then
 * constructs one or more <code>RemoteRenderedImage</code>s (usually
 * <code>PlanarImageServerProxy</code>s) to do the actual work (or
 * returns a <code>RemoteRenderedImage</code> by other means. The
 * <code>OperationRegistry</code> maps a protocol name into a
 * <code>RemoteRIF</code>, since there is one to one correspondence
 * between a protocol name and a <code>RemoteRIF</code>. This differs from
 * the case of <code>RenderedOp</code>s, where the
 * <code>OperationRegistry</code> maps each operation name to a
 * <code>RenderedImageFactory</code> (RIF), since there is a one to one
 * correspondence between an operation name and a RIF. The
 * <code>RemoteRIF</code>s are therefore protocol-specific and not operation
 * specific, while a RIF is operation specific.
 *
 * <p> Once a protocol name has been mapped into a <code>RemoteRIF</code>,
 * the <code>RemoteRIF.create()</code> method is used to create a rendering.
 * This rendering is responsible for communicating with the server to
 * perform the specified operation remotely.
 *
 * <p> By virtue of being a subclass of <code>RenderedOp</code>, this class
 * participates in Java Bean-style events as specified by
 * <code>RenderedOp</code>. This means that <code>PropertyChangeEmitter</code>
 * methods may be used to register and unregister
 * <code>PropertyChangeListener</code>s. <code>RemoteRenderedOp</code>s
 * are also <code>PropertyChangeListener</code>s so that they may be
 * registered as listeners of other <code>PropertyChangeEmitter</code>s
 * or the equivalent. Each <code>RemoteRenderedOp</code> also automatically
 * receives any <code>RenderingChangeEvent</code>s emitted by any of its
 * sources which are <code>RenderedOp</code>s.
 *
 * <p> <code>RemoteRenderedOp</code>s add the server name and the protocol
 * name to the critical attributes, the editing (changing) of which,
 * coupled with a difference in the old and new rendering over some
 * non-empty region, may cause a <code>RenderingChangeEvent</code> to
 * be emitted. As with <code>RenderedOp</code>, editing of a critical
 * attribute of a <code>RemoteRenderedOp</code> will cause a
 * <code>PropertyChangeEventJAI</code> detailing the change to be fired
 * to all registered <code>PropertyChangeListener</code>s.
 * <code>RemoteRenderedOp</code> registers itself as a
 * <code>PropertyChangeListener</code> for all critical attributes, and
 * thus receives all <code>PropertyChangeEventJAI</code> events generated
 * by itself. This is done in order to allow the event handling code
 * to generate a new rendering and reuse any tiles that might be valid
 * after the critical argument change.
 *
 * <p> When a <code>RemoteRenderedOp</code> node receives a
 * <code>PropertyChangeEventJAI</code> from itself, the region of
 * the current rendering which is invalidated is computed using
 * <code>RemoteDescriptor.getInvalidRegion()</code>. When a
 * <code>RemoteRenderedOp</code> node receives a
 * <code>RenderingChangeEvent</code> from one of its sources, the region of
 * the current rendering which is invalidated is computed using
 * the <code>mapSourceRect()</code> method of the current rendering and
 * the invalid region of the source (retrieved using
 * <code>RenderingChangeEvent.getInvalidRegion()</code>)
 * If the complement of the invalid region contains any tiles of the
 * current rendering, a new rendering of the node will be generated using
 * the new source node and its rendering generated using that version of
 * <code>RemoteRIF.create</code>() that updates the rendering of the node
 * according to the specified <code>PropertyChangeEventJAI</code>. The
 * identified tiles will be retained from the old rendering insofar as
 * possible.  This might involve for example adding tiles to a
 * <code>TileCache</code> under the ownership of the new rendering.
 * A <code>RenderingChangeEvent</code> will then be fired to all
 * <code>PropertyChangeListener</code>s of the node, and to any node sinks
 * that are <code>PropertyChangeListener</code>s. The
 * <code>newRendering</code> parameter of the event constructor
 * (which may be retrieved via the <code>getNewValue()</code> method of
 * the event) will be set to either the new rendering of the node or to
 * <code>null</code> if it was not possible to retain any tiles of the
 * previous rendering.
 *
 * @see RenderedOp
 * @see RemoteRenderedImage
 *
 * @since JAI 1.1
 */
public class RemoteRenderedOp extends RenderedOp
    implements RemoteRenderedImage {

    /** The name of the protocol this class provides an implementation for. */
    protected String protocolName;

    /** The name of the server. */
    protected String serverName;

    // The NegotiableCapabilitySet representing the negotiated values.
    private NegotiableCapabilitySet negotiated;

    /**
     * The RenderingHints when the node was last rendered, i.e., when
     * "theImage" was set to its current value.
     */
    private transient RenderingHints oldHints;

    /** Node event names. */
    private static Set nodeEventNames = null;

    static {
	nodeEventNames = new HashSet();
        nodeEventNames.add("protocolname");
        nodeEventNames.add("servername");
        nodeEventNames.add("protocolandservername");
        nodeEventNames.add("operationname");
        nodeEventNames.add("operationregistry");
        nodeEventNames.add("parameterblock");
        nodeEventNames.add("sources");
        nodeEventNames.add("parameters");
        nodeEventNames.add("renderinghints");
    }

    /**
     * Constructs a <code>RemoteRenderedOp</code> that will be used to
     * instantiate a particular rendered operation to be performed remotely
     * using the default operation registry, the name of the remote imaging
     * protocol, the name of the server to perform the operation on, an
     * operation name, a <code>ParameterBlock</code>, and a set of
     * rendering hints.  All input parameters are saved by reference.
     *
     * <p> An <code>IllegalArgumentException</code> may
     * be thrown by the protocol specific classes at a later point, if
     * null is provided as the serverName argument and null is not
     * considered a valid server name by the specified protocol.
     *
     * <p> The <code>RenderingHints</code> may contain negotiation
     * preferences specified under the <code>KEY_NEGOTIATION_PREFERENCES</code>
     * key.
     *
     * @param protocolName The protocol name as a String.
     * @param serverName   The server name as a String.
     * @param opName       The operation name.
     * @param pb           The sources and parameters. If <code>null</code>,
     *                     it is assumed that this node has no sources and
     *                     parameters.
     * @param hints        The rendering hints.  If <code>null</code>, it is
     *                     assumed that no hints are associated with the
     *                     rendering.
     *
     * @throws IllegalArgumentException if <code>protocolName</code> is
     * <code>null</code>.
     * @throws IllegalArgumentException if <code>opName</code> is
     * <code>null</code>.
     */
    public RemoteRenderedOp(String protocolName,
			    String serverName,
			    String opName,
			    ParameterBlock pb,
			    RenderingHints hints) {
	this(null, protocolName, serverName, opName, pb, hints);
    }

    /**
     * Constructs a <code>RemoteRenderedOp</code> that will be used to
     * instantiate a particular rendered operation to be performed remotely
     * using the specified operation registry, the name of the remote imaging
     * protocol, the name of the server to perform the operation on, an
     * operation name, a <code>ParameterBlock</code>, and a set of
     * rendering hints.  All input parameters are saved by reference.
     *
     * <p> An <code>IllegalArgumentException</code> may
     * be thrown by the protocol specific classes at a later point, if
     * null is provided as the serverName argument and null is not
     * considered a valid server name by the specified protocol.
     *
     * <p> The <code>RenderingHints</code> may contain negotiation
     * preferences specified under the <code>KEY_NEGOTIATION_PREFERENCES</code>
     * key.
     *
     * @param registry     The <code>OperationRegistry</code> to be used for
     *                     instantiation.  if <code>null</code>, the default
     *                     registry is used.
     * @param protocolName The protocol name as a String.
     * @param serverName   The server name as a String.
     * @param opName       The operation name.
     * @param pb           The sources and parameters. If <code>null</code>,
     *                     it is assumed that this node has no sources and
     *                     parameters.
     * @param hints        The rendering hints.  If <code>null</code>, it is
     *                     assumed that no hints are associated with the
     *                     rendering.
     *
     * @throws IllegalArgumentException if <code>protocolName</code> is
     * <code>null</code>.
     * @throws IllegalArgumentException if <code>opName</code> is
     * <code>null</code>.
     */
    public RemoteRenderedOp(OperationRegistry registry,
			    String protocolName,
			    String serverName,
			    String opName,
			    ParameterBlock pb,
			    RenderingHints hints) {

	// This will throw IAE for opName if null
	super(registry, opName, pb, hints);

	if (protocolName == null)
	    throw new IllegalArgumentException(JaiI18N.getString("Generic1"));

	this.protocolName = protocolName;
	this.serverName = serverName;

	// Add the node as a PropertyChangeListener of itself for
        // the critical attributes of the node. Superclass RenderedOp
	// takes care of all critical attributes except the following.
	// Case is ignored in the property names but infix caps are
	// used here anyway.
	addPropertyChangeListener("ServerName", this);
        addPropertyChangeListener("ProtocolName", this);
        addPropertyChangeListener("ProtocolAndServerName", this);
    }

    /**
     * Returns the <code>String</code> that identifies the server.
     */
    public String getServerName() {
	return serverName;
    }

    /**
     * Sets a <code>String</code> identifying the server.
     *
     * <p> If the supplied name does not equal the current server name, a
     * <code>PropertyChangeEventJAI</code> named "ServerName"
     * will be fired and a <code>RenderingChangeEvent</code> may be
     * fired if the node has already been rendered. The oldValue
     * field in the <code>PropertyChangeEventJAI</code> will contain
     * the old server name <code>String</code> and the newValue
     * field will contain the new server name <code>String</code>.
     *
     * @param serverName A <code>String</code> identifying the server.
     * @throws IllegalArgumentException if serverName is null.
     */
    public void setServerName(String serverName) {

	if (serverName == null)
	    throw new IllegalArgumentException(JaiI18N.getString("Generic2"));

	if (serverName.equalsIgnoreCase(this.serverName)) return;

	String oldServerName = this.serverName;
	this.serverName = serverName;
	fireEvent("ServerName", oldServerName, serverName);
        nodeSupport.resetPropertyEnvironment(false);
    }

    /**
     * Returns the <code>String</code> that identifies the remote imaging
     * protocol.
     */
    public String getProtocolName() {
	return protocolName;
    }

    /**
     * Sets a <code>String</code> identifying the remote imaging protocol.
     * This method causes this <code>RemoteRenderedOp</code> to use
     * the new protocol name with the server name set on this node
     * previously. If the server is not compliant with the new
     * protocol name, the <code>setProtocolAndServerNames()</code>
     * method should be used to set a new protocol name and a compliant
     * new server name at the same time.
     *
     * <p> If the supplied name does not equal the current protocol name, a
     * <code>PropertyChangeEventJAI</code> named "ProtocolName"
     * will be fired and a <code>RenderingChangeEvent</code> may be
     * fired if the node has already been rendered. The oldValue
     * field in the <code>PropertyChangeEventJAI</code> will contain
     * the old protocol name <code>String</code> and the newValue
     * field will contain the new protocol name <code>String</code>.
     *
     * @param protocolName A <code>String</code> identifying the server.
     * @throws IllegalArgumentException if protocolName is null.
     */
    public void setProtocolName(String protocolName) {

	if (protocolName == null)
	    throw new IllegalArgumentException(JaiI18N.getString("Generic1"));

	if (protocolName.equalsIgnoreCase(this.protocolName)) return;

	String oldProtocolName = this.protocolName;
	this.protocolName = protocolName;
	fireEvent("ProtocolName", oldProtocolName, protocolName);
        nodeSupport.resetPropertyEnvironment(false);
    }

    /**
     * Sets the protocol name and the server name of this
     * <code>RemoteRenderedOp</code> to the specified arguments..
     *
     * <p> If both the supplied protocol name and the supplied server
     * name values do not equal the current values, a
     * <code>PropertyChangeEventJAI</code> named "ProtocolAndServerName"
     * will be fired. The oldValue field in the
     * <code>PropertyChangeEventJAI</code> will contain a two element
     * array of <code>String</code>s, the old protocol name being the
     * first element and the old server name being the second. Similarly
     * the newValue field of the <code>PropertyChangeEventJAI</code> will
     * contain a two element array of <code>String</code>s, the new protocol
     * name being the first element and the new server name being the
     * second. If only the supplied protocol name does not equal
     * the current protocol name, a <code>PropertyChangeEventJAI</code>
     * named "ProtocolName" will be fired. If only the supplied server
     * name does not equal the current server name, a
     * <code>PropertyChangeEventJAI</code> named "ServerName"
     * will be fired.
     *
     * @param protocolName A <code>String</code> identifying the protocol.
     * @param serverName A <code>String</code> identifying the server.
     * @throws IllegalArgumentException if protocolName is null.
     * @throws IllegalArgumentException if serverName is null.
     */
    public void setProtocolAndServerNames(String protocolName,
					  String serverName) {

	if (serverName == null)
	    throw new IllegalArgumentException(JaiI18N.getString("Generic2"));

	if (protocolName == null)
	    throw new IllegalArgumentException(JaiI18N.getString("Generic1"));

	boolean protocolNotChanged =
	    protocolName.equalsIgnoreCase(this.protocolName);
	boolean serverNotChanged =
	    serverName.equalsIgnoreCase(this.serverName);

	if (protocolNotChanged) {
	    if (serverNotChanged)
		// Neither changed
		return;
	    else {
		// Only serverName changed
		setServerName(serverName);
		return;
	    }
	} else {
	    if (serverNotChanged) {
		// Only protocolName changed
		setProtocolName(protocolName);
		return;
	    }
	}

	String oldProtocolName = this.protocolName;
	String oldServerName = this.serverName;
	this.protocolName = protocolName;
	this.serverName = serverName;

	// Both changed
	fireEvent("ProtocolAndServerName",
		  new String[] {oldProtocolName, oldServerName},
		  new String[] {protocolName, serverName});
        nodeSupport.resetPropertyEnvironment(false);
    }

    /**
     * Returns the name of the <code>RegistryMode</code> corresponding to
     * this <code>RemoteRenderedOp</code>.  This method overrides the
     * implementation in <code>RenderedOp</code> to always returns the
     * <code>String</code> "remoteRendered".
     */
    public String getRegistryModeName() {
        return RegistryMode.getMode("remoteRendered").getName();
    }

    /**
     * Overrides the <code>RenderedOp</code> method to allow the operation
     * to be performed remotely.
     */
    protected synchronized PlanarImage createInstance(boolean isNodeRendered) {

        ParameterBlock pb = new ParameterBlock();
        pb.setParameters(getParameters());

        int numSources = getNumSources();

        for (int i = 0; i < numSources; i++) {

            Object source = getNodeSource(i);
            Object ai = null;
	    if (source instanceof RenderedOp) {

                RenderedOp src = (RenderedOp)source;
		ai = isNodeRendered ?
                    src.getRendering() :
                    src.createInstance();

            } else if ((source instanceof RenderedImage) ||
                       (source instanceof Collection)) {

                ai = source;
	    } else if (source instanceof CollectionOp) {
                ai = ((CollectionOp)source).getCollection();
            } else {
                /* Source is some other type. Pass on (for now). */
                ai = source;
            }
            pb.addSource(ai);
        }

	RemoteRenderedImage instance =
	    RemoteRIFRegistry.create(nodeSupport.getRegistry(),
				     protocolName,
				     serverName,
				     nodeSupport.getOperationName(),
				     pb,
				     nodeSupport.getRenderingHints());

        // Throw an exception if the rendering is null.
        if (instance == null) {
            throw new ImagingException(JaiI18N.getString("RemoteRenderedOp2"));
        }

	// Save the state of the node.
	RenderingHints rh = nodeSupport.getRenderingHints();
        oldHints = rh == null ? null : (RenderingHints)rh.clone();

	// Ensure that the rendering is a PlanarImage.
        return PlanarImage.wrapRenderedImage(instance);
    }

    /* ----- PropertyChangeListener method. ----- */

    /**
     * Implementation of <code>PropertyChangeListener</code>.
     *
     * <p> When invoked with an event which is an instance of
     * <code>RenderingChangeEvent</code> the node will respond by
     * re-rendering itself while retaining any tiles possible.
     */
    // XXX Update javadoc both here and at class level.
    public synchronized void propertyChange(PropertyChangeEvent evt) {

        //
        // React if and only if the node has been rendered and
        // A: a non-PropertySourceChangeEvent PropertyChangeEventJAI
        //    was received from this node, or
        // B: a RenderingChangeEvent was received from a source node.
        //

        // Cache event and node sources.
        Object evtSrc = evt.getSource();
        Vector nodeSources = nodeSupport.getParameterBlock().getSources();

        // Get the name of the bean property and convert it to lower
        // case now for efficiency later.
        String propName = evt.getPropertyName().toLowerCase(Locale.ENGLISH);

        if (theImage != null &&
           ((evt instanceof PropertyChangeEventJAI &&
             evtSrc == this &&
             !(evt instanceof PropertySourceChangeEvent) &&
             nodeEventNames.contains(propName)) ||
            ((evt instanceof RenderingChangeEvent ||
              evt instanceof CollectionChangeEvent ||
              (evt instanceof PropertyChangeEventJAI &&
               evtSrc instanceof RenderedImage &&
               propName.equals("invalidregion"))) &&
             nodeSources.contains(evtSrc)))) {

            // Save the previous rendering.
            PlanarImage theOldImage = theImage;

            // Initialize the event flag.
            boolean shouldFireEvent = false;

            // Set default invalid region to null (the entire image).
            Shape invalidRegion = null;

            if (evtSrc == this &&
               (propName.equals("operationregistry") ||
		propName.equals("protocolname") ||
		propName.equals("protocolandservername"))) {

                // invalidate the entire rendering.
                shouldFireEvent = true;
                theImage = null;

            } else if (evt instanceof RenderingChangeEvent ||
                      (evtSrc instanceof RenderedImage &&
                       propName.equals("invalidregion"))) {

                // Set the event flag.
                shouldFireEvent = true;
                Shape srcInvalidRegion = null;

                if (evt instanceof RenderingChangeEvent) {

                    // RenderingChangeEvent presumably from a source
		    // RenderedOp.
                    RenderingChangeEvent rcEvent = (RenderingChangeEvent)evt;

                    // Get the invalidated region of the source.
                    srcInvalidRegion = rcEvent.getInvalidRegion();

                    // If entire source is invalid replace with source bounds.
                    if (srcInvalidRegion == null) {
                        srcInvalidRegion =
                            ((PlanarImage)rcEvent.getOldValue()).getBounds();
                    }
                } else {

                    // Get the invalidated region of the source.
                    srcInvalidRegion = (Shape)evt.getNewValue();

                    // If entire source is invalid replace with source bounds.
                    if (srcInvalidRegion == null) {
                        RenderedImage rSrc = (RenderedImage)evtSrc;
                        srcInvalidRegion =
                            new Rectangle(rSrc.getMinX(), rSrc.getMinY(),
                                          rSrc.getWidth(), rSrc.getHeight());
                    }
                }

                // Only process further if the rendering is a
		// PlanarImageServerProxy.
                if (!(theImage instanceof PlanarImageServerProxy)) {

                    // Clear the current rendering.
                    theImage = null;

                } else {

                    // Save the previous rendering as a PlanarImageServerProxy.
                    PlanarImageServerProxy oldPISP =
			(PlanarImageServerProxy)theImage;

                    // Cache source invalid bounds.
                    Rectangle srcInvalidBounds = srcInvalidRegion.getBounds();

                    // If bounds are empty, replace srcInvalidRegion with
                    // the complement of the image bounds within the
                    // bounds of all tiles.
                    if (srcInvalidBounds.isEmpty()) {
                        int x = oldPISP.tileXToX(oldPISP.getMinTileX());
                        int y = oldPISP.tileYToY(oldPISP.getMinTileY());
                        int w =
			    oldPISP.getNumXTiles() * oldPISP.getTileWidth();
                        int h =
			    oldPISP.getNumYTiles() * oldPISP.getTileHeight();
                        Rectangle tileBounds = new Rectangle(x, y, w, h);
                        Rectangle imageBounds = oldPISP.getBounds();
                        if (!tileBounds.equals(imageBounds)) {
                            Area tmpArea = new Area(tileBounds);
                            tmpArea.subtract(new Area(imageBounds));
                            srcInvalidRegion = tmpArea;
                            srcInvalidBounds = srcInvalidRegion.getBounds();
                        }
                    }

                    // ----- Determine invalid destination region. -----

                    boolean saveAllTiles = false;
                    ArrayList validTiles = null;
                    if (srcInvalidBounds.isEmpty()) {
                        invalidRegion = srcInvalidRegion;
                        saveAllTiles = true;

                    } else {

                        // Get index of source which changed.
                        int idx = nodeSources.indexOf(evtSrc);

                        // Determine bounds of invalid destination region.
                        Rectangle dstRegionBounds =
                            oldPISP.mapSourceRect(srcInvalidBounds, idx);

                        if (dstRegionBounds == null) {
                            dstRegionBounds = oldPISP.getBounds();
                        }

                        // Determine invalid destination region.
                        Point[] indices = getTileIndices(dstRegionBounds);
                        int numIndices = indices != null ? indices.length : 0;
                        GeneralPath gp = null;

                        for(int i = 0; i < numIndices; i++) {
                            if (i % 1000 == 0 && gp != null)
                                gp = new GeneralPath(new Area(gp));

                            Rectangle dstRect =
                                getTileRect(indices[i].x, indices[i].y);
                            Rectangle srcRect =
                                oldPISP.mapDestRect(dstRect, idx);
                            if(srcRect == null) {
                                gp = null;
                                break;
                            }
                            if(srcInvalidRegion.intersects(srcRect)) {
                                if(gp == null) {
                                    gp = new GeneralPath(dstRect);
                                } else {
                                    gp.append(dstRect, false);
                                }
                            } else {
                                if(validTiles == null) {
                                    validTiles = new ArrayList();
                                }
                                validTiles.add(indices[i]);
                            }
                        }

                        invalidRegion = (gp == null) ? null : new Area(gp);
                    }

                    // Retrieve the old TileCache.
                    TileCache oldCache = oldPISP.getTileCache();
		    theImage = null;

                    // Only perform further processing if there is a cache
                    // and there are tiles to save.
                    if (oldCache != null &&
			(saveAllTiles || validTiles != null)) {

			// Create new rendering
			newEventRendering(protocolName,
					  oldPISP,
					  (PropertyChangeEventJAI)evt);

                        // Only perform further processing if the new
                        // rendering is an OpImage with a non-null TileCache.
                        if (theImage instanceof PlanarImageServerProxy &&
                           ((PlanarImageServerProxy)theImage).getTileCache() !=
			   null) {
                            PlanarImageServerProxy newPISP =
				(PlanarImageServerProxy)theImage;
                            TileCache newCache = newPISP.getTileCache();

                            Object tileCacheMetric =
                                newPISP.getTileCacheMetric();

                            if (saveAllTiles) {
                                Raster[] tiles = oldCache.getTiles(oldPISP);
                                int numTiles = tiles == null ?
				    0 : tiles.length;
                                for(int i = 0; i < numTiles; i++) {
                                    Raster tile = tiles[i];
                                    int tx = newPISP.XToTileX(tile.getMinX());
                                    int ty = newPISP.YToTileY(tile.getMinY());
                                    newCache.add(newPISP,
                                                 tx, ty, tile,
					         tileCacheMetric);
                                }
                            } else { // save some, but not all, tiles
                                int numValidTiles = validTiles.size();
                                for(int i = 0; i < numValidTiles; i++) {
                                    Point tileIndex = (Point)validTiles.get(i);
                                    Raster tile =
                                        oldCache.getTile(oldPISP,
                                                         tileIndex.x,
                                                         tileIndex.y);
                                    if (tile != null) {
                                        newCache.add(newPISP,
                                                     tileIndex.x,
                                                     tileIndex.y,
                                                     tile,
						     tileCacheMetric);
                                    }
                                }
                            }
                        }
                    }
                }
            } else { // not op name or registry change nor RenderingChangeEvent
                ParameterBlock oldPB = null;
                ParameterBlock newPB = null;
		String oldServerName = serverName;
		String newServerName = serverName;

                boolean checkInvalidRegion = false;

		if (propName.equals("operationname")) {

		    if (theImage instanceof PlanarImageServerProxy) {
			newEventRendering(protocolName,
					  (PlanarImageServerProxy)theImage,
					  (PropertyChangeEventJAI)evt);
		    } else {
			theImage = null;
			createRendering();
		    }

		    // Do not set checkInvalidRegion to true, since there
		    // are no tiles to save for this case.

		    shouldFireEvent = true;

		    // XXX Do we need to do any evaluation of any
		    // DeferredData parameters.

		} else if (propName.equals("parameterblock")) {
                    oldPB = (ParameterBlock)evt.getOldValue();
                    newPB = (ParameterBlock)evt.getNewValue();
                    checkInvalidRegion = true;
                } else if (propName.equals("sources")) {
                    // Replace source(s)
                    Vector params =
			nodeSupport.getParameterBlock().getParameters();
                    oldPB = new ParameterBlock((Vector)evt.getOldValue(),
                                               params);
                    newPB = new ParameterBlock((Vector)evt.getNewValue(),
                                               params);
                    checkInvalidRegion = true;
                } else if (propName.equals("parameters")) {
                    // Replace parameter(s)
                    oldPB = new ParameterBlock(nodeSources,
                                               (Vector)evt.getOldValue());
                    newPB = new ParameterBlock(nodeSources,
                                               (Vector)evt.getNewValue());
                    checkInvalidRegion = true;
                } else if (propName.equals("renderinghints")) {
                    oldPB = newPB = nodeSupport.getParameterBlock();
                    checkInvalidRegion = true;
                } else if (propName.equals("servername")) {
		    oldPB = newPB = nodeSupport.getParameterBlock();
		    oldServerName = (String)evt.getOldValue();
		    newServerName = (String)evt.getNewValue();
		    checkInvalidRegion = true;
		} else if (evt instanceof CollectionChangeEvent) {
		    // Event from a CollectionOp source.
                    // Replace appropriate source.
                    int collectionIndex = nodeSources.indexOf(evtSrc);
                    Vector oldSources = (Vector)nodeSources.clone();
                    Vector newSources = (Vector)nodeSources.clone();
                    oldSources.set(collectionIndex, evt.getOldValue());
                    newSources.set(collectionIndex, evt.getNewValue());

                    Vector params =
                        nodeSupport.getParameterBlock().getParameters();

                    oldPB = new ParameterBlock(oldSources, params);
                    newPB = new ParameterBlock(newSources, params);

                    checkInvalidRegion = true;
                }

                if (checkInvalidRegion) {
                    // Set event flag.
                    shouldFireEvent = true;

                    // Get the associated RemoteDescriptor.
                    OperationRegistry registry = nodeSupport.getRegistry();
                    RemoteDescriptor odesc = (RemoteDescriptor)
                        registry.getDescriptor(RemoteDescriptor.class,
                                               protocolName);

		    // XXX
                    // Evaluate any DeferredData parameters.
                    oldPB = ImageUtil.evaluateParameters(oldPB);
                    newPB = ImageUtil.evaluateParameters(newPB);

                    // Determine the invalid region.
                    invalidRegion = (Shape)
                        odesc.getInvalidRegion("rendered",
					       oldServerName,
                                               oldPB,
                                               oldHints,
					       newServerName,
                                               newPB,
                                               nodeSupport.getRenderingHints(),
                                               this);

                    if (invalidRegion == null ||
                       !(theImage instanceof PlanarImageServerProxy)) {
                        // Can't save any tiles; clear the rendering.
                        theImage = null;

                    } else {

                        // Create a new rendering.
                        PlanarImageServerProxy oldRendering =
			    (PlanarImageServerProxy)theImage;

			newEventRendering(protocolName, oldRendering,
					  (PropertyChangeEventJAI)evt);

                        // If the new rendering is also a
			// PlanarImageServerProxy, save some tiles.
                        if (theImage instanceof PlanarImageServerProxy &&
			    oldRendering.getTileCache() != null &&
			    ((PlanarImageServerProxy)theImage).getTileCache()
			    != null) {
                            PlanarImageServerProxy newRendering =
				(PlanarImageServerProxy)theImage;

                            TileCache oldCache = oldRendering.getTileCache();
                            TileCache newCache = newRendering.getTileCache();

                            Object tileCacheMetric =
                                newRendering.getTileCacheMetric();

                            // If bounds are empty, replace invalidRegion with
                            // the complement of the image bounds within the
                            // bounds of all tiles.
                            if (invalidRegion.getBounds().isEmpty()) {
                                int x = oldRendering.tileXToX(
                                            oldRendering.getMinTileX());
                                int y = oldRendering.tileYToY(
                                            oldRendering.getMinTileY());
                                int w = oldRendering.getNumXTiles() *
                                    oldRendering.getTileWidth();
                                int h = oldRendering.getNumYTiles() *
                                    oldRendering.getTileHeight();
                                Rectangle tileBounds =
				    new Rectangle(x, y, w, h);
                                Rectangle imageBounds =
                                    oldRendering.getBounds();
                                if (!tileBounds.equals(imageBounds)) {
                                    Area tmpArea = new Area(tileBounds);
                                    tmpArea.subtract(new Area(imageBounds));
                                    invalidRegion = tmpArea;
                                }
                            }

                            if (invalidRegion.getBounds().isEmpty()) {

                                // Save all tiles.
                                Raster[] tiles =
                                    oldCache.getTiles(oldRendering);
                                int numTiles = tiles == null ?
                                    0 : tiles.length;
                                for(int i = 0; i < numTiles; i++) {
                                    Raster tile = tiles[i];
                                    int tx =
                                        newRendering.XToTileX(tile.getMinX());
                                    int ty =
                                        newRendering.YToTileY(tile.getMinY());
                                    newCache.add(newRendering,
                                                 tx, ty, tile,
						 tileCacheMetric);
                                }
                            } else {
                                // Copy tiles not in invalid region from old
                                // TileCache to new TileCache.
                                Raster[] tiles =
                                    oldCache.getTiles(oldRendering);
                                int numTiles = tiles == null ?
                                    0 : tiles.length;
                                for(int i = 0; i < numTiles; i++) {
                                    Raster tile = tiles[i];
                                    Rectangle bounds = tile.getBounds();
                                    if (!invalidRegion.intersects(bounds)) {
                                        newCache.add(
					      newRendering,
					      newRendering.XToTileX(bounds.x),
					      newRendering.YToTileY(bounds.y),
					      tile,
					      tileCacheMetric);
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Re-render the node. This will only occur if theImage
            // has been set to null above.
	    if (theOldImage instanceof PlanarImageServerProxy &&
		theImage == null) {
		newEventRendering(protocolName,
				  (PlanarImageServerProxy)theOldImage,
				  (PropertyChangeEventJAI)evt);
	    } else {
		createRendering();
	    }

            // Fire an event if the flag was set.
            if (shouldFireEvent) {

                // Clear the synthetic and cached properties and reset the
                // property source.
		resetProperties(true);

                // Create the event object.
                RenderingChangeEvent rcEvent =
                    new RenderingChangeEvent(this, theOldImage, theImage,
                                             invalidRegion);

                // Fire to all registered listeners.
                eventManager.firePropertyChange(rcEvent);

		// Fire an event to all PropertyChangeListener sinks.
		Vector sinks = getSinks();
		if (sinks != null) {
		    int numSinks = sinks.size();
		    for (int i = 0; i < numSinks; i++) {
			Object sink = sinks.get(i);
			if (sink instanceof PropertyChangeListener) {
			    ((PropertyChangeListener)sink).propertyChange(rcEvent);
			}
		    }
		}
	    }
	}
    }

    /**
     * Creates a new rendering in response to the provided event, and
     * assigns the new rendering to "theImage" variable.
     */
    private void newEventRendering(String protocolName,
				   PlanarImageServerProxy oldPISP,
				   PropertyChangeEventJAI event) {
	RemoteRIF rrif = (RemoteRIF)nodeSupport.getRegistry().
	    getFactory("remoterendered", protocolName);
	theImage = (PlanarImage)rrif.create(oldPISP, this, event);
    }

    /**
     * Fire an events to all registered listeners.
     */
    private void fireEvent(String propName, Object oldVal, Object newVal) {
        if (eventManager != null) {
            Object eventSource = eventManager.getPropertyChangeEventSource();
            PropertyChangeEventJAI evt =
                new PropertyChangeEventJAI(eventSource,
                                           propName, oldVal, newVal);
            eventManager.firePropertyChange(evt);
        }
    }

    /**
     * Returns the amount of time between retries in milliseconds.
     *
     * <p> If this <code>RemoteRenderedOp</code> has been rendered, then its
     * <code>getRetryInterval()</code> method will be called to return
     * the current retry interval. If no rendering has been created, and
     * a value was set using the <code>setRetryInterval()</code> method), that
     * value will be returned, else the default retry interval as defined by
     * <code>RemoteJAI.DEFAULT_RETRY_INTERVAL</code> is returned.
     */
    public int getRetryInterval() {
	if (theImage != null) {
	    return ((RemoteRenderedImage)theImage).getRetryInterval();
	} else {
	    RenderingHints rh = nodeSupport.getRenderingHints();
	    if (rh == null) {
		return RemoteJAI.DEFAULT_RETRY_INTERVAL;
	    } else {
		Integer i = (Integer)rh.get(JAI.KEY_RETRY_INTERVAL);
		if (i == null)
		    return RemoteJAI.DEFAULT_RETRY_INTERVAL;
		else
		    return i.intValue();
	    }
	}
    }

    /**
     * Sets the amount of time between retries in milliseconds. If this
     * <code>RemoteRenderedOp</code> has already been rendered, the
     * <code>setRetryInterval()</code> method is called on the rendering
     * to inform it of the new retry interval. The rendering can choose to
     * ignore this new setting, in which case <code>getRetryInterval()</code>
     * will still return the old value, or the rendering can honor these
     * settings, in which case <code>getRetryInterval()</code> will return
     * the new settings. If this <code>RemoteRenderedOp</code> has not been
     * rendered, the new retry interval specified will be stored.
     * These new stored retry interval will be passed as
     * part of the <code>RenderingHints</code> using the
     * <code>KEY_RETRY_INTERVAL</code> key, to the new rendering
     * when it is created.
     *
     * @param retryInterval The amount of time (in milliseconds) to wait
     *                      between retries.
     * @throws IllegalArgumentException if retryInterval is negative.
     */
    public void setRetryInterval(int retryInterval) {

	if (retryInterval < 0)
	    throw new IllegalArgumentException(JaiI18N.getString("Generic3"));

	if (theImage != null) {
	    ((RemoteRenderedImage)theImage).setRetryInterval(retryInterval);
	}

	RenderingHints rh = nodeSupport.getRenderingHints();
	if (rh == null) {
	    nodeSupport.setRenderingHints(new RenderingHints(null));
	    rh = nodeSupport.getRenderingHints();
	}

	rh.put(JAI.KEY_RETRY_INTERVAL, new Integer(retryInterval));
    }

    /**
     * Returns the number of retries.
     *
     * <p> If this <code>RemoteRenderedOp</code> has been rendered, then its
     * <code>getNumRetries()</code> method will be called to return
     * the current number of retries. If no rendering has been created, and
     * a value was set using the <code>setNumRetries()</code> method), that
     * value will be returned, else the default retry interval as defined by
     * <code>RemoteJAI.DEFAULT_NUM_RETRIES</code> is returned.
     */
    public int getNumRetries() {
	if (theImage != null) {
	    return ((RemoteRenderedImage)theImage).getNumRetries();
	} else {
	    RenderingHints rh = nodeSupport.getRenderingHints();
	    if (rh == null) {
		return RemoteJAI.DEFAULT_NUM_RETRIES;
	    } else {
		Integer i = (Integer)rh.get(JAI.KEY_NUM_RETRIES);
		if (i == null)
		    return RemoteJAI.DEFAULT_NUM_RETRIES;
		else
		    return i.intValue();
	    }
	}
    }

    /**
     * Sets the number of retries. If this <code>RemoteRenderedOp</code>
     * has already been rendered, the <code>setNumRetries()</code> method
     * is called on the rendering to inform it of the new number of retries.
     * The rendering can choose to ignore these new settings, in which case
     * <code>getNunRetries()</code> will still return the old values, or
     * the rendering can honor these new settings in which
     * case <code>getNumRetries()</code> will return the new value.
     * If this <code>RemoteRenderedOp</code> has not been rendered,
     * the new setting specified will be stored.
     * These new settings which have been stored will be passed as
     * part of the <code>RenderingHints</code> using the
     * <code>KEY_NUM_RETRIES</code> key, to the new rendering
     * when it is created.
     *
     * @param numRetries The number of times an operation should be retried
     *                   in case of a network error.
     * @throws IllegalArgumentException if numRetries is negative.
     */
    public void setNumRetries(int numRetries) {

	if (numRetries < 0)
	    throw new IllegalArgumentException(
				      JaiI18N.getString("Generic4"));

	if (theImage != null) {
	    ((RemoteRenderedImage)theImage).setNumRetries(numRetries);
	}

	RenderingHints rh = nodeSupport.getRenderingHints();
	if (rh == null) {
	    nodeSupport.setRenderingHints(new RenderingHints(null));
	    rh = nodeSupport.getRenderingHints();
	}

	rh.put(JAI.KEY_NUM_RETRIES, new Integer(numRetries));
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
     * cycles by allowing negotiation preferences to be set
     * multiple times. If this <code>RemoteRenderedOp</code> has already
     * been rendered, the <code>setNegotiationPreferences()</code> method
     * is called on the rendering to inform it of the new preferences. The
     * rendering can choose to ignore these new preferences, in which case
     * <code>getNegotiatedValues()</code> will still return the results of
     * the old negotiation, or the rendering can re-perform the negotiation,
     * (using the <code>RemoteJAI.negotiate</code>, for example) in which
     * case <code>getNegotiatedValues()</code> will return the new
     * negotiated values. If this <code>RemoteRenderedOp</code> has not been
     * rendered, the new preferences specified will be stored, a negotiation
     * with these new preferences will be initiated and the results stored.
     * These new preferences which have been stored will be passed as
     * part of the <code>RenderingHints</code> using the
     * <code>KEY_NEGOTIATION_PREFERENCES</code> key, to the new rendering
     * when it is created.
     *
     * @param preferences The preferences to be used in the negotiation
     * process.
     */
    public void setNegotiationPreferences(NegotiableCapabilitySet preferences)
    {
	if (theImage != null) {
	    ((RemoteRenderedImage)theImage).setNegotiationPreferences(
								 preferences);
	}

	RenderingHints rh = nodeSupport.getRenderingHints();

	if (preferences != null) {
	    if (rh == null) {
		nodeSupport.setRenderingHints(new RenderingHints(null));
		rh = nodeSupport.getRenderingHints();
	    }

	    rh.put(JAI.KEY_NEGOTIATION_PREFERENCES, preferences);
	} else {
	    // Remove any previous values set for negotiation preferences
	    if (rh != null) {
		rh.remove(JAI.KEY_NEGOTIATION_PREFERENCES);
	    }
	}

	negotiated = negotiate(preferences);
    }

    /**
     * Returns the current negotiation preferences or null, if none were
     * set previously.
     */
    public NegotiableCapabilitySet getNegotiationPreferences() {

	RenderingHints rh = nodeSupport.getRenderingHints();
	return rh == null ? null : (NegotiableCapabilitySet)rh.get(
					     JAI.KEY_NEGOTIATION_PREFERENCES);
    }

    // do the negotiation
    private NegotiableCapabilitySet negotiate(NegotiableCapabilitySet prefs) {

	OperationRegistry registry = nodeSupport.getRegistry();

	NegotiableCapabilitySet serverCap = null;

	// Get the RemoteDescriptor for protocolName
	RemoteDescriptor descriptor = (RemoteDescriptor)
	    registry.getDescriptor(RemoteDescriptor.class, protocolName);

	if (descriptor == null) {
	    Object[] msgArg0 = {new String(protocolName)};
	    MessageFormat formatter = new MessageFormat("");
	    formatter.setLocale(Locale.getDefault());
	    formatter.applyPattern(JaiI18N.getString("RemoteJAI16"));
	    throw new ImagingException(formatter.format(msgArg0));
	}

	int count=0;
	int numRetries = getNumRetries();
	int retryInterval = getRetryInterval();

	Exception rieSave = null;
	while (count++ < numRetries) {
	    try {
		serverCap = descriptor.getServerCapabilities(serverName);
		break;
	    } catch (RemoteImagingException rie) {
		// Print that an Exception occured
		System.err.println(JaiI18N.getString("RemoteJAI24"));
		rieSave = rie;
		// Sleep for retryInterval milliseconds
		try {
		    Thread.sleep(retryInterval);
		} catch (InterruptedException ie) {
//		    throw new RuntimeException(ie.toString());
                    sendExceptionToListener(JaiI18N.getString("Generic5"),
                                            new ImagingException(JaiI18N.getString("Generic5"), ie));
		}
	    }
	}

	if (serverCap == null && count > numRetries) {
            sendExceptionToListener(JaiI18N.getString("RemoteJAI18"), rieSave);
//	    throw new RemoteImagingException(JaiI18N.getString("RemoteJAI18")+"\n"+rieSave.getMessage());
	}

	RemoteRIF rrif = (RemoteRIF)registry.getFactory("remoteRendered",
							protocolName);

	return RemoteJAI.negotiate(prefs,
				   serverCap,
				   rrif.getClientCapabilities());
    }

    /**
     * Returns the results of the negotiation between the client and server
     * capabilities according to the preferences set via the
     * <code>setNegotiationPreferences()</code> method. This will return
     * null if no negotiation preferences were set, and no negotiation
     * was performed, or if the negotiation failed.
     *
     * <p> If this <code>RemoteRenderedOp</code> has been rendered, then its
     * <code>getNegotiatedValues()</code> method will be called to return
     * the current negotiated values. If no rendering has been created, then
     * the internally stored negotiated value (calculated when the new
     * preferences were set using the <code>setNegotiationPreferences()</code>
     * method) will be returned.
     */
    public NegotiableCapabilitySet getNegotiatedValues()
	throws RemoteImagingException {
	if (theImage != null) {
	    return ((RemoteRenderedImage)theImage).getNegotiatedValues();
	} else {
	    return negotiated;
	}
    }

    /**
     * Returns the results of the negotiation between the client and server
     * capabilities for the given category according to the preferences
     * set via the <code>setNegotiationPreferences()</code> method. This
     * will return null if no negotiation preferences were set, and no
     * negotiation was performed, or if the negotiation failed.
     *
     * <p> If this <code>RemoteRenderedOp</code> has been rendered, then its
     * <code>getNegotiatedValues()</code> method will be called to return
     * the current negotiated values. If no rendering has been created, then
     * the internally stored negotiated value (calculated when the new
     * preferences were set using the <code>setNegotiationPreferences()</code>
     * method) will be returned.
     *
     * @param category The category to return the negotiated results for.
     */
    public NegotiableCapability getNegotiatedValue(String category)
	throws RemoteImagingException {
	if (theImage != null) {
	    return ((RemoteRenderedImage)theImage).getNegotiatedValue(
								    category);
	} else {
	    return negotiated == null ? null :
		negotiated.getNegotiatedValue(category);
	}
    }

    /**
     * Informs the server of the negotiated values that are the result of
     * a successful negotiation. If this <code>RemoteRenderedOp</code> has
     * been rendered, then the rendering's
     * <code>setServerNegotiatedValues</code> method will be called to
     * inform the server of the negotiated results. If no rendering has
     * been created, this method will do nothing.
     *
     * @param negotiatedValues    The result of the negotiation.
     */
    public void setServerNegotiatedValues(NegotiableCapabilitySet
					  negotiatedValues)
	throws RemoteImagingException {

	if (theImage != null) {
	    ((RemoteRenderedImage)theImage).setServerNegotiatedValues(
							    negotiatedValues);
	}
    }

    void sendExceptionToListener(String message, Exception e) {
        ImagingListener listener =
            (ImagingListener)getRenderingHints().get(JAI.KEY_IMAGING_LISTENER);

        listener.errorOccurred(message, e, this, false);
    }
}
