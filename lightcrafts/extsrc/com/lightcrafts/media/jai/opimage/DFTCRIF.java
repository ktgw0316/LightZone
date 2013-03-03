/*
 * $RCSfile: DFTCRIF.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:22 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.opimage;
import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import com.lightcrafts.mediax.jai.CRIFImpl;
import com.lightcrafts.mediax.jai.EnumeratedParameter;
import com.lightcrafts.mediax.jai.ImageLayout;

/**
 * A <code>CRIF</code> supporting the "DFT" operation in the rendered
 * image layer.
 *
 * @since Beta
 * @see com.lightcrafts.mediax.jai.operator.DFTDescriptor
 *
 */
public class DFTCRIF extends CRIFImpl {

    /** Constructor. */
    public DFTCRIF() {
        super("dft");
    }

    /**
     * Creates a new instance of a DFT operator according to the scaling type.
     *
     * @param paramBlock The scaling type.
     */
    public RenderedImage create(ParameterBlock paramBlock,
                                RenderingHints renderHints) {
        // Get ImageLayout from renderHints if any.
        ImageLayout layout = RIFUtil.getImageLayoutHint(renderHints);
        
        
        RenderedImage source = paramBlock.getRenderedSource(0);
        EnumeratedParameter scalingType =
            (EnumeratedParameter)paramBlock.getObjectParameter(0);
        EnumeratedParameter dataNature =
            (EnumeratedParameter)paramBlock.getObjectParameter(1);

        FFT fft = new FFT(true, new Integer(scalingType.getValue()), 2);

        return new DFTOpImage(source, renderHints, layout, dataNature, fft);
    }
}
