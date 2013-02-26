/*
 * $RCSfile: LookupCRIF.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:30 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.opimage;
import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import com.lightcrafts.mediax.jai.CRIFImpl;
import com.lightcrafts.mediax.jai.ImageLayout;
import com.lightcrafts.mediax.jai.LookupTableJAI;

/**
 * A <code>CRIF</code> supporting the "Lookup" operation in the
 * rendered and renderable image layers.
 *
 * <p>Although Lookup is supported in the renderable layer, it is necessary
 * to understand that in some situations the operator may not produce smooth
 * results. This is due to an affine transform being performed on the source
 * image, which combined with certain types of table data will produce
 * random/unexpected destination values. In addition, a lookup operation
 * with the same input source and table in the renderable chain will yield
 * to different destination from different rendering.
 *
 * @see com.lightcrafts.mediax.jai.operator.LookupDescriptor
 * @see LookupOpImage
 *
 */
public class LookupCRIF extends CRIFImpl {

    /** Constructor. */
    public LookupCRIF() {
        super("lookup");
    }

    /**
     * Creates a new instance of <code>LookupOpImage</code>
     * in the rendered layer.
     *
     * @param args   The source image and the lookup table.
     * @param hints  Optionally contains destination image layout.
     */
    public RenderedImage create(ParameterBlock args,
                                RenderingHints renderHints) {
        // Get ImageLayout from renderHints if any.
        ImageLayout layout = RIFUtil.getImageLayoutHint(renderHints);
        

        return new LookupOpImage(args.getRenderedSource(0),
                                 renderHints,
                                 layout,
                                 (LookupTableJAI)args.getObjectParameter(0));
    }
}
