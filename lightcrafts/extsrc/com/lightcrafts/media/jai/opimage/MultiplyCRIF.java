/*
 * $RCSfile: MultiplyCRIF.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:36 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.opimage;
import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import com.lightcrafts.mediax.jai.CRIFImpl;
import com.lightcrafts.mediax.jai.ImageLayout;

/**
 * A <code>CRIF</code> supporting the "Multiply" operation in the
 * rendered and renderable image layers.
 *
 * @since EA2
 * @see com.lightcrafts.mediax.jai.operator.MultiplyDescriptor
 * @see MultiplyOpImage
 *
 */
public class MultiplyCRIF extends CRIFImpl {

     /** Constructor. */
    public MultiplyCRIF() {
        super("multiply");
    }

    /**
     * Creates a new instance of <code>MultiplyOpImage</code> in the
     * rendered layer. This method satisifies the implementation of RIF.
     *
     * @param paramBlock   The two source images to be multiplied.
     * @param renderHints  Optionally contains destination image layout.     
     */
    public RenderedImage create(ParameterBlock paramBlock,
                                RenderingHints renderHints) {
        // Get ImageLayout from renderHints if any.
        ImageLayout layout = RIFUtil.getImageLayoutHint(renderHints);
        

        return new MultiplyOpImage(paramBlock.getRenderedSource(0),
				   paramBlock.getRenderedSource(1),
                                   renderHints,
				   layout);
    }
}
