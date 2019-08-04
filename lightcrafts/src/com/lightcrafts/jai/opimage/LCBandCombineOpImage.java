/* Copyright (C) 2005-2011 Fabio Riccardi */

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
package com.lightcrafts.jai.opimage;

import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.util.Map;
import javax.media.jai.ImageLayout;
import javax.media.jai.PointOpImage;
import javax.media.jai.RasterAccessor;
import javax.media.jai.RasterFormatTag;
import javax.media.jai.RasterFactory;
import com.sun.media.jai.util.ImageUtil;
import com.sun.media.jai.util.JDKWorkarounds;

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
 * @see javax.media.jai.operator.BandCombineDescriptor
 * @see LCBandCombineCRIF
 *
 *
 * @since EA3
 */
final class LCBandCombineOpImage extends PointOpImage {

    private double[][] matrix;

    /**
     * Constructor.
     *
     * @param source       The source image.
     * @param layout       The destination image layout.
     * @param matrix       The matrix of values used to perform the
     *                     linear combination.
     */
    public LCBandCombineOpImage(RenderedImage source,
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

        if (sbands > 1 && sPixelStride > 1 && (dbands == 1 || (dbands > 1 && dPixelStride > 1))) {
            byte[] ddData = dData[0];
            byte[] ssData = sData[0];

            int rows = matrix.length;
            int cols = matrix[0].length;
            int[][] sortedMatrix = new int[rows][cols];

            int[] bandOffsets = src.getOffsetsForBands();

            int minBandOffset = sBandOffsets[0];
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols - 1; j++) {
                    sortedMatrix[i][j] = (int) (matrix[i][j < cols - 1 ? bandOffsets[j] : j] * 0x100 + 0.5);
                    if (j < cols - 1)
                        minBandOffset = sBandOffsets[j] < minBandOffset ? sBandOffsets[j] : minBandOffset;
                }
            }

            int sso = minBandOffset, dso = 0;

            if (sbands == 3 && dbands == 3) {
                for (int h = 0; h < dheight; h++) {
                    int spo = sso;
                    int dpo = dso;

                    for (int w = 0; w < dwidth; w++) {
                        int[] mat = sortedMatrix[0];
                        int sum = mat[0] * (int) (ssData[spo + 0] & 0xFF) +
                                  mat[1] * (int) (ssData[spo + 1] & 0xFF) +
                                  mat[2] * (int) (ssData[spo + 2] & 0xFF);
                        ddData[dpo + dBandOffsets[0]] = ImageUtil.clampByte(sum / 0x100 + mat[3]);

                        mat = sortedMatrix[1];
                        sum = mat[0] * (int) (ssData[spo + 0] & 0xFF) +
                              mat[1] * (int) (ssData[spo + 1] & 0xFF) +
                              mat[2] * (int) (ssData[spo + 2] & 0xFF);
                        ddData[dpo + dBandOffsets[1]] = ImageUtil.clampByte(sum / 0x100 + mat[3]);

                        mat = sortedMatrix[2];
                        sum = mat[0] * (int) (ssData[spo + 0] & 0xFF) +
                              mat[1] * (int) (ssData[spo + 1] & 0xFF) +
                              mat[2] * (int) (ssData[spo + 2] & 0xFF);
                        ddData[dpo + dBandOffsets[2]] = ImageUtil.clampByte(sum / 0x100 + mat[3]);

                        spo += sPixelStride;
                        dpo += dPixelStride;
                    }

                    sso += sLineStride;
                    dso += dLineStride;
                }
            } else if (sbands == 3 && dbands == 1) {
                for (int h = 0; h < dheight; h++) {
                    int spo = sso;
                    int dpo = dso;

                    for (int w = 0; w < dwidth; w++) {
                        int[] mat = sortedMatrix[0];
                        int sum = mat[0] * (int) (ssData[spo + 0] & 0xFF) +
                                  mat[1] * (int) (ssData[spo + 1] & 0xFF) +
                                  mat[2] * (int) (ssData[spo + 2] & 0xFF);
                        ddData[dpo + dBandOffsets[0]] = ImageUtil.clampByte(sum / 0x100 + mat[3]);

                        spo += sPixelStride;
                        dpo += dPixelStride;
                    }

                    sso += sLineStride;
                    dso += dLineStride;
                }
            } else {
                for (int h = 0; h < dheight; h++) {
                    int spo = sso;
                    int dpo = dso;

                    for (int w = 0; w < dwidth; w++) {
                        for (int b = 0; b < dbands; b++) {
                            int sum = 0;
                            int[] mat = sortedMatrix[b];

                            for (int k = 0; k < sbands; k++ ) {
                                sum += (mat[k] * (int)(ssData[spo+k] & 0xFF)) / 0x100;
                            }

                            ddData[dpo+dBandOffsets[b]] = ImageUtil.clampByte(sum + mat[sbands]);
                        }
                        spo += sPixelStride;
                        dpo += dPixelStride;
                    }

                    sso += sLineStride;
                    dso += dLineStride;
                }
            }
        } else {
            int rows = matrix.length;
            int cols = matrix[0].length;
            int[][] intMatrix = new int[rows][cols];

            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols - 1; j++) {
                    intMatrix[i][j] = (int) (matrix[i][j] * 0x100 + 0.5);
                }
            }

            int sso = 0, dso = 0;

            for (int h = 0; h < dheight; h++) {
                int spo = sso;
                int dpo = dso;

                for (int w = 0; w < dwidth; w++) {
                    for (int b = 0; b < dbands; b++) {
                        int sum = 0;
                        int[] mat = intMatrix[b];

                        for (int k = 0; k < sbands; k++) {
                            sum += (mat[k] * (int) (sData[k][spo + sBandOffsets[k]] & 0xFF)) / 0x100;
                        }

                        dData[b][dpo + dBandOffsets[b]] = ImageUtil.clampByte(sum + mat[sbands]);
                    }

                    spo += sPixelStride;
                    dpo += dPixelStride;
                }

                sso += sLineStride;
                dso += dLineStride;
            }
        }
    }

    private void computeRectUShort(RasterAccessor src, RasterAccessor dst) {
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

        if (sbands > 1 && sPixelStride > 1 && (dbands == 1 || (dbands > 1 && dPixelStride > 1))) {
            short[] ddData = dData[0];
            short[] ssData = sData[0];

            int rows = matrix.length;
            int cols = matrix[0].length;
            int[][] sortedMatrix = new int[rows][cols];

            int minBandOffset = sBandOffsets[0];
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols - 1; j++) {
                    sortedMatrix[i][j] = (int) (matrix[i][j < cols - 1 ? sBandOffsets[j] : j] * 0x10000 + 0.5);
                    if (j < cols - 1)
                        minBandOffset = sBandOffsets[j] < minBandOffset ? sBandOffsets[j] : minBandOffset;
                }
            }

            int sso = minBandOffset, dso = 0;

            if (sbands == 3 && dbands == 3) {
                for (int h = 0; h < dheight; h++) {
                    int spo = sso;
                    int dpo = dso;

                    for (int w = 0; w < dwidth; w++) {
                        int[] mat = sortedMatrix[0];
                        long sum = mat[0] * (long) (ssData[spo + 0] & 0xFFFF) +
                                   mat[1] * (long) (ssData[spo + 1] & 0xFFFF) +
                                   mat[2] * (long) (ssData[spo + 2] & 0xFFFF);
                        ddData[dpo + dBandOffsets[0]] = ImageUtil.clampUShort((int) (sum / 0x10000 + mat[3]));

                        mat = sortedMatrix[1];
                        sum = mat[0] * (long) (ssData[spo + 0] & 0xFFFF) +
                              mat[1] * (long) (ssData[spo + 1] & 0xFFFF) +
                              mat[2] * (long) (ssData[spo + 2] & 0xFFFF);
                        ddData[dpo + dBandOffsets[1]] = ImageUtil.clampUShort((int) (sum / 0x10000 + mat[3]));

                        mat = sortedMatrix[2];
                        sum = mat[0] * (long) (ssData[spo + 0] & 0xFFFF) +
                              mat[1] * (long) (ssData[spo + 1] & 0xFFFF) +
                              mat[2] * (long) (ssData[spo + 2] & 0xFFFF);
                        ddData[dpo + dBandOffsets[2]] = ImageUtil.clampUShort((int) (sum / 0x10000 + mat[3]));

                        spo += sPixelStride;
                        dpo += dPixelStride;
                    }

                    sso += sLineStride;
                    dso += dLineStride;
                }
            } else if (sbands == 3 && dbands == 1) {
                for (int h = 0; h < dheight; h++) {
                    int spo = sso;
                    int dpo = dso;

                    for (int w = 0; w < dwidth; w++) {
                        int[] mat = sortedMatrix[0];
                        long sum = mat[0] * (long) (ssData[spo + 0] & 0xFFFF) +
                                   mat[1] * (long) (ssData[spo + 1] & 0xFFFF) +
                                   mat[2] * (long) (ssData[spo + 2] & 0xFFFF);
                        ddData[dpo + dBandOffsets[0]] = ImageUtil.clampUShort((int) (sum / 0x10000 + mat[3]));

                        spo += sPixelStride;
                        dpo += dPixelStride;
                    }

                    sso += sLineStride;
                    dso += dLineStride;
                }
            } else {
                for (int h = 0; h < dheight; h++) {
                    int spo = sso;
                    int dpo = dso;

                    for (int w = 0; w < dwidth; w++) {
                        for (int b = 0; b < dbands; b++) {
                            long sum = 0;
                            int[] mat = sortedMatrix[b];

                            for (int k = 0; k < sbands; k++ ) {
                                sum += (mat[k] * (long)(ssData[spo+k] & 0xFFFF)) / 0x10000;
                            }

                            ddData[dpo+dBandOffsets[b]] = ImageUtil.clampUShort((int) (sum + mat[sbands]));
                        }
                        spo += sPixelStride;
                        dpo += dPixelStride;
                    }

                    sso += sLineStride;
                    dso += dLineStride;
                }
            }
        } else {
            int rows = matrix.length;
            int cols = matrix[0].length;
            int[][] intMatrix = new int[rows][cols];

            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols - 1; j++) {
                    intMatrix[i][j] = (int) (matrix[i][j] * 0x10000 + 0.5);
                }
            }

            int sso = 0, dso = 0;

            for (int h = 0; h < dheight; h++) {
                int spo = sso;
                int dpo = dso;

                for (int w = 0; w < dwidth; w++) {
                    for (int b = 0; b < dbands; b++) {
                        long sum = 0;
                        int[] mat = intMatrix[b];

                        for (int k = 0; k < sbands; k++ ) {
                            sum += (mat[k] * (long) (sData[k][spo+sBandOffsets[k]] & 0xFFFF)) / 0x10000;
                        }

                        dData[b][dpo+dBandOffsets[b]] = ImageUtil.clampUShort((int) (sum + mat[sbands]));
                    }

                    spo += sPixelStride;
                    dpo += dPixelStride;
                }

                sso += sLineStride;
                dso += dLineStride;
            }
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
                    double sum = 0.0F;
                    double[] mat = matrix[b];

                    for (int k = 0; k < sbands; k++ ) {
                        sum += mat[k] * sData[k][spo+sBandOffsets[k]];
                    }

                    dData[b][dpo+dBandOffsets[b]] = ImageUtil.clampUShort((int) (sum + mat[sbands]));
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
                    double sum = 0.0F;
                    double[] mat = matrix[b];

                    for (int k = 0; k < sbands; k++ ) {
                        sum += mat[k] * sData[k][spo+sBandOffsets[k]];
                    }

                    dData[b][dpo+dBandOffsets[b]] = ImageUtil.clampRoundInt(sum + mat[sbands]);
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
                    double sum = 0.0F;
                    double[] mat = matrix[b];

                    for (int k = 0; k < sbands; k++ ) {
                        sum += mat[k] * sData[k][spo+sBandOffsets[k]];
                    }

                    dData[b][dpo+dBandOffsets[b]] = (float) (sum + mat[sbands]);
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

                    dData[b][dpo+dBandOffsets[b]] = sum + mat[sbands];
                }

                spo += sPixelStride;
                dpo += dPixelStride;
            }

            sso += sLineStride;
            dso += dLineStride;
        }
    }
}
