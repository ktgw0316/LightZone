/*
 * $RCSfile: SubtractConstCRIF.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:44 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.opimage;
import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import com.lightcrafts.mediax.jai.CRIFImpl;
import com.lightcrafts.mediax.jai.ImageLayout;

/**
 * A <code>CRIF</code> supporting the "SubtractConst" operation in the rendered
 * and renderable image layers.
 *
 * @see com.lightcrafts.mediax.jai.operator.SubtractConstDescriptor
 * @see AddConstOpImage
 *
 */
public class SubtractConstCRIF extends CRIFImpl {

    /** Constructor. */
    public SubtractConstCRIF() {
        super("subtractconst");
    }

    /**
     * Creates a new instance of <code>SubtractConstOpImage</code> in the
     * rendered layer. This method satisfies the implementation of RIF.
     *
     * @param args   The source image and the constants.
     * @param hints  Optionally contains destination image layout.
     */
    public RenderedImage create(ParameterBlock args,
                                RenderingHints hints) {
        // Get ImageLayout from redering hints if any.
        ImageLayout layout = RIFUtil.getImageLayoutHint(hints);
        
        // Negate the constants vector
        double[] constants = (double[])args.getObjectParameter(0);
        int length = constants.length;

        double[] negConstants = new double[length];

        for (int i = 0; i < length; i++) {
            negConstants[i] = -constants[i];
        }

        return new AddConstOpImage(args.getRenderedSource(0),
                                   hints, layout,
                                   negConstants);
    }
}
