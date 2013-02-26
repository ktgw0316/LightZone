/*
 * $RCSfile: UntiledOpImage.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:23 $
 * $State: Exp $
 */
package com.lightcrafts.mediax.jai;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.util.Map;
import java.util.Vector;
import com.lightcrafts.mediax.jai.ImageLayout;
import com.lightcrafts.mediax.jai.PlanarImage;

/**
 * A general class for single-source operations which require cobbled
 * sources and create an image consisting of a single tile equal in location
 * and size to the image bounds.
 *
 * <p> The output image will have a single tile, regardless of the 
 * <code>ImageLayout</code> settings passed to the constructor.  Any
 * specified settings for tile grid offset and tile dimensions will be
 * replaced by the image origin and tile dimensions, respectively.
 *
 * <p> Subclasses should implement the <code>computeImage</code> method
 * which requests computation of the entire image at once.
 *
 * @see OpImage
 * @see com.lightcrafts.mediax.jai.operator.DCTDescriptor
 * @see com.lightcrafts.mediax.jai.operator.DFTDescriptor
 * @see com.lightcrafts.mediax.jai.operator.ErrorDiffusionDescriptor
 */
public abstract class UntiledOpImage extends OpImage {
    /**
     * Creates the <code>ImageLayout</code> for the image. If the
     * layout parameter is null, create a new <code>ImageLayout</code>
     * from the supplied <code>RenderedImage</code>. Also, force the tile
     * grid offset to equal the image origin and the tile width and height
     * to be equal to the image width and height, respectively, thereby
     * forcing the image to have a single tile.
     *
     * @param layout The <code>ImageLayout</code> to be cloned; may be null.
     * @param source The <code>RenderedImage</code> the attributes of which
     * are to be used as fallbacks in creating a new <code>ImageLayout</code>.
     *
     * @return The <code>ImageLayout</code> to be used.
     */
    private static ImageLayout layoutHelper(ImageLayout layout,
                                            Vector sources) {
        if(sources.size() < 1) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic5"));
        }

        RenderedImage source = (RenderedImage)sources.get(0);

        ImageLayout il = layout == null ?
            new ImageLayout() : (ImageLayout)layout.clone();

        // Force the image to have one tile. For this to obtain with minimal
        // tile size the tile grid offset must coincide with the image origin.
        il.setTileGridXOffset(il.getMinX(source));
        il.setTileGridYOffset(il.getMinY(source));
        il.setTileWidth(il.getWidth(source));
        il.setTileHeight(il.getHeight(source));

        return il;
    }

    /**
     * Constructs an <code>UntiledOpImage</code>.  The image origin and
     * dimensions, <code>SampleModel</code>, and <code>ColorModel</code>
     * may optionally be specified by an <code>ImageLayout</code> object.
     * In all cases the tile grid offset will be set to the image origin
     * and the tile dimensions to the image dimensions.  If not specified
     * in the <code>ImageLayout</code>, the image origin and dimensions
     * are set to the corresponding attributes of the first source image.
     * Cobbling will be performed on the source(s) as needed.
     *
     * @param sources  The immediate sources of this image.
     * @param configuration Configurable attributes of the image including
     *        configuration variables indexed by
     *        <code>RenderingHints.Key</code>s and image properties indexed
     *        by <code>String</code>s or <code>CaselessStringKey</code>s.
     *        This is simply forwarded to the superclass constructor.
     * @param layout an <code>ImageLayout</code> optionally containing
     *        the <code>SampleModel</code>, and
     *        <code>ColorModel</code>.  The tile grid layout
     *        information will be overridden in order to ensure that
     *        the image has a single tile.
     *
     * @throws IllegalArgumentException if <code>sources</code>
     *         is <code>null</code>.
     * @throws IllegalArgumentException  If <code>sources</code>
     *         is non-<code>null</code> and any object in
     *         <code>sources</code> is <code>null</code>.
     * @throws IllegalArgumentException if <code>sources</code> does not
     *         contain at least one element.
     * @throws ClassCastException  If the first object in <code>sources</code>
     *         is not a <code>RenderedImage</code>.
     *
     * @since JAI 1.1
     */
    public UntiledOpImage(Vector sources,
                          Map configuration,
                          ImageLayout layout) {
        super(checkSourceVector(sources, true),
              layoutHelper(layout, sources),
              configuration,
              true);
    }

    /**
     * Constructs an <code>UntiledOpImage</code>.  The image origin and
     * dimensions, <code>SampleModel</code>, and <code>ColorModel</code>
     * may optionally be specified by an <code>ImageLayout</code> object.
     * In all cases the tile grid offset will be set to the image origin
     * and the tile dimensions to the image dimensions.  If not specified
     * in the <code>ImageLayout</code>, the image origin and dimensions
     * are set to the corresponding attributes of the source image.
     * Cobbling will be performed on the source as needed.
     *
     * @param source a <code>RenderedImage</code>.
     * @param configuration Configurable attributes of the image including
     *        configuration variables indexed by
     *        <code>RenderingHints.Key</code>s and image properties indexed
     *        by <code>String</code>s or <code>CaselessStringKey</code>s.
     *        This is simply forwarded to the superclass constructor.
     * @param layout an <code>ImageLayout</code> optionally containing
     *        the <code>SampleModel</code>, and
     *        <code>ColorModel</code>.  The tile grid layout
     *        information will be overridden in order to ensure that
     *        the image has a single tile.
     *
     * @throws IllegalArgumentException if <code>source</code>
     *         is <code>null</code>.
     *
     * @since JAI 1.1
     */
    public UntiledOpImage(RenderedImage source,
                          Map configuration,
                          ImageLayout layout) {
        super(vectorize(source), // vectorize() checks for null source.
              layoutHelper(layout, vectorize(source)),
              configuration,
              true);
    }

    /**
     * Returns the image bounds.
     *
     * @param sourceRect the <code>Rectangle</code> in source coordinates
     *         (ignored).
     * @param sourceIndex the index of the source image (ignored).
     * @return The image bounds.
     */
    public Rectangle mapSourceRect(Rectangle sourceRect,
                                   int sourceIndex) {
        return getBounds();
    }

    /**
     * Returns the bounds of the indicated source image.
     *
     * @param destRect the Rectangle in destination coordinates (ignored).
     * @param sourceIndex the index of the source image.
     * @return The bounds of the indicated source image.
     *
     * @throws IllegalArgumentException if <code>sourceIndex</code> is
     *         negative or greater than the index of the last source.
     */
    public Rectangle mapDestRect(Rectangle destRect,
                                 int sourceIndex) {
        if(sourceIndex < 0 || sourceIndex >= getNumSources()) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic1"));
        }

        return getSource(sourceIndex).getBounds();
    }

    /**
     * Computes a tile.  All sources are cobbled together and
     * <code>computeImage</code> is called to produce the single
     * output tile.
     *
     * @param tileX The X index of the tile.
     * @param tileY The Y index of the tile.
     */
    public Raster computeTile(int tileX, int tileY) {
        // Create a new raster.
        Point org = new Point(getMinX(), getMinY());
        WritableRaster dest = createWritableRaster(sampleModel, org);
            
        // Determine the active area. Since the image has a single
        // tile equal in coverage to the image bounds just set this
        // to the image bounds.
        Rectangle destRect = getBounds();

        // Cobble source image(s).
        int numSources = getNumSources();
        Raster[] rasterSources = new Raster[numSources];
        for(int i = 0; i < numSources; i++) {
            PlanarImage source = getSource(i);
            Rectangle srcRect = mapDestRect(destRect, i);
            rasterSources[i] = source.getData(srcRect);
        }

        // Compute the image.
        computeImage(rasterSources, dest, destRect);

        for (int i = 0; i < numSources; i++) {
            Raster sourceData = rasterSources[i];
            if(sourceData != null) {
                PlanarImage source = getSourceImage(i);

                // Recycle the source tile
                if(source.overlapsMultipleTiles(sourceData.getBounds())) {
                    recycleTile(sourceData);
                }
            }
        }

        return dest;
    }

    /**
     * Calculate the destination image from the source image.
     *
     * @param sources The source Rasters; should be the whole image for
     *                each source.
     * @param dest The destination WritableRaster; should be the whole image.
     * @param destRect The destination Rectangle; should equal the destination
     * image bounds.
     *
     * @since JAI 1.1
     */
    protected abstract void computeImage(Raster[] sources,
                                         WritableRaster dest,
                                         Rectangle destRect);

    /**
     * Returns an array of points indicating the tile dependencies which in
     * this case is the set of all tiles in the specified source image.
     *
     * @since JAI 1.1
     */
    public Point[] getTileDependencies(int tileX, int tileY,
                                       int sourceIndex) {
        // Compute the tile dependencies only the first time that this
        // method is invoked.
        PlanarImage source = getSource(sourceIndex);

        int minTileX = source.getMinTileX();
        int minTileY = source.getMinTileY();
        int maxTileX = minTileX + source.getNumXTiles() - 1;
        int maxTileY = minTileY + source.getNumYTiles() - 1;

        Point[] tileDependencies =
            new Point[(maxTileX - minTileX + 1)*(maxTileY - minTileY + 1)];

        int count = 0;
        for(int ty = minTileY; ty <= maxTileY; ty++) {
            for(int tx = minTileX; tx <= maxTileX; tx++) {
                tileDependencies[count++] = new Point(tx, ty);
            }
        }

        return tileDependencies;
    }
}
