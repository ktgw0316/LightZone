/*
 * $RCSfile: MlibDivideByConstRIF.java,v $
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
import com.lightcrafts.mediax.jai.ImageLayout;

import com.lightcrafts.media.jai.opimage.RIFUtil;

/**
 * A <code>RIF</code> supporting the "DivideByConst" operation in the
 * rendered image mode using MediaLib.
 *
 * @see com.lightcrafts.mediax.jai.operator.DivideByConstDescriptor
 * @see MlibMultiplyConstOpImage
 *
 */
public class MlibDivideByConstRIF implements RenderedImageFactory {

    /** Constructor. */
    public MlibDivideByConstRIF() {}

    /**
     * Creates a new instance of <code>MlibMultiplyConstOpImage</code> in
     * the rendered image mode.  By inverting the constants, the result
     * of "DivideByConst" is obtained.
     *
     * @param args  The source image and the constants.
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

        /* Negate the constants vector. */
        double[] constants = (double[])args.getObjectParameter(0);
        int length = constants.length;

        double[] invConstants = new double[length];

        for (int i = 0; i < length; i++) {
            invConstants[i] = 1.0D / constants[i];
        }

	return new MlibMultiplyConstOpImage(args.getRenderedSource(0),
                                            hints, layout,
                                            invConstants);
    }
}
