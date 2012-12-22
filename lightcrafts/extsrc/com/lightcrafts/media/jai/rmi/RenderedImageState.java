/*
 * $RCSfile: RenderedImageState.java,v $
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
import java.awt.image.RenderedImage;
import java.awt.image.WritableRenderedImage;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import com.lightcrafts.mediax.jai.JAI;
import com.lightcrafts.mediax.jai.OperationRegistry;
import com.lightcrafts.mediax.jai.TiledImage;
import com.lightcrafts.mediax.jai.remote.SerializableRenderedImage;
import com.lightcrafts.mediax.jai.tilecodec.TileCodecParameterList;

/**
 * A <code>SerializableState</code> wrapper for a <code>RenderedImage</code>
 * or <code>WritableRenderedImage</code>.  This class simply uses a
 * <code>SerializableRenderedImage</code> to do the work.
 *
 * @since 1.1
 */
public final class RenderedImageState extends SerializableStateImpl {
    private boolean isWritable;

    private transient boolean useDeepCopy;
    private transient OperationRegistry registry;
    private transient String formatName;
    private transient TileCodecParameterList encodingParam;
    private transient TileCodecParameterList decodingParam;

    public static Class[] getSupportedClasses() {
        return new Class[] {
            RenderedImage.class,
            WritableRenderedImage.class};
    }

    public RenderedImageState(Class c, Object o, RenderingHints h) {
        super(c, o, h);

        isWritable = o instanceof WritableRenderedImage;

        if(h != null) {
            Object value = h.get(JAI.KEY_SERIALIZE_DEEP_COPY);
            if(value != null) {
                useDeepCopy = ((Boolean)value).booleanValue();
            } else {
		useDeepCopy = false;
	    }

            value = h.get(JAI.KEY_OPERATION_REGISTRY);
            if(value != null) {
                registry = (OperationRegistry)value;
            }

            value = h.get(JAI.KEY_TILE_CODEC_FORMAT);
            if(value != null) {
                formatName = (String)value;
            }

            value = h.get(JAI.KEY_TILE_ENCODING_PARAM);
            if(value != null) {
                encodingParam = (TileCodecParameterList)value;
            }

            value = h.get(JAI.KEY_TILE_DECODING_PARAM);
            if(value != null) {
                decodingParam = (TileCodecParameterList)value;
            }
        }
    }

    private void writeObject(ObjectOutputStream out) throws IOException {

        out.defaultWriteObject();

	SerializableRenderedImage sri;

	if (formatName == null || 
	    encodingParam == null || 
	    decodingParam == null) {
	    sri = new SerializableRenderedImage((RenderedImage)theObject,
						useDeepCopy);
	} else {
	    sri =
            new SerializableRenderedImage((RenderedImage)theObject,
                                          useDeepCopy,
                                          registry,
                                          formatName,
                                          encodingParam,
                                          decodingParam);
	}
	
        out.writeObject(sri);
    }

    private void readObject(ObjectInputStream in)
        throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        theObject = in.readObject();
        if(isWritable) {
            theObject = new TiledImage((RenderedImage)theObject, true);
        }
    }
}
