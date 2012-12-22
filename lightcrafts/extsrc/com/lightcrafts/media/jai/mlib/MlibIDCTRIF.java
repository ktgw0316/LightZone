/*
 * $RCSfile: MlibIDCTRIF.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:55:57 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.mlib;
import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderedImageFactory;
import com.lightcrafts.mediax.jai.ImageLayout;

import com.lightcrafts.media.jai.opimage.DCTOpImage;
import com.lightcrafts.media.jai.opimage.RIFUtil;

/**
 * A <code>RIF</code> supporting the "IDCT" operation in the
 * rendered image mode using MediaLib.
 *
 * @see com.lightcrafts.mediax.jai.operator.IDCTDescriptor
 * @see com.lightcrafts.media.jai.opimage.DCTOpImage
 *
 * @since EA4
 *
 */
public class MlibIDCTRIF implements RenderedImageFactory {

    /** Constructor. */
    public MlibIDCTRIF() {}

    /**
     * Creates a new instance of <code>IDCTOpImage</code> in
     * the rendered image mode.
     *
     * @param args  The source image.
     * @param hints  May contain rendering hints and destination image layout.
     */
    public RenderedImage create(ParameterBlock args,
                                RenderingHints hints) {
        /* Get ImageLayout and TileCache from RenderingHints. */
        ImageLayout layout = RIFUtil.getImageLayoutHint(hints);
        

        if (!MediaLibAccessor.isMediaLibCompatible(new ParameterBlock())) {
            return null;
        }

        return new DCTOpImage(args.getRenderedSource(0),
                              hints, layout,
                              new FCTmediaLib(false, 2));
    }
}
