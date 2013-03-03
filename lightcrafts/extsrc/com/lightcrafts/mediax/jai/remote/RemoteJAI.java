/*
 * $RCSfile: RemoteJAI.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.2 $
 * $Date: 2005/05/12 18:24:34 $
 * $State: Exp $
 */package com.lightcrafts.mediax.jai.remote;

import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderableImage;
import java.text.MessageFormat;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;
import com.lightcrafts.mediax.jai.JAI;
import com.lightcrafts.mediax.jai.OperationRegistry;
import com.lightcrafts.mediax.jai.OperationDescriptor;
import com.lightcrafts.mediax.jai.PlanarImage;
import com.lightcrafts.mediax.jai.TileCache;
import com.lightcrafts.mediax.jai.util.CaselessStringKey;
import com.lightcrafts.mediax.jai.util.ImagingException;
import com.lightcrafts.mediax.jai.util.ImagingListener;

/**
 * A convenience class for instantiating operations on remote machines.
 *
 * This class also provides information related to the server and allows
 * for setting of parameters for the remote communication with the server.
 *
 * <p>Conceptually this class is very similar to the <code>JAI</code>
 * class, except that the <code>RemoteJAI</code> class deals with
 * remote operations. This class allows programmers to use the syntax:
 *
 * <pre>
 * import com.lightcrafts.mediax.jai.remote.RemoteJAI;
 * RemoteJAI rc = new RemoteJAI(protocolName, serverName);
 * RemoteRenderedOp im = rc.create("convolve", paramBlock, renderHints);
 * </pre>
 *
 * to create new images by applying operators that are executed remotely on
 * the specified server. The <code>create()</code> method returns a
 * <code>RemoteRenderedOp</code> encapsulating the protocol name, server
 * name, operation name, parameter block, and rendering hints. Additionally,
 * it performs validity checking on the operation parameters. The operation
 * parameters are determined from the <code>OperationDescriptor</code>
 * retrieved using the <code>getServerSupportedOperationList()</code> method.
 * Programmers may also refer to
 * RemoteJAI.createRenderable("opname", paramBlock, renderHints);
 *
 * <p> If the <code>OperationDescriptor</code> associated with the
 * named operation returns <code>true</code> from its
 * <code>isImmediate()</code> method, the <code>create()</code>
 * method will ask the <code>RemoteRenderedOp</code> it constructs to render
 * itself immediately.  If this rendering is <code>null</code>,
 * <code>create()</code> will itself return <code>null</code>
 * rather than returning an instance of <code>RemoteRenderedOp</code>
 * as it normally does.
 *
 * <p> The registry being used by this class may be
 * inspected or set using the <code>getOperationRegistry()</code> and
 * <code>setOperationRegistry()</code> methods.  Only experienced
 * users should attempt to set the registry. This registry is used to
 * map protocol names into either a <code>RemoteRIF</code> or a
 * <code>RemoteCRIF</code>.
 *
 * <p> The <code>TileCache</code> associated with an instance may be
 * similarly accessed.
 *
 * <p> Each instance of <code>RemoteJAI</code> contains a set of
 * default rendering hints which will be used for all image creations.
 * These hints are merged with any hints supplied to the
 * <code>create</code> method; directly supplied hints take precedence
 * over the common hints. When a new <code>RemoteJAI</code> instance is
 * constructed, its hints are initialized to a copy of the default
 * hints. Thus when an instance of <code>RemoteJAI</code> is
 * constructed, hints for the default registry, tile cache, number of
 * retries, and the retry interval are added to the set of common
 * rendering hints.  Similarly, invoking <code>setOperationRegistry()</code>,
 * <code>setTileCache()</code>, <code>setNumRetries()</code> or
 * <code>setRetryInterval()</code> on a <code>RemoteJAI</code> instance
 * will cause the respective entity to be added to the common rendering
 * hints. The hints associated with any instance may be manipulated
 * using the <code>getRenderingHints()</code>,
 * <code>setRenderingHints()</code>, <and
 * <code>clearRenderingHints()</code> methods.
 *
 * <p> The <code>TileCache</code> to be used by a particular operation
 * may be set during construction, or by calling
 * the <code>setTileCache()</code> method.  This will result in the
 * provided tile cache being added to the set of common rendering
 * hints.
 *
 * <p> Network errors are dealt with through the use of retry intervals and
 * retries. Retries refers to the maximum number of times a remote operation
 * will be retried. The retry interval refers to the amount of time (in
 * milliseconds) between two consecutive retries. If errors are encountered
 * at each retry and the number of specified retries has been exhausted, a
 * <code>RemoteImagingException</code> will be thrown. By default, the
 * number of retries is set to five, and the retry interval
 * is set to a thousand milliseconds. These values can be changed by using
 * the <code>setNumRetries()</code> and the <code>setRetryInterval</code>
 * methods and can also be specified via the <code>RenderingHints</code>
 * object passed as an argument to <code>RemoteJAI.create()</code>. Time
 * outs (When the amount of time taken to get a response or
 * the result of an operation from the remote machine exceeds a limit) are
 * not dealt with, and must be taken care of by the network imaging
 * protocol implementation itself. The implementation must be responsible
 * for monitoring time outs, but on encountering one can deal with it by
 * throwing a <code>RemoteImagingException</code>, which will then be dealt
 * with using retries and retry intervals.
 *
 * <p> This class provides the capability of negotiating capabilities
 * between the client and the server. The <code>negotiate</code>
 * method uses the preferences specified via the
 * <code>setNegotiationPreferences</code> method alongwith the server
 * and client capabilities retrieved via the <code>getServerCapabilities</code>
 * and <code>getClientCapabilities</code> respectively to negotiate on each
 * of the preferences. This negotiation treats the client and server
 * capabilities as being non-preferences, and the user set
 * <code>NegotiableCapabilitySet</code> as being a preference. The
 * negotiation is performed according to the rules described in the class
 * documentation for <code>NegotiableCapability</code>.
 *
 * <p> Note that negotiation preferences can be set either prior to
 * specifying a particular rendered or renderable operation (by using
 * <code>RemoteJAI.create()</code> or
 * <code>RemoteJAI.createRenderable()</code>) or afterwards. The currently
 * set negotiation preferences are passed to the <code>RemoteRenderedOp</code>
 * on its construction through the <code>RenderingHints</code> using the
 * <code>KEY_NEGOTIATION_PREFERENCES</code> key. Since
 * <code>RemoteRenderableOp</code> does not accept a
 * <code>RenderingHints</code> object as a construction argument, the newly
 * created <code>RemoteRenderableOp</code> is informed of these preferences
 * using it's <code>setRenderingHints()</code> method. These preferences
 * can be changed after the construction using the
 * <code>setNegotiationPreferences()</code> method on both
 * <code>RemoteRenderedOp</code> and <code>RemoteRenderableOp</code>.
 *
 * The same behavior applies to the number of retries and the retry interval,
 * whether they be the default values contained in the default
 * <code>RenderingHints</code> or whether they are set using the
 * <code>setNumRetries</code> or <code>setRetryInterval</code> methods, the
 * existing values are passed to <code>RemoteRenderedOp</code>'s when they
 * are created through the <code>RenderingHints</code> argument, and are set
 * on the newly created <code>RemoteRenderableOp</code> using the
 * <code>setNumRetries</code> or <code>setRetryInterval</code> methods on
 * <code>RemoteRenderableOp</code>.
 *
 * @see JAI
 * @see JAIRMIDescriptor
 * @see RemoteImagingException
 *
 * @since JAI 1.1
 */
public class RemoteJAI {

    /** The String representing the remote server machine. */
    protected String serverName;

    /** The name of the protocol used for client-server communication. */
    protected String protocolName;

    /** The OperationRegistry instance used for instantiating operations. */
    private OperationRegistry operationRegistry =
        JAI.getDefaultInstance().getOperationRegistry();

    /** The amount of time to wait between retries (in Millseconds). */
    public static final int DEFAULT_RETRY_INTERVAL = 1000;

    /** The default number of retries. */
    public static final int DEFAULT_NUM_RETRIES = 5;

    /**
     * Time in milliseconds between retries, initialized to default value.
     */
    private int retryInterval = DEFAULT_RETRY_INTERVAL; // Milliseconds

    /** The number of retries, initialized to default value. */
    private int numRetries = DEFAULT_NUM_RETRIES;

    /** A reference to a centralized TileCache object. */
    private transient TileCache cache =
        JAI.getDefaultInstance().getTileCache();

    /**
     * The RenderingHints object used to retrieve the TileCache,
     * OperationRegistry hints.
     */
    private RenderingHints renderingHints;

    /**
     * The set of preferences to be used for the communication between
     * the client and the server.
     */
    private NegotiableCapabilitySet preferences = null;

    /**
     * The set of properties agreed upon after the negotiation process
     * between the client and the server has been completed.
     */
    private static NegotiableCapabilitySet negotiated;

    /** The client and server capabilities. */
    private NegotiableCapabilitySet serverCapabilities = null;
    private NegotiableCapabilitySet clientCapabilities = null;

    /**
     * A Hashtable containing OperationDescriptors hashed by their
     * operation names.
     */
    private Hashtable odHash = null;

    /** The array of descriptors supported by the server. */
    private OperationDescriptor descriptors[] = null;

    /** Required to I18N compound messages. */
    private static MessageFormat formatter;

    /**
     * Constructs a <code>RemoteJAI</code> instance with the given
     * protocol name and server name. The semantics of the serverName
     * are defined by the particular protocol used to create this
     * class. Instructions on how to create a serverName that is
     * compatible with this protocol can be retrieved from the
     * <code>getServerNameDocs()</code> method on the
     * <code>RemoteDescriptor</code> associated with the given
     * protocolName. An <code>IllegalArgumentException</code> may
     * be thrown by the protocol specific classes at a later point, if
     * null is provided as the serverName argument and null is not
     * considered a valid serverName by the specified protocol.
     *
     * @param protocolName The <code>String</code> that identifies the
     *                     remote imaging protocol.
     * @param serverName   The <code>String</code> that identifies the server.
     *
     * @throws IllegalArgumentException if protocolName is null.
     */
    public RemoteJAI(String protocolName, String serverName) {
	this(protocolName, serverName, null, null);
    }

    /**
     * Constructs a <code>RemoteJAI</code> instance with the given
     * protocol name, server name, <code>OperationRegistry</code>
     * and <code>TileCache</code>. If the specified
     * <code>OperationRegistry</code> is null, the registry associated
     * with the default <code>JAI</code> instance will be used. If the
     * specified <code>TileCache</code> is null, the <code>TileCache</code>
     * associated with the default <code>JAI</code> instance will be used.
     *
     * <p> An <code>IllegalArgumentException</code> may
     * be thrown by the protocol specific classes at a later point, if
     * null is provided as the serverName argument and null is not
     * considered a valid serverName by the specified protocol.
     *
     * @param serverName        The <code>String</code> that identifies
     *                          the server.
     * @param protocolName      The <code>String</code> that identifies
     *                          the remote imaging protocol.
     * @param operationRegistry The <code>OperationRegistry</code> associated
     *                          with this class, if null, default will be used.
     * @param tileCache         The <code>TileCache</code> associated with
     *                          this class, if null, default will be used.
     * @throws IllegalArgumentException if protocolName is null.
     */
    public RemoteJAI(String protocolName,
		     String serverName,
		     OperationRegistry registry,
		     TileCache tileCache) {

	if (protocolName == null) {
	    throw new IllegalArgumentException(JaiI18N.getString("Generic1"));
	}

	// For formatting error strings.
	formatter = new MessageFormat("");
	formatter.setLocale(Locale.getDefault());

	this.protocolName = protocolName;
	this.serverName = serverName;

	// operationRegistry and cache variables are already initialized
	// via static initializers, so change them only if the user has
	// provided a non-null value for them.
	if (registry != null) {
	    this.operationRegistry = registry;
	}

	if (tileCache != null) {
	    this.cache = tileCache;
	}

	this.renderingHints = new RenderingHints(null);
        this.renderingHints.put(JAI.KEY_OPERATION_REGISTRY, operationRegistry);
	this.renderingHints.put(JAI.KEY_TILE_CACHE, cache);
	this.renderingHints.put(JAI.KEY_RETRY_INTERVAL,
				new Integer(retryInterval));
        this.renderingHints.put(JAI.KEY_NUM_RETRIES, new Integer(numRetries));
    }

    /**
     * Returns a <code>String</code> identifying the remote server machine.
     */
    public String getServerName() {
	return serverName;
    }

    /**
     * Returns the protocol name.
     */
    public String getProtocolName() {
	return protocolName;
    }

    /**
     * Sets the amount of time between retries in milliseconds. The
     * specified <code>retryInterval</code> parameter will be added
     * to the common <code>RenderingHints</code> of this
     * <code>RemoteJAI</code> instance, under the
     * <code>JAI.KEY_RETRY_INTERVAL</code> key.
     *
     * @param retryInterval The time interval between retries (milliseconds).
     * @throws IllegalArgumentException if retryInterval is negative.
     */
    public void setRetryInterval(int retryInterval) {

	if (retryInterval < 0) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic3"));
        }

	this.retryInterval = retryInterval;
	renderingHints.put(JAI.KEY_RETRY_INTERVAL, new Integer(retryInterval));
    }

    /**
     * Returns the amount of time between retries in milliseconds.
     */
    public int getRetryInterval() {
	return retryInterval;
    }

    /**
     * Sets the number of retries. The specified <code>numRetries</code>
     * parameter will be added to the common <code>RenderingHints</code>
     * of this <code>RemoteJAI</code> instance, under the
     * <code>JAI.KEY_NUM_RETRIES</code> key.
     *
     * @param numRetries The number of retries.
     * @throws IllegalArgumentException if numRetries is negative.
     */
    public void setNumRetries(int numRetries) {
        if (numRetries < 0) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic4"));
        }

	this.numRetries = numRetries;
	renderingHints.put(JAI.KEY_NUM_RETRIES, new Integer(numRetries));
    }

    /**
     * Returns the number of retries.
     */
    public int getNumRetries() {
	return numRetries;
    }

    /**
     * Returns the <code>OperationRegistry</code> being used by this
     * <code>RemoteJAI</code> instance.
     */
    public OperationRegistry getOperationRegistry() {
        return operationRegistry;
    }

    /**
     * Sets the<code>OperationRegistry</code> to be used by this
     * <code>RemoteJAI</code> instance. The <code>operationRegistry</code>
     * parameter will be added to the <code>RenderingHints</code> of this
     * <code>RemoteJAI</code> instance.
     *
     * @throws IllegalArgumentException if operationRegistry is null.
     */
    public void setOperationRegistry(OperationRegistry operationRegistry) {
        if (operationRegistry == null) {
            throw new IllegalArgumentException(
					     JaiI18N.getString("RemoteJAI4"));
        }
        this.operationRegistry = operationRegistry;
        this.renderingHints.put(JAI.KEY_OPERATION_REGISTRY, operationRegistry);
    }

    /**
     * Sets the <code>TileCache</code> to be used by this
     * <code>RemoteJAI</code>. The <code>tileCache</code> parameter
     * will be added to the <code>RenderingHints</code> of this
     * <code>RemoteJAI</code> instance.
     *
     * @throws IllegalArgumentException if tileCache is null.
     */
    public void setTileCache(TileCache tileCache) {
        if (tileCache == null) {
            throw new IllegalArgumentException(
					     JaiI18N.getString("RemoteJAI5"));
        }
        this.cache = tileCache;
        renderingHints.put(JAI.KEY_TILE_CACHE, cache);
    }

    /**
     * Returns the <code>TileCache</code> being used by this
     * <code>RemoteJAI</code> instance.
     */
    public TileCache getTileCache() {
	return cache;
    }

    /**
     * Returns the <code>RenderingHints</code> associated with this
     * <code>RemoteJAI</code> instance.  These rendering hints will be
     * merged with any hints supplied as an argument to the
     * <code>create()</code> method.
     */
    public RenderingHints getRenderingHints() {
        return renderingHints;
    }

    /**
     * Sets the <code>RenderingHints</code> associated with this
     * <code>RemoteJAI</code> instance.  These rendering hints will be
     * merged with any hints supplied as an argument to the
     * <code>create()</code> method.
     *
     * @throws IllegalArgumentException if hints is null.
     */
    public void setRenderingHints(RenderingHints hints) {
        if (hints == null) {
	    throw new IllegalArgumentException(
					     JaiI18N.getString("RemoteJAI6"));
        }
        this.renderingHints = hints;
    }

    /**
     * Clears the <code>RenderingHints</code> associated with this
     * <code>RemoteJAI</code> instance.
     */
    public void clearRenderingHints() {
        this.renderingHints = new RenderingHints(null);
    }

    /**
     * Returns the hint value associated with a given key
     * in this <code>RemoteJAI</code> instance, or <code>null</code>
     * if no value is associated with the given key.
     *
     * @throws IllegalArgumentException if key is null.
     */
    public Object getRenderingHint(RenderingHints.Key key) {
        if (key == null) {
            throw new IllegalArgumentException(
					     JaiI18N.getString("RemoteJAI7"));
        }
        return renderingHints.get(key);
    }

    /**
     * Sets the hint value associated with a given key
     * in this <code>RemoteJAI</code> instance.
     *
     * @throws IllegalArgumentException if <code>key</code> is
     *         <code>null</code>.
     * @throws IllegalArgumentException if <code>value</code> is
     *         <code>null</code>.
     * @throws IllegalArgumentException if <code>value</code> is
     *         not of the correct type for the given hint.
     */
    public void setRenderingHint(RenderingHints.Key key, Object value) {
        if (key == null) {
            throw new IllegalArgumentException(
					     JaiI18N.getString("RemoteJAI7"));
        }
        if (value == null) {
            throw new IllegalArgumentException(
					     JaiI18N.getString("RemoteJAI8"));
        }

        try {
            renderingHints.put(key, value);
        } catch (Exception e) {
            throw new IllegalArgumentException(e.toString());
        }
    }

    /**
     * Removes the hint value associated with a given key
     * in this <code>RemoteJAI</code> instance.
     */
    public void removeRenderingHint(RenderingHints.Key key) {
        renderingHints.remove(key);
    }

    /**
     * Creates a <code>RemoteRenderedOp</code> which represents the named
     * operation to be performed remotely, using the source(s) and/or
     * parameter(s) specified in the <code>ParameterBlock</code>, and
     * applying the specified hints to the destination. This method
     * should only be used when the final result returned is a single
     * <code>RemoteRenderedImage</code>.
     *
     * <p> The supplied operation name is validated against the
     * names of the <code>OperationDescriptor</code>s returned from
     * the <code>getServerSupportedOperationList()</code> method. The
     * source(s) and/or parameter(s) in the <code>ParameterBlock</code>
     * are validated against the named operation's descriptor, both in
     * their numbers and types. Additional restrictions placed on the
     * sources and parameters by an individual operation are also
     * validated by calling its
     * <code>OperationDescriptor.validateArguments()</code> method.
     *
     * <p>Parameters are allowed to have a <code>null</code> input
     * value, if that particular parameter has a default value specified
     * in its operation's descriptor.  In this case, the default value
     * will replace the <code>null</code> input.
     *
     * <p>Unspecified tailing parameters are allowed, if these
     * parameters have default values specified in the operation's
     * descriptor. However, if a parameter, which has a default value,
     * is followed by one or more parameters that
     * have no default values, this parameter must be specified in the
     * <code>ParameterBlock</code>, even if it only has a value of
     * code>null</code>.
     *
     * <p> The rendering hints associated with this instance of
     * <code>RemoteJAI</code> are overlaid with the hints passed to this
     * method.  That is, the set of keys will be the union of the
     * keys from the instance's hints and the hints parameter.
     * If the same key exists in both places, the value from the
     * hints parameter will be used.
     *
     * @param opName The name of the operation.
     * @param args   The source(s) and/or parameter(s) for the operation.
     * @param hints  The hints for the operation.
     *
     * @throws IllegalArgumentException if <code>opName</code> is
     * <code>null</code>.
     * @throws IllegalArgumentException if <code>args</code> is
     * <code>null</code>.
     * @throws IllegalArgumentException if no
     *         <code>OperationDescriptor</code> is available from the server
     *         with the specified operation name.
     * @throws IllegalArgumentException if the
     *         <code>OperationDescriptor</code> for the specified
     *         operation name on the server does not
     *         support the "rendered" registry mode.
     * @throws IllegalArgumentException if the specified operation does
     *         not produce a
     *         <code>java.awt.image.RenderedImage</code>.
     * @throws IllegalArgumentException if the specified operation is
     *         unable to handle the sources and parameters specified in
     *         <code>args</code>.
     *
     * @return  A <code>RemoteRenderedOp</code> that represents the named
     *          operation to be performed remotely, or <code>null</code>
     *          if the specified operation
     *          is in the "immediate" mode and the rendering of the
     *          <code>PlanarImage</code> failed.
     */
    public RemoteRenderedOp create(String opName,
				   ParameterBlock args,
				   RenderingHints hints) {

	if (opName == null) {
	    throw new IllegalArgumentException(
					     JaiI18N.getString("RemoteJAI9"));
	}

	if (args == null) {
	    throw new IllegalArgumentException(
					     JaiI18N.getString("RemoteJAI10"));
	}

	// Initialize the odHash hashtable
	getServerSupportedOperationList();

	// Get the OperationDescriptor associated with this name.
	OperationDescriptor odesc =
	    (OperationDescriptor)odHash.get(new CaselessStringKey(opName));

	if (odesc == null) {
	    throw new IllegalArgumentException(
					    JaiI18N.getString("RemoteJAI11"));
	}

        // Does this operation support rendered mode?
        if (!odesc.isModeSupported("rendered")) {
            throw new IllegalArgumentException(
					    JaiI18N.getString("RemoteJAI12"));
        }

	// Does the operation produce a RenderedImage?
	if (!RenderedImage.class.isAssignableFrom(
				odesc.getDestClass("rendered"))) {
	    throw new IllegalArgumentException(
					    JaiI18N.getString("RemoteJAI13"));
        }

	// Validate input arguments. The ParameterBlock is cloned here
	// because OperationDescriptor.validateArguments() may change
	// its content.
        StringBuffer msg = new StringBuffer();
        args = (ParameterBlock)args.clone();
        if (!odesc.validateArguments("rendered", args, msg)) {
            throw new IllegalArgumentException(msg.toString());
        }

	// Merge rendering hints.  Hints passed in take precedence.
        RenderingHints mergedHints;
        if (hints == null) {
            mergedHints = renderingHints;
        } else if (renderingHints.isEmpty()) {
            mergedHints = hints;
        } else {
            mergedHints = new RenderingHints((Map)renderingHints);
            mergedHints.add(hints);
        }

	RemoteRenderedOp op = new RemoteRenderedOp(operationRegistry,
						   protocolName,
						   serverName,
						   opName,
						   args,
						   mergedHints);

	// If the operation requests immediate rendering, do so.
        if (odesc.isImmediate()) {
            PlanarImage im = null;
            im = op.getRendering();

            if (im == null) {
		// Op could not be rendered, return null.
                return null;
            }
        }

        // Return the RemoteRenderedOp associated with this operation.
        return op;
    }

    /**
     * Creates a <code>RemoteRenderableOp</code> that represents the named
     * operation to be performed remotely, using the source(s) and/or
     * parameter(s) specified in the <code>ParameterBlock</code>.
     * This method should only be used when the final result returned
     * is a single <code>RenderableImage</code>.
     *
     * <p> The supplied operation name is validated against the names
     * of the <code>OperationDescriptor</code>s returned from
     * the <code>getServerSupportedOperationList()</code> method.
     * The source(s) and/or parameter(s) in the
     * <code>ParameterBlock</code> are validated against the named
     * operation's descriptor, both in their numbers and types.
     * Additional restrictions placed on the sources and parameters
     * by an individual operation are also validated by calling its
     * <code>OperationDescriptor.validateRenderableArguments()</code>
     * method.
     *
     * <p>Parameters are allowed to have a <code>null</code> input
     * value, if that particular parameter has a default value specified
     * in its operation's descriptor.  In this case, the default value
     * will replace the <code>null</code> input.
     *
     * <p>Unspecified tailing parameters are allowed, if these
     * parameters have default values specified in the operation's
     * descriptor. However, if a parameter, which
     * has a default value, is followed by one or more parameters that
     * have no default values, this parameter must be specified in the
     * <code>ParameterBlock</code>, even if it only has a value of
     * code>null</code>.
     *
     * @param opName  The name of the operation.
     * @param args    The source(s) and/or parameter(s) for the operation.
     *
     * @throws IllegalArgumentException if <code>opName</code> is
     * <code>null</code>.
     * @throws IllegalArgumentException if <code>args</code> is
     * <code>null</code>.
     * @throws IllegalArgumentException if no
     *         <code>OperationDescriptor</code> is available from the server
     *         with the specified operation name.
     * @throws IllegalArgumentException if the
     *         <code>OperationDescriptor</code> for the specified
     *         operation name on the server does not
     *         support "renderable" registry mode.
     * @throws IllegalArgumentException if the specified operation does
     *         not produce a
     *         <code>java.awt.image.renderable.RenderableImage</code>.
     * @throws IllegalArgumentException if the specified operation is
     *         unable to handle the sources and parameters specified in
     *         <code>args</code>.
     *
     * @return  A <code>RemoteRenderableOp</code> that represents the named
     *          operation to be performed remotely.
     */
    public RemoteRenderableOp createRenderable(String opName,
					       ParameterBlock args) {

	if (opName == null) {
	    throw new IllegalArgumentException(
					     JaiI18N.getString("RemoteJAI9"));
	}

	if (args == null) {
	    throw new IllegalArgumentException(
					     JaiI18N.getString("RemoteJAI10"));
	}

	// Initialize the odHash hashtable
	getServerSupportedOperationList();

	// Get the OperationDescriptor associated with this name.
	OperationDescriptor odesc =
	    (OperationDescriptor)odHash.get(new CaselessStringKey(opName));

	if (odesc == null) {
	    throw new IllegalArgumentException(
					    JaiI18N.getString("RemoteJAI11"));
	}

        // Does this operation support rendered mode?
        if (!odesc.isModeSupported("renderable")) {
            throw new IllegalArgumentException(
					    JaiI18N.getString("RemoteJAI14"));
        }

	// Does the operation produce a RenderedImage?
	if (!RenderableImage.class.isAssignableFrom(
				    odesc.getDestClass("renderable"))) {
	    throw new IllegalArgumentException(
					    JaiI18N.getString("RemoteJAI15"));
        }

	// Validate input arguments. The ParameterBlock is cloned here
	// because OperationDescriptor.validateRenderableArguments()
	// may change its content.
        StringBuffer msg = new StringBuffer();
        args = (ParameterBlock)args.clone();
        if (!odesc.validateArguments("renderable", args, msg)) {
            throw new IllegalArgumentException(msg.toString());
        }

 	RemoteRenderableOp op = new RemoteRenderableOp(operationRegistry,
						       protocolName,
						       serverName,
						       opName,
						       args);
	// Set the node-scope hints
	op.setRenderingHints(renderingHints);

	// Return the RemoteRenderableOp.
        return op;
    }

    //
    // NEGOTIATION RELATED METHODS
    //

    /**
     * Sets the preferences to be used in the client-server
     * communication. These preferences are utilized in the negotiation
     * process. Note that preferences for more than one category can be
     * specified using this method since <code>NegotiableCapabilitySet</code>
     * allows different <code>NegotiableCapability</code> objects to be
     * bundled up in one <code>NegotiableCapabilitySet</code> class. Even
     * under the same category (as specified by the getCategory() method
     * on <code>NegotiableCapability</code>), multiple
     * <code>NegotiableCapability</code> objects can be added to the
     * preferences. The preference added first for a particular category is
     * given highest priority in the negotiation process.
     *
     * <p> Since a new set of preferences is set everytime this method is
     * called, this method allows for changing negotiation preferences
     * multiple times. However it should be noted that preferences set on
     * this method are relevant only prior to the creation of an
     * operation (using the <code>RemoteJAI.create</code> method). To
     * change negotiation preferences on an operation after it has been
     * created, the <code>setNegotiationPreferences()</code> method on the
     * created <code>RemoteRenderedOp</code> should be used. The
     * <code>preferences</code> parameter will be added to the
     * <code>RenderingHints</code> of this <code>RemoteJAI</code> instance.
     */
    public void setNegotiationPreferences(NegotiableCapabilitySet preferences) {

	this.preferences = preferences;

	if (preferences == null)
	    renderingHints.remove(JAI.KEY_NEGOTIATION_PREFERENCES);
	else
	    renderingHints.put(JAI.KEY_NEGOTIATION_PREFERENCES, preferences);

	// Every time new preferences are set, invalidate old Negotiation
	// results and do the negotiation again.
	negotiated = null;
	getNegotiatedValues();
    }

    /**
     * Returns the results of the negotiation between the client and server
     * capabilities according to the user preferences specified at an
     * earlier time. This will return null if the negotiation failed.
     *
     * <p> If a negotiation cycle has not been initiated prior to calling
     * this method, or the negotiation preferences have been
     * changed, this method will initiate a new negotiation cycle, which will
     * create and return a new set of negotiated values.
     *
     * @returns A <code>NegotiableCapabilitySet</code> that is the
     * result of the negotiation process, if negotiation is successful,
     * otherwise returns null.
     */
    public NegotiableCapabilitySet getNegotiatedValues()
	throws RemoteImagingException {

	// If negotiation was not performed before, or if new preferences
	// have invalidated the old negotiated results.
	if (negotiated == null) {

	    if (serverCapabilities == null) {
                serverCapabilities = getServerCapabilities();
	    }

	    if (clientCapabilities == null) {
		clientCapabilities = getClientCapabilities();
	    }

	    // Do the negotiation
	    negotiated = negotiate(preferences,
				   serverCapabilities,
				   clientCapabilities);
	}

	return negotiated;
    }

    /**
     * Returns the results of the negotiation between the client and server
     * capabilities according to the user preferences specified at an
     * earlier time for the given category. This method returns a
     * <code>NegotiableCapability</code> object, that represents the result
     * of the negotiation for the given category. If the negotiation failed,
     * null will be returned.
     *
     * <p> If a negotiation cycle has not been initiated prior to calling
     * this method, or the negotiation preferences have been
     * changed, this method will initiate a new negotiation cycle, which will
     * create and return a new negotiated value for the given category.
     *
     * @param category The category to negotiate on.
     * @throws IllegalArgumentException if category is null.
     * @returns A <code>NegotiableCapabilitySet</code> that is the
     * result of the negotiation process, if negotiation is successful,
     * otherwise returns null.
     */
    public NegotiableCapability getNegotiatedValues(String category)
	throws RemoteImagingException {

	// We do not need to check for category being null, since that
	// check will be made by the methods called from within this method.

	// If negotiation was not performed before, or if new preferences
	// have invalidated the old negotiated results.
	if (negotiated == null) {

	    if (serverCapabilities == null) {
                serverCapabilities = getServerCapabilities();
	    }

	    if (clientCapabilities == null) {
		clientCapabilities = getClientCapabilities();
	    }

	    // Do the negotiation
	    return negotiate(preferences,
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

    /**
     * This method negotiates the capabilities to be used in the remote
     * communication. Upon completion of the negotiation process,
     * this method returns a <code>NegotiableCapabilitySet</code> which
     * contains an aggregation of the <code>NegotiableCapability</code>
     * objects that represent the results of negotiation. If the negotiation
     * fails, null will be returned.
     *
     * <p> The negotiation process treats the serverCapabilities and the
     * clientCapabilities as non-preferences and will throw an
     * <code>IllegalArgumentException</code> if the
     * <code>isPreference</code> method for either of these returns
     * true. The preferences <code>NegotiableCapabilitySet</code> should
     * return true from its <code>isPreference</code> method, otherwise an
     * <code>IllegalArgumentException</code> will be thrown. The negotiation
     * is done in accordance with the rules described in the class comments
     * for <code>NegotiableCapability</code>.
     *
     * <p> If either the serverCapabilities or the clientCapabilities
     * is null, then the negotiation will fail, and null will be returned.
     * If preferences is null, the negotiation will become a two-way
     * negotiation between the two non-null
     * <code>NegotiableCapabilitySet</code>s.
     *
     * @param preferences        The user preferences for the negotiation.
     * @param serverCapabilities The capabilities of the server.
     * @param clientCapabilities The capabilities of the client.
     *
     * @throws IllegalArgumentException if serverCapabilities is a
     * preference, i.e., if it's <code>isPreference()</code> method
     * returns true.
     * @throws IllegalArgumentException if clientCapabilities is a
     * preference, i.e., if it's <code>isPreference()</code> method
     * returns true.
     * @throws IllegalArgumentException if preferences is a
     * non-preference, i.e., if it's <code>isPreference()</code> method
     * returns false.
     */
    public static NegotiableCapabilitySet negotiate(
				NegotiableCapabilitySet preferences,
				NegotiableCapabilitySet serverCapabilities,
				NegotiableCapabilitySet clientCapabilities) {

	if (serverCapabilities == null || clientCapabilities == null)
	    return null;

	if (serverCapabilities != null &&
	    serverCapabilities.isPreference() == true)
	    throw new IllegalArgumentException(
					    JaiI18N.getString("RemoteJAI20"));

	if (clientCapabilities != null &&
	    clientCapabilities.isPreference() == true)
	    throw new IllegalArgumentException(
					    JaiI18N.getString("RemoteJAI21"));

	if (preferences == null) {
	    return serverCapabilities.negotiate(clientCapabilities);
	} else {
	    if (preferences.isPreference() == false)
		throw new IllegalArgumentException(
					    JaiI18N.getString("RemoteJAI19"));

	    NegotiableCapabilitySet clientServerCap =
		serverCapabilities.negotiate(clientCapabilities);
	    if (clientServerCap == null)
		return null;
	    return clientServerCap.negotiate(preferences);
	}
    }

    /**
     * This method negotiates the capabilities to be used in the remote
     * communication for the given category. Upon completion of the
     * negotiation process, this method returns a
     * <code>NegotiableCapability</code> object, that represents the result
     * of the negotiation for the given category. If the negotiation fails,
     * null will be returned.
     *
     * <p> The negotiation process treats the serverCapabilities and the
     * clientCapabilities as non-preferences and will throw an
     * <code>IllegalArgumentException</code> if the
     * <code>isPreference</code> method for either of these returns
     * true. The preferences <code>NegotiableCapabilitySet</code> should
     * return true from its <code>isPreference</code> method or an
     * <code>IllegalArgumentException</code> will be thrown. The negotiation
     * is done in accordance with the rules described in the class comments
     * for <code>NegotiableCapability</code>.
     *
     * <p> If either the serverCapabilities or the clientCapabilities
     * is null, then the negotiation will fail, and null will be returned.
     * If preferences is null, the negotiation will become a two-way
     * negotiation between the two non-null
     * <code>NegotiableCapabilitySet</code>s.
     *
     * @param preferences        The user preferences for the negotiation.
     * @param serverCapabilities The capabilities of the server.
     * @param clientCapabilities The capabilities of the client.
     * @param category           The category to perform the negotiation on.
     *
     * @throws IllegalArgumentException if preferences is a
     * non-preference, i.e., if it's <code>isPreference()</code> method
     * returns false.
     * @throws IllegalArgumentException if serverCapabilities is a
     * preference, i.e., if it's <code>isPreference()</code> method
     * returns true.
     * @throws IllegalArgumentException if clientCapabilities is a
     * preference, i.e., if it's <code>isPreference()</code> method
     * returns true.
     * @throws IllegalArgumentException if category is null.
     */
    public static NegotiableCapability negotiate(
				NegotiableCapabilitySet preferences,
				NegotiableCapabilitySet serverCapabilities,
				NegotiableCapabilitySet clientCapabilities,
				String category) {

	if (serverCapabilities == null || clientCapabilities == null)
	    return null;

	if (serverCapabilities != null &&
	    serverCapabilities.isPreference() == true)
	    throw new IllegalArgumentException(
					    JaiI18N.getString("RemoteJAI20"));

	if (clientCapabilities != null &&
	    clientCapabilities.isPreference() == true)
	    throw new IllegalArgumentException(
					    JaiI18N.getString("RemoteJAI21"));

	if (preferences != null && preferences.isPreference() == false)
	    throw new IllegalArgumentException(
					    JaiI18N.getString("RemoteJAI19"));

	if (category == null)
	    throw new IllegalArgumentException(
					    JaiI18N.getString("RemoteJAI26"));

	if (preferences == null || preferences.isEmpty()) {
	    return serverCapabilities.getNegotiatedValue(clientCapabilities,
							 category);
	} else {

	    List prefList = preferences.get(category);
	    List serverList = serverCapabilities.get(category);
	    List clientList = clientCapabilities.get(category);
	    Iterator p = prefList.iterator();

	    NegotiableCapability server, client, result;

	    NegotiableCapability pref = null;
	    //If there are no preferences for the current category
	    if (p.hasNext() == false)
		pref = null;
	    else
		pref = (NegotiableCapability)p.next();

	    Vector results = new Vector();

	    // Negotiate every server NC with every client NC
	    for (Iterator s = serverList.iterator(); s.hasNext(); ) {
		server = (NegotiableCapability)s.next();
		for (Iterator c = clientList.iterator(); c.hasNext(); ) {
		    client = (NegotiableCapability)c.next();

		    result = server.negotiate(client);
		    if (result == null) {
			// This negotiation failed, continue to the next one
			continue;
		    } else {
			// Negotiation between client and server succeeded,
			// add to results array
			results.add(result);

			if (pref != null) {
			    // Negotiate with the pref, if negotiation is
			    // successful, return the result from this method.
			    result = result.negotiate(pref);
			}

			if (result != null) {
			    return result;
			} // else move onto next negotiation
		    }
		}
	    }

	    for (; p.hasNext(); ) {
		pref = (NegotiableCapability)p.next();
		for (int r=0; r<results.size(); r++) {
		    if ((result = pref.negotiate((NegotiableCapability)
					      results.elementAt(r))) != null) {
			return result;
		    }
		}
	    }

	    // If all negotiations failed, return null.
	    return null;
	}
    }

    /**
     * Returns the set of capabilites supported by the server. If any
     * network related errors are encountered by this method (identified
     * as such by receiving a <code>RemoteImagingException</code>), they
     * will be dealt with by the use of retries and retry intervals.
     */
    public NegotiableCapabilitySet getServerCapabilities() throws RemoteImagingException {

	if (serverCapabilities == null) {

	    // Get the RemoteDescriptor for protocolName
	    RemoteDescriptor descriptor = (RemoteDescriptor)
		operationRegistry.getDescriptor(RemoteDescriptor.class,
						protocolName);

	    if (descriptor == null) {
		Object[] msgArg0 = {new String(protocolName)};
		formatter.applyPattern(JaiI18N.getString("RemoteJAI16"));
		throw new RuntimeException(formatter.format(msgArg0));
	    }
	    Exception rieSave = null;
	    int count=0;
	    while (count++ < numRetries) {
		try {
		    serverCapabilities =
			descriptor.getServerCapabilities(serverName);
		    break;
		} catch (RemoteImagingException rie) {
		    // Print that an Exception occured
		    System.err.println(JaiI18N.getString("RemoteJAI24"));
		    rieSave = rie;
		    // Sleep for retryInterval milliseconds
		    try {
			Thread.sleep(retryInterval);
		    } catch (InterruptedException ie) {
                        sendExceptionToListener(JaiI18N.getString("Generic5"),
                                                new ImagingException(JaiI18N.getString("Generic5"), ie));
//			throw new RuntimeException(ie.toString());
		    }
		}
	    }

	    if (serverCapabilities == null && count > numRetries) {
                sendExceptionToListener(JaiI18N.getString("RemoteJAI18"), rieSave);
//		throw new RemoteImagingException(
//					   JaiI18N.getString("RemoteJAI18")+"\n"+rieSave.getMessage());
	    }
	}

	return serverCapabilities;
    }

    /**
     * Returns the set of capabilities supported by the client.
     */
    public NegotiableCapabilitySet getClientCapabilities() {

	if (clientCapabilities == null) {

	    RemoteRIF rrif =
		(RemoteRIF)operationRegistry.getFactory("remoteRendered",
							protocolName);
	    if (rrif == null) {
		rrif =
		    (RemoteRIF)operationRegistry.getFactory("remoteRenderable",
							    protocolName);
	    }

	    if (rrif == null) {
		Object[] msgArg0 = {new String(protocolName)};
		formatter.applyPattern(JaiI18N.getString("RemoteJAI17"));
		throw new RuntimeException(formatter.format(msgArg0));
	    }

	    clientCapabilities = rrif.getClientCapabilities();
	}

	return clientCapabilities;
    }

    /**
     * Returns the list of <code>OperationDescriptor</code>s that describe
     * the operations supported by the server. If any
     * network related errors are encountered by this method (identified
     * as such by receiving a <code>RemoteImagingException</code>), they
     * will be dealt with by the use of retries and retry intervals.
     */
    public OperationDescriptor[] getServerSupportedOperationList()
	throws RemoteImagingException {

	if (descriptors == null) {

	    // Get the RemoteDescriptor for protocolName
	    RemoteDescriptor descriptor = (RemoteDescriptor)
		operationRegistry.getDescriptor(RemoteDescriptor.class,
						protocolName);

	    if (descriptor == null) {
		Object[] msgArg0 = {new String(protocolName)};
		formatter.applyPattern(JaiI18N.getString("RemoteJAI16"));
		throw new RuntimeException(formatter.format(msgArg0));
	    }
	    Exception rieSave = null;
	    int count = 0;
	    while (count++ < numRetries) {
		try {
		    descriptors =
			descriptor.getServerSupportedOperationList(serverName);
		    break;
		} catch (RemoteImagingException rie) {
		    // Print that an Exception occured
		    System.err.println(JaiI18N.getString("RemoteJAI25"));
      		    rieSave = rie;
		    // Sleep for retryInterval milliseconds
		    try {
			Thread.sleep(retryInterval);
		    } catch (InterruptedException ie) {
//			throw new ImagingException(ie);
                        sendExceptionToListener(JaiI18N.getString("Generic5"),
                                                new ImagingException(JaiI18N.getString("Generic5"), ie));
		    }
		}
	    }

	    if (descriptors == null && count > numRetries) {
                sendExceptionToListener(JaiI18N.getString("RemoteJAI23"), rieSave);
//		throw new RemoteImagingException(
//					   JaiI18N.getString("RemoteJAI23")+"\n"+rieSave.getMessage());
	    }

	    // Store the descriptors into a Hashtable hashed by
	    // their operation name.
	    odHash = new Hashtable();
	    for (int i=0; i<descriptors.length; i++) {
		odHash.put(new CaselessStringKey(descriptors[i].getName()),
			   descriptors[i]);
	    }
	}

	return descriptors;
    }

    void sendExceptionToListener(String message, Exception e) {
        ImagingListener listener = JAI.getDefaultInstance().getImagingListener();
        listener.errorOccurred(message, e, this, false);
    }
}

