/*
 * $RCSfile: UnsharpMaskRIF.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:46 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.opimage;

import com.lightcrafts.media.jai.util.ImageUtil;
import java.awt.RenderingHints;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderedImageFactory;
import com.lightcrafts.mediax.jai.BorderExtender;
import com.lightcrafts.mediax.jai.ImageLayout;
import com.lightcrafts.mediax.jai.KernelJAI;

/**
 * @see UnsharpMaskOpImage
 */
public class UnsharpMaskRIF implements RenderedImageFactory {

    /** Constructor. */
    public UnsharpMaskRIF() {}

    /**
     * Create a new instance of UnsharpMaskOpImage in the rendered layer.
     * This method satisfies the implementation of RIF.
     *
     * @param paramBlock  The source image, the unsharp mask kernel and
     *			  the gain factor.
     */
    public RenderedImage create(ParameterBlock paramBlock,
                                RenderingHints renderHints) {
        // Get ImageLayout from renderHints if any.
        ImageLayout layout = RIFUtil.getImageLayoutHint(renderHints);
        

        // Get BorderExtender from renderHints if any.
        BorderExtender extender = RIFUtil.getBorderExtenderHint(renderHints);

	// map the input kernel + gain factor to an equivalent
	// convolution kernel and then do a normal convolve.
	KernelJAI unRotatedKernel =
		ImageUtil.getUnsharpMaskEquivalentKernel(
			(KernelJAI)paramBlock.getObjectParameter(0),
			paramBlock.getFloatParameter(1));

        KernelJAI kJAI = unRotatedKernel.getRotatedKernel();

	RenderedImage source = paramBlock.getRenderedSource(0);
        int dataType = source.getSampleModel().getDataType();

        boolean dataTypeOk = (dataType == DataBuffer.TYPE_BYTE  ||
                              dataType == DataBuffer.TYPE_SHORT ||
                              dataType == DataBuffer.TYPE_INT);

        if ((kJAI.getWidth()   == 3) && (kJAI.getHeight()  == 3) &&
            (kJAI.getXOrigin() == 1) && (kJAI.getYOrigin() == 1) && dataTypeOk) {
            return new Convolve3x3OpImage(source,
                                          extender,
                                          renderHints,
                                          layout,
                                          kJAI);
        } else if (kJAI.isSeparable()) {
           return new SeparableConvolveOpImage(source,
                                               extender,
                                               renderHints,
                                               layout,
                                               kJAI);

        } else {
            return new ConvolveOpImage(source,
                                       extender,
                                       renderHints,
                                       layout,
                                       kJAI);
        }
    }
}
