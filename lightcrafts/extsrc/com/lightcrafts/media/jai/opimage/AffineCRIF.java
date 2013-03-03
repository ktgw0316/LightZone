/*
 * $RCSfile: AffineCRIF.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:13 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.opimage;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.DataBuffer;
import java.awt.image.MultiPixelPackedSampleModel;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.renderable.RenderableImage;
import java.awt.image.renderable.RenderContext;
import java.awt.image.renderable.ParameterBlock;
import com.lightcrafts.mediax.jai.BorderExtender;
import com.lightcrafts.mediax.jai.ImageLayout;
import com.lightcrafts.mediax.jai.Interpolation;
import com.lightcrafts.mediax.jai.InterpolationBicubic;
import com.lightcrafts.mediax.jai.InterpolationBicubic2;
import com.lightcrafts.mediax.jai.InterpolationBilinear;
import com.lightcrafts.mediax.jai.InterpolationNearest;
import com.lightcrafts.mediax.jai.TileCache;
import com.lightcrafts.mediax.jai.CRIFImpl;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Point2D;

/**
 * @since EA4
 * @see AffineOpimage, ScaleOpImage
 */
public class AffineCRIF extends CRIFImpl {

    private static final float TOLERANCE = 0.01F;

    /** Constructor. */
    public AffineCRIF() {
        super("affine");
    }

    /**
     * Creates an affine operation as an instance of AffineOpImage.
     */
    public RenderedImage create(ParameterBlock paramBlock,
                                RenderingHints renderHints) {
        // Get ImageLayout from renderHints if any.
        ImageLayout layout = RIFUtil.getImageLayoutHint(renderHints);

        // Get TileCache from renderHints if any.
        TileCache cache = RIFUtil.getTileCacheHint(renderHints);

        // Get BorderExtender from renderHints if any.
        BorderExtender extender = RIFUtil.getBorderExtenderHint(renderHints);

        RenderedImage source = paramBlock.getRenderedSource(0);

        Object arg0 = paramBlock.getObjectParameter(0);
        AffineTransform transform = (AffineTransform)arg0;

        Object arg1 = paramBlock.getObjectParameter(1);
        Interpolation interp = (Interpolation)arg1;

	double[] backgroundValues = (double[]) paramBlock.getObjectParameter(2);

        SampleModel sm = source.getSampleModel();
        boolean isBinary = (sm instanceof MultiPixelPackedSampleModel) &&
            (sm.getSampleSize(0) == 1) &&
            (sm.getDataType() == DataBuffer.TYPE_BYTE ||
             sm.getDataType() == DataBuffer.TYPE_USHORT ||
             sm.getDataType() == DataBuffer.TYPE_INT);

        // Get the affine transform
        double tr[];
        tr = new double[6];
        transform.getMatrix(tr);

        //
        // Check and see if the affine transform is doing a copy.
        // If so call the copy operation.
        //
        if ((tr[0] == 1.0) &&
            (tr[3] == 1.0) &&
            (tr[2] == 0.0) &&
            (tr[1] == 0.0) &&
            (tr[4] == 0.0) &&
            (tr[5] == 0.0)) {
            // It's a copy
            return new CopyOpImage(source, renderHints, layout);
        }

        //
        // Check and see if the affine transform is in fact doing
        // a Translate operation. That is a scale by 1 and no rotation.
        // In which case call translate. Note that only integer translate
        // is applicable. For non-integer translate we'll have to do the
        // affine.
        // If the hints contain an ImageLayout hint, we can't use 
	// TranslateIntOpImage since it isn't capable of dealing with that.
        if ((tr[0] == 1.0) &&
            (tr[3] == 1.0) &&
            (tr[2] == 0.0) &&
            (tr[1] == 0.0) &&
            (Math.abs(tr[4] - (int) tr[4]) < TOLERANCE) &&
            (Math.abs(tr[5] - (int) tr[5]) < TOLERANCE) &&
	    layout == null) {
            // It's a integer translate
            return new TranslateIntOpImage(source,
					   renderHints,
                                           (int) tr[4],
                                           (int) tr[5]);
        }

        //
        // Check and see if the affine transform is in fact doing
        // a Scale operation. In which case call Scale which is more
        // optimized than Affine.
        //
        if ((tr[0] > 0.0) &&
            (tr[2] == 0.0) &&
            (tr[1] == 0.0) &&
            (tr[3] > 0.0)) {
            // It's a scale
            if (interp instanceof InterpolationNearest) {
                if (isBinary) {
                    return new ScaleNearestBinaryOpImage(source,
                                                          extender,
                                                          renderHints,
                                                          layout,
                                                          (float)tr[0],
                                                          (float)tr[3],
                                                          (float)tr[4],
                                                          (float)tr[5],
                                                          interp);
                } else {
                    return new ScaleNearestOpImage(source,
                                                   extender,
                                                   renderHints,
                                                   layout,
                                                   (float)tr[0], // xScale
                                                   (float)tr[3], // yScale
                                                   (float)tr[4], // xTrans
                                                   (float)tr[5], // yTrans
                                                   interp);
                }
            } else if (interp instanceof InterpolationBilinear) {
                if (isBinary) {
                    return new ScaleBilinearBinaryOpImage(source,
						     extender,
						     renderHints,
						     layout,
						     (float)tr[0],
						     (float)tr[3],
						     (float)tr[4],
						     (float)tr[5],
						     interp);
                } else {

		  return new ScaleBilinearOpImage(source,
						  extender,
						  renderHints,
						  layout,
						  (float)tr[0], // xScale
						  (float)tr[3], // yScale
						  (float)tr[4], // xTrans
						  (float)tr[5], // yTrans
						  interp);
		}
            } else if ((interp instanceof InterpolationBicubic) ||
                       (interp instanceof InterpolationBicubic2)) {
                return new ScaleBicubicOpImage(source,
                                               extender,
                                               renderHints,
                                               layout,
                                               (float)tr[0], // xScale
                                               (float)tr[3], // yScale
                                               (float)tr[4], // xTrans
                                               (float)tr[5], // yTrans
                                               interp);
            } else {
                return new ScaleGeneralOpImage(source,
                                               extender,
                                               renderHints,
                                               layout,
                                               (float)tr[0], // xScale
                                               (float)tr[3], // yScale
                                               (float)tr[4], // xTrans
                                               (float)tr[5], // yTrans
                                               interp);
            }
        }

        // Have to do Affine
        if (interp instanceof InterpolationNearest) {
            if (isBinary) {
                return new AffineNearestBinaryOpImage(source,
                                                       extender,
                                                       renderHints,
                                                       layout,
                                                       transform,
                                                       interp,
                                                       backgroundValues);
            } else {
                return new AffineNearestOpImage(source,
                                                extender,
                                                renderHints,
                                                layout,
                                                transform,
                                                interp,
                                                backgroundValues);
            }
        } else if (interp instanceof InterpolationBilinear) {
            return new AffineBilinearOpImage(source,
                                             extender,
                                             renderHints,
                                             layout,
                                             transform,
                                             interp,
                                             backgroundValues);
        } else if (interp instanceof InterpolationBicubic) {
            return new AffineBicubicOpImage(source,
                                            extender,
                                            renderHints,
                                            layout,
                                            transform,
                                            interp,
                                            backgroundValues);
        } else if (interp instanceof InterpolationBicubic2) {
            return new AffineBicubic2OpImage(source,
                                             extender,
                                             renderHints,
                                             layout,
                                             transform,
                                             interp,
                                             backgroundValues);
        } else {
            return new AffineGeneralOpImage(source,
                                            extender,
                                            renderHints,
                                            layout,
                                            transform,
                                            interp,
					    backgroundValues);
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
        Object arg0 = paramBlock.getObjectParameter(0);
        AffineTransform affine = (AffineTransform)arg0;

        RenderContext RC = (RenderContext)renderContext.clone();
        AffineTransform usr2dev = RC.getTransform();
        usr2dev.concatenate(affine);
	RC.setTransform(usr2dev);
	return RC;
    }

    /**
     * Gets the bounding box for the output of <code>AffineOpImage</code>.
     * This method satisfies the implementation of CRIF.
     */
    public Rectangle2D getBounds2D(ParameterBlock paramBlock) {
        RenderableImage source = paramBlock.getRenderableSource(0);
        Object arg0 = paramBlock.getObjectParameter(0);
        AffineTransform forward_tr = (AffineTransform)arg0;

        Object arg1 = paramBlock.getObjectParameter(1);
        Interpolation interp = (Interpolation)arg1;

        // Get the affine transform
        double tr[];
        tr = new double[6];
        forward_tr.getMatrix(tr);

        //
        // Check and see if the affine transform is doing a copy.
        //
        if ((tr[0] == 1.0) &&
            (tr[3] == 1.0) &&
            (tr[2] == 0.0) &&
            (tr[1] == 0.0) &&
            (tr[4] == 0.0) &&
            (tr[5] == 0.0)) {
            return new Rectangle2D.Float(source.getMinX(),
                                         source.getMinY(),
                                         source.getWidth(),
                                         source.getHeight());
        }

        //
        // Check and see if the affine transform is in fact doing
        // a Translate operation.
        //
        if ((tr[0] == 1.0) &&
            (tr[3] == 1.0) &&
            (tr[2] == 0.0) &&
            (tr[1] == 0.0) &&
            (Math.abs(tr[4] - (int) tr[4]) < TOLERANCE) &&
            (Math.abs(tr[5] - (int) tr[5]) < TOLERANCE)) {
            return new Rectangle2D.Float(source.getMinX() + (float)tr[4],
                                         source.getMinY() + (float)tr[5],
                                         source.getWidth(),
                                         source.getHeight());
        }

        //
        // Check and see if the affine transform is in fact doing
        // a Scale operation.
        //
        if ((tr[0] > 0.0) &&
            (tr[2] == 0.0) &&
            (tr[1] == 0.0) &&
            (tr[3] > 0.0)) {
            // Get the source dimensions
            float x0 = (float)source.getMinX();
            float y0 = (float)source.getMinY() ;
            float w = (float)source.getWidth();
            float h = (float)source.getHeight();

            // Forward map the source using x0, y0, w and h
            float d_x0 = x0 * (float)tr[0] + (float)tr[4];
            float d_y0 = y0 * (float)tr[3] + (float)tr[5];
            float d_w = w * (float)tr[0];
            float d_h = h * (float)tr[3];

            return new Rectangle2D.Float(d_x0,
					 d_y0,
					 d_w,
                                         d_h);
        }

        // It's an Affine

        //
        // Get sx0,sy0 coordinates and width & height of the source
        //
        float sx0 = (float) source.getMinX();
        float sy0 = (float) source.getMinY();
        float sw = (float) source.getWidth();
        float sh = (float) source.getHeight();

        //
        // The 4 points (clockwise order) are
        //      (sx0, sy0),    (sx0+sw, sy0)
        //      (sx0, sy0+sh), (sx0+sw, sy0+sh)
        //
        Point2D[] pts = new Point2D[4];
        pts[0] = new Point2D.Float(sx0, sy0);
        pts[1] = new Point2D.Float((sx0+sw), sy0);
        pts[2] = new Point2D.Float((sx0+sw), (sy0+sh));
        pts[3] = new Point2D.Float(sx0, (sy0+sh));

        // Forward map
        forward_tr.transform(pts, 0, pts, 0, 4);

        float dx0 =  Float.MAX_VALUE;
        float dy0 =  Float.MAX_VALUE;
        float dx1 = -Float.MAX_VALUE;
        float dy1 = -Float.MAX_VALUE;
        for (int i = 0; i < 4; i++) {
            float px = (float)pts[i].getX();
            float py = (float)pts[i].getY();

            dx0 = Math.min(dx0, px);
            dy0 = Math.min(dy0, py);
            dx1 = Math.max(dx1, px);
            dy1 = Math.max(dy1, py);
        }

        //
        // Get the width & height of the resulting bounding box.
        // This is set on the layout
        //
        float lw = dx1 - dx0;
        float lh = dy1 - dy0;

        return new Rectangle2D.Float(dx0, dy0, lw, lh);
    }
}
