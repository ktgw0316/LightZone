/*
 * $RCSfile: PolarToComplexOpImage.java,v $
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
import com.lightcrafts.mediax.jai.ImageLayout;
import com.lightcrafts.mediax.jai.PointOpImage;
import com.lightcrafts.mediax.jai.RasterAccessor;
import com.lightcrafts.mediax.jai.RasterFormatTag;
import com.lightcrafts.mediax.jai.RasterFactory;
import java.util.Map;
import com.lightcrafts.media.jai.util.ImageUtil;
import com.lightcrafts.media.jai.util.JDKWorkarounds;

/// XXX Testing
/// import com.lightcrafts.mediax.jai.TiledImage;
/// import com.lightcrafts.mediax.jai.JAI;

/**
 * An <code>OpImage</code> implementing the "PolarToComplex" operation
 * as described in
 * <code>com.lightcrafts.mediax.jai.operator.PolarToComplexDescriptor</code>.
 *
 * <p> The number of bands in the destination image is clamped to twice the
 * minimum number of bands across all source images.  The two source images
 * are expected to be magnitude (first source) and phase (second source).
 * If the phase image is integral, its values are assumed to lie in the
 * range [0, MAX_VALUE] where MAX_VALUE is a functino of the data type.
 * These values will be scaled to the range [-Math.PI, Math.PI] before being
 * used.
 *
 * @since EA4
 *
 * @see com.lightcrafts.mediax.jai.PointOpImage
 * @see com.lightcrafts.mediax.jai.operator.MagnitudeDescriptor
 * @see com.lightcrafts.mediax.jai.operator.PhaseDescriptor
 * @see com.lightcrafts.mediax.jai.operator.PolarToComplexDescriptor
 *
 */
final class PolarToComplexOpImage extends PointOpImage {
    /** The gain to be applied to the phase. */
    private double phaseGain = 1.0;

    /** The bias to be applied to the phase. */
    private double phaseBias = 0.0;
    
    /**
     * Constructs a <code>PolarToComplexOpImage</code> object.
     *
     * <p>The tile grid layout, SampleModel, and ColorModel may optionally
     * be specified by an ImageLayout object.
     *
     * @param magnitude A RenderedImage representing magnitude.
     * @param phase A RenderedImage representing phase.
     * @param layout An ImageLayout optionally containing the tile grid layout,
     * SampleModel, and ColorModel, or null.
     */
    public PolarToComplexOpImage(RenderedImage magnitude,
                                 RenderedImage phase,
                                 Map config,
                                 ImageLayout layout) {
        super(magnitude, phase, layout, config, true);

        // Force the number of bands to be twice the minimum source band count.
        int numBands =
            2*Math.min(magnitude.getSampleModel().getNumBands(),
                       phase.getSampleModel().getNumBands());
        if(sampleModel.getNumBands() != numBands) {
            // Create a new SampleModel for the destination.
            sampleModel =
                RasterFactory.createComponentSampleModel(sampleModel,
                                                 sampleModel.getTransferType(),
                                                 sampleModel.getWidth(),
                                                 sampleModel.getHeight(),
                                                 numBands);

            if(colorModel != null &&
               !JDKWorkarounds.areCompatibleDataModels(sampleModel,
                                                       colorModel)) {
                colorModel = ImageUtil.getCompatibleColorModel(sampleModel,
                                                               config);
            }
        }

        // Set phase gain and bias as a function of the phase image data type.
        switch(phase.getSampleModel().getTransferType()) {
        case DataBuffer.TYPE_BYTE:
            phaseGain = (2.0*Math.PI)/255.0;
            phaseBias = -Math.PI;
            break;
        case DataBuffer.TYPE_SHORT:
            phaseGain = (2.0*Math.PI)/Short.MAX_VALUE;
            phaseBias = -Math.PI;
            break;
        case DataBuffer.TYPE_USHORT:
            phaseGain = (2.0*Math.PI)/(Short.MAX_VALUE - Short.MIN_VALUE);
            phaseBias = -Math.PI;
            break;
        case DataBuffer.TYPE_INT:
            phaseGain = (2.0*Math.PI)/Integer.MAX_VALUE;
            phaseBias = -Math.PI;
            break;
        default:
            // A floating point type: do nothing - use class defaults.
        }

        // TODO: Set "complex" property.
    }

    /*
     * Calculate a complex rectangle given the magnitude and phase.
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

        // Construct RasterAccessors.
        RasterAccessor magAccessor =
            new RasterAccessor(sources[0], destRect, formatTags[0], 
                               getSource(0).getColorModel());
        RasterAccessor phsAccessor =
            new RasterAccessor(sources[1], destRect, formatTags[1], 
                               getSource(1).getColorModel());
        RasterAccessor dstAccessor =
            new RasterAccessor(dest, destRect, formatTags[2], getColorModel());

        // Branch to the method appropriate to the accessor data type.
        switch(dstAccessor.getDataType()) {
        case DataBuffer.TYPE_BYTE:
            computeRectByte(magAccessor, phsAccessor, dstAccessor,
                            destRect.height, destRect.width);
            break;
        case DataBuffer.TYPE_SHORT:
            computeRectShort(magAccessor, phsAccessor, dstAccessor,
                             destRect.height, destRect.width);
            break;
        case DataBuffer.TYPE_USHORT:
            computeRectUShort(magAccessor, phsAccessor, dstAccessor,
                              destRect.height, destRect.width);
            break;
        case DataBuffer.TYPE_INT:
            computeRectInt(magAccessor, phsAccessor, dstAccessor,
                           destRect.height, destRect.width);
            break;
        case DataBuffer.TYPE_FLOAT:
            computeRectFloat(magAccessor, phsAccessor, dstAccessor,
                             destRect.height, destRect.width);
            break;
        case DataBuffer.TYPE_DOUBLE:
            computeRectDouble(magAccessor, phsAccessor, dstAccessor,
                              destRect.height, destRect.width);
            break;
        default:
            // NB: This statement should be unreachable.
            throw new RuntimeException(JaiI18N.getString("PolarToComplexOpImage0"));
        }

        if (dstAccessor.needsClamping()) {
            dstAccessor.clampDataArrays();
        }

        // Make sure that the output data is copied to the destination.
        dstAccessor.copyDataToRaster();
    }

    private void computeRectDouble(RasterAccessor magAccessor,
                                   RasterAccessor phsAccessor,
                                   RasterAccessor dstAccessor,
                                   int numRows,
                                   int numCols) {
        // Set pixel and line strides.
        int dstPixelStride = dstAccessor.getPixelStride();
        int dstScanlineStride = dstAccessor.getScanlineStride();
        int magPixelStride = magAccessor.getPixelStride();
        int magScanlineStride = magAccessor.getScanlineStride();
        int phsPixelStride = phsAccessor.getPixelStride();
        int phsScanlineStride = phsAccessor.getScanlineStride();

        // Loop over the destination components.
        int numComponents = sampleModel.getNumBands()/2;
        for(int component = 0; component < numComponents; component++) {
            // Set source band indices.
            int dstBandReal = 2*component;
            int dstBandImag = dstBandReal + 1;

            // Get the source and destination arrays for this band.
            double[] dstReal = dstAccessor.getDoubleDataArray(dstBandReal);
            double[] dstImag = dstAccessor.getDoubleDataArray(dstBandImag);
            double[] magData = magAccessor.getDoubleDataArray(component);
            double[] phsData = phsAccessor.getDoubleDataArray(component);

            // Initialize the data offsets for this band.
            int dstOffsetReal = dstAccessor.getBandOffset(dstBandReal);
            int dstOffsetImag = dstAccessor.getBandOffset(dstBandImag);
            int magOffset = magAccessor.getBandOffset(component);
            int phsOffset = phsAccessor.getBandOffset(component);

            // Initialize the line offsets for looping.
            int dstLineReal = dstOffsetReal;
            int dstLineImag = dstOffsetImag;
            int magLine = magOffset;
            int phsLine = phsOffset;

            for(int row = 0; row < numRows; row++) {
                // Initialize pixel offsets for this row.
                int dstPixelReal = dstLineReal;
                int dstPixelImag = dstLineImag;
                int magPixel = magLine;
                int phsPixel = phsLine;

                for(int col = 0; col < numCols; col++) {
                    double mag = magData[magPixel];
                    double phs = phsData[phsPixel]*phaseGain + phaseBias;

                    dstReal[dstPixelReal] = mag*Math.cos(phs);
                    dstImag[dstPixelImag] = mag*Math.sin(phs);

                    dstPixelReal += dstPixelStride;
                    dstPixelImag += dstPixelStride;
                    magPixel += magPixelStride;
                    phsPixel += phsPixelStride;
                }

                // Increment the line offsets.
                dstLineReal += dstScanlineStride;
                dstLineImag += dstScanlineStride;
                magLine += magScanlineStride;
                phsLine += phsScanlineStride;
            }
        }
    }

    private void computeRectFloat(RasterAccessor magAccessor,
                                  RasterAccessor phsAccessor,
                                  RasterAccessor dstAccessor,
                                  int numRows,
                                  int numCols) {
        // Set pixel and line strides.
        int dstPixelStride = dstAccessor.getPixelStride();
        int dstScanlineStride = dstAccessor.getScanlineStride();
        int magPixelStride = magAccessor.getPixelStride();
        int magScanlineStride = magAccessor.getScanlineStride();
        int phsPixelStride = phsAccessor.getPixelStride();
        int phsScanlineStride = phsAccessor.getScanlineStride();

        // Loop over the destination components.
        int numComponents = sampleModel.getNumBands()/2;
        for(int component = 0; component < numComponents; component++) {
            // Set source band indices.
            int dstBandReal = 2*component;
            int dstBandImag = dstBandReal + 1;

            // Get the source and destination arrays for this band.
            float[] dstReal = dstAccessor.getFloatDataArray(dstBandReal);
            float[] dstImag = dstAccessor.getFloatDataArray(dstBandImag);
            float[] magData = magAccessor.getFloatDataArray(component);
            float[] phsData = phsAccessor.getFloatDataArray(component);

            // Initialize the data offsets for this band.
            int dstOffsetReal = dstAccessor.getBandOffset(dstBandReal);
            int dstOffsetImag = dstAccessor.getBandOffset(dstBandImag);
            int magOffset = magAccessor.getBandOffset(component);
            int phsOffset = phsAccessor.getBandOffset(component);

            // Initialize the line offsets for looping.
            int dstLineReal = dstOffsetReal;
            int dstLineImag = dstOffsetImag;
            int magLine = magOffset;
            int phsLine = phsOffset;

            for(int row = 0; row < numRows; row++) {
                // Initialize pixel offsets for this row.
                int dstPixelReal = dstLineReal;
                int dstPixelImag = dstLineImag;
                int magPixel = magLine;
                int phsPixel = phsLine;

                for(int col = 0; col < numCols; col++) {
                    double mag = magData[magPixel];
                    double phs = phsData[phsPixel]*phaseGain + phaseBias;

                    dstReal[dstPixelReal] = ImageUtil.clampFloat(mag*Math.cos(phs));
                    dstImag[dstPixelImag] = ImageUtil.clampFloat(mag*Math.sin(phs));

                    dstPixelReal += dstPixelStride;
                    dstPixelImag += dstPixelStride;
                    magPixel += magPixelStride;
                    phsPixel += phsPixelStride;
                }

                // Increment the line offsets.
                dstLineReal += dstScanlineStride;
                dstLineImag += dstScanlineStride;
                magLine += magScanlineStride;
                phsLine += phsScanlineStride;
            }
        }
    }

    private void computeRectInt(RasterAccessor magAccessor,
                                RasterAccessor phsAccessor,
                                RasterAccessor dstAccessor,
                                int numRows,
                                int numCols) {
        // Set pixel and line strides.
        int dstPixelStride = dstAccessor.getPixelStride();
        int dstScanlineStride = dstAccessor.getScanlineStride();
        int magPixelStride = magAccessor.getPixelStride();
        int magScanlineStride = magAccessor.getScanlineStride();
        int phsPixelStride = phsAccessor.getPixelStride();
        int phsScanlineStride = phsAccessor.getScanlineStride();

        // Loop over the destination components.
        int numComponents = sampleModel.getNumBands()/2;
        for(int component = 0; component < numComponents; component++) {
            // Set source band indices.
            int dstBandReal = 2*component;
            int dstBandImag = dstBandReal + 1;

            // Get the source and destination arrays for this band.
            int[] dstReal = dstAccessor.getIntDataArray(dstBandReal);
            int[] dstImag = dstAccessor.getIntDataArray(dstBandImag);
            int[] magData = magAccessor.getIntDataArray(component);
            int[] phsData = phsAccessor.getIntDataArray(component);

            // Initialize the data offsets for this band.
            int dstOffsetReal = dstAccessor.getBandOffset(dstBandReal);
            int dstOffsetImag = dstAccessor.getBandOffset(dstBandImag);
            int magOffset = magAccessor.getBandOffset(component);
            int phsOffset = phsAccessor.getBandOffset(component);

            // Initialize the line offsets for looping.
            int dstLineReal = dstOffsetReal;
            int dstLineImag = dstOffsetImag;
            int magLine = magOffset;
            int phsLine = phsOffset;

            for(int row = 0; row < numRows; row++) {
                // Initialize pixel offsets for this row.
                int dstPixelReal = dstLineReal;
                int dstPixelImag = dstLineImag;
                int magPixel = magLine;
                int phsPixel = phsLine;

                for(int col = 0; col < numCols; col++) {
                    double mag = magData[magPixel];
                    double phs = phsData[phsPixel]*phaseGain + phaseBias;

                    dstReal[dstPixelReal] = ImageUtil.clampRoundInt(mag*Math.cos(phs));
                    dstImag[dstPixelImag] = ImageUtil.clampRoundInt(mag*Math.sin(phs));

                    dstPixelReal += dstPixelStride;
                    dstPixelImag += dstPixelStride;
                    magPixel += magPixelStride;
                    phsPixel += phsPixelStride;
                }

                // Increment the line offsets.
                dstLineReal += dstScanlineStride;
                dstLineImag += dstScanlineStride;
                magLine += magScanlineStride;
                phsLine += phsScanlineStride;
            }
        }
    }

    private void computeRectUShort(RasterAccessor magAccessor,
                                   RasterAccessor phsAccessor,
                                   RasterAccessor dstAccessor,
                                   int numRows,
                                   int numCols) {
        // Set pixel and line strides.
        int dstPixelStride = dstAccessor.getPixelStride();
        int dstScanlineStride = dstAccessor.getScanlineStride();
        int magPixelStride = magAccessor.getPixelStride();
        int magScanlineStride = magAccessor.getScanlineStride();
        int phsPixelStride = phsAccessor.getPixelStride();
        int phsScanlineStride = phsAccessor.getScanlineStride();

        // Loop over the destination components.
        int numComponents = sampleModel.getNumBands()/2;
        for(int component = 0; component < numComponents; component++) {
            // Set source band indices.
            int dstBandReal = 2*component;
            int dstBandImag = dstBandReal + 1;

            // Get the source and destination arrays for this band.
            short[] dstReal = dstAccessor.getShortDataArray(dstBandReal);
            short[] dstImag = dstAccessor.getShortDataArray(dstBandImag);
            short[] magData = magAccessor.getShortDataArray(component);
            short[] phsData = phsAccessor.getShortDataArray(component);

            // Initialize the data offsets for this band.
            int dstOffsetReal = dstAccessor.getBandOffset(dstBandReal);
            int dstOffsetImag = dstAccessor.getBandOffset(dstBandImag);
            int magOffset = magAccessor.getBandOffset(component);
            int phsOffset = phsAccessor.getBandOffset(component);

            // Initialize the line offsets for looping.
            int dstLineReal = dstOffsetReal;
            int dstLineImag = dstOffsetImag;
            int magLine = magOffset;
            int phsLine = phsOffset;

            for(int row = 0; row < numRows; row++) {
                // Initialize pixel offsets for this row.
                int dstPixelReal = dstLineReal;
                int dstPixelImag = dstLineImag;
                int magPixel = magLine;
                int phsPixel = phsLine;

                for(int col = 0; col < numCols; col++) {
                    double mag = magData[magPixel]&0xffff;
                    double phs =
                        (phsData[phsPixel]&0xffff)*phaseGain + phaseBias;

                    dstReal[dstPixelReal] =
                        ImageUtil.clampRoundUShort(mag*Math.cos(phs));
                    dstImag[dstPixelImag] =
                        ImageUtil.clampRoundUShort(mag*Math.sin(phs));

                    dstPixelReal += dstPixelStride;
                    dstPixelImag += dstPixelStride;
                    magPixel += magPixelStride;
                    phsPixel += phsPixelStride;
                }

                // Increment the line offsets.
                dstLineReal += dstScanlineStride;
                dstLineImag += dstScanlineStride;
                magLine += magScanlineStride;
                phsLine += phsScanlineStride;
            }
        }
    }

    private void computeRectShort(RasterAccessor magAccessor,
                                  RasterAccessor phsAccessor,
                                  RasterAccessor dstAccessor,
                                  int numRows,
                                  int numCols) {
        // Set pixel and line strides.
        int dstPixelStride = dstAccessor.getPixelStride();
        int dstScanlineStride = dstAccessor.getScanlineStride();
        int magPixelStride = magAccessor.getPixelStride();
        int magScanlineStride = magAccessor.getScanlineStride();
        int phsPixelStride = phsAccessor.getPixelStride();
        int phsScanlineStride = phsAccessor.getScanlineStride();

        // Loop over the destination components.
        int numComponents = sampleModel.getNumBands()/2;
        for(int component = 0; component < numComponents; component++) {
            // Set source band indices.
            int dstBandReal = 2*component;
            int dstBandImag = dstBandReal + 1;

            // Get the source and destination arrays for this band.
            short[] dstReal = dstAccessor.getShortDataArray(dstBandReal);
            short[] dstImag = dstAccessor.getShortDataArray(dstBandImag);
            short[] magData = magAccessor.getShortDataArray(component);
            short[] phsData = phsAccessor.getShortDataArray(component);

            // Initialize the data offsets for this band.
            int dstOffsetReal = dstAccessor.getBandOffset(dstBandReal);
            int dstOffsetImag = dstAccessor.getBandOffset(dstBandImag);
            int magOffset = magAccessor.getBandOffset(component);
            int phsOffset = phsAccessor.getBandOffset(component);

            // Initialize the line offsets for looping.
            int dstLineReal = dstOffsetReal;
            int dstLineImag = dstOffsetImag;
            int magLine = magOffset;
            int phsLine = phsOffset;

            for(int row = 0; row < numRows; row++) {
                // Initialize pixel offsets for this row.
                int dstPixelReal = dstLineReal;
                int dstPixelImag = dstLineImag;
                int magPixel = magLine;
                int phsPixel = phsLine;

                for(int col = 0; col < numCols; col++) {
                    double mag = magData[magPixel];
                    double phs = phsData[phsPixel]*phaseGain + phaseBias;

                    dstReal[dstPixelReal] = ImageUtil.clampRoundShort(mag*Math.cos(phs));
                    dstImag[dstPixelImag] = ImageUtil.clampRoundShort(mag*Math.sin(phs));

                    dstPixelReal += dstPixelStride;
                    dstPixelImag += dstPixelStride;
                    magPixel += magPixelStride;
                    phsPixel += phsPixelStride;
                }

                // Increment the line offsets.
                dstLineReal += dstScanlineStride;
                dstLineImag += dstScanlineStride;
                magLine += magScanlineStride;
                phsLine += phsScanlineStride;
            }
        }
    }

    private void computeRectByte(RasterAccessor magAccessor,
                                 RasterAccessor phsAccessor,
                                 RasterAccessor dstAccessor,
                                 int numRows,
                                 int numCols) {
        // Set pixel and line strides.
        int dstPixelStride = dstAccessor.getPixelStride();
        int dstScanlineStride = dstAccessor.getScanlineStride();
        int magPixelStride = magAccessor.getPixelStride();
        int magScanlineStride = magAccessor.getScanlineStride();
        int phsPixelStride = phsAccessor.getPixelStride();
        int phsScanlineStride = phsAccessor.getScanlineStride();

        // Loop over the destination components.
        int numComponents = sampleModel.getNumBands()/2;
        for(int component = 0; component < numComponents; component++) {
            // Set source band indices.
            int dstBandReal = 2*component;
            int dstBandImag = dstBandReal + 1;

            // Get the source and destination arrays for this band.
            byte[] dstReal = dstAccessor.getByteDataArray(dstBandReal);
            byte[] dstImag = dstAccessor.getByteDataArray(dstBandImag);
            byte[] magData = magAccessor.getByteDataArray(component);
            byte[] phsData = phsAccessor.getByteDataArray(component);

            // Initialize the data offsets for this band.
            int dstOffsetReal = dstAccessor.getBandOffset(dstBandReal);
            int dstOffsetImag = dstAccessor.getBandOffset(dstBandImag);
            int magOffset = magAccessor.getBandOffset(component);
            int phsOffset = phsAccessor.getBandOffset(component);

            // Initialize the line offsets for looping.
            int dstLineReal = dstOffsetReal;
            int dstLineImag = dstOffsetImag;
            int magLine = magOffset;
            int phsLine = phsOffset;

            for(int row = 0; row < numRows; row++) {
                // Initialize pixel offsets for this row.
                int dstPixelReal = dstLineReal;
                int dstPixelImag = dstLineImag;
                int magPixel = magLine;
                int phsPixel = phsLine;

                for(int col = 0; col < numCols; col++) {
                    double mag = magData[magPixel]&0xff;
                    double phs =
                        (phsData[phsPixel]&0xff)*phaseGain + phaseBias;

                    dstReal[dstPixelReal] = ImageUtil.clampRoundByte(mag*Math.cos(phs));
                    dstImag[dstPixelImag] = ImageUtil.clampRoundByte(mag*Math.sin(phs));

                    dstPixelReal += dstPixelStride;
                    dstPixelImag += dstPixelStride;
                    magPixel += magPixelStride;
                    phsPixel += phsPixelStride;
                }

                // Increment the line offsets.
                dstLineReal += dstScanlineStride;
                dstLineImag += dstScanlineStride;
                magLine += magScanlineStride;
                phsLine += phsScanlineStride;
            }
        }
    }
}
