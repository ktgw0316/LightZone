/*
 * $RCSfile: MlibSubsampleBinaryToGrayRIF.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:06 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.mlib;
import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.RenderedImageFactory;
import java.awt.image.renderable.ParameterBlock;
import com.lightcrafts.mediax.jai.ImageLayout;

import com.lightcrafts.media.jai.opimage.CopyOpImage;
import com.lightcrafts.media.jai.opimage.RIFUtil;

/**
 * A <code>RIF</code> supporting the "SubsampleBinaryToGray" operation in the
 * rendered image mode using MediaLib.
 *
 * @see com.lightcrafts.mediax.jai.operator.SubsampleBinaryToGrayDescriptor
 */
public class MlibSubsampleBinaryToGrayRIF implements RenderedImageFactory {

    /**
     * The width and height of blocks to be condensed into one gray pixel.
     * They are expected to be computed in the same way as in 
     * import com.lightcrafts.media.jai.opimage.SubsampleBinaryToGrayOpImage;
     */
    private int blockX;
    private int blockY;

    /** Constructor. */
    public MlibSubsampleBinaryToGrayRIF() {}

    /**
     * Creates a new instance of <code>MlibSubsampleBinaryToGrayOpImage</code> in
     * the rendered image mode.
     *
     * @param args  The source image, scale factors,
     *              and the <code>Interpolation</code>.
     * @param hints  May contain rendering hints and destination image layout.
     */
    public RenderedImage create(ParameterBlock args,
                                RenderingHints hints) {
	RenderedImage source = args.getRenderedSource(0);

        // Verify that the source is mediaLib-compatible.
        if (!MediaLibAccessor.isMediaLibBinaryCompatible(args, null)) {
            return null;
        }

        // Get ImageLayout from RenderingHints.
        ImageLayout layout = RIFUtil.getImageLayoutHint(hints);

        // Verify that the destination is mediaLib-compatible and has
        // the same number of bands as the source.
        if ((layout != null &&
             layout.isValid(ImageLayout.SAMPLE_MODEL_MASK) &&
             !MediaLibAccessor.isMediaLibCompatible(
                  layout.getSampleModel(null),
                  layout.getColorModel(null))) ||
            !MediaLibAccessor.hasSameNumBands(args, layout)) {
            return null;
        }

        // Get BorderExtender from hints if any.
	// BorderExtender extender = RIFUtil.getBorderExtenderHint(hints);

        float xScale = args.getFloatParameter(0);
        float yScale = args.getFloatParameter(1);
 
	// When scaling by 1.0 in both x and y, a copy is all we need
	if (xScale == 1.0F && yScale == 1.0F){
            // Use CopyOpImage as MlibCopyOpImage doesn't handle
            // binary-to-gray case.
	    return new CopyOpImage(source, hints, layout);
	}

	return new MlibSubsampleBinaryToGrayOpImage(source,
						    layout,
						    hints,
						    xScale,
						    yScale);

    }
}
