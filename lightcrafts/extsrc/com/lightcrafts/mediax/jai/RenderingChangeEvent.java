/*
 * $RCSfile: RenderingChangeEvent.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:21 $
 * $State: Exp $
 */
package com.lightcrafts.mediax.jai;

import java.awt.Shape;

/**
 * Class representing the event that occurs when a <code>RenderedOp</code>
 * node is re-rendered.
 *
 * @since JAI 1.1
 */
public class RenderingChangeEvent extends PropertyChangeEventJAI {
    private Shape invalidRegion;

    /**
     * Constructs a <code>RenderingChangeEvent</code>.  The inherited
     * <code>getSource()</code> method of the event would return the
     * <code>RenderedOp</code> source; the inherited methods
     * <code>getOldValue()</code> and <code>getNewValue()</code>
     * would return the old and new renderings, respectively.  If
     * either the new rendering or the invalid region is null, the
     * data of the node's rendering need to be re-requested.
     *
     * <p> The invalid region should be <code>null</code> if there is no
     * area of the extant rendering which remains valid.  If the invalid
     * region is empty, this serves to indicate that all pixels within the
     * bounds of the old rendering remain valid.  Pixels outside the image
     * bounds proper but within the bounds of all tiles of the image are
     * not guaranteed to be valid of the invalid region is empty.
     */
    public RenderingChangeEvent(RenderedOp source,
                                PlanarImage oldRendering,
                                PlanarImage newRendering,
                                Shape invalidRegion) {
        super(source, "Rendering", oldRendering, newRendering);
        this.invalidRegion = invalidRegion;
    }

    /**
     * Returns an object which represents the region over which the
     * the two renderings should differ.
     *
     * @return The region over which the two renderings differ or
     *         <code>null</code> to indicate that they differ everywhere.
     *         An empty <code>Shape</code> indicates that all pixels
     *         within the bounds of the old rendering remain valid.
     */
    public Shape getInvalidRegion() {
        return invalidRegion;
    }
}
