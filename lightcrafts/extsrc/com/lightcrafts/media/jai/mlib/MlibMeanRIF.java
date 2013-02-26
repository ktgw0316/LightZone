/*
 * $RCSfile: MlibMeanRIF.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:55:59 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.mlib;
import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderedImageFactory;
import com.lightcrafts.mediax.jai.ROI;

/**
 * A <code>RIF</code> supporting the "Mean" operation in the
 * rendered image mode using MediaLib.
 *
 * @see com.lightcrafts.mediax.jai.operator.MeanDescriptor
 * @see MlibMeanOpImage
 *
 * @since EA3
 *
 */
public class MlibMeanRIF implements RenderedImageFactory {

    /** Constructor. */
    public MlibMeanRIF() {}

    /**
     * Creates a new instance of <code>MlibMeanOpImage</code> in
     * the rendered image mode.
     *
     * @param args  The source image and the parameters.
     * @param hints  Rendering hints are ignored.
     */
    public RenderedImage create(ParameterBlock args,
                                RenderingHints hints) {
        if (!MediaLibAccessor.isMediaLibCompatible(args)) {
            return null;
        }

	RenderedImage source = args.getRenderedSource(0);
	ROI roi = (ROI)args.getObjectParameter(0);
        int xPeriod = args.getIntParameter(1);
        int yPeriod = args.getIntParameter(2);
	
        int xStart = source.getMinX();	// default values
        int yStart = source.getMinY();

        int maxWidth = source.getWidth();
        int maxHeight = source.getHeight();

        if (roi != null &&
            !roi.contains(xStart, yStart, maxWidth, maxHeight)) {
            return null;
        }

	// mediaLib supports only a sampling period of 1
	if ((xPeriod != 1) || (yPeriod != 1)) {
	    return null;
	}

        return new MlibMeanOpImage(source,
				   roi,
				   xStart, yStart,
				   xPeriod, yPeriod);
    }
}
