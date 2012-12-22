/*
 * $RCSfile: PatternRIF.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:40 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.opimage;
import java.awt.RenderingHints;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.RenderedImageFactory;
import java.awt.image.renderable.ParameterBlock;
import com.lightcrafts.mediax.jai.ImageLayout;

/**
 * A <code>RIF</code> supporting the "Pattern" operation in the
 * rendered image layer.
 *
 * @see com.lightcrafts.mediax.jai.operator.PatternDescriptor
 * @see PatternOpImage
 *
 */
public class PatternRIF implements RenderedImageFactory {

    /** Constructor. */
    public PatternRIF() {}

    /**
     * Creates a new instance of PatternOpImage in the rendered layer.
     * This method satisfies the implementation of RIF.
     */
    public RenderedImage create(ParameterBlock paramBlock,
                                RenderingHints renderHints) {
        // Get ImageLayout from renderHints if any.
        ImageLayout layout = RIFUtil.getImageLayoutHint(renderHints);
        
        int minX = 0;
        int minY = 0;

        if (layout != null) {
            if (layout.isValid(ImageLayout.MIN_X_MASK)) {
                minX = layout.getMinX(null);
            }
            if (layout.isValid(ImageLayout.MIN_Y_MASK)) {
                minY = layout.getMinY(null);
            }
        }

        RenderedImage source = (RenderedImage)paramBlock.getSource(0);
        Raster pattern = source.getData();
        ColorModel colorModel = source.getColorModel();
 
        // Get image width and height from the parameter block
        int width = paramBlock.getIntParameter(0);
        int height = paramBlock.getIntParameter(1);

        return new PatternOpImage(pattern, colorModel,
                                  minX, minY, width, height);
    }
}
