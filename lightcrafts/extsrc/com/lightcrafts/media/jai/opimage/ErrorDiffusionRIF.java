/*
 * $RCSfile: ErrorDiffusionRIF.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:25 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.opimage;
import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderedImageFactory;
import com.lightcrafts.mediax.jai.ImageLayout;
import com.lightcrafts.mediax.jai.KernelJAI;
import com.lightcrafts.mediax.jai.LookupTableJAI;

/**
 * A <code>RIF</code> supporting the "ErrorDiffusion" operation in the rendered
 * image layer.
 *
 * @since EA2
 * @see com.lightcrafts.mediax.jai.operator.ErrorDiffusionDescriptor
 */
public class ErrorDiffusionRIF implements RenderedImageFactory {

    /** Constructor. */
    public ErrorDiffusionRIF() {}

    /**
     * Creates a new instance of an error diffusion operator according to the
     * color map and error filter kernel.
     *
     * @param paramBlock  The color map and error filter kernel objects.
     */
    public RenderedImage create(ParameterBlock paramBlock,
                                RenderingHints renderHints) {
        // Get ImageLayout from renderHints if any.
        ImageLayout layout = RIFUtil.getImageLayoutHint(renderHints);
        

        RenderedImage source = paramBlock.getRenderedSource(0);
        LookupTableJAI lookupTable =
            (LookupTableJAI)paramBlock.getObjectParameter(0);
        KernelJAI kernel = (KernelJAI)paramBlock.getObjectParameter(1);

        return new ErrorDiffusionOpImage(source, renderHints, layout,
                                         lookupTable, kernel);
    }
}
