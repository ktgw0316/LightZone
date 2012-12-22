/*
 * $RCSfile: MeanRIF.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:33 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.opimage;
import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderedImageFactory;
import com.lightcrafts.mediax.jai.ROI;

/**
 * A <code>RIF</code> supporting the "Mean" operation in the
 * rendered image layer.
 *
 * @since EA2
 * @see com.lightcrafts.mediax.jai.operator.MeanDescriptor
 *
 */
public class MeanRIF implements RenderedImageFactory {

    /** Constructor. */
    public MeanRIF() {}

    /**
     * Creates a new instance of <code>MeanOpImage</code>
     * in the rendered layer. Any image layout information in
     * <code>RenderingHints</code> is ignored.
     * This method satisfies the implementation of RIF.
     */
    public RenderedImage create(ParameterBlock paramBlock,
                                RenderingHints renderHints) {
        RenderedImage src = paramBlock.getRenderedSource(0);

        int xStart = src.getMinX();		// default values
        int yStart = src.getMinY();

        int maxWidth = src.getWidth();
        int maxHeight = src.getHeight();

        return new MeanOpImage(src,
                               (ROI)paramBlock.getObjectParameter(0),
                               xStart, yStart,
                               paramBlock.getIntParameter(1),
                               paramBlock.getIntParameter(2));
    }
}
