/*
 * $RCSfile: MinFilterRIF.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:35 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.opimage;
import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderedImageFactory;
import com.lightcrafts.mediax.jai.BorderExtender;
import com.lightcrafts.mediax.jai.ImageLayout;
import com.lightcrafts.mediax.jai.operator.MinFilterDescriptor;
import com.lightcrafts.mediax.jai.operator.MinFilterShape;

/**
 *  Creates a MinFilterOpImage subclass for the given input
 *  mask type
 *  @see MinFilterOpImage
 */
public class MinFilterRIF implements RenderedImageFactory {

    /** Constructor. */
    public MinFilterRIF() {}

    /**
     * Create a new instance of MinFilterOpImage in the rendered layer.
     * This method satisfies the implementation of RIF.
     *
     * @param paramBlock  The source image and the convolution kernel.
     */
    public RenderedImage create(ParameterBlock paramBlock,
                                RenderingHints renderHints) {
        // Get ImageLayout from renderHints if any.
        ImageLayout layout = RIFUtil.getImageLayoutHint(renderHints);
        

        // Get BorderExtender from renderHints if any.
        BorderExtender extender = RIFUtil.getBorderExtenderHint(renderHints);

        MinFilterShape maskType =
            (MinFilterShape)paramBlock.getObjectParameter(0);
        int maskSize = paramBlock.getIntParameter(1);
        RenderedImage ri = paramBlock.getRenderedSource(0);
        
        if(maskType.equals(MinFilterDescriptor.MIN_MASK_SQUARE)) {
           return new MinFilterSquareOpImage(ri,
                                             extender,
                                             renderHints,
                                             layout,
                                             maskSize);
        } else if(maskType.equals(MinFilterDescriptor.MIN_MASK_PLUS)) {
           return new MinFilterPlusOpImage(ri,
                                           extender,
                                           renderHints,
                                           layout,
                                           maskSize);
        } else if(maskType.equals(MinFilterDescriptor.MIN_MASK_X)) {
           return new MinFilterXOpImage(ri,
                                        extender,
                                        renderHints,
                                        layout,
                                        maskSize);
        } else if(maskType.equals(MinFilterDescriptor.MIN_MASK_SQUARE_SEPARABLE)) {
           return new MinFilterSeparableOpImage(ri,
                                                extender,
                                                renderHints,
                                                layout,
                                                maskSize);
        }
        return null;
    }
}
