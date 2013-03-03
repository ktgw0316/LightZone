/*
 * $RCSfile: MlibCompositeRIF.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:55:52 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.mlib;
import java.awt.RenderingHints;
import java.awt.image.ComponentSampleModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderedImageFactory;
import com.lightcrafts.mediax.jai.EnumeratedParameter;
import com.lightcrafts.mediax.jai.ImageLayout;
import com.lightcrafts.mediax.jai.operator.CompositeDescriptor;
import com.lightcrafts.media.jai.opimage.RIFUtil;

/**
 * A <code>RIF</code> supporting the "Composite" operation in the
 * rendered image mode using MediaLib.
 *
 * @see com.lightcrafts.mediax.jai.operator.CompositeDescriptor
 * @see MlibCompositeOpImage
 *
 * @since 1.0
 *
 */
public class MlibCompositeRIF implements RenderedImageFactory {

    /** Constructor. */
    public MlibCompositeRIF() {}

    /**
     * Creates a new instance of <code>MlibCompositeOpImage</code> in
     * the rendered image mode.
     *
     * @param args  The source images.
     * @param hints  May contain rendering hints and destination image layout.
     */
    public RenderedImage create(ParameterBlock args,
                                RenderingHints hints) {
        /* Get ImageLayout and TileCache from RenderingHints. */
        ImageLayout layout = RIFUtil.getImageLayoutHint(hints);
        

        if (!MediaLibAccessor.isMediaLibCompatible(args, layout) ||
            !MediaLibAccessor.hasSameNumBands(args, layout)) {
            return null;
        }

        RenderedImage alpha1 = (RenderedImage)args.getObjectParameter(0);
        Object alpha2 = args.getObjectParameter(1);
        boolean premultiplied =
            ((Boolean)args.getObjectParameter(2)).booleanValue();
        EnumeratedParameter destAlpha =
            (EnumeratedParameter)args.getObjectParameter(3);

        SampleModel sm = alpha1.getSampleModel();

        if (!(sm instanceof ComponentSampleModel) ||
            sm.getNumBands() != 1 ||
            !(alpha1.getColorModel() instanceof ComponentColorModel) ||
            alpha2 != null ||
            premultiplied ||
            !(destAlpha.equals(CompositeDescriptor.NO_DESTINATION_ALPHA))) {
            return null;
        }
            

        return new MlibCompositeOpImage(args.getRenderedSource(0),
                                        args.getRenderedSource(1),
                                        hints, layout,
                                        alpha1);
    }
}
