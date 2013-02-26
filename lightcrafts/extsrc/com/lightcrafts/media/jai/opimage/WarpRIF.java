/*
 * $RCSfile: WarpRIF.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:47 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.opimage;
import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderedImageFactory;
import com.lightcrafts.mediax.jai.BorderExtender;
import com.lightcrafts.mediax.jai.ImageLayout;
import com.lightcrafts.mediax.jai.Interpolation;
import com.lightcrafts.mediax.jai.InterpolationNearest;
import com.lightcrafts.mediax.jai.InterpolationBilinear;
import com.lightcrafts.mediax.jai.Warp;

/**
 * A <code>RIF</code> supporting the "Warp" operation in the rendered
 * image layer.
 *
 * @since EA2
 * @see com.lightcrafts.mediax.jai.operator.WarpDescriptor
 * @see GeneralWarpOpImage
 *
 */
public class WarpRIF implements RenderedImageFactory {

    /** Constructor. */
    public WarpRIF() {}

    /**
     * Creates a new instance of warp operator according to the warp object
     * and interpolation method.
     *
     * @param paramBlock  The warp and interpolation objects.
     */
    public RenderedImage create(ParameterBlock paramBlock,
                                RenderingHints renderHints) {
        // Get ImageLayout from renderHints if any.
        ImageLayout layout = RIFUtil.getImageLayoutHint(renderHints);


        // Get BorderExtender from renderHints if any.
        BorderExtender extender = RIFUtil.getBorderExtenderHint(renderHints);

        RenderedImage source = paramBlock.getRenderedSource(0);
        Warp warp = (Warp)paramBlock.getObjectParameter(0);
        Interpolation interp = (Interpolation)paramBlock.getObjectParameter(1);

        double[] backgroundValues = (double[])paramBlock.getObjectParameter(2);

        if (interp instanceof InterpolationNearest) {
            return new WarpNearestOpImage(source,
                                          renderHints,
                                          layout,
                                          warp,
                                          interp,
                                          backgroundValues);
        } else if (interp instanceof InterpolationBilinear) {
            return new WarpBilinearOpImage(source, extender, renderHints,
                                           layout, warp, interp,
                                           backgroundValues);
        } else {
            return new WarpGeneralOpImage(source, extender, renderHints,
                                          layout, warp, interp,
                                          backgroundValues);
        }
    }
}
