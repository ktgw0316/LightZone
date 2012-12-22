/*
 * $RCSfile: RotateCRIF.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.2 $
 * $Date: 2005/11/21 22:49:40 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.opimage;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.image.DataBuffer;
import java.awt.image.MultiPixelPackedSampleModel;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.renderable.RenderableImage;
import java.awt.image.renderable.RenderContext;
import java.awt.image.renderable.ParameterBlock;
import com.lightcrafts.mediax.jai.CRIFImpl;
import com.lightcrafts.mediax.jai.BorderExtender;
import com.lightcrafts.mediax.jai.ImageLayout;
import com.lightcrafts.mediax.jai.Interpolation;
import com.lightcrafts.mediax.jai.InterpolationBicubic;
import com.lightcrafts.mediax.jai.InterpolationBicubic2;
import com.lightcrafts.mediax.jai.InterpolationBilinear;
import com.lightcrafts.mediax.jai.InterpolationNearest;
import com.lightcrafts.mediax.jai.JAI;
import com.lightcrafts.mediax.jai.OpImage;
import com.lightcrafts.mediax.jai.PlanarImage;

import com.lightcrafts.media.jai.opimage.PointMapperOpImage;

import java.awt.geom.Rectangle2D;
import java.awt.geom.Point2D;

/**
 * @since EA4
 * @see AffineOpimage
 */
public class RotateCRIF extends CRIFImpl {

    /** Constructor. */
    public RotateCRIF() {
        super("rotate");
    }

    /**
     * Creates an rotate operation.
     */
    public RenderedImage create(ParameterBlock paramBlock,
                                RenderingHints renderHints) {
        // Get ImageLayout from renderHints if any.
        ImageLayout layout = RIFUtil.getImageLayoutHint(renderHints);


        // Get BorderExtender from renderHints if any.
        BorderExtender extender = RIFUtil.getBorderExtenderHint(renderHints);

        RenderedImage source = paramBlock.getRenderedSource(0);

        float x_center = paramBlock.getFloatParameter(0);
        float y_center = paramBlock.getFloatParameter(1);
        float angle = paramBlock.getFloatParameter(2);

        Object arg1 = paramBlock.getObjectParameter(3);
        Interpolation interp = (Interpolation)arg1;

        double[] backgroundValues = (double[]) paramBlock.getObjectParameter(4);

        SampleModel sm = source.getSampleModel();
        boolean isBinary = (sm instanceof MultiPixelPackedSampleModel) &&
            (sm.getSampleSize(0) == 1) &&
            (sm.getDataType() == DataBuffer.TYPE_BYTE ||
             sm.getDataType() == DataBuffer.TYPE_USHORT ||
             sm.getDataType() == DataBuffer.TYPE_INT);

        //
        // Convert angle to degrees (within some precision) given PI's
        // transcendental nature. All this, to check if we can call
        // simpler methods like Copy or Transpose for certain angles
        // viz., 0, 90, 180, 270, 360, 450, .....
        //
        double tmp_angle = (180.0/Math.PI)*angle;
        double rnd_angle = Math.round(tmp_angle);

        //
        // Represent the angle as an AffineTransform
        //
        AffineTransform transform =
            AffineTransform.getRotateInstance(angle, x_center, y_center);

        // Check if angle is (nearly) integral
        if (Math.abs(rnd_angle - tmp_angle) < 0.0001) {
            int dangle = (int)rnd_angle % 360;

            // Shift dangle into the range [0..359].
            if (dangle < 0) {
                dangle += 360;
            }

            //
            // Do a copy if angle is 0 degrees or
            // multiple of 360 degrees
            //
            if (dangle == 0) {
                return new CopyOpImage(source, renderHints, layout);
            }

            int ix_center = (int)Math.round(x_center);
            int iy_center = (int)Math.round(y_center);

            // Do a transpose if angle is mutiple of 270, 180, 90 degrees
            // and the translation is (nearly) integral.
            if (((dangle % 90) == 0) &&
                (Math.abs(x_center - ix_center) < 0.0001) &&
                (Math.abs(y_center - iy_center) < 0.0001)) {

                int transType = -1;
                int rotMinX = 0;
                int rotMinY = 0;

                int sourceMinX = source.getMinX();
                int sourceMinY = source.getMinY();
                int sourceMaxX = sourceMinX + source.getWidth();
                int sourceMaxY = sourceMinY + source.getHeight();

                if (dangle == 90) {
                    transType = 4;
                    rotMinX = ix_center - (sourceMaxY - iy_center);
                    rotMinY = iy_center - (ix_center - sourceMinX);
                } else if (dangle == 180) {
                    transType = 5;
                    rotMinX = 2*ix_center - sourceMaxX;
                    rotMinY = 2*iy_center - sourceMaxY;
                } else { // dangle == 270
                    transType = 6;
                    rotMinX = ix_center - (iy_center - sourceMinY);
                    rotMinY = iy_center - (sourceMaxX - ix_center);
                }

                RenderedImage trans;
                if (isBinary) {
                    trans = new TransposeBinaryOpImage(source, renderHints, layout,
                                                       transType);
                } else {
                    trans = new TransposeOpImage(source, renderHints, layout, transType);
                }

                // Determine current image origin
                int imMinX = trans.getMinX();
                int imMinY = trans.getMinY();

		// TranslateIntOpImage can't deal with ImageLayout hint
		if (layout == null) {
		    // Translate image and return it
                    OpImage intermediateImage =
                        new TranslateIntOpImage(trans,
                                                renderHints,
                                                rotMinX - imMinX,
                                                rotMinY - imMinY);
                    try {
                        return new PointMapperOpImage(intermediateImage,
                                                      renderHints,
                                                      transform);
                    } catch(NoninvertibleTransformException nite) {
                        return intermediateImage;
                    }
		} else {
		    ParameterBlock pbScale = new ParameterBlock();
		    pbScale.addSource(trans);
		    pbScale.add(0F);
		    pbScale.add(0F);
		    pbScale.add(rotMinX - imMinX);
		    pbScale.add(rotMinY - imMinY);
		    pbScale.add(interp);
                    PlanarImage intermediateImage =
                        JAI.create("scale", pbScale,
                                   renderHints).getRendering();
                    try {
                        return new PointMapperOpImage(intermediateImage,
                                                      renderHints,
                                                      transform);
                    } catch(NoninvertibleTransformException nite) {
                        return intermediateImage;
                    }
		}
            }
        }

        //
        // At this point we know that we cannot call other operations.
        // Have to do Affine.
        //

        //
        // Do the Affine operation
        //
        if (interp instanceof InterpolationNearest) {
            if (isBinary) {
                return new AffineNearestBinaryOpImage(source, extender,
                                                       renderHints,
                                                       layout,
                                                       transform,
                                                       interp,
                                                       backgroundValues);
            } else {
                return new AffineNearestOpImage(source, extender,
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
        float x_center = paramBlock.getFloatParameter(0);
        float y_center = paramBlock.getFloatParameter(1);
        float angle = paramBlock.getFloatParameter(2);

        AffineTransform rotate =
            AffineTransform.getRotateInstance(angle, x_center, y_center);

        RenderContext RC = (RenderContext)renderContext.clone();
        AffineTransform usr2dev = RC.getTransform();
        usr2dev.concatenate(rotate);
	RC.setTransform(usr2dev);
	return RC;
    }

    /**
     * Gets the bounding box for the output of <code>TranslateOpImage</code>.
     * This method satisfies the implementation of CRIF.
     */
    public Rectangle2D getBounds2D(ParameterBlock paramBlock) {
        RenderableImage source = paramBlock.getRenderableSource(0);

        float x_center = paramBlock.getFloatParameter(0);
        float y_center = paramBlock.getFloatParameter(1);
        float angle = paramBlock.getFloatParameter(2);
        Interpolation interp = (Interpolation)paramBlock.getObjectParameter(3);

        //
        // Convert angle to degrees (within some precision) given PI's
        // transcendantal nature. All this, to check if we can call
        // simpler methods like Copy or Transpose for certain angles
        // viz., 0, 90, 180, 270, 360, 450, .....
        //
        int dangle = 0;
        double tmp_angle = 180.0F * angle / Math.PI;
        double rnd_angle = Math.round(tmp_angle);

        if (Math.abs(rnd_angle - tmp_angle) < 0.0001) {
            dangle = (int) rnd_angle;
        } else {
            dangle = (int) tmp_angle;
        }

        //
        // It's a copy if angle is 0 degrees or multiple of 360 degrees
        //
        if (dangle % 360 == 0) {
            return new Rectangle2D.Float(source.getMinX(),
                                         source.getMinY(),
                                         source.getWidth(),
                                         source.getHeight());
        }

        //
        // It's a transpose if angle is mutiple of 270, 180, 90 degrees
        //
        float x0 = (float)source.getMinX();
        float y0 = (float)source.getMinY();
        float s_width = (float)source.getWidth();
        float s_height = (float)source.getHeight();
        float x1 = x0 + s_width - 1;
        float y1 = y0 + s_height - 1;

        float tx0 = 0;
        float ty0 = 0;
        float tx1 = 0;
        float ty1 = 0;

        if (dangle % 270 == 0) {
            if (dangle < 0) {
                // -270 degrees
                tx0 = s_height - y1 - 1;
                ty0 = x0;
                tx1 = s_height - y0 - 1;
                ty1 = x1;
                return new Rectangle2D.Float(tx0,
                                             ty0,
                                             tx1 - tx0 + 1,
                                             ty1 - ty0 + 1);
            } else {
                // 270 degrees
                tx0 = y0;
                ty0 = s_width - x1 - 1;
                tx1 = y1;
                ty1 = s_width - x0 - 1;
                return new Rectangle2D.Float(tx0,
                                             ty0,
                                             tx1 - tx0 + 1,
                                             ty1 - ty0 + 1);
            }
        }

        if (dangle % 180 == 0) {
            tx0 = s_width - x1 - 1;
            ty0 = s_height - y1 - 1;
            tx1 = s_width - x0 - 1;
            ty1 = s_height - y0 - 1;
            // 180 degrees
            return new Rectangle2D.Float(tx0,
                                         ty0,
                                         tx1 - tx0 + 1,
                                         ty1 - ty0 + 1);
        }

        if (dangle % 90 == 0) {
            if (dangle < 0) {
                // -90 degrees
                tx0 = y0;
                ty0 = s_width - x1 - 1;
                tx1 = y1;
                ty1 = s_width - x0 - 1;
                return new Rectangle2D.Float(tx0,
                                             ty0,
                                             tx1 - tx0 + 1,
                                             ty1 - ty0 + 1);
            } else {
                // 90 degrees
                tx0 = s_height - y1 - 1;
                ty0 = x0;
                tx1 = s_height - y0 - 1;
                ty1 = x1;
                return new Rectangle2D.Float(tx0,
                                             ty0,
                                             tx1 - tx0 + 1,
                                             ty1 - ty0 + 1);
            }
        }

        //
        // It's a Affine
        //
        AffineTransform rotate =
            AffineTransform.getRotateInstance(angle, x_center, y_center);

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
        rotate.transform(pts, 0, pts, 0, 4);

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
