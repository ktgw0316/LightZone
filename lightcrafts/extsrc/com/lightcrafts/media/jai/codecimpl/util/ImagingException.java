/*
 * $RCSfile: ImagingException.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:57 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.codecimpl.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.security.PrivilegedActionException;

/**
 * This class is designed to contain information of the (abnormal)
 * situations that happen in JAI and the operations plugged into JAI.
 * The behavior of this class imitates the <code>Exception</code> class
 * in Java<sup><font size="-2">TM</font></sup> 2 Platform version 1.4
 * to define a chained exception and is call-compatible
 * with Java<sup><font size="-2">TM</font></sup> 2 Platform version 1.4:
 * The cause can be stored and retrieved from the
 * instance of this class. Also, the root cause for an instance
 * can be retrieved.
 *
 * @since JAI 1.1.2
 */
public class ImagingException extends RuntimeException {
    /** The cached cause. */
    private Throwable cause = null;

    /** The default constructor. */
    public ImagingException() {
	super();
    }

    /**
     * The constructor to accept the message that describes the situation.
     *
     * @param message The message to describe the situation.
     */
    public ImagingException(String message) {
	super(message);
    }

    /**
     * The constructor to accept the cause of this <code>ImagingException</code>.
     *
     * @param cause The cause of this <code>ImagingException</code>.
     */
    public ImagingException(Throwable cause) {
	super();
	this.cause = cause;
    }

    /**
     * The constructor to accept the cause of this <code>ImagingException</code>
     * and the message that describes the situation.
     *
     * @param message The message that describes the situation.
     * @param cause The cause of this <code>ImagingException</code>
     */
    public ImagingException(String message, Throwable cause) {
	super(message);
	this.cause = cause;
    }

    /**
     * Returns the cause of this <code>ImagingException</code>.
     */
    public Throwable getCause() {
	return cause;
    }

    /**
     * Recursively retrieves the root cause of this
     * <code>ImagingException</code>.
     */
    public Throwable getRootCause() {
	Throwable rootCause = cause;
	Throwable atop = this;

	while(rootCause != atop && rootCause != null) {
	    try {
		atop = rootCause;
		Method getCause = rootCause.getClass().getMethod("getCause", (Class<?>[]) null);
		rootCause = (Throwable)getCause.invoke(rootCause, (Object[]) null);
	    } catch (Exception e) {
		// do nothing.  This happens (1) getCause method is not defined.
		// (2) Reflection error.  So rootCause will be the same as
		// atop. Then stop the loop.
                if (rootCause instanceof InvocationTargetException)
                    rootCause = ((InvocationTargetException)rootCause).getTargetException();
                else if (rootCause instanceof PrivilegedActionException)
                    rootCause = ((PrivilegedActionException)rootCause).getException();
		else rootCause = atop;
	    } finally {
		if (rootCause == null)
		    rootCause = atop;
	    }
	}

	return rootCause;
    }

    /**
     * Prints the stack trace. For Java<sup><font size="-2">TM</font></sup> 2
     * Platform version 1.4 and up, calls the same
     * method in the super class.  For Java<sup><font size="-2">TM</font></sup>
     * 2 Platform version 1.3, prints the stack trace of
     * this <code>ImagingException</code> and the trace of its cause.
     */
    public void printStackTrace() {
        printStackTrace(System.err);
    }

    /**
     * Prints the stack trace.  For Java<sup><font size="-2">TM</font></sup>
     * 2 Platform version 1.4 and up, calls the same
     * method in the super class.  For Java<sup><font size="-2">TM</font></sup>
     * 2 Platform version 1.4, prints the stack trace of
     * this <code>ImagingException</code> and the trace of its cause.
     */
    public void printStackTrace(PrintStream s) {

        synchronized (s) {
	    super.printStackTrace(s);
	    boolean is14 = false;
	    try {
		String version = System.getProperty("java.version");
		is14 = version.indexOf("1.4") >=0 ;
	    } catch(Exception e) {
	    }

	    if (!is14 && cause != null) {
		s.println("Caused by:");
		cause.printStackTrace(s);
	    }

        }
    }

    /**
     * Prints the stack trace.  For Java<sup><font size="-2">TM</font></sup> 2
     * Platform version 1.4 and up, calls the same
     * method in the super class.  For Java<sup><font size="-2">TM</font></sup>
     * 2 Platform version 1.43, prints the stack trace of
     * this <code>ImagingException</code> and the trace of its cause.
     */

    public void printStackTrace(PrintWriter s) {
        synchronized (s) {
            super.printStackTrace(s);
            boolean is14 = false;
            try {
                String version = System.getProperty("java.version");
                is14 = version.indexOf("1.4") >=0 ;
            } catch(Exception e) {
            }

            if (!is14 && cause != null) {
                s.println("Caused by:");
                cause.printStackTrace(s);
            }
        }
    }
/*
    public static void main(String[] args) {
	Exception ce = new ImagingException(new RuntimeException("test"));
	Throwable cause = ce.getCause();
	System.out.println("Cause: " + cause);
	System.out.println("Root cause: " + ((ImagingException)ce).getRootCause());
	ce.printStackTrace();
    }
*/
}
