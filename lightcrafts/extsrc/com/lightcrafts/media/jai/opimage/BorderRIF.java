/*
 * $RCSfile: BorderRIF.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:16 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.opimage;
import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderedImageFactory;
import com.lightcrafts.mediax.jai.BorderExtender;
import com.lightcrafts.mediax.jai.ImageLayout;

/**
 * A <code>RIF</code> supporting the "border" operation in the
 * rendered image layer.
 *
 * @see java.awt.image.renderable.RenderedImageFactory
 * @see com.lightcrafts.mediax.jai.operator.BorderDescriptor
 * @see BorderOpImage
 *
 */
public class BorderRIF implements RenderedImageFactory {

    /** Constructor. */
    public BorderRIF() {}

    /**
     * Creates a new instance of <code>BorderOpImage</code>
     * in the rendered layer.
     *
     * @param args   The source image and the border information
     * @param hints  Optionally contains destination image layout.
     */
    public RenderedImage create(ParameterBlock args,
                                RenderingHints renderHints) {
        // Get ImageLayout from renderHints if any.
        ImageLayout layout = RIFUtil.getImageLayoutHint(renderHints);
        

        RenderedImage source = args.getRenderedSource(0);
        int leftPad = args.getIntParameter(0);
        int rightPad = args.getIntParameter(1);
        int topPad = args.getIntParameter(2);
        int bottomPad = args.getIntParameter(3);
        BorderExtender type =
            (BorderExtender)args.getObjectParameter(4);

        if (type ==
            BorderExtender.createInstance(BorderExtender.BORDER_WRAP)) {
            int minX = source.getMinX() - leftPad;
            int minY = source.getMinY() - topPad;
            int width = source.getWidth() + leftPad + rightPad;
            int height = source.getHeight() + topPad + bottomPad;

            return new PatternOpImage(source.getData(),
                                      source.getColorModel(),
                                      minX, minY,
                                      width, height);
        } else {
            return new BorderOpImage(source, renderHints, layout,
                                     leftPad, rightPad, topPad, bottomPad,
                                     type);
        }
    }
}
