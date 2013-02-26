/*
 * $RCSfile: MlibLookupRIF.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:55:58 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.mlib;
import java.awt.RenderingHints;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderedImageFactory;
import com.lightcrafts.mediax.jai.ImageLayout;
import com.lightcrafts.mediax.jai.LookupTableJAI;

import com.lightcrafts.media.jai.opimage.RIFUtil;

/**
 * A <code>RIF</code> supporting the "Lookup" operation in the
 * rendered image mode using MediaLib.
 *
 * @see com.lightcrafts.mediax.jai.operator.LookupDescriptor
 * @see MlibLookupOpImage
 *
 */
public class MlibLookupRIF implements RenderedImageFactory {

    /** Constructor. */
    public MlibLookupRIF() {}

    /**
     * Creates a new instance of <code>MlibLookupOpImage</code> in
     * the rendered image mode.
     *
     * @param args  The source image and lookup table.
     * @param hints  May contain rendering hints and destination image layout.
     */
    public RenderedImage create(ParameterBlock args,
                                RenderingHints hints) {
        /* Get ImageLayout and TileCache from RenderingHints. */
        ImageLayout layout = RIFUtil.getImageLayoutHint(hints);
        

        if (!MediaLibAccessor.isMediaLibCompatible(args)) {
            return null;
        }

        /* The table should be less than or equal to 4 bands. */
        LookupTableJAI table = (LookupTableJAI)args.getObjectParameter(0);
        if (table.getNumBands() > 4 ||
            table.getDataType() == DataBuffer.TYPE_USHORT) {
            return null;
        }

        return new MlibLookupOpImage(args.getRenderedSource(0),
                                     hints, layout,
                                     table);
    }
}
