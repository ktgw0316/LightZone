/*
 * $RCSfile: BinarizeCRIF.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:15 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.opimage;
import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import com.lightcrafts.mediax.jai.CRIFImpl;
import com.lightcrafts.mediax.jai.ImageLayout;

/**
 * A <code>CRIF</code> supporting the "Binarize" operation in the rendered
 * and renderable image layers.
 *
 * @see com.lightcrafts.mediax.jai.operator.BinarizeDescriptor
 * @see com.lightcrafts.mediax.jai.operator.BinarizeDescriptor
 * @see BinarizeOpImage
 *
 */
public class BinarizeCRIF extends CRIFImpl {

    /** Constructor. */
    public BinarizeCRIF() {
        super("binarize");
    }

    /**
     * Creates a new instance of <code>BinarizeOpImage</code> in the
     * rendered layer.
     *
     * @param args   The source image and the input parameters.
     * @param hints  Optionally contains destination image layout.
     */
    public RenderedImage create(ParameterBlock args,
                                RenderingHints renderHints) {
        // Get ImageLayout from renderHints if any.
        ImageLayout layout = RIFUtil.getImageLayoutHint(renderHints);
        
        return new BinarizeOpImage(args.getRenderedSource(0),
				   renderHints,
				   layout,
				   args.getDoubleParameter(0));
    }
}
