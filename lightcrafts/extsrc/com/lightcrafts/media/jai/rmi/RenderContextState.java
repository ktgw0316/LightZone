/*
 * $RCSfile: RenderContextState.java,v $
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
import com.lightcrafts.mediax.jai.remote.SerializableState;
import com.lightcrafts.mediax.jai.remote.SerializerFactory;

/**
 * This class is a serializable proxy for a RenderContext from which the
 * RenderContext may be reconstituted.
 *
 *
 * @since 1.1
 */
public class RenderContextState extends SerializableStateImpl {
    public static Class[] getSupportedClasses() {
        return new Class[] {RenderContext.class};
    }

    /**
      * Constructs a <code>RenderContextState</code> from a
      * <code>RenderContext</code>.
      *
      * @param c The class of the object to be serialized.
      * @param o The <code>RenderContext</code> to be serialized.
      * @param h The <code>RenderingHints</code> (ignored).
      */
    public RenderContextState(Class c, Object o, RenderingHints h) {
        super(c, o, h);
    }
    
    /**
     * Serialize the <code>RenderContextState</code>.
     *
     * @param out The <code>ObjectOutputStream</code>.
     */
    private void writeObject(ObjectOutputStream out) throws IOException {

        RenderContext renderContext = (RenderContext)theObject;

        // Extract the affine transform.
        AffineTransform usr2dev = renderContext.getTransform();

        // Extract the hints.
	RenderingHints hints = renderContext.getRenderingHints();

	// Extract the AOI
        Shape aoi = renderContext.getAreaOfInterest();

        // Write serialized form to the stream.
        out.writeObject(usr2dev);
	out.writeObject(SerializerFactory.getState(aoi));
        out.writeObject(SerializerFactory.getState(hints, null));
    }

    /**
      * Deserialize the <code>RenderContextState</code>.
      *
      * @param out The <code>ObjectInputStream</code>.
      */
    private void readObject(ObjectInputStream in)
        throws IOException, ClassNotFoundException {

        RenderContext renderContext = null;

        // Read serialized form from the stream.
        AffineTransform usr2dev = (AffineTransform)in.readObject();
	
	SerializableState aoi = (SerializableState)in.readObject();
	Shape shape = (Shape)aoi.getObject();

	SerializableState rhs = (SerializableState)in.readObject();
        RenderingHints hints = (RenderingHints)rhs.getObject();

        // Restore the transient RenderContext.
	renderContext = new RenderContext(usr2dev, shape, hints);
        theObject = renderContext;
    }
}
