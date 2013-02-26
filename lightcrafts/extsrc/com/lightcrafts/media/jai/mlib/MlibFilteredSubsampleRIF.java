/*
 * $RCSfile: MlibFilteredSubsampleRIF.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:55:57 $
 * $State: Exp $
 */package com.lightcrafts.media.jai.mlib;
import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.DataBuffer;
import java.awt.image.SampleModel;
import java.awt.image.MultiPixelPackedSampleModel;
import java.awt.image.renderable.RenderedImageFactory;
import java.awt.image.renderable.ParameterBlock;
import java.util.Map;
import com.lightcrafts.mediax.jai.BorderExtender;
import com.lightcrafts.mediax.jai.ImageLayout;
import com.lightcrafts.mediax.jai.Interpolation;
import com.lightcrafts.mediax.jai.JAI;

/**
 * <p> Class implementing the RIF interface for the MlibFilteredSubsample
 * operator.  An instance of this class should be registered with the
 * OperationRegistry with operation name "FilteredSubsample."
 *
 * <p> This code is very similar to FilteredSubsampleRIF.
 */
public class MlibFilteredSubsampleRIF implements RenderedImageFactory {

    /** Default constructor (there is no input). */
    public MlibFilteredSubsampleRIF() {}

    /**
     * Creates a new instance of SubsampleOpImage in the rendered layer.
     * This method satisfies the implementation of RIF.
     *
     * @param paramBlock  The source image, the X and Y scale factors.
     * @param renderHints RenderingHints.
     */
    public RenderedImage create(ParameterBlock paramBlock,
                                RenderingHints renderHints) {

        BorderExtender extender = renderHints == null ? null :
            (BorderExtender)renderHints.get(JAI.KEY_BORDER_EXTENDER);
        ImageLayout layout = renderHints == null ? null :
            (ImageLayout)renderHints.get(JAI.KEY_IMAGE_LAYOUT);

        // Test for media lib compatibility
        if (!MediaLibAccessor.isMediaLibCompatible(paramBlock, layout) ||
            !MediaLibAccessor.hasSameNumBands(paramBlock, layout)) {
            return null;
        }

        RenderedImage source = paramBlock.getRenderedSource(0);
        SampleModel sm = source.getSampleModel();
        boolean isBilevel = (sm instanceof MultiPixelPackedSampleModel) &&
            (sm.getSampleSize(0) == 1) &&
            (sm.getDataType() == DataBuffer.TYPE_BYTE || 
             sm.getDataType() == DataBuffer.TYPE_USHORT || 
             sm.getDataType() == DataBuffer.TYPE_INT);
        if (isBilevel) {
            // Let Java code handle it, reformatting is slower
            return null;
        }

        int scaleX = paramBlock.getIntParameter(0);
        int scaleY = paramBlock.getIntParameter(1);
        float [] qsFilter = (float [])paramBlock.getObjectParameter(2);
        Interpolation interp = (Interpolation)paramBlock.getObjectParameter(3);

        return new MlibFilteredSubsampleOpImage(source, extender, (Map)renderHints, layout,
                                    scaleX, scaleY, qsFilter, interp);
    } // create

} // MlibFilteredSubsampleRIF
