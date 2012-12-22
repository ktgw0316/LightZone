/*
 * $RCSfile: TileCodecUtils.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:58 $
 * $State: Exp $
 */package com.lightcrafts.media.jai.tilecodec;

import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.text.MessageFormat;
import com.lightcrafts.mediax.jai.JAI;
import com.lightcrafts.mediax.jai.tilecodec.TileCodecDescriptor;
import com.lightcrafts.mediax.jai.remote.SerializableState;
import com.lightcrafts.mediax.jai.remote.SerializerFactory;

/**
 * A class containing methods of utility to all TileCodec implementations.
 */
public class TileCodecUtils {
    /* Required to I18N compound messages. */
    private static MessageFormat formatter = new MessageFormat("");

    /**
     * Get the <code>TileCodecDescriptor</code> associated with the
     * specified registry mode.
     */
    public static TileCodecDescriptor getTileCodecDescriptor(String registryMode,
							     String formatName) {
        return (TileCodecDescriptor)
            JAI.getDefaultInstance().getOperationRegistry()
                .getDescriptor(registryMode, formatName);
    }

    /** Deserialize a <code>Raster</code> from its serialized version */
    public static Raster deserializeRaster(Object object) {
        if (!(object instanceof SerializableState))
            return null;

	SerializableState ss = (SerializableState)object;
	Class c = ss.getObjectClass();
	if (Raster.class.isAssignableFrom(c)) {
	    return (Raster)ss.getObject();
	}
	return null;
    }

    /** Deserialize a <code>SampleModel</code> from its serialized version */
    public static SampleModel deserializeSampleModel(Object object) {
	if (!(object instanceof SerializableState))
	    return null;

	SerializableState ss = (SerializableState)object;
        Class c = ss.getObjectClass();
        if (SampleModel.class.isAssignableFrom(c)) {
            return (SampleModel)ss.getObject();
        }
        return null;
    }

    /** Serialize a <code>Raster</code>. */
    public static Object serializeRaster(Raster ras) {
        return SerializerFactory.getState(ras, null);
    }

    /** Serialize a <code>SampleModel</code>. */
    public static Object serializeSampleModel(SampleModel sm) {
	return SerializerFactory.getState(sm, null);
    }
}
