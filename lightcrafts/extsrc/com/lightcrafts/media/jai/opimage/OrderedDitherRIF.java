/*
 * $RCSfile: OrderedDitherRIF.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:39 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.opimage;
import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderedImageFactory;
import com.lightcrafts.mediax.jai.ColorCube;
import com.lightcrafts.mediax.jai.ImageLayout;
import com.lightcrafts.mediax.jai.KernelJAI;

/**
 * A <code>RIF</code> supporting the "OrderedDither" operation in the rendered
 * image layer.
 *
 * @since EA3
 * @see com.lightcrafts.mediax.jai.operator.OrderedDitherDescriptor
 *
 */
public class OrderedDitherRIF implements RenderedImageFactory {

    /** Constructor. */
    public OrderedDitherRIF() {}

    /**
     * Creates a new instance of an ordered dither operator according to the
     * color map and dither mask kernel array.
     *
     * @param paramBlock  The color map and dither mask kernel array objects.
     */
    public RenderedImage create(ParameterBlock paramBlock,
                                RenderingHints renderHints) {
        // Get ImageLayout from renderHints if any.
        ImageLayout layout = RIFUtil.getImageLayoutHint(renderHints);
        

        RenderedImage source = paramBlock.getRenderedSource(0);
        ColorCube colorMap =
            (ColorCube)paramBlock.getObjectParameter(0);
        KernelJAI[] ditherMask = (KernelJAI[])paramBlock.getObjectParameter(1);

        return new OrderedDitherOpImage(source, renderHints, layout,
                                        colorMap, ditherMask);
    }
}
