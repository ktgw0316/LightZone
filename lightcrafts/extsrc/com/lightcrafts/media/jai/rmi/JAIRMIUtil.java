/*
 * $RCSfile: JAIRMIUtil.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:51 $
 * $State: Exp $
 */package com.lightcrafts.media.jai.rmi;

import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.io.Serializable;
import java.util.Vector;
import java.util.Hashtable;
import com.lightcrafts.mediax.jai.RenderedOp;
import com.lightcrafts.mediax.jai.PlanarImage;
import com.lightcrafts.mediax.jai.remote.RemoteRenderedOp;
import com.lightcrafts.mediax.jai.remote.SerializableRenderedImage;

/**
 * A class containing utility methods used for the implementation of
 * the "jairmi" protocol.
 */
public final class JAIRMIUtil {
 
    /**
     * Replaces any element in the source Vector which is an ID
     * with the server-side node that is associated with the
     * given ID.
     */
    public static Vector replaceIdWithSources(Vector srcs,
					      Hashtable nodes,
					      String opName,
					      RenderingHints hints) {
	
	Vector replacedSrcs = new Vector();
	Object obj;
	for (int i=0; i<srcs.size(); i++) {
	    obj = srcs.elementAt(i);
	    if (obj instanceof String){
		String serverNodeDesc = (String)obj;
		int index = serverNodeDesc.indexOf("::");
		boolean diffServer = index != -1;
		if (diffServer){
		    // If the source is on a different server, create
		    // a RMIServerProxy to access it.
		    replacedSrcs.add(new RMIServerProxy(serverNodeDesc,
							opName,
							hints));
		} else {
		    // If the source is on the same server, get it
		    // from the nodes Hashtable and set it as a source
		    // in the sources Vector.
		    replacedSrcs.add(nodes.get(Long.valueOf(serverNodeDesc)));
		}
	    } else {
		PlanarImage pi = 
		    PlanarImage.wrapRenderedImage((RenderedImage)obj);
		replacedSrcs.add(pi);
	    }
	}
	
	return replacedSrcs;
    }
    
    /**
     * Replaces any source that is an <code>RMIServerProxy</code> with
     * the id of the server-side node that is represented by the
     * <code>RMIServerProxy</code>.
     */
    public static  Vector replaceSourcesWithId(Vector srcs,
					       String serverName) {

	Vector replacedSrcs = new Vector();
	Object obj;
	for (int i=0; i<srcs.size(); i++) {
	    obj = srcs.elementAt(i);
	    if (obj instanceof RMIServerProxy) {
		RMIServerProxy rmisp = (RMIServerProxy)obj;
		if (rmisp.getServerName().equalsIgnoreCase(serverName)){
		    replacedSrcs.add(rmisp.getRMIID().toString());
		} else {
		    String str = 
			new String(rmisp.getServerName()+
				   "::"+rmisp.getRMIID());
		    replacedSrcs.add(str);
		}
	    } else if (obj instanceof RemoteRenderedOp){
		RemoteRenderedOp rrop = (RemoteRenderedOp)obj;
		Object ai = rrop.getRendering();
		if (ai instanceof RMIServerProxy) {
		    RMIServerProxy rmisp = (RMIServerProxy)ai;
		    if (rmisp.getServerName().equalsIgnoreCase(serverName)){
			replacedSrcs.add(rmisp.getRMIID().toString());
		    } else {
			String str = 
			    new String(rmisp.getServerName()+
				       "::"+rmisp.getRMIID());
			replacedSrcs.add(str);
		    }
		} else {
		    RenderedImage ri = (RenderedImage)ai;
		    replacedSrcs.add(new SerializableRenderedImage(ri));
		}
	    } else if (obj instanceof RenderedOp){
		RenderedOp rop = (RenderedOp)obj;
		replacedSrcs.add(new SerializableRenderedImage(
					  (RenderedImage)rop.getRendering()));
	    } else if (obj instanceof Serializable) {
		replacedSrcs.add(obj);
	    } else if (obj instanceof RenderedImage) {
		RenderedImage ri = (RenderedImage)obj;
		replacedSrcs.add(new SerializableRenderedImage(ri));
	    }
	}

	return replacedSrcs;					      
    }

    /**
     * Replace an image with an equivalent representation that can be accessed
     * on the server.
     */
    public static Object replaceImage(RenderedImage obj, 
				      String thisServerName) {

	if (obj instanceof RMIServerProxy) {

	    RMIServerProxy rmisp = (RMIServerProxy)obj;
	    if (rmisp.getServerName().equalsIgnoreCase(thisServerName))
		return "::" + rmisp.getRMIID();
	    else
		return rmisp.getServerName() + "::" + rmisp.getRMIID() + 
		    ";;" + rmisp.getOperationName();

	} else if (obj instanceof RenderedOp) {
	    
	    RenderedImage rendering = ((RenderedOp)obj).getRendering();
	    return replaceImage(rendering, thisServerName);

	} else if (obj instanceof RenderedImage) {

	    if (obj instanceof Serializable)
		return obj;
	    else 
		return new SerializableRenderedImage(obj);
	}

	return obj;
    }

    public static void checkClientParameters(ParameterBlock pb, 
					     String thisServerName) {

	// XXX 07/18/01, aastha TODO
	// This code should check every parameter to test Serializability
	// If parameter is Serializable, then keep it as is
	// If not serializable then use SerializerFactory to serialize it
	// If that doesn't work, throw an Exception
	// Note that parameters that are images may still need to be treated
	// differently as in the code below.

	if (pb == null)
	    return;

	int numParams = pb.getNumParameters();
	Vector params = pb.getParameters();

	Object obj;
	for (int i=0; i<numParams; i++) {
	    obj = params.elementAt(i);
	    
	    if (obj == null) {

		continue;

	    } else if (obj instanceof RenderedImage) {

		pb.set(replaceImage((RenderedImage)obj, thisServerName), i);

	    }
	}
    }

    public static void checkClientParameters(Vector parameters, 
					     String thisServerName) {

	// XXX 07/18/01, aastha TODO
	// This code should check every parameter to test Serializability
	// If parameter is Serializable, then keep it as is
	// If not serializable then use SerializerFactory to serialize it
	// If that doesn't work, throw an Exception
	// Note that parameters that are images may still need to be treated
	// differently as in the code below.

	if (parameters == null)
	    return;

	Object obj;
	for (int i=0; i<parameters.size(); i++) {
	    obj = parameters.elementAt(i);
	    
	    if (obj == null) {

		continue;

	    } else if (obj instanceof RenderedImage) {

		parameters.set(i, replaceImage((RenderedImage)obj, 
					       thisServerName));

	    }
	}
    }

    /**
     * Method to convert a String representation of an image into the
     * image itself. The String representation is supplied by the client
     * generally as a parameter in the ParameterBlock.
     */
    public static Object replaceStringWithImage(String s, Hashtable nodes) {
	String paramServerName, opName;
	int index1 = s.indexOf("::");
	int index2 = s.indexOf(";;");
	Long id;

	if (index1 == -1) {
	    return s;
	} else if (index2 == -1) {
	    id = Long.valueOf(s.substring(index1 + 2));
	    return nodes.get(id);

	} else {
	    // Extract the RMI ID from the servername string and 
	    // replace the original serverName string with one of
	    // the usual type.
	    id = Long.valueOf(s.substring(index1+2, index2));
	    paramServerName = s.substring(0, index1);
	    opName = s.substring(index2+2);

	    // Create an RMIServerProxy to access the image on
	    // the other server.
	    return new RMIServerProxy((paramServerName + "::" + id), 
				      opName, 
				      null);
	}
    }

    /**
     * Method to check whether any of the parameters in the ParameterBlock
     * are replacement representations of images.
     */
    public static void checkServerParameters(ParameterBlock pb, 
					     Hashtable nodes) {

	if (pb == null)
	    return;

	int numParams = pb.getNumParameters();
	Vector params = pb.getParameters();

	Object obj;
	for (int i=0; i<numParams; i++) {
	    obj = params.elementAt(i);

	    if (obj == null) {
		continue;
	    } else if (obj instanceof String) {
		pb.set(replaceStringWithImage((String)obj, nodes), i);
	    }
	}
    }

    /**
     * Method to check whether any of the parameters in the ParameterBlock
     * are replacement representations of images.
     */
    public static void checkServerParameters(Vector parameters, 
					     Hashtable nodes) {

	if (parameters == null)
	    return;

	Object obj;
	for (int i=0; i<parameters.size(); i++) {
	    obj = parameters.elementAt(i);

	    if (obj == null) {
		continue;
	    } else if (obj instanceof String) {
		parameters.set(i, replaceStringWithImage((String)obj, nodes));
	    }
	}
    }

}
