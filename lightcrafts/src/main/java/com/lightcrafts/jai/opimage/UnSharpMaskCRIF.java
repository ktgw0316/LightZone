/* Copyright (C) 2005-2011 Fabio Riccardi */

/*
 * $RCSfile: AddCRIF.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:11 $
 * $State: Exp $
 */
package com.lightcrafts.jai.opimage;
import com.sun.media.jai.opimage.RIFUtil;

import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import javax.media.jai.CRIFImpl;
import javax.media.jai.ImageLayout;

public class UnSharpMaskCRIF extends CRIFImpl {

    /** Constructor. */
    public UnSharpMaskCRIF() {
        super("LCUnSharpMask");
    }

    /**
     * Creates a new instance of <code>AddOpImage</code> in the rendered
     * layer. This method satisfies the implementation of RIF.
     *
     * @param paramBlock   The two source images to be added.
     * @param renderHints  Optionally contains destination image layout.
     */
    public RenderedImage create(ParameterBlock paramBlock,
                                RenderingHints renderHints) {
        // Get ImageLayout from renderHints if any.
        ImageLayout layout = RIFUtil.getImageLayoutHint(renderHints);


        return new UnSharpMaskOpImage(paramBlock.getRenderedSource(0),
                                      paramBlock.getRenderedSource(1),
                                      renderHints,
                                      layout,
                                      paramBlock.getDoubleParameter(0),
                                      paramBlock.getIntParameter(1));
    }
}
