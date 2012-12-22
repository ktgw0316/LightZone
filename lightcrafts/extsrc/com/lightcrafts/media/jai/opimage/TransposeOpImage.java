/*
 * $RCSfile: TransposeOpImage.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.2 $
 * $Date: 2005/12/13 21:23:06 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.opimage;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import com.lightcrafts.mediax.jai.GeometricOpImage;
import com.lightcrafts.mediax.jai.ImageLayout;
import com.lightcrafts.mediax.jai.IntegerSequence;
import com.lightcrafts.mediax.jai.PlanarImage;
import com.lightcrafts.mediax.jai.RasterAccessor;
import com.lightcrafts.mediax.jai.RasterFormatTag;
import java.util.Map;

/**
 * An OpImage class to perform transposition and 90 degree rotation
 * of an image.
 *
 * @since EA2
 *
 */
public class TransposeOpImage extends GeometricOpImage {

    /** The Transpose type */
    protected int type;

    /**
     * Store source width & height
     */
    protected int src_width, src_height;

    protected Rectangle sourceBounds;

    // Set the bounds and tile grid of the output image.
    private static ImageLayout layoutHelper(ImageLayout layout,
                                            RenderedImage source,
                                            int type) {
        ImageLayout newLayout;
        if (layout != null) {
            newLayout = (ImageLayout)layout.clone();
        } else {
            newLayout = new ImageLayout();
        }

        // Set the size of the destination to exactly cover the
        // forward-mapped source image bounds
        Rectangle sourceBounds = new Rectangle(source.getMinX(),
                                               source.getMinY(),
                                               source.getWidth(),
                                               source.getHeight());
        Rectangle rect = mapRect(sourceBounds, sourceBounds, type, true);

        newLayout.setMinX(rect.x);
        newLayout.setMinY(rect.y);
        newLayout.setWidth(rect.width);
        newLayout.setHeight(rect.height);

        // Make each destination tile correspond to a source tile
        Rectangle tileRect = new Rectangle(source.getTileGridXOffset(),
                                           source.getTileGridYOffset(),
                                           source.getTileWidth(),
                                           source.getTileHeight());
        rect = mapRect(tileRect, sourceBounds, type, true);

        // Respect any pre-existing tile grid settings
        if (newLayout.isValid(ImageLayout.TILE_GRID_X_OFFSET_MASK)) {
            newLayout.setTileGridXOffset(rect.x);
        }
        if (newLayout.isValid(ImageLayout.TILE_GRID_Y_OFFSET_MASK)) {
            newLayout.setTileGridYOffset(rect.y);
        }
        if (newLayout.isValid(ImageLayout.TILE_WIDTH_MASK)) {
            newLayout.setTileWidth(Math.abs(rect.width));
        }
        if (newLayout.isValid(ImageLayout.TILE_HEIGHT_MASK)) {
            newLayout.setTileHeight(Math.abs(rect.height));
        }

        return newLayout;
    }

    /**
     * Constructs an TransposeOpImage from a RenderedImage source,
     * and Transpose type.  The image dimensions are determined by
     * forward-mapping the source bounds.
     * The tile grid layout, SampleModel, and ColorModel are specified
     * by the image source, possibly overridden by values from the
     * ImageLayout parameter.
     *
     * @param source a RenderedImage.
     * @param layout an ImageLayout optionally containing the tile grid layout,
     *        SampleModel, and ColorModel, or null.
     * @param type the desired Tranpose type.
     */
    public TransposeOpImage(RenderedImage source,
                            Map config,
                            ImageLayout layout,
                            int type) {
        super(vectorize(source),
              layoutHelper(layout,
                           source,
                           type),
              config,
              true,
              null, // BorderExtender
              null,
              null); // Interpolation (superclass defaults to nearest neighbor)

        // If the source has an IndexColorModel, override the default setting
        // in OpImage. The dest shall have exactly the same SampleModel and
        // ColorModel as the source.
        // Note, in this case, the source should have an integral data type.
        ColorModel srcColorModel = source.getColorModel();
        if (srcColorModel instanceof IndexColorModel) {
             sampleModel = source.getSampleModel().createCompatibleSampleModel(
                                                   tileWidth, tileHeight);
             colorModel = srcColorModel;
        }

        // store the Transpose type
        this.type = type;

        // Store the source width & height
        this.src_width = source.getWidth();
        this.src_height = source.getHeight();

        this.sourceBounds = new Rectangle(source.getMinX(),
                                          source.getMinY(),
                                          source.getWidth(),
                                          source.getHeight());
    }

    /**
     * Forward map the source Rectangle.
     */
    protected Rectangle forwardMapRect(Rectangle sourceRect,
                                       int sourceIndex) {
        return mapRect(sourceRect, sourceBounds, type, true);
    }

    /**
     * Backward map the destination Rectangle.
     */
    protected Rectangle backwardMapRect(Rectangle destRect,
                                        int sourceIndex) {
        return mapRect(destRect, sourceBounds, type, false);
    }

    /**
     * Map a point according to the transposition type.
     * If <code>mapForwards</code> is <code>true</code>,
     * the point is considered to lie in the source image and
     * is mapping into the destination space.  Otherwise,
     * the point lies in the destination and is mapped
     * into the source space.
     *
     * <p> In either case, the bounds of the source image
     * must be supplied.  The bounds are given by the indices
     * of the upper left and lower right pixels, i.e.,
     * maxX = minX + width - 1 and similarly for maxY.
     */
    protected static void mapPoint(int[] pt,
                                   int minX, int minY,
                                   int maxX, int maxY,
                                   int type,
                                   boolean mapForwards) {
        int sx = pt[0];
        int sy = pt[1];
        int dx = -1;
        int dy = -1;

        switch (type) {
        case 0: // FLIP_VERTICAL
            dx = sx;
            dy = minY + maxY - sy;
            break;

        case 1: // FLIP_HORIZONTAL
            dx = minX + maxX - sx;
            dy = sy;
            break;

        case 2: // FLIP_DIAGONAL
            dx = minX - minY + sy;
            dy = minY - minX + sx;
            break;

        case 3: // FLIP_ANTIDIAGONAL
            if (mapForwards) {
                dx = minX + maxY - sy;
                dy = minY + maxX - sx;
            } else {
                dx = minY + maxX - sy;
                dy = minX + maxY - sx;
            }
            break;

        case 4: // ROTATE_90
            if (mapForwards) {
                dx = minX + maxY - sy;
                dy = minY - minX + sx;
            } else {
                dx = minX - minY + sy;
                dy = minX + maxY - sx;
            }
            break;

        case 5: // ROTATE_180
            dx = minX + maxX - sx;
            dy = minY + maxY - sy;
            break;

        case 6: // ROTATE_270
            if (mapForwards) {
                dx = minX - minY + sy;
                dy = maxX + minY - sx;
            } else {
                dx = maxX + minY - sy;
                dy = minY - minX + sx;
            }
            break;
        }

        pt[0] = dx;
        pt[1] = dy;
    }

    private static Rectangle mapRect(Rectangle rect,
                                     Rectangle sourceBounds,
                                     int type,
                                     boolean mapForwards) {
        int sMinX = sourceBounds.x;
        int sMinY = sourceBounds.y;
        int sMaxX = sMinX + sourceBounds.width - 1;
        int sMaxY = sMinY + sourceBounds.height - 1;
        int dMinX, dMinY, dMaxX, dMaxY;

        int[] pt = new int[2];
        pt[0] = rect.x;
        pt[1] = rect.y;
        mapPoint(pt, sMinX, sMinY, sMaxX, sMaxY, type, mapForwards);
        dMinX = dMaxX = pt[0];
        dMinY = dMaxY = pt[1];

        pt[0] = rect.x + rect.width - 1;
        pt[1] = rect.y;
        mapPoint(pt, sMinX, sMinY, sMaxX, sMaxY, type, mapForwards);
        dMinX = Math.min(dMinX, pt[0]);
        dMinY = Math.min(dMinY, pt[1]);
        dMaxX = Math.max(dMaxX, pt[0]);
        dMaxY = Math.max(dMaxY, pt[1]);

        pt[0] = rect.x;
        pt[1] = rect.y + rect.height - 1;
        mapPoint(pt, sMinX, sMinY, sMaxX, sMaxY, type, mapForwards);
        dMinX = Math.min(dMinX, pt[0]);
        dMinY = Math.min(dMinY, pt[1]);
        dMaxX = Math.max(dMaxX, pt[0]);
        dMaxY = Math.max(dMaxY, pt[1]);

        pt[0] = rect.x + rect.width - 1;
        pt[1] = rect.y + rect.height - 1;
        mapPoint(pt, sMinX, sMinY, sMaxX, sMaxY, type, mapForwards);
        dMinX = Math.min(dMinX, pt[0]);
        dMinY = Math.min(dMinY, pt[1]);
        dMaxX = Math.max(dMaxX, pt[0]);
        dMaxY = Math.max(dMaxY, pt[1]);

        return new Rectangle(dMinX, dMinY,
                             dMaxX - dMinX + 1,
                             dMaxY - dMinY + 1);
    }

    public Raster computeTile(int tileX, int tileY) {
        // Create a new WritableRaster.
        Point org = new Point(tileXToX(tileX), tileYToY(tileY));
        WritableRaster dest = createWritableRaster(sampleModel, org);

        // Output bounds are initially equal to the tile bounds.
        int destMinX = dest.getMinX();
        int destMinY = dest.getMinY();
        int destMaxX = destMinX + dest.getWidth();
        int destMaxY = destMinY + dest.getHeight();

        // Clip output bounds to the dest image bounds.
        Rectangle bounds = getBounds();
        if (destMinX < bounds.x) {
            destMinX = bounds.x;
        }
        int boundsMaxX = bounds.x + bounds.width;
        if (destMaxX > boundsMaxX) {
            destMaxX = boundsMaxX;
        }
        if (destMinY < bounds.y) {
            destMinY = bounds.y;
        }
        int boundsMaxY = bounds.y + bounds.height;
        if (destMaxY > boundsMaxY) {
            destMaxY = boundsMaxY;
        }

        if (destMinX >= destMaxX || destMinY >= destMaxY) {
            return dest; // nothing to write
        }

        // Initialize the (possibly clipped) destination Rectangle.
        Rectangle destRect = new Rectangle(destMinX, destMinY,
                                           destMaxX - destMinX,
                                           destMaxY - destMinY);

        // Initialize X and Y split sequences with the dest bounds
        IntegerSequence xSplits =
            new IntegerSequence(destMinX, destMaxX);
        xSplits.insert(destMinX);
        xSplits.insert(destMaxX);

        IntegerSequence ySplits =
            new IntegerSequence(destMinY, destMaxY);
        ySplits.insert(destMinY);
        ySplits.insert(destMaxY);

        // Overlay the forward-mapped source tile grid
        PlanarImage src = getSource(0);
        int sMinX = src.getMinX();
        int sMinY = src.getMinY();
        int sWidth = src.getWidth();
        int sHeight = src.getHeight();
        int sMaxX = sMinX + sWidth - 1;
        int sMaxY = sMinY + sHeight - 1;
        int sTileWidth = src.getTileWidth();
        int sTileHeight = src.getTileHeight();
        int sTileGridXOffset = src.getTileGridXOffset();
        int sTileGridYOffset = src.getTileGridYOffset();

        int xStart = 0;
        int xGap = 0;
        int yStart = 0;
        int yGap = 0;

        // Insert splits from source image.
        //
        // We can think of the splits as forming an infinite sequence
        // xStart + kx*xGap, yStart + ky*yGap, where kx and ky range
        // over all integers, negative and positive.

        // Forward map the source tile grid origin Note that in cases
        // where an axis is "flipped" an adjustment must be made.  For
        // example, consider flipping an image horizontally.
        // If the image has a tile X origin of 0, a tile width
        // of 50, and a total width of 100, then forward mapping the
        // points (0, 0) and (50, 0) yields the points (99, 0) and
        // (49, 0).  In the original image, the tile split lines lay
        // to the left of the pixel; in the flipped image, they lie
        // to the right of the forward mapped pixels.  Thus 1 must
        // be added to the forward mapped pixel position to get the
        // correct split location.
        int[] pt = new int[2];
        pt[0] = sTileGridXOffset;
        pt[1] = sTileGridYOffset;
        mapPoint(pt, sMinX, sMinY, sMaxX, sMaxY, type, true);
        xStart = pt[0];
        yStart = pt[1];

        // Forward map the input tile size
        switch (type) {
        case 0: // FLIP_VERTICAL
            ++yStart;
            xGap = sTileWidth;
            yGap = sTileHeight;
            break;

        case 1: // FLIP_HORIZONTAL
            ++xStart;
            xGap = sTileWidth;
            yGap = sTileHeight;
            break;

        case 2: // FLIP_DIAGONAL
            xGap = sTileHeight;
            yGap = sTileWidth;
            break;

        case 3: // FLIP_ANTIDIAGONAL
            ++xStart;
            ++yStart;
            xGap = sTileHeight;
            yGap = sTileWidth;
            break;

        case 4: // ROTATE_90
            ++xStart;
            xGap = sTileHeight;
            yGap = sTileWidth;
            break;

        case 5: // ROTATE_180
            ++xStart;
            ++yStart;
            xGap = sTileWidth;
            yGap = sTileHeight;
            break;

        case 6: // ROTATE_270
            ++yStart;
            xGap = sTileHeight;
            yGap = sTileWidth;
            break;
        }

        // Now we identify the source splits that intersect
        // the destination rectangle and merge them in.
        int kx = (int)Math.floor((double)(destMinX - xStart)/xGap);
        int xSplit = xStart + kx*xGap;
        while (xSplit < destMaxX) {
            xSplits.insert(xSplit);
            xSplit += xGap;
        }

        int ky = (int)Math.floor((double)(destMinY - yStart)/yGap);
        int ySplit = yStart + ky*yGap;
        while (ySplit < destMaxY) {
            ySplits.insert(ySplit);
            ySplit += yGap;
        }

        // Allocate memory for source Rasters.
        Raster[] sources = new Raster[1];

        //
        // Divide destRect into sub rectangles based on the source
        // splits, and compute each sub rectangle separately.
        //
        int x1, x2, y1, y2, w, h;
        Rectangle subRect = new Rectangle();

        ySplits.startEnumeration();
        for (y1 = ySplits.nextElement(); ySplits.hasMoreElements(); y1 = y2) {
            y2 = ySplits.nextElement();
            h = y2 - y1;

            xSplits.startEnumeration();
            for (x1 = xSplits.nextElement();
                 xSplits.hasMoreElements(); x1 = x2) {
                x2 = xSplits.nextElement();
                w = x2 - x1;

                // Get sources

                // Backwards map the starting destination point
                pt[0] = x1;
                pt[1] = y1;
                mapPoint(pt, sMinX, sMinY, sMaxX, sMaxY, type, false);

                // Determine the source tile involved
                int tx = src.XToTileX(pt[0]);
                int ty = src.YToTileY(pt[1]);
                sources[0] = src.getTile(tx, ty);

                subRect.x = x1;
                subRect.y = y1;
                subRect.width = w;
                subRect.height = h;
                computeRect(sources, dest, subRect);
            }
        }

        return dest;
    }

    protected void computeRect(Raster[] sources,
                               WritableRaster dest,
                               Rectangle destRect) {
        // Retrieve format tags.
        RasterFormatTag[] formatTags = getFormatTags();
        Raster src = sources[0];

        //
        // Get the minX, minY, width & height of sources raster
        //
        PlanarImage source = getSource(0);
        int sMinX = source.getMinX();
        int sMinY = source.getMinY();
        int sWidth = source.getWidth();
        int sHeight = source.getHeight();
        int sMaxX = sMinX + sWidth - 1;
        int sMaxY = sMinY + sHeight - 1;

        int translateX = src.getSampleModelTranslateX();
        int translateY = src.getSampleModelTranslateY();

        //
        // Get data for the source rectangle & the destination rectangle
        Rectangle srcRect = src.getBounds();

        RasterAccessor srcAccessor =
            new RasterAccessor(src, srcRect,
                               formatTags[0],
                               getSource(0).getColorModel());
        RasterAccessor dstAccessor =
            new RasterAccessor(dest, destRect, formatTags[1], getColorModel());

        int incr1 = 0, incr2 = 0, s_x = 0, s_y = 0;
        int srcPixelStride = srcAccessor.getPixelStride();
        int srcScanlineStride = srcAccessor.getScanlineStride();

        // Backwards map starting point of destination rectangle
        int[] pt = new int[2];
        pt[0] = destRect.x;
        pt[1] = destRect.y;
        mapPoint(pt, sMinX, sMinY, sMaxX, sMaxY, type, false);
        s_x = pt[0];
        s_y = pt[1];

        // Determine source stride along dest row (incr1) and column (incr2)
        switch (type) {
        case 0: // FLIP_VERTICAL
            incr1 = srcPixelStride;
            incr2 = -srcScanlineStride;
            break;

        case 1: // FLIP_HORIZONTAL
            incr1 = -srcPixelStride;
            incr2 = srcScanlineStride;
            break;

        case 2: // FLIP_DIAGONAL;
            incr1 = srcScanlineStride;
            incr2 = srcPixelStride;
            break;

        case 3: // FLIP_ANTIDIAGONAL
            incr1 = -srcScanlineStride;
            incr2 = -srcPixelStride;
            break;

        case 4: // ROTATE_90
            incr1 = -srcScanlineStride;
            incr2 = srcPixelStride;
            break;

        case 5: // ROTATE_180
            incr1 = -srcPixelStride;
            incr2 = -srcScanlineStride;
            break;

        case 6: // ROTATE_270
            incr1 = srcScanlineStride;
            incr2 = -srcPixelStride;
            break;
        }

        switch (dstAccessor.getDataType()) {
        case DataBuffer.TYPE_BYTE:
            byteLoop(srcAccessor,
                     destRect,
                     translateX,
                     translateY,
                     dstAccessor,
                     incr1, incr2, s_x, s_y);
            break;

        case DataBuffer.TYPE_INT:
            intLoop(srcAccessor,
                    destRect,
                    translateX,
                    translateY,
                    dstAccessor,
                    incr1, incr2, s_x, s_y);
            break;

        case DataBuffer.TYPE_SHORT:
        case DataBuffer.TYPE_USHORT:
            shortLoop(srcAccessor,
                      destRect,
                      translateX,
                      translateY,
                      dstAccessor,
                      incr1, incr2, s_x, s_y);
            break;

        case DataBuffer.TYPE_FLOAT:
            floatLoop(srcAccessor,
                      destRect,
                      translateX,
                      translateY,
                      dstAccessor,
                      incr1, incr2, s_x, s_y);
            break;

        case DataBuffer.TYPE_DOUBLE:
            doubleLoop(srcAccessor,
                       destRect,
                       translateX,
                       translateY,
                       dstAccessor,
                       incr1, incr2, s_x, s_y);
            break;
        }

        //
        // If the RasterAccessor object set up a temporary buffer for the
        // op to write to, tell the RasterAccessor to write that data
        // to the raster, that we're done with it.
        //
        if (dstAccessor.isDataCopy()) {
            dstAccessor.clampDataArrays();
            dstAccessor.copyDataToRaster();
        }
    }

    private void byteLoop(RasterAccessor src,
                          Rectangle destRect,
                          int srcTranslateX,
                          int srcTranslateY,
                          RasterAccessor dst,
                          int incr1, int incr2, int s_x, int s_y) {
        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int dnumBands = dst.getNumBands();

        byte dstDataArrays[][] = dst.getByteDataArrays();
        int dstBandOffsets[] = dst.getBandOffsets();
        int dstPixelStride = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();

        byte srcDataArrays[][] = src.getByteDataArrays();
        int bandOffsets[] = src.getOffsetsForBands();
        int srcPixelStride = src.getPixelStride();
        int srcScanlineStride = src.getScanlineStride();

        int dst_num_bands = dst.getNumBands();

        int dst_min_x = destRect.x;
        int dst_min_y = destRect.y;
        int dst_max_x = destRect.x + destRect.width;
        int dst_max_y = destRect.y + destRect.height;

        // Translate to/from SampleModel space & Raster space
        int posy = (s_y - srcTranslateY) * srcScanlineStride;
        int posx = (s_x - srcTranslateX) * srcPixelStride;
        int srcScanlineOffset = posx + posy;
        int dstScanlineOffset = 0;

        // loop around
        for (int y = dst_min_y; y < dst_max_y; y++)  {
            for (int k2=0; k2 < dst_num_bands; k2++) {
                byte[] srcDataArray = srcDataArrays[k2];
                byte[] dstDataArray = dstDataArrays[k2];

                int dstPixelOffset = dstScanlineOffset + dstBandOffsets[k2];
                int srcPixelOffset = srcScanlineOffset + bandOffsets[k2];

                for (int x = dst_min_x; x < dst_max_x; x++)  {
                    dstDataArray[dstPixelOffset] =
                        srcDataArray[srcPixelOffset];
                    srcPixelOffset += incr1;

                    // Go to next pixel
                    dstPixelOffset += dstPixelStride;
                }
            }

            srcScanlineOffset += incr2;

            // Got the next line in the destination rectangle
            dstScanlineOffset += dstScanlineStride;
        }
    }

    private void intLoop(RasterAccessor src,
                         Rectangle destRect,
                         int srcTranslateX,
                         int srcTranslateY,
                         RasterAccessor dst,
                         int incr1, int incr2, int s_x, int s_y) {
        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int dnumBands = dst.getNumBands();

        int dstDataArrays[][] = dst.getIntDataArrays();
        int dstBandOffsets[] = dst.getBandOffsets();
        int dstPixelStride = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();

        int srcDataArrays[][] = src.getIntDataArrays();
        int bandOffsets[] = src.getOffsetsForBands();
        int srcPixelStride = src.getPixelStride();
        int srcScanlineStride = src.getScanlineStride();

        int dst_num_bands = dst.getNumBands();

        int dst_min_x = destRect.x;
        int dst_min_y = destRect.y;
        int dst_max_x = destRect.x + destRect.width;
        int dst_max_y = destRect.y + destRect.height;

        // Translate to/from SampleModel space & Raster space
        int posy = (s_y - srcTranslateY) * srcScanlineStride;
        int posx = (s_x - srcTranslateX) * srcPixelStride;
        int srcScanlineOffset = posx + posy;
        int dstScanlineOffset = 0;

        // loop around
        for (int y = dst_min_y; y < dst_max_y; y++)  {
            for (int k2=0; k2 < dst_num_bands; k2++) {
                int[] srcDataArray = srcDataArrays[k2];
                int[] dstDataArray = dstDataArrays[k2];

                int dstPixelOffset = dstScanlineOffset + dstBandOffsets[k2];
                int srcPixelOffset = srcScanlineOffset + bandOffsets[k2];

                for (int x = dst_min_x; x < dst_max_x; x++)  {
                    dstDataArray[dstPixelOffset] =
                        srcDataArray[srcPixelOffset];
                    srcPixelOffset += incr1;

                    // Go to next pixel
                    dstPixelOffset += dstPixelStride;
                }
            }

            srcScanlineOffset += incr2;

            // Got the next line in the destination rectangle
            dstScanlineOffset += dstScanlineStride;
        }
    }

    private void shortLoop(RasterAccessor src,
                           Rectangle destRect,
                           int srcTranslateX,
                           int srcTranslateY,
                           RasterAccessor dst,
                           int incr1, int incr2, int s_x, int s_y) {
        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int dnumBands = dst.getNumBands();

        short dstDataArrays[][] = dst.getShortDataArrays();
        int dstBandOffsets[] = dst.getBandOffsets();
        int dstPixelStride = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();

        short srcDataArrays[][] = src.getShortDataArrays();
        int bandOffsets[] = src.getOffsetsForBands();
        int srcPixelStride = src.getPixelStride();
        int srcScanlineStride = src.getScanlineStride();

        int dst_num_bands = dst.getNumBands();

        int dst_min_x = destRect.x;
        int dst_min_y = destRect.y;
        int dst_max_x = destRect.x + destRect.width;
        int dst_max_y = destRect.y + destRect.height;

        // Translate to/from SampleModel space & Raster space
        int posy = (s_y - srcTranslateY) * srcScanlineStride;
        int posx = (s_x - srcTranslateX) * srcPixelStride;
        int srcScanlineOffset = posx + posy;
        int dstScanlineOffset = 0;

        // loop around
        for (int y = dst_min_y; y < dst_max_y; y++)  {
            for (int k2=0; k2 < dst_num_bands; k2++) {
                short[] srcDataArray = srcDataArrays[k2];
                short[] dstDataArray = dstDataArrays[k2];

                int dstPixelOffset = dstScanlineOffset + dstBandOffsets[k2];
                int srcPixelOffset = srcScanlineOffset + bandOffsets[k2];

                for (int x = dst_min_x; x < dst_max_x; x++)  {
                    dstDataArray[dstPixelOffset] =
                        srcDataArray[srcPixelOffset];
                    srcPixelOffset += incr1;

                    // Go to next pixel
                    dstPixelOffset += dstPixelStride;
                }
            }

            srcScanlineOffset += incr2;

            // Got the next line in the destination rectangle
            dstScanlineOffset += dstScanlineStride;
        }
    }

    private void floatLoop(RasterAccessor src,
                           Rectangle destRect,
                           int srcTranslateX,
                           int srcTranslateY,
                           RasterAccessor dst,
                           int incr1, int incr2, int s_x, int s_y) {
        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int dnumBands = dst.getNumBands();

        float dstDataArrays[][] = dst.getFloatDataArrays();
        int dstBandOffsets[] = dst.getBandOffsets();
        int dstPixelStride = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();

        float srcDataArrays[][] = src.getFloatDataArrays();
        int bandOffsets[] = src.getOffsetsForBands();
        int srcPixelStride = src.getPixelStride();
        int srcScanlineStride = src.getScanlineStride();

        int dst_num_bands = dst.getNumBands();

        int dst_min_x = destRect.x;
        int dst_min_y = destRect.y;
        int dst_max_x = destRect.x + destRect.width;
        int dst_max_y = destRect.y + destRect.height;

        // Translate to/from SampleModel space & Raster space
        int posy = (s_y - srcTranslateY) * srcScanlineStride;
        int posx = (s_x - srcTranslateX) * srcPixelStride;
        int srcScanlineOffset = posx + posy;
        int dstScanlineOffset = 0;

        // loop around
        for (int y = dst_min_y; y < dst_max_y; y++)  {
            for (int k2=0; k2 < dst_num_bands; k2++) {
                float[] srcDataArray = srcDataArrays[k2];
                float[] dstDataArray = dstDataArrays[k2];

                int dstPixelOffset = dstScanlineOffset + dstBandOffsets[k2];
                int srcPixelOffset = srcScanlineOffset + bandOffsets[k2];

                for (int x = dst_min_x; x < dst_max_x; x++)  {
                    dstDataArray[dstPixelOffset] =
                        srcDataArray[srcPixelOffset];
                    srcPixelOffset += incr1;

                    // Go to next pixel
                    dstPixelOffset += dstPixelStride;
                }
            }

            srcScanlineOffset += incr2;

            // Got the next line in the destination rectangle
            dstScanlineOffset += dstScanlineStride;
        }
    }

    private void doubleLoop(RasterAccessor src,
                            Rectangle destRect,
                            int srcTranslateX,
                            int srcTranslateY,
                            RasterAccessor dst,
                            int incr1, int incr2, int s_x, int s_y) {
        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int dnumBands = dst.getNumBands();

        double dstDataArrays[][] = dst.getDoubleDataArrays();
        int dstBandOffsets[] = dst.getBandOffsets();
        int dstPixelStride = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();

        double srcDataArrays[][] = src.getDoubleDataArrays();
        int bandOffsets[] = src.getOffsetsForBands();
        int srcPixelStride = src.getPixelStride();
        int srcScanlineStride = src.getScanlineStride();

        int dst_num_bands = dst.getNumBands();

        int dst_min_x = destRect.x;
        int dst_min_y = destRect.y;
        int dst_max_x = destRect.x + destRect.width;
        int dst_max_y = destRect.y + destRect.height;

        // Translate to/from SampleModel space & Raster space
        int posy = (s_y - srcTranslateY) * srcScanlineStride;
        int posx = (s_x - srcTranslateX) * srcPixelStride;
        int srcScanlineOffset = posx + posy;
        int dstScanlineOffset = 0;

        // loop around
        for (int y = dst_min_y; y < dst_max_y; y++)  {
            for (int k2=0; k2 < dst_num_bands; k2++) {
                double[] srcDataArray = srcDataArrays[k2];
                double[] dstDataArray = dstDataArrays[k2];

                int dstPixelOffset = dstScanlineOffset + dstBandOffsets[k2];
                int srcPixelOffset = srcScanlineOffset + bandOffsets[k2];

                for (int x = dst_min_x; x < dst_max_x; x++)  {
                    dstDataArray[dstPixelOffset] =
                        srcDataArray[srcPixelOffset];
                    srcPixelOffset += incr1;

                    // Go to next pixel
                    dstPixelOffset += dstPixelStride;
                }
            }

            srcScanlineOffset += incr2;

            // Got the next line in the destination rectangle
            dstScanlineOffset += dstScanlineStride;
        }
    }
}
