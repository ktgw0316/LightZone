/*
 * $RCSfile: CollectionImageFactory.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:06 $
 * $State: Exp $
 */
package com.lightcrafts.mediax.jai;
import java.awt.RenderingHints;
import java.awt.image.renderable.ParameterBlock;

/**
 * The <code>CollectionImageFactory</code> (CIF) interface is intended
 * to be implemented by classes that wish to act as factories to produce
 * different collection image operators.  In JAI, the <code>create()</code>
 * method will be invoked in a chain of <code>CollectionOp</code>s when the
 * operation is being executed in rendered mode.
 */
public interface CollectionImageFactory {

    /**
     * Creates a <code>CollectionImage</code> that represents the
     * result of an operation (or chain of operations) for a given
     * <code>ParameterBlock</code> and <code>RenderingHints</code>.
     * If the operation is unable to handle the input arguments, this
     * method should return <code>null</code>.
     *
     * <p> Generally this method is expected to be invoked by an operation
     * being executed in rendered mode.
     *
     * @param args  Input arguments to the operation, including
     *        sources and/or parameters.
     * @param hints  The rendering hints.
     *
     * @return  A <code>CollectionImage</code> containing the desired output.
     */
    CollectionImage create(ParameterBlock args,
                           RenderingHints hints);

    /**
     * Attempts to modify a rendered <code>CollectionImage</code> previously
     * created by this <code>CollectionImageFactory</code> as a function
     * of how the sources, parameters and hints of the operation have
     * changed.  The <code>CollectionImage</code> passed in should not be
     * modified in place but some or or all of its contents may be copied
     * by reference into the <code>CollectionImage</code> returned, if any.
     * If none of the contents of the old <code>CollectionImage</code> can
     * be re-used, then <code>null</code> should be returned.
     *
     * @throws IllegalArgumentException if the name of the operation
     *	       associated with the <code>CollectionOp</code> does not
     *	       match that expected by this <code>CollectionImageFactory</code>.
     *
     * @return  A <code>CollectionImage</code> modified according to the
     *		new values of the <code>ParameterBlock</code> and
     *		<code>RenderingHints</code> or <code>null</code> if it
     *		is impracticable to perform the update.
     *
     * @since JAI 1.1
     */
    CollectionImage update(ParameterBlock oldParamBlock,
			   RenderingHints oldHints,
			   ParameterBlock newParamBlock,
			   RenderingHints newHints,
			   CollectionImage oldRendering,
			   CollectionOp op);
}
