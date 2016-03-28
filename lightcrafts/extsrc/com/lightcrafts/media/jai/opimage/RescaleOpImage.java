/*
 * $RCSfile: RescaleOpImage.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:41 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.opimage;
import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;

import com.lightcrafts.mediax.jai.ColormapOpImage;
import com.lightcrafts.mediax.jai.ImageLayout;
import com.lightcrafts.mediax.jai.RasterAccessor;
import com.lightcrafts.mediax.jai.RasterFormatTag;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.lightcrafts.media.jai.util.ImageUtil;

/**
 * An <code>OpImage</code> implementing the "Rescale" operation.
 *
 * <p> The "Rescale" operation maps the pixel values of an image from
 * one range to another range by multiplying each pixel value by one
 * of a set of constants and then adding another constant to the
 * result of the multiplication. The pixel values of the destination
 * image are defined by the pseudocode:
 *
 * <pre>
 *     for (int h = 0; h < dstHeight; h++) {
 *         for (int w = 0; w < dstWidth; w++) {
 *             for (int b = 0; b < dstNumBands; b++) {
 *                 scale = (scales.length < dstNumBands)?
 *                 scales[0]:scales[b];
 *                 offset = (offsets.length < dstNumBands)?
 *                 offsets[0]:offsets[b];
 *                 dst[h][w][b] = srcs[h][w][b] * scale + offset;
 *             }
 *         }
 *     }
 * </pre>
 *
 * @see com.lightcrafts.mediax.jai.operator.RescaleDescriptor
 * @see RescaleCRIF
 *
 *
 * @since EA3
 */
final class RescaleOpImage extends ColormapOpImage {

    /** The constants to be multiplied, one for each band. */
    protected double[] constants;
    protected double[] offsets;

    private byte[][] byteTable = null;

    static final int numProc = Runtime.getRuntime().availableProcessors();

    private synchronized void initByteTable() {

        if (byteTable != null) {
            return;
        }

        int nbands = constants.length;

        byteTable = new byte[nbands][256];

        // Initialize table which implements Rescale and clamp
        for(int band=0; band<nbands; band++) {
            byte[] t = byteTable[band];
            double c = constants[band];
            double o = offsets[band];
            for (int i = 0; i < 256; i++) {
                t[i] = ImageUtil.clampRoundByte(i * c + o);
            }
        }
    }

    /**
     * Constructor.
     *
     * @param source     The source image.
     * @param config     Configurable attributes of the image including
     *        configuration variables indexed by
     *        <code>RenderingHints.Key</code>s and image properties indexed
     *        by <code>String</code>s or <code>CaselessStringKey</code>s.
     *        This is simply forwarded to the superclass constructor.
     * @param layout     The destination image layout.
     * @param constants  The constants to be multiplied, stored as reference.
     * @param offsets    The offsets to be added, stored as reference.
     */
    public RescaleOpImage(RenderedImage source,
                          Map config,
                          ImageLayout layout,
                          double[] constants,
                          double[] offsets) {
        super(source, layout, config, true);

        int numBands = getSampleModel().getNumBands();

        if (constants.length < numBands) {
            this.constants = new double[numBands];
            for (int i = 0; i < numBands; i++) {
                this.constants[i] = constants[0];
            }
        } else {
            this.constants = constants;
        }

        if (offsets.length < numBands) {
            this.offsets   = new double[numBands];
            for (int i = 0; i < numBands; i++) {
                this.offsets[i]   = offsets[0];
            }
        } else {
            this.offsets = offsets;
        }

        // Set flag to permit in-place operation.
        permitInPlaceOperation();

        // Initialize the colormap if necessary.
        initializeColormapOperation();
    }

    /**
     * Transform the colormap according to the rescaling parameters.
     */
    protected void transformColormap(byte[][] colormap) {
        for (int b = 0; b < 3; b++) {
            byte[] map = colormap[b];
            int mapSize = map.length;

            float c = (float)(b < constants.length ?
                              constants[b] : constants[0]);
            float o = (float)(b < constants.length ?
                              offsets[b] : offsets[0]);

            for (int i = 0; i < mapSize; i++) {
                map[i] = ImageUtil.clampRoundByte((map[i] & 0xFF) * c + o);
            }
        }
    }

    /**
     * Rescales to the pixel values within a specified rectangle.
     *
     * @param sources   Cobbled sources, guaranteed to provide all the
     *                  source data necessary for computing the rectangle.
     * @param dest      The tile containing the rectangle to be computed.
     * @param destRect  The rectangle within the tile to be computed.
     */
    protected void computeRect(Raster[] sources,
                               WritableRaster dest,
                               Rectangle destRect) {
        // Retrieve format tags.
        RasterFormatTag[] formatTags = getFormatTags();

        Rectangle srcRect = mapDestRect(destRect, 0);

        RasterAccessor dst = new RasterAccessor(dest, destRect,
                                                formatTags[1], getColorModel());
        RasterAccessor src = new RasterAccessor(sources[0], srcRect,
                                                formatTags[0],
                                                getSourceImage(0).getColorModel());

        switch (dst.getDataType()) {
        case DataBuffer.TYPE_BYTE:
            computeRectByte(src, dst);
            break;
        case DataBuffer.TYPE_USHORT:
            computeRectUShort(src, dst);
            break;
        case DataBuffer.TYPE_SHORT:
            computeRectShort(src, dst);
            break;
        case DataBuffer.TYPE_INT:
            computeRectInt(src, dst);
            break;
        case DataBuffer.TYPE_FLOAT:
            computeRectFloat(src, dst);
            break;
        case DataBuffer.TYPE_DOUBLE:
            computeRectDouble(src, dst);
            break;
        }

        if (dst.needsClamping()) {
            /* Further clamp down to underlying raster data type. */
            dst.clampDataArrays();
        }
        dst.copyDataToRaster();
    }

    private void computeRectByte(RasterAccessor src,
                                 RasterAccessor dst) {
        final int dstWidth = dst.getWidth();
        final int dstHeight = dst.getHeight();
        final int dstBands = dst.getNumBands();

        final int dstLineStride = dst.getScanlineStride();
        final int dstPixelStride = dst.getPixelStride();
        final int[] dstBandOffsets = dst.getBandOffsets();
        final byte[][] dstData = dst.getByteDataArrays();

        final int srcLineStride = src.getScanlineStride();
        final int srcPixelStride = src.getPixelStride();
        final int[] srcBandOffsets = src.getBandOffsets();
        final byte[][] srcData = src.getByteDataArrays();

        initByteTable();

        ExecutorService threadPool = Executors.newFixedThreadPool(numProc);
        Collection<Callable<Void>> processes = new LinkedList<Callable<Void>>();
        for (int band = 0; band < dstBands; band++) {
            final byte[] s = srcData[band];
            final byte[] d = dstData[band];

            final int dstLineOffset = dstBandOffsets[band];
            final int srcLineOffset = srcBandOffsets[band];

            final byte[] clamp = byteTable[band];

            for (int h = 0; h < dstHeight; h++) {
                final int hh = h;
                processes.add(new Callable<Void>() {
                    @Override
                    public Void call() {
                        int dstPixelOffset = dstLineOffset + hh * dstLineStride;
                        int srcPixelOffset = srcLineOffset + hh * srcLineStride;

                        for (int w = 0; w < dstWidth; w++) {
                            d[dstPixelOffset] = clamp[s[srcPixelOffset] & 0xFF];

                            dstPixelOffset += dstPixelStride;
                            srcPixelOffset += srcPixelStride;
                        }
                        return null;
                    }
                });
            }
        }
        try {
            threadPool.invokeAll(processes);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            threadPool.shutdown();
        }
    }

    private void computeRectUShort(RasterAccessor src,
                                   RasterAccessor dst) {
        final int dstWidth = dst.getWidth();
        final int dstHeight = dst.getHeight();
        final int dstBands = dst.getNumBands();

        final int dstLineStride = dst.getScanlineStride();
        final int dstPixelStride = dst.getPixelStride();
        final int[] dstBandOffsets = dst.getBandOffsets();
        final short[][] dstData = dst.getShortDataArrays();

        final int srcLineStride = src.getScanlineStride();
        final int srcPixelStride = src.getPixelStride();
        final int[] srcBandOffsets = src.getBandOffsets();
        final short[][] srcData = src.getShortDataArrays();

        ExecutorService threadPool = Executors.newFixedThreadPool(numProc);
        Collection<Callable<Void>> processes = new LinkedList<Callable<Void>>();
        for (int band = 0; band < dstBands; band++) {
            final float c = (float)constants[band];
            final float o = (float)offsets[band];
            final short[] s = srcData[band];
            final short[] d = dstData[band];

            final int dstLineOffset = dstBandOffsets[band];
            final int srcLineOffset = srcBandOffsets[band];

            for (int h = 0; h < dstHeight; h++) {
                final int hh = h;
                processes.add(new Callable<Void>() {
                    @Override
                    public Void call() {
                        int dstPixelOffset = dstLineOffset + hh * dstLineStride;
                        int srcPixelOffset = srcLineOffset + hh * srcLineStride;

                        for (int w = 0; w < dstWidth; w++) {
                            d[dstPixelOffset] = ImageUtil.clampRoundUShort(
                                    (s[srcPixelOffset] & 0xFFFF) * c + o);

                            dstPixelOffset += dstPixelStride;
                            srcPixelOffset += srcPixelStride;
                        }
                    return null;
                    }
                });
            }
        }
        try {
            threadPool.invokeAll(processes);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            threadPool.shutdown();
        }
    }

    private void computeRectShort(RasterAccessor src,
                                  RasterAccessor dst) {
        final int dstWidth = dst.getWidth();
        final int dstHeight = dst.getHeight();
        final int dstBands = dst.getNumBands();

        final int dstLineStride = dst.getScanlineStride();
        final int dstPixelStride = dst.getPixelStride();
        final int[] dstBandOffsets = dst.getBandOffsets();
        final short[][] dstData = dst.getShortDataArrays();

        final int srcLineStride = src.getScanlineStride();
        final int srcPixelStride = src.getPixelStride();
        final int[] srcBandOffsets = src.getBandOffsets();
        final short[][] srcData = src.getShortDataArrays();

        ExecutorService threadPool = Executors.newFixedThreadPool(numProc);
        Collection<Callable<Void>> processes = new LinkedList<Callable<Void>>();
        for (int band = 0; band < dstBands; band++) {
            final float c = (float)constants[band];
            final float o = (float)offsets[band];
            final short[] s = srcData[band];
            final short[] d = dstData[band];

            final int dstLineOffset = dstBandOffsets[band];
            final int srcLineOffset = srcBandOffsets[band];

            for (int h = 0; h < dstHeight; h++) {
                final int hh = h;
                processes.add(new Callable<Void>() {
                    @Override
                    public Void call() {
                        int dstPixelOffset = dstLineOffset + hh * dstLineStride;
                        int srcPixelOffset = srcLineOffset + hh * srcLineStride;

                        for (int w = 0; w < dstWidth; w++) {
                            d[dstPixelOffset] = ImageUtil.clampRoundShort(s[srcPixelOffset] * c + o);

                            dstPixelOffset += dstPixelStride;
                            srcPixelOffset += srcPixelStride;
                        }
                    return null;
                    }
                });
            }
        }
        try {
            threadPool.invokeAll(processes);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            threadPool.shutdown();
        }
    }

    private void computeRectInt(RasterAccessor src,
                                RasterAccessor dst) {
        final int dstWidth = dst.getWidth();
        final int dstHeight = dst.getHeight();
        final int dstBands = dst.getNumBands();

        final int dstLineStride = dst.getScanlineStride();
        final int dstPixelStride = dst.getPixelStride();
        final int[] dstBandOffsets = dst.getBandOffsets();
        final int[][] dstData = dst.getIntDataArrays();

        final int srcLineStride = src.getScanlineStride();
        final int srcPixelStride = src.getPixelStride();
        final int[] srcBandOffsets = src.getBandOffsets();
        final int[][] srcData = src.getIntDataArrays();

        ExecutorService threadPool = Executors.newFixedThreadPool(numProc);
        Collection<Callable<Void>> processes = new LinkedList<Callable<Void>>();
        for (int b = 0; b < dstBands; b++) {
            final double c = constants[b];
            final double o = offsets[b];
            final int[] s = srcData[b];
            final int[] d = dstData[b];

            final int dstLineOffset = dstBandOffsets[b];
            final int srcLineOffset = srcBandOffsets[b];

            for (int h = 0; h < dstHeight; h++) {
                final int hh = h;
                processes.add(new Callable<Void>() {
                    @Override
                    public Void call() {
                        int dstPixelOffset = dstLineOffset + hh * dstLineStride;
                        int srcPixelOffset = srcLineOffset + hh * srcLineStride;

                        for (int w = 0; w < dstWidth; w++) {
                            d[dstPixelOffset] = ImageUtil.clampRoundInt(s[srcPixelOffset] * c + o);

                            dstPixelOffset += dstPixelStride;
                            srcPixelOffset += srcPixelStride;
                        }
                        return null;
                    }
                });
            }
        }
        try {
            threadPool.invokeAll(processes);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            threadPool.shutdown();
        }
    }

    private void computeRectFloat(RasterAccessor src,
                                  RasterAccessor dst) {
        final int dstWidth = dst.getWidth();
        final int dstHeight = dst.getHeight();
        final int dstBands = dst.getNumBands();

        final int dstLineStride = dst.getScanlineStride();
        final int dstPixelStride = dst.getPixelStride();
        final int[] dstBandOffsets = dst.getBandOffsets();
        final float[][] dstData = dst.getFloatDataArrays();

        final int srcLineStride = src.getScanlineStride();
        final int srcPixelStride = src.getPixelStride();
        final int[] srcBandOffsets = src.getBandOffsets();
        final float[][] srcData = src.getFloatDataArrays();

        ExecutorService threadPool = Executors.newFixedThreadPool(numProc);
        Collection<Callable<Void>> processes = new LinkedList<Callable<Void>>();
        for (int band = 0; band < dstBands; band++) {
            final double c = constants[band];
            final double o = offsets[band];
            final float[] s = srcData[band];
            final float[] d = dstData[band];

            final int dstLineOffset = dstBandOffsets[band];
            final int srcLineOffset = srcBandOffsets[band];

            for (int h = 0; h < dstHeight; h++) {
                final int hh = h;
                processes.add(new Callable<Void>() {
                    @Override
                    public Void call() {
                        int dstPixelOffset = dstLineOffset + hh * dstLineStride;
                        int srcPixelOffset = srcLineOffset + hh * srcLineStride;

                        for (int w = 0; w < dstWidth; w++) {
                            d[dstPixelOffset] = ImageUtil.clampFloat(s[srcPixelOffset] * c + o);

                            dstPixelOffset += dstPixelStride;
                            srcPixelOffset += srcPixelStride;
                        }
                        return null;
                    }
                });
            }
        }
        try {
            threadPool.invokeAll(processes);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            threadPool.shutdown();
        }
    }

    private void computeRectDouble(RasterAccessor src,
                                   RasterAccessor dst) {
        final int dstWidth = dst.getWidth();
        final int dstHeight = dst.getHeight();
        final int dstBands = dst.getNumBands();

        final int dstLineStride = dst.getScanlineStride();
        final int dstPixelStride = dst.getPixelStride();
        final int[] dstBandOffsets = dst.getBandOffsets();
        final double[][] dstData = dst.getDoubleDataArrays();

        final int srcLineStride = src.getScanlineStride();
        final int srcPixelStride = src.getPixelStride();
        final int[] srcBandOffsets = src.getBandOffsets();
        final double[][] srcData = src.getDoubleDataArrays();

        ExecutorService threadPool = Executors.newFixedThreadPool(numProc);
        Collection<Callable<Void>> processes = new LinkedList<Callable<Void>>();
        for (int band = 0; band < dstBands; band++) {
            final double c = constants[band];
            final double o = offsets[band];
            final double[] s = srcData[band];
            final double[] d = dstData[band];

            final int dstLineOffset = dstBandOffsets[band];
            final int srcLineOffset = srcBandOffsets[band];

            for (int h = 0; h < dstHeight; h++) {
                final int hh = h;
                processes.add(new Callable<Void>() {
                    @Override
                    public Void call() {
                        int dstPixelOffset = dstLineOffset + hh * dstLineStride;
                        int srcPixelOffset = srcLineOffset + hh * srcLineStride;

                        for (int w = 0; w < dstWidth; w++) {
                            d[dstPixelOffset] = s[srcPixelOffset] * c + o;

                            dstPixelOffset += dstPixelStride;
                            srcPixelOffset += srcPixelStride;
                        }
                    return null;
                    }
                });
            }
        }
        try {
            threadPool.invokeAll(processes);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            threadPool.shutdown();
        }
    }
}
