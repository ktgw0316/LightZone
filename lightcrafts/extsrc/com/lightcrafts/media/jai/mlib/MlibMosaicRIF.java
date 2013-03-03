/*
 * $RCSfile: MlibMosaicRIF.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:01 $
 * $State: Exp $
 */package com.lightcrafts.media.jai.mlib;

import java.awt.RenderingHints;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderedImageFactory;
import java.util.Vector;
import com.lightcrafts.mediax.jai.ImageLayout;
import com.lightcrafts.mediax.jai.PlanarImage;
import com.lightcrafts.mediax.jai.ROI;
import com.lightcrafts.mediax.jai.operator.MosaicType;
import com.lightcrafts.media.jai.opimage.RIFUtil;

/**
 * A <code>RIF</code> supporting the "Mosaic" operation in the rendered
 * image layer.
 *
 * @since JAI 1.1.2
 * @see com.lightcrafts.mediax.jai.operator.MosaicDescriptor
 */
public class MlibMosaicRIF implements RenderedImageFactory {

    /** Constructor. */
    public MlibMosaicRIF() {}

    /**
     * Renders a "Mosaic" operation node.
     */
    public RenderedImage create(ParameterBlock paramBlock,
                                RenderingHints renderHints) {
        // Get ImageLayout from renderHints if any.
        ImageLayout layout = RIFUtil.getImageLayoutHint(renderHints);

        // Return if not mediaLib-compatible.
        if(!MediaLibAccessor.isMediaLibCompatible(paramBlock, layout) ||
           !MediaLibAccessor.hasSameNumBands(paramBlock, layout)) {
            return null;
        }

        // Get sources.
        Vector sources = paramBlock.getSources();

        // Get target SampleModel.
        SampleModel targetSM = null;
        if(sources.size() > 0) {
            targetSM = ((RenderedImage)sources.get(0)).getSampleModel();
        } else if(layout != null &&
                  layout.isValid(ImageLayout.SAMPLE_MODEL_MASK)) {
            targetSM = layout.getSampleModel(null);
        }

        if(targetSM != null) {
            // Return if target data type is floating point. Other more
            // extensive type checking is done in MosaicOpImage constructor.
            int dataType = targetSM.getDataType();
            if(dataType == DataBuffer.TYPE_FLOAT ||
               dataType == DataBuffer.TYPE_DOUBLE) {
                return null;
            }
        }

        return
            new MlibMosaicOpImage(sources,
                                  layout,
                                  renderHints,
                                  (MosaicType)paramBlock.getObjectParameter(0),
                                  (PlanarImage[])paramBlock.getObjectParameter(1),
                                  (ROI[])paramBlock.getObjectParameter(2),
                                  (double[][])paramBlock.getObjectParameter(3),
                                  (double[])paramBlock.getObjectParameter(4));
    }
}
