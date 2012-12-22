/*
 * $RCSfile: AWTImageRIF.java,v $
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
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderedImageFactory;
import com.lightcrafts.mediax.jai.ImageLayout;

/**
 * A <code>RIF</code> supporting the "AWTImage" operation in the rendered
 * layer. It's used by </code>OperationRegistry</code> to create an
 * <code>AWTImageOpImage</code>.
 *
 * @see com.lightcrafts.mediax.jai.operator.AWTImageDescriptor
 * @see AWTImageOpImage
 *
 */
public class AWTImageRIF implements RenderedImageFactory {

    /** Constructor. */
    public AWTImageRIF() {}

    /**
     * Creates a new instance of <code>AWTImageOpImage</code>
     * in the rendered layer.
     * This method satisfies the implementation of RIF.
     *
     * @param paramBlock  The AWT image.
     */
    public RenderedImage create(ParameterBlock paramBlock,
                                RenderingHints renderHints) {
        // Get ImageLayout from renderHints if any.
        ImageLayout layout = RIFUtil.getImageLayoutHint(renderHints);

        // Extract the source AWT Image and cast it.
        Image awtImage = (Image)paramBlock.getObjectParameter(0);

        // If it's already a RenderedImage (as for a BufferedImage) just cast.
        if(awtImage instanceof RenderedImage) {
            return (RenderedImage)awtImage;
        }

        // Create a RenderedImage from the data.
        return new AWTImageOpImage(renderHints, layout, awtImage);
    }
}
