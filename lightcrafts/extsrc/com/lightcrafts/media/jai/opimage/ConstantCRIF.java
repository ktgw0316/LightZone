/*
 * $RCSfile: ConstantCRIF.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:19 $
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
import com.lightcrafts.mediax.jai.ImageLayout;

/**
 * This image factory supports image operator <code>ConstantOpImage</code>
 * in the rendered and renderable image layers.
 *
 * @see ConstantOpImage
 */
public class ConstantCRIF extends CRIFImpl {

    private static final int DEFAULT_TILE_SIZE = 128;

    /** Constructor. */
    public ConstantCRIF() {
        super("constant");
    }

    /**
     * Creates a new instance of <code>ConstantOpImage</code>
     * in the rendered layer. This method satisfies the
     * implementation of RIF.
     */
    public RenderedImage create(ParameterBlock paramBlock,
                                RenderingHints renderHints) {
        // Get ImageLayout from renderHints if any.
        ImageLayout layout = RIFUtil.getImageLayoutHint(renderHints);

        // Get width, height, bandValues from the parameter block
        int width = Math.round(paramBlock.getFloatParameter(0));
        int height = Math.round(paramBlock.getFloatParameter(1));
        Number[] bandValues = (Number[])paramBlock.getObjectParameter(2);

        int minX = 0;
        int minY = 0;
        int tileWidth = Math.min(width, DEFAULT_TILE_SIZE);
        int tileHeight = Math.min(height, DEFAULT_TILE_SIZE);

        // Attempt to get minX, minY, tileWidth, tileHeight
        // from the ImageLayout hint
        if (layout != null) {
            if (layout.isValid(ImageLayout.MIN_X_MASK)) {
                minX = layout.getMinX(null);
            }
            if (layout.isValid(ImageLayout.MIN_Y_MASK)) {
                minY = layout.getMinY(null);
            }
            if (layout.isValid(ImageLayout.TILE_WIDTH_MASK)) {
                tileWidth = layout.getTileWidth(null);
            }
            if (layout.isValid(ImageLayout.TILE_HEIGHT_MASK)) {
                tileHeight = layout.getTileHeight(null);
            }
        }
        
        return new ConstantOpImage(minX, minY, width, height,
                                   tileWidth, tileHeight,
                                   bandValues);
    }

    /**
     * Creates a new instance of <code>ConstantOpImage</code>
     * in the renderable layer. This method satisfies the
     * implementation of CRIF.
     *
     * @pram renderContext  Rendering information.
     * @param paramBlock    The image layout for the output of
     *                      <code>ConstantOpImage</code>
     *                      and the constant pixel value.
     */
    public RenderedImage create(RenderContext renderContext,
                                ParameterBlock paramBlock) {
        float minX = 0;
        float minY = 0;
        float width = paramBlock.getFloatParameter(0);
        float height = paramBlock.getFloatParameter(1);
        Number[] bandValues = (Number[])paramBlock.getObjectParameter(2);

        AffineTransform trans = renderContext.getTransform();
        float maxX, maxY;
        float[] ptSrc = new float[8];
        float[] ptDst = new float[8];

        ptSrc[0] = minX;
        ptSrc[1] = minY;
        ptSrc[2] = minX + width;
        ptSrc[3] = minY;
        ptSrc[4] = minX + width;
        ptSrc[5] = minY + height;
        ptSrc[6] = minX;
        ptSrc[7] = minY + height;
        trans.transform(ptSrc, 0, ptDst, 0, 4);
        
        minX = Math.min(ptDst[0], ptDst[2]);
        minX = Math.min(minX, ptDst[4]);
        minX = Math.min(minX, ptDst[6]);

        maxX = Math.max(ptDst[0], ptDst[2]);
        maxX = Math.max(maxX, ptDst[4]);
        maxX = Math.max(maxX, ptDst[6]);

        minY = Math.min(ptDst[1], ptDst[3]);
        minY = Math.min(minY, ptDst[5]);
        minY = Math.min(minY, ptDst[7]);

        maxY = Math.max(ptDst[1], ptDst[3]);
        maxY = Math.max(maxY, ptDst[5]);
        maxY = Math.max(maxY, ptDst[7]);

        int iMinX = (int)minX;
        int iMinY = (int)minY;
        int iWidth = (int)maxX - iMinX;
        int iHeight = (int)maxY - iMinY;

        return new ConstantOpImage(iMinX, iMinY, iWidth, iHeight,
                                   Math.min(iWidth, DEFAULT_TILE_SIZE),
                                   Math.min(iHeight, DEFAULT_TILE_SIZE),
                                   bandValues);
    }
    
    /**
     * Gets the bounding box for the output of <code>ConstantOpImage</code>.
     * This method satisfies the implementation of CRIF.
     *
     * @param paramBlock  Image's width, height, and constant pixel values.
     */
    public Rectangle2D getBounds2D(ParameterBlock paramBlock) {
        return new Rectangle2D.Float(0, 0,
                                     paramBlock.getFloatParameter(0),
                                     paramBlock.getFloatParameter(1));
    }
}
