/*
 * $RCSfile: PNGRIF.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:39 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.opimage;
import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderedImageFactory;

/**
 * @since EA2
 */
public class PNGRIF implements RenderedImageFactory {

    /** Constructor. */
    public PNGRIF() {}

    /**
     * Creates a <code>RenderedImage</code> representing the contents
     * of a PNM-encoded image.
     *
     * @param paramBlock A <code>ParameterBlock</code> containing the PNM
     *        <code>SeekableStream</code> to read.
     * @param renderHints An instance of <code>RenderingHints</code>,
     *        or null.
     */
    public RenderedImage create(ParameterBlock paramBlock,
                                RenderingHints renderHints) {
        return CodecRIFUtil.create("png", paramBlock, renderHints);
    }
}
