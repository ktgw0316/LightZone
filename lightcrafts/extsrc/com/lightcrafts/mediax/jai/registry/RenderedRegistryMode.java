/*
 * $RCSfile: RenderedRegistryMode.java,v $
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
 * A class that provides information about the "rendered" registry
 * (operation) mode.
 *
 * @since JAI 1.1
 */
public class RenderedRegistryMode extends RegistryMode {

    public static final String MODE_NAME = "rendered";

    // The Method used to "create" objects from this factory.
    private static Method factoryMethod = null;

    private static Method getThisFactoryMethod() {

	if (factoryMethod != null)
	    return factoryMethod;

	// The factory Class that this registry mode represents.
	Class factoryClass =
		    java.awt.image.renderable.RenderedImageFactory.class;

	try {
	    Class[] paramTypes = new Class[]
		    {java.awt.image.renderable.ParameterBlock.class,
		     java.awt.RenderingHints.class};

	    factoryMethod = factoryClass.getMethod("create", paramTypes);

	} catch (NoSuchMethodException e) {
            ImagingListener listener =
                JAI.getDefaultInstance().getImagingListener();
            String message = JaiI18N.getString("RegistryMode0") + " " +
                             factoryClass.getName() + ".";
            listener.errorOccurred(message, e,
                                   RenderedRegistryMode.class, false);
//	    e.printStackTrace();
	}

	return factoryMethod;
    }

    /**
     * Constructor. A <code>RegistryMode</code> that represents a
     * <code>RenderedImageFactory</code> keyed by the string "rendered".
     */
    public RenderedRegistryMode() {

	super(MODE_NAME, com.lightcrafts.mediax.jai.OperationDescriptor.class,
		getThisFactoryMethod().getReturnType(),
		getThisFactoryMethod(), true, true);
    }
}
