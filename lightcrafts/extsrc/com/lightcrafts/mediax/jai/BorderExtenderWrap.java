/*
 * $RCSfile: BorderExtenderWrap.java,v $
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
import java.awt.image.WritableRaster;

/**
 * A subclass of <code>BorderExtender</code> that implements 
 * border extension by filling all pixels outside of the image
 * bounds with copies of the whole image.  For example, the image:
 *
 * <p><center>
 * <table border=1>
 * <tr align=center><td>A</td><td>B</td><td>C</td> </tr>
 * <tr align=center><td>D</td><td>E</td><td>F</td> </tr>
 * <tr align=center><td>G</td><td>H</td><td>I</td> </tr>
 * </table></center>
 *
 * <br>if extended by adding two extra rows to the top and bottom and
 * two extra columns on the left and right sides, would become:
 *
 * <p><center>
 * <table border=1>
 * <tr align=center>
 * <td>E</td><td>F</td><td>D</td><td>E</td><td>F</td><td>D</td><td>E</td> </tr>
 * <td>H</td><td>I</td><td>G</td><td>H</td><td>I</td><td>G</td><td>H</td> </tr>
 * <td>B</td><td>C</td><td>A</td><td>B</td><td>C</td><td>A</td><td>B</td> </tr>
 * <td>E</td><td>F</td><td>D</td><td>E</td><td>F</td><td>D</td><td>E</td> </tr>
 * <td>H</td><td>I</td><td>G</td><td>H</td><td>I</td><td>G</td><td>H</td> </tr>
 * <td>B</td><td>C</td><td>A</td><td>B</td><td>C</td><td>A</td><td>B</td> </tr>
 * <td>E</td><td>F</td><td>D</td><td>E</td><td>F</td><td>D</td><td>E</td> </tr>
 * </table></center>
 *
 * <p> This form of extension is appropriate for data that is inherently
 * periodic, such as the Fourier transform of an image, or a wallpaper
 * pattern.
 *
 * @see BorderExtender
 */
public class BorderExtenderWrap extends BorderExtender {

    BorderExtenderWrap() {}

    /**
     * Fills in the portions of a given <code>Raster</code> that lie
     * outside the bounds of a given <code>PlanarImage</code> with
     * copies of the entire image.
     *
     * <p> The portion of <code>raster</code> that lies within 
     * <code>im.getBounds()</code> is not altered.
     *
     * @param raster The <code>WritableRaster</code> the border area of
     *               which is to be filled with copies of the given image.
     * @param im     The <code>PlanarImage</code> which will be copied
     *               to fill the border area of the
     *               <code>WritableRaster</code>.
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

        Rectangle rect = new Rectangle();
        
        // Notionally extend the source image by treating it as a single
        // tile of an infinite tiled image.

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

                // Create a child raster with coordinates within the
                // actual image.
                WritableRaster child =
                    RasterFactory.createWritableChild(raster,
                                                      rect.x, rect.y,
                                                      rect.width, rect.height,
                                                      imMinX + xOffset,
                                                      imMinY + yOffset,
                                                      null);

                // Copy the data into the Raster
                im.copyData(child);
            }
        }
    }
}
