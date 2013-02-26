/*
 * $RCSfile: RenderableCollectionImageFactory.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:19 $
 * $State: Exp $
 */
package com.lightcrafts.mediax.jai;

import java.awt.image.renderable.ParameterBlock;

/**
 * The <code>RenderableCollectionImageFactory</code> (RCIF) interface
 * is intended to be implemented by classes that wish to act as factories
 * to produce different collection image operators.  In JAI, the
 * <code>create()</code> method defined by this interface will be
 * invoked in a chain of <code>CollectionOp</code>s when the operation is
 * being executed in renderable mode.  The images contained in the
 * generated <code>CollectionImage</code> would be expected to be
 * <code>RenderableImage</code>s.
 *
 * @since JAI 1.1
 */
public interface RenderableCollectionImageFactory {

    /**
     * Creates a <code>CollectionImage</code> that represents the
     * result of an operation (or chain of operations) for a given
     * <code>ParameterBlock</code>.
     * If the operation is unable to handle the input arguments, this
     * method should return <code>null</code>.
     *
     * <p> Generally this method is expected to be invoked by an operation
     * being executed in renderable mode.  Therefore the images contained
     * in the generated <code>CollectionImage</code> would be expected to
     * be <code>RenderableImage</code>s.
     *
     * @param args  Input arguments to the operation, including
     *        sources and/or parameters.
     *
     * @return  A <code>CollectionImage</code> containing the desired output.
     */
    CollectionImage create(ParameterBlock parameters);
}
