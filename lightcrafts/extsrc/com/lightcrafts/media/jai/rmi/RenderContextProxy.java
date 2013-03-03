/*
 * $RCSfile: RenderContextProxy.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:53 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.rmi;

import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.image.renderable.RenderContext;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import com.lightcrafts.mediax.jai.ROIShape;

/**
 * This class is a serializable proxy for a RenderContext from which the
 * RenderContext may be reconstituted.
 *
 *
 * @since EA3
 */
public class RenderContextProxy implements Serializable {
    /** The RenderContext. */
    private transient RenderContext renderContext;

    /**
      * Constructs a <code>DatabufferProxy</code> from a
      * <code>Databuffer</code>.
      *
      * @param source The <code>Databuffer</code> to be serialized.
      */
    public RenderContextProxy(RenderContext source) {
        renderContext = source;
    }

    /**
      * Retrieves the associated <code>Databuffer</code>.
      * @return The (perhaps reconstructed) <code>Databuffer</code>.
      */
    public RenderContext getRenderContext() {
        return renderContext;
    }

    /**
      * Serialize the <code>RenderContextProxy</code>.
      *
      * @param out The <code>ObjectOutputStream</code>.
      */
    private void writeObject(ObjectOutputStream out) throws IOException {
	boolean isNull = renderContext == null;
	out.writeBoolean(isNull);
	if (isNull) 
	    return;

        // Extract the affine transform.
        AffineTransform usr2dev = renderContext.getTransform();

        // Create serializable form of the hints.
        RenderingHintsProxy rhp =
            new RenderingHintsProxy(renderContext.getRenderingHints());

        Shape aoi = renderContext.getAreaOfInterest();

        // Write serialized form to the stream.
        out.writeObject(usr2dev);

        out.writeBoolean(aoi != null);
        if(aoi != null) {
	    if(aoi instanceof Serializable){
		out.writeObject(aoi);
	    }else {
		out.writeObject(new ROIShape(aoi));
	    }
        }

        out.writeObject(rhp);
    }

    /**
      * Deserialize the <code>RenderContextProxy</code>.
      *
      * @param out The <code>ObjectInputStream</code>.
      */
    private void readObject(ObjectInputStream in)
        throws IOException, ClassNotFoundException {

	if (in.readBoolean()) {
	    renderContext = null;
	    return;
	}

        // Read serialized form from the stream.
        AffineTransform usr2dev = (AffineTransform)in.readObject();
	
	Shape shape = null;
	Object aoi = in.readBoolean() ?
            (Object)in.readObject() : null;
	RenderingHintsProxy rhp = (RenderingHintsProxy)in.readObject();
	
	RenderingHints hints = rhp.getRenderingHints();
	
	// Restore the transient RenderContext.
	if (aoi != null){
	    if (aoi instanceof ROIShape){
		shape = ((ROIShape)aoi).getAsShape();
	    }else {
		shape = (Shape)aoi;
	    }
	}
	
	if(aoi == null && hints.isEmpty()) {
	    renderContext = new RenderContext(usr2dev);
	} else if(aoi == null) {
	    renderContext = new RenderContext(usr2dev, hints);
	} else if(hints.isEmpty()) {
	    renderContext = new RenderContext(usr2dev, shape);
	} else {
	    renderContext = new RenderContext(usr2dev, shape, hints);
	}
    }
}
