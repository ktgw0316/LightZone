/*
 * $RCSfile: IIPResolutionRIF.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:29 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.opimage;
import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderedImageFactory;

/**
 * A <code>RIF</code> supporting the "IIPResolution" operation in the
 * rendered image layer.
 *
 * @see com.lightcrafts.mediax.jai.operator.IIPResolutionDescriptor
 * @see IIPResolutionOpImage
 *
 * @since 1.0
 *
 */
public class IIPResolutionRIF implements RenderedImageFactory {

    /** Constructor. */
    public IIPResolutionRIF() {}

    /**
     * Creates a new instance of <code>IIPResolutionOpImage</code>
     * in the rendered layer.  Any image layout information in
     * the <code>RenderingHints</code> is ignored.
     * This method satisfies the implementation of RIF.
     */
    public RenderedImage create(ParameterBlock args,
                                RenderingHints hints) {
        return new IIPResolutionOpImage(hints,
                                        (String)args.getObjectParameter(0),
                                        args.getIntParameter(1),
                                        args.getIntParameter(2));
    }
}
