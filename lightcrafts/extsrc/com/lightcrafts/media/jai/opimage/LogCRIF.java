/*
 * $RCSfile: LogCRIF.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:30 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.opimage;
import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import com.lightcrafts.mediax.jai.CRIFImpl;
import com.lightcrafts.mediax.jai.ImageLayout;

/**
 * This image factory supports image operator <code>LogOpImage</code>
 * in the rendered and renderable image layers.
 *
 * @since EA2
 * @see com.lightcrafts.mediax.jai.operator.LogDescriptor
 * @see LogOpImage
 *
 */

public class LogCRIF extends CRIFImpl {

    /** Constructor. */
    public LogCRIF() {
        super("log");
    }

    /**
     * Creates a new instance of <code>LogOpImage</code> in the
     * rendered layer. This method satisfies the implementation of RIF.
     *
     * @param paramBlock   The source image and the constants.
     * @param renderHints  Optionally contains destination image layout.
     */
    public RenderedImage create(ParameterBlock pb,
                                RenderingHints renderHints) {
        // Get ImageLayout from renderHints if any.
        ImageLayout layout = RIFUtil.getImageLayoutHint(renderHints);
        
        
        return new LogOpImage(pb.getRenderedSource(0), renderHints, layout);
    }
}
