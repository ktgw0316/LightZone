/*
 * $RCSfile: RenderableCRIF.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:41 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.opimage;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.RenderContext;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderableImage;
import java.lang.ref.SoftReference;
import java.util.Hashtable;
import java.util.Vector;
import com.lightcrafts.mediax.jai.CRIFImpl;
import com.lightcrafts.mediax.jai.ImageMIPMap;
import com.lightcrafts.mediax.jai.MultiResolutionRenderableImage;
import com.lightcrafts.mediax.jai.RenderedOp;

/**
 * A <code>CRIF</code> supporting the "Renderable" operation in the
 * renderable image layers.
 *
 * @see com.lightcrafts.mediax.jai.operator.RenderableDescriptor
 *
 *
 * @since 1.0
 */
public class RenderableCRIF extends CRIFImpl {
    /** Cache of SoftReferences to the MultiResolutionRenderableImages. */
    private Hashtable mresTable = null;

    /** Derives a hash key for this ParameterBlock. */
    private static final Object getKey(ParameterBlock paramBlock) {
        // Initialize the key string.
        String key = new String();

        // Add the hash code of the source to the key.
        key += String.valueOf(paramBlock.getRenderedSource(0).hashCode());

        // Add the RenderedOp parameter to the key.
        key += getKey((RenderedOp)paramBlock.getObjectParameter(0));

        // Add the numerical parameters to the key.
        key += String.valueOf(paramBlock.getIntParameter(1));
        key += String.valueOf(paramBlock.getFloatParameter(2));
        key += String.valueOf(paramBlock.getFloatParameter(3));
        key += String.valueOf(paramBlock.getFloatParameter(4));

        return key;
    }

    /** Derives a hash key string for this RenderedOp. */
    private static final String getKey(RenderedOp op) {
        // Initialize the key string to the RenderedOp hash code.
        String key = new String(String.valueOf(op.hashCode()));

        // Get the ParameterBlock
        ParameterBlock pb = op.getParameterBlock();

        // Add the sources.
        int numSources = pb.getNumSources();
        for(int s = 0; s < numSources; s++) {
            RenderedImage src = pb.getRenderedSource(s);

            // If the source is a node recurse up the chain.
            if(src instanceof RenderedOp) {
                key += getKey((RenderedOp)src);
            } else {
                key += String.valueOf(src.hashCode());
            }
        }

        // Add the parameters.
        int numParameters = pb.getNumParameters();
        for(int p = 0; p < numParameters; p++) {
            // Use toString() instead of hashCode() here because the
            // majority of parameters are numerical.
            key += pb.getObjectParameter(p).toString();
        }

        return key;
    }

    /** Constructor. */
    public RenderableCRIF() {}

    /**
     * Creates a RenderableImage pyramid from the source and parameters.
     * If the down sampler operation chain does not decrease both the
     * width and height at a given level an IllegalArgumentException will
     * be thrown.
     *
     * @param paramBlock The ParameterBlock containing a single RenderedImage
     * source and parameters sufficient to create an image pyramid.
     */
    private RenderableImage createRenderable(ParameterBlock paramBlock) {
        // Create the Hashtable "just in time".
        if(mresTable == null) {
            mresTable = new Hashtable();
        }

        // Check for a SoftReference hashed on a ParameterBlock-derived key.
        Object key = getKey(paramBlock);
        SoftReference ref = (SoftReference)mresTable.get(key);

        // Retrieve the image from the SoftReference if possible.
        RenderableImage mres = null;
        if(ref != null && (mres = (RenderableImage)ref.get()) == null) {
            // null referent: remove the ParameterBlock key from the Hashtable.
            mresTable.remove(key);
        }

        // Derive the image if necessary.
        if(mres == null) {
            // Retrieve the source and parameters.
            RenderedImage source = paramBlock.getRenderedSource(0);
            RenderedOp downSampler =
                (RenderedOp)paramBlock.getObjectParameter(0);
            int maxLowResDim = paramBlock.getIntParameter(1);
            float minX = paramBlock.getFloatParameter(2);
            float minY = paramBlock.getFloatParameter(3);
            float height = paramBlock.getFloatParameter(4);

            // Create an image pyramid.
            ImageMIPMap pyramid = new ImageMIPMap(source, downSampler);

            // Create a Vector of RenderedImages from the pyramid.
            Vector sourceVector = new Vector();
            RenderedImage currentImage = pyramid.getCurrentImage();
            sourceVector.add(currentImage);
            while(currentImage.getWidth() > maxLowResDim ||
                  currentImage.getHeight() > maxLowResDim) {
                RenderedImage nextImage = pyramid.getDownImage();
                if(nextImage.getWidth() >= currentImage.getWidth() ||
                   nextImage.getHeight() >= currentImage.getHeight()) {
                    throw new IllegalArgumentException(JaiI18N.getString("RenderableCRIF0"));
                }
                sourceVector.add(nextImage);
                currentImage = nextImage;
            }

            // Create a RenderableImage
            mres = new MultiResolutionRenderableImage(sourceVector,
                                                      minX, minY, height);

            // Store a SoftReference to the RenderableImage in the Hashtable.
            mresTable.put(key, new SoftReference(mres));
        }

        return mres;
    }

    /**
     * Returns the source RenderedImage.
     * This method satisfies the implementation of RIF.
     */
    public RenderedImage create(ParameterBlock paramBlock,
                                RenderingHints renderHints) {
        return paramBlock.getRenderedSource(0);
    }

    /**
     * Creates a new instance of <code>AffineOpImage</code>
     * in the renderable layer. This method satisfies the
     * implementation of CRIF.
     */
    public RenderedImage create(RenderContext renderContext,
                                ParameterBlock paramBlock) {
        RenderableImage mres = createRenderable(paramBlock);

        return mres.createRendering(renderContext);
    }

    /**
     * Gets the output bounding box in rendering-independent space.
     * This method satisfies the implementation of CRIF.
     */
    public Rectangle2D getBounds2D(ParameterBlock paramBlock) {        
        RenderableImage mres = createRenderable(paramBlock);

	return new Rectangle2D.Float(mres.getMinX(), mres.getMinY(),
                                     mres.getWidth(), mres.getHeight());
    }

}
