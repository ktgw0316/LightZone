/*
 * $RCSfile: BorderExtenderZero.java,v $
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
import java.awt.image.DataBuffer;
import java.awt.image.WritableRaster;

/**
 * A subclass of <code>BorderExtender</code> that implements 
 * border extension by filling all pixels outside of the image
 * bounds with zeros.  For example, the image:
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
 * <td>0</td><td>0</td><td>0</td><td>0</td><td>0</td><td>0</td><td>0</td> </tr>
 * <td>0</td><td>0</td><td>0</td><td>0</td><td>0</td><td>0</td><td>0</td> </tr>
 * <td>0</td><td>0</td><td>A</td><td>B</td><td>C</td><td>0</td><td>0</td> </tr>
 * <td>0</td><td>0</td><td>D</td><td>E</td><td>F</td><td>0</td><td>0</td> </tr>
 * <td>0</td><td>0</td><td>G</td><td>H</td><td>I</td><td>0</td><td>0</td> </tr>
 * <td>0</td><td>0</td><td>0</td><td>0</td><td>0</td><td>0</td><td>0</td> </tr>
 * <td>0</td><td>0</td><td>0</td><td>0</td><td>0</td><td>0</td><td>0</td> </tr>
 * </table></center>
 *
 * @see BorderExtender
 */
public final class BorderExtenderZero extends BorderExtender {

    BorderExtenderZero() {}

    /**
     * Fills in the portions of a given <code>Raster</code> that lie
     * outside the bounds of a given <code>PlanarImage</code> with
     * zeros.
     *
     * <p> The portion of <code>raster</code> that lies within 
     * <code>im.getBounds()</code> is not altered.
     *
     * @param raster The <code>WritableRaster</code> the border area of
     *               which is to be filled with zero.
     * @param im     The <code>PlanarImage</code> which determines the
     *               portion of the <code>WritableRaster</code> <i>not</i>
     *               to be filled.
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

        int row;
        switch (raster.getSampleModel().getDataType()) {
            case DataBuffer.TYPE_BYTE:
            case DataBuffer.TYPE_SHORT:
            case DataBuffer.TYPE_USHORT:
            case DataBuffer.TYPE_INT:
                int[] iData = new int[width*numBands];
                if(validMinX > validMaxX || validMinY > validMaxY) {
                    // Raster does not intersect image.
                    for (row = minY; row < maxY; row++) {
                        raster.setPixels(minX, row, width, 1, iData);
                    }
                } else {
                    for (row = minY; row < validMinY; row++) {
                        raster.setPixels(minX, row, width, 1, iData);
                    }
                    for (row = validMinY; row < validMaxY; row++) {
                        if (minX < validMinX) {
                            raster.setPixels(minX, row,
                                             validMinX - minX, 1, iData);
                        }
                        if (validMaxX < maxX) {
                            raster.setPixels(validMaxX, row,
                                             maxX - validMaxX, 1, iData);
                        }
                    }
                    for (row = validMaxY; row < maxY; row++) {
                        raster.setPixels(minX, row, width, 1, iData);
                    }
                }
                break;

            case DataBuffer.TYPE_FLOAT:
                float[] fData = new float[width*numBands];
                if(validMinX > validMaxX || validMinY > validMaxY) {
                    // Raster does not intersect image.
                    for (row = minY; row < maxY; row++) {
                        raster.setPixels(minX, row, width, 1, fData);
                    }
                } else {
                    for (row = minY; row < validMinY; row++) {
                        raster.setPixels(minX, row, width, 1, fData);
                    }
                    for (row = validMinY; row < validMaxY; row++) {
                        if (minX < validMinX) {
                            raster.setPixels(minX, row,
                                             validMinX - minX, 1, fData);
                        }
                        if (validMaxX < maxX) {
                            raster.setPixels(validMaxX, row,
                                             maxX - validMaxX, 1, fData);
                        }
                    }
                    for (row = validMaxY; row < maxY; row++) {
                        raster.setPixels(minX, row, width, 1, fData);
                    }
                }
                break;

            case DataBuffer.TYPE_DOUBLE:
                double[] dData = new double[width*numBands];
                if(validMinX > validMaxX || validMinY > validMaxY) {
                    // Raster does not intersect image.
                    for (row = minY; row < maxY; row++) {
                        raster.setPixels(minX, row, width, 1, dData);
                    }
                } else {
                    for (row = minY; row < validMinY; row++) {
                        raster.setPixels(minX, row, width, 1, dData);
                    }
                    for (row = validMinY; row < validMaxY; row++) {
                        if (minX < validMinX) {
                            raster.setPixels(minX, row,
                                             validMinX - minX, 1, dData);
                        }
                        if (validMaxX < maxX) {
                            raster.setPixels(validMaxX, row,
                                             maxX - validMaxX, 1, dData);
                        }
                    }
                    for (row = validMaxY; row < maxY; row++) {
                        raster.setPixels(minX, row, width, 1, dData);
                    }
                }
                break;
        }
    }
}
