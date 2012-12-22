/*
 * $RCSfile: PiecewiseCRIF.java,v $
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
 * A <code>CRIF</code> supporting the "Piecewise" operation in the rendered
 * and renderable image layers.
 *
 * @see com.lightcrafts.mediax.jai.operator.PiecewiseDescriptor
 * @see PiecewiseOpImage
 *
 *
 * @since EA4
 */
public class PiecewiseCRIF extends CRIFImpl {

    /** Constructor. */
    public PiecewiseCRIF() {
        super("piecewise");
    }

    /**
     * Creates a new instance of <code>PiecewiseOpImage</code> in the
     * rendered layer.
     *
     * @param args   The source image and the breakpoints.
     * @param hints  Optionally contains destination image layout.
     */
    public RenderedImage create(ParameterBlock args,
                                RenderingHints renderHints) {
        // Get ImageLayout from renderHints if any.
        ImageLayout layout = RIFUtil.getImageLayoutHint(renderHints);
        

	return new PiecewiseOpImage(args.getRenderedSource(0),
				    renderHints,
				    layout,
				    (float[][][])args.getObjectParameter(0));
    }
}
