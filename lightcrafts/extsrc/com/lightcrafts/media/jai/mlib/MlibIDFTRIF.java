/*
 * $RCSfile: MlibIDFTRIF.java,v $
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
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderedImageFactory;
import com.lightcrafts.mediax.jai.BorderExtender;
import com.lightcrafts.mediax.jai.EnumeratedParameter;
import com.lightcrafts.mediax.jai.ImageLayout;
import com.lightcrafts.mediax.jai.JAI;
import com.lightcrafts.mediax.jai.operator.DFTDescriptor;
import com.lightcrafts.media.jai.opimage.DFTOpImage;
import com.lightcrafts.media.jai.opimage.FFT;
import com.lightcrafts.media.jai.opimage.RIFUtil;
import com.lightcrafts.media.jai.util.MathJAI;

/**
 * A <code>RIF</code> supporting the "IDFT" operation in the
 * rendered image mode using MediaLib.
 *
 * @see com.lightcrafts.mediax.jai.operator.DFTDescriptor
 * @see com.lightcrafts.mediax.jai.operator.IDFTDescriptor
 * @see com.lightcrafts.media.jai.opimage.DFTOpImage
 *
 * @since EA4
 *
 */
public class MlibIDFTRIF implements RenderedImageFactory {

    /** Constructor. */
    public MlibIDFTRIF() {}

    /**
     * Creates a new instance of <code>DFTOpImage</code> in
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

        RenderedImage source = args.getRenderedSource(0);
        EnumeratedParameter scalingType =
            (EnumeratedParameter)args.getObjectParameter(0);
        EnumeratedParameter dataNature =
            (EnumeratedParameter)args.getObjectParameter(1);

        boolean isComplexSource =
            !dataNature.equals(DFTDescriptor.REAL_TO_COMPLEX);
        int numSourceBands = source.getSampleModel().getNumBands();

        // Use the two-dimensional mediaLib DFT if possible: it supports
        // only data which have a single component (real or complex)
        // per pixel and which have dimensions which are equal to a positive
        // power of 2.
        if(((isComplexSource && numSourceBands == 2) ||
            (!isComplexSource && numSourceBands == 1)) &&
           MlibDFTOpImage.isAcceptableSampleModel(source.getSampleModel())) {
            // If necessary, pad the source to ensure that
            // both dimensions are positive powers of 2.
            int sourceWidth = source.getWidth();
            int sourceHeight = source.getHeight();
            if(!MathJAI.isPositivePowerOf2(sourceWidth) ||
               !MathJAI.isPositivePowerOf2(sourceHeight)) {
                ParameterBlock pb = new ParameterBlock();
                pb.addSource(source);
                pb.add(0);
                pb.add(MathJAI.nextPositivePowerOf2(sourceWidth) -
                       sourceWidth);
                pb.add(0);
                pb.add(MathJAI.nextPositivePowerOf2(sourceHeight) -
                       sourceHeight);
                pb.add(BorderExtender.createInstance(BorderExtender.BORDER_ZERO));
                source = JAI.create("border", pb);
            }

            return new MlibDFTOpImage(source, hints, layout, dataNature,
                                      false, scalingType);
        } else { // General case
            FFT fft = new FFTmediaLib(false,
                                      new Integer(scalingType.getValue()), 2);

            return new DFTOpImage(source, hints, layout, dataNature, fft);
        }
    }
}
