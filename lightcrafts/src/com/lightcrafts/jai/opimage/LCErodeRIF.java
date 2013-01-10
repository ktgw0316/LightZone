/* Copyright (C) 2005-2011 Fabio Riccardi */

/*
 * $RCSfile: ErodeRIF.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:24 $
 * $State: Exp $
 */
package com.lightcrafts.jai.opimage;

import com.sun.media.jai.opimage.RIFUtil;

import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderedImageFactory;
import javax.media.jai.BorderExtender;
import javax.media.jai.ImageLayout;
import javax.media.jai.KernelJAI;

/**
 * @see LCErodeOpImage
 */
public class LCErodeRIF implements RenderedImageFactory {

    /** Constructor. */
    public LCErodeRIF() {}

    /**
     * Create a new instance of ErodeOpImage in the rendered layer.
     * This method satisfies the implementation of RIF.
     *
     * @param paramBlock  The source image and the erosion kernel.
     */
    public RenderedImage create(ParameterBlock paramBlock,
                                RenderingHints renderHints) {
        // Get ImageLayout from renderHints if any.
        ImageLayout layout = RIFUtil.getImageLayoutHint(renderHints);


        // Get BorderExtender from renderHints if any.
        BorderExtender extender = RIFUtil.getBorderExtenderHint(renderHints);

        KernelJAI unRotatedKernel =
            (KernelJAI)paramBlock.getObjectParameter(0);
        KernelJAI kJAI = unRotatedKernel.getRotatedKernel();

	RenderedImage source = paramBlock.getRenderedSource(0);

        return new LCErodeOpImage(source,
                                  extender,
                                  renderHints,
                                  layout,
                                  kJAI);
    }
}
