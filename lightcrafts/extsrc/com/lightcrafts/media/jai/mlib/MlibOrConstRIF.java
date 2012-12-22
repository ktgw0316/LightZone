/*
 * $RCSfile: MlibOrConstRIF.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:02 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.mlib;
import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.DataBuffer;
import java.awt.image.SampleModel;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderedImageFactory;
import com.lightcrafts.mediax.jai.ImageLayout;

import com.lightcrafts.media.jai.opimage.RIFUtil;

/**
 * A <code>RIF</code> supporting the "OrConst" operation in the
 * rendered image mode using MediaLib.
 *
 * @see com.lightcrafts.mediax.jai.operator.OrConstDescriptor
 * @see MlibOrConstOpImage
 *
 */
public class MlibOrConstRIF implements RenderedImageFactory {

    /** Constructor. */
    public MlibOrConstRIF() {}

    /**
     * Creates a new instance of <code>MlibOrConstOpImage</code> in
     * the rendered image mode.
     *
     * @param args  The source image and the constants.
     * @param hints  May contain rendering hints and destination image layout.
     */
    public RenderedImage create(ParameterBlock args,
                                RenderingHints hints) {
        ImageLayout layout = RIFUtil.getImageLayoutHint(hints);
        

        if (!MediaLibAccessor.isMediaLibCompatible(args, layout) ||
            !MediaLibAccessor.hasSameNumBands(args, layout)) {
            return null;
        }

        /* Check whether dest has data type of float or double. */
        if (layout != null) {
            SampleModel sm = layout.getSampleModel(null);

            if (sm != null) {
                int dtype = sm.getDataType();
                if (dtype == DataBuffer.TYPE_FLOAT ||
                    dtype == DataBuffer.TYPE_DOUBLE) {
                    return null;
                }
            }
        }

        return new MlibOrConstOpImage(args.getRenderedSource(0),
                                      hints, layout,
                                      (int[])args.getObjectParameter(0));
    }
}
