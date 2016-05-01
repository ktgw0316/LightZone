/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.jai.opimage;

import com.lightcrafts.media.jai.opimage.RIFUtil;

import com.lightcrafts.mediax.jai.ImageLayout;
import com.lightcrafts.mediax.jai.CRIFImpl;
import com.lightcrafts.mediax.jai.ROI;

import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.awt.*;

/**
 * Created by IntelliJ IDEA.
 * User: fabio
 * Date: Apr 1, 2005
 * Time: 10:14:47 AM
 * To change this template use File | Settings | File Templates.
 */
public class BlendCRIF extends CRIFImpl {

    /** Constructor. */
    public BlendCRIF() {
        super("blend");
    }

    /**
     * Creates a new instance of <code>BlendOpImage</code> in the rendered
     * layer. This method satisfies the implementation of RIF.
     *
     * @param paramBlock   The two source images to be blended and the blending mode.
     * @param renderHints  Optionally contains destination image layout.
     */
    public RenderedImage create(ParameterBlock paramBlock,
                                RenderingHints renderHints) {
        // Get ImageLayout from renderHints if any.
        ImageLayout layout = RIFUtil.getImageLayoutHint(renderHints);

        return new BlendOpImage(paramBlock.getRenderedSource(0),
                                paramBlock.getRenderedSource(1),
                                (String) paramBlock.getObjectParameter(0),
                                (Double) paramBlock.getObjectParameter(1),
                                (ROI) paramBlock.getObjectParameter(2),
                                (RenderedImage) paramBlock.getObjectParameter(3),
                                renderHints,
                                layout);
    }
}
