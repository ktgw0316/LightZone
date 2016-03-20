/*
 * $RCSfile: BandCombineOpImage.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:15 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.opimage;
import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.lightcrafts.mediax.jai.ImageLayout;
import com.lightcrafts.mediax.jai.PointOpImage;
import com.lightcrafts.mediax.jai.RasterAccessor;
import com.lightcrafts.mediax.jai.RasterFormatTag;
import com.lightcrafts.mediax.jai.RasterFactory;
import com.lightcrafts.media.jai.util.ImageUtil;
import com.lightcrafts.media.jai.util.JDKWorkarounds;

/**
 * An <code>OpImage</code> implementing the "BandCombine" operation.
 *
 * <p>This <code>OpImage</code> performs the arbitrary interband
 * linear combination of an image using the specified matrix.  The
 * width of the matrix must be one larger that the number of bands
 * in the source image.  The height of the matrix must be equal to
 * the number of bands in the destination image.  Because the matrix
 * can be of arbitrary size, this function can be used to produce
 * a destination image with a different number of bands from the
 * source image.
 * <p>The destination image is formed by performing a matrix-
 * multiply operation between the bands of the source image and
 * the specified matrix.  The extra column of values is a constant
 * that is added after the matrix-multiply operation takes place.
 *
 * @see com.lightcrafts.mediax.jai.operator.BandCombineDescriptor
 * @see BandCombineCRIF
 *
 *
 * @since EA3
 */
final class BandCombineOpImage extends PointOpImage {

    static final int numProc = Runtime.getRuntime().availableProcessors();

    private double[][] matrix;

    /**
     * Constructor.
     *
     * @param source       The source image.
     * @param layout       The destination image layout.
     * @param matrix       The matrix of values used to perform the
     *                     linear combination.
     */
    public BandCombineOpImage(RenderedImage source,
                              Map config,
                              ImageLayout layout,
                              double[][] matrix) {
        super(source, layout, config, true);

        this.matrix = matrix;

        int numBands = matrix.length;  // matrix height is dst numBands
        if (getSampleModel().getNumBands() != numBands) {
            sampleModel = RasterFactory.createComponentSampleModel(sampleModel,
                                  sampleModel.getDataType(),
                                  tileWidth, tileHeight, numBands);

            if(colorModel != null &&
               !JDKWorkarounds.areCompatibleDataModels(sampleModel,
                                                       colorModel)) {
                colorModel = ImageUtil.getCompatibleColorModel(sampleModel,
                                                               config);
            }
        }
    }

    /**
     * Performs linear combination of source image with matrix
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

        RasterAccessor s = new RasterAccessor(sources[0], destRect,  
                                              formatTags[0], 
                                              getSourceImage(0).getColorModel());
        RasterAccessor d = new RasterAccessor(dest, destRect,  
                                              formatTags[1], getColorModel());

        switch (d.getDataType()) {
        case DataBuffer.TYPE_BYTE:
            computeRectByte(s, d);
            break;
        case DataBuffer.TYPE_USHORT:
            computeRectUShort(s, d);
            break;
        case DataBuffer.TYPE_SHORT:
            computeRectShort(s, d);
            break;
        case DataBuffer.TYPE_INT:
            computeRectInt(s, d);
            break;
        case DataBuffer.TYPE_FLOAT:
            computeRectFloat(s, d);
            break;
        case DataBuffer.TYPE_DOUBLE:
            computeRectDouble(s, d);
            break;
        }

        if (d.isDataCopy()) {
            d.clampDataArrays();
            d.copyDataToRaster();
        }
    }

    private void computeRectByte(RasterAccessor src, RasterAccessor dst) {
        int sLineStride = src.getScanlineStride();
        int sPixelStride = src.getPixelStride();
        int sbands = src.getNumBands();
        int[] sBandOffsets = src.getBandOffsets();
        byte[][] sData = src.getByteDataArrays();

        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int dbands = dst.getNumBands();
        int dLineStride = dst.getScanlineStride();
        int dPixelStride = dst.getPixelStride();
        int[] dBandOffsets = dst.getBandOffsets();
        byte[][] dData = dst.getByteDataArrays();

        int sso = 0, dso = 0;

        for (int h = 0; h < dheight; h++) {
            int spo = sso;
            int dpo = dso;

            for (int w = 0; w < dwidth; w++) {
                for (int b = 0; b < dbands; b++) {
                    float sum = 0.0F;
                    double[] mat = matrix[b];

                    for (int k = 0; k < sbands; k++ ) {
                        sum += (float)mat[k] *
                               (float)(sData[k][spo+sBandOffsets[k]] & 0xFF);
                    }

                    dData[b][dpo+dBandOffsets[b]] = ImageUtil.clampRoundByte(sum + (float)mat[sbands]);
                }

                spo += sPixelStride;
                dpo += dPixelStride;
            }

            sso += sLineStride;
            dso += dLineStride;
        }
    }

    private void computeRectUShort(RasterAccessor src, RasterAccessor dst) {
        final int sLineStride = src.getScanlineStride();
        final int sPixelStride = src.getPixelStride();
        final int sbands = src.getNumBands();
        final int[] sBandOffsets = src.getBandOffsets();
        final short[][] sData = src.getShortDataArrays();

        final int dwidth = dst.getWidth();
        final int dheight = dst.getHeight();
        final int dbands = dst.getNumBands();
        final int dLineStride = dst.getScanlineStride();
        final int dPixelStride = dst.getPixelStride();
        final int[] dBandOffsets = dst.getBandOffsets();
        final short[][] dData = dst.getShortDataArrays();

        int sso = 0, dso = 0;

        ExecutorService threadPool = Executors.newFixedThreadPool(numProc);
        Collection<Callable<Void>> processes = new LinkedList<Callable<Void>>();
        for (int h = 0; h < dheight; h++) {
            final int sso_f = sso;
            final int dso_f = dso;
            processes.add(new Callable<Void>() {
                @Override
                public Void call() {
                    int spo = sso_f;
                    int dpo = dso_f;

                    for (int w = 0; w < dwidth; w++) {
                        for (int b = 0; b < dbands; b++) {
                            float sum = 0.0F;
                            double[] mat = matrix[b];

                            for (int k = 0; k < sbands; k++) {
                                sum += (float) mat[k] *
                                       (float) (sData[k][spo + sBandOffsets[k]] & 0xFFFF);
                            }
                            dData[b][dpo + dBandOffsets[b]] =
                                    ImageUtil.clampRoundUShort(sum + (float) matrix[b][sbands]);
                        }
                        spo += sPixelStride;
                        dpo += dPixelStride;
                    }
                    return null;
                }
            });
            sso += sLineStride;
            dso += dLineStride;
        }
        try {
            threadPool.invokeAll(processes);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            threadPool.shutdown();
        }
    }

    private void computeRectShort(RasterAccessor src, RasterAccessor dst) {
        int sLineStride = src.getScanlineStride();
        int sPixelStride = src.getPixelStride();
        int sbands = src.getNumBands();
        int[] sBandOffsets = src.getBandOffsets();
        short[][] sData = src.getShortDataArrays();

        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int dbands = dst.getNumBands();
        int dLineStride = dst.getScanlineStride();
        int dPixelStride = dst.getPixelStride();
        int[] dBandOffsets = dst.getBandOffsets();
        short[][] dData = dst.getShortDataArrays();

        int sso = 0, dso = 0;

        for (int h = 0; h < dheight; h++) {
            int spo = sso;
            int dpo = dso;

            for (int w = 0; w < dwidth; w++) {
                for (int b = 0; b < dbands; b++) {
                    float sum = 0.0F;
                    double[] mat = matrix[b];

                    for (int k = 0; k < sbands; k++ ) {
                        sum += (float)mat[k] *
                               (float)(sData[k][spo+sBandOffsets[k]]);
                    }

                    dData[b][dpo+dBandOffsets[b]] = ImageUtil.clampRoundUShort(sum + (float)matrix[b][sbands]);
                }

                spo += sPixelStride;
                dpo += dPixelStride;
            }

            sso += sLineStride;
            dso += dLineStride;
        }

    }

    private void computeRectInt(RasterAccessor src, RasterAccessor dst) {
        int sLineStride = src.getScanlineStride();
        int sPixelStride = src.getPixelStride();
        int sbands = src.getNumBands();
        int[] sBandOffsets = src.getBandOffsets();
        int[][] sData = src.getIntDataArrays();

        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int dbands = dst.getNumBands();
        int dLineStride = dst.getScanlineStride();
        int dPixelStride = dst.getPixelStride();
        int[] dBandOffsets = dst.getBandOffsets();
        int[][] dData = dst.getIntDataArrays();

        int sso = 0, dso = 0;

        for (int h = 0; h < dheight; h++) {
            int spo = sso;
            int dpo = dso;

            for (int w = 0; w < dwidth; w++) {
                for (int b = 0; b < dbands; b++) {
                    float sum = 0.0F;
                    double[] mat = matrix[b];

                    for (int k = 0; k < sbands; k++ ) {
                        sum += (float)mat[k] *
                               (float)(sData[k][spo+sBandOffsets[k]]);
                    }

                    dData[b][dpo+dBandOffsets[b]] = ImageUtil.clampRoundInt(sum + (float)matrix[b][sbands]);
                }

                spo += sPixelStride;
                dpo += dPixelStride;
            }

            sso += sLineStride;
            dso += dLineStride;
        }
    }

    private void computeRectFloat(RasterAccessor src, RasterAccessor dst) {
        int sLineStride = src.getScanlineStride();
        int sPixelStride = src.getPixelStride();
        int sbands = src.getNumBands();
        int[] sBandOffsets = src.getBandOffsets();
        float[][] sData = src.getFloatDataArrays();

        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int dbands = dst.getNumBands();
        int dLineStride = dst.getScanlineStride();
        int dPixelStride = dst.getPixelStride();
        int[] dBandOffsets = dst.getBandOffsets();
        float[][] dData = dst.getFloatDataArrays();

        int sso = 0, dso = 0;

        for (int h = 0; h < dheight; h++) {
            int spo = sso;
            int dpo = dso;

            for (int w = 0; w < dwidth; w++) {
                for (int b = 0; b < dbands; b++) {
                    float sum = 0.0F;
                    double[] mat = matrix[b];

                    for (int k = 0; k < sbands; k++ ) {
                        sum += (float)mat[k] * sData[k][spo+sBandOffsets[k]];
                    }

                    dData[b][dpo+dBandOffsets[b]] = sum + (float)matrix[b][sbands];
                }

                spo += sPixelStride;
                dpo += dPixelStride;
            }

            sso += sLineStride;
            dso += dLineStride;
        }
    }

    private void computeRectDouble(RasterAccessor src, RasterAccessor dst) {
        int sLineStride = src.getScanlineStride();
        int sPixelStride = src.getPixelStride();
        int sbands = src.getNumBands();
        int[] sBandOffsets = src.getBandOffsets();
        double[][] sData = src.getDoubleDataArrays();

        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int dbands = dst.getNumBands();
        int dLineStride = dst.getScanlineStride();
        int dPixelStride = dst.getPixelStride();
        int[] dBandOffsets = dst.getBandOffsets();
        double[][] dData = dst.getDoubleDataArrays();

        int sso = 0, dso = 0;

        for (int h = 0; h < dheight; h++) {
            int spo = sso;
            int dpo = dso;

            for (int w = 0; w < dwidth; w++) {
                for (int b = 0; b < dbands; b++) {
                    double sum = 0.0D;
                    double[] mat = matrix[b];

                    for (int k = 0; k < sbands; k++ ) {
                        sum += mat[k] * sData[k][spo+sBandOffsets[k]];
                    }

                    dData[b][dpo+dBandOffsets[b]] = sum + matrix[b][sbands];
                }

                spo += sPixelStride;
                dpo += dPixelStride;
            }

            sso += sLineStride;
            dso += dLineStride;
        }
    }
}
