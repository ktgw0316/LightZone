/*
 * $RCSfile: BorderExtenderCopy.java,v $
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
 * bounds with copies of the edge pixels.  For example, the image:
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
 * <td>A</td><td>A</td><td>A</td><td>B</td><td>C</td><td>C</td><td>C</td> </tr>
 * <td>A</td><td>A</td><td>A</td><td>B</td><td>C</td><td>C</td><td>C</td> </tr>
 * <td>A</td><td>A</td><td>A</td><td>B</td><td>C</td><td>C</td><td>C</td> </tr>
 * <td>D</td><td>D</td><td>D</td><td>E</td><td>F</td><td>F</td><td>F</td> </tr>
 * <td>G</td><td>G</td><td>G</td><td>H</td><td>I</td><td>I</td><td>I</td> </tr>
 * <td>G</td><td>G</td><td>G</td><td>H</td><td>I</td><td>I</td><td>I</td> </tr>
 * <td>G</td><td>G</td><td>G</td><td>H</td><td>I</td><td>I</td><td>I</td> </tr>
 * </table></center>
 *
 * <p> Although this type of extension is not particularly
 * visually appealing, it is very useful as a way of padding
 * source images prior to area or geometric operations, such as
 * convolution, scaling, or rotation.
 *
 * @see BorderExtender
 */
public final class BorderExtenderCopy extends BorderExtender {

    BorderExtenderCopy() {}

    /**
     * Fills in the portions of a given <code>Raster</code> that lie
     * outside the bounds of a given <code>PlanarImage</code> with
     * copies of the edge pixels of the image.
     *
     * <p> The portion of <code>raster</code> that lies within 
     * <code>im.getBounds()</code> is not altered.
     *
     * @param raster The <code>WritableRaster</code> the border area of
     *               which is to be filled with copies of the edge pixels
     *               of the image.
     * @param im     The <code>PlanarImage</code> which will provide the
     *               edge data with which to fill the border area of the
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
        int numBands = raster.getNumBands();

        int minX = raster.getMinX();
        int maxX = minX + width;
        int minY = raster.getMinY();
        int maxY = minY + height;

        int validMinX = Math.max(im.getMinX(), minX);
        int validMaxX = Math.min(im.getMaxX(), maxX);
        int validMinY = Math.max(im.getMinY(), minY);
        int validMaxY = Math.min(im.getMaxY(), maxY);

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
        int size = Math.max(width, height);

        int row, col;
        switch (raster.getSampleModel().getDataType()) {
            case DataBuffer.TYPE_BYTE:
            case DataBuffer.TYPE_SHORT:
            case DataBuffer.TYPE_USHORT:
            case DataBuffer.TYPE_INT:
                int[] iData = new int[size*numBands];

                if (minX < validMinX) {
                    rect.x = validMinX;
                    rect.y = validMinY;
                    rect.width = 1;
                    rect.height = validMaxY - validMinY;

                    if (rect.height > 0) {
                        Raster leftEdge = im.getData(rect);
                        leftEdge.getPixels(validMinX, validMinY,
                                           1, rect.height, iData);
                        
                        for (col = minX; col < validMinX; col++) {
                            raster.setPixels(col, validMinY, 1, rect.height,
                                             iData);
                        }
                    }
                }

                if (validMaxX < maxX) {
                    rect.x = validMaxX - 1;
                    rect.y = validMinY;
                    rect.width = 1;
                    rect.height = validMaxY - validMinY;
                    
                    if (rect.height > 0) {
                        Raster rightEdge = im.getData(rect);
                        rightEdge.getPixels(validMaxX - 1, validMinY,
                                            1, rect.height, iData);
                        
                        for (col = validMaxX; col < maxX; col++) {
                            raster.setPixels(col, validMinY, 1, rect.height,
                                             iData);
                        }
                    }
                }

                if (minY < validMinY) {
                    rect.x = minX;
                    rect.y = validMinY;
                    rect.width = width;
                    rect.height = 1;

                    Raster topRow = im.getExtendedData(rect, this);
                    topRow.getPixels(minX, validMinY, width, 1, iData);
                    for (row = minY; row < validMinY; row++) {
                        raster.setPixels(minX, row, width, 1, iData);
                    }
                }

                if (validMaxY < maxY) {
                    rect.x = minX;
                    rect.y = validMaxY - 1;
                    rect.width = width;
                    rect.height = 1;

                    Raster bottomRow = im.getExtendedData(rect, this);
                    bottomRow.getPixels(minX, validMaxY - 1,
                                        width, 1, iData);
                    for (row = validMaxY; row < maxY; row++) {
                        raster.setPixels(minX, row, width, 1, iData);
                    }
                }
                break;

            case DataBuffer.TYPE_FLOAT:
                float[] fData = new float[size*numBands];

                if (minX < validMinX) {
                    rect.x = validMinX;
                    rect.y = validMinY;
                    rect.width = 1;
                    rect.height = validMaxY - validMinY;

                    if (rect.height > 0) {
                        Raster leftEdge = im.getData(rect);
                        leftEdge.getPixels(validMinX, validMinY,
                                           1, rect.height, fData);
                        
                        for (col = minX; col < validMinX; col++) {
                            raster.setPixels(col, validMinY, 1, rect.height,
                                             fData);
                        }
                    }
                }

                if (validMaxX < maxX) {
                    rect.x = validMaxX - 1;
                    rect.y = validMinY;
                    rect.width = 1;
                    rect.height = validMaxY - validMinY;
                    
                    if (rect.height > 0) {
                        Raster rightEdge = im.getData(rect);
                        rightEdge.getPixels(validMaxX - 1, validMinY,
                                            1, rect.height, fData);
                        
                        for (col = validMaxX; col < maxX; col++) {
                            raster.setPixels(col, validMinY, 1, rect.height,
                                             fData);
                        }
                    }
                }

                if (minY < validMinY) {
                    rect.x = minX;
                    rect.y = validMinY;
                    rect.width = width;
                    rect.height = 1;
                    
                    Raster topRow = im.getExtendedData(rect, this);
                    topRow.getPixels(minX, validMinY, width, 1, fData);
                    for (row = minY; row < validMinY; row++) {
                        raster.setPixels(minX, row, width, 1, fData);
                    }
                }

                if (validMaxY < maxY) {
                    rect.x = minX;
                    rect.y = validMaxY - 1;
                    rect.width = width;
                    rect.height = 1;

                    Raster bottomRow = im.getExtendedData(rect, this);
                    bottomRow.getPixels(minX, validMaxY - 1,
                                        width, 1, fData);
                    for (row = validMaxY; row < maxY; row++) {
                        raster.setPixels(minX, row, width, 1, fData);
                    }
                }
                break;

            case DataBuffer.TYPE_DOUBLE:
                double[] dData = new double[size*numBands];

                if (minX < validMinX) {
                    rect.x = validMinX;
                    rect.y = validMinY;
                    rect.width = 1;
                    rect.height = validMaxY - validMinY;

                    if (rect.height > 0) {
                        Raster leftEdge = im.getData(rect);
                        leftEdge.getPixels(validMinX, validMinY,
                                           1, rect.height, dData);
                        
                        for (col = minX; col < validMinX; col++) {
                            raster.setPixels(col, validMinY, 1, rect.height,
                                             dData);
                        }
                    }
                }

                if (validMaxX < maxX) {
                    rect.x = validMaxX - 1;
                    rect.y = validMinY;
                    rect.width = 1;
                    rect.height = validMaxY - validMinY;
                    
                    if (rect.height > 0) {
                        Raster rightEdge = im.getData(rect);
                        rightEdge.getPixels(validMaxX - 1, validMinY,
                                            1, rect.height, dData);
                        
                        for (col = validMaxX; col < maxX; col++) {
                            raster.setPixels(col, validMinY, 1, rect.height,
                                             dData);
                        }
                    }
                }

                if (minY < validMinY) {
                    rect.x = minX;
                    rect.y = validMinY;
                    rect.width = width;
                    rect.height = 1;
                    
                    Raster topRow = im.getExtendedData(rect, this);
                    topRow.getPixels(minX, validMinY, width, 1, dData);
                    for (row = minY; row < validMinY; row++) {
                        raster.setPixels(minX, row, width, 1, dData);
                    }
                }

                if (validMaxY < maxY) {
                    rect.x = minX;
                    rect.y = validMaxY - 1;
                    rect.width = width;
                    rect.height = 1;

                    Raster bottomRow = im.getExtendedData(rect, this);
                    bottomRow.getPixels(minX, validMaxY - 1,
                                        width, 1, dData);
                    for (row = validMaxY; row < maxY; row++) {
                        raster.setPixels(minX, row, width, 1, dData);
                    }
                }
                break;
        }
    }
}
