/*
 * $RCSfile: ShearRIF.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:43 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.opimage;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.DataBuffer;
import java.awt.image.MultiPixelPackedSampleModel;
import java.awt.image.SampleModel;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderedImageFactory;
import com.lightcrafts.mediax.jai.EnumeratedParameter;
import com.lightcrafts.mediax.jai.BorderExtender;
import com.lightcrafts.mediax.jai.ImageLayout;
import com.lightcrafts.mediax.jai.Interpolation;
import com.lightcrafts.mediax.jai.InterpolationBicubic2;
import com.lightcrafts.mediax.jai.InterpolationBicubic;
import com.lightcrafts.mediax.jai.InterpolationBilinear;
import com.lightcrafts.mediax.jai.InterpolationNearest;
import com.lightcrafts.mediax.jai.operator.ShearDescriptor;

/**
 * @see AffineOpimage
 */
public class ShearRIF implements RenderedImageFactory {

    /** Constructor. */
    public ShearRIF() {}

    /**
     * Creates an shear operation as an instance of AffineOpImage.
     */
    public RenderedImage create(ParameterBlock paramBlock,
                                RenderingHints renderHints) {
        // Get ImageLayout from renderHints if any.
        ImageLayout layout = RIFUtil.getImageLayoutHint(renderHints);


        // Get BorderExtender from renderHints if any.
        BorderExtender extender = RIFUtil.getBorderExtenderHint(renderHints);

        RenderedImage source = paramBlock.getRenderedSource(0);

        float shear_amt = paramBlock.getFloatParameter(0);
        EnumeratedParameter shear_dir =
            (EnumeratedParameter)paramBlock.getObjectParameter(1);

        float xTrans = paramBlock.getFloatParameter(2);
        float yTrans = paramBlock.getFloatParameter(3);

        Object arg1 = paramBlock.getObjectParameter(4);
        Interpolation interp = (Interpolation)arg1;

        double[] backgroundValues = (double[])paramBlock.getObjectParameter(5);

        // Create the affine transform
        AffineTransform tr = new AffineTransform();

        if (shear_dir.equals(ShearDescriptor.SHEAR_HORIZONTAL)) {
            // SHEAR_HORIZONTAL
            tr.setTransform(1.0, 0.0, shear_amt, 1.0, xTrans, 0.0);
        } else {
            // SHEAR_VERTICAL
            tr.setTransform(1.0, shear_amt, 0.0, 1.0, 0.0, yTrans);
        }

        // Do Affine
        if (interp instanceof InterpolationNearest) {
            SampleModel sm = source.getSampleModel();
            boolean isBinary = (sm instanceof MultiPixelPackedSampleModel) &&
                (sm.getSampleSize(0) == 1) &&
                (sm.getDataType() == DataBuffer.TYPE_BYTE ||
                 sm.getDataType() == DataBuffer.TYPE_USHORT ||
                 sm.getDataType() == DataBuffer.TYPE_INT);
            if(isBinary) {
                return new AffineNearestBinaryOpImage(source,
                                                      extender,
                                                      renderHints,
                                                      layout,
                                                      tr,
                                                      interp,
                                                      backgroundValues);
            } else {
                return new AffineNearestOpImage(source, extender,
                                                renderHints,
                                                layout,
                                                tr,
                                                interp,
                                                backgroundValues);
            }
        } else if (interp instanceof InterpolationBilinear) {
            return new AffineBilinearOpImage(source,
                                             extender,
                                             renderHints,
                                             layout,
                                             tr,
                                             interp,
                                             backgroundValues);
        } else if (interp instanceof InterpolationBicubic) {
            return new AffineBicubicOpImage(source,
                                            extender,
                                            renderHints,
                                            layout,
                                            tr,
                                            interp,
                                            backgroundValues);
        } else if (interp instanceof InterpolationBicubic2) {
            return new AffineBicubic2OpImage(source,
                                             extender,
                                             renderHints,
                                             layout,
                                             tr,
                                             interp,
                                             backgroundValues);
        } else {
            return new AffineGeneralOpImage(source,
                                            extender,
                                            renderHints,
                                            layout,
                                            tr,
                                            interp,
                                            backgroundValues);
        }
    }
}
