/*
 * $RCSfile: RasterProxy.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:52 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.rmi;

import java.awt.Point;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import com.lightcrafts.mediax.jai.remote.SerializableState;
import com.lightcrafts.mediax.jai.remote.SerializerFactory;

/**
 * This class is a serializable proxy for a Raster from which the
 * Raster may be reconstituted.
 *
 *
 * @since EA3
 */
public class RasterProxy implements Serializable {
    /** The Raster. */
    private transient Raster raster;

    /**
      * Constructs a <code>RasterProxy</code> from a
      * <code>Raster</code>.
      *
      * @param source The <code>Raster</code> to be serialized.
      */
    public RasterProxy(Raster source) {
        raster = source;
    }

    /**
      * Retrieves the associated <code>Raster</code>.
      * @return The (perhaps reconstructed) <code>Raster</code>.
      */
    public Raster getRaster() {
        return raster;
    }

    /**
      * Serialize the <code>RasterProxy</code>.
      *
      * @param out The <code>ObjectOutputStream</code>.
      */
    private void writeObject(ObjectOutputStream out) throws IOException {
        Raster r;

        if (raster.getParent() != null) {
            // Use the child ratser to create another Raster
            // containing data for the requested bounds but which does
            // not share the SampleModel and DataBuffer of the parent.
            // Fix : 4631478
            r = raster.createCompatibleWritableRaster(raster.getBounds());
            ((WritableRaster)r).setRect(raster);
        } else {
            r = raster;
        }

        out.writeInt(r.getWidth());
        out.writeInt(r.getHeight());
        out.writeObject(SerializerFactory.getState(r.getSampleModel(), null));
        out.writeObject(SerializerFactory.getState(r.getDataBuffer(), null));
        out.writeObject(new Point(r.getMinX(), r.getMinY()));
    }

    /**
      * Deserialize the <code>RasterProxy</code>.
      *
      * @param out The <code>ObjectInputStream</code>.
      */
    private void readObject(ObjectInputStream in)
        throws IOException, ClassNotFoundException {
        int width;
        int height;
        SerializableState sampleModelState = null;
        SerializableState dataBufferState = null;
        Point location = null;
        
        width = in.readInt();
        height = in.readInt();
        sampleModelState = (SerializableState)in.readObject();
        dataBufferState = (SerializableState)in.readObject();
        location = (Point)in.readObject();

        // Restore the SampleModel from its serialized form.
        SampleModel sampleModel = (SampleModel)sampleModelState.getObject();
        if(sampleModel == null) {
            raster = null;
            return;
        }

        // Restore the DataBuffer from its serialized form.
        DataBuffer dataBuffer = (DataBuffer)dataBufferState.getObject();

        // Reconstruct the Raster.
        raster = Raster.createRaster(sampleModel, dataBuffer, location);
    }
}
