/*
 * $RCSfile: MinCRIF.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:34 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.opimage;
import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import com.lightcrafts.mediax.jai.CRIFImpl;
import com.lightcrafts.mediax.jai.ImageLayout;

/**
 * A <code>CRIF</code> supporting the "Min" operation in the
 * rendered and renderable image layer.
 *
 * @see com.lightcrafts.mediax.jai.operator.MinDescriptor
 * @see MinOpImage
 *
 */
public class MinCRIF extends CRIFImpl {

    /** Constructor. */
    public MinCRIF() {
        super("min");
    }

    /**
     * Creates a new instance of <code>MinOpImage</code> in the rendered
     * layer. This method satisfies the implementation of RIF.
     *
     * @param paramBlock  The two source images from which the minimum
     *        pixel values are chosen.
     * @param renderHints  Optionally contains destination image layout
     *        and tile cache.
     */
    public RenderedImage create(ParameterBlock paramBlock,
                                RenderingHints renderHints) {
        // Get ImageLayout from renderHints if any.
        ImageLayout layout = RIFUtil.getImageLayoutHint(renderHints);
        

        return new MinOpImage(paramBlock.getRenderedSource(0),
                              paramBlock.getRenderedSource(1),
                              renderHints,
                              layout);
    }
}
