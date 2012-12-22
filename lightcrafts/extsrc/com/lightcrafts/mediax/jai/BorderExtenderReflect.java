/*
 * $RCSfile: BorderExtenderReflect.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:04 $
 * $State: Exp $
 */
package com.lightcrafts.mediax.jai;
import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import com.lightcrafts.media.jai.util.JDKWorkarounds;

/**
 * A subclass of <code>BorderExtender</code> that implements 
 * border extension by filling all pixels outside of the image
 * bounds with copies of the whole image.  For example, the image:
 *
 * <p><center>
 * <table width="10%" border=1>
 * <tr align=center><td><tt>|><br>|\</td> </tr>
 * </table></center>
 *
 * <br>if extended by adding two extra rows to the top and bottom and
 * one extra column on the left and right sides, would become:
 *
 * <p><center>
 * <table width="30%" border=1>
 * <tr align=center><td><tt><|<br>/|</tt></td> <td><tt>|><br>|\</tt></td> <td><tt><|<br>/|</tt></td> </tr>
 * <tr align=center><td><tt>\|<br><|</tt></td> <td><tt>|/<br>|></tt></td> <td><tt>\|<br><|</tt></td> </tr>
 * <tr align=center><td><tt><|<br>/|</tt></td> <td><tt>|><br>|\</tt></td> <td><tt><|<br>/|</tt></td> </tr>
 * <tr align=center><td><tt>\|<br><|</tt></td> <td><tt>|/<br>|></tt></td> <td><tt>\|<br><|</tt></td> </tr>
 * <tr align=center><td><tt><|<br>/|</tt></td> <td><tt>|><br>|\</tt></td> <td><tt><|<br>/|</tt></td> </tr>
 * </table></center>
 *
 * <p> This form of extension avoids discontinuities around the edges
 * of the image.
 */
public final class BorderExtenderReflect extends BorderExtender {

    BorderExtenderReflect() {}

    private void flipX(WritableRaster raster) {
        int minX = raster.getMinX();
        int minY = raster.getMinY();
        int height = raster.getHeight();
        int width = raster.getWidth();
        int maxX = minX + width - 1; // Inclusive
        int numBands = raster.getNumBands();

        switch (raster.getSampleModel().getDataType()) {
        case DataBuffer.TYPE_BYTE:
        case DataBuffer.TYPE_SHORT:
        case DataBuffer.TYPE_USHORT:
        case DataBuffer.TYPE_INT:
            int[] iData0 = new int[height*numBands];
            int[] iData1 = new int[height*numBands];

            for (int i = 0; i < width/2; i++) {
                raster.getPixels(minX + i, minY, 1, height, iData0);
                raster.getPixels(maxX - i, minY, 1, height, iData1);

                raster.setPixels(minX + i, minY, 1, height, iData1);
                raster.setPixels(maxX - i, minY, 1, height, iData0);
            }
            break;

        case DataBuffer.TYPE_FLOAT:
            float[] fData0 = new float[height*numBands];
            float[] fData1 = new float[height*numBands];

            for (int i = 0; i < width/2; i++) {
                raster.getPixels(minX + i, minY, 1, height, fData0);
                raster.getPixels(maxX - i, minY, 1, height, fData1);

                raster.setPixels(minX + i, minY, 1, height, fData1);
                raster.setPixels(maxX - i, minY, 1, height, fData0);
            }
            break;

        case DataBuffer.TYPE_DOUBLE:
            double[] dData0 = new double[height*numBands];
            double[] dData1 = new double[height*numBands];

            for (int i = 0; i < width/2; i++) {
                raster.getPixels(minX + i, minY, 1, height, dData0);
                raster.getPixels(maxX - i, minY, 1, height, dData1);

                raster.setPixels(minX + i, minY, 1, height, dData1);
                raster.setPixels(maxX - i, minY, 1, height, dData0);
            }
            break;
        }
    }

    private void flipY(WritableRaster raster) {
        int minX = raster.getMinX();
        int minY = raster.getMinY();
        int height = raster.getHeight();
        int width = raster.getWidth();
        int maxY = minY + height - 1; // Inclusive
        int numBands = raster.getNumBands();

        switch (raster.getSampleModel().getDataType()) {
        case DataBuffer.TYPE_BYTE:
        case DataBuffer.TYPE_SHORT:
        case DataBuffer.TYPE_USHORT:
        case DataBuffer.TYPE_INT:
            int[] iData0 = new int[width*numBands];
            int[] iData1 = new int[width*numBands];

            for (int i = 0; i < height/2; i++) {
                raster.getPixels(minX, minY + i, width, 1, iData0);
                raster.getPixels(minX, maxY - i, width, 1, iData1);
                
                raster.setPixels(minX, minY + i, width, 1, iData1);
                raster.setPixels(minX, maxY - i, width, 1, iData0);
            }
            break;

        case DataBuffer.TYPE_FLOAT:
            float[] fData0 = new float[width*numBands];
            float[] fData1 = new float[width*numBands];

            for (int i = 0; i < height/2; i++) {
                raster.getPixels(minX, minY + i, width, 1, fData0);
                raster.getPixels(minX, maxY - i, width, 1, fData1);
                
                raster.setPixels(minX, minY + i, width, 1, fData1);
                raster.setPixels(minX, maxY - i, width, 1, fData0);
            }
            break;

        case DataBuffer.TYPE_DOUBLE:
            double[] dData0 = new double[width*numBands];
            double[] dData1 = new double[width*numBands];

            for (int i = 0; i < height/2; i++) {
                raster.getPixels(minX, minY + i, width, 1, dData0);
                raster.getPixels(minX, maxY - i, width, 1, dData1);
                
                raster.setPixels(minX, minY + i, width, 1, dData1);
                raster.setPixels(minX, maxY - i, width, 1, dData0);
            }
            break;
        }
    }

    /**
     * Fills in the portions of a given <code>Raster</code> that lie
     * outside the bounds of a given <code>PlanarImage</code> with
     * suitably reflected copies of the entire image.
     *
     * <p> The portion of <code>raster</code> that lies within 
     * <code>im.getBounds()</code> is not altered.
     *
     * @param raster The <code>WritableRaster</code> the border area of
     *               which is to be filled with suitably reflected copies
     *               of portions of the specified image.
     * @param im     The <code>PlanarImage</code> the data of which is
     *               to be reflected and used to fill the
     *               <code>WritableRaster</code> border.
     *
     * @throws <code>IllegalArgumentException</code> if either parameter is
     *         <code>null</code>.
     */
    public final void extend(WritableRaster raster,
                             PlanarImage im) {

        if ( raster == null || im == null ) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        int width = raster.getWidth();
        int height = raster.getHeight();

        int minX = raster.getMinX();
        int maxX = minX + width;
        int minY = raster.getMinY();
        int maxY = minY + height;

        int imMinX = im.getMinX();
        int imMinY = im.getMinY();
        int imWidth = im.getWidth();
        int imHeight = im.getHeight();

        int validMinX = Math.max(imMinX, minX);
        int validMaxX = Math.min(imMinX + imWidth, maxX);
        int validMinY = Math.max(imMinY, minY);
        int validMaxY = Math.min(imMinY + imHeight, maxY);

        if(validMinX > validMaxX || validMinY > validMaxY) {
            // Raster does not intersect image. Determine the location
            // and size of the smallest rectangle containing the Raster
            // and which intersects the image.
            if(validMinX > validMaxX) { // no intersetion in X
                if(minX == validMinX) {
                    minX = im.getMaxX() - 1;
                } else {
                    maxX = im.getMinX();
                }
            }
            if(validMinY > validMaxY) { // no intersetion in Y
                if(minY == validMinY) {
                    minY = im.getMaxY() - 1;
                } else {
                    maxY = im.getMinY();
                }
            }

            // Create minimum Raster.
            WritableRaster wr =
                raster.createCompatibleWritableRaster(minX, minY,
                                                      maxX - minX,
                                                      maxY - minY);

            // Extend the data.
            extend(wr, im);

            // Create a child with same bounds as the target Raster.
            Raster child =
                wr.createChild(raster.getMinX(), raster.getMinY(),
                               raster.getWidth(), raster.getHeight(),
                               raster.getMinX(), raster.getMinY(), null);

            // Copy the data from the child.
            JDKWorkarounds.setRect(raster, child, 0, 0);

            return;
        }

        Rectangle rect = new Rectangle();
        
        // Notionally extend the source image by treating it as a single
        // tile of an infinite tiled image.  Adjacent tiles are reflections
        // of one another.

        // Compute the min and max X and Y tile indices of the area
        // intersected by the output raster.
        int minTileX = PlanarImage.XToTileX(minX, imMinX, imWidth);
        int maxTileX = PlanarImage.XToTileX(maxX - 1, imMinX, imWidth);
        int minTileY = PlanarImage.YToTileY(minY, imMinY, imHeight);
        int maxTileY = PlanarImage.YToTileY(maxY - 1, imMinY, imHeight);

        // Loop over the tiles
        for (int tileY = minTileY; tileY <= maxTileY; tileY++) {
            int ty = tileY*imHeight + imMinY;
            for (int tileX = minTileX; tileX <= maxTileX; tileX++) {
                int tx = tileX*imWidth + imMinX;

                // Don't touch the central "tile" (actual image)
                if (tileX == 0 && tileY == 0) {
                    continue;
                }

                boolean flipX = (Math.abs(tileX) % 2) == 1;
                boolean flipY = (Math.abs(tileY) % 2) == 1;

                // Clip the tile bounds against the bounds of the Raster.
                // Keep track of the (x, y) offset of the start of the tile.
                rect.x = tx;
                rect.y = ty;
                rect.width = imWidth;
                rect.height = imHeight;

                int xOffset = 0;
                if (rect.x < minX) {
                    xOffset = minX - rect.x;
                    rect.x = minX;
                    rect.width -= xOffset;
                }
                int yOffset = 0;
                if (rect.y < minY) {
                    yOffset = minY - rect.y;
                    rect.y = minY;
                    rect.height -= yOffset;
                }
                if (rect.x + rect.width > maxX) {
                    rect.width = maxX - rect.x;
                }
                if (rect.y + rect.height > maxY) {
                    rect.height = maxY - rect.y;
                }

                int imX;
                if (flipX) {
                    if (xOffset == 0) {
                        imX = imMinX + imWidth - rect.width;
                    } else {
                        imX = imMinX;
                    }
                } else {
                    imX = imMinX + xOffset;
                }

                int imY;
                if (flipY) {
                    if (yOffset == 0) {
                        imY = imMinY + imHeight - rect.height;
                    } else {
                        imY = imMinY;
                    }
                } else {
                    imY = imMinY + yOffset;
                }

                // Create a child raster with coordinates within the
                // actual image.
                WritableRaster child = 
                    RasterFactory.createWritableChild(raster,
                                                      rect.x, rect.y,
                                                      rect.width,
                                                      rect.height,
                                                      imX, imY,
                                                      null);

                // Copy the data into the Raster
                im.copyData(child);

                if (flipX) {
                    flipX(child);
                }

                if (flipY) {
                    flipY(child);
                }
            }
        }
    }
}
