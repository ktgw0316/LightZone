/*
 * $RCSfile: MlibBandSelectRIF.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:55:51 $
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
 * A <code>RIF</code> supporting the "BandSelect" operation in the
 * rendered image mode using MediaLib.
 *
 * @see com.lightcrafts.mediax.jai.operator.BandSelectDescriptor
 * @see MlibBandSelectOpImage
 *
 */
public class MlibBandSelectRIF implements RenderedImageFactory {

    /** Constructor. */
    public MlibBandSelectRIF() {}

    /**
     * Creates a new instance of <code>MlibBandSelectOpImage</code> in
     * the rendered image mode.
     *
     * @param args  The source image and the band indices.
     * @param hints  May contain rendering hints and destination image layout.
     */
    public RenderedImage create(ParameterBlock args,
                                RenderingHints hints) {
        // Get ImageLayout and TileCache from RenderingHints.
        ImageLayout layout = RIFUtil.getImageLayoutHint(hints);
        

        if (!MediaLibAccessor.isMediaLibCompatible(args, layout)) {
            return null;
        }

        int[] bandIndices = (int[])args.getObjectParameter(0);

        // If the band selection is not monotonically increasing
        // fall back to Java code as mediaLib does not support this.
        for(int i = 1; i < bandIndices.length; i++) {
            if(bandIndices[i] <= bandIndices[i-1]) {
                return null;
            }
        }

	return new MlibBandSelectOpImage(args.getRenderedSource(0),
                                         hints, layout,
                                         bandIndices);
    }
}
