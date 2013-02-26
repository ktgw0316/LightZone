/*
 * $RCSfile: FPXRIF.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:26 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.opimage;
import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderedImageFactory;

/**
 * A <code>RIF</code> supporting the "FPX" operation in the
 * rendered image layer.
 *
 * @see com.lightcrafts.mediax.jai.operator.FPXDescriptor
 */
public class FPXRIF implements RenderedImageFactory {

    /** Constructor. */
    public FPXRIF() {}

    /**
     * Creates a <code>RenderedImage</code> representing the contents
     * of a FlashPIX-encoded image.
     *
     * @param paramBlock A <code>ParameterBlock</code> containing the FPX
     *        <code>SeekableStream</code> to read.
     * @param renderHints Rendering hints.
     */
    public RenderedImage create(ParameterBlock paramBlock,
                                RenderingHints renderHints) {
        return CodecRIFUtil.create("fpx", paramBlock, renderHints);
    }
}
