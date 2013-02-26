/*
 * $RCSfile: ImagingListener.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:57 $
 * $State: Exp $
 */
package com.lightcrafts.mediax.jai.util;

/**
 * An <code> ImagingListener</code> has the capability to report the
 * abnormal situations in the image processing procedures.
 * The concrete class either holds the reported information and processes
 * it, or passes them to other processing or logging mechanisms.
 *
 * <p> A default <code>ImagingListener</code> resides in an instance of
 * <code>JAI</code> (by calling the method
 * <code>setImagingListener</code>), and can be retrieved by calling
 * the method <code>getImagingListener</code>.  This listener should not
 * propagate the <code>Throwable</code> reported from an
 * <code>OperationRegistry</code>.  Otherwise, it may break the loop through
 * the image factories. The typical <code>JAI</code> to be used will
 * be the default JAI instance.
 *
 * <p> An <code>ImagingListener</code> can also be attached to a rendering node
 * as a rendering hint with a key <code>JAI.KEY_IMAGING_LISTENER</code>.
 * This listener can monitor errors occurring in the rendering process.
 * The default value for this rendering hint is the listener registered
 * to the default <code>JAI</code> instance.
 *
 * <p> The typical situations where <code>ImagingListener</code>
 * objects can be called are:
 * (1) The <code>create</code> method of a concrete
 * <code>RenderedImageFactory</code> or
 * <code>ContextualRenderedImageFactory</code>. (2) The rendering of the node.
 * For the latter case, the I/O, network, and arithmetic problems
 * will be reported.
 *
 * <p> When errors are encountered in user-written operations, those
 * operations have two choices.  The typical choice will be to simply
 * throw an exception and let the JAI framework call
 * <code>errorOccurred</code>.  However, it is also acceptable to
 * obtain the proper <code>ImagingListener</code> and call
 * <code>errorOccurred</code> directly.  This might be useful if
 * for example special retry options were available to the user
 * operation.  Care should be taken in this case to avoid an
 * infinite retry loop.
 *
 * <p> For backward compatibility, an instance of a simple
 * implementation of this interface is used as the default in all the
 * <code>JAI</code> instances.  It re-throws the <code>Throwable</code>
 * if it is a <code>RuntimeException</code>.  For the other types of
 * <code>Throwable</code>, it only prints the message and the stack trace
 * to the stream <code>System.err</code>, and returns <code>false</code>.
 * To process the reported errors or warnings an alternate implementation
 * of <code>ImagingListener</code> should be written.</p>
 *
 * <p> The provided <code>Throwable</code>, its cause, or its root cause
 *  may be re-thrown directly, or wrapped into a subclass of
 *  <code>RuntimeException</code> and thrown if this listener cannot
 *  handle it properly, in which case the <code>Throwable</code> will
 *  be propogated back to the calling application.
 *
 *  <p> In the JAI 1.1.2 implementation from Sun, when the method
 *  <code>errorOccurred</code> is called, the parameter
 *  <code>isRetryable</code> is always <code>false</code>; future
 *  implementations may activate retry capability.</p>
 *
 * @since JAI 1.1.2
 */
public interface ImagingListener {
    /**
     * Reports the occurrence of an error or warning generated
     * from JAI or one of its operation nodes.
     *
     * @param message The message that describes what is reported.
     * @param thrown The <code>Throwable</code> that caused this
     *               method to be called..
     * @param where The object from which this <code>Throwable</code> is caught
     *               and sent to this listener.  The typical type for this
     *               argument will be <code>RenderedOp</code>,
     *               <code>OpImage</code>, <code>RenderedImageFactory</code>,
     *               or other image types such as the
     *               <code>RenderedImage</code> generated from codecs.
     * @param isRetryable Indicates whether or not the caller is capable of
     *               retrying the operation, if the problem is corrected
     *               in this method.  If this parameter is <code>false</code>,
     *               the return value should also be <code>false</code>.
     *               This parameter can be used to stop the retry, e.g.,
     *               if a maximum retry number is reached.
     *
     * @return Returns <code>true</code> if the recovery is a success
     *               and the caller should attempt a retry; otherwise
     *               returns <code>false</code> (in which case no retry
     *               should be attempted).  The return value may be
     *               ignored by the caller if <code>isRetryable</code>
     *               is <code>false</code>.
     * @throws RuntimeException Propagates the <code>Throwable</code> to
     *               the caller.
     */
    boolean errorOccurred(String message,
                          Throwable thrown,
                          Object where,
                          boolean isRetryable) throws RuntimeException;
}
