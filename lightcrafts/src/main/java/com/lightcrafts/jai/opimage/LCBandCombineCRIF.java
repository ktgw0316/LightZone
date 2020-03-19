/* Copyright (C) 2005-2011 Fabio Riccardi */

/*
 * $RCSfile: BandCombineCRIF.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:14 $
 * $State: Exp $
 */
package com.lightcrafts.jai.opimage;
import com.sun.media.jai.opimage.RIFUtil;

import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import javax.media.jai.CRIFImpl;
import javax.media.jai.ImageLayout;

/**
 * A <code>CRIF</code> supporting the "BandCombine" operation in the
 * rendered and renderable image layers.
 *
 * @see javax.media.jai.operator.BandCombineDescriptor
 * @see LCBandCombineOpImage
 *
 *
 * @since EA3
 */
public class LCBandCombineCRIF extends CRIFImpl {

    /** Constructor. */
    public LCBandCombineCRIF() {
        super("LCBandCombine");
    }

    /**
     * Creates a new instance of <code>LCBandCombineOpImage</code>
     * in the rendered layer.
     *
     * @param args   The source image and the constants.
     * @param hints  Optionally contains destination image layout.
     */
    public RenderedImage create(ParameterBlock args,
                                RenderingHints hints) {
        // Get ImageLayout from renderHints if any.
        ImageLayout layout = RIFUtil.getImageLayoutHint(hints);


        return new LCBandCombineOpImage(args.getRenderedSource(0),
                                        hints,
                                        layout,
                                        (double[][])args.getObjectParameter(0));
    }
}
