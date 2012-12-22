/*
 * $RCSfile: ExtremaRIF.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:25 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.opimage;
import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderedImageFactory;
import com.lightcrafts.mediax.jai.ROI;

/**
 * A <code>RIF</code> supporting the "Extrema" operation in the
 * rendered image layer.
 *
 * @see com.lightcrafts.mediax.jai.operator.ExtremaDescriptor
 */
public class ExtremaRIF implements RenderedImageFactory {

    /** Constructor. */
    public ExtremaRIF() {}

    /**
     * Creates a new instance of <code>ExtremaOpImage</code>
     * in the rendered layer. Any image layout information in
     * <code>RenderingHints</code> is ignored.
     * This method satisfies the implementation of RIF.
     */
    public RenderedImage create(ParameterBlock paramBlock,
                                RenderingHints hints) {
        RenderedImage src = paramBlock.getRenderedSource(0);

        int xStart = src.getMinX();		// default values
        int yStart = src.getMinY();

        int maxWidth = src.getWidth();
        int maxHeight = src.getHeight();

        return new ExtremaOpImage(src,
                                  (ROI)paramBlock.getObjectParameter(0),
                                  xStart, yStart,
                                  paramBlock.getIntParameter(1),
                                  paramBlock.getIntParameter(2),
                                  ((Boolean)paramBlock.getObjectParameter(3)).booleanValue(),
                                  paramBlock.getIntParameter(4));
    }
}
