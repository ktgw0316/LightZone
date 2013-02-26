/*
 * $RCSfile: PeriodicShiftCRIF.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:40 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.opimage;
import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import com.lightcrafts.mediax.jai.CRIFImpl;
import com.lightcrafts.mediax.jai.ImageLayout;

/**
 * This image factory supports image operator <code>PeriodicShiftOpImage</code>
 * in the rendered and renderable image layers.
 *
 * @see PeriodicShiftOpImage
 */
public class PeriodicShiftCRIF extends CRIFImpl {

    /** Constructor. */
    public PeriodicShiftCRIF() {
        super("periodicshift");
    }

    /**
     * Creates a new instance of <code>PeriodicShiftOpImage</code>
     * in the rendered layer. This method satisfies the
     * implementation of RIF.
     */
    public RenderedImage create(ParameterBlock paramBlock,
                                RenderingHints renderHints) {
        // Get ImageLayout from renderHints if any.
        ImageLayout layout = RIFUtil.getImageLayoutHint(renderHints);
        
        
        // Get the source image.
        RenderedImage source = paramBlock.getRenderedSource(0);

        // Get the translation parameters.
        int shiftX = paramBlock.getIntParameter(0);
        int shiftY = paramBlock.getIntParameter(1);

        // Return the OpImage.
        return new PeriodicShiftOpImage(source, renderHints, layout, shiftX, shiftY);
    }
}
