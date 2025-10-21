/* Copyright (C) 2005-2011 Fabio Riccardi */

/*
 * $RCSfile: LCColorConvertCRIF.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.2 $
 * $Date: 2005/03/28 17:45:12 $
 * $State: Exp $
 */

package com.lightcrafts.jai.opimage;

import java.awt.RenderingHints;
import java.awt.color.ICC_Profile;
import java.awt.image.ColorModel;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import org.eclipse.imagen.CRIFImpl;
import org.eclipse.imagen.ImageLayout;
import org.eclipse.imagen.media.opimage.RIFUtil;
import com.lightcrafts.jai.operator.LCMSColorConvertDescriptor;


/**
 * A <code>CRIF</code> supporting the "ColorConvert" operation in the rendered
 * and renderable image layers.
 *
 * @see org.eclipse.imagen.operator.ColorConvertDescriptor
 * @see org.eclipse.imagen.media.opimage.ColorConvertOpImage
 *
 * @since EA4
 *
 */
public class LCMSColorConvertCRIF extends CRIFImpl {

    /** Constructor. */
    public LCMSColorConvertCRIF() {
        super("LCMSColorConvert");
    }

    /**
     * Creates a new instance of <code>ColorConvertOpImage</code> in the
     * rendered layer.
     *
     * @param args         The source image and the destination ColorModel.
     * @param renderHints  Optionally contains destination image layout.
     */
    @Override
    public RenderedImage create(ParameterBlock args,
                                RenderingHints renderHints) {
        // Get ImageLayout from renderHints if any.
        ImageLayout layout = RIFUtil.getImageLayoutHint(renderHints);

        return new LCMSColorConvertOpImage(args.getRenderedSource(0),
                                       renderHints,
				       layout,
				       (ColorModel) args.getObjectParameter(0),
                                       (LCMSColorConvertDescriptor.RenderingIntent) args.getObjectParameter(1),
                                       (ICC_Profile) args.getObjectParameter(2),
                                       (LCMSColorConvertDescriptor.RenderingIntent) args.getObjectParameter(3));
    }
}
