/*
 * $RCSfile: RemoteImagingException.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:52 $
 * $State: Exp $
 */
package com.lightcrafts.mediax.jai.remote;

import com.lightcrafts.mediax.jai.util.ImagingException;

/**
 * <code>RemoteImagingException</code> is an <code>Exception</code> thrown
 * to indicate that an error condition was
 * encountered during remote image processing. All methods which might
 * encounter error conditions during remote image processing should
 * be tagged as throwing this exception, as this is something an
 * application might want to catch.
 *
 * <p> From JAI 1.1.2 on, this class is re-parented to
 * <code>ImagingException</code> which behaves as a chained exception.
 * Thus, the cause of a <code>RemoteImagingException</code> can be
 * retrieved by using the method <code>getCause</code>.</p>
 *
 * @since JAI 1.1
 */
public class RemoteImagingException extends ImagingException {

    /**
     * Constructs a <code>RemoteImagingException</code> with no detail
     * message. A detail message is a <code>String</code> that describes
     * this particular exception.
     */
    public RemoteImagingException() {
	super();
    }

    /**
     * Constructs a <code>RemoteImagingException</code> with the
     * specified detail message.  A detail message is a <code>String</code>
     * that describes this particular exception.
     *
     * @param message the <code>String</code> that contains a detailed message.
     */
    public RemoteImagingException(String message) {
	super(message);
    }

    /**
     * Constructs a <code>RemoteImagingException</code> with the
     * provided cause.
     *
     * @param cause The cause of this <code>RemoteImagingException</code>.
     *
     * @since JAI 1.1.2
     */
    public RemoteImagingException(Throwable cause) {
	super(cause);
    }

    /**
     * The constructor to accept the cause of this
     * <code>RemoteImagingException</code> and the message that
     * describes the situation.
     *
     * @param message The message that describes the situation.
     * @param cause The cause of this <code>RemoteImagingException</code>
     *
     * @since JAI 1.1.2
     */
    public RemoteImagingException(String message, Throwable cause) {
	super(message, cause);
    }
}
