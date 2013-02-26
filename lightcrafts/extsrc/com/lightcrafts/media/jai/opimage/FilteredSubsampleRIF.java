/*
 * $RCSfile: FilteredSubsampleRIF.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:27 $
 * $State: Exp $
 */package com.lightcrafts.media.jai.opimage;
import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.renderable.RenderedImageFactory;
import java.awt.image.renderable.ParameterBlock;
import java.util.Map;
import com.lightcrafts.mediax.jai.BorderExtender;
import com.lightcrafts.mediax.jai.ImageLayout;
import com.lightcrafts.mediax.jai.Interpolation;
import com.lightcrafts.mediax.jai.InterpolationNearest;
import com.lightcrafts.mediax.jai.InterpolationBilinear;
import com.lightcrafts.mediax.jai.InterpolationBicubic;
import com.lightcrafts.mediax.jai.InterpolationBicubic2;
import com.lightcrafts.mediax.jai.JAI;

/**
 * <p> Class implementing the RIF interface for the FilteredSubsample
 * operator.  An instance of this class should be registered with the
 * OperationRegistry with operation name "FilteredSubsample."
 */
public class FilteredSubsampleRIF implements RenderedImageFactory {

    /** <p> Default constructor (there is no input). */
    public FilteredSubsampleRIF() {}

    /**
     * <p> Creates a new instance of SubsampleOpImage in the rendered layer.
     * This method satisfies the implementation of RIF.
     *
     * @param paramBlock  The source image, the X and Y scale factors.
     * @param renderHints RenderingHints.
     */
    public RenderedImage create(ParameterBlock paramBlock,
                                RenderingHints renderHints) {
        RenderedImage source = paramBlock.getRenderedSource(0);

        BorderExtender extender = renderHints == null ? null :
            (BorderExtender)renderHints.get(JAI.KEY_BORDER_EXTENDER);
        ImageLayout layout = renderHints == null ? null :
            (ImageLayout)renderHints.get(JAI.KEY_IMAGE_LAYOUT);

        int scaleX = paramBlock.getIntParameter(0);
        int scaleY = paramBlock.getIntParameter(1);
        float [] qsFilter = (float [])paramBlock.getObjectParameter(2);
        Interpolation interp = (Interpolation)paramBlock.getObjectParameter(3);

        // check if binary and interpolation type allowed
	SampleModel sm = source.getSampleModel();
        int dataType = sm.getDataType();

        // Determine the interpolation type, if not supported throw exception
	boolean validInterp = (interp instanceof InterpolationNearest)  ||
                              (interp instanceof InterpolationBilinear) ||
                              (interp instanceof InterpolationBicubic)  ||
                              (interp instanceof InterpolationBicubic2);

        if (!validInterp)
            throw new IllegalArgumentException(
	        JaiI18N.getString("FilteredSubsample3"));

        return new FilteredSubsampleOpImage(source, extender, (Map)renderHints, layout,
                                    scaleX, scaleY, qsFilter, interp);
    } // create

} // FilteredSubsampleRIF
