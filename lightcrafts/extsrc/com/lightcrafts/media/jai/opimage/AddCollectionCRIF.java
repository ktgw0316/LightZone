/*
 * $RCSfile: AddCollectionCRIF.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:11 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.opimage;
import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.util.Collection;
import com.lightcrafts.mediax.jai.CRIFImpl;
import com.lightcrafts.mediax.jai.ImageLayout;

/**
 * A <code>CRIF</code> supporting the "AddCollection" operation
 * in the rendered and renderable image layers.
 *
 * @see com.lightcrafts.mediax.jai.operator.AddCollectionDescriptor
 * @see AddCollectionOpImage
 *
 *
 * @since EA3
 */
public class AddCollectionCRIF extends CRIFImpl {

    /** Constructor. */
    public AddCollectionCRIF() {
        super("addcollection");
    }

    /**
     * Creates a new instance of <code>AddCollectionOpImage</code>
     * in the rendered layer.
     *
     * @param args   A collection of rendered images to be added.
     * @param hints  Optionally contains destination image layout.
     */
    public RenderedImage create(ParameterBlock args,
                                RenderingHints renderHints) {
        // Get ImageLayout from renderHints if any.
        ImageLayout layout = RIFUtil.getImageLayoutHint(renderHints);
        
        
        return new AddCollectionOpImage((Collection)args.getSource(0),
                                        renderHints, layout);
    }
}
