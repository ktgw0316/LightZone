/*
 * $RCSfile: ColorQuantizerRIF.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:17 $
 * $State: Exp $
 */package com.lightcrafts.media.jai.opimage;
import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.renderable.RenderedImageFactory;
import java.awt.image.renderable.ParameterBlock;
import java.util.Map;
import com.lightcrafts.mediax.jai.ImageLayout;
import com.lightcrafts.mediax.jai.JAI;
import com.lightcrafts.mediax.jai.ROI;
import com.lightcrafts.mediax.jai.operator.ColorQuantizerDescriptor;
import com.lightcrafts.mediax.jai.operator.ColorQuantizerType;

/**
 * <p> Class implementing the RIF interface for the ColorQuantizer
 * operator.  An instance of this class should be registered with the
 * OperationRegistry with operation name "ColorQuantizer."
 */
public class ColorQuantizerRIF implements RenderedImageFactory {

    /** <p> Default constructor (there is no input). */
    public ColorQuantizerRIF() {}

    /**
     * <p> Creates a new instance of ColorQuantizerOpImage in the
     * rendered layer.  This method satisfies the implementation of RIF.
     *
     * @param paramBlock  The source image, the color quantization algorithm
     *                    name, the maximum number of colors, the
     *                    parameter for training (the histogram size for
     *                    median-cut, the cycle for neuquant, and maximum tree
     *                    size for oct-tree), and the ROI.
     * @param renderHints RenderingHints.
     */
    public RenderedImage create(ParameterBlock paramBlock,
                                RenderingHints renderHints) {
        RenderedImage source = paramBlock.getRenderedSource(0);

        ImageLayout layout = renderHints == null ? null :
            (ImageLayout)renderHints.get(JAI.KEY_IMAGE_LAYOUT);

        ColorQuantizerType algorithm =
            (ColorQuantizerType)paramBlock.getObjectParameter(0);
        int maxColorNum = paramBlock.getIntParameter(1);
        int upperBound = paramBlock.getIntParameter(2);
        ROI roi= (ROI)paramBlock.getObjectParameter(3);
        int xPeriod = paramBlock.getIntParameter(4);
        int yPeriod = paramBlock.getIntParameter(5);

        // check if 3-band byte-type image
	SampleModel sm = source.getSampleModel();
        if (sm.getNumBands() != 3  && sm.getDataType() == DataBuffer.TYPE_BYTE)
            throw new IllegalArgumentException("ColorQuantizerRIF0");

        if (algorithm.equals(ColorQuantizerDescriptor.NEUQUANT))
            return new NeuQuantOpImage(source, (Map)renderHints, layout,
                                        maxColorNum, upperBound, roi,
                                        xPeriod, yPeriod);

        if (algorithm.equals(ColorQuantizerDescriptor.OCTTREE))
            return new OctTreeOpImage(source, (Map)renderHints, layout,
                                        maxColorNum, upperBound, roi,
                                        xPeriod, yPeriod);
        else
            return new MedianCutOpImage(source, (Map)renderHints, layout,
                                        maxColorNum, upperBound, roi,
                                        xPeriod, yPeriod);

    } // create

} // ColorQuantizerRIF
