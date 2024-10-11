/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.jai.opimage;

import org.eclipse.imagen.media.opimage.RIFUtil;

import org.eclipse.imagen.CRIFImpl;
import org.eclipse.imagen.ImageLayout;
import org.eclipse.imagen.NullOpImage;
import org.eclipse.imagen.OpImage;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.awt.*;

/**
 * Created by IntelliJ IDEA.
 * User: fabio
 * Date: Apr 15, 2005
 * Time: 2:01:38 AM
 * To change this template use File | Settings | File Templates.
 */
public class NOPCRIF extends CRIFImpl {

    /** Constructor. */
    public NOPCRIF() {
        super("nop");
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

        return new NullOpImage(paramBlock.getRenderedSource(0),
                               layout,
                               renderHints,
                               OpImage.OP_COMPUTE_BOUND);
    }

}
