/*
 * $RCSfile: ImagingListenerProxy.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:55:36 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.codecimpl;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import com.lightcrafts.media.jai.codecimpl.util.ImagingException;

public class ImagingListenerProxy {
    public static synchronized boolean errorOccurred(String message,
                                              Throwable thrown,
                                              Object where,
                                              boolean isRetryable)
                                              throws RuntimeException {
	Method errorOccurred = null;
	Object listener = null;

        try {
            Class<?> jaiClass = Class.forName("com.lightcrafts.mediax.jai.JAI");
            if (jaiClass == null)
                return defaultImpl(message, thrown, where, isRetryable);

            Method jaiInstance =
                jaiClass.getMethod("getDefaultInstance", (Class<?>[]) null);
            Method getListener =
		jaiClass.getMethod("getImagingListener", (Class<?>[]) null);

            Object jai = jaiInstance.invoke(null, (Object[]) null);
            if (jai == null)
                return defaultImpl(message, thrown, where, isRetryable);

            listener = getListener.invoke(jai, (Object[]) null);
            Class<? extends Object> listenerClass = listener.getClass();

            errorOccurred =
                listenerClass.getMethod("errorOccurred",
                                        new Class[]{String.class,
                                                    Throwable.class,
                                                    Object.class,
                                                    boolean.class});
	} catch(Throwable e) {
	    return defaultImpl(message, thrown, where, isRetryable);
	}

	try {
	    Boolean result =
                (Boolean)errorOccurred.invoke(listener, new Object[] {message,
                                                         thrown,
                                                         where,
                                                         new Boolean(isRetryable)});
	    return result.booleanValue();
	} catch(InvocationTargetException e) {
            Throwable te = ((InvocationTargetException)e).getTargetException();
	    throw new ImagingException(te);
	} catch(Throwable e) {
	    return defaultImpl(message, thrown, where, isRetryable);
	}
    }

    private static synchronized boolean defaultImpl(String message,
                                              Throwable thrown,
                                              Object where,
                                              boolean isRetryable)
                                              throws RuntimeException {
        // Silent the RuntimeException occuring in any OperationRegistry
        // and rethrown all the other RuntimeExceptions.
        if (thrown instanceof RuntimeException)
            throw (RuntimeException)thrown;

        System.err.println("Error: " + message);
        System.err.println("Occurs in: " +
                           ((where instanceof Class) ?
                           ((Class<?>)where).getName() :
                           where.getClass().getName()));
        thrown.printStackTrace(System.err);
        return false;
    }
}
