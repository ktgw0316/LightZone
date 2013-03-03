/*
 * $RCSfile: MlibBandCombineRIF.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:55:50 $
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
 * A <code>RIF</code> supporting the "BandCombine" operation in the
 * rendered image mode using MediaLib.
 *
 * @see com.lightcrafts.mediax.jai.operator.BandCombineDescriptor
 * @see MlibBandCombineOpImage
 *
 */
public class MlibBandCombineRIF implements RenderedImageFactory {

    /** Constructor. */
    public MlibBandCombineRIF() {}

    /**
     * Creates a new instance of <code>MlibBandCombineOpImage</code> in
     * the rendered image mode.
     *
     * @param args  The source image and the matrix.
     * @param hints  May contain rendering hints and destination image layout.
     */
    public RenderedImage create(ParameterBlock args,
                                RenderingHints hints) {
        // Get ImageLayout and TileCache from RenderingHints.
        ImageLayout layout = RIFUtil.getImageLayoutHint(hints);
        

        if (!MediaLibAccessor.isMediaLibCompatible(args, layout)) {
            return null;
        }

        // Fall back to Java code if the matrix is not 3-by-4.
        double[][] matrix = (double[][])args.getObjectParameter(0);
        if(matrix.length != 3) {
            return null;
        }
        for(int i = 0; i < 3; i++) {
            if(matrix[i].length != 4) {
                return null;
            }
        }

	return new MlibBandCombineOpImage(args.getRenderedSource(0),
                                          hints, layout,
                                          matrix);
    }
}
