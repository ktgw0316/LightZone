/*
 * $RCSfile: DCTCRIF.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:21 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.opimage;
import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import com.lightcrafts.mediax.jai.CRIFImpl;
import com.lightcrafts.mediax.jai.ImageLayout;

/**
 * A <code>CRIF</code> supporting the "DCT" operation in the rendered
 * image layer.
 *
 * @since Beta
 * @see com.lightcrafts.mediax.jai.operator.DCTDescriptor
 *
 */
public class DCTCRIF extends CRIFImpl {

    /** Constructor. */
    public DCTCRIF() {
        super("dct");
    }

    /**
     * Creates a new instance of a DCT operator.
     *
     * @param paramBlock The scaling type.
     */
    public RenderedImage create(ParameterBlock paramBlock,
                                RenderingHints renderHints) {
        // Get ImageLayout from renderHints if any.
        ImageLayout layout = RIFUtil.getImageLayoutHint(renderHints);
        

        RenderedImage source = paramBlock.getRenderedSource(0);

        return new DCTOpImage(source, renderHints, layout, new FCT(true, 2));
    }
}
