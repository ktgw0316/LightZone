/*
 * $RCSfile: ExtremaOpImage.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:25 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.opimage;
import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.ListIterator;
import com.lightcrafts.mediax.jai.PixelAccessor;
import com.lightcrafts.mediax.jai.ROI;
import com.lightcrafts.mediax.jai.StatisticsOpImage;
import com.lightcrafts.mediax.jai.UnpackedImageData;

/**
 * An <code>OpImage</code> implementing the "Extrema" operation as
 * described in <code>com.lightcrafts.mediax.jai.operator.ExtremaDescriptor</code>.
 *
 * @see com.lightcrafts.mediax.jai.operator.ExtremaDescriptor
 * @see ExtremaCRIF
 */
public class ExtremaOpImage extends StatisticsOpImage {

    protected double[][] extrema;

    protected ArrayList[] minLocations;

    protected ArrayList[] maxLocations;

    protected int[] minCounts;

    protected int[] maxCounts;

    protected boolean saveLocations;

    protected int maxRuns;

    protected int numMinLocations = 0;

    protected int numMaxLocations = 0;

    private boolean isInitialized = false;

    private PixelAccessor srcPA;

    private int srcSampleType;

    private final boolean tileIntersectsROI(int tileX, int tileY) {
        if (roi == null) {	// ROI is entire tile
            return true;
        } else {
            return roi.intersects(tileXToX(tileX), tileYToY(tileY),
                                  tileWidth, tileHeight);
        }
    }

    /**
     * Constructs an <code>ExtremaOpImage</code>.
     *
     * @param source  The source image.
     */
    public ExtremaOpImage(RenderedImage source,
                          ROI roi,
                          int xStart,
                          int yStart,
                          int xPeriod,
                          int yPeriod,
                          boolean saveLocations,
                          int maxRuns) {
        super(source, roi, xStart, yStart, xPeriod, yPeriod);

        extrema = null;
        this.saveLocations = saveLocations;
        this.maxRuns = maxRuns;
    }

    /** Returns one of the available statistics as a property. */
    public Object getProperty(String name) {
        int numBands = sampleModel.getNumBands();

        if (extrema == null) {
            // Statistics have not been accumulated: call superclass
            // method to do so.
            return super.getProperty(name);
        } else if (name.equalsIgnoreCase("extrema")) {
            double[][] stats = new double[2][numBands];
            for (int i = 0; i < numBands; i++) {
                stats[0][i] = extrema[0][i];
                stats[1][i] = extrema[1][i];
            }
            return stats;
        } else if (name.equalsIgnoreCase("minimum")) {
            double[] stats = new double[numBands];
            for (int i = 0; i < numBands; i++) {
                stats[i] = extrema[0][i];
            }
            return stats;
        } else if (name.equalsIgnoreCase("maximum")) {
            double[] stats = new double[numBands];
            for (int i = 0; i < numBands; i++) {
                stats[i] = extrema[1][i];
            }
            return stats;
        } else if (saveLocations && name.equalsIgnoreCase("minLocations")) {
            return minLocations;
        } else if (saveLocations && name.equalsIgnoreCase("maxLocations")) {
            return maxLocations;
        }

        return java.awt.Image.UndefinedProperty;
    }

    protected String[] getStatisticsNames() {
        return new String[] {"extrema", "maximum", "minimum",
			     "maxLocations", "minLocations"};
    }

    protected Object createStatistics(String name) {
        int numBands = sampleModel.getNumBands();
        Object stats = null;

        if (name.equalsIgnoreCase("extrema")) {
            stats = new double[2][numBands];
        } else if (name.equalsIgnoreCase("minimum") ||
                   name.equalsIgnoreCase("maximum")) {
            stats = new double[numBands];
        } else if (saveLocations &&
                   (name.equalsIgnoreCase("minLocations") ||
		    name.equalsIgnoreCase("maxLocations"))){
	    stats = new ArrayList[numBands];
	} else {
            stats = java.awt.Image.UndefinedProperty;
        }
        return stats;
    }

    private final int startPosition(int pos, int start, int period) {
        int t = (pos - start) % period;
        return t == 0 ? pos : pos + (period - t);
    }

    protected void accumulateStatistics(String name,
                                        Raster source,
                                        Object stats) {
        if(!isInitialized) {
            srcPA = new PixelAccessor(getSourceImage(0));
            srcSampleType = srcPA.sampleType == PixelAccessor.TYPE_BIT ?
                DataBuffer.TYPE_BYTE : srcPA.sampleType;
            isInitialized = true;
        }

        Rectangle srcBounds = getSourceImage(0).getBounds().intersection(
                                                  source.getBounds());

        LinkedList rectList;
        if (roi == null) {	// ROI is the whole Raster
            rectList = new LinkedList();
            rectList.addLast(srcBounds);
        } else {
            rectList = roi.getAsRectangleList(srcBounds.x,
                                              srcBounds.y,
                                              srcBounds.width,
                                              srcBounds.height);
            if (rectList == null) {
                return; // ROI does not intersect with Raster boundary.
            }
        }
        ListIterator iterator = rectList.listIterator(0);

        while (iterator.hasNext()) {
            Rectangle rect = srcBounds.intersection((Rectangle)iterator.next());
            int tx = rect.x;
            int ty = rect.y;

            // Find the actual ROI based on start and period.
            rect.x = startPosition(tx, xStart, xPeriod);
            rect.y = startPosition(ty, yStart, yPeriod);
            rect.width = tx + rect.width - rect.x;
            rect.height = ty + rect.height - rect.y;

            if (rect.isEmpty()) {
                continue;	// no pixel to count in this rectangle
            }

            initializeState(source);

            UnpackedImageData uid = srcPA.getPixels(source, rect,
                                                    srcSampleType, false);
            switch (uid.type) {
            case DataBuffer.TYPE_BYTE:
                accumulateStatisticsByte(uid);
                break;
            case DataBuffer.TYPE_USHORT:
                accumulateStatisticsUShort(uid);
                break;
            case DataBuffer.TYPE_SHORT:
                accumulateStatisticsShort(uid);
                break;
            case DataBuffer.TYPE_INT:
                accumulateStatisticsInt(uid);
                break;
            case DataBuffer.TYPE_FLOAT:
                accumulateStatisticsFloat(uid);
                break;
            case DataBuffer.TYPE_DOUBLE:
                accumulateStatisticsDouble(uid);
                break;
            }
        }

        if (name.equalsIgnoreCase("extrema")) {
            double[][] ext = (double[][])stats;
            for (int i = 0; i < srcPA.numBands; i++) {
                ext[0][i] = extrema[0][i];
                ext[1][i] = extrema[1][i];
            }
        } else if (name.equalsIgnoreCase("minimum")) {
            double[] min = (double[])stats;
            for (int i = 0; i < srcPA.numBands; i++) {
                min[i] = extrema[0][i];
            }
        } else if (name.equalsIgnoreCase("maximum")) {
            double[] max = (double[])stats;
            for (int i = 0; i < srcPA.numBands; i++) {
                max[i] = extrema[1][i];
            }
        } else if (name.equalsIgnoreCase("minLocations")) {
	    ArrayList[] minLoc = (ArrayList[])stats;
	    for (int i = 0; i < srcPA.numBands; i++) {
		minLoc[i] = minLocations[i];
            }
	} else if (name.equalsIgnoreCase("maxLocations")) {
	    ArrayList[] maxLoc = (ArrayList[])stats;
	    for (int i = 0; i < srcPA.numBands; i++)
		maxLoc[i] = maxLocations[i];
	}
    }

    private void accumulateStatisticsByte(UnpackedImageData uid) {
        Rectangle rect = uid.rect;
        byte[][] data = uid.getByteData();
        int lineStride = uid.lineStride;
        int pixelStride = uid.pixelStride;

        int lineInc = lineStride * yPeriod;
        int pixelInc = pixelStride * xPeriod;

        if (!saveLocations) {
            for (int b = 0; b < srcPA.numBands; b++) {
                int min = (int)extrema[0][b];       // minimum
                int max = (int)extrema[1][b];       // maximum

                byte[] d = data[b];
                int lastLine = uid.bandOffsets[b] + rect.height * lineStride;

                for (int lo = uid.bandOffsets[b]; lo < lastLine; lo += lineInc) {
                    int lastPixel = lo + rect.width * pixelStride;

                    for (int po = lo; po < lastPixel; po += pixelInc) {
                        int p = d[po] & 0xff;

                        if (p < min) {
                            min = p;
                        } else if (p > max) {
                            max = p;
                        }
                    }
                }
                extrema[0][b] = min;
                extrema[1][b] = max;
            }
        } else {
            for (int b = 0; b < srcPA.numBands; b++) {
                int min = (int)extrema[0][b];	// minimum
                int max = (int)extrema[1][b];	// maximum
                ArrayList minList = minLocations[b];
                ArrayList maxList = maxLocations[b];
                int minCount = minCounts[b];
                int maxCount = maxCounts[b];

                byte[] d = data[b];
                int lastLine = uid.bandOffsets[b] + rect.height * lineStride;

                for (int lo = uid.bandOffsets[b], y = rect.y; lo < lastLine;
                    lo += lineInc, y += yPeriod) {

                    int lastPixel = lo + rect.width * pixelStride;
                    int minStart = 0;
                    int maxStart = 0;
                    int minLength = 0;
                    int maxLength = 0;

                    for (int po = lo, x = rect.x; po < lastPixel; po += pixelInc,
                        x += xPeriod) {

                        int p = d[po] & 0xff;

                        if (p < min) {
                            min = p;
                            minStart = x;
                            minLength = 1;
                            minList.clear();
                            minCount = 0;
                        } else if (p > max) {
                            max = p;
                            maxStart = x;
                            maxLength = 1;
                            maxList.clear();
                            maxCount = 0;
                        } else {
                            if (p == min) {
                                if (minLength == 0)
                                    minStart = x;
                                minLength++;
                            } else if (minLength > 0 && minCount < maxRuns) {
                                minList.add(new int[]{minStart, y, minLength});
                                minCount++;
                                minLength = 0;
                            }

                            if (p == max) {
                                if (maxLength == 0)
                                    maxStart = x;
                                maxLength++;
                            } else if (maxLength > 0 && maxCount < maxRuns) {
                                maxList.add(new int[]{maxStart, y, maxLength});
                                maxCount++;
                                maxLength = 0;
                            }
                        }
                    }

                    if (maxLength > 0 && maxCount < maxRuns) {
                        maxList.add(new int[]{maxStart, y, maxLength});
                        maxCount++;
                    }

                    if (minLength > 0 && minCount < maxRuns) {
                        minList.add(new int[]{minStart, y, minLength});
                        minCount++;
                    }
                }

                extrema[0][b] = min;
                extrema[1][b] = max;
                minCounts[b] = minCount;
                maxCounts[b] = maxCount;
            }
        }
    }

    private void accumulateStatisticsUShort(UnpackedImageData uid) {
        Rectangle rect = uid.rect;
        short[][] data = uid.getShortData();
        int lineStride = uid.lineStride;
        int pixelStride = uid.pixelStride;

        int lineInc = lineStride * yPeriod;
        int pixelInc = pixelStride * xPeriod;

        if (!saveLocations) {
            for (int b = 0; b < srcPA.numBands; b++) {
                int min = (int)extrema[0][b];       // minimum
                int max = (int)extrema[1][b];       // maximum

                short[] d = data[b];
                int lastLine = uid.bandOffsets[b] + rect.height * lineStride;

                for (int lo = uid.bandOffsets[b]; lo < lastLine; lo += lineInc) {
                    int lastPixel = lo + rect.width * pixelStride;

                    for (int po = lo; po < lastPixel; po += pixelInc) {
                        int p = d[po] & 0xffff;

                        if (p < min) {
                            min = p;
                        } else if (p > max) {
                            max = p;
                        }
                    }
                }
                extrema[0][b] = min;
                extrema[1][b] = max;
            }
        } else {
            for (int b = 0; b < srcPA.numBands; b++) {
                int min = (int)extrema[0][b];       // minimum
                int max = (int)extrema[1][b];       // maximum
                ArrayList minList = minLocations[b];
                ArrayList maxList = maxLocations[b];
                int minCount = minCounts[b];
                int maxCount = maxCounts[b];

                short[] d = data[b];
                int lastLine = uid.bandOffsets[b] + rect.height * lineStride;

                for (int lo = uid.bandOffsets[b], y = rect.y; lo < lastLine;
                    lo += lineInc, y += yPeriod) {

                    int lastPixel = lo + rect.width * pixelStride;
                    int minStart = 0;
                    int maxStart = 0;
                    int minLength = 0;
                    int maxLength = 0;

                    for (int po = lo, x = rect.x; po < lastPixel; po += pixelInc,
                        x += xPeriod) {

                        int p = d[po] & 0xffff;

                        if (p < min) {
                            min = p;
                            minStart = x;
                            minLength = 1;
                            minList.clear();
                            minCount = 0;
                        } else if (p > max) {
                            max = p;
                            maxStart = x;
                            maxLength = 1;
                            maxList.clear();
                            maxCount = 0;
                        } else {
                            if (p == min) {
                                if (minLength == 0)
                                    minStart = x;
                                minLength++;
                            } else if (minLength > 0 && minCount < maxRuns) {
                                minList.add(new int[]{minStart, y, minLength});
                                minCount++;
                                minLength = 0;
                            }

                            if (p == max) {
                                if (maxLength == 0)
                                    maxStart = x;
                                maxLength++;
                            } else if (maxLength > 0 && maxCount < maxRuns) {
                                maxList.add(new int[]{maxStart, y, maxLength});
                                maxCount++;
                                maxLength = 0;
                            }
                        }
                    }

                    if (maxLength > 0 && maxCount < maxRuns) {
                        maxList.add(new int[]{maxStart, y, maxLength});
                        maxCount++;
                    }

                    if (minLength > 0 && minCount < maxRuns) {
                        minList.add(new int[]{minStart, y, minLength});
                        minCount++;
                    }
                }

                extrema[0][b] = min;
                extrema[1][b] = max;
                minCounts[b] = minCount;
                maxCounts[b] = maxCount;
            }
        }
    }

    private void accumulateStatisticsShort(UnpackedImageData uid) {
        Rectangle rect = uid.rect;
        short[][] data = uid.getShortData();
        int lineStride = uid.lineStride;
        int pixelStride = uid.pixelStride;

        int lineInc = lineStride * yPeriod;
        int pixelInc = pixelStride * xPeriod;

        if (!saveLocations) {
            for (int b = 0; b < srcPA.numBands; b++) {
                int min = (int)extrema[0][b];       // minimum
                int max = (int)extrema[1][b];       // maximum

                short[] d = data[b];
                int lastLine = uid.bandOffsets[b] + rect.height * lineStride;

                for (int lo = uid.bandOffsets[b]; lo < lastLine; lo += lineInc) {
                    int lastPixel = lo + rect.width * pixelStride;

                    for (int po = lo; po < lastPixel; po += pixelInc) {
                        int p = d[po];

                        if (p < min) {
                            min = p;
                        } else if (p > max) {
                            max = p;
                        }
                    }
                }
                extrema[0][b] = min;
                extrema[1][b] = max;
            }
        } else {
            for (int b = 0; b < srcPA.numBands; b++) {
                int min = (int)extrema[0][b];       // minimum
                int max = (int)extrema[1][b];       // maximum
                ArrayList minList = minLocations[b];
                ArrayList maxList = maxLocations[b];
                int minCount = minCounts[b];
                int maxCount = maxCounts[b];

                short[] d = data[b];
                int lastLine = uid.bandOffsets[b] + rect.height * lineStride;

                for (int lo = uid.bandOffsets[b], y = rect.y; lo < lastLine;
                    lo += lineInc, y += yPeriod) {

                    int lastPixel = lo + rect.width * pixelStride;
                    int minStart = 0;
                    int maxStart = 0;
                    int minLength = 0;
                    int maxLength = 0;

                    for (int po = lo, x = rect.x; po < lastPixel; po += pixelInc,
                        x += xPeriod) {

                        int p = d[po];

                        if (p < min) {
                            min = p;
                            minStart = x;
                            minLength = 1;
                            minList.clear();
                            minCount = 0;
                        } else if (p > max) {
                            max = p;
                            maxStart = x;
                            maxLength = 1;
                            maxList.clear();
                            maxCount = 0;
                        } else {
                            if (p == min) {
                                if (minLength == 0)
                                    minStart = x;
                                minLength++;
                            } else if (minLength > 0 && minCount < maxRuns) {
                                minList.add(new int[]{minStart, y, minLength});
                                minCount++;
                                minLength = 0;
                            }

                            if (p == max) {
                                if (maxLength == 0)
                                    maxStart = x;
                                maxLength++;
                            } else if (maxLength > 0 && maxCount < maxRuns) {
                                maxList.add(new int[]{maxStart, y, maxLength});
                                maxCount++;
                                maxLength = 0;
                            }
                        }
                    }

                    if (maxLength > 0 && maxCount < maxRuns) {
                        maxList.add(new int[]{maxStart, y, maxLength});
                        maxCount++;
                    }

                    if (minLength > 0 && minCount < maxRuns) {
                        minList.add(new int[]{minStart, y, minLength});
                        minCount++;
                    }
                }

                extrema[0][b] = min;
                extrema[1][b] = max;
                minCounts[b] = minCount;
                maxCounts[b] = maxCount;
            }
        }
    }

    private void accumulateStatisticsInt(UnpackedImageData uid) {
        Rectangle rect = uid.rect;
        int[][] data = uid.getIntData();
        int lineStride = uid.lineStride;
        int pixelStride = uid.pixelStride;

        int lineInc = lineStride * yPeriod;
        int pixelInc = pixelStride * xPeriod;

        if (!saveLocations) {
            for (int b = 0; b < srcPA.numBands; b++) {
                int min = (int)extrema[0][b];       // minimum
                int max = (int)extrema[1][b];       // maximum

                int[] d = data[b];
                int lastLine = uid.bandOffsets[b] + rect.height * lineStride;

                for (int lo = uid.bandOffsets[b]; lo < lastLine; lo += lineInc) {
                    int lastPixel = lo + rect.width * pixelStride;

                    for (int po = lo; po < lastPixel; po += pixelInc) {
                        int p = d[po];

                        if (p < min) {
                            min = p;
                        } else if (p > max) {
                            max = p;
                        }
                    }
                }
                extrema[0][b] = min;
                extrema[1][b] = max;
            }
        } else {
            for (int b = 0; b < srcPA.numBands; b++) {
                int min = (int)extrema[0][b];       // minimum
                int max = (int)extrema[1][b];       // maximum
                ArrayList minList = minLocations[b];
                ArrayList maxList = maxLocations[b];
                int minCount = minCounts[b];
                int maxCount = maxCounts[b];

                int[] d = data[b];
                int lastLine = uid.bandOffsets[b] + rect.height * lineStride;

                for (int lo = uid.bandOffsets[b], y = rect.y; lo < lastLine;
                    lo += lineInc, y += yPeriod) {

                    int lastPixel = lo + rect.width * pixelStride;
                    int minStart = 0;
                    int maxStart = 0;
                    int minLength = 0;
                    int maxLength = 0;

                    for (int po = lo, x = rect.x; po < lastPixel; po += pixelInc,
                        x += xPeriod) {

                        int p = d[po];

                        if (p < min) {
                            min = p;
                            minStart = x;
                            minLength = 1;
                            minList.clear();
                            minCount = 0;
                        } else if (p > max) {
                            max = p;
                            maxStart = x;
                            maxLength = 1;
                            maxList.clear();
                            maxCount = 0;
                        } else {
                            if (p == min) {
                                if (minLength == 0)
                                    minStart = x;
                                minLength++;
                            } else if (minLength > 0 && minCount < maxRuns) {
                                minList.add(new int[]{minStart, y, minLength});
                                minCount++;
                                minLength = 0;
                            }

                            if (p == max) {
                                if (maxLength == 0)
                                    maxStart = x;
                                maxLength++;
                            } else if (maxLength > 0 && maxCount < maxRuns) {
                                maxList.add(new int[]{maxStart, y, maxLength});
                                maxCount++;
                                maxLength = 0;
                            }
                        }
                    }

                    if (maxLength > 0 && maxCount < maxRuns) {
                        maxList.add(new int[]{maxStart, y, maxLength});
                        maxCount++;
                    }

                    if (minLength > 0 && minCount < maxRuns) {
                        minList.add(new int[]{minStart, y, minLength});
                        minCount++;
                    }
                }

                extrema[0][b] = min;
                extrema[1][b] = max;
                minCounts[b] = minCount;
                maxCounts[b] = maxCount;
            }
        }
    }

    private void accumulateStatisticsFloat(UnpackedImageData uid) {
        Rectangle rect = uid.rect;
        float[][] data = uid.getFloatData();
        int lineStride = uid.lineStride;
        int pixelStride = uid.pixelStride;

        int lineInc = lineStride * yPeriod;
        int pixelInc = pixelStride * xPeriod;

        if (!saveLocations) {
            for (int b = 0; b < srcPA.numBands; b++) {
                float min = (float)extrema[0][b];       // minimum
                float max = (float)extrema[1][b];       // maximum

                float[] d = data[b];
                int lastLine = uid.bandOffsets[b] + rect.height * lineStride;

                for (int lo = uid.bandOffsets[b]; lo < lastLine; lo += lineInc) {
                    int lastPixel = lo + rect.width * pixelStride;

                    for (int po = lo; po < lastPixel; po += pixelInc) {
                        float p = d[po];

                        if (p < min) {
                            min = p;
                        } else if (p > max) {
                            max = p;
                        }
                    }
                }
                extrema[0][b] = min;
                extrema[1][b] = max;
            }
        } else {
            for (int b = 0; b < srcPA.numBands; b++) {
                float min = (float)extrema[0][b];       // minimum
                float max = (float)extrema[1][b];       // maximum
                ArrayList minList = minLocations[b];
                ArrayList maxList = maxLocations[b];
                int minCount = minCounts[b];
                int maxCount = maxCounts[b];

                float[] d = data[b];
                int lastLine = uid.bandOffsets[b] + rect.height * lineStride;

                for (int lo = uid.bandOffsets[b], y = rect.y; lo < lastLine;
                    lo += lineInc, y += yPeriod) {

                    int lastPixel = lo + rect.width * pixelStride;
                    int minStart = 0;
                    int maxStart = 0;
                    int minLength = 0;
                    int maxLength = 0;

                    for (int po = lo, x = rect.x; po < lastPixel; po += pixelInc,
                        x += xPeriod) {

                        float p = d[po];

                        if (p < min) {
                            min = p;
                            minStart = x;
                            minLength = 1;
                            minList.clear();
                            minCount = 0;
                        } else if (p > max) {
                            max = p;
                            maxStart = x;
                            maxLength = 1;
                            maxList.clear();
                            maxCount = 0;
                        } else {
                            if (p == min) {
                                if (minLength == 0)
                                    minStart = x;
                                minLength++;
                            } else if (minLength > 0 && minCount < maxRuns) {
                                minList.add(new int[]{minStart, y, minLength});
                                minCount++;
                                minLength = 0;
                            }

                            if (p == max) {
                                if (maxLength == 0)
                                    maxStart = x;
                                maxLength++;
                            } else if (maxLength > 0 && maxCount < maxRuns) {
                                maxList.add(new int[]{maxStart, y, maxLength});
                                maxCount++;
                                maxLength = 0;
                            }
                        }
                    }

                    if (maxLength > 0 && maxCount < maxRuns) {
                        maxList.add(new int[]{maxStart, y, maxLength});
                        maxCount++;
                    }

                    if (minLength > 0 && minCount < maxRuns) {
                        minList.add(new int[]{minStart, y, minLength});
                        minCount++;
                    }
                }

                extrema[0][b] = min;
                extrema[1][b] = max;
                minCounts[b] = minCount;
                maxCounts[b] = maxCount;
            }
        }
    }

    private void accumulateStatisticsDouble(UnpackedImageData uid) {
        Rectangle rect = uid.rect;
        double[][] data = uid.getDoubleData();
        int lineStride = uid.lineStride;
        int pixelStride = uid.pixelStride;

        int lineInc = lineStride * yPeriod;
        int pixelInc = pixelStride * xPeriod;

        if (!saveLocations) {
            for (int b = 0; b < srcPA.numBands; b++) {
                double min = extrema[0][b];       // minimum
                double max = extrema[1][b];       // maximum

                double[] d = data[b];
                int lastLine = uid.bandOffsets[b] + rect.height * lineStride;

                for (int lo = uid.bandOffsets[b]; lo < lastLine; lo += lineInc) {
                    int lastPixel = lo + rect.width * pixelStride;

                    for (int po = lo; po < lastPixel; po += pixelInc) {
                        double p = d[po];

                        if (p < min) {
                            min = p;
                        } else if (p > max) {
                            max = p;
                        }
                    }
                }
                extrema[0][b] = min;
                extrema[1][b] = max;
            }
        } else {
            for (int b = 0; b < srcPA.numBands; b++) {
                double min = extrema[0][b];       // minimum
                double max = extrema[1][b];       // maximum
                ArrayList minList = minLocations[b];
                ArrayList maxList = maxLocations[b];
                int minCount = minCounts[b];
                int maxCount = maxCounts[b];

                double[] d = data[b];
                int lastLine = uid.bandOffsets[b] + rect.height * lineStride;

                for (int lo = uid.bandOffsets[b], y = rect.y; lo < lastLine;
                    lo += lineInc, y += yPeriod) {

                    int lastPixel = lo + rect.width * pixelStride;
                    int minStart = 0;
                    int maxStart = 0;
                    int minLength = 0;
                    int maxLength = 0;

                    for (int po = lo, x = rect.x; po < lastPixel; po += pixelInc,
                        x += xPeriod) {

                        double p = d[po];

                        if (p < min) {
                            min = p;
                            minStart = x;
                            minLength = 1;
                            minList.clear();
                            minCount = 0;
                        } else if (p > max) {
                            max = p;
                            maxStart = x;
                            maxLength = 1;
                            maxList.clear();
                            maxCount = 0;
                        } else {
                            if (p == min) {
                                if (minLength == 0)
                                    minStart = x;
                                minLength++;
                            } else if (minLength > 0 && minCount < maxRuns) {
                                minList.add(new int[]{minStart, y, minLength});
                                minCount++;
                                minLength = 0;
                            }

                            if (p == max) {
                                if (maxLength == 0)
                                    maxStart = x;
                                maxLength++;
                            } else if (maxLength > 0 && maxCount < maxRuns) {
                                maxList.add(new int[]{maxStart, y, maxLength});
                                maxCount++;
                                maxLength = 0;
                            }
                        }
		    }

                    if (maxLength > 0 && maxCount < maxRuns) {
                        maxList.add(new int[]{maxStart, y, maxLength});
                        maxCount++;
                    }

                    if (minLength > 0 && minCount < maxRuns) {
                        minList.add(new int[]{minStart, y, minLength});
                        minCount++;
                    }
                }

                extrema[0][b] = min;
                extrema[1][b] = max;
                minCounts[b] = minCount;
                maxCounts[b] = maxCount;
            }
        }
    }

    protected void initializeState(Raster source) {
        if (extrema == null) {
            int numBands = sampleModel.getNumBands();
            extrema = new double[2][numBands];

            Rectangle rect = source.getBounds();

            // Initialize extrema with the first pixel value.
	    // Fix 4810617: Extrema intialization problem; When a ROI
	    // parameter is used, the ROI may not include the fix pixel
	    // of the image.  So initializing with the first pixel value
	    // of the image is not correct.
	    if (roi != null) {
            	LinkedList rectList = roi.getAsRectangleList(rect.x,
                                              rect.y,
                                              rect.width,
                                              rect.height);
            	if (rectList == null) {
                    return; // ROI does not intersect with Raster boundary.
            	}   
        	ListIterator iterator = rectList.listIterator(0);
        	if (iterator.hasNext()) 
            	    rect = rect.intersection((Rectangle)iterator.next());
            }           
                    
            // Find the actual ROI based on start and period.
            rect.x = startPosition(rect.x, xStart, xPeriod);
            rect.y = startPosition(rect.y, yStart, yPeriod);
            source.getPixel(rect.x, rect.y, extrema[0]);

            for (int i = 0; i < numBands; i++) {
                extrema[1][i] = extrema[0][i];
            }

            if (saveLocations) {
                minLocations = new ArrayList[numBands];
                maxLocations = new ArrayList[numBands];
                minCounts = new int[numBands];
                maxCounts = new int[numBands];
                for (int i = 0; i < numBands; i++) {
                    minLocations[i] = new ArrayList();
                    maxLocations[i] = new ArrayList();
                    minCounts[i] = maxCounts[i] = 0;
                }
            }
        }
    }

}
