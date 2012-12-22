/*
 * $RCSfile: MlibRotateRIF.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.2 $
 * $Date: 2005/11/21 22:49:40 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.mlib;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderedImageFactory;
import com.lightcrafts.mediax.jai.BorderExtender;
import com.lightcrafts.mediax.jai.ImageLayout;
import com.lightcrafts.mediax.jai.Interpolation;
import com.lightcrafts.mediax.jai.InterpolationBicubic2;
import com.lightcrafts.mediax.jai.InterpolationBicubic;
import com.lightcrafts.mediax.jai.InterpolationBilinear;
import com.lightcrafts.mediax.jai.InterpolationNearest;
import com.lightcrafts.mediax.jai.InterpolationTable;
import com.lightcrafts.mediax.jai.JAI;
import com.lightcrafts.mediax.jai.OpImage;
import com.lightcrafts.mediax.jai.PlanarImage;

import com.lightcrafts.media.jai.opimage.RIFUtil;
import com.lightcrafts.media.jai.opimage.PointMapperOpImage;
import com.lightcrafts.media.jai.opimage.TranslateIntOpImage;

/**
 * A <code>RIF</code> supporting the "Rotate" operation in the
 * rendered image mode using MediaLib.
 *
 * @see com.lightcrafts.mediax.jai.operator.RotateDescriptor
 * @see MlibAffineOpimage
 *
 * @since EA4
 */
public class MlibRotateRIF implements RenderedImageFactory {

    /** Constructor. */
    public MlibRotateRIF() {}

    /**
     * Creates an rotate operation.
     */
    public RenderedImage create(ParameterBlock args,
                                RenderingHints hints) {
        /* Get ImageLayout and TileCache from RenderingHints. */
        ImageLayout layout = RIFUtil.getImageLayoutHint(hints);

        Interpolation interp = (Interpolation)args.getObjectParameter(3);
        double[] backgroundValues = (double[])args.getObjectParameter(4);

        RenderedImage source = args.getRenderedSource(0);

        if (!MediaLibAccessor.isMediaLibCompatible(args, layout) ||
            !MediaLibAccessor.hasSameNumBands(args, layout) ||
	    // Medialib cannot deal with source image having tiles with any
	    // dimension greater than or equal to 32768
	    source.getTileWidth() >= 32768 || 
	    source.getTileHeight() >= 32768) {
            return null;
        }

        /* Get BorderExtender from hints if any. */
        BorderExtender extender = RIFUtil.getBorderExtenderHint(hints);

        float x_center = args.getFloatParameter(0);
        float y_center = args.getFloatParameter(1);
        float angle = args.getFloatParameter(2);

        /*
         * Convert angle to degrees (within some precision) given PI's
         * transcendantal nature. All this, to check if we can call
         * simpler methods like Copy or Transpose for certain angles
         * viz., 0, 90, 180, 270, 360, 450, .....
         */
        double tmp_angle = 180.0F * angle / Math.PI;
        double rnd_angle = Math.round(tmp_angle);

        /* Represent the angle as an AffineTransform. */
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
                return new MlibCopyOpImage(source, hints, layout);
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

                RenderedImage trans =
                    new MlibTransposeOpImage(source, hints, layout, transType);

                // Determine current image origin
                int imMinX = trans.getMinX();
                int imMinY = trans.getMinY();

                // Translate image and return it
		// TranslateIntOpImage can't deal with ImageLayout hint
		if (layout == null) {
		    OpImage intermediateImage =
                        new TranslateIntOpImage(trans,
                                                hints,
                                                rotMinX - imMinX,
                                                rotMinY - imMinY);
                    try {
                        return new PointMapperOpImage(intermediateImage,
                                                      hints,
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
                        JAI.create("scale", pbScale, hints).getRendering();
                    try {
                        return new PointMapperOpImage(intermediateImage,
                                                      hints,
                                                      transform);
                    } catch(NoninvertibleTransformException nite) {
                        return intermediateImage;
                    }
		}
            }
        }

        /*
         * At this point we know that we cannot call other operations.
         * Have to do Affine.
         */

        /* Do the Affine operation. */
        if (interp instanceof InterpolationNearest) {
            return new MlibAffineNearestOpImage(source, extender,
                                                hints, layout,
                                                transform,
                                                interp,
                                                backgroundValues);
        } else if (interp instanceof InterpolationBilinear) {
            return new MlibAffineBilinearOpImage(source,
                                                 extender, hints, layout,
                                                 transform,
                                                 interp,
                                                 backgroundValues);
        } else if (interp instanceof InterpolationBicubic ||
                   interp instanceof InterpolationBicubic2) {
            return new MlibAffineBicubicOpImage(source,
                                                extender, hints, layout,
                                                transform,
                                                interp,
                                                backgroundValues);
        } else if (interp instanceof InterpolationTable) {
            return new MlibAffineTableOpImage(source,
                                              extender, hints, layout,
                                              transform,
                                              interp,
                                              backgroundValues);
        } else {
            return null;
        }
    }
}
