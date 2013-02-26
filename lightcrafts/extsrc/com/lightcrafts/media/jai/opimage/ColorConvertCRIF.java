/*
 * $RCSfile: ColorConvertCRIF.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:17 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.opimage;
import java.awt.RenderingHints;
import java.awt.image.ColorModel;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import com.lightcrafts.mediax.jai.CRIFImpl;
import com.lightcrafts.mediax.jai.ImageLayout;

/**
 * A <code>CRIF</code> supporting the "ColorConvert" operation in the rendered
 * and renderable image layers.
 *
 * @see com.lightcrafts.mediax.jai.operator.ColorConvertDescriptor
 * @see ColorConvertOpImage
 *
 * @since EA4
 *
 */
public class ColorConvertCRIF extends CRIFImpl {

    /** Constructor. */
    public ColorConvertCRIF() {
        super("colorconvert");
    }

    /**
     * Creates a new instance of <code>ColorConvertOpImage</code> in the
     * rendered layer.
     *
     * @param args   The source image and the destination ColorModel.
     * @param hints  Optionally contains destination image layout.
     */
    public RenderedImage create(ParameterBlock args,
                                RenderingHints renderHints) {
        // Get ImageLayout from renderHints if any.
        ImageLayout layout = RIFUtil.getImageLayoutHint(renderHints);
        

        return new ColorConvertOpImage(args.getRenderedSource(0),
                                       renderHints,
				       layout,
				       (ColorModel)args.getObjectParameter(0));
    }
}
