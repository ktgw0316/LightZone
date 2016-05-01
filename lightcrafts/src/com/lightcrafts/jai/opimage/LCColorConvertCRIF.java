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
import java.awt.image.ColorModel;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import com.lightcrafts.mediax.jai.CRIFImpl;
import com.lightcrafts.mediax.jai.ImageLayout;
import com.lightcrafts.media.jai.opimage.RIFUtil;
import com.lightcrafts.jai.operator.LCColorConvertDescriptor;


/**
 * A <code>CRIF</code> supporting the "ColorConvert" operation in the rendered
 * and renderable image layers.
 *
 * @see com.lightcrafts.mediax.jai.operator.ColorConvertDescriptor
 * @see com.lightcrafts.media.jai.opimage.ColorConvertOpImage
 *
 * @since EA4
 *
 */
public class LCColorConvertCRIF extends CRIFImpl {

    /** Constructor. */
    public LCColorConvertCRIF() {
        super("LCColorConvert");
    }

    /**
     * Creates a new instance of <code>ColorConvertOpImage</code> in the
     * rendered layer.
     *
     * @param args        The source image and the destination ColorModel.
     * @param renderHints Optionally contains destination image layout.
     */
    public RenderedImage create(ParameterBlock args,
                                RenderingHints renderHints) {
        // Get ImageLayout from renderHints if any.
        ImageLayout layout = RIFUtil.getImageLayoutHint(renderHints);


        return new LCColorConvertOpImage(args.getRenderedSource(0),
                                       renderHints,
				       layout,
				       (ColorModel) args.getObjectParameter(0),
                                       (LCColorConvertDescriptor.RenderingIntent) args.getObjectParameter(1));
    }
}
