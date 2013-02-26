/*
 * $RCSfile: MlibWarpRIF.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:09 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.mlib;
import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderedImageFactory;
import com.lightcrafts.mediax.jai.BorderExtender;
import com.lightcrafts.mediax.jai.ImageLayout;
import com.lightcrafts.mediax.jai.Interpolation;
import com.lightcrafts.mediax.jai.InterpolationNearest;
import com.lightcrafts.mediax.jai.InterpolationBilinear;
import com.lightcrafts.mediax.jai.InterpolationBicubic;
import com.lightcrafts.mediax.jai.InterpolationBicubic2;
import com.lightcrafts.mediax.jai.InterpolationTable;
import com.lightcrafts.mediax.jai.Warp;
import com.lightcrafts.mediax.jai.WarpGrid;
import com.lightcrafts.mediax.jai.WarpPolynomial;
import com.lightcrafts.media.jai.opimage.RIFUtil;

import com.sun.medialib.mlib.*;

/**
 * A <code>RIF</code> supporting the "Warp" operation in the
 * rendered image mode using MediaLib.
 *
 * @see com.lightcrafts.mediax.jai.operator.WarpDescriptor
 * @see MlibWarpNearestOpImage
 * @see MlibWarpBilinearOpImage
 * @see MlibWarpBicubicOpImage
 *
 * @since 1.0
 *
 */
public class MlibWarpRIF implements RenderedImageFactory {

    /** Constructor. */
    public MlibWarpRIF() {}

    /**
     * Creates a new instance of <code>MlibWarpOpImage</code> in
     * the rendered image mode.
     *
     * @param args  The source images.
     * @param hints  May contain rendering hints and destination image layout.
     */
    public RenderedImage create(ParameterBlock args,
                                RenderingHints hints) {
        /* Get ImageLayout and TileCache from RenderingHints. */
        ImageLayout layout = RIFUtil.getImageLayoutHint(hints);
        
        RenderedImage source = args.getRenderedSource(0);


        if (!MediaLibAccessor.isMediaLibCompatible(args, layout) ||
            !MediaLibAccessor.hasSameNumBands(args, layout) ||
	    // Medialib cannot deal with source image having tiles with any
	    // dimension greater than or equal to 32768
	    source.getTileWidth() >= 32768 || 
	    source.getTileHeight() >= 32768) {
            return null;
        }

        /* Get BorderExtender from hints if any. */
        BorderExtender extender = RIFUtil.getBorderExtenderHint(hints);

        Warp warp = (Warp)args.getObjectParameter(0);
        Interpolation interp = (Interpolation)args.getObjectParameter(1);
        double[] backgroundValues = (double[])args.getObjectParameter(2);

        int filter = -1;
        if (interp instanceof InterpolationNearest)  {
            filter = Constants.MLIB_NEAREST;
        } else if (interp instanceof InterpolationBilinear) {
            filter = Constants.MLIB_BILINEAR;
        } else if (interp instanceof InterpolationBicubic) {
            filter = Constants.MLIB_BICUBIC;
        } else if (interp instanceof InterpolationBicubic2) {
            filter = Constants.MLIB_BICUBIC2;
        } else if (interp instanceof InterpolationTable) {
	    ;
	    // filter =  Constants.MLIB_TABLE; not defined yet;
        } else {
            /* Other kinds of interpolation cannot be handled via mlib. */
            return null;
        }

        if (warp instanceof WarpGrid) {
	  if (interp instanceof InterpolationTable){
            return new MlibWarpGridTableOpImage(source,
                                           extender, hints, layout,
                                           (WarpGrid)warp,
                                           interp,
                                           backgroundValues);
	  }else{
            return new MlibWarpGridOpImage(source,
                                           extender, hints, layout,
                                           (WarpGrid)warp,
                                           interp, filter,
                                           backgroundValues);
	  }

        } else if (warp instanceof WarpPolynomial) {
	  if (interp instanceof InterpolationTable){
            return new MlibWarpPolynomialTableOpImage(source,
                                                 extender, hints, layout,
                                                 (WarpPolynomial)warp,
                                                 interp,
                                                 backgroundValues);
	  }else{
            return new MlibWarpPolynomialOpImage(source,
                                                 extender, hints, layout,
                                                 (WarpPolynomial)warp,
                                                 interp, filter,
                                                 backgroundValues);
	  }
        } else {
            return null;
        }

    }
}
