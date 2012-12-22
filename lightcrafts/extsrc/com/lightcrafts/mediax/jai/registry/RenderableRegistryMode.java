/*
 * $RCSfile: RenderableRegistryMode.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:49 $
 * $State: Exp $
 */
package com.lightcrafts.mediax.jai.registry;

import java.awt.image.renderable.RenderableImage;
import java.lang.reflect.Method;
import com.lightcrafts.mediax.jai.JAI;
import com.lightcrafts.mediax.jai.RegistryMode;
import com.lightcrafts.mediax.jai.util.ImagingListener;

/**
 * A class that provides information about the "renderable" registry
 * (operation) mode.
 *
 * @since JAI 1.1
 */
public class RenderableRegistryMode extends RegistryMode {

    public static final String MODE_NAME = "renderable";

    // The Method used to "create" objects from this factory.
    private static Method factoryMethod = null;

    private static Method getThisFactoryMethod() {

	if (factoryMethod != null)
	    return factoryMethod;

	// The factory Class that this registry mode represents.
	Class factoryClass =
	    java.awt.image.renderable.ContextualRenderedImageFactory.class;

	try {
	    Class[] paramTypes = new Class[]
		    {java.awt.image.renderable.RenderContext.class,
		     java.awt.image.renderable.ParameterBlock.class};

	    factoryMethod = factoryClass.getMethod("create", paramTypes);

	} catch (NoSuchMethodException e) {
            ImagingListener listener =
                JAI.getDefaultInstance().getImagingListener();
            String message = JaiI18N.getString("RegistryMode0") + " " +
                             factoryClass.getName() + ".";
            listener.errorOccurred(message, e,
                                   RenderableRegistryMode.class, false);
//	    e.printStackTrace();
	}

	return factoryMethod;
    }

    /**
     * Constructor. A <code>RegistryMode</code> that represents a
     * <code>ContextualRenderedImageFactory</code> keyed in a case
     * insensitive fashion by the string "renderable". The "renderable"
     * mode has no preferences but supports properties.
     */
    public RenderableRegistryMode() {

	super(MODE_NAME, com.lightcrafts.mediax.jai.OperationDescriptor.class,
		RenderableImage.class,
		getThisFactoryMethod(), false, true);
    }
}
