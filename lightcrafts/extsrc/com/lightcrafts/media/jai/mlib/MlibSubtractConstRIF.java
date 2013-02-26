/*
 * $RCSfile: MlibSubtractConstRIF.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:07 $
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
 * A <code>RIF</code> supporting the "SubtractConst" operation in the
 * rendered image mode using MediaLib.
 *
 * @see com.lightcrafts.mediax.jai.operator.SubtractConstDescriptor
 * @see MlibAddConstOpImage
 *
 */
public class MlibSubtractConstRIF implements RenderedImageFactory {

    /** Constructor. */
    public MlibSubtractConstRIF() {}

    /**
     * Creates a new instance of <code>MlibAddConstOpImage</code> in
     * the rendered image mode.  By negating the constants, the result
     * of "SubtractConst" is obtained.
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

        double[] negConstants = new double[length];

        for (int i = 0; i < length; i++) {
            negConstants[i] = -constants[i];
        }

        return new MlibAddConstOpImage(args.getRenderedSource(0),
                                       hints, layout,
                                       negConstants);
    }
}
