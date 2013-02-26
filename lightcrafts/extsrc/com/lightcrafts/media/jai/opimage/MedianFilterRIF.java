/*
 * $RCSfile: MedianFilterRIF.java,v $
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
import java.awt.image.renderable.RenderedImageFactory;
import com.lightcrafts.mediax.jai.BorderExtender;
import com.lightcrafts.mediax.jai.ImageLayout;
import com.lightcrafts.mediax.jai.operator.MedianFilterDescriptor;
import com.lightcrafts.mediax.jai.operator.MedianFilterShape;

/**
 *  Creates a MedianFilterOpImage subclass for the given input
 *  mask type
 *  @see MedianFilterOpImage
 */
public class MedianFilterRIF implements RenderedImageFactory {

    /** Constructor. */
    public MedianFilterRIF() {}

    /**
     * Create a new instance of MedianFilterOpImage in the rendered layer.
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

        MedianFilterShape maskType =
            (MedianFilterShape)paramBlock.getObjectParameter(0);
        int maskSize = paramBlock.getIntParameter(1);
        RenderedImage ri = paramBlock.getRenderedSource(0);
        
        if(maskType.equals(MedianFilterDescriptor.MEDIAN_MASK_SQUARE)) {
           return new MedianFilterSquareOpImage(ri,
                                             extender,
                                             renderHints,
                                             layout,
                                             maskSize);
        } else if(maskType.equals(MedianFilterDescriptor.MEDIAN_MASK_PLUS)) {
           return new MedianFilterPlusOpImage(ri,
                                           extender,
                                           renderHints,
                                           layout,
                                           maskSize);
        } else if(maskType.equals(MedianFilterDescriptor.MEDIAN_MASK_X)) {
           return new MedianFilterXOpImage(ri,
                                        extender,
                                        renderHints,
                                        layout,
                                        maskSize);
        } else if(maskType.equals(MedianFilterDescriptor.MEDIAN_MASK_SQUARE_SEPARABLE)) {
           return new MedianFilterSeparableOpImage(ri,
                                                extender,
                                                renderHints,
                                                layout,
                                                maskSize);
        }
        return null;
    }
}
