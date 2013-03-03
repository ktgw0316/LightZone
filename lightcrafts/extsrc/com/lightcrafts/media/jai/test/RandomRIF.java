/*
 * $RCSfile: RandomRIF.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.2 $
 * $Date: 2005/02/24 02:07:44 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.test;
import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderedImageFactory;
import com.lightcrafts.mediax.jai.ImageLayout;
import com.lightcrafts.mediax.jai.JAI;

/**
 * @see RandomOpImage
 */
public class RandomRIF implements RenderedImageFactory {

    /** Constructor. */
    public RandomRIF() {}

    /**
     * Creates a new instance of RandomOpImage in the rendered layer.
     * This method satisfies the implementation of RIF.
     */
    public RenderedImage create(ParameterBlock paramBlock,
                                RenderingHints renderHints) {
        // Get ImageLayout from renderHints if any.
        ImageLayout layout = (renderHints == null) ? null : 
	    (ImageLayout)renderHints.get(JAI.KEY_IMAGE_LAYOUT);
        
        return OpImageTester.createRandomOpImage(renderHints, layout);
    }
}
