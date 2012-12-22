/*
 * $RCSfile: ImageFunctionRIF.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:29 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.opimage;
import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.RenderedImageFactory;
import java.awt.image.renderable.ParameterBlock;
import com.lightcrafts.mediax.jai.ImageFunction;
import com.lightcrafts.mediax.jai.ImageLayout;

/**
 * A <code>RIF</code> supporting the "ImageFunction" operation in the rendered
 * image layer.
 *
 * @see com.lightcrafts.mediax.jai.operator.ImageFunctionDescriptor
 * @see com.lightcrafts.mediax.jai.ImageFunction
 * @since EA4
 */
public class ImageFunctionRIF implements RenderedImageFactory {

    /** Constructor. */
    public ImageFunctionRIF() {}

    /**
     * Creates a new instance of ImageFunctionOpImage in the rendered layer.
     * This method satisfies the implementation of RIF.
     *
     * @param paramBlock  The source image, the X and Y scale factor,
     *                    and the interpolation method for resampling.
     */
    public RenderedImage create(ParameterBlock paramBlock,
                                RenderingHints renderHints) {
        // Get ImageLayout from renderHints if any.
        ImageLayout layout = RIFUtil.getImageLayoutHint(renderHints);
        

        ImageFunction function =
            (ImageFunction)paramBlock.getObjectParameter(0);

        // Ascertain that a supplied SampleModel has the requisite
        // number of bands vis-a-vis the ImageFunction.
        int numBandsRequired = function.isComplex() ?
            function.getNumElements() * 2 : function.getNumElements();
        if(layout != null &&
           layout.isValid(ImageLayout.SAMPLE_MODEL_MASK) &&
           layout.getSampleModel(null).getNumBands() != numBandsRequired ) {
            throw new RuntimeException(JaiI18N.getString("ImageFunctionRIF0"));
        }

        int minX = 0;
        int minY = 0;
        if (layout != null) {
            if (layout.isValid(ImageLayout.MIN_X_MASK)) {
                minX = layout.getMinX(null);
            }
            if (layout.isValid(ImageLayout.MIN_Y_MASK)) {
                minY = layout.getMinX(null);
            }
        }

        int width = paramBlock.getIntParameter(1);
        int height = paramBlock.getIntParameter(2);
        float xScale = paramBlock.getFloatParameter(3);
        float yScale = paramBlock.getFloatParameter(4);
        float xTrans = paramBlock.getFloatParameter(5);
        float yTrans = paramBlock.getFloatParameter(6);

        return new ImageFunctionOpImage(function,
                                        minX, minY,
                                        width, height,
                                        xScale, yScale,
                                        xTrans, yTrans,
                                        renderHints, layout);
    }
}
