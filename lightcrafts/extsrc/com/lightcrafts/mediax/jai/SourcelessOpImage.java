/*
 * $RCSfile: SourcelessOpImage.java,v $
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
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.util.Map;

/**
 * An abstract base class for image operators that have no image
 * sources.
 *
 * <p> <code>SourcelessOpImage</code> is intended as a convenient superclass
 * for <code>OpImage</code>s that have no source image.  Some examples are
 * constant color images, file readers, protocol-based network readers,
 * and mathematically-defined imagery such as fractals.
 *
 * <p> The <code>computeTile</code> method of this class will call the
 * <code>computeRect(PlanarImage[], WritableRaster, Rectangle)</code>
 * method of the subclass to perform the computation.  The first
 * argument will be <code>null</code> as there are no source images.
 *
 * @see OpImage
 */
public abstract class SourcelessOpImage extends OpImage {

    private static ImageLayout layoutHelper(int minX, int minY,
                                            int width, int height,
                                            SampleModel sampleModel,
                                            ImageLayout il) {
        ImageLayout layout = (il == null) ?
            new ImageLayout() : (ImageLayout)il.clone();

        layout.setMinX(minX);
        layout.setMinY(minY);
        layout.setWidth(width);
        layout.setHeight(height);
        layout.setSampleModel(sampleModel);

        if (!layout.isValid(ImageLayout.TILE_GRID_X_OFFSET_MASK)) {
            layout.setTileGridXOffset(layout.getMinX(null));
        }
        if (!layout.isValid(ImageLayout.TILE_GRID_Y_OFFSET_MASK)) {
            layout.setTileGridYOffset(layout.getMinY(null));
        }

        return layout;
    }

    /**
     * Constructs a <code>SourcelessOpImage</code>.  The image bounds
     * and <code>SampleModel</code> are set explicitly; other layout
     * parameters may be set using the <code>layout</code> parameter.
     * The min X, min Y, width, height, and <code>SampleModel</code>
     * fields of the <code>layout</code> parameter are ignored.
     *
     * <p> If <code>sampleModel</code> is <code>null</code>, no
     * exceptions will be thrown.  However, the caller must be sure to
     * set the <code>sampleModel</code> instance variable before
     * construction terminates.  This feature allows subclasses that
     * require external computation such as file loading to defer the
     * determination of their <code>SampleModel</code> until after the
     * call to <code>super</code>.
     *
     * <p> Similarly, <code>minX</code>, <code>minY</code>,
     * <code>width</code>, and <code>height</code> may be dummy values
     * if care is taken to manually set all values that depend on
     * them, namely the tile grid offset, tile size, and
     * <code>SampleModel</code> width and height.
     *
     * <p> The tile dimensions, tile grid X and Y offsets, and
     * <code>ColorModel</code> of the output will be set in the standard
     * way by the <code>OpImage</code> constructor.
     *
     * @param configuration Configurable attributes of the image including
     *        configuration variables indexed by
     *        <code>RenderingHints.Key</code>s and image properties indexed
     *        by <code>String</code>s or <code>CaselessStringKey</code>s.
     *        This is simply forwarded to the superclass constructor.
     * @param layout an <code>ImageLayout</code> describing the layout.
     *
     * @since JAI 1.1
     */
    public SourcelessOpImage(ImageLayout layout,
                             Map configuration,
                             SampleModel sampleModel,
                             int minX, int minY,
                             int width, int height) {
        super(null,
              layoutHelper(minX, minY, width, height, sampleModel, layout),
              configuration,
              false);
    }

    /**
     * Returns false as SourcelessOpImages often return Rasters
     * via computeTile() tile that are internally cached.  Some
     * subclasses may want to override this method and return true.
     */
    public boolean computesUniqueTiles() {
        return false;
    }

    /**
     * Computes a tile.  Since the operation has no sources,
     * there is no need to worry about cobbling.
     *
     * <p> Subclasses should implement the
     * <code>computeRect(PlanarImage[], WritableRaster, Rectangle)</code>
     * method to perform the actual computation.
     *
     * @param tileX The X index of the tile.
     * @param tileY The Y index of the tile.
     */
    public Raster computeTile(int tileX, int tileY) {
        /* Create a new WritableRaster to represent this tile. */
        Point org = new Point(tileXToX(tileX), tileYToY(tileY));
        WritableRaster dest = createWritableRaster(sampleModel, org);

        /* Clip output rectangle to image bounds. */
        Rectangle rect = new Rectangle(org.x, org.y,
                                       sampleModel.getWidth(),
                                       sampleModel.getHeight());
        Rectangle destRect = rect.intersection(getBounds());
        computeRect((PlanarImage[])null, dest, destRect);
        return dest;
    }
    
    /**
     * Throws an IllegalArgumentException since the image has no image
     * sources.
     *
     * @param sourceRect ignored.
     * @param sourceIndex ignored.
     *
     * @throws IllegalArgumentException since the image has no sources.
     */
    public Rectangle mapSourceRect(Rectangle sourceRect,
                                   int sourceIndex) {
        throw new IllegalArgumentException(
            JaiI18N.getString("SourcelessOpImage0"));
    }
    
    /**
     * Throws an IllegalArgumentException since the image has no image
     * sources.
     *
     * @param destRect ignored.
     * @param sourceIndex ignored.
     *
     * @throws IllegalArgumentException since the image has no sources.
     */
    public Rectangle mapDestRect(Rectangle destRect,
                                 int sourceIndex) {
        throw new IllegalArgumentException(
            JaiI18N.getString("SourcelessOpImage0"));
    }
}
