/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.jai.opimage;
import org.eclipse.imagen.media.opimage.RIFUtil;

import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import org.eclipse.imagen.CRIFImpl;
import org.eclipse.imagen.ImageLayout;

public class LCUnsharpMaskCRIF extends CRIFImpl {

    /** Constructor. */
    public LCUnsharpMaskCRIF() {
        super("LCUnsharpMask");
    }

    /**
     * Creates a new instance of <code>LCUnsharpMaskOpImage</code> in the rendered
     * layer. This method satisfies the implementation of RIF.
     *
     * @param paramBlock   The two source images to be added.
     * @param renderHints  Optionally contains destination image layout.
     */
    public RenderedImage create(ParameterBlock paramBlock,
                                RenderingHints renderHints) {
        // Get ImageLayout from renderHints if any.
        ImageLayout layout = RIFUtil.getImageLayoutHint(renderHints);


        return new LCUnsharpMaskOpImage(paramBlock.getRenderedSource(0),
                                      paramBlock.getRenderedSource(1),
                                      renderHints,
                                      layout,
                                      paramBlock.getDoubleParameter(0),
                                      paramBlock.getIntParameter(1));
    }
}
