/*
 * $RCSfile: MlibScaleOpImage.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:04 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.mlib;
import java.awt.Rectangle;
import java.awt.image.RenderedImage;
import com.lightcrafts.mediax.jai.BorderExtender;
import com.lightcrafts.mediax.jai.ImageLayout;
import com.lightcrafts.mediax.jai.Interpolation;
import com.lightcrafts.mediax.jai.ScaleOpImage;
import java.util.Map;

/**
 * MlibScaleOpImage extends ScaleOpImage for use by further extension
 * classes.
 *
 * @see ScaleOpImage
 *
 */
abstract class MlibScaleOpImage extends ScaleOpImage {

    /**
     * Constructs a MlibScaleOpImage from a RenderedImage source,
     * Interpolation object, x and y scale values.  The image
     * dimensions are determined by forward-mapping the source bounds.
     * The tile grid layout, SampleModel, and ColorModel are specified
     * by the image source, possibly overridden by values from the
     * ImageLayout parameter.
     * 
     * @param source a RenderedImage.
     * @param extender a BorderExtender, or null.

     *        or null.  If null, a default cache will be used.
     * @param layout an ImageLayout optionally containing the tile grid layout,
     *        SampleModel, and ColorModel, or null.
     * @param scaleX scale factor along x axis.
     * @param scaleY scale factor along y axis.
     * @param transX translation factor along x axis.
     * @param transY translation factor along y axis.
     * @param interp an Interpolation object to use for resampling.
     * @param cobbleSources a boolean indicating whether computeRect()
     *        expects contiguous sources.
     */
    public MlibScaleOpImage(RenderedImage source,
                            BorderExtender extender,
                            Map config,
			    ImageLayout layout,
			    float scaleX,
			    float scaleY,
			    float transX,
			    float transY,
			    Interpolation interp,
			    boolean cobbleSources) {
        super(source,
              layout,
              config,
              cobbleSources,
              extender,
              interp,
              scaleX,
              scaleY,
              transX,
              transY);

	// If the user did not provide a BorderExtender, attach a
	// BorderExtenderCopy to Medialib such that when Medialib
	// ask for additional source which may lie outside the 
	// bounds, it always gets it.
	this.extender = (extender == null) ? 
	    BorderExtender.createInstance(BorderExtender.BORDER_COPY)
	    : extender;
    }

    // Override backwardMapRect to pad the source by one extra pixel
    // in all directions for non Nearest Neighbor interpolations, so 
    // that precision issues don't cause Medialib to not write areas
    // in the destination rectangle.

    /**
     * Returns the minimum bounding box of the region of the specified
     * source to which a particular <code>Rectangle</code> of the
     * destination will be mapped.
     *
     * @param destRect the <code>Rectangle</code> in destination coordinates.
     * @param sourceIndex the index of the source image.
     *
     * @return a <code>Rectangle</code> indicating the source bounding box,
     *         or <code>null</code> if the bounding box is unknown.
     *
     * @throws IllegalArgumentException if <code>sourceIndex</code> is
     *         negative or greater than the index of the last source.
     * @throws IllegalArgumentException if <code>destRect</code> is
     *         <code>null</code>.
     */
    protected Rectangle backwardMapRect(Rectangle destRect,
                                        int sourceIndex) {

	Rectangle srcRect = super.backwardMapRect(destRect, sourceIndex);
	Rectangle paddedSrcRect = new Rectangle(srcRect.x - 1, 
						srcRect.y - 1,
						srcRect.width + 2, 
						srcRect.height + 2);

	return paddedSrcRect;
    }
}
