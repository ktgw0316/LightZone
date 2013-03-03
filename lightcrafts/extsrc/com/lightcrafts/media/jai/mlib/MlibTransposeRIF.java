/*
 * $RCSfile: MlibTransposeRIF.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:08 $
 * $State: Exp $
 */ 
package com.lightcrafts.media.jai.mlib;
import java.awt.RenderingHints;
import java.awt.image.DataBuffer;
import java.awt.image.MultiPixelPackedSampleModel;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderedImageFactory;
import com.lightcrafts.mediax.jai.EnumeratedParameter;
import com.lightcrafts.mediax.jai.ImageLayout;

import com.lightcrafts.media.jai.opimage.RIFUtil;

/**
 * A <code>RIF</code> supporting the "Transpose" operation in the
 * rendered image mode using MediaLib.
 *
 * @see com.lightcrafts.mediax.jai.operator.TransposeDescriptor
 * @see MlibTransposeOpImage 
 *
 * @since EA3
 */
public class MlibTransposeRIF implements RenderedImageFactory {

    /** Constructor. */
    public MlibTransposeRIF() {}

    /**
     * Creates a new instance of <code>MlibTransposeOpImage</code> in
     * the rendered image mode.
     *
     * @param args  The source image and the transpose type.
     * @param hints  May contain rendering hints and destination image layout.
     */
    public RenderedImage create(ParameterBlock args,
                                RenderingHints hints) {
        /* Get ImageLayout and TileCache from RenderingHints. */
        ImageLayout layout = RIFUtil.getImageLayoutHint(hints);
        

        if (!MediaLibAccessor.isMediaLibCompatible(args, layout) ||
            !MediaLibAccessor.hasSameNumBands(args, layout)) {
            return null;
        }

        RenderedImage source = args.getRenderedSource(0);

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

        EnumeratedParameter transposeType =
            (EnumeratedParameter)args.getObjectParameter(0);
        return new MlibTransposeOpImage(args.getRenderedSource(0),
                                        hints, layout,
                                        transposeType.getValue());
    }
}
