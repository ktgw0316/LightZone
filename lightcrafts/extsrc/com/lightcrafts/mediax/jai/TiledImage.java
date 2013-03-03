/*
 * $RCSfile: TiledImage.java,v $
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
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.image.BandedSampleModel;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.TileObserver;
import java.awt.image.WritableRaster;
import java.awt.image.WritableRenderedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Vector;
import com.lightcrafts.media.jai.util.JDKWorkarounds;

/**
 * A concrete implementation of <code>WritableRenderedImage</code>.
 *
 * <p> <code>TiledImage</code> is the main class for writable images
 * in JAI.  <code>TiledImage</code> provides a straightforward
 * implementation of the <code>WritableRenderedImage</code> interface,
 * taking advantage of that interface's ability to describe images
 * with multiple tiles.  The tiles of a
 * <code>WritableRenderedImage</code> must share a
 * <code>SampleModel</code>, which determines their width, height, and
 * pixel format.  The tiles form a regular grid, which may occupy any
 * rectangular region of the plane.  Tile pixels the locations of which
 * lie outside the stated image bounds have undefined values.
 *
 * <p> The contents of a <code>TiledImage</code> are defined by a
 * single <code>RenderedImage</code> source provided by means of one of
 * the <code>set()</code> methods or to a constructor which accepts a
 * <code>RenderedImage</code>.  The <code>set()</code> methods provide
 * a way to selectively overwrite a portion of a <code>TiledImage</code>,
 * possibly using a region of interest (ROI).
 *
 * <p> <code>TiledImage</code> also supports direct manipulation of
 * pixels by means of the <code>getWritableTile()</code> method.  This
 * method returns a <code>WritableRaster</code> that can be modified directly.
 * Such changes become visible to readers according to the regular
 * thread synchronization rules of the Java virtual machine; JAI makes
 * no additional guarantees.  When a writer is finished modifying a
 * tile, it should call <code>releaseWritableTile()</code>.  A
 * shortcut is to call <code>setData()</code>, which copies a rectangular
 * region or an area specified by a <code>ROI</code> from a supplied
 * <code>Raster</code> directly into the <code>TiledImage</code>.
 *
 * <p> A final way to modify the contents of a <code>TiledImage</code>
 * is through calls to the object returned by <code>createGraphics()</code>.
 * This returns a <code>Graphics2D</code> object that can be used to draw
 * line art, text, and images in the usual Abstract Window Toolkit (AWT)
 * manner.
 *
 * <p> A <code>TiledImage</code> does not attempt to maintain
 * synchronous state on its own.  That task is left to
 * <code>SnapshotImage</code>.  If a synchronous (unchangeable) view
 * of a <code>TiledImage</code> is desired, its
 * <code>createSnapshot()</code> method must be used.  Otherwise,
 * changes due to calls to set() or direct writing of tiles by objects
 * that call <code>getWritableTile()</code> will be visible.
 *
 * <p> <code>TiledImage</code> does not actually cause its tiles to be
 * copied from the specified source until their contents are demanded.
 * Once a tile has been computed, its contents may be discarded if it can
 * be determined that it can be recomputed identically from the source.
 * The <code>lockTile()</code> method forces a tile to be computed and
 * maintained for the lifetime of the <code>TiledImage</code>.
 *
 * @see SnapshotImage
 * @see java.awt.image.RenderedImage
 * @see java.awt.image.WritableRenderedImage
 *
 */
public class TiledImage extends PlanarImage
    implements WritableRenderedImage, PropertyChangeListener {

    /** The number of tiles in the X direction. */
    protected int tilesX;

    /** The number of tiles in the Y direction. */
    protected int tilesY;

    /** The index of the leftmost column of tiles. */
    protected int minTileX;

    /** The index of the uppermost row of tiles. */
    protected int minTileY;

    /** The tile array. */
    protected WritableRaster[][] tiles;

    /** The number of writers of each tile; -1 indicates a locked tile. */
    protected int[][] writers;

    /** The current set of TileObservers. */
    protected Vector tileObservers = null;

    /** Whether DataBuffers are shared with the source image. */
    private boolean areBuffersShared = false;

    /** The parent TiledImage if this one was created using getSubImage(). */
    private TiledImage parent = null;

    /** The SampleModel of the TiledImage ancestor. */
    private SampleModel ancestorSampleModel = null;

    /** The sub-banding list with respect to the ancestor. */
    private int[] bandList = null;

    /** The number of writable tiles; shared with all ancestors. */
    private int[] numWritableTiles = null;

    /** The ROI to be used with the source image of uncomputed tiles. */
    private ROI srcROI = null;

    /** The bounds of the intersection of the source image bounds with
       those of this image and with the source ROI if present. */
    private Rectangle overlapBounds = null;

    /**
     * Derives a <code>SampleModel</code> with the specified dimensions
     * from the input <code>SampleModel</code>.  If the input
     * <code>SampleModel</code> already has these dimensions, it is
     * used directly; otherwise a new <code>SampleModel</code> of the
     * required dimensions is derived and returned.
     */
    private static SampleModel coerceSampleModel(SampleModel sampleModel,
                                                 int sampleModelWidth,
                                                 int sampleModelHeight) {
        return (sampleModel.getWidth() == sampleModelWidth &&
                sampleModel.getHeight() == sampleModelHeight) ?
            sampleModel :
            sampleModel.createCompatibleSampleModel(sampleModelWidth,
                                                    sampleModelHeight);
    }

    /*
     * Derives the values of minTileX, minTileY, tilesX, and tilesY
     * from minX, minY, width, height, tileGridXOffset, tileGridYOffset,
     * tileWidth, and tileHeight.  If the image has a parent, its tile
     * grid minima are set to those of the parent.
     *
     * @param parent The parent TiledImage.
     */
    private void initTileGrid(TiledImage parent) {
        if(parent != null) {
            this.minTileX = parent.minTileX;
            this.minTileY = parent.minTileY;
        } else {
            this.minTileX = getMinTileX();
            this.minTileY = getMinTileY();
        }

        int maxTileX = getMaxTileX();
        int maxTileY = getMaxTileY();

        this.tilesX = maxTileX - minTileX + 1;
        this.tilesY = maxTileY - minTileY + 1;
    }

    /**
     * Constructs a <code>TiledImage</code> with a given layout,
     * <code>SampleModel</code>, and <code>ColorModel</code>.  The
     * width and height of the image tiles will be respectively equal
     * to the width and height of the <code>SampleModel</code>. The
     * <code>tileFactory</code> instance variable will be set to the
     * value of the <code>JAI.KEY_TILE_FACTORY</code> hint set on
     * the default instance of <code>JAI</code>.
     *
     * @param minX The X coordinate of the upper-left pixel
     * @param minY The Y coordinate of the upper-left pixel.
     * @param width The width of the image.
     * @param height The height of the image.
     * @param tileGridXOffset The X coordinate of the upper-left
     *        pixel of tile (0, 0).
     * @param tileGridYOffset The Y coordinate of the upper-left
     *        pixel of tile (0, 0).
     * @param tileSampleModel A <code>SampleModel</code> with which to be
     *        compatible.
     * @param colorModel A <code>ColorModel</code> to associate with the
     *        image.
     */
    public TiledImage(int minX, int minY,
                      int width, int height,
                      int tileGridXOffset, int tileGridYOffset,
                      SampleModel tileSampleModel,
                      ColorModel colorModel) {
        this(null, minX, minY, width, height,
             tileGridXOffset, tileGridYOffset,
             tileSampleModel, colorModel);
    }

    /**
     * Constructs a child TiledImage. If the parent is null it is a root
     * TiledImage and all instance variables will be allocated.
     */
    private TiledImage(TiledImage parent,
                       int minX, int minY,
                       int width, int height,
                       int tileGridXOffset, int tileGridYOffset,
                       SampleModel sampleModel,
                       ColorModel colorModel) {
        super(new ImageLayout(minX, minY, width, height,
                              tileGridXOffset, tileGridYOffset,
                              sampleModel.getWidth(), sampleModel.getHeight(),
                              sampleModel, colorModel), null, null);
        initTileGrid(parent);

        if(parent == null) {
            this.tiles = new WritableRaster[tilesX][tilesY];
            this.writers = new int[tilesX][tilesY];
            tileObservers = new Vector();
            numWritableTiles = new int[1];
            numWritableTiles[0] = 0;
            ancestorSampleModel = sampleModel;
        } else {
            this.parent = parent;
            this.tiles = parent.tiles;
            this.writers = parent.writers;
            tileObservers = parent.tileObservers;
            numWritableTiles = parent.numWritableTiles;
            ancestorSampleModel = parent.ancestorSampleModel;
        }

        tileFactory =
            (TileFactory)JAI.getDefaultInstance().getRenderingHint(
                                                      JAI.KEY_TILE_FACTORY);
    }

    /**
     * Constructs a <code>TiledImage</code> with a
     * <code>SampleModel</code> that is compatible with a given
     * <code>SampleModel</code>, and given tile dimensions.  The width
     * and height are taken from the <code>SampleModel</code>, and the
     * image begins at a specified point.  The <code>ColorModel</code>
     * will be derived from the <code>SampleModel</code> using the
     * <code>createColorModel</code> method of <code>PlanarImage</code>.
     * Note that this implies that the <code>ColorModel</code> could be
     * <code>null</code>.
     *
     * @param origin A <code>Point</code> indicating the image's upper
     *        left corner.
     * @param sampleModel A <code>SampleModel</code> with which to be
     *        compatible.
     * @param tileWidth The desired tile width.
     * @param tileHeight The desired tile height.
     *
     * @deprecated as of JAI 1.1.
     */
    public TiledImage(Point origin,
                      SampleModel sampleModel,
                      int tileWidth, int tileHeight) {
        this(origin.x, origin.y,
             sampleModel.getWidth(), sampleModel.getHeight(),
             origin.x, origin.y,
             coerceSampleModel(sampleModel, tileWidth, tileHeight),
             PlanarImage.createColorModel(sampleModel));
    }

    /**
     * Constructs a <code>TiledImage</code> starting at the global
     * coordinate origin.  The <code>ColorModel</code>
     * will be derived from the <code>SampleModel</code> using the
     * <code>createColorModel</code> method of <code>PlanarImage</code>.
     * Note that this implies that the <code>ColorModel</code> could be
     * <code>null</code>.
     *
     * @param sampleModel A <code>SampleModel</code> with which to be
     *        compatible.
     * @param tileWidth The desired tile width.
     * @param tileHeight The desired tile height.
     *
     * @deprecated as of JAI 1.1.
     */
    public TiledImage(SampleModel sampleModel,
                      int tileWidth,
                      int tileHeight) {
        this(0, 0,
             sampleModel.getWidth(), sampleModel.getHeight(),
             0, 0,
             coerceSampleModel(sampleModel, tileWidth, tileHeight),
             PlanarImage.createColorModel(sampleModel));
    }

    /**
     * Constructs a <code>TiledImage</code> equivalent to a given
     * <code>RenderedImage</code> but with specific tile dimensions.
     * Actual copying of the pixel data from the <code>RenderedImage</code>
     * will be deferred until the first time they are requested from the
     * <code>TiledImage</code>.
     *
     * @param source The source <code>RenderedImage</code>.
     * @param tileWidth The desired tile width.
     * @param tileHeight The desired tile height.
     *
     * @since JAI 1.1
     */
    public TiledImage(RenderedImage source, int tileWidth, int tileHeight) {
        this(source.getMinX(), source.getMinY(),
             source.getWidth(), source.getHeight(),
             source.getTileGridXOffset(), source.getTileGridYOffset(),
             coerceSampleModel(source.getSampleModel(),
                               tileWidth, tileHeight),
             source.getColorModel());

        set(source);
    }

    /**
     * Constructs a <code>TiledImage</code> equivalent to a given
     * <code>RenderedImage</code>.  Actual copying of the pixel data from
     * the <code>RenderedImage</code> will be deferred until the first time
     * they are requested from the <code>TiledImage</code>.  The tiles of
     * the <code>TiledImage</code> may optionally share
     * <code>DataBuffer</code>s with the tiles of the source image but it
     * should be realized in this case that data written into the
     * <code>TiledImage</code> will be visible in the source image.
     *
     * @param source The source <code>RenderedImage</code>.
     * @param areBuffersShared Whether the tile <code>DataBuffer</code>s
     *                         of the source are re-used in the tiles of
     *			       this image.  If <code>false</code> new
     *			       <code>WritableRaster</code>s will be
     *			       created.
     *
     * @since JAI 1.1
     */
    public TiledImage(RenderedImage source, boolean areBuffersShared) {
        this(source, source.getTileWidth(), source.getTileHeight());
        this.areBuffersShared = areBuffersShared;
    }

    /**
     * Returns a <code>TiledImage</code> making use of an
     * interleaved <code>SampleModel</code> with a given layout,
     * number of bands, and data type.  The <code>ColorModel</code>
     * will be derived from the <code>SampleModel</code> using the
     * <code>createColorModel</code> method of <code>PlanarImage</code>.
     * Note that this implies that the <code>ColorModel</code> could be
     * <code>null</code>.
     *
     * @param minX The X coordinate of the upper-left pixel
     * @param minY The Y coordinate of the upper-left pixel.
     * @param width The width of the image.
     * @param height The height of the image.
     * @param numBands The number of bands in the image.
     * @param dataType The data type, from among the constants
     *        <code>DataBuffer.TYPE_*</code>.
     * @param tileWidth The tile width.
     * @param tileHeight The tile height.
     * @param bandOffsets An array of non-duplicated integers between 0 and
     *        <code>numBands - 1</code> of length <code>numBands</code>
     *        indicating the relative offset of each band.
     *
     * @deprecated as of JAI 1.1.
     */
    public static TiledImage createInterleaved(int minX, int minY,
                                               int width, int height,
                                               int numBands,
                                               int dataType,
                                               int tileWidth,
                                               int tileHeight,
                                               int[] bandOffsets) {
        SampleModel sm =
           RasterFactory.createPixelInterleavedSampleModel(dataType,
                                            tileWidth, tileHeight,
                                            numBands,
                                            numBands*tileWidth,
                                            bandOffsets);
        return new TiledImage(minX, minY, width, height,
                              minX, minY,
                              sm, PlanarImage.createColorModel(sm));
    }

    /**
     * Returns a <code>TiledImage</code> making use of an
     * banded <code>SampleModel</code> with a given layout,
     * number of bands, and data type.  The <code>ColorModel</code>
     * will be derived from the <code>SampleModel</code> using the
     * <code>createColorModel</code> method of <code>PlanarImage</code>.
     * Note that this implies that the <code>ColorModel</code> could be
     * <code>null</code>.
     *
     * @param minX The X coordinate of the upper-left pixel
     * @param minY The Y coordinate of the upper-left pixel.
     * @param width The width of the image.
     * @param height The height of the image.
     * @param dataType The data type, from among the constants
     *        <code>DataBuffer.TYPE_*</code>.
     * @param tileWidth The tile width.
     * @param tileHeight The tile height.
     * @param bankIndices An array of <code>int</code>s indicating the
     *        index of the bank to use for each band.  Bank indices
     *        may be duplicated.
     * @param bandOffsets An array of integers indicating the starting
     *        offset of each band within its bank.  Bands stored in
     *        the same bank must have sufficiently different offsets
     *        so as not to overlap.
     *
     * @deprecated as of JAI 1.1.
     */
    public static TiledImage createBanded(int minX, int minY,
                                          int width, int height,
                                          int dataType,
                                          int tileWidth,
                                          int tileHeight,
                                          int[] bankIndices,
                                          int[] bandOffsets) {
        SampleModel sm =
            new BandedSampleModel(dataType,
                                  tileWidth, tileHeight,
                                  tileWidth,
                                  bankIndices,
                                  bandOffsets);
        return new TiledImage(minX, minY, width, height,
                              minX, minY,
                              sm, PlanarImage.createColorModel(sm));
    }

    /**
     * Overlays a rectangular area of pixels from an image onto a tile.
     *
     * @param tile
     * @param im
     * @param rect
     */
    private void overlayPixels(WritableRaster tile,
                               RenderedImage im,
                               Rectangle rect) {
        // Create a child of tile occupying the intersection area
        WritableRaster child =
            tile.createWritableChild(rect.x, rect.y,
                                     rect.width, rect.height,
                                     rect.x, rect.y,
                                     bandList);

        im.copyData(child);
    }

    /**
     * Overlays a set of pixels described by an Area from an image
     * onto a tile.
     *
     * @param tile
     * @param im
     * @param a
     */
    private void overlayPixels(WritableRaster tile,
                               RenderedImage im,
                               Area a) {
        ROIShape rs = new ROIShape(a);
        Rectangle bounds = rs.getBounds();
        LinkedList rectList =
            rs.getAsRectangleList(bounds.x, bounds.y,
                                  bounds.width, bounds.height);
        int numRects = rectList.size();
        for(int i = 0; i < numRects; i++) {
            Rectangle rect = (Rectangle)rectList.get(i);
            WritableRaster child =
                tile.createWritableChild(rect.x, rect.y,
                                         rect.width, rect.height,
                                         rect.x, rect.y,
                                         bandList);
            im.copyData(child);
        }
    }

    /**
     * Overlays a set of pixels described by a bitmask
     * onto a tile.
     */
    private void overlayPixels(WritableRaster tile,
                               RenderedImage im,
                               Rectangle rect,
                               int[][] bitmask) {
        Raster r = im.getData(rect);

        // If this is sub-banded child image, create a child of the
        // tile into which to write.
        if(bandList != null) {
            tile = tile.createWritableChild(rect.x, rect.y,
                                            rect.width, rect.height,
                                            rect.x, rect.y,
                                            bandList);
        }

        // Create a buffer suitable for transferring pixels
        Object data = r.getDataElements(rect.x, rect.y, null);

	// The bitmask passed in might have undefined values outside
	// the specified rect - therefore make sure that those bits
	// are ignored.
	int leftover = rect.width % 32;
	int bitWidth = ((rect.width+31)/32) - (leftover > 0 ? 1 : 0);
        int y = rect.y;

        for (int j = 0; j < rect.height; j++, y++) {
            int[] rowMask = bitmask[j];
            int   i, x = rect.x;

            for (i = 0; i < bitWidth; i++) {
                int mask32 = rowMask[i];
                int bit = 0x80000000;

                for (int b = 0; b < 32; b++, x++) {
                    if ((mask32 & bit) != 0) {
                        r.getDataElements(x, y, data);
                        tile.setDataElements(x, y, data);
                    }
                    bit >>>= 1;
                }
            }

	    if (leftover > 0) {
		int mask32 = rowMask[i];
                int bit = 0x80000000;

                for (int b = 0; b < leftover; b++, x++) {
                    if ((mask32 & bit) != 0) {
                        r.getDataElements(x, y, data);
                        tile.setDataElements(x, y, data);
                    }
                    bit >>>= 1;
                }
	    }
	}
    }

    /**
     * Overlays a given <code>RenderedImage</code> on top of the
     * current contents of the <code>TiledImage</code>.  The source
     * image must have a <code>SampleModel</code> compatible with that
     * of this image.  If the source image does not overlap this image
     * then invoking this method will have no effect.
     *
     * <p> The source image is added as a fallback <code>PropertySource</code>
     * for the <code>TiledImage</code>: if a given property is not set directly
     * on the <code>TiledImage</code> an attempt will be made to obtain its
     * value from the source image.
     *
     * @param im A <code>RenderedImage</code> source to overlay.
     *
     * @throws <code>IllegalArgumentException</code> if <code>im</code> is
     *         <code>null</code>.
     */
    public void set(RenderedImage im) {

        if ( im == null ) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        // Same source: do nothing.
        if(getNumSources() > 0 && im == getSourceImage(0)) {
            return;
        }

        Rectangle imRect = new Rectangle(im.getMinX(), im.getMinY(),
                                         im.getWidth(), im.getHeight());

        // Return if the source image does not overlap this image.
        if((imRect = imRect.intersection(getBounds())).isEmpty()) {
            return;
        }

        // Unset buffer sharing flag.
        areBuffersShared = false;

        // Set tile index limits.
        int txMin = XToTileX(imRect.x);
        int tyMin = YToTileY(imRect.y);
        int txMax = XToTileX(imRect.x + imRect.width - 1);
        int tyMax = YToTileY(imRect.y + imRect.height - 1);

        // Loop over all in-bound tiles, find ones that have been computed
        for (int j = tyMin; j <= tyMax; j++) {
            for (int i = txMin; i <= txMax; i++) {
                WritableRaster t;
                if ((t = tiles[i - minTileX][j - minTileY]) != null
                    && !isTileLocked(i, j)) {
                    Rectangle tileRect = getTileRect(i, j);
                    tileRect = tileRect.intersection(imRect);
                    if(!tileRect.isEmpty()) {
                        overlayPixels(t, im, tileRect);
                    }
                }
            }
        }

        // Cache the (wrapped) source image and clear the source ROI.
        PlanarImage src = PlanarImage.wrapRenderedImage(im);
        if(getNumSources() == 0) {
            addSource(src);
        } else {
            setSource(src, 0);
        }
        srcROI = null;
        overlapBounds = imRect;

        // Add the source as fallback PropertySource.
        properties.addProperties(src);
    }

    /**
     * Overlays a given <code>RenderedImage</code> on top of the
     * current contents of the <code>TiledImage</code> and its
     * intersection with the supplied ROI.  The source
     * image must have a <code>SampleModel</code> compatible with that
     * of this image.  If the source image and the region of interest
     * do not both overlap this image then invoking this method will
     * have no effect.
     *
     * <p> The source image is added as a fallback <code>PropertySource</code>
     * for the <code>TiledImage</code>: if a given property is not set directly
     * on the <code>TiledImage</code> an attempt will be made to obtain its
     * value from the source image.
     *
     * @param im A <code>RenderedImage</code> source to overlay.
     * @param roi The region of interest.
     *
     * @throws <code>IllegalArgumentException</code> either parameter is
     *         <code>null</code>.
     */
    public void set(RenderedImage im, ROI roi) {

        if ( im == null ) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        // Same source: do nothing.
        if(getNumSources() > 0 && im == getSourceImage(0)) {
            return;
        }

        Rectangle imRect = new Rectangle(im.getMinX(), im.getMinY(),
                                         im.getWidth(), im.getHeight());

        // Return if there is not a common intersection among this
        // image and the source image and the region of interest.
        Rectangle overlap = imRect.intersection(roi.getBounds());
        if(overlap.isEmpty() ||
           (overlap = overlap.intersection(getBounds())).isEmpty()) {
            return;
        }

        // Unset buffer sharing flag.
        areBuffersShared = false;

        // Set tile index limits.
        int txMin = XToTileX(overlap.x);
        int tyMin = YToTileY(overlap.y);
        int txMax = XToTileX(overlap.x + overlap.width - 1);
        int tyMax = YToTileY(overlap.y + overlap.height - 1);

        Shape roiShape = roi.getAsShape();
        Area roiArea = null;
        if (roiShape != null) {
            roiArea = new Area(roiShape);
        }

        // Loop over all in-bound tiles, find ones that have been computed
        for (int j = tyMin; j <= tyMax; j++) {
            for (int i = txMin; i <= txMax; i++) {
                WritableRaster t;
                if ((t = tiles[i - minTileX][j - minTileY]) != null &&
                    !isTileLocked(i, j)) {
                    Rectangle rect = getTileRect(i, j).intersection(overlap);
                    if(!rect.isEmpty()) {
                        if (roiShape != null) {
                            Area a = new Area(rect);
                            a.intersect(roiArea);

                            if(!a.isEmpty()) {
                                overlayPixels(t, im, a);
                            }
                        } else {
                            int[][] bitmask =
                                roi.getAsBitmask(rect.x, rect.y,
                                                 rect.width,
                                                 rect.height,
                                                 null);

                            if(bitmask != null && bitmask.length > 0) {
                                overlayPixels(t, im, rect, bitmask);
                            }
                        }
                    }
                }
            }
        }

        // Cache the (wrapped) source image and the source ROI.
        PlanarImage src = PlanarImage.wrapRenderedImage(im);
        if(getNumSources() == 0) {
            addSource(src);
        } else {
            setSource(src, 0);
        }
        srcROI = roi;
        overlapBounds = overlap;

        // Add the source as fallback PropertySource.
        properties.addProperties(src);
    }

    /**
     * Creates a <code>Graphics</code> object that can be used to
     * paint text and graphics onto the <code>TiledImage</code>.
     * The <code>TiledImage</code> must be of integral data type
     * or an <code>UnsupportedOperationException</code> will be thrown.
     *
     * @deprecated as of JAI 1.1.
     */
    public Graphics getGraphics() {
        return createGraphics();
    }

    /**
     * Creates a <code>Graphics2D</code> object that can be used to
     * paint text and graphics onto the <code>TiledImage</code>.
     * The <code>TiledImage</code> must be of integral data type
     * or an <code>UnsupportedOperationException</code> will be thrown.
     */
    public Graphics2D createGraphics() {
        int dataType = sampleModel.getDataType();
        if(dataType != DataBuffer.TYPE_BYTE &&
           dataType != DataBuffer.TYPE_SHORT &&
           dataType != DataBuffer.TYPE_USHORT &&
           dataType != DataBuffer.TYPE_INT) {
            throw new UnsupportedOperationException(JaiI18N.getString("TiledImage0"));
        }
        return new TiledImageGraphics(this);
    }

    /**
     * Returns a <code>TiledImage</code> that shares the tile
     * <code>Raster</code>s of this image.  The returned image
     * occupies a sub-area of the parent image, and possesses a
     * possibly permuted subset of the parent's bands.  The two images
     * share a common coordinate system.
     *
     * <p> The image bounds are clipped against the bounds of the
     * parent image.
     *
     * <p> If the specified <code>ColorModel</code> is <code>null</code>
     * then the <code>ColorModel</code> of the sub-image will be set to
     * <code>null</code> unless <code>bandSelect</code> is either
     * <code>null</code> or equal in length to the number of bands in the
     * image in which cases the sub-image <code>ColorModel</code> will be
     * set to that of the current image.
     *
     * @param x the minimum X coordinate of the subimage.
     * @param y the minimum Y coordinate of the subimage.
     * @param w the width of the subimage.
     * @param h the height of the subimage.
     * @param bandSelect an array of band indices; if null,
     *                   all bands are selected.
     * @param cm the <code>ColorModel</code> of the sub-image.
     *
     * @return The requested sub-image or <code>null</code> if either
     *         the specified rectangular area or its intersection with
     *         the current image is empty.
     *
     * @since JAI 1.1
     */
    public TiledImage getSubImage(int x, int y, int w, int h,
                                  int[] bandSelect,
                                  ColorModel cm) {
        // Check for empty overlap.
        Rectangle subImageBounds = new Rectangle(x, y, w, h);
        if(subImageBounds.isEmpty()) {
            return null;
        }
        Rectangle overlap = subImageBounds.intersection(getBounds());
        if(overlap.isEmpty()) {
            return null;
        }

        // Use the original SampleModel or create a subset of it.
        SampleModel sm = bandSelect != null ?
            getSampleModel().createSubsetSampleModel(bandSelect) :
            getSampleModel();

        // Set the ColorModel.
        if(cm == null &&
           (bandSelect == null ||
            bandSelect.length == getSampleModel().getNumBands())) {
            cm = getColorModel();
        }

        // Create the sub-image.
        TiledImage subImage = new TiledImage(this, overlap.x, overlap.y,
                                             overlap.width, overlap.height,
                                             getTileGridXOffset(),
                                             getTileGridYOffset(),
                                             sm, cm);

        // Derive sub-image sub-band list with respect to the ancestor
        // TiledImage.  It is possible here that an
        // ArrayIndexOutOfBoundsException could be thrown if the user
        // is not careful.
        int[] subBandList = null;
        if(bandSelect != null) { // "this" is being sub-banded.
            if(bandList != null) { //"this" is a sub-band child.
                // Derive the sub-band list using the sub-band list of
                // "this" and the list passed in.
                subBandList = new int[bandSelect.length];
                for(int band = 0; band < bandSelect.length; band++) {
                    subBandList[band] = bandList[bandSelect[band]];
                }
            } else { // "this" is not a sub-band child.
                // Set the sub-band list to the list passed in.
                subBandList = bandSelect;
            }
        } else { // "this" is not being sub-banded.
            // Pass on the sub-band list of "this".
            subBandList = bandList;
        }

        // Set the sub-band list of the newly created sub-image.
        subImage.bandList = subBandList;

        return subImage;
    }

    /**
     * Returns a <code>TiledImage</code> that shares the tile
     * <code>Raster</code>s of this image.  The returned image
     * occupies a sub-area of the parent image, and possesses a
     * possibly permuted subset of the parent's bands.  The two images
     * share a common coordinate system.  The <code>ColorModel</code>
     * will be derived from the sub-image <code>SampleModel</code> using the
     * <code>createColorModel</code> method of <code>PlanarImage</code>.
     * Note that this implies that the <code>ColorModel</code> could be
     * <code>null</code>.
     *
     * <p> The image bounds are clipped against the bounds of the
     * parent image.
     *
     * @param x the minimum X coordinate of the subimage.
     * @param y the minimum Y coordinate of the subimage.
     * @param w the width of the subimage.
     * @param h the height of the subimage.
     * @param bandSelect an array of band indices; if null,
     *                   all bands are selected.
     *
     * @return The requested sub-image or <code>null</code> if either
     *         the specified rectangular area or its intersection with
     *         the current image is empty.
     *
     * @deprecated as of JAI 1.1.
     */
    public TiledImage getSubImage(int x, int y, int w, int h,
                                  int[] bandSelect) {
        SampleModel sm = bandSelect != null ?
            getSampleModel().createSubsetSampleModel(bandSelect) :
            getSampleModel();
        return getSubImage(x, y, w, h, bandSelect, createColorModel(sm));
    }

    /**
     * Returns a <code>TiledImage</code> that shares the tile
     * <code>Raster</code>s of this image.  The returned image
     * occupies a subarea of the parent image.  The two images share a
     * common coordinate system.
     *
     * <p> The image bounds are clipped against the bounds of the
     * parent image.
     *
     * @param x the minimum X coordinate of the subimage.
     * @param y the minimum Y coordinate of the subimage.
     * @param w the width of the subimage.
     * @param h the height of the subimage.
     */
    public TiledImage getSubImage(int x, int y, int w, int h) {
        return getSubImage(x, y, w, h, null, null);
    }

    /**
     * Returns a <code>TiledImage</code> that shares the tile
     * <code>Raster</code>s of this image.
     *
     * <p> If the specified <code>ColorModel</code> is <code>null</code>
     * then the <code>ColorModel</code> of the sub-image will be set to
     * <code>null</code> unless <code>bandSelect</code> is equal in length
     * to the number of bands in the image in which cases the sub-image
     * <code>ColorModel</code> will be set to that of the current image.
     *
     * @param bandSelect an array of band indices.
     * @param cm the <code>ColorModel</code> of the sub-image.
     *
     * @throws <code>IllegalArgumentException</code> is <code>bandSelect</code>
     *         is <code>null</code>.
     *
     * @since JAI 1.1
     */
    public TiledImage getSubImage(int[] bandSelect, ColorModel cm) {
        if(bandSelect == null) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }
        return getSubImage(getMinX(), getMinY(), getWidth(), getHeight(),
                           bandSelect, cm);
    }

    /**
     * Returns a <code>TiledImage</code> that shares the tile
     * <code>Raster</code>s of this image.  The returned image
     * occupies the same area as the parent image, and possesses a
     * possibly permuted subset of the parent's bands.  The
     * <code>ColorModel</code> will be derived from the sub-image
     * <code>SampleModel</code> using the <code>createColorModel</code>
     * method of <code>PlanarImage</code>.  Note that this implies that
     * the <code>ColorModel</code> could be <code>null</code>.
     *
     * @param bandSelect an array of band indices.
     *
     * @deprecated as of JAI 1.1.
     */
    public TiledImage getSubImage(int[] bandSelect) {
        if(bandSelect == null) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }
        return getSubImage(getMinX(), getMinY(), getWidth(), getHeight(),
                           bandSelect); // Deliberately using 5-param version.
    }

    /**
     * Forces the requested tile to be computed if has not already been so
     * and if a source is available.
     *
     * @throws ArrayIndexOutOfBoundsException if at least one of the supplied
     *         tile indices is out of the image.
     */
    private void createTile(int tileX, int tileY) {
        PlanarImage src = getNumSources() > 0 ? getSourceImage(0) : null;

        // If this is a child image with no source for uncomputed tiles
        // forward the call to the parent.
        if(src == null && parent != null) {
            parent.createTile(tileX, tileY);
            return;
        }

        synchronized(tiles) {
            // Do nothing if tile is non-null, i.e., already computed.
            if(tiles[tileX - minTileX][tileY - minTileY] == null) {
                // If sharing buffers, do so.
                if(areBuffersShared) {
                    Raster srcTile = src.getTile(tileX, tileY);
                    if(srcTile instanceof WritableRaster) {
                        tiles[tileX - minTileX][tileY - minTileY] =
                            (WritableRaster)srcTile;
                    } else {
                        Point location = new Point(srcTile.getMinX(),
                                                   srcTile.getMinY());
                        tiles[tileX - minTileX][tileY - minTileY] =
                            Raster.createWritableRaster(sampleModel,
                                                        srcTile.getDataBuffer(),
                                                        location);
                    }
                    return;
                }

                // Create the tile using the ancestor SampleModel.
                tiles[tileX - minTileX][tileY - minTileY] =
                    createWritableRaster(ancestorSampleModel,
                                         new Point(tileXToX(tileX),
                                                   tileYToY(tileY)));
                WritableRaster tile = tiles[tileX - minTileX][tileY - minTileY];

                // If a source is available try to set the tile's data.
                if(src != null) {
                    // Get the bounds of the tile's support.
                    Rectangle tileRect = getTileRect(tileX, tileY);

                    // Determine the intersection of the tile and the overlap.
                    Rectangle rect = overlapBounds.intersection(tileRect);

                    // Bail if this doesn't intersect the effective overlap.
                    if(rect.isEmpty()) {
                        return;
                    }

                    // If a source ROI is present, use it.
                    if(srcROI != null) {
                        // Attempt to get the ROI as a Shape.
                        Shape roiShape = srcROI.getAsShape();

                        if (roiShape != null) {
                            // Determine the area of overlap.
                            Area a = new Area(rect);
                            a.intersect(new Area(roiShape));

                            if(!a.isEmpty()) {
                                // If the area is non-empty overlay the pixels.
                                overlayPixels(tile, src, a);
                            }
                        } else {
                            int[][] bitmask =
                                srcROI.getAsBitmask(rect.x, rect.y,
                                                    rect.width, rect.height,
                                                    null);

                            overlayPixels(tile, src, rect, bitmask);
                        }
                    } else {
                        // If the intersection equals the tile area, copy data into
                        // the entire tile.  If the tile straddles the edge of the
                        // source, copy only into the intersection.
                        if(!rect.isEmpty()) {
                            if(bandList == null && rect.equals(tileRect)) {
                                // The current image has the same bands in the
                                // same order as its ancestor TiledImage and
                                // the requested tile is completely within "src".
                                if (tileRect.equals(tile.getBounds()))
                                    src.copyData(tile);
                                else
                                    src.copyData(
                                        tile.createWritableChild(rect.x, rect.y,
                                                                 rect.width,
                                                                 rect.height,
                                                                 rect.x,
                                                                 rect.y,
                                                                 null));
                            } else {
                                overlayPixels(tile, src, rect);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Retrieves a particular tile from the image for reading only.
     * The tile will be computed if it hasn't been previously.
     * Any attempt to write to the tile will produce undefined results.
     *
     * @param tileX the X index of the tile.
     * @param tileY the Y index of the tile.
     */
    public Raster getTile(int tileX, int tileY) {
        if(tileX < minTileX || tileY < minTileY ||
           tileX > getMaxTileX() || tileY > getMaxTileY()) {
            return null;
        }

        createTile(tileX, tileY);

        // For non-sub-banded image return the tile directly.
        if(bandList == null) {
            return (Raster)tiles[tileX - minTileX][tileY - minTileY];
        }

        // For sub-banded image return appropriate band subset.
        Raster r = (Raster)tiles[tileX - minTileX][tileY - minTileY];

        return r.createChild(r.getMinX(), r.getMinY(),
                             r.getWidth(), r.getHeight(),
                             r.getMinX(), r.getMinY(),
                             bandList);
    }

    /**
     * Retrieves a particular tile from the image for reading and writing.
     * If the tile is locked, null will be returned.  Otherwise, the tile
     * will be computed if it hasn't been previously.  Updates of the tile
     * will become visible to readers of this image as they occur.
     *
     * @param tileX the X index of the tile.
     * @param tileY the Y index of the tile.
     * @return The requested tile or null if the tile is locked.
     */
    public WritableRaster getWritableTile(int tileX, int tileY) {
        if(tileX < minTileX || tileY < minTileY ||
           tileX > getMaxTileX() || tileY > getMaxTileY()) {
            return null;
        }

        if(isTileLocked(tileX, tileY)) {
            return null;
        }

        createTile(tileX, tileY);
        ++writers[tileX - minTileX][tileY - minTileY];

        if (writers[tileX - minTileX][tileY - minTileY] == 1) {
            numWritableTiles[0]++;

            Enumeration e = tileObservers.elements();
            while (e.hasMoreElements()) {
                TileObserver t = (TileObserver)e.nextElement();
                t.tileUpdate(this, tileX, tileY, true);
            }
        }

        // For non-sub-banded image return the tile directly.
        if(bandList == null) {
            return tiles[tileX - minTileX][tileY - minTileY];
        }

        // For sub-banded image return appropriate band subset.
        WritableRaster wr = tiles[tileX - minTileX][tileY - minTileY];

        return wr.createWritableChild(wr.getMinX(), wr.getMinY(),
                                      wr.getWidth(), wr.getHeight(),
                                      wr.getMinX(), wr.getMinY(),
                                      bandList);
    }

    /**
     * Indicates that a writer is done updating a tile.
     * The effects of attempting to release a tile that has not been
     * grabbed, or releasing a tile more than once are undefined.
     *
     * @param tileX the X index of the tile.
     * @param tileY the Y index of the tile.
     */
    public void releaseWritableTile(int tileX, int tileY) {
        if(isTileLocked(tileX, tileY)) {
            return;
        }

        --writers[tileX - minTileX][tileY - minTileY];

        if (writers[tileX - minTileX][tileY - minTileY] < 0) {
            throw new RuntimeException(JaiI18N.getString("TiledImage1"));
        }

        if (writers[tileX - minTileX][tileY - minTileY] == 0) {
            numWritableTiles[0]--;

            Enumeration e = tileObservers.elements();
            while (e.hasMoreElements()) {
                TileObserver t = (TileObserver)e.nextElement();
                t.tileUpdate(this, tileX, tileY, false);
            }
        }
    }

    /**
     * Forces a tile to be computed, and its contents stored
     * indefinitely.  A tile may not be locked if it is currently
     * writable.  This method should only be used within JAI, in
     * order to optimize memory allocation.
     *
     * @param tileX the X index of the tile.
     * @param tileY the Y index of the tile.
     * @return Whether the tile was successfully locked.
     */
    protected boolean lockTile(int tileX, int tileY) {
        if(tileX < minTileX || tileY < minTileY ||
           tileX > getMaxTileX() || tileY > getMaxTileY()) {
            return false;
        }

        // Return false if the tile is writable.
        if(isTileWritable(tileX, tileY)) {
            return false;
        }

        // Force the tile to be computed if it has not yet been.
        createTile(tileX, tileY);

        // Set the corresponding writers count to -1.
        writers[tileX - minTileX][tileY - minTileY] = -1;

        return true;
    }

    /**
     * Returns <code>true</code> if a tile is locked.
     *
     * @param tileX the X index of the tile.
     * @param tileY the Y index of the tile.
     * @return Whether the tile is locked.
     */
    protected boolean isTileLocked(int tileX, int tileY) {
	return writers[tileX - minTileX][tileY - minTileY] < 0;
    }

    /**
     * Sets a region of a <code>TiledImage</code> to be a copy of a
     * supplied <code>Raster</code>.  The <code>Raster</code>'s
     * coordinate system is used to position it within the image.
     * The computation of all overlapping tiles will be forced prior
     * to modification of the data of the affected area.
     *
     * @param r a <code>Raster</code> containing pixels to be copied
     * into the <code>TiledImage</code>.
     */
    public void setData(Raster r) {
        // Return if the intersection of the image and Raster bounds is empty.
        Rectangle rBounds = r.getBounds();
        if((rBounds = rBounds.intersection(getBounds())).isEmpty()) {
            return;
        }

        // Set tile index limits.
        int txMin = XToTileX(rBounds.x);
        int tyMin = YToTileY(rBounds.y);
        int txMax = XToTileX(rBounds.x + rBounds.width - 1);
        int tyMax = YToTileY(rBounds.y + rBounds.height - 1);

        for(int ty = tyMin; ty <= tyMax; ty++) {
            for(int tx = txMin; tx <= txMax; tx++) {
                WritableRaster wr = getWritableTile(tx, ty);
                if(wr != null) {
                    // XXX bpb 02/04/1999
                    // All this checking shouldn't be necessary except
                    // that it doesn't look as if WritableRaster.setRect()
                    // correctly accounts for cases wherein the parameter
                    // Raster is not contained in the target WritableRaster.
                    Rectangle tileRect = getTileRect(tx, ty);
                    if(tileRect.contains(rBounds)) {
                        JDKWorkarounds.setRect(wr, r, 0, 0);
                    } else {
                        Rectangle xsect = rBounds.intersection(tileRect);
                        Raster rChild =
                            r.createChild(xsect.x, xsect.y,
                                          xsect.width, xsect.height,
                                          xsect.x, xsect.y, null);
                        WritableRaster wChild =
                            wr.createWritableChild(xsect.x, xsect.y,
                                                   xsect.width, xsect.height,
                                                   xsect.x, xsect.y, null);
                        JDKWorkarounds.setRect(wChild, rChild, 0, 0);
                    }
                    releaseWritableTile(tx, ty);
                }
            }
        }
    }

    /**
     * Sets a region of a <code>TiledImage</code> to be a copy of a
     * supplied <code>Raster</code>.  The <code>Raster</code>'s
     * coordinate system is used to position it within the image.
     * The computation of all overlapping tiles will be forced prior
     * to modification of the data of the affected area.
     *
     * @param r a <code>Raster</code> containing pixels to be copied
     * into the <code>TiledImage</code>.
     * @param roi The region of interest.
     */
    public void setData(Raster r, ROI roi) {
        // Return if the intersection of the image bounds, the Raster,
        // and the ROI bounds is empty.
        Rectangle rBounds = r.getBounds();
        if((rBounds = rBounds.intersection(getBounds())).isEmpty() ||
           (rBounds = rBounds.intersection(roi.getBounds())).isEmpty()) {
            return;
        }

        // Get the Rectangle list representation of the ROI.
        LinkedList rectList =
            roi.getAsRectangleList(rBounds.x, rBounds.y,
                                   rBounds.width, rBounds.height);

        // Set tile index limits.
        int txMin = XToTileX(rBounds.x);
        int tyMin = YToTileY(rBounds.y);
        int txMax = XToTileX(rBounds.x + rBounds.width - 1);
        int tyMax = YToTileY(rBounds.y + rBounds.height - 1);

        int numRects = rectList.size();

        for(int ty = tyMin; ty <= tyMax; ty++) {
            for(int tx = txMin; tx <= txMax; tx++) {
                WritableRaster wr = getWritableTile(tx, ty);
                if(wr != null) {
                    Rectangle tileRect = getTileRect(tx, ty);
                    for(int i = 0; i < numRects; i++) {
                        Rectangle rect = (Rectangle)rectList.get(i);
                        rect = rect.intersection(tileRect);
                        // XXX: Should the if-block below be split as in
                        // set(RenderedImage, ROI) above?
                        if(!rect.isEmpty()) {
                            Raster rChild =
                                r.createChild(rect.x, rect.y,
                                              rect.width, rect.height,
                                              rect.x, rect.y, null);
                            WritableRaster wChild =
                                wr.createWritableChild(rect.x, rect.y,
                                                       rect.width, rect.height,
                                                       rect.x, rect.y, null);
                            JDKWorkarounds.setRect(wChild, rChild, 0, 0);
                        }
                    }
                    releaseWritableTile(tx, ty);
                }
            }
        }
    }

    /**
     * Informs this <code>TiledImage</code> that another object is
     * interested in being notified whenever any tile becomes writable
     * or ceases to be writable.  A tile becomes writable when it is
     * not currently writable and <code>getWritableTile()</code> is
     * called.  A tile ceases to be writable when
     * <code>releaseTile()</code> is called and the number of calls to
     * <code>getWritableTile()</code> and
     * <code>releaseWritableTile()</code> are identical.
     *
     * <p> It is the responsibility of the <code>TiledImage</code> to
     * inform all registered <code>TileObserver</code> objects of such
     * changes in tile writability before the writer has a chance to
     * make any modifications.
     *
     * @param observer An object implementing the
     * <code>TileObserver</code> interface.
     */
    public void addTileObserver(TileObserver observer) {
        tileObservers.addElement(observer);
    }

    /**
     * Informs this <code>TiledImage</code> that a particular TileObserver no
     * longer wishes to receive updates on tile writability status.
     * The result of attempting to remove a listener that is not
     * registered is undefined.
     *
     * @param observer An object implementing the
     * <code>TileObserver</code> interface.
     */
    public void removeTileObserver(TileObserver observer) {
        tileObservers.removeElement(observer);
    }

    /**
     * Returns a list of tiles that are currently held by one or more
     * writers or <code>null</code> of no tiles are so held.
     *
     * @return An array of <code>Point</code>s representing tile indices
     *         or <code>null</code>.
     */
    public Point[] getWritableTileIndices() {
        Point[] indices = null;

        if(hasTileWriters()) {
            Vector v = new Vector();
            int count = 0;

            for (int j = 0; j < tilesY; j++) {
                for (int i = 0; i < tilesX; i++) {
                    if (writers[i][j] > 0) {
                        v.addElement(new Point(i + minTileX, j + minTileY));
                        ++count;
                    }
                }
            }

            indices = new Point[count];
            for (int k = 0; k < count; k++) {
                indices[k] = (Point)v.elementAt(k);
            }
        }

        return indices;
    }

    /**
     * Returns <code>true</code> if any tile is being held by a
     * writer, <code>false</code> otherwise.  This provides a quick
     * way to check whether it is necessary to make copies of tiles --
     * if there are no writers, it is safe to use the tiles directly,
     * while registering to learn of future writers.
     */
    public boolean hasTileWriters() {
        return numWritableTiles[0] > 0;
    }

    /**
     * Returns <code>true</code> if a tile has writers.
     *
     * @param tileX the X index of the tile.
     * @param tileY the Y index of the tile.
     */
    public boolean isTileWritable(int tileX, int tileY) {
        return writers[tileX - minTileX][tileY - minTileY] > 0;
    }

    /**
     * Sets the <code>tiles</code> array to <code>null</code> so that
     * the image may be used again.
     *
     * @throws IllegalStateException if <code>hasTileWriters()</code>
     * returns <code>true</code>.
     *
     * @since JAI 1.1.2
     */
    public void clearTiles() {
        if(hasTileWriters()) {
            throw new IllegalStateException(JaiI18N.getString("TiledImage2"));
        }
        tiles = null;
    }

    /*
    private int[] XToTileXArray = null;
    private int[] YToTileYArray = null;

    private void initTileArrays() {
        if (XTileTileXArray == null) {
            XToTileXArray = new int[width];
            YToTileYArray = new int[height];

            for (int i = 0; i < width; i++) {
                XToTileXArray[i] = XToTileX(minX + i);
            }

            for (int j = 0; j < height; j++) {
                YToTileYArray[j] = YToTileY(minY + j);
            }
        }
    }
    */

    /**
     * Sets a sample of a pixel to a given <code>int</code> value.
     *
     * @param x The X coordinate of the pixel.
     * @param y The Y coordinate of the pixel.
     * @param b The band of the sample within the pixel.
     * @param s The value to which to set the sample.
     */
    public void setSample(int x, int y, int b, int s) {
        int tileX = XToTileX(x);
        int tileY = YToTileY(y);
        WritableRaster t = getWritableTile(tileX, tileY);
        if(t != null) {
            t.setSample(x, y, b, s);
        }
        releaseWritableTile(tileX, tileY);
    }

    /**
     * Returns the value of a given sample of a pixel as an <code>int</code>.
     *
     * @param x The X coordinate of the pixel.
     * @param y The Y coordinate of the pixel.
     * @param b The band of the sample within the pixel.
     */
    public int getSample(int x, int y, int b) {
        int tileX = XToTileX(x);
        int tileY = YToTileY(y);
        Raster t = getTile(tileX, tileY);
        return t.getSample(x, y, b);
    }

    /**
     * Sets a sample of a pixel to a given <code>float</code> value.
     *
     * @param x The X coordinate of the pixel.
     * @param y The Y coordinate of the pixel.
     * @param b The band of the sample within the pixel.
     * @param s The value to which to set the sample.
     */
    public void setSample(int x, int y, int b, float s) {
        int tileX = XToTileX(x);
        int tileY = YToTileY(y);
        WritableRaster t = getWritableTile(tileX, tileY);
        if(t != null) {
            t.setSample(x, y, b, s);
        }
        releaseWritableTile(tileX, tileY);
    }

    /**
     * Returns the value of a given sample of a pixel as a <code>float</code>.
     *
     * @param x The X coordinate of the pixel.
     * @param y The Y coordinate of the pixel.
     * @param b The band of the sample within the pixel.
     */
    public float getSampleFloat(int x, int y, int b) {
        int tileX = XToTileX(x);
        int tileY = YToTileY(y);
        Raster t = getTile(tileX, tileY);
        return t.getSampleFloat(x, y, b);
    }

    /**
     * Sets a sample of a pixel to a given <code>double</code> value.
     *
     * @param x The X coordinate of the pixel.
     * @param y The Y coordinate of the pixel.
     * @param b The band of the sample within the pixel.
     * @param s The value to which to set the sample.
     */
    public void setSample(int x, int y, int b, double s) {
        int tileX = XToTileX(x);
        int tileY = YToTileY(y);
        WritableRaster t = getWritableTile(tileX, tileY);
        if(t != null) {
            t.setSample(x, y, b, s);
        }
        releaseWritableTile(tileX, tileY);
    }

    /**
     * Returns the value of a given sample of a pixel as a <code>double</code>.
     *
     * @param x The X coordinate of the pixel.
     * @param y The Y coordinate of the pixel.
     * @param b The band of the sample within the pixel.
     */
    public double getSampleDouble(int x, int y, int b) {
        int tileX = XToTileX(x);
        int tileY = YToTileY(y);
        Raster t = getTile(tileX, tileY);
        return t.getSampleDouble(x, y, b);
    }

    /**
     * Implementation of <code>PropertyChangeListener</code>.
     *
     * <p> When invoked with an event emitted by the source image specified
     * for this <code>TiledImage</code> and the event is either a
     * <code>PropertyChangeEventJAI</code> named "InvalidRegion"
     * (case-insensitive) or a <code>RenderingChangeEvent</code>, then
     * all tiles which overlap the intersection of the invalid region and the
     * region of interest specified for this image (if any) will
     * be cleared.  If the event is a <code>RenderingChangeEvent</code> then
     * the invalid region will be obtained from the <code>getInvalidRegion</code>
     * method of the event object; if a <code>PropertyChangeEventJAI</code>
     * it will be obtained from the <code>getNewValue()</code> method.
     * In either case, a new <code>PropertyChangeEventJAI</code> will be
     * fired to all registered listeners of the property name
     * "InvalidRegion" and to all known sinks which are
     * <code>PropertyChangeListener</code>s.  Its old and new values will
     * contain the previous and current invalid regions.  This may be used to
     * determine which tiles must be re-requested.  The
     * <code>TiledImage</code> itself will not re-request the data.
     *
     * @since JAI 1.1
     */
    public synchronized void propertyChange(PropertyChangeEvent evt) {
        PlanarImage src = getNumSources() > 0 ? getSourceImage(0) : null;

        if(evt.getSource() == src &&
           (evt instanceof RenderingChangeEvent ||
            (evt instanceof PropertyChangeEventJAI &&
             evt.getPropertyName().equalsIgnoreCase("InvalidRegion")))) {

            // Get the region.
            Shape invalidRegion =
                evt instanceof RenderingChangeEvent ?
                ((RenderingChangeEvent)evt).getInvalidRegion() :
                (Shape)evt.getNewValue();

            // If empty, all is valid.
            Rectangle invalidBounds = invalidRegion.getBounds();
            if(invalidBounds.isEmpty()) {
                return;
            }

            // Intersect with ROI.
            Area invalidArea = new Area(invalidRegion);
            if(srcROI != null) {
                Shape roiShape = srcROI.getAsShape();
                if(roiShape != null) {
                    invalidArea.intersect(new Area(roiShape));
                } else {
                    LinkedList rectList =
                        srcROI.getAsRectangleList(invalidBounds.x,
                                                  invalidBounds.y,
                                                  invalidBounds.width,
                                                  invalidBounds.height);
                    Iterator it = rectList.iterator();
                    while(it.hasNext() && !invalidArea.isEmpty()) {
                        invalidArea.intersect(new Area((Rectangle)it.next()));
                    }
                }
            }

            // If empty, all is valid.
            if(invalidArea.isEmpty()) {
                return;
            }

            // Determine all possible overlapping tiles.
            Point[] tileIndices = getTileIndices(invalidArea.getBounds());
            int numIndices = tileIndices.length;

            // Clear any tiles which intersect the invalid area.
            for(int i = 0; i < numIndices; i++) {
                int tx = tileIndices[i].x;
                int ty = tileIndices[i].y;
                Raster tile = tiles[tx][ty];
                if ((tile != null) &&
		    invalidArea.intersects(tile.getBounds())) {
                    tiles[tx][ty] = null;
                }
            }

            if(eventManager.hasListeners("InvalidRegion")) {
                // Determine the old invalid region.
                Shape oldInvalidRegion = new Rectangle(); // default is empty.

                // If there is a ROI, the old invalid region is the
                // complement of the ROI within the image bounds.
                if(srcROI != null) {
                    Area oldInvalidArea = new Area(getBounds());
                    Shape roiShape = srcROI.getAsShape();
                    if(roiShape != null) {
                        oldInvalidArea.subtract(new Area(roiShape));
                    } else {
                        Rectangle oldInvalidBounds =
                            oldInvalidArea.getBounds();
                        LinkedList rectList =
                            srcROI.getAsRectangleList(oldInvalidBounds.x,
                                                      oldInvalidBounds.y,
                                                      oldInvalidBounds.width,
                                                      oldInvalidBounds.height);
                        Iterator it = rectList.iterator();
                        while(it.hasNext() && !oldInvalidArea.isEmpty()) {
                            oldInvalidArea.subtract(new Area((Rectangle)it.next()));
                        }
                    }
                    oldInvalidRegion = oldInvalidArea;
                }

                // Fire an InvalidRegion event.
                PropertyChangeEventJAI irEvt =
                    new PropertyChangeEventJAI(this, "InvalidRegion",
                                               oldInvalidRegion,
                                               invalidRegion);

                // Fire an event to all registered PropertyChangeListeners.
                eventManager.firePropertyChange(irEvt);

                // Fire an event to all PropertyChangeListener sinks.
                Vector sinks = getSinks();
                if(sinks != null) {
                    int numSinks = sinks.size();
                    for(int i = 0; i < numSinks; i++) {
                        Object sink = sinks.get(i);
                        if(sink instanceof PropertyChangeListener) {
                            ((PropertyChangeListener)sink).propertyChange(irEvt);
                        }
                    }
                }
            }
        }
    }
}
