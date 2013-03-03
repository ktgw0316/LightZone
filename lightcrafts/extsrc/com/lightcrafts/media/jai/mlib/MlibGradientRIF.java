/*
 * $RCSfile: MlibGradientRIF.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:55:57 $
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
 * A <code>RIF</code> supporting the "Gradient" operation in the
 * rendered image mode using MediaLib.
 *
 * @see com.lightcrafts.mediax.jai.operator.GradientDescriptor
 * @see MlibGradientOpImage
 */
public class MlibGradientRIF implements RenderedImageFactory {

    /** Constructor. */
    public MlibGradientRIF() {}

    /**
     * Creates a new instance of <code>MlibGradientOpImage</code> in
     * the rendered image mode.
     *
     * @param args  The source image and convolution kernel.
     * @param hints  May contain rendering hints and destination image layout.
     */
    public RenderedImage create(ParameterBlock args,
                                RenderingHints hints) {
        /* Get ImageLayout and TileCache from RenderingHints. */
        ImageLayout layout = RIFUtil.getImageLayoutHint(hints);
        

        if (!MediaLibAccessor.isMediaLibCompatible(args, layout) ||
            !MediaLibAccessor.hasSameNumBands(args, layout)) {
            return null;
        }

        /* Get BorderExtender from hints if any. */
        BorderExtender extender = RIFUtil.getBorderExtenderHint(hints);

        RenderedImage source = args.getRenderedSource(0);

        /*
         * Get the Horizontal & Vertical kernels.
         * At this point these kernels should have the same width & height
         */
        KernelJAI kern_h = (KernelJAI)args.getObjectParameter(0);
        KernelJAI kern_v = (KernelJAI)args.getObjectParameter(1);

        /* Get the width & height of the kernels. */
        int kWidth = kern_h.getWidth();
        int kHeight = kern_v.getHeight();

        /* Check and see if the operation is a Sobel. */
        float khdata[], kvdata[];
        khdata = kern_h.getKernelData();
        kvdata = kern_v.getKernelData();
        if ((khdata[0] == -1.0F && khdata[1] == -2.0F && khdata[2] == -1.0F &&
             khdata[3] ==  0.0F && khdata[4] ==  0.0F && khdata[5] ==  0.0F &&
             khdata[6] ==  1.0F && khdata[7] ==  2.0F && khdata[8] ==  1.0F) &&
            (kvdata[0] == -1.0F && kvdata[1] == 0.0F && kvdata[2] == 1.0F &&
             kvdata[3] == -2.0F && kvdata[4] == 0.0F && kvdata[5] == 2.0F &&
             kvdata[6] == -1.0F && kvdata[7] == 0.0F && kvdata[8] == 1.0F) &&
            kWidth == 3 && kHeight == 3) {
            return new MlibSobelOpImage(source,
                                        extender, hints, layout,
                                        kern_h);    
        }

        /* Call the Generic version. */
        return new MlibGradientOpImage(source,
                                       extender, hints, layout,
                                       kern_h,
                                       kern_v);
    }
}
