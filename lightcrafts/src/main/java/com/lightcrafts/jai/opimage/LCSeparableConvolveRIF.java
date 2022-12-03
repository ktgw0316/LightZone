/* Copyright (C) 2005-2011 Fabio Riccardi */

/*
 * $RCSfile: ConvolveRIF.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:20 $
 * $State: Exp $
 */
package com.lightcrafts.jai.opimage;

import com.sun.media.jai.opimage.RIFUtil;

import java.awt.RenderingHints;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderedImageFactory;
import javax.media.jai.BorderExtender;
import javax.media.jai.ImageLayout;
import javax.media.jai.KernelJAI;

/**
 * @see ConvolveOpImage
 */
public class LCSeparableConvolveRIF implements RenderedImageFactory {

    /** Constructor. */
    public LCSeparableConvolveRIF() {}

    /**
     * Create a new instance of ConvolveOpImage in the rendered layer.
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

        KernelJAI unRotatedKernel =
            (KernelJAI)paramBlock.getObjectParameter(0);
        KernelJAI kJAI = unRotatedKernel.getRotatedKernel();

        int dataType =
           paramBlock.getRenderedSource(0).getSampleModel().getDataType();
        boolean dataTypeOk = (dataType == DataBuffer.TYPE_BYTE ||
                              dataType == DataBuffer.TYPE_SHORT ||
                              dataType == DataBuffer.TYPE_INT);

        if (kJAI.isSeparable()) {
           return new LCSeparableConvolveOpImage(paramBlock.getRenderedSource(0),
                                               extender,
                                               renderHints,
                                               layout,
                                               kJAI);

        } else {
            return null;
        }
    }
}
