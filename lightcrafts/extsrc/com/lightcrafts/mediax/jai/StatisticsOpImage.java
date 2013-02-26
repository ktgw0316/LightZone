/*
 * $RCSfile: StatisticsOpImage.java,v $
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
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.util.Vector;
import com.lightcrafts.media.jai.util.PropertyUtil;

/**
 * An abstract base class representing image operators that compute
 * statistics on a given region of an image, and with a given sampling
 * period.  Such operators may only have one source image.
 *
 * <p> The layout of this image is exactly the same as that of the source
 * image.  Any user supplied layout values via the
 * <code>RenderingHints</code> are ignored.
 * The <code>StatisticsOpImage</code> simply passes the pixels of the
 * source image through unchanged.  However, the desired statistics
 * are computed on demand and made available as a property or set of
 * properties on the image.
 *
 * <p> All instances of <code>StatisticsOpImage</code> make use of a
 * region of interest, specified as a <code>ROI</code> object.  If this
 * argument is <code>null</code>, the entire source image is used.
 * Additionally, they may perform spatial subsampling of the region of
 * interest according to <code>xPeriod</code> and <code>yPeriod</code>
 * parameters that may vary from 1 (sample every pixel of the region
 * of interest) upwards.  This allows the speed and quality of
 * statistics gathering to be traded off against one another.
 *
 * <p> Subclasses should provide implementations
 * of the <code>getStatisticsNames</code>, <code>createStatistics</code>,
 * and <code>accumulateStatistics</code> methods.
 * 
 * @see OpImage
 */
public abstract class StatisticsOpImage extends OpImage {
    
    /**
     * The region of interest over which to compute the statistics.
     * If it is <code>null</code>, the entire image is used to compute
     * the statistics.
     */
    protected ROI roi;

    /** The X coordinate of the initial sample. */
    protected int xStart;

    /** The Y coordinate of the initial sample. */
    protected int yStart;

    /** The horizontal sampling rate. */
    protected int xPeriod;

    /** The vertical sampling rate. */
    protected int yPeriod;

    /** Whether to check for skipped tiles. **/
    private boolean checkForSkippedTiles;

    /**
     * Constructor.
     *
     * <p> The layout of this image is exactly the same as that of the
     * source image.  Any user supplied layout values via the
     * <code>RenderingHints</code> are ignored.
     *
     * @param source  The source image over which the statistics
     *        is accumulated.
     * @param roi  The region of interest that specifies the region of the
     *        source image over which to compute the statistics.  If it
     *        is <code>null</code>, the entire source image is used.
     * @param xStart   The initial X sample coordinate.
     * @param yStart   The initial Y sample coordinate.
     * @param xPeriod  The horizontal sampling rate.
     * @param yPeriod  The vertical sampling rate.
     *
     * @throws IllegalArgumentException  If <code>source</code> is
     *         <code>null</code>.
     *
     * @since JAI 1.1
     */
    public StatisticsOpImage(RenderedImage source,
                             ROI roi,
                             int xStart,
                             int yStart,
                             int xPeriod,
                             int yPeriod) {
        super(vectorize(source), // vectorize() checks for null source.
              new ImageLayout(source),
              null,	// configuration
              false);

        this.roi = roi == null ?
                   new ROIShape(getSource(0).getBounds()) : roi;
        this.xStart  = xStart;
        this.yStart  = yStart;
        this.xPeriod = xPeriod;
        this.yPeriod = yPeriod;

        this.checkForSkippedTiles =
            xPeriod > tileWidth || yPeriod > tileHeight;
    }

    /**
     * Returns <code>false</code> as <code>computeTile()</code> invocations
     * are forwarded to the <code>RenderedImage</code> source and are
     * therefore not unique objects in the global sense.
     *
     * @since JAI 1.1
     */
    public boolean computesUniqueTiles() {
        return false;
    }

    /**
     * Returns a tile of this image as a <code>Raster</code>.  If the
     * requested tile is completely outside of this image's bounds,
     * this method returns <code>null</code>.
     *
     * <p> Statistics operators do not cache their tiles internally.
     * Rather, the implementation of this method in this class simply
     * forwards the request to the source image.
     *
     * @param tileX  The X index of the tile.
     * @param tileY  The Y index of the tile.
     *
     * @return The requested tile as a <code>Raster</code> or
     *         <code>null</code>.
     */
    public Raster getTile(int tileX, int tileY) {
        return getSource(0).getTile(tileX, tileY);
    }

    /**
     * Computes the image data of a tile.
     *
     * <p> The implementation of this method in this class simply forwards
     * the request to the source image.
     *
     * @param tileX  The X index of the tile.
     * @param tileY  The Y index of the tile.
     *
     * @since JAI 1.1
     */
    public Raster computeTile(int tileX, int tileY) {
        return getSource(0).getTile(tileX, tileY);
    }

    /**
     * Returns a list of tiles.  The request is simply
     * forwarded to the source image.
     *
     * @param tileIndices  The indices of the tiles requested.
     *
     * @throws IllegalArgumentException  If <code>tileIndices</code> is
     *         <code>null</code>.
     */
    public Raster[] getTiles(Point[] tileIndices) {
        if ( tileIndices == null ) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        return getSource(0).getTiles(tileIndices);
    }

    /**
     * Maps the source rectangle into destination space unchanged.
     *
     * @param sourceRect the Rectangle in source coordinates.
     * @param sourceIndex the index of the source image.
     *
     * @return A <code>Rectangle</code> indicating the valid destination region.
     *
     * @throws IllegalArgumentException  If <code>sourceIndex</code>
     *         is not 0.
     * @throws IllegalArgumentException  If <code>sourceRect</code> is
     *         <code>null</code>.
     */
    public Rectangle mapSourceRect(Rectangle sourceRect,
                                   int sourceIndex) {
        if ( sourceRect == null ) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        if (sourceIndex != 0) {		// there is only 1 source
            throw new IllegalArgumentException(JaiI18N.getString("Generic1"));
        }
        return new Rectangle(sourceRect);
    }
    
    /**
     * Maps the destination rectangle into source space unchanged.
     *
     * @param destRect the Rectangle in destination coordinates.
     * @param sourceIndex the index of the source image.
     *
     * @return A <code>Rectangle</code> indicating the required source region.
     *
     * @throws IllegalArgumentException  If <code>sourceIndex</code>
     *         is not 0.
     * @throws IllegalArgumentException  If <code>destRect</code> is
     *         <code>null</code>.
     */
    public Rectangle mapDestRect(Rectangle destRect,
                                 int sourceIndex) {
        if ( destRect == null ) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        if (sourceIndex != 0) {		// there is only 1 source
            throw new IllegalArgumentException(JaiI18N.getString("Generic1"));
        }
        return new Rectangle(destRect);
    }

    /**
     * Returns one of the available statistics as a property.  If the
     * property name is not recognized, this method returns
     * <code>java.awt.Image.UndefinedProperty</code>.
     *
     * @throws IllegalArgumentException  If <code>name</code> is
     *         <code>null</code>.
     */
    public Object getProperty(String name) {
        if (name == null) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        // Is this property already in the Hashtable?
        Object stats = super.getProperty(name);

        if (stats.equals(java.awt.Image.UndefinedProperty)) {
            // This property has not been generated; generate it.
            synchronized (this) {	// lock other threads
                stats = createStatistics(name);

                if (!stats.equals(java.awt.Image.UndefinedProperty)) {
                    PlanarImage source = getSource(0);

                    // Cycle throw all source tiles.
                    int minTileX = source.getMinTileX();
                    int maxTileX = source.getMaxTileX();
                    int minTileY = source.getMinTileY();
                    int maxTileY = source.getMaxTileY();

                    for (int y = minTileY; y <= maxTileY; y++) {
                        for (int x = minTileX; x <= maxTileX; x++) {
                            // Determine the required region of this tile.
                            // (Note that getTileRect() instersects tile and
                            // image bounds.)
                            Rectangle tileRect = getTileRect(x, y);

                            // Process if and only if within ROI bounds.
                            if (roi.intersects(tileRect)) {

                                // If checking for skipped tiles determine
                                // whether this tile is "hit".
                                if(checkForSkippedTiles &&
                                   tileRect.x >= xStart &&
                                   tileRect.y >= yStart) {
                                    // Determine the offset within the tile.
                                    int offsetX =
                                        (xPeriod -
                                         ((tileRect.x - xStart) % xPeriod)) %
                                        xPeriod;
                                    int offsetY =
                                        (yPeriod -
                                         ((tileRect.y - yStart) % yPeriod)) %
                                        yPeriod;

                                    // Continue with next tile if offset
                                    // is larger than either tile dimension.
                                    if(offsetX >= tileRect.width ||
                                       offsetY >= tileRect.height) {
                                        continue;
                                    }
                                }

                                // Accumulate statistics for this tile.
                                accumulateStatistics(name,
                                                     source.getData(tileRect),
                                                     stats);
                            }
                        }
                    }

                    // Store the generated property in Hastable.
                    setProperty(name, stats);
                }
            }
        }

        return stats;
    }

    /**
     * Returns a list of property names that are recognized by this image.
     *
     * @return  An array of <code>String</code>s containing valid
     *          property names.
     */
    public String[] getPropertyNames() {
        // Get statistics names and names from superclass.
        String[] statsNames = getStatisticsNames();
        String[] superNames = super.getPropertyNames();

        // Return stats names if not superclass names.
        if(superNames == null) {
            return statsNames;
        }

        // Check for overlap between stats names and superclass names.
        Vector extraNames = new Vector();
        for (int i = 0; i < statsNames.length; i++) {
            String prefix = statsNames[i];
            String[] names = PropertyUtil.getPropertyNames(superNames, prefix);
            if(names != null) {
                for(int j = 0; j < names.length; j++) {
                    if(names[j].equalsIgnoreCase(prefix)) {
                        extraNames.add(prefix);
                    }
                }
            }
        }

        // If no overlap then return.
        if (extraNames.size() == 0) {
            return superNames;
        }

        // Combine superclass and extra names.
        String[] propNames = new String[superNames.length + extraNames.size()];
        System.arraycopy(superNames, 0, propNames, 0, superNames.length);
        int offset = superNames.length;
        for (int i = 0; i < extraNames.size(); i++) {
            propNames[offset++] = (String)extraNames.get(i);
        }

        // Return combined name set.
	return propNames;
    }

    /**
     * Returns a list of names of statistics understood
     * by this class.
     */
    protected abstract String[] getStatisticsNames();

    /**
     * Returns an object that will be used to gather the
     * named statistic.
     *
     * @param name  The name of the statistic to be gathered.
     */
    protected abstract Object createStatistics(String name);

    /**
     * Accumulates statistics on the specified region into
     * the previously created statistics object.  The
     * region of interest and X and Y sampling rate
     * should be respected.
     *
     * @param name  The name of the statistic to be gathered.
     * @param source  A <code>Raster</code> containing source pixels.
     *        The dimensions of the Raster will not
     *        exceed maxWidth x maxHeight.
     * @param stats  A statistics object generated by a previous call
     *        to createStatistics. 
     */
    protected abstract void accumulateStatistics(String name,
                                                 Raster source,
                                                 Object stats);
}
