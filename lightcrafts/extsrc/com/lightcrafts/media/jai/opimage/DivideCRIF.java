/*
 * $RCSfile: DivideCRIF.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:23 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.opimage;
import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import com.lightcrafts.mediax.jai.CRIFImpl;
import com.lightcrafts.mediax.jai.ImageLayout;

/**
 * A <code>CRIF</code> supporting the "Divide" operation in the rendered
 * and renderable image layers.
 *
 * @since EA2
 * @see com.lightcrafts.mediax.jai.operator.DivideDescriptor
 * @see DivideOpImage
 *
 */
public class DivideCRIF extends CRIFImpl {

    /** Constructor. */
    public DivideCRIF() {
        super("divide");
    }

    /**
     * Creates a new instance of <code>DivideOpImage</code> in the rendered
     * layer. This method satisfies the implementation of RIF.
     *
     * @param paramBlock   The two source images to be divided.
     * @param renderHints  Optionally contains destination image layout.
     */
    public RenderedImage create(ParameterBlock paramBlock,
                                RenderingHints renderHints) {
        // Get ImageLayout from renderHints if any.
        ImageLayout layout = RIFUtil.getImageLayoutHint(renderHints);
        

        return new DivideOpImage(paramBlock.getRenderedSource(0),
				 paramBlock.getRenderedSource(1),
                                 renderHints,
				 layout);
    }
}
