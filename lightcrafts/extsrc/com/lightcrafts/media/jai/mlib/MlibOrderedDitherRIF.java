/*
 * $RCSfile: MlibOrderedDitherRIF.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:03 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.mlib;
import java.awt.RenderingHints;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderedImageFactory;
import com.lightcrafts.mediax.jai.ColorCube;
import com.lightcrafts.mediax.jai.ImageLayout;
import com.lightcrafts.mediax.jai.KernelJAI;

import com.lightcrafts.media.jai.opimage.RIFUtil;
import com.lightcrafts.media.jai.util.ImageUtil;

/**
 * A <code>RIF</code> supporting the "OrderedDither" operation in the
 * rendered image mode using mediaLib.
 *
 * @see com.lightcrafts.mediax.jai.operator.OrderedDitherDescriptor
 * @see MlibOrderedDitherOpImage
 */
public class MlibOrderedDitherRIF implements RenderedImageFactory {

    /** Constructor. */
    public MlibOrderedDitherRIF() {}

    /**
     * Creates a new instance of <code>MlibOrderedDitherOpImage</code> in
     * the rendered image mode.
     *
     * @param args  The source image and lookup table.
     * @param hints  May contain rendering hints and destination image layout.
     */
    public RenderedImage create(ParameterBlock args,
                                RenderingHints hints) {
        // Get source and parameters.
        RenderedImage source = args.getRenderedSource(0);
        ColorCube colorMap =
            (ColorCube)args.getObjectParameter(0);
        KernelJAI[] ditherMask = (KernelJAI[])args.getObjectParameter(1);

        // Check colorMap compatibility.
        if(colorMap.getNumBands() != 1 &&
           colorMap.getNumBands() != 3) {
            // 1 or 3 band colorMaps only.
            return null;
        } else if(colorMap.getDataType() != DataBuffer.TYPE_BYTE) {
            // byte colorMaps only
            return null;
        }

        // Check source compatibility.
        SampleModel sourceSM = source.getSampleModel();
        if(sourceSM.getDataType() != DataBuffer.TYPE_BYTE) {
            // byte source images only
            return null;
        } else if(sourceSM.getNumBands() != colorMap.getNumBands()) {
            // band counts must match
            return null;
        }

        // Get ImageLayout from RenderingHints if any.
        ImageLayout layoutHint = RIFUtil.getImageLayoutHint(hints);

        // Calculate the final ImageLayout.
        ImageLayout layout = 
            MlibOrderedDitherOpImage.layoutHelper(layoutHint,
                                                  source, colorMap);

        // Check for source and destination compatibility. The ColorModel
        // is suppressed in the second test because it will be an
        // IndexColorModel which would cause the test to fail.
        SampleModel destSM = layout.getSampleModel(null);
        if (!MediaLibAccessor.isMediaLibCompatible(args) ||
            (!MediaLibAccessor.isMediaLibCompatible(destSM, null) &&
             !ImageUtil.isBinary(destSM))) {
            return null;
        }

        return new MlibOrderedDitherOpImage(source, hints, layout,
                                            colorMap, ditherMask);
    }
}
