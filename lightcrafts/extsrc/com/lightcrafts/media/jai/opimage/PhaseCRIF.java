/*
 * $RCSfile: PhaseCRIF.java,v $
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

import com.lightcrafts.media.jai.opimage.MagnitudePhaseOpImage;

/**
 * A <code>CRIF</code> supporting the "Phase" operation in the rendered
 * image layer.
 *
 * @since Beta
 * @see com.lightcrafts.mediax.jai.operator.PhaseDescriptor
 *
 */
public class PhaseCRIF extends CRIFImpl {

    /** Constructor. */
    public PhaseCRIF() {
        super("phase");
    }

    /**
     * Creates a new instance of a Phase operator.
     *
     * @param paramBlock The scaling type.
     */
    public RenderedImage create(ParameterBlock paramBlock,
                                RenderingHints renderHints) {
        // Get ImageLayout from renderHints if any.
        ImageLayout layout = RIFUtil.getImageLayoutHint(renderHints);
        
        
        RenderedImage source = paramBlock.getRenderedSource(0);

        return new MagnitudePhaseOpImage(source, renderHints, layout,
                                         MagnitudePhaseOpImage.PHASE);
    }
}
