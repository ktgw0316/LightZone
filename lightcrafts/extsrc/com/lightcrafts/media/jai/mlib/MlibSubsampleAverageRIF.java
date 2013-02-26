/*
 * $RCSfile: MlibSubsampleAverageRIF.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:06 $
 * $State: Exp $
 */package com.lightcrafts.media.jai.mlib;

import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.RenderedImageFactory;
import java.awt.image.renderable.ParameterBlock;
import com.lightcrafts.mediax.jai.ImageLayout;
import com.lightcrafts.media.jai.opimage.RIFUtil;

/**
 * A <code>RIF</code> supporting the "SubsampleAverage" operation in the
 * rendered image mode using MediaLib.
 *
 * @see com.lightcrafts.mediax.jai.operator.SubsampleaverageDescriptor
 * @see MlibSubsampleAverageOpImage

 */
public class MlibSubsampleAverageRIF implements RenderedImageFactory {

    /** Constructor. */
    public MlibSubsampleAverageRIF() {}

    /**
     * Creates a new instance of <code>MlibSubsampleAverageOpImage</code> in
     * the rendered image mode.
     *
     * @param args  The source image, scale factors,
     *              and the <code>Interpolation</code>.
     * @param hints  May contain rendering hints and destination image layout.
     */
    public RenderedImage create(ParameterBlock args,
                                RenderingHints hints) {
        // Get the scale factors.
        double scaleX = args.getDoubleParameter(0);
        double scaleY = args.getDoubleParameter(1);

        // If unity scaling return the source directly.
        if(scaleX == 1.0 && scaleY == 1.0) {
            return args.getRenderedSource(0);
        }

        // Get the layout.
        ImageLayout layout = RIFUtil.getImageLayoutHint(hints);

        // Check mediaLib compatibility.
        if (!MediaLibAccessor.isMediaLibCompatible(args, layout) ||
            !MediaLibAccessor.hasSameNumBands(args, layout)) {
            // Return null to indicate to fallback to next RIF.
            return null;
        }

        // Create and return the OpImage.
        return new MlibSubsampleAverageOpImage(args.getRenderedSource(0),
                                               layout, hints,
                                               scaleX, scaleY);
    }
}
