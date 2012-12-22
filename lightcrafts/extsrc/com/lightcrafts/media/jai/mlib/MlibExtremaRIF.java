/*
 * $RCSfile: MlibExtremaRIF.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:55:56 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.mlib;
import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderedImageFactory;
import com.lightcrafts.mediax.jai.ROI;

/**
 * A <code>RIF</code> supporting the "Extrema" operation in the
 * rendered image mode using MediaLib.
 *
 * @see com.lightcrafts.mediax.jai.operator.ExtremaDescriptor
 * @see MlibExtremaOpImage
 *
 * @since EA4
 *
 */
public class MlibExtremaRIF implements RenderedImageFactory {

    /** Constructor. */
    public MlibExtremaRIF() {}

    /**
     * Creates a new instance of <code>MlibExtremaOpImage</code> in
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
	boolean saveLocations = ((Boolean)args.getObjectParameter(3)).booleanValue();
	int maxRuns = args.getIntParameter(4);
	
        int xStart = source.getMinX();	// default values
        int yStart = source.getMinY();

        int maxWidth = source.getWidth();
        int maxHeight = source.getHeight();

	if (roi != null &&
	    !roi.contains(xStart, yStart, maxWidth, maxHeight)) {
	    return null;
	}

        return new MlibExtremaOpImage(source,
				      roi,
				      xStart, yStart,
				      xPeriod, yPeriod,
				      saveLocations, maxRuns);
    }
}
