/*
 * $RCSfile: RenderableCollectionRegistryMode.java,v $
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

import java.lang.reflect.Method;
import com.lightcrafts.mediax.jai.JAI;
import com.lightcrafts.mediax.jai.RegistryMode;
import com.lightcrafts.mediax.jai.util.ImagingListener;

/**
 * A class that provides information about the "renderableCollection" registry
 * (operation) mode.
 *
 * @since JAI 1.1
 */
public class RenderableCollectionRegistryMode extends RegistryMode {

    public static final String MODE_NAME = "renderableCollection";

    // The Method used to "create" objects from this factory.
    private static Method factoryMethod = null;

    private static Method getThisFactoryMethod() {

	if (factoryMethod != null)
	    return factoryMethod;

	// The factory Class that this registry mode represents.
	Class factoryClass =
                com.lightcrafts.mediax.jai.RenderableCollectionImageFactory.class;

	try {
	    Class[] paramTypes = new Class[]
		    {java.awt.image.renderable.ParameterBlock.class};

	    factoryMethod = factoryClass.getMethod("create", paramTypes);

	} catch (NoSuchMethodException e) {
            ImagingListener listener =
                JAI.getDefaultInstance().getImagingListener();
            String message = JaiI18N.getString("RegistryMode0") + " " +
                             factoryClass.getName() + ".";
            listener.errorOccurred(message, e,
                                   RenderableCollectionRegistryMode.class, false);
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
    public RenderableCollectionRegistryMode() {

	super(MODE_NAME, com.lightcrafts.mediax.jai.OperationDescriptor.class,
		getThisFactoryMethod().getReturnType(),
		getThisFactoryMethod(), false, true);
    }
}
