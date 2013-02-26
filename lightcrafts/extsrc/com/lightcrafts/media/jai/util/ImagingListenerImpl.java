/*
 * $RCSfile: ImagingListenerImpl.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:00 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.util;
import java.lang.ref.SoftReference;
import com.lightcrafts.mediax.jai.OperationRegistry;
import com.lightcrafts.mediax.jai.util.ImagingListener;

/**
 * A simply implementation of the interface <code> ImagingListener</code>.
 * In the method <code>errorOccurred</code>, only the message and the
 * stack trace of the provided <code>Throwable</code> is printed
 * into the stream <code>System.err</code>.  This keeps the
 * backward compatibility.
 *
 * <p> This class is a singleton that has only one instance.  This single
 * instance can be retrieved by calling the static method
 * <code>getInstance</code>.
 *
 * @see ImagingListener
 *
 * @since JAI 1.1.2
 */
public class ImagingListenerImpl implements ImagingListener {
    private static SoftReference reference = new SoftReference(null);

    /**
     * Retrieves the unique instance of this class the construction of
     * which is deferred until the first invocation of this method.
     */
    public static ImagingListenerImpl getInstance() {
        synchronized(reference) {
            Object referent = reference.get();
            ImagingListenerImpl listener;
            if (referent == null) {
                // First invocation or SoftReference has been cleared.
                reference =
                    new SoftReference(listener = new ImagingListenerImpl());
            } else {
                // SoftReference has not been cleared.
                listener = (ImagingListenerImpl)referent;
            }

            return listener;
        }
    }

    /**
     * The constructor.
     */
    private ImagingListenerImpl() {}

    public synchronized boolean errorOccurred(String message,
                                              Throwable thrown,
                                              Object where,
                                              boolean isRetryable)
                                              throws RuntimeException {
        // Silent the RuntimeException occuring in any OperationRegistry
        // and rethrown all the other RuntimeExceptions.
        if (thrown instanceof RuntimeException &&
            !(where instanceof OperationRegistry))
            throw (RuntimeException)thrown;

        System.err.println("Error: " + message);
        System.err.println("Occurs in: " +
                           ((where instanceof Class) ?
                           ((Class)where).getName() :
                           where.getClass().getName()));
        thrown.printStackTrace(System.err);
        return false;
    }
}
