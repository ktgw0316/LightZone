/*
 * $RCSfile: MlibScaleRIF.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:05 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.mlib;
import java.awt.RenderingHints;
import java.awt.image.DataBuffer;
import java.awt.image.MultiPixelPackedSampleModel;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.renderable.RenderedImageFactory;
import java.awt.image.renderable.ParameterBlock;
import com.lightcrafts.mediax.jai.BorderExtender;
import com.lightcrafts.mediax.jai.ImageLayout;
import com.lightcrafts.mediax.jai.Interpolation;
import com.lightcrafts.mediax.jai.InterpolationNearest;
import com.lightcrafts.mediax.jai.InterpolationBilinear;
import com.lightcrafts.mediax.jai.InterpolationBicubic;
import com.lightcrafts.mediax.jai.InterpolationBicubic2;
import com.lightcrafts.mediax.jai.InterpolationTable;

import com.lightcrafts.media.jai.opimage.RIFUtil;
import com.lightcrafts.media.jai.opimage.TranslateIntOpImage;

/**
 * A <code>RIF</code> supporting the "Scale" operation in the
 * rendered image mode using MediaLib.
 *
 * @see com.lightcrafts.mediax.jai.operator.ScaleDescriptor
 * @see MlibScaleNearestOpImage
 * @see MlibScaleBilinearOpImage
 * @see MlibScaleBicubicOpImage
 *
 */
public class MlibScaleRIF implements RenderedImageFactory {

    private static final float TOLERANCE = 0.01F;

    /** Constructor. */
    public MlibScaleRIF() {}

    /**
     * Creates a new instance of <code>MlibScaleOpImage</code> in
     * the rendered image mode.
     *
     * @param args  The source image, scale factors,
     *              and the <code>Interpolation</code>.
     * @param hints  May contain rendering hints and destination image layout.
     */
    public RenderedImage create(ParameterBlock args,
                                RenderingHints hints) {
        /* Get ImageLayout and TileCache from RenderingHints. */
        ImageLayout layout = RIFUtil.getImageLayoutHint(hints);
        
        Interpolation interp = (Interpolation)args.getObjectParameter(4);

	RenderedImage source = args.getRenderedSource(0);

        if (!MediaLibAccessor.isMediaLibCompatible(args, layout) ||
            !MediaLibAccessor.hasSameNumBands(args, layout) ||
	    // Medialib cannot deal with source image having tiles with any
	    // dimension greater than or equal to 32768
	    source.getTileWidth() >= 32768 || 
	    source.getTileHeight() >= 32768) {
            return null;
        }

        SampleModel sm = source.getSampleModel();
        boolean isBilevel = (sm instanceof MultiPixelPackedSampleModel) &&
            (sm.getSampleSize(0) == 1) &&
            (sm.getDataType() == DataBuffer.TYPE_BYTE || 
             sm.getDataType() == DataBuffer.TYPE_USHORT || 
             sm.getDataType() == DataBuffer.TYPE_INT);
        if (isBilevel) {
            // Let Java code handle it, reformatting is slower
            return null;
        }

        // Get BorderExtender from hints if any.
        BorderExtender extender = RIFUtil.getBorderExtenderHint(hints);

        float xScale = args.getFloatParameter(0);
        float yScale = args.getFloatParameter(1);
        float xTrans = args.getFloatParameter(2);
        float yTrans = args.getFloatParameter(3);

	// Check and see if we are scaling by 1.0 in both x and y and no
        // translations. If so call the copy operation.
	if (xScale == 1.0F && yScale == 1.0F && 
	    xTrans == 0.0F && yTrans == 0.0F) {
	    return new MlibCopyOpImage(source, hints, layout);
	}

	// Check to see whether the operation specified is a pure 
	// integer translation. If so call translate
	if (xScale == 1.0F && yScale == 1.0F &&
	    (Math.abs(xTrans - (int)xTrans) < TOLERANCE) &&
	    (Math.abs(yTrans - (int)yTrans) < TOLERANCE) &&
	    layout == null) { // TranslateIntOpImage can't deal with ImageLayout hint
	    /* It's a integer translate. */
            return new TranslateIntOpImage(source,
					   hints,
					   (int)xTrans,
					   (int)yTrans);
	}

	if (interp instanceof InterpolationNearest)  {
	    return new MlibScaleNearestOpImage(source, extender,
                                               hints, layout,
					       xScale, yScale,
					       xTrans, yTrans,
					       interp);
        } else if (interp instanceof InterpolationBilinear) {
	    return new MlibScaleBilinearOpImage(source,
                                                extender, hints, layout,
						xScale, yScale,
						xTrans, yTrans,
						interp);
        } else if (interp instanceof InterpolationBicubic ||
		   interp instanceof InterpolationBicubic2) {
	    return new MlibScaleBicubicOpImage(source,
                                               extender, hints, layout,
					       xScale, yScale,
					       xTrans, yTrans,
					       interp);
        } else if (interp instanceof InterpolationTable) {
	    return new MlibScaleTableOpImage(source, extender, hints, layout,
					     xScale, yScale,
					     xTrans, yTrans,
					     interp);
	} else {
	    /* Other kinds of interpolation cannot be handled via mlib. */
	    return null;
        }
    }
}
