/*
 * $RCSfile: DivideByConstCRIF.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:23 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.opimage;
import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import com.lightcrafts.mediax.jai.CRIFImpl;
import com.lightcrafts.mediax.jai.ImageLayout;

/**
 * A <code>CRIF</code> supporting the "DivideByConst" operation in the rendered
 * and renderable image layers.
 *
 * @see com.lightcrafts.mediax.jai.operator.DivideByConstDescriptor
 * @see MultiplyConstOpImage
 *
 *
 * @since EA2
 */
public class DivideByConstCRIF extends CRIFImpl {

    /** Constructor. */
    public DivideByConstCRIF() {
        super("dividebyconst");
    }

    /**
     * Creates a new instance of <code>DivideByConstOpImage</code> in the
     * rendered layer.
     *
     * @param args   The source image and the constants.
     * @param hints  Optionally contains destination image layout.
     */
    public RenderedImage create(ParameterBlock args,
                                RenderingHints renderHints) {
        // Get ImageLayout from renderHints if any.
        ImageLayout layout = RIFUtil.getImageLayoutHint(renderHints);
        

        /* Invert the constants. */
        double[] constants = (double[])args.getObjectParameter(0);
        int length = constants.length;

        double[] invConstants = new double[length];

        for (int i = 0; i < length; i++) {
            invConstants[i] = 1.0 / constants[i];
        }

        return new MultiplyConstOpImage(args.getRenderedSource(0),
                                        renderHints,
					layout,
					invConstants);
    }
}
