/*
 * $RCSfile: TileDecoderRegistryMode.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:49 $
 * $State: Exp $
 */package com.lightcrafts.mediax.jai.registry;

import java.lang.reflect.Method;
import com.lightcrafts.mediax.jai.JAI;
import com.lightcrafts.mediax.jai.RegistryMode;
import com.lightcrafts.mediax.jai.tilecodec.TileCodecDescriptor;
import com.lightcrafts.mediax.jai.tilecodec.TileCodecParameterList;
import com.lightcrafts.mediax.jai.tilecodec.TileDecoderFactory;
import com.lightcrafts.mediax.jai.util.ImagingListener;

/**
 * A class which provides information about the "tileDecoder" registry
 * mode.
 *
 * @since JAI 1.1
 */
public class TileDecoderRegistryMode extends RegistryMode {

    public static final String MODE_NAME = "tileDecoder";

    // Method to return the factory method for the "tileDecoder" mode.
    // The Method used to "create" objects from this factory.
    private static Method factoryMethod = null;

    private static Method getThisFactoryMethod() {

	if (factoryMethod != null)
	    return factoryMethod;

	// The factory Class that this registry mode represents.
	Class factoryClass = TileDecoderFactory.class;

	try {
	    Class[] paramTypes = new Class[] {java.io.InputStream.class,
					      TileCodecParameterList.class};

	    factoryMethod = factoryClass.getMethod("createDecoder", paramTypes);

	} catch (NoSuchMethodException e) {
            ImagingListener listener =
                JAI.getDefaultInstance().getImagingListener();
            String message = JaiI18N.getString("RegistryMode0") + " " +
                             factoryClass.getName() + ".";
            listener.errorOccurred(message, e,
                                   TileDecoderRegistryMode.class, false);
//	    e.printStackTrace();
	}

	return factoryMethod;
    }

    /**
     * Creates a <code>TileDecoderRegistryMode</code> for describing
     * the "tileDecoder" registry mode.
     */
    public TileDecoderRegistryMode() {
	super(MODE_NAME,
	      TileCodecDescriptor.class,
	      getThisFactoryMethod().getReturnType(),
	      getThisFactoryMethod(),    // factoryMethod
	      true,                      // arePreferencesSupported
	      false);                    // arePropertiesSupported,
    }
}
