/*
 * $RCSfile: JPEGRIF.java,v $
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
import java.awt.image.renderable.RenderedImageFactory;

/**
 * @see com.lightcrafts.mediax.jai.operator.JPEGDescriptor
 *
 */
public class JPEGRIF implements RenderedImageFactory {

    /** Constructor. */
    public JPEGRIF() {}

    /**
     * Creates a <code>RenderedImage</code> representing the contents
     * of a JPEG-encoded image.
     *
     * @param paramBlock A <code>ParameterBlock</code> containing the JPEG
     *        <code>SeekableStream</code> to read.
     * @param renderHints Ignored.
     */
    public RenderedImage create(ParameterBlock paramBlock,
                                RenderingHints renderHints) {
        return CodecRIFUtil.create("jpeg", paramBlock, renderHints);
    }
}
