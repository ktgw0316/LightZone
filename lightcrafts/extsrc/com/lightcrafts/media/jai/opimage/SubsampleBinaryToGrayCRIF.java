/*
 * $RCSfile: SubsampleBinaryToGrayCRIF.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:44 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.opimage;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.DataBuffer;
import java.awt.image.MultiPixelPackedSampleModel;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.renderable.RenderContext;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderableImage;
import com.lightcrafts.mediax.jai.CRIFImpl;
import com.lightcrafts.mediax.jai.BorderExtender;
import com.lightcrafts.mediax.jai.ImageLayout;

/**
 * @see SubsampleBinaryToGrayOpImage
 */
public class SubsampleBinaryToGrayCRIF extends CRIFImpl {

    /** Constructor. */
    public SubsampleBinaryToGrayCRIF() {
        super("subsamplebinarytogray");
    }

    /**
     * Creates a new instance of SubsampleBinaryToGrayOpImage in the rendered layer.
     * This method satisfies the implementation of RIF.
     *
     * @param paramBlock  The source image, the X and Y scale factor
     */
    public RenderedImage create(ParameterBlock paramBlock,
                                RenderingHints renderHints) {
        // Get ImageLayout from renderHints if any.
        ImageLayout layout = RIFUtil.getImageLayoutHint(renderHints);
        

        // Get BorderExtender from renderHints if any.
        BorderExtender extender = RIFUtil.getBorderExtenderHint(renderHints);

        RenderedImage source = paramBlock.getRenderedSource(0);
        float xScale = paramBlock.getFloatParameter(0);
        float yScale = paramBlock.getFloatParameter(1);

	// Check and see if we are scaling by 1.0 in both x and y and no
        // translations. If so call the copy operation.
	if (xScale == 1.0F && yScale == 1.0F){
	    return new CopyOpImage(source, renderHints, layout);
	}


        // The image is assumed to have
        // a MultiPixelPackedSampleModel and a byte, ushort,
        // or int DataBuffer we can access the pixel data directly.
        // Note that there is a potential loophole that has not been
        // resolved by Java2D as to whether the underlying DataBuffer
        // must be of one of the standard types.  Here we make the
        // assumption that it will be -- we can't check without
        // forcing an actual tile to be computed.
        //
        SampleModel sm = source.getSampleModel();
        if ((sm instanceof MultiPixelPackedSampleModel) &&
            (sm.getSampleSize(0) == 1) &&
            (sm.getDataType() == DataBuffer.TYPE_BYTE || 
             sm.getDataType() == DataBuffer.TYPE_USHORT || 
             sm.getDataType() == DataBuffer.TYPE_INT)) {
	     
	     int srcWidth = source.getWidth();
	     int srcHeight= source.getHeight();
	     float floatTol = .1F * Math.min(xScale/(srcWidth*xScale+1.0F), yScale/(srcHeight*yScale+1.0F));
	     int invScale = (int) Math.round(1.0F/xScale);
	     if(Math.abs((float)invScale - 1.0F/xScale) < floatTol  &&
		Math.abs((float)invScale - 1.0F/yScale) < floatTol){
	       switch (invScale){
	       case 2:
		 return new SubsampleBinaryToGray2x2OpImage(source, layout, renderHints);
	       case 4:
		 return new SubsampleBinaryToGray4x4OpImage(source, layout, renderHints);
	       default:
		 break;
	       }
	     }
	     return new SubsampleBinaryToGrayOpImage(source,
						     layout,
						     renderHints,
						     xScale,
						     yScale);
	     
	}else{
	    throw 
		new IllegalArgumentException(JaiI18N.getString("SubsampleBinaryToGray3"));
	}
    }




    /**
     * Creates a new instance of <code>AffineOpImage</code>
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
	
	float scale_x = paramBlock.getFloatParameter(0);
        float scale_y = paramBlock.getFloatParameter(1);

        AffineTransform scale = new AffineTransform(scale_x, 0.0, 0.0, scale_y,
                                                    0.0, 0.0);

        RenderContext RC = (RenderContext)renderContext.clone();
        AffineTransform usr2dev = RC.getTransform();
        usr2dev.concatenate(scale);
	RC.setTransform(usr2dev);
	return RC;
    }

    /**
     * Gets the bounding box for the output of <code>SubsampleBinaryToGrayOpImage</code>.
     * This method satisfies the implementation of CRIF.
     */
    public Rectangle2D getBounds2D(ParameterBlock paramBlock) {        

        RenderableImage source = paramBlock.getRenderableSource(0);

        float scale_x = paramBlock.getFloatParameter(0);
        float scale_y = paramBlock.getFloatParameter(1);

	// Get the source dimensions
	float x0 = (float)source.getMinX();
	float y0 = (float)source.getMinY() ;
	float w = (float)source.getWidth();
	float h = (float)source.getHeight();
	
	// Forward map the source using x0, y0, w and h
	float d_x0 = x0 * scale_x;
	float d_y0 = y0 * scale_y;
	float d_w = w * scale_x;
	float d_h = h * scale_y;
	
	// debugging
	// System.out.println("*** CRIF getBounds2D() ");

	return new Rectangle2D.Float(d_x0, d_y0, d_w, d_h);
    }

}
