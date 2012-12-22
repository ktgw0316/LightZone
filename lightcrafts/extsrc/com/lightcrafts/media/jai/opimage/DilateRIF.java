/*
 * $RCSfile: DilateRIF.java,v $
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
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.MultiPixelPackedSampleModel;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderedImageFactory;
import com.lightcrafts.mediax.jai.BorderExtender;
import com.lightcrafts.mediax.jai.ImageLayout;
import com.lightcrafts.mediax.jai.KernelJAI;

/**
 * @see DilateOpImage
 */
public class DilateRIF implements RenderedImageFactory {

    /** Constructor. */
    public DilateRIF() {}

    /**
     * Create a new instance of DilateOpImage in the rendered layer.
     * This method satisfies the implementation of RIF.
     *
     * @param paramBlock  The source image and the dilation kernel.
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
	SampleModel sm = source.getSampleModel();

	// check dataType and binary 
        int dataType = sm.getDataType();

        boolean isBinary = (sm instanceof MultiPixelPackedSampleModel) &&
            (sm.getSampleSize(0) == 1) &&
            (dataType == DataBuffer.TYPE_BYTE || 
             dataType == DataBuffer.TYPE_USHORT || 
             dataType == DataBuffer.TYPE_INT);

	// possible speed up later: 3x3 with table lookup
	if (isBinary){


	  return new DilateBinaryOpImage(source,
				 extender,
				 renderHints,
				 layout,
				 kJAI);
	}else{
	  return new DilateOpImage(source,
				 extender,
				 renderHints,
				 layout,
				 kJAI);
	}
    }
}
