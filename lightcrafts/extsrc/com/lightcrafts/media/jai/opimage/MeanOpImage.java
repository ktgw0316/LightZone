/*
 * $RCSfile: MeanOpImage.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:33 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.opimage;
import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.util.LinkedList;
import java.util.ListIterator;
import com.lightcrafts.mediax.jai.PixelAccessor;
import com.lightcrafts.mediax.jai.ROI;
import com.lightcrafts.mediax.jai.StatisticsOpImage;
import com.lightcrafts.mediax.jai.UnpackedImageData;

/**
 * An <code>OpImage</code> implementing the "Mean" operation as
 * described in <code>com.lightcrafts.mediax.jai.operator.MeanDescriptor</code>.
 *
 * @since EA2
 * @see com.lightcrafts.mediax.jai.operator.MeanDescriptor
 * @see MeanCRIF
 *
 */
public class MeanOpImage extends StatisticsOpImage {

    private boolean isInitialized = false;

    /**
     * Note: For very large images, these two variables may be overflowed.
     * An alternative would be to have a set for each tile. But then, the
     * user could specify very large tile size.
     */
    private double[] totalPixelValue;
    private int totalPixelCount;

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
     * Constructs an <code>MeanOpImage</code>.
     *
     * @param source  The source image.
     */
    public MeanOpImage(RenderedImage source,
                       ROI roi,
                       int xStart,
                       int yStart,
                       int xPeriod,
                       int yPeriod) {
        super(source, roi, xStart, yStart, xPeriod, yPeriod);
    }

    protected String[] getStatisticsNames() {
        return new String[] {"mean"};
    }

    protected Object createStatistics(String name) {
        Object stats;

        if (name.equalsIgnoreCase("mean")) {
            stats = new double[sampleModel.getNumBands()];
        } else {
            stats = java.awt.Image.UndefinedProperty;
        }
        return stats;
    }

    private final int startPosition(int pos, int start, int period) {
        int t = (pos - start) % period;
        if (t == 0) {
            return pos;
        } else {
            return (pos + (period - t));
        }
    }

    protected void accumulateStatistics(String name,
                                        Raster source,
                                        Object stats) {
        if(!isInitialized) {
            srcPA = new PixelAccessor(getSourceImage(0));
            srcSampleType = srcPA.sampleType == PixelAccessor.TYPE_BIT ?
                DataBuffer.TYPE_BYTE : srcPA.sampleType;

            totalPixelValue = new double[srcPA.numBands];
            totalPixelCount = 0;
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

            /* Find the actual ROI based on start and period. */
            rect.x = startPosition(tx, xStart, xPeriod);
            rect.y = startPosition(ty, yStart, yPeriod);
            rect.width = tx + rect.width - rect.x;
            rect.height = ty + rect.height - rect.y;

            if (rect.isEmpty()) {
                continue;	// no pixel to count in this rectangle
            }

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

        if(name.equalsIgnoreCase("mean")) {
            // This is a totally disgusting hack but no worse than the
            // code was before ... bpb 1 September 2000
            double[] mean = (double[])stats;
            if (totalPixelCount != 0) {
                for (int i = 0; i < srcPA.numBands; i++) {
                    mean[i] = totalPixelValue[i] /
                        (double)totalPixelCount;
                }
            }
        }
    }

    private void accumulateStatisticsByte(UnpackedImageData uid) {
        Rectangle rect = uid.rect;
        byte[][] data = uid.getByteData();
        int lineStride = uid.lineStride;
        int pixelStride = uid.pixelStride;

        int lineInc = lineStride * yPeriod;
        int pixelInc = pixelStride * xPeriod;

        for (int b = 0; b < srcPA.numBands; b++) {
            byte[] d = data[b];
            int lastLine = uid.bandOffsets[b] + rect.height * lineStride;

            for (int lo = uid.bandOffsets[b]; lo < lastLine; lo += lineInc) {
                int lastPixel = lo + rect.width * pixelStride;

                for (int po = lo; po < lastPixel; po += pixelInc) {
                    totalPixelValue[b] += d[po] & 0xff;
                }
            }
        }
        totalPixelCount += (int)Math.ceil((double)rect.height / yPeriod) *
                           (int)Math.ceil((double)rect.width / xPeriod);
    }

    private void accumulateStatisticsUShort(UnpackedImageData uid) {
        Rectangle rect = uid.rect;
        short[][] data = uid.getShortData();
        int lineStride = uid.lineStride;
        int pixelStride = uid.pixelStride;

        int lineInc = lineStride * yPeriod;
        int pixelInc = pixelStride * xPeriod;

        for (int b = 0; b < srcPA.numBands; b++) {
            short[] d = data[b];
            int lastLine = uid.bandOffsets[b] + rect.height * lineStride;

            for (int lo = uid.bandOffsets[b]; lo < lastLine; lo += lineInc) {
                int lastPixel = lo + rect.width * pixelStride;

                for (int po = lo; po < lastPixel; po += pixelInc) {
                    totalPixelValue[b] += d[po] & 0xffff;
                }
            }
        }
        totalPixelCount += (int)Math.ceil((double)rect.height / yPeriod) *
                           (int)Math.ceil((double)rect.width / xPeriod);
    }

    private void accumulateStatisticsShort(UnpackedImageData uid) {
        Rectangle rect = uid.rect;
        short[][] data = uid.getShortData();
        int lineStride = uid.lineStride;
        int pixelStride = uid.pixelStride;

        int lineInc = lineStride * yPeriod;
        int pixelInc = pixelStride * xPeriod;

        for (int b = 0; b < srcPA.numBands; b++) {
            short[] d = data[b];
            int lastLine = uid.bandOffsets[b] + rect.height * lineStride;

            for (int lo = uid.bandOffsets[b]; lo < lastLine; lo += lineInc) {
                int lastPixel = lo + rect.width * pixelStride;

                for (int po = lo; po < lastPixel; po += pixelInc) {
                    totalPixelValue[b] += d[po];
                }
            }
        }
        totalPixelCount += (int)Math.ceil((double)rect.height / yPeriod) *
                           (int)Math.ceil((double)rect.width / xPeriod);
    }

    private void accumulateStatisticsInt(UnpackedImageData uid) {
        Rectangle rect = uid.rect;
        int[][] data = uid.getIntData();
        int lineStride = uid.lineStride;
        int pixelStride = uid.pixelStride;

        int lineInc = lineStride * yPeriod;
        int pixelInc = pixelStride * xPeriod;

        for (int b = 0; b < srcPA.numBands; b++) {
            int[] d = data[b];
            int lastLine = uid.bandOffsets[b] + rect.height * lineStride;

            for (int lo = uid.bandOffsets[b]; lo < lastLine; lo += lineInc) {
                int lastPixel = lo + rect.width * pixelStride;

                for (int po = lo; po < lastPixel; po += pixelInc) {
                    totalPixelValue[b] += d[po];
                }
            }
        }
        totalPixelCount += (int)Math.ceil((double)rect.height / yPeriod) *
                           (int)Math.ceil((double)rect.width / xPeriod);
    }

    private void accumulateStatisticsFloat(UnpackedImageData uid) {
        Rectangle rect = uid.rect;
        float[][] data = uid.getFloatData();
        int lineStride = uid.lineStride;
        int pixelStride = uid.pixelStride;

        int lineInc = lineStride * yPeriod;
        int pixelInc = pixelStride * xPeriod;

        for (int b = 0; b < srcPA.numBands; b++) {
            float[] d = data[b];
            int lastLine = uid.bandOffsets[b] + rect.height * lineStride;

            for (int lo = uid.bandOffsets[b]; lo < lastLine; lo += lineInc) {
                int lastPixel = lo + rect.width * pixelStride;

                for (int po = lo; po < lastPixel; po += pixelInc) {
                    totalPixelValue[b] += d[po];
                }
            }
        }
        totalPixelCount += (int)Math.ceil((double)rect.height / yPeriod) *
                           (int)Math.ceil((double)rect.width / xPeriod);
    }

    private void accumulateStatisticsDouble(UnpackedImageData uid) {
        Rectangle rect = uid.rect;
        double[][] data = uid.getDoubleData();
        int lineStride = uid.lineStride;
        int pixelStride = uid.pixelStride;

        int lineInc = lineStride * yPeriod;
        int pixelInc = pixelStride * xPeriod;

        for (int b = 0; b < srcPA.numBands; b++) {
            double[] d = data[b];
            int lastLine = uid.bandOffsets[b] + rect.height * lineStride;

            for (int lo = uid.bandOffsets[b]; lo < lastLine; lo += lineInc) {
                int lastPixel = lo + rect.width * pixelStride;

                for (int po = lo; po < lastPixel; po += pixelInc) {
                    totalPixelValue[b] += d[po];
                }
            }
        }
        totalPixelCount += (int)Math.ceil((double)rect.height / yPeriod) *
                           (int)Math.ceil((double)rect.width / xPeriod);
    }
}
