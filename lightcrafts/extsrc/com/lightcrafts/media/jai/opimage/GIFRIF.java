/*
 * $RCSfile: GIFRIF.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:27 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.opimage;
import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderedImageFactory;

/**
 * A <code>RIF</code> supporting the "GIF" operation in the rendered
 * layer.
 *
 * @since EA2
 * @see com.lightcrafts.mediax.jai.operator.GIFDescriptor
 *
 */
public class GIFRIF implements RenderedImageFactory {

    /** Constructor. */
    public GIFRIF() {}

    /**
     * Creates a <code>RenderedImage</code> representing the contents
     * of a GIF-encoded image. Any layout information is ignored.
     *
     * @param paramBlock A <code>ParameterBlock</code> containing the GIF
     *        <code>SeekableStream</code> to read.
     * @param renderHints An instance of <code>RenderingHints</code>,
     *        or null.
     */
    public RenderedImage create(ParameterBlock paramBlock,
                                RenderingHints renderHints) {
        return CodecRIFUtil.create("gif", paramBlock, renderHints);
    }
}
