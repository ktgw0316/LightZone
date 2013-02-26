/*
 * $RCSfile: RemoteRenderableOp.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.2 $
 * $Date: 2005/05/12 18:24:35 $
 * $State: Exp $
 */package com.lightcrafts.mediax.jai.remote;

import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderContext;
import java.awt.image.renderable.RenderableImage;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;
import com.lightcrafts.mediax.jai.JAI;
import com.lightcrafts.mediax.jai.OperationRegistry;
import com.lightcrafts.mediax.jai.PropertyChangeEventJAI;
import com.lightcrafts.mediax.jai.RenderedOp;
import com.lightcrafts.mediax.jai.RenderableOp;
import com.lightcrafts.mediax.jai.RegistryMode;
import com.lightcrafts.mediax.jai.WritablePropertySource;
import com.lightcrafts.mediax.jai.registry.RemoteCRIFRegistry;
import com.lightcrafts.mediax.jai.util.ImagingException;
import com.lightcrafts.mediax.jai.util.ImagingListener;

/**
 * A subclass of <code>RenderableOp</code> for remote operations. This
 * class represents a node in a remote renderable imaging
 * chain. A <code>RemoteRenderableOp</code> stores a protocol name (as a
 * <code>String</code>), a server name (as a <code>String</code>), an
 * operation name (as a <code>String</code>), and a
 * <code>ParameterBlock</code> containing sources and miscellaneous
 * parameters.
 *
 * <p> By virtue of being a subclass of <code>RemoteRenderableOp</code>,
 * this class participates in Java Bean-style events as specified by
 * <code>RenderableOp</code>. <code>RemoteRenderableOp</code>s add the
 * server name and the protocol name to the critical attributes, the
 * editing (chaging) of which may cause a <code>PropertyChangeEventJAI</code>
 * to be emitted.
 *
 * @see com.lightcrafts.mediax.jai.RenderableOp
 *
 * @since JAI 1.1
 */
public class RemoteRenderableOp extends RenderableOp {

    /** The name of the protocol this class provides an implementation for. */
    protected String protocolName;

    /** The name of the server. */
    protected String serverName;

    // The RemoteCRIF used to create a rendering.
    private transient RemoteCRIF remoteCRIF = null;

    // The NegotiableCapabilitySet representing the negotiated values.
    private NegotiableCapabilitySet negotiated = null;

    // A reference to the RMIServerProxy which connects to the corresponding
    // RenderableOp on the server. Holding this reference ensures that the
    // RMIServerProxy doesn't get garbage-collected.
    private transient RenderedImage linkToRemoteOp;

    /**
     * Constructs a <code>RemoteRenderableOp</code> using the default
     * operation registry, given the name of the remote imaging protocol,
     * the name of the server to perform the operation on, the name of the
     * operation to be performed remotely and a <code>ParameterBlock</code>
     * containing <code>RenderableImage</code> sources and other parameters.
     * Any <code>RenderedImage</code> sources referenced by the
     * <code>ParameterBlock</code> will be ignored.
     *
     * <p> An <code>IllegalArgumentException</code> may
     * be thrown by the protocol specific classes at a later point, if
     * null is provided as the serverName argument and null is not
     * considered a valid server name by the specified protocol.
     *
     * @param protocolName The protocol name as a <code>String</code>.
     * @param serverName   The server name as a <code>String</code>.
     * @param opName       The operation name.
     * @param pb           The sources and other parameters. If
     *                     <code>null</code>, it is assumed that this node has
     *                     no sources and parameters.
     *
     * @throws IllegalArgumentException if <code>protocolName</code> is
     * <code>null</code>.
     * @throws IllegalArgumentException if <code>opName</code> is
     * <code>null</code>.
     */
    public RemoteRenderableOp(String protocolName,
			      String serverName,
			      String opName,
			      ParameterBlock pb) {
        this(null, protocolName, serverName, opName, pb);
    }

    /**
     * Constructs a <code>RemoteRenderableOp</code> using the specified
     * operation registry, given the name of the remote imaging protocol,
     * the name of the server to perform the operation on, the name of the
     * operation to be performed remotely and a <code>ParameterBlock</code>
     * containing <code>RenderableImage</code> sources and other parameters.
     * Any <code>RenderedImage</code> sources referenced by the
     * <code>ParameterBlock</code> will be ignored.
     *
     * <p> An <code>IllegalArgumentException</code> may
     * be thrown by the protocol specific classes at a later point, if
     * null is provided as the serverName argument and null is not
     * considered a valid server name by the specified protocol.
     *
     * @param registry     The <code>OperationRegistry</code> to be used for
     *                     instantiation.  if <code>null</code>, the default
     *                     registry is used.
     * @param protocolName The protocol name as a <code>String</code>.
     * @param serverName   The server name as a <code>String</code>.
     * @param opName       The operation name.
     * @param pb           The sources and other parameters. If
     *                     <code>null</code>, it is assumed that this node has
     *                     no sources and parameters.
     *
     * @throws IllegalArgumentException if <code>protocolName</code> is
     * <code>null</code>.
     * @throws IllegalArgumentException if <code>opName</code> is
     * <code>null</code>.
     */
    public RemoteRenderableOp(OperationRegistry registry,
			      String protocolName,
			      String serverName,
			      String opName,
			      ParameterBlock pb) {

	super(registry, opName, pb);

        if (protocolName == null || opName == null) {
            throw new IllegalArgumentException();
        }

	this.protocolName = protocolName;
	this.serverName = serverName;
    }

    /**
     * Returns the name of the <code>RegistryMode</code> corresponding to
     * this <code>RenderableOp</code>.  This method overrides the
     * implementation in <code>RenderableOp</code> to always returns the
     * <code>String</code> "remoteRenderable".
     */
    public String getRegistryModeName() {
        return RegistryMode.getMode("remoteRenderable").getName();
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
     * will be fired. The oldValue field in the
     * <code>PropertyChangeEventJAI</code> will contain the old server
     * name <code>String</code> and the newValue field will contain the
     * new server name <code>String</code>.
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
     *
     * <p> If the supplied name does not equal the current protocol name, a
     * <code>PropertyChangeEventJAI</code> named "ProtocolName"
     * will be fired. The oldValue field in the
     * <code>PropertyChangeEventJAI</code> will contain the old protocol
     * name <code>String</code> and the newValue field will contain the
     * new protocol name <code>String</code>.
     *
     * @param protocolName A <code>String</code> identifying the protocol.
     * @throws IllegalArgumentException if protocolName is null.
     */
    public void setProtocolName(String protocolName) {

	if (protocolName == null)
	    throw new IllegalArgumentException(
			       JaiI18N.getString("Generic1"));

	if (protocolName.equalsIgnoreCase(this.protocolName)) return;

	String oldProtocolName = this.protocolName;
	this.protocolName = protocolName;
	fireEvent("ProtocolName", oldProtocolName, protocolName);
        nodeSupport.resetPropertyEnvironment(false);
    }

    /**
     * Sets the protocol name and the server name of this
     * <code>RemoteRenderableOp</code> to the specified arguments..
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

    // Fire an event to all listeners registered with this node.
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
     * Overrides the method in <code>RenderableOp</code> to return the
     * rendering-independent width of the image, as queried from the remote
     * server.
     *
     * @return the image width as a float.
     */
    public float getWidth() {

	findRemoteCRIF();
        Rectangle2D boundingBox =
            remoteCRIF.getBounds2D(serverName,
                                   nodeSupport.getOperationName(),
                                   nodeSupport.getParameterBlock());

	return (float)boundingBox.getWidth();
    }

    /**
     * Overrides the method in <code>RenderableOp</code> to return the
     * rendering-independent height of the image, as queried from the remote
     * server.
     *
     * @return the image height as a float.
     */
    public float getHeight() {

	findRemoteCRIF();
        Rectangle2D boundingBox =
            remoteCRIF.getBounds2D(serverName,
                                   nodeSupport.getOperationName(),
                                   nodeSupport.getParameterBlock());

	return (float)boundingBox.getHeight();
    }

    /**
     * Overrides the method in <code>RenderableOp</code> to return the
     * minimum X coordinate of the rendering-independent image data, as
     * queried from the remote server.
     */
    public float getMinX() {

	findRemoteCRIF();
        Rectangle2D boundingBox =
            remoteCRIF.getBounds2D(serverName,
                                   nodeSupport.getOperationName(),
                                   nodeSupport.getParameterBlock());

	return (float)boundingBox.getX();
    }

    /**
     * Overrides the method in <code>RenderableOp</code> to return the
     * maximum X coordinate of the rendering-independent image data, as
     * queried from the remote server.
     */
    public float getMinY() {

	findRemoteCRIF();
        Rectangle2D boundingBox =
            remoteCRIF.getBounds2D(serverName,
                                   nodeSupport.getOperationName(),
                                   nodeSupport.getParameterBlock());

	return (float)boundingBox.getY();
    }

    /**
     * Overrides the <code>RenderableOp</code> method to return a
     * <code>RemoteRenderedImage</code> that represents the remote rendering
     * of this image using a given <code>RenderContext</code>.  This is the
     * most general way to obtain a rendering of a
     * <code>RemoteRenderableOp</code>.
     *
     * <p> This method does not validate sources and parameters supplied
     * in the <code>ParameterBlock</code> against the specification
     * of the operation this node represents.  It is the caller's
     * responsibility to ensure that the data in the
     * <code>ParameterBlock</code> are suitable for this operation.
     * Otherwise, some kind of exception or error will occur.
     *
     * <p> <code>RemoteJAI.createRenderable()</code> is the method that does
     * the validation.  Therefore, it is strongly recommended that all
     * <code>RemoteRenderableOp</code>s are created using
     * <code>RemoteJAI.createRenderable()</code>.
     *
     * <p> The <code>RenderContext</code> may contain a <code>Shape</code>
     * that represents the area-of-interest (aoi).  If the aoi is specifed,
     * it is still legal to return an image that's larger than this aoi.
     * Therefore, by default, the aoi, if specified, is ignored at the
     * rendering.
     *
     * <p> The <code>RenderingHints</code> in the <code>RenderContext</code>
     * may contain negotiation preferences specified under the
     * <code>KEY_NEGOTIATION_PREFERENCES</code> key. These preferences
     * can be ignored by the rendering if it so chooses.
     *
     * @param renderContext the RenderContext to use to produce the rendering.
     * @return a RemoteRenderedImage containing the rendered data.
     */
    public RenderedImage createRendering(RenderContext renderContext) {

	findRemoteCRIF();

        // Clone the original ParameterBlock; if the ParameterBlock
        // contains RenderableImage sources, they will be replaced by
        // RenderedImages.
        ParameterBlock renderedPB =
	    (ParameterBlock)nodeSupport.getParameterBlock().clone();

	// If there are any hints set on the node, create a new
	// RenderContext which merges them with those in the RenderContext
	// passed in with the passed in hints taking precedence.
	RenderContext rcIn = renderContext;
	RenderingHints nodeHints = nodeSupport.getRenderingHints();
	if(nodeHints != null) {
	    RenderingHints hints = renderContext.getRenderingHints();
	    RenderingHints mergedHints;
	    if (hints == null) {
		mergedHints = nodeHints;
	    } else if (nodeHints == null || nodeHints.isEmpty()) {
		mergedHints = hints;
	    } else {
		mergedHints = new RenderingHints((Map)nodeHints);
                mergedHints.add(hints);
	    }

	    if(mergedHints != hints) {
		rcIn = new RenderContext(renderContext.getTransform(),
					 renderContext.getAreaOfInterest(),
					 mergedHints);
	    }
	}

	// Get all sources - whether rendered or renderable.
	Vector sources = nodeSupport.getParameterBlock().getSources();

        try {
            if (sources != null) {
                Vector renderedSources = new Vector();
                for (int i = 0; i < sources.size(); i++) {

		    RenderedImage rdrdImage = null;
		    Object source = sources.elementAt(i);
		    if (source instanceof RenderableImage) {
			RenderContext rcOut =
			    remoteCRIF.mapRenderContext(
					       serverName,
					       nodeSupport.getOperationName(),
					       i,
					       renderContext,
					       nodeSupport.getParameterBlock(),
					       this);

			RenderableImage src = (RenderableImage)source;
			rdrdImage = src.createRendering(rcOut);
		    }  else if (source instanceof RenderedOp) {
			rdrdImage = ((RenderedOp)source).getRendering();
		    } else if (source instanceof RenderedImage) {
			rdrdImage = (RenderedImage)source;
		    }

                    if (rdrdImage == null) {
                        return null;
                    }

                    // Add this rendered image to the ParameterBlock's
                    // list of RenderedImages.
                    renderedSources.addElement(rdrdImage);
                }

                if (renderedSources.size() > 0) {
                    renderedPB.setSources(renderedSources);
                }
            }

            RenderedImage rendering =
		remoteCRIF.create(serverName,
				  nodeSupport.getOperationName(),
				  renderContext,
				  renderedPB);

	    if (rendering instanceof RenderedOp) {
                rendering = ((RenderedOp)rendering).getRendering();
            }

	    // Save a reference to the RMIServerProxy that is the link to
	    // the RenderableOp on the server. We don't want this to be
	    // garbage collected.
	    linkToRemoteOp = rendering;

	    // Copy properties to the rendered node.
	    if(rendering != null &&
	       rendering instanceof WritablePropertySource) {
	        String[] propertyNames = getPropertyNames();
		if(propertyNames != null){
		    WritablePropertySource wps =
                        (WritablePropertySource)rendering;
		    for(int j = 0; j < propertyNames.length; j++) {
			String name = propertyNames[j];
			Object value = getProperty(name);
			if(value != null &&
			   value != java.awt.Image.UndefinedProperty) {
			    wps.setProperty(name, value);
			}
		    }
		}
	    }

	    return rendering;
        } catch (ArrayIndexOutOfBoundsException e) {
            // This should never happen
            return null;
        }
    }

    /** Use registry to find an appropriate RemoteCRIF */
    private RemoteCRIF findRemoteCRIF() {

	if (remoteCRIF == null) {
	    // find the RemoteCRIF from the registry.
	    remoteCRIF = RemoteCRIFRegistry.get(nodeSupport.getRegistry(),
						protocolName);

	    if (remoteCRIF == null) {
		throw new ImagingException(
				    JaiI18N.getString("RemoteRenderableOp0"));
	    }
	}

	return remoteCRIF;
    }

    /**
     * Returns the amount of time between retries in milliseconds. If
     * a value for the retry interval has been set previously by
     * <code>setRetryInterval()</code>, the same value is returned, else
     * the default retry interval as defined by
     * <code>RemoteJAI.DEFAULT_RETRY_INTERVAL</code> is returned.
     */
    public int getRetryInterval() {

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

    /**
     * Sets the amount of time between retries in milliseconds.
     *
     * <p> This new value for retry interval will be stored and will
     * be passed as <code>RenderingHints</code> as part
     * of the <code>RenderContext</code> used to create the rendering. The
     * <code>RenderingHints</code> in the <code>RenderContext</code> will
     * contain this information under the
     * <code>KEY_RETRY_INTERVAL</code> key. If the
     * <code>RenderingHints</code> in the <code>RenderContext</code> already
     * contains a retry interval value specified by the user, that will
     * take preference over the one stored in this class.
     *
     * @param retryInterval The amount of time (in milliseconds) to wait
     *                      between retries.
     * @throws IllegalArgumentException if retryInterval is negative.
     */
    public void setRetryInterval(int retryInterval) {

	if (retryInterval < 0)
	    throw new IllegalArgumentException(JaiI18N.getString("Generic3"));

	RenderingHints rh = nodeSupport.getRenderingHints();
	if (rh == null) {
	    RenderingHints hints = new RenderingHints(null);
	    nodeSupport.setRenderingHints(hints);
	}

	nodeSupport.getRenderingHints().put(JAI.KEY_RETRY_INTERVAL,
					    new Integer(retryInterval));
    }

    /**
     * Returns the number of retries. If a value for the number of retries
     * has been set previously by <code>setNumRetries()</code>, the same
     * value is returned, else the default number of retries as defined by
     * <code>RemoteJAI.DEFAULT_NUM_RETRIES</code> is returned.
     */
    public int getNumRetries() {

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

    /**
     * Sets the number of retries.
     *
     * <p> This new value for number of retries will be stored and will
     * be passed as <code>RenderingHints</code> as part
     * of the <code>RenderContext</code> used to create the rendering. The
     * <code>RenderingHints</code> in the <code>RenderContext</code> will
     * contain this information under the
     * <code>KEY_NUM_RETRIES</code> key. If the
     * <code>RenderingHints</code> in the <code>RenderContext</code> already
     * contains a number of retries value specified by the user, that will
     * take preference over the one stored in this class.
     *
     * @param numRetries The number of times an operation should be retried
     *                   in case of a network error.
     * @throws IllegalArgumentException if numRetries is negative.
     */
    public void setNumRetries(int numRetries) {

	if (numRetries < 0)
	    throw new IllegalArgumentException(JaiI18N.getString("Generic4"));

	RenderingHints rh = nodeSupport.getRenderingHints();
	if (rh == null) {
	    RenderingHints hints = new RenderingHints(null);
	    nodeSupport.setRenderingHints(hints);
	}

	nodeSupport.getRenderingHints().put(JAI.KEY_NUM_RETRIES,
					    new Integer(numRetries));
    }

    /**
     * Returns the current negotiation preferences or null, if none were
     * set previously.
     */
    public NegotiableCapabilitySet getNegotiationPreferences() {

	RenderingHints rh = nodeSupport.getRenderingHints();

	NegotiableCapabilitySet ncs =
	    rh == null ? null : (NegotiableCapabilitySet)rh.get(
					     JAI.KEY_NEGOTIATION_PREFERENCES);
	return ncs;
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
     * multiple times. Every time this method is called, the new preferences
     * specified will be stored, a negotiation with these new preferences
     * will be initiated and the results stored. These new preferences which
     * have been stored will be passed as <code>RenderingHints</code> as part
     * of the <code>RenderContext</code> used to create the rendering. The
     * <code>RenderingHints</code> in the <code>RenderContext</code> will
     * contain this information under the
     * <code>KEY_NEGOTIATION_PREFERENCES</code> key. If the
     * <code>RenderingHints</code> in the <code>RenderContext</code> already
     * contains negotiation preferences specified by the user, the user
     * specified negotiation preferences will take preference over the ones
     * stored in this class.
     *
     * <p> If preferences to be set are null, the negotiation will become
     * a two-way negotiation between the client and server capabilities.
     *
     * @param preferences The preferences to be used in the negotiation
     * process.
     */
    public void setNegotiationPreferences(NegotiableCapabilitySet preferences)
    {

	RenderingHints rh = nodeSupport.getRenderingHints();

	// If there are preferences to set
	if (preferences != null) {

	    // Check whether RenderingHints exists, if not, create it.
	    if (rh == null) {
		RenderingHints hints = new RenderingHints(null);
		nodeSupport.setRenderingHints(hints);
	    }

	    // Set the provided preferences into the RenderingHints
	    nodeSupport.getRenderingHints().put(
					      JAI.KEY_NEGOTIATION_PREFERENCES,
					      preferences);
	} else {                   // Preferences is null
	    // Remove any previous values set for negotiation preferences
	    if (rh != null) {
		rh.remove(JAI.KEY_NEGOTIATION_PREFERENCES);
	    }
	}

	negotiated = negotiate(preferences);
    }

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
	    throw new RuntimeException(formatter.format(msgArg0));
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
//	    throw new RemoteImagingException(JaiI18N.getString("RemoteJAI18")
//					     + "\n" + rieSave.getMessage());
	}

	RemoteRIF rrif = (RemoteRIF)registry.getFactory("remoteRenderable",
							protocolName);

	return RemoteJAI.negotiate(prefs,
				   serverCap,
				   rrif.getClientCapabilities());
    }

    /**
     * Returns the results of the negotiation between the client and server
     * capabilities according to the preferences set via the
     * <code>setNegotiationPreferences()</code> method. This will return null
     * if no negotiation preferences were set, and no negotiation was
     * performed, or if the negotiation failed.
     */
    public NegotiableCapabilitySet getNegotiatedValues()
	throws RemoteImagingException {
	return negotiated;
    }

    /**
     * Returns the results of the negotiation between the client and server
     * capabilities for the given catgory according to the preferences set
     * via the <code>setNegotiationPreferences()</code> method. This will
     * return null if no negotiation preferences were set, and no
     * negotiation was performed, or if the negotiation failed.
     *
     * @param category The category to return negotiated results for.
     */
    public NegotiableCapability getNegotiatedValues(String category)
	throws RemoteImagingException {
	if (negotiated != null)
	    return negotiated.getNegotiatedValue(category);
	return null;
    }

    void sendExceptionToListener(String message, Exception e) {
        ImagingListener listener =
            (ImagingListener)getRenderingHints().get(JAI.KEY_IMAGING_LISTENER);

        listener.errorOccurred(message, e, this, false);
    }
}
