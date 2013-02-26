/*
 * $RCSfile: MlibDilateRIF.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:55:54 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.mlib;
import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderedImageFactory;
import com.lightcrafts.mediax.jai.BorderExtender;
import com.lightcrafts.mediax.jai.ImageLayout;
import com.lightcrafts.mediax.jai.KernelJAI;

import com.lightcrafts.media.jai.opimage.RIFUtil;

/**
 * A <code>RIF</code> supporting the "Dilate" operation in the
 * rendered image mode using MediaLib.
 *
 * @see com.lightcrafts.mediax.jai.operator.DilateDescriptor
 * @see MlibDilateOpImage
 */
public class MlibDilateRIF implements RenderedImageFactory {

    /** Constructor. */
    public MlibDilateRIF() {}

    /**
     * Creates a new instance of <code>MlibDilateOpImage</code> in
     * the rendered image mode.
     *
     * @param args  The source image and dilation kernel.
     * @param hints  May contain rendering hints and destination image layout.
     */
    public RenderedImage create(ParameterBlock args,
                                RenderingHints hints) {
        /* Get ImageLayout and TileCache from RenderingHints. */
        ImageLayout layout = RIFUtil.getImageLayoutHint(hints);
        

        boolean isBinary = false;
        if (!MediaLibAccessor.isMediaLibCompatible(args, layout) ||
            !MediaLibAccessor.hasSameNumBands(args, layout)) {
            if(!MediaLibAccessor.isMediaLibBinaryCompatible(args, layout)) {
                return null;
            }
            isBinary = true;
        }

        /* Get BorderExtender from hints if any. */
        BorderExtender extender = RIFUtil.getBorderExtenderHint(hints);

        RenderedImage source = args.getRenderedSource(0);

        KernelJAI unRotatedKernel = (KernelJAI)args.getObjectParameter(0);
        KernelJAI kJAI = unRotatedKernel.getRotatedKernel();

        int kWidth = kJAI.getWidth();
        int kHeight= kJAI.getHeight();
	int xOri   = kJAI.getXOrigin();
	int yOri   = kJAI.getYOrigin();
	int numB   = source.getSampleModel().getNumBands();

        /* mediaLib does not handle kernels with either dimension < 2. */

        if (xOri != 1 || yOri != 1 || kWidth != 3 || kHeight != 3 || numB != 1) {
	    return null;
	}
	   
	// check for plus and square type of kernel

	float[] kdata = kJAI.getKernelData();

	if (isBinary && isKernel3Square1(kdata) || !isBinary && isKernel3Square0(kdata)){	  

	    return new MlibDilate3SquareOpImage(source, extender, hints, layout);

	}


	if (isBinary && isKernel3Plus1(kdata)){
  	    // plus shape
	  
	    return new MlibDilate3PlusOpImage(source, extender, hints, layout);
	}
	


	return null;

    }

    // check to see if a 3x3 kernel has 1s at the plus positions and 0s elsewhere
    private boolean isKernel3Plus1(float[] kdata){
      
        return (kdata[0] == 0.0F && kdata[1] == 1.0F && kdata[2] == 0.0F &&
		kdata[3] == 1.0F && kdata[4] == 1.0F && kdata[5] == 1.0F &&
		kdata[6] == 0.0F && kdata[7] == 1.0F && kdata[8] == 0.0F);
    }

    // check to see if a 3x3 kernel has 1s at the plus positions
    private boolean isKernel3Square0(float[] kdata){
      
        return (kdata[0] == 0.0F && kdata[1] == 0.0F && kdata[2] == 0.0F &&
		kdata[3] == 0.0F && kdata[4] == 0.0F && kdata[5] == 0.0F &&
		kdata[6] == 0.0F && kdata[7] == 0.0F && kdata[8] == 0.0F);
    }

    // check to see if a 3x3 kernel has 1s at the plus positions
    private boolean isKernel3Square1(float[] kdata){
      
        return (kdata[0] == 1.0F && kdata[1] == 1.0F && kdata[2] == 1.0F &&
		kdata[3] == 1.0F && kdata[4] == 1.0F && kdata[5] == 1.0F &&
		kdata[6] == 1.0F && kdata[7] == 1.0F && kdata[8] == 1.0F);
    }
}
