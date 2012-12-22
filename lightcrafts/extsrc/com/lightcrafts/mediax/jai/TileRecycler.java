/*
 * $RCSfile: TileRecycler.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:22 $
 * $State: Exp $
 */
package com.lightcrafts.mediax.jai;

import java.awt.image.Raster;

/**
 * An interface to a mechanism which is capable of recycling tiles.
 * In general the term <i>recycle</i> in this context is taken to
 * mean re-using memory allocated to the tile.  This would usually
 * be accomplished by reclaiming the data bank (array) associated
 * with the <code>DataBuffer</code> of the tile <code>Raster</code>.
 * It would also be possible by simple translation of a
 * <code>WritableRaster</code> provided the <code>SampleModel</code>
 * was compatible with the tile required by its eventual user.
 *
 * <p><i>Tile recycling should be used with caution.  In particular,
 * the calling code must be certain that any tile submitted for
 * recycling not be used elsewhere.  If one or more references to
 * tiles submitted to a recycler are held by the calling code then
 * undefined and unexpected behavior may be observed.  A similar
 * caution applies to the tile's <code>DataBuffer</code> and the
 * data bank array contained therein.</i></p>
 *
 * @since JAI 1.1.2
 */
public interface TileRecycler {
    /**
     * Suggests to the <code>TileRecycler</code> that the parameter
     * tile is no longer needed and may be used in creating a new
     * <code>Raster</code>.  This will inevitably result in at least
     * the internal array being overwritten.  If a reference to
     * the tile, its <code>DataBuffer</code>, or the data bank(s) of its
     * <code>DataBuffer</code> is held elsewhere in the caller's code,
     * undefined behavior may result.  <i>It is the responsibilty of
     * the calling code to ensure that this does not occur.</i>
     *
     * @param tile A tile which mey be re-used either directly or
     *        by reclaiming its internal <code>DataBuffer</code>
     *        or primitive data array.
     * @throws IllegalArgumentException if <code>tile</code> is
     *         <code>null</code>.
     */
    void recycleTile(Raster tile);
}
