/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.jai.opimage;

import org.eclipse.imagen.media.opimage.RIFUtil;

import org.eclipse.imagen.CRIFImpl;
import org.eclipse.imagen.ImageLayout;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.awt.*;

public class RawAdjustmentsCRIF extends CRIFImpl {

    /** Constructor. */
    public RawAdjustmentsCRIF() {
        super("RawAdjustments");
    }

    /**
     * Creates a new instance of <code>ColorConvertOpImage</code> in the
     * rendered layer.
     *
     * @param args        The source image and the destination ColorModel.
     * @param renderHints Optionally contains destination image layout.
     */
    public RenderedImage create(ParameterBlock args,
                                RenderingHints renderHints) {
        // Get ImageLayout from renderHints if any.
        ImageLayout layout = RIFUtil.getImageLayoutHint(renderHints);

        return new RawAdjustmentsOpImage(args.getRenderedSource(0),
                                       renderHints,
				       layout,
				       args.getFloatParameter(0),
                                       args.getFloatParameter(1),
                                       (float[][]) args.getObjectParameter(2));
    }
}
