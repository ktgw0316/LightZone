/*
 * $RCSfile: MlibExtremaOpImage.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:55:56 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.mlib;
import java.awt.Rectangle;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.DataBuffer;
import java.util.ArrayList;
import com.lightcrafts.mediax.jai.ROI;

import com.lightcrafts.media.jai.opimage.ExtremaOpImage;
import com.sun.medialib.mlib.*;

/**
 * An OpImage that performs the Extrema operation on an image through mediaLib.
 *
 */
final class MlibExtremaOpImage extends ExtremaOpImage {
    /** Buffer for the minimum location count. */
    private int[] minCount;

    /** Buffer for the maximum location count. */
    private int[] maxCount;

    /** Buffer for the minimum location. */
    private int[][] minLocs;

    /** Buffer for the maximum location. */
    private int[][] maxLocs;

    /**
     * Constructs an MlibExtremaOpImage. The image dimensions are copied
     * from the source image.  The tile grid layout, SampleModel, and
     * ColorModel may optionally be specified by an ImageLayout object.
     *
     * @param source    The source image.
     */
    public MlibExtremaOpImage(RenderedImage source,
                              ROI roi,
                              int xStart,
                              int yStart,
                              int xPeriod,
                              int yPeriod,
			      boolean saveLocations,
			      int maxRuns) {
	super(source, roi, xStart, yStart, xPeriod, yPeriod,
	      saveLocations, maxRuns);
    }

    protected void accumulateStatistics(String name,
                                        Raster source,
                                        Object stats) {
        int numBands = sampleModel.getNumBands();

        initializeState(source);

        // Determine the required region of this tile. (Note
        // that getTileRect() instersects tile and image bounds.)
        Rectangle tileRect = source.getBounds();

        // Set the starting offset in the mediaLib image.
        int offsetX =
            (xPeriod - ((tileRect.x - xStart) % xPeriod)) %
            xPeriod;
        int offsetY =
            (yPeriod - ((tileRect.y - yStart) % yPeriod)) %
            yPeriod;

        // If this offset specifies a position outside of the
        // tile then go to the next tile. This test is likely
        // redundant to a similar test in StatisticsOpImage.
        if(offsetX >= tileRect.width || offsetY >= tileRect.height) {
            return;
        }

        // Determine the format tag and create an accessor.
        int formatTag = MediaLibAccessor.findCompatibleTag(null, source);
        MediaLibAccessor srcAccessor =
            new MediaLibAccessor(source, tileRect, formatTag);

        // Get the mediaLib image.
        mediaLibImage[] srcML = srcAccessor.getMediaLibImages();

        // NOTE:  currently srcML.length always equals 1

        if (!saveLocations) {
            switch (srcAccessor.getDataType()) {
            case DataBuffer.TYPE_BYTE:
            case DataBuffer.TYPE_USHORT:
            case DataBuffer.TYPE_SHORT:
            case DataBuffer.TYPE_INT:
                int[] imin = new int[numBands];
                int[] imax = new int[numBands];

                for (int i = 0 ; i < srcML.length; i++) {
                    Image.Extrema2(imin, imax, srcML[i],
                                   offsetX, offsetY,
                                   xPeriod, yPeriod);
                }

                imin = srcAccessor.getIntParameters(0, imin);
                imax = srcAccessor.getIntParameters(0, imax);

                // Update the extrema.
                for ( int i = 0; i < numBands; i++ ) {
                    extrema[0][i] = Math.min((double)imin[i],
                                             extrema[0][i]);
                    extrema[1][i] = Math.max((double)imax[i],
                                             extrema[1][i]);
                }
                break;

            case DataBuffer.TYPE_FLOAT:
            case DataBuffer.TYPE_DOUBLE:
                double[] dmin = new double[numBands];
                double[] dmax = new double[numBands];
                for (int i = 0 ; i < srcML.length; i++) {
                    Image.Extrema2_Fp(dmin, dmax, srcML[i],
                                      offsetX, offsetY,
                                      xPeriod, yPeriod);
                }

                dmin = srcAccessor.getDoubleParameters(0, dmin);
                dmax = srcAccessor.getDoubleParameters(0, dmax);

                // Update the extrema.
                for ( int i = 0; i < numBands; i++ ) {
                    extrema[0][i] = Math.min((double)dmin[i],
                                             extrema[0][i]);
                    extrema[1][i] = Math.max((double)dmax[i],
                                             extrema[1][i]);
                }
                break;
            }
        } else {
            Rectangle loc = source.getBounds();
            int xOffset = loc.x;
            int yOffset = loc.y;

            switch (srcAccessor.getDataType()) {
            case DataBuffer.TYPE_BYTE:
            case DataBuffer.TYPE_USHORT:
            case DataBuffer.TYPE_SHORT:
            case DataBuffer.TYPE_INT:
                int[] imin = new int[numBands];
                int[] imax = new int[numBands];

                for ( int i = 0; i < numBands; i++ ) {
                    imin[i] = (int)extrema[0][i];
                    imax[i] = (int)extrema[1][i];
                }

                for (int i = 0 ; i < srcML.length; i++) {
                    Image.ExtremaLocations(imin, imax, srcML[i],
                                           offsetX, offsetY,
                                           xPeriod, yPeriod,
                                           saveLocations, maxRuns,
                                           minCount, maxCount,
                                           minLocs, maxLocs);
                }

                imin = srcAccessor.getIntParameters(0, imin);
                imax = srcAccessor.getIntParameters(0, imax);
                minCount = srcAccessor.getIntParameters(0, minCount);
                maxCount = srcAccessor.getIntParameters(0, maxCount);
                minLocs = srcAccessor.getIntArrayParameters(0, minLocs);
                maxLocs = srcAccessor.getIntArrayParameters(0, maxLocs);

                // Update the extrema.
                for ( int i = 0; i < numBands; i++ ) {
                    ArrayList minList = minLocations[i];
                    ArrayList maxList = maxLocations[i];
                    if (imin[i] < extrema[0][i]) {
                        minList.clear();
                        extrema[0][i] = imin[i];
                    }

                    int[] minBuf = minLocs[i];
                    int[] maxBuf = maxLocs[i];

                    for (int j = 0, k = 0; j < minCount[i]; j++)
                        minList.add(new int[]{minBuf[k++] + xOffset, minBuf[k++] + yOffset, minBuf[k++]});

                    if (imax[i] > extrema[1][i]) {
                        maxList.clear();
                        extrema[1][i] = imax[i];
                    }

                    for (int j = 0, k = 0; j < maxCount[i]; j++)
                        maxList.add(new int[]{maxBuf[k++] + xOffset, maxBuf[k++] + yOffset, maxBuf[k++]});
                }
                break;

            case DataBuffer.TYPE_FLOAT:
            case DataBuffer.TYPE_DOUBLE:
                double[] dmin = new double[numBands];
                double[] dmax = new double[numBands];

                for (int i = 0; i < numBands; i++ ) {
                    dmin[i] = extrema[0][i];
                    dmax[i] = extrema[1][i];
                }

                for (int i = 0 ; i < srcML.length; i++) {
                    Image.ExtremaLocations_Fp(dmin, dmax, srcML[i],
                                              offsetX, offsetY,
                                              xPeriod, yPeriod,
                                              saveLocations, maxRuns,
                                              minCount, maxCount,
                                              minLocs, maxLocs);
                }

                dmin = srcAccessor.getDoubleParameters(0, dmin);
                dmax = srcAccessor.getDoubleParameters(0, dmax);
                minCount = srcAccessor.getIntParameters(0, minCount);
                maxCount = srcAccessor.getIntParameters(0, maxCount);
                minLocs = srcAccessor.getIntArrayParameters(0, minLocs);
                maxLocs = srcAccessor.getIntArrayParameters(0, maxLocs);

                // Update the extrema.
                for ( int i = 0; i < numBands; i++ ) {
                    ArrayList minList = minLocations[i];
                    ArrayList maxList = maxLocations[i];
                    if (dmin[i] < extrema[0][i]) {
                        minList.clear();
                        extrema[0][i] = dmin[i];
                    }

                    int[] minBuf = minLocs[i];
                    int[] maxBuf = maxLocs[i];

                    for (int j = 0, k = 0; j < minCount[i]; j++)
                        minList.add(new int[]{minBuf[k++] + xOffset, minBuf[k++] + yOffset, minBuf[k++]});

                    if (dmax[i] > extrema[1][i]) {
                        maxList.clear();
                        extrema[1][i] = dmax[i];
                    }

                    for (int j = 0, k = 0; j < maxCount[i]; j++)
                        maxList.add(new int[]{maxBuf[k++] + xOffset, maxBuf[k++] + yOffset, maxBuf[k++]});
                }
                break;
            }
        }

        if (name.equalsIgnoreCase("extrema")) {
            double[][] ext = (double[][])stats;
            for (int i = 0; i < numBands; i++) {
                ext[0][i] = extrema[0][i];
                ext[1][i] = extrema[1][i];
            }
        } else if (name.equalsIgnoreCase("minimum")) {
            double[] min = (double[])stats;
            for (int i = 0; i < numBands; i++ ) {
               min[i] = extrema[0][i];
            }
        } else if (name.equalsIgnoreCase("maximum")) {
            double[] max = (double[])stats;
            for (int i = 0; i < numBands; i++ ) {
               max[i] = extrema[1][i];
            }
        } else if (name.equalsIgnoreCase("minLocations")) {
	    ArrayList[] minLoc = (ArrayList[])stats;
	    for (int i = 0; i < numBands; i++) {
		minLoc[i] = minLocations[i];
            }
	} else if (name.equalsIgnoreCase("maxLocations")) {
	    ArrayList[] maxLoc = (ArrayList[])stats;
	    for (int i = 0; i < numBands; i++)
		maxLoc[i] = maxLocations[i];
	}
    }

    protected void initializeState(Raster source) {
        if (extrema == null) {
            int numBands = sampleModel.getNumBands();
            minCount = new int[numBands];
            maxCount = new int[numBands];

            minLocs = new int[numBands][];
            maxLocs = new int[numBands][];

            int size = (getTileWidth() + 1) / 2 * getTileHeight();

            for (int i = 0; i < numBands; i++) {
                minLocs[i] = new int[size];
                maxLocs[i] = new int[size];
            }

            super.initializeState(source);
        }
    }
}
