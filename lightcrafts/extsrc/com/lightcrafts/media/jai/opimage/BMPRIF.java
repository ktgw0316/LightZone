/*
 * $RCSfile: BMPRIF.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:14 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.opimage;
import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderedImageFactory;

/**
 * A <code>RIF</code> supporting the "BMP" operation in the
 * rendered image layer.
 *
 * @see com.lightcrafts.mediax.jai.operator.BMPDescriptor
 *
 */
public class BMPRIF implements RenderedImageFactory {

    /** Constructor. */
    public BMPRIF() {}

    /**
     * Creates a <code>RenderedImage</code> representing the contents
     * of a BMP-encoded image.
     *
     * @param paramBlock A <code>ParameterBlock</code> containing the BMP
     *        <code>SeekableStream</code> to read.
     * @param renderHints Ignored.
     */
    public RenderedImage create(ParameterBlock paramBlock,
                                RenderingHints renderHints) {
        return CodecRIFUtil.create("bmp", paramBlock, renderHints);
    }
}
