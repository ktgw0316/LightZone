/*
 * $RCSfile: SubsampleAverageCRIF.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:43 $
 * $State: Exp $
 */package com.lightcrafts.media.jai.opimage;

import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.RenderContext;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderableImage;
import com.lightcrafts.mediax.jai.ImageLayout;
import com.lightcrafts.mediax.jai.CRIFImpl;

/**
 * @see SubsampleAverageOpImage
 */
public class SubsampleAverageCRIF extends CRIFImpl {

    /** Constructor. */
    public SubsampleAverageCRIF() {
        super("SubsampleAverage");
    }

    /**
     * Creates a new instance of SubsampleAverageOpImage in the rendered layer.
     * This method satisfies the implementation of RIF.
     *
     * @param paramBlock  The source image, the X and Y scale factor,
     *                    and the interpolation method for resampling.
     */
    public RenderedImage create(ParameterBlock paramBlock,
                                RenderingHints renderHints) {

        // Get ImageLayout from renderHints if any.
        ImageLayout layout = RIFUtil.getImageLayoutHint(renderHints);
        
        RenderedImage source = paramBlock.getRenderedSource(0);
        double scaleX = paramBlock.getDoubleParameter(0);
        double scaleY = paramBlock.getDoubleParameter(1);

	// Check and see if we are scaling by 1.0 in both x and y and no
        // translations. If so return the source directly.	
	if (scaleX == 1.0 && scaleY == 1.0) {
	    return source;
	}

        return new SubsampleAverageOpImage(source, layout, renderHints,
                                           scaleX, scaleY);
    }

    /**
     * Creates a new instance of <code>SubsampleAverageOpImage</code>
     * in the renderable layer. This method satisfies the
     * implementation of CRIF.
     */
    public RenderedImage create(RenderContext renderContext,
                                ParameterBlock paramBlock) {
        return paramBlock.getRenderedSource(0);
    }

    /**
     * Maps the output RenderContext into the RenderContext for the ith
     * source.
     * This method satisfies the implementation of CRIF.
     *
     * @param i               The index of the source image.
     * @param renderContext   The renderContext being applied to the operation.
     * @param paramBlock      The ParameterBlock containing the sources
     *                        and the translation factors.
     * @param image           The RenderableImageOp from which this method
     *                        was called.
     */
    public RenderContext mapRenderContext(int i,
                                          RenderContext renderContext,
					  ParameterBlock paramBlock,
					  RenderableImage image) {
	
        double scaleX = paramBlock.getDoubleParameter(0);
        double scaleY = paramBlock.getDoubleParameter(1);

        AffineTransform scale =
            new AffineTransform(scaleX, 0.0, 0.0, scaleY, 0.0, 0.0);

        RenderContext RC = (RenderContext)renderContext.clone();
        AffineTransform usr2dev = RC.getTransform();
        usr2dev.concatenate(scale);
	RC.setTransform(usr2dev);
	return RC;
    }

    /**
     * Gets the bounding box for the output of <code>ScaleOpImage</code>.
     * This method satisfies the implementation of CRIF.
     */
    public Rectangle2D getBounds2D(ParameterBlock paramBlock) {        

        RenderableImage source = paramBlock.getRenderableSource(0);

        double scaleX = paramBlock.getDoubleParameter(0);
        double scaleY = paramBlock.getDoubleParameter(1);

	// Get the source dimensions
	float x0 = (float)source.getMinX();
	float y0 = (float)source.getMinY() ;
	float w = (float)source.getWidth();
	float h = (float)source.getHeight();
	
	// Forward map the source using x0, y0, w and h
	float d_x0 = (float)(x0 * scaleX);
	float d_y0 = (float)(y0 * scaleY);
	float d_w = (float)(w * scaleX);
	float d_h = (float)(h * scaleY);
	
	return new Rectangle2D.Float(d_x0, d_y0, d_w, d_h);
    }

}
