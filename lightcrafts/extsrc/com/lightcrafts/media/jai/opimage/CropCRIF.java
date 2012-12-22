/*
 * $RCSfile: CropCRIF.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:21 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.opimage;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderContext;
import com.lightcrafts.mediax.jai.CRIFImpl;
import com.lightcrafts.mediax.jai.JAI;

/**
 * A <code>CRIF</code> supporting the "Crop" operation in the rendered
 * and renderable image layers.
 *
 * @see com.lightcrafts.mediax.jai.operator.CropDescriptor
 * @see CropOpImage
 *
 *
 * @since EA4
 */
public class CropCRIF extends CRIFImpl {

    /** Constructor. */
    public CropCRIF() {
        super("crop");
    }

    /**
     * Creates a new instance of <code>CropOpImage</code> in the
     * rendered layer.
     *
     * @param args   The source image and bounding rectangle
     * @param hints  Optionally contains destination image layout.
     */
    public RenderedImage create(ParameterBlock args,
                                RenderingHints renderHints) {
        // Get the source image.
        RenderedImage src = (RenderedImage)args.getRenderedSource(0);

        // Get the parameters.
        float originX = args.getFloatParameter(0);
        float originY = args.getFloatParameter(1);
        float width   = args.getFloatParameter(2);
        float height  = args.getFloatParameter(3);

        // Return the OpImage.
        return new CropOpImage(src,
                               originX,
                               originY,
                               width,
                               height);
    }

    /**
     * Creates a <Code>RenderedImage</Code> from the renderable layer.
     *
     * @param renderContext The rendering information associated with
     *        this rendering.
     * @param paramBlock The parameters used to create the image.
     * @return A <code>RenderedImage</code>.
     */
    public RenderedImage create(RenderContext renderContext,
                                ParameterBlock paramBlock) {
        // Get the destination bounds in rendering-independent coordinates.
        Rectangle2D dstRect2D = getBounds2D(paramBlock);

        // Map the destination bounds to rendered coordinates. This method
        // will cause extra data to be present if there is any rotation or
        // shear.
        AffineTransform tf = renderContext.getTransform();
        Rectangle2D rect = tf.createTransformedShape(dstRect2D).getBounds2D();

        // Make sure that the rendered rectangle is non-empty.
        if(rect.getWidth() < 1.0 || rect.getHeight() < 1.0) {
            double w = Math.max(rect.getWidth(), 1.0);
            double h = Math.max(rect.getHeight(), 1.0);
            rect.setRect(rect.getMinX(), rect.getMinY(), w, h);
        }

        // Initialize the rendered ParameterBlock.
        ParameterBlock pb = new ParameterBlock();
        pb.addSource(paramBlock.getRenderedSource(0));
        pb.set((float)rect.getMinX(), 0);
        pb.set((float)rect.getMinY(), 1);
        pb.set((float)rect.getWidth(), 2);
        pb.set((float)rect.getHeight(), 3);

        // Crop the rendered source.
        return JAI.create("crop", pb, renderContext.getRenderingHints());
    }

    /**
     * Returns the bounding box for the output of the operation.
     *
     * @param paramBlock A <code>ParameterBlock</code> containing the
     *        renderable sources and parameters of the operation.
     * @return A <code>Rectangle2D</code> specifying the bounding box.
     */
    public Rectangle2D getBounds2D(ParameterBlock paramBlock) {
        // Return a rectangle representing the desired bounds.
        return new Rectangle2D.Float(paramBlock.getFloatParameter(0),
                                     paramBlock.getFloatParameter(1),
                                     paramBlock.getFloatParameter(2),
                                     paramBlock.getFloatParameter(3));
    }
}
