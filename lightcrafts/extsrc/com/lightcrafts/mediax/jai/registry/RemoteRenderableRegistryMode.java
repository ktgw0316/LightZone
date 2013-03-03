/*
 * $RCSfile: RemoteRenderableRegistryMode.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:48 $
 * $State: Exp $
 */package com.lightcrafts.mediax.jai.registry;

import java.lang.reflect.Method;
import com.lightcrafts.mediax.jai.JAI;
import com.lightcrafts.mediax.jai.RegistryMode;
import com.lightcrafts.mediax.jai.util.ImagingListener;
import com.lightcrafts.mediax.jai.remote.RemoteDescriptor;

/**
 * A class which provides information about the "remoteRenderable" registry
 * mode.
 *
 * @since JAI 1.1
 */
public class RemoteRenderableRegistryMode extends RegistryMode {

    public static final String MODE_NAME = "remoteRenderable";

    // The Method used to "create" objects from this factory.
    private static Method factoryMethod = null;

    private static Method getThisFactoryMethod() {

	if (factoryMethod != null)
	    return factoryMethod;

	// The factory Class that this registry mode represents.
	Class factoryClass =
		    com.lightcrafts.mediax.jai.remote.RemoteCRIF.class;

	try {
	    Class[] paramTypes = new Class[]
		    {java.lang.String.class,
		     java.lang.String.class,
		     java.awt.image.renderable.RenderContext.class,
		     java.awt.image.renderable.ParameterBlock.class};

	    factoryMethod = factoryClass.getMethod("create", paramTypes);

	} catch (NoSuchMethodException e) {
            ImagingListener listener =
                JAI.getDefaultInstance().getImagingListener();
            String message = JaiI18N.getString("RegistryMode0") + " " +
                             factoryClass.getName() + ".";
            listener.errorOccurred(message, e,
                                   RemoteRenderableRegistryMode.class, false);
//	    e.printStackTrace();
	}

	return factoryMethod;
    }

    /**
     * Creates a <code>RemoteRenderableRegistryMode</code> for describing
     * the "remoteRenderable" registry mode.
     */
    public RemoteRenderableRegistryMode() {
	super(MODE_NAME,
	      RemoteDescriptor.class,
	      getThisFactoryMethod().getReturnType(),
	      getThisFactoryMethod(),
	      false,                        // arePreferencesSupported
	      false);                       // arePropertiesSupported)
    }
}
