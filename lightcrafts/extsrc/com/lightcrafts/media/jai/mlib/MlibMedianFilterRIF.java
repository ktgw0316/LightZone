/*
 * $RCSfile: MlibMedianFilterRIF.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:00 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.mlib;
import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderedImageFactory;
import com.lightcrafts.mediax.jai.BorderExtender;
import com.lightcrafts.mediax.jai.ImageLayout;
import com.lightcrafts.mediax.jai.operator.MedianFilterShape;
import com.lightcrafts.media.jai.opimage.RIFUtil;

/**
 *  Creates a MlibMedianFilterOpImage subclass for the given input
 *  mask type
 *  @see MlibMedianFilterOpImage
 */
public class MlibMedianFilterRIF implements RenderedImageFactory {

    /** Constructor. */
    public MlibMedianFilterRIF() {}

    /**
     * Create a new instance of MlibMedianFilterOpImage in the rendered layer.
     * This method satisfies the implementation of RIF.
     *
     * @param paramBlock  The source image and the convolution kernel.
     */
    public RenderedImage create(ParameterBlock paramBlock,
                                RenderingHints renderHints) {
        // Get ImageLayout from renderHints if any.
        ImageLayout layout = RIFUtil.getImageLayoutHint(renderHints);
        

        if (!MediaLibAccessor.isMediaLibCompatible(paramBlock, layout) ||
            !MediaLibAccessor.hasSameNumBands(paramBlock, layout)) {
            return null;
        }

        // Get BorderExtender from renderHints if any.
        BorderExtender extender = RIFUtil.getBorderExtenderHint(renderHints);

        MedianFilterShape maskType =
            (MedianFilterShape)paramBlock.getObjectParameter(0);
        int maskSize = paramBlock.getIntParameter(1);
        RenderedImage ri = paramBlock.getRenderedSource(0);
        
        return new MlibMedianFilterOpImage(ri,
                                           extender,
                                           renderHints,
                                           layout,
                                           maskType,
                                           maskSize);
    }
}
