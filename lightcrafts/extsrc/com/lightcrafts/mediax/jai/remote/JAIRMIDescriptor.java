/*
 * $RCSfile: JAIRMIDescriptor.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:50 $
 * $State: Exp $
 */package com.lightcrafts.mediax.jai.remote;

import java.awt.RenderingHints;
import java.awt.image.renderable.ParameterBlock;
import java.net.URL;
import java.net.InetAddress;
import java.rmi.Naming;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Locale;
import java.util.List;
import java.util.Vector;
import com.lightcrafts.mediax.jai.JAI;
import com.lightcrafts.mediax.jai.OperationDescriptor;
import com.lightcrafts.mediax.jai.OperationNode;
import com.lightcrafts.mediax.jai.RenderedOp;
import com.lightcrafts.mediax.jai.ParameterListDescriptor;
import com.lightcrafts.mediax.jai.util.CaselessStringKey;
import com.lightcrafts.mediax.jai.util.ImagingException;
import com.lightcrafts.mediax.jai.util.ImagingListener;
import com.lightcrafts.media.jai.rmi.ImageServer;
import com.lightcrafts.media.jai.rmi.RMIServerProxy;
import com.lightcrafts.media.jai.rmi.JAIRMIUtil;

/**
 * This class describes the "jairmi" remote imaging protocol. This protocol
 * assumes that both the client and the server are running JAI. The
 * communication between the client and the server takes place using
 * the Remote Method Invocation (RMI) mechanism.
 *
 * <p> In order to locate the "jairmi" server, a RMI registry must be
 * running on this server, and the "jairmi" server must have registered
 * itself with this RMI registry by binding itself under the
 * <code>IMAGE_SERVER_BIND_NAME</code> <code>String</code>. The RMI
 * registry is a simple remote object name service that allows remote
 * clients to get a reference to a remote object by name.
 *
 * <p> The "jairmi" protocol expects the <code>String</code> that
 * represents the server to be a <code>URL</code> formatted
 * <code>String</code> of the form:
 *
 * <pre>
 * //host:port
 * </pre>
 *
 * where <code>host</code> is the name, or IP address of the "jairmi"
 * remote imaging server, and <code>port</code> is the port number
 * where a rmiregistry is running on the same host. A protocol like
 * "rmi:" does not need to be included in this URL formatted
 * <code>String</code>. If the serverName
 * <code>String</code> is null, the local host is used as a default.
 * If the port is not included in the serverName <code>String</code>, it
 * defaults to the well-known port for rmiregistry, 1099.
 *
 * <p> If the serverName supplied to any "jairmi" protocol implementing
 * class's method is null, then the local host will be used instead.
 *
 * <p> The default "jairmi" server provided with JAI is
 * <code>com.lightcrafts.media.jai.rmi.JAIRMIRemoteServer</code>. This server
 * can be run in the following manner, after starting a rmiregistry on
 * the host where the server will be run:
 *
 * <pre>
 * java -Djava.rmi.server.codebase="file:$JAI/lib/jai_core.jar file:$JAI/lib/jai_codec.jar" -Djava.rmi.server.useCodebaseOnly=false -Djava.security.policy=file:$JAI/policy com.lightcrafts.media.jai.rmi.JAIRMIImageServer
 * </pre>
 *
 * where $JAI refers to the directory where JAI is installed. This server
 * binds itself with the running rmiregistry under the
 * <code>IMAGE_SERVER_BIND_NAME</code> <code>String</code> bind name, and
 * can be used to serve "jairmi" requests. The policy file specified
 * above needs to be created by the user. Information on policy
 * files and permissions can be found at
 * <p>http://java.sun.com/j2se/1.3/docs/guide/security/PolicyFiles.html
 * <p>http://java.sun.com/j2se/1.3/docs/guide/security/permissions.html
 *
 * <p> The JAI instance used by the "jairmi" remote imaging server can be
 * configured by providing an implementation of the
 * <code>com.lightcrafts.media.jai.remote.JAIServerConfigurationSpi</code> interface
 * on the <code>CLASSPATH</code> when starting the server.
 * For more details, please refer to
 * {@link com.lightcrafts.media.jai.remote.JAIServerConfigurationSpi}
 *
 * <p> The "jairmi" remote imaging server supports the following
 * configurable parameters whose values can be specified on the command
 * line when starting the server :
 *
 * <code>
 *        -host <string> The server name or server IP address
 *        -port <integer> The port that rmiregistry is running on
 *        -rmiRegistryPort <integer> Same as -port option
 *        -serverPort <integer> The port that the server should listen on,
 *                           for connections from clients
 *        -cacheMemCapacity <long> The memory capacity in bytes.
 *        -cacheMemThreshold <float> The memory threshold, which is the
 *                           fractional amount of cache memory to
 *                           retain during tile removal
 *        -disableDefaultCache Disable use of default tile cache.
 *        -schedulerParallelism <integer> The degree of parallelism of the
 *                           default TileScheduler
 *        -schedulerPrefetchParallelism <integer> The degree of parallelism
 *                           of the default TileScheduler for tile prefetching
 *        -schedulerPriority <integer> The priority of tile scheduling for
 *                           the default TileScheduler
 *        -schedulerPrefetchPriority <integer> The priority of tile prefetch
 *                           scheduling for the default TileScheduler
 *        -defaultTileSize <integer>x<integer> The default tile dimensions in
 *                           the form <xSize>x<ySize>
 *        -defaultRenderingSize <integer>x<integer> The default size to render
 *                           a RenderableImage to, in the form <xSize>x<ySize>
 *        -serializeDeepCopy <boolean> Whether a deep copy of the image data
 *                           should be used when serializing images
 *        -tileCodecFormat <string> The default format to be used for tile
 *                           serialization via TileCodecs
 *        -retryInterval <integer> The retry interval value to be used for
 *                           dealing with network errors during remote imaging
 *        -numRetries <integer> The number of retries to be used for dealing
 *                           with network errors during remote imaging
 * </code>
 *
 * <p> It should be noted that if a parameter
 * was set via JAIServerConfigurationSpi, and the command line option for
 * the same parameter specifies a different value, then the command line
 * specified parameter value will be honored. That is to say that the
 * JAIServerConfigurationSpi specified configuration happens first, followed
 * by command line parameter configuration, and the last configuration to
 * be applied overwrites all previous settings.

 * @since JAI 1.1
 */
public class JAIRMIDescriptor extends RemoteDescriptorImpl {

    /**
     * The bind name for the remote "jairmi" server. This is also the
     * name that the "jairmi" client looks for when trying to locate
     * a "jairmi" server.
     */
    public static final String IMAGE_SERVER_BIND_NAME =
                                              "JAIRMIRemoteServer1.1";

    // A MessageFormat object to format the error strings.
    private MessageFormat formatter;

    /**
     * Creates a <code>JAIRMIDescriptor</code>.
     */
    public JAIRMIDescriptor() throws java.net.MalformedURLException {
	super("jairmi",
	      new URL("http://java.sun.com/products/java-media/jai/forDevelopers/jai-apidocs/javax/media/jai/remote/JAIRMIDescriptor.html"));

	formatter = new MessageFormat("");
	formatter.setLocale(Locale.getDefault());
    }

    /**
     * Returns the list of <code>OperationDescriptor</code>s that describe
     * the operations supported by the server. It is the
     * implementing class's responsibility to extract this information from
     * either the server or from its own knowledge of the remote imaging
     * protocol. The "jairmi" protocol gets this information from the server.
     *
     * <p> If the supplied serverName argument is null, then the local
     * host will be used instead.
     *
     * @param serverName The <code>String</code> identifying the server.
     */
    public OperationDescriptor[] getServerSupportedOperationList(String
								 serverName)
	throws RemoteImagingException {

	List odList = null;
	try {
	      odList = getImageServer(serverName).getOperationDescriptors();
	} catch (Exception e) {
            sendExceptionToListener(JaiI18N.getString("JAIRMIDescriptor12"),
                                    new RemoteImagingException(JaiI18N.getString("JAIRMIDescriptor12"), e));
//	    throw new RemoteImagingException(ImageUtil.getStackTraceString(e));
	}

	OperationDescriptor[] od = new OperationDescriptor[odList.size()];
	int count=0;
	for (Iterator i = odList.iterator(); i.hasNext(); ) {
	    od[count++] = (OperationDescriptor)i.next();
	}

	return od;
    }

    private ImageServer getImageServer(String serverName) {

 	if (serverName == null) {
	    try {
		serverName = InetAddress.getLocalHost().getHostAddress();
	    } catch(Exception e) {
                sendExceptionToListener(JaiI18N.getString("JAIRMIDescriptor13"),
                                        new ImagingException(JaiI18N.getString("JAIRMIDescriptor13"), e));
//		throw new RuntimeException(e.getMessage());
	    }
	}

	// Derive the service name.
	String serviceName = new String("rmi://"+serverName+"/"+
					IMAGE_SERVER_BIND_NAME);

	ImageServer imageServer = null;
	// Look up the remote object.
	try {
	    imageServer = (ImageServer)Naming.lookup(serviceName);
	} catch (Exception e) {
            sendExceptionToListener(JaiI18N.getString("JAIRMIDescriptor14"),
                                    new RemoteImagingException(JaiI18N.getString("JAIRMIDescriptor14"), e));
//	    throw new RemoteImagingException(ImageUtil.getStackTraceString(e));
	}

	return imageServer;
    }

    /**
     * Returns the set of capabilites supported by the server. It is the
     * implementing class's responsibility to extract this information from
     * either the server or from its own knowledge of the remote imaging
     * protocol. The "jairmi" protocol gets this information from the server.
     *
     * <p> If the supplied serverName argument is null, then the local
     * host will be used instead.
     *
     * @param serverName The <code>String</code> identifying the server.
     */
    public NegotiableCapabilitySet getServerCapabilities(String serverName)
	throws RemoteImagingException {

	NegotiableCapabilitySet serverCapabilities = null;
	try {
	    serverCapabilities =
		getImageServer(serverName).getServerCapabilities();
	} catch (Exception e) {
            sendExceptionToListener(JaiI18N.getString("JAIRMIDescriptor15"),
                                    new RemoteImagingException(JaiI18N.getString("JAIRMIDescriptor15"), e));
//	    throw new RemoteImagingException(ImageUtil.getStackTraceString(e));
	}

	return serverCapabilities;
    }

    /**
     * Calculates the region over which two distinct remote renderings
     * of an operation may be expected to differ. The operation is
     * represented by the <code>OperationNode</code> argument to this
     * method. The <code>String</code> that identifies the operation
     * can be retrieved via the <code>OperationNode</code>'s
     * <code>getOperationName()</code> method.
     *
     * <p> The class of the returned object will vary as a function of
     * the nature of the operation.  For rendered and renderable two-
     * dimensional images this should be an instance of a class which
     * implements <code>java.awt.Shape</code>.
     *
     * @param registryModeName The name of the mode.
     * @param oldServerName The previous server name.
     * @param oldParamBlock The previous sources and parameters.
     * @param oldHints The previous hints.
     * @param newServerName The current server name.
     * @param newParamBlock The current sources and parameters.
     * @param newHints The current hints.
     * @param node The affected node in the processing chain.
     *
     * @return The region over which the data of two renderings of this
     *         operation may be expected to be invalid or <code>null</code>
     *         if there is no common region of validity. If an empty
     *         <code>java.awt.Shape</code> is returned, this indicates
     *         that all pixels within the bounds of the old rendering
     *         remain valid.
     *
     * @throws IllegalArgumentException if <code>registryModeName</code>
     *         is <code>null</code> or if the operation requires either
     *         sources or parameters and either <code>oldParamBlock</code>
     *         or <code>newParamBlock</code> is <code>null</code>.
     * @throws IllegalArgumentException if there is no OperationDescriptor
     *         for the specified operationName on any one or both of the
     *         servers identified by <code>oldServerName</code> and
     *         <code>newServerName</code>, or if the number of sources or
     *         the name, number and <code>Class</code> of the operation's
     *         parameters is not the same on both the servers.
     * @throws IllegalArgumentException if <code>oldParamBlock</code> or
     *         <code>newParamBlock</code> do not contain sufficient sources
     *         or parameters for the operation in question.
     */
    public Object getInvalidRegion(String registryModeName,
				   String oldServerName,
				   ParameterBlock oldParamBlock,
				   RenderingHints oldHints,
				   String newServerName,
				   ParameterBlock newParamBlock,
				   RenderingHints newHints,
				   OperationNode node)
	throws RemoteImagingException {

	if (registryModeName == null)
	    throw new IllegalArgumentException(
				     JaiI18N.getString("JAIRMIDescriptor11"));

	String operationName = node.getOperationName();
	OperationDescriptor oldDescs[] =
	    getServerSupportedOperationList(oldServerName);
	OperationDescriptor oldOD =
	    getOperationDescriptor(oldDescs, operationName);

	if (oldOD == null)
	    throw new IllegalArgumentException(
				       JaiI18N.getString("JAIRMIDescriptor1"));

	int numSources = oldOD.getNumSources();

	// If the supplied registryModeName is "remoteRendered" or
	// "remoteRenderable", in order to get the OperationDescriptor's
	// ParameterListDescriptor, we need to actually use the "rendered"
	// or "renderable" mode respectively.
	ParameterListDescriptor oldPLD = null;
	if (registryModeName.equalsIgnoreCase("remoteRendered")) {
	    oldPLD = oldOD.getParameterListDescriptor("rendered");
	} else if (registryModeName.equalsIgnoreCase("remoteRenderable")) {
	    oldPLD = oldOD.getParameterListDescriptor("renderable");
	} else {
	    oldPLD = oldOD.getParameterListDescriptor(registryModeName);
	}

	int numParams = oldPLD.getNumParameters();

	// If the serverNames are same, nothing to be done for that
	if (oldServerName != newServerName) {

	    // Check whether they both support the supplied operation name

	    OperationDescriptor newDescs[] =
		getServerSupportedOperationList(newServerName);
	    OperationDescriptor newOD;

	    if ((newOD = getOperationDescriptor(newDescs,
						operationName)) == null)
		throw new IllegalArgumentException(
				       JaiI18N.getString("JAIRMIDescriptor2"));

	    // Check the OperationDescriptor equivalence

	    // Sources
	    if (numSources != newOD.getNumSources())
		throw new IllegalArgumentException(
				       JaiI18N.getString("JAIRMIDescriptor3"));

	    // Parameters
	    ParameterListDescriptor newPLD =
		newOD.getParameterListDescriptor(registryModeName);

	    if (numParams != newPLD.getNumParameters())
		throw new IllegalArgumentException(
				      JaiI18N.getString("JAIRMIDescriptor4"));

	    // Param names
	    String oldParamNames[] = oldPLD.getParamNames();
	    if (oldParamNames == null)
		oldParamNames = new String[0];
	    String newParamNames[] = newPLD.getParamNames();
	    if (newParamNames == null)
		newParamNames = new String[0];

	    Hashtable oldHash = hashNames(oldParamNames);
	    Hashtable newHash = hashNames(newParamNames);

	    // The same names should be present in both in the same order.
            if (containsAll(oldHash, newHash) == false)
		throw new IllegalArgumentException(
				       JaiI18N.getString("JAIRMIDescriptor8"));

	    // Param class types
            Class thisParamClasses[] = oldPLD.getParamClasses();
            Class otherParamClasses[] = newPLD.getParamClasses();
            for (int i=0; i<oldParamNames.length; i++) {
                if (thisParamClasses[i] !=
                    otherParamClasses[getIndex(newHash, oldParamNames[i])])
                    throw new IllegalArgumentException(
				       JaiI18N.getString("JAIRMIDescriptor9"));
            }

	    // XXX Could be made more efficient by returning the area that
	    // might be valid if both the servers support the same operations,
	    // current implementation just returns null.
	    return null;
	}

	// Perform the other checks listed in the method spec

	// Neither the old and the new ParamBlock should be null, if
	// the operation requires some sources or some parameters.
	if ((registryModeName == null) ||
            ((numSources > 0 || numParams > 0) &&
             (oldParamBlock == null || newParamBlock == null))) {
            throw new IllegalArgumentException(
				      JaiI18N.getString("JAIRMIDescriptor5"));
        }

	// Both the old and new ParameterBlock should contain the
	// required number of sources.
        if ((numSources > 0) &&
            (oldParamBlock.getNumSources() != numSources ||
             newParamBlock.getNumSources() != numSources)) {
	    Object[] msgArg0 = {
		operationName,
		new Integer(numParams)
	    };
	    formatter.applyPattern(JaiI18N.getString("JAIRMIDescriptor6"));
            throw new IllegalArgumentException(formatter.format(msgArg0));
        }

	// Both the old and new ParameterBlock should contain the
	// required number of parameters.
        if ((numParams > 0) &&
            (oldParamBlock.getNumParameters() != numParams ||
             newParamBlock.getNumParameters() != numParams)) {
	    Object[] msgArg0 = {
		operationName,
		new Integer(numParams)
	    };
	    formatter.applyPattern(JaiI18N.getString("JAIRMIDescriptor7"));
            throw new IllegalArgumentException(formatter.format(msgArg0));
        }

	// Find the id that refers to the corresponding RenderedOp on the
	// server
	RenderedOp op = (RenderedOp)node;
	Object rendering = op.getRendering();
	Long id = null;
	if (rendering instanceof RMIServerProxy) {
	    id = ((RMIServerProxy)rendering).getRMIID();
	} else {
	    throw new RuntimeException(
				     JaiI18N.getString("JAIRMIDescriptor10"));
	}

	// Check whether any of the sources of this operation are on
	// remote "jairmi" servers and if so, replace the source with
	// it's id.

	boolean samePBs = false;
	if (oldParamBlock == newParamBlock)
	    samePBs = true;

	Vector oldSources = oldParamBlock.getSources();
	oldParamBlock.removeSources();
	// Ensure that any images which are parameters are replaced byte
	// suitable representations
	JAIRMIUtil.checkClientParameters(oldParamBlock, oldServerName);
	oldParamBlock.setSources(
		  JAIRMIUtil.replaceSourcesWithId(oldSources, oldServerName));

	if (samePBs) {
	    newParamBlock = oldParamBlock;
	} else {
	    Vector newSources = newParamBlock.getSources();
	    newParamBlock.removeSources();
	    // Ensure that any images which are parameters are replaced byte
	    // suitable representations
	    JAIRMIUtil.checkClientParameters(newParamBlock, oldServerName);
	    newParamBlock.setSources(
		  JAIRMIUtil.replaceSourcesWithId(newSources, oldServerName));

	}

	// Serialize the old and new RenderingHints
	SerializableState oldRHS = SerializerFactory.getState(oldHints, null);
	SerializableState newRHS = SerializerFactory.getState(newHints, null);

	SerializableState shapeState = null;
	try {
	    shapeState =
		getImageServer(oldServerName).getInvalidRegion(id,
							       oldParamBlock,
							       oldRHS,
							       newParamBlock,
							       newRHS);
	} catch (Exception e) {
            sendExceptionToListener(JaiI18N.getString("JAIRMIDescriptor16"),
                                    new RemoteImagingException(JaiI18N.getString("JAIRMIDescriptor16"), e));
//	    throw new RemoteImagingException(ImageUtil.getStackTraceString(e));
	}

	return shapeState.getObject();
    }

    private Hashtable hashNames(String paramNames[]) {

        Hashtable h = new Hashtable();
        if (paramNames != null) {
            for (int i=0; i<paramNames.length; i++) {
                h.put(new CaselessStringKey(paramNames[i]), new Integer(i));
            }
        }

        return h;
    }

    private int getIndex(Hashtable h, String s) {
        return ((Integer)h.get(new CaselessStringKey(s))).intValue();
    }

    // A case insensitive containsAll for Hashtables containing Strings
    private boolean containsAll(Hashtable thisHash, Hashtable otherHash) {

        CaselessStringKey thisNameKey;
        for (Enumeration i=thisHash.keys(); i.hasMoreElements(); ) {
            thisNameKey = (CaselessStringKey)i.nextElement();
            if (otherHash.containsKey(thisNameKey) == false)
                return false;
        }

        return true;
    }

    private OperationDescriptor getOperationDescriptor(OperationDescriptor
						       descriptors[],
						       String operationName) {

	OperationDescriptor od;
	for (int i = 0; i < descriptors.length; i++) {
	    od = descriptors[i];
	    if (od.getName().equalsIgnoreCase(operationName))
		return od;
	}

	return null;
    }

    void sendExceptionToListener(String message, Exception e) {
        ImagingListener listener= JAI.getDefaultInstance().getImagingListener();
        listener.errorOccurred(message, e, this, false);
    }
}
