/*
 * $RCSfile: MosaicOpImage.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.2 $
 * $Date: 2005/02/23 21:02:26 $
 * $State: Exp $
 */package com.lightcrafts.media.jai.opimage;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Vector;
import com.lightcrafts.mediax.jai.BorderExtender;
import com.lightcrafts.mediax.jai.BorderExtenderConstant;
import com.lightcrafts.mediax.jai.ImageLayout;
import com.lightcrafts.mediax.jai.OpImage;
import com.lightcrafts.mediax.jai.PlanarImage;
import com.lightcrafts.mediax.jai.RasterAccessor;
import com.lightcrafts.mediax.jai.RasterFormatTag;
import com.lightcrafts.mediax.jai.ROI;
import com.lightcrafts.mediax.jai.operator.MosaicType;
import com.lightcrafts.mediax.jai.operator.MosaicDescriptor;
import com.lightcrafts.media.jai.util.ImageUtil;

public class MosaicOpImage extends OpImage {
    private static final int WEIGHT_TYPE_ALPHA = 1;
    private static final int WEIGHT_TYPE_ROI = 2;
    private static final int WEIGHT_TYPE_THRESHOLD = 3;

    protected MosaicType mosaicType;
    protected PlanarImage[] sourceAlpha;
    protected ROI[] sourceROI;
    protected double[][] sourceThreshold;
    protected double[] backgroundValues;

    protected int numBands;

    /** Integral background value. */
    protected int[] background;

    /** Integral thresholds. */
    protected int[][] threshold;

    protected boolean isAlphaBitmask = false;

    private BorderExtender sourceExtender;
    private BorderExtender zeroExtender;
    private PlanarImage[] roiImage;

    private static final ImageLayout getLayout(Vector sources,
                                               ImageLayout layout) {

        // Contingent variables.
        RenderedImage source0 = null;
        SampleModel targetSM = null;
        ColorModel targetCM = null;

        // Get source count (might be zero).
        int numSources = sources.size();

        if(numSources > 0) {
            // Get SampleModel and ColorModel from first source.
            source0 = (RenderedImage)sources.get(0);
            targetSM = source0.getSampleModel();
            targetCM = source0.getColorModel();
        } else if(layout != null &&
                  layout.isValid(ImageLayout.WIDTH_MASK |
                                 ImageLayout.HEIGHT_MASK |
                                 ImageLayout.SAMPLE_MODEL_MASK)) {
            // Get SampleModel and ColorModel from layout.
            targetSM = layout.getSampleModel(null);
            if(targetSM == null) {
                throw new IllegalArgumentException
                    (JaiI18N.getString("MosaicOpImage7"));
            }
        } else {
            throw new IllegalArgumentException
                (JaiI18N.getString("MosaicOpImage8"));
        }

        // Get data type, band count, and sample depth.
        int dataType = targetSM.getDataType();
        int numBands = targetSM.getNumBands();
        int sampleSize = targetSM.getSampleSize(0);

        // Sample size must equal that of first band.
        for(int i = 1; i < numBands; i++) {
            if(targetSM.getSampleSize(i) != sampleSize) {
                throw new IllegalArgumentException(JaiI18N.getString("MosaicOpImage1"));
            }
        }

        // Return a clone of ImageLayout passed in if no sources.
        // The input ImageLayout was checked for null above.
        if(numSources < 1) {
            return (ImageLayout)layout.clone();
        }

        // Check the other sources against the first.
        for(int i = 1; i < numSources; i++) {
            RenderedImage source = (RenderedImage)sources.get(i);
            SampleModel sourceSM = source.getSampleModel();

            // Data type and band count must be equal.
            if(sourceSM.getDataType() != dataType) {
                throw new IllegalArgumentException(JaiI18N.getString("MosaicOpImage2"));
            } else if(sourceSM.getNumBands() != numBands) {
                throw new IllegalArgumentException(JaiI18N.getString("MosaicOpImage3"));
            }

            // Sample size must be equal.
            for(int j = 0; j < numBands; j++) {
                if(sourceSM.getSampleSize(j) != sampleSize) {
                    throw new IllegalArgumentException(JaiI18N.getString("MosaicOpImage1"));
                }
            }
        }

        // Create a new layout or clone the one passed in.
        ImageLayout mosaicLayout = layout == null ?
            new ImageLayout() : (ImageLayout)layout.clone();

        // Determine the mosaic bounds.
        Rectangle mosaicBounds = new Rectangle();
        if(mosaicLayout.isValid(ImageLayout.MIN_X_MASK |
                                ImageLayout.MIN_Y_MASK |
                                ImageLayout.WIDTH_MASK |
                                ImageLayout.HEIGHT_MASK)) {
            // Set the mosaic bounds to the value given in the layout.
            mosaicBounds.setBounds(mosaicLayout.getMinX(null),
                                   mosaicLayout.getMinY(null),
                                   mosaicLayout.getWidth(null),
                                   mosaicLayout.getHeight(null));
        } else if(numSources > 0) {
            // Set the mosaic bounds to the union of source bounds.
            mosaicBounds.setBounds(source0.getMinX(), source0.getMinY(),
                                   source0.getWidth(), source0.getHeight());
            for(int i = 1; i < numSources; i++) {
                RenderedImage source = (RenderedImage)sources.get(i);
                Rectangle sourceBounds =
                    new Rectangle(source.getMinX(), source.getMinY(),
                                  source.getWidth(), source.getHeight());
                mosaicBounds = mosaicBounds.union(sourceBounds);
            }
        }

        // Set the mosaic bounds in the layout.
        mosaicLayout.setMinX(mosaicBounds.x);
        mosaicLayout.setMinY(mosaicBounds.y);
        mosaicLayout.setWidth(mosaicBounds.width);
        mosaicLayout.setHeight(mosaicBounds.height);

        // Check the SampleModel if defined.
        if(mosaicLayout.isValid(ImageLayout.SAMPLE_MODEL_MASK)) {
            // Get the SampleModel.
            SampleModel destSM = mosaicLayout.getSampleModel(null);

            // Unset SampleModel if differing band count or data type.
            boolean unsetSampleModel =
                destSM.getNumBands() != numBands ||
                destSM.getDataType() != dataType;

            // Unset SampleModel if differing sample size.
            for(int i = 0; !unsetSampleModel && i < numBands; i++) {
                if(destSM.getSampleSize(i) != sampleSize) {
                    unsetSampleModel = true;
                }
            }

            // Unset SampleModel if needed.
            if(unsetSampleModel) {
                mosaicLayout.unsetValid(ImageLayout.SAMPLE_MODEL_MASK);
            }
        }

        return mosaicLayout;
    }

    public MosaicOpImage(Vector sources,
                         ImageLayout layout,
                         Map config,
                         MosaicType mosaicType,
                         PlanarImage[] sourceAlpha,
                         ROI[] sourceROI,
                         double[][] sourceThreshold,
                         double[] backgroundValues) {
        super(sources, getLayout(sources, layout), config, true);

        // Set the band count.
        this.numBands = sampleModel.getNumBands();

        // Set the source count.
        int numSources = getNumSources();

        // Set the mosaic type.
        this.mosaicType = mosaicType;

        // Save the alpha array.
        this.sourceAlpha = null;
        if(sourceAlpha != null) {
            // Check alpha images.
            for(int i = 0; i < sourceAlpha.length; i++) {
                if(sourceAlpha[i] != null) {
                    SampleModel alphaSM = sourceAlpha[i].getSampleModel();

                    if(alphaSM.getNumBands() != 1) {
                        throw new IllegalArgumentException(JaiI18N.getString("MosaicOpImage4"));
                    } else if(alphaSM.getDataType() !=
                              sampleModel.getDataType()) {
                        throw new IllegalArgumentException(JaiI18N.getString("MosaicOpImage5"));
                    } else if(alphaSM.getSampleSize(0) !=
                              sampleModel.getSampleSize(0)) {
                        throw new IllegalArgumentException(JaiI18N.getString("MosaicOpImage6"));
                    }
                }
            }

            this.sourceAlpha = new PlanarImage[numSources];
            System.arraycopy(sourceAlpha, 0,
                             this.sourceAlpha, 0,
                             Math.min(sourceAlpha.length, numSources));
        }

        // Save the ROI array.
        this.sourceROI = null;
        if(sourceROI != null) {
            this.sourceROI = new ROI[numSources];
            System.arraycopy(sourceROI, 0,
                             this.sourceROI, 0,
                             Math.min(sourceROI.length, numSources));
        }

        // isAlphaBitmask is true if and only if type is blend and an
        // alpha image is supplied for each source.
        this.isAlphaBitmask =
            !(mosaicType == MosaicDescriptor.MOSAIC_TYPE_BLEND &&
              sourceAlpha != null && !(sourceAlpha.length < numSources));
        if(!this.isAlphaBitmask) {
            for(int i = 0; i < numSources; i++) {
                if(sourceAlpha[i] == null) {
                    this.isAlphaBitmask = true;
                    break;
                }
            }
        }

        // Copy the threshold values according to the specification.
        this.sourceThreshold = new double[numSources][numBands];

        // Ensure the parameter is non-null and has one value.
        if(sourceThreshold == null) {
            sourceThreshold = new double[][] {{1.0}};
        }
        for(int i = 0; i < numSources; i++) {
            // If there's an array for this source, use it.
            if(i < sourceThreshold.length && sourceThreshold[i] != null) {
                if(sourceThreshold[i].length < numBands) {
                    // If the array is less than numBands, fill with element 0.
                    Arrays.fill(this.sourceThreshold[i],
                                sourceThreshold[i][0]);
                } else {
                    // Copy the whole array.
                    System.arraycopy(sourceThreshold[i], 0,
                                     this.sourceThreshold[i], 0,
                                     numBands);
                }
            } else {
                // Beyond the array or a null element: use the zeroth array.
                this.sourceThreshold[i] = this.sourceThreshold[0];
            }
        }

        // Initialize the integral thresholds.
        this.threshold = new int[numSources][numBands];
        for(int i = 0; i < numSources; i++) {
            for(int j = 0; j < numBands; j++) {
                // Truncate as the specified comparison is ">=".
                this.threshold[i][j] = (int)this.sourceThreshold[i][j];
            }
        }

        // Copy the background values per the specification.
        this.backgroundValues = new double[numBands];
        if(backgroundValues == null) {
            backgroundValues = new double[] {0.0};
        }
        if(backgroundValues.length < numBands) {
            Arrays.fill(this.backgroundValues, backgroundValues[0]);
        } else {
            System.arraycopy(backgroundValues, 0,
                             this.backgroundValues, 0,
                             numBands);
        }

        // Clamp the floating point values for the integral cases.
        this.background = new int[this.backgroundValues.length];
        int dataType = sampleModel.getDataType();
        for(int i = 0; i < this.background.length; i++) {
            switch (dataType) {
            case DataBuffer.TYPE_BYTE:
                this.background[i] =
                    ImageUtil.clampRoundByte(this.backgroundValues[i]);
                break;
            case DataBuffer.TYPE_USHORT:
                this.background[i] =
                    ImageUtil.clampRoundUShort(this.backgroundValues[i]);
                break;
            case DataBuffer.TYPE_SHORT:
                this.background[i] =
                    ImageUtil.clampRoundShort(this.backgroundValues[i]);
                break;
            case DataBuffer.TYPE_INT:
                this.background[i] =
                    ImageUtil.clampRoundInt(this.backgroundValues[i]);
                break;
            default:
            }
        }

        // Determine constant value for source border extension.
        double sourceExtensionConstant;
        switch (dataType) {
        case DataBuffer.TYPE_BYTE:
            sourceExtensionConstant = 0.0;
            break;
        case DataBuffer.TYPE_USHORT:
            sourceExtensionConstant = 0.0;
            break;
        case DataBuffer.TYPE_SHORT:
            sourceExtensionConstant = Short.MIN_VALUE;
            break;
        case DataBuffer.TYPE_INT:
            sourceExtensionConstant = Integer.MIN_VALUE;
            break;
        case DataBuffer.TYPE_FLOAT:
            sourceExtensionConstant = -Float.MAX_VALUE;
            break;
        case DataBuffer.TYPE_DOUBLE:
        default:
            sourceExtensionConstant = -Double.MAX_VALUE;
        }

        // Extend the sources filling with the minimum possible value
        // on account of the threshold technique.
        this.sourceExtender =
            sourceExtensionConstant == 0.0 ?
            BorderExtender.createInstance(BorderExtender.BORDER_ZERO) :
            new BorderExtenderConstant(new double[] {sourceExtensionConstant});

        // Extends alpha or ROI data with zeros.
        if(sourceAlpha != null || sourceROI != null) {
            this.zeroExtender =
                BorderExtender.createInstance(BorderExtender.BORDER_ZERO);
        }

        // Get the ROI images.
        if(sourceROI != null) {
            roiImage = new PlanarImage[numSources];
            for(int i = 0; i < sourceROI.length; i++) {
                if(sourceROI[i] != null) {
                    roiImage[i] = sourceROI[i].getAsImage();
                }
            }
        }
    }

    public Rectangle mapDestRect(Rectangle destRect,
                                 int sourceIndex) {
        if(destRect == null) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        if(sourceIndex < 0 || sourceIndex >= getNumSources()) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic1"));
        }

        return destRect.intersection(getSourceImage(sourceIndex).getBounds());
    }

    public Rectangle mapSourceRect(Rectangle sourceRect,
                                   int sourceIndex) {
        if(sourceRect == null) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        if(sourceIndex < 0 || sourceIndex >= getNumSources()) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic1"));
        }

        return sourceRect.intersection(getBounds());
    }

    public Raster computeTile(int tileX, int tileY) {
        // Create a new Raster.
        WritableRaster dest = createWritableRaster(sampleModel,
                                                   new Point(tileXToX(tileX),
                                                             tileYToY(tileY)));

        // Determine the active area; tile intersects with image's bounds.
        Rectangle destRect = getTileRect(tileX, tileY);

        int numSources = getNumSources();

        Raster[] rasterSources = new Raster[numSources];
        Raster[] alpha = sourceAlpha != null ?
            new Raster[numSources] : null;
        Raster[] roi = sourceROI != null ?
            new Raster[numSources] : null;

        // Cobble areas
        for (int i = 0; i < numSources; i++) {
            PlanarImage source = getSourceImage(i);
            Rectangle srcRect = mapDestRect(destRect, i);

            // If srcRect is empty, set the Raster for this source to
            // null; otherwise pass srcRect to getData(). If srcRect
            // is null, getData() will return a Raster containing the
            // data of the entire source image.
            rasterSources[i] = srcRect != null && srcRect.isEmpty() ?
                null : source.getExtendedData(destRect, sourceExtender);

            if(rasterSources[i] != null) {
                if(sourceAlpha != null && sourceAlpha[i] != null) {
                    alpha[i] = sourceAlpha[i].getExtendedData(destRect,
                                                              zeroExtender);
                }

                if(sourceROI != null && sourceROI[i] != null) {
                    roi[i] = roiImage[i].getExtendedData(destRect,
                                                         zeroExtender);
                }
            }
        }

        computeRect(rasterSources, dest, destRect, alpha, roi);

        for (int i = 0; i < numSources; i++) {
            Raster sourceData = rasterSources[i];
            if(sourceData != null) {
                PlanarImage source = getSourceImage(i);

                // Recycle the source tile
                if(source.overlapsMultipleTiles(sourceData.getBounds())) {
                    recycleTile(sourceData);
                }
            }
        }

        return dest;
    }

    protected void computeRect(Raster[] sources,
                               WritableRaster dest,
                               Rectangle destRect) {
        computeRect(sources, dest, destRect, null, null);
    }

    protected void computeRect(Raster[] sources,
                               WritableRaster dest,
                               Rectangle destRect,
                               Raster[] alphaRaster,
                               Raster[] roiRaster) {
        // Save the source count.
        int numSources = sources.length;

        // Put all non-null sources in a list.
        ArrayList sourceList = new ArrayList(numSources);
        for(int i = 0; i < numSources; i++) {
            if(sources[i] != null) {
                sourceList.add(sources[i]);
            }
        }

        // Clear the background and return if no sources.
        int numNonNullSources = sourceList.size();
        if(numNonNullSources == 0) {
            ImageUtil.fillBackground(dest, destRect, backgroundValues);
            return;
        }

        // Determine the format tag id.
        SampleModel[] sourceSM = new SampleModel[numNonNullSources];
        for(int i = 0; i < numNonNullSources; i++) {
            sourceSM[i] = ((Raster)sourceList.get(i)).getSampleModel();
        }
        int formatTagID =
            RasterAccessor.findCompatibleTag(sourceSM,
                                             dest.getSampleModel());

        // Create source accessors.
        RasterAccessor[] s = new RasterAccessor[numSources];
        for(int i = 0; i < numSources; i++) {
            if(sources[i] != null) {
                RasterFormatTag formatTag =
                    new RasterFormatTag(sources[i].getSampleModel(),
                                        formatTagID);
                s[i] = new RasterAccessor(sources[i], destRect, formatTag,
                                          null);
            }
        }

        // Create dest accessor.
        RasterAccessor d =
            new RasterAccessor(dest, destRect,
                               new RasterFormatTag(dest.getSampleModel(),
                                                   formatTagID),
                               null);

        // Create the alpha accessors.
        RasterAccessor[] a = new RasterAccessor[numSources];
        if(alphaRaster != null) {
            for(int i = 0; i < numSources; i++) {
                if(alphaRaster[i] != null) {
                    SampleModel alphaSM = alphaRaster[i].getSampleModel();
                    int alphaFormatTagID =
                        RasterAccessor.findCompatibleTag(null, alphaSM);
                    RasterFormatTag alphaFormatTag =
                        new RasterFormatTag(alphaSM, alphaFormatTagID);
                    a[i] = new RasterAccessor(alphaRaster[i], destRect,  
                                              alphaFormatTag,
                                              sourceAlpha[i].getColorModel());
                }
            }
        }

        // Branch to data type-specific method.
        switch (d.getDataType()) {
        case DataBuffer.TYPE_BYTE:
            computeRectByte(s, d, a, roiRaster);
            break;
        case DataBuffer.TYPE_USHORT:
            computeRectUShort(s, d, a, roiRaster);
            break;
        case DataBuffer.TYPE_SHORT:
            computeRectShort(s, d, a, roiRaster);
            break;
        case DataBuffer.TYPE_INT:
            computeRectInt(s, d, a, roiRaster);
            break;
        case DataBuffer.TYPE_FLOAT:
            computeRectFloat(s, d, a, roiRaster);
            break;
        case DataBuffer.TYPE_DOUBLE:
            computeRectDouble(s, d, a, roiRaster);
            break;
        }

        d.copyDataToRaster();
    }

    private void computeRectByte(RasterAccessor[] src,
                                 RasterAccessor dst,
                                 RasterAccessor[] alfa,
                                 Raster[] roi) {
        // Save the source count.
        int numSources = src.length;

        // Allocate stride, offset, and data arrays for sources.
        int[] srcLineStride = new int[numSources];
        int[] srcPixelStride = new int[numSources];
        int[][] srcBandOffsets = new int[numSources][];
        byte[][][] srcData = new byte[numSources][][];

        // Initialize stride, offset, and data arrays for sources.
        for(int i = 0; i < numSources; i++) {
            if(src[i] != null) {
                srcLineStride[i] = src[i].getScanlineStride();
                srcPixelStride[i] = src[i].getPixelStride();
                srcBandOffsets[i] = src[i].getBandOffsets();
                srcData[i] = src[i].getByteDataArrays();
            }
        }

        // Initialize destination variables.
        int dstMinX = dst.getX();
        int dstMinY = dst.getY();
        int dstWidth = dst.getWidth();
        int dstHeight = dst.getHeight();
        int dstMaxX = dstMinX + dstWidth;  // x max exclusive
        int dstMaxY = dstMinY + dstHeight; // y max exclusive
        int dstBands = dst.getNumBands();
        int dstLineStride = dst.getScanlineStride();
        int dstPixelStride = dst.getPixelStride();
        int[] dstBandOffsets = dst.getBandOffsets();
        byte[][] dstData = dst.getByteDataArrays();

        // Check for alpha.
        boolean hasAlpha = false;
        for(int i = 0; i < numSources; i++) {
            if(alfa[i] != null) {
                hasAlpha = true;
                break;
            }
        }

        // Declare alpha channel arrays.
        int[] alfaLineStride = null;
        int[] alfaPixelStride = null;
        int[][] alfaBandOffsets = null;
        byte[][][] alfaData = null;

        if(hasAlpha) {
            // Allocate stride, offset, and data arrays for alpha channels.
            alfaLineStride = new int[numSources];
            alfaPixelStride = new int[numSources];
            alfaBandOffsets = new int[numSources][];
            alfaData = new byte[numSources][][];

            // Initialize stride, offset, and data arrays for alpha channels.
            for(int i = 0; i < numSources; i++) {
                if(alfa[i] != null) {
                    alfaLineStride[i] = alfa[i].getScanlineStride();
                    alfaPixelStride[i] = alfa[i].getPixelStride();
                    alfaBandOffsets[i] = alfa[i].getBandOffsets();
                    alfaData[i] = alfa[i].getByteDataArrays();
                }
            }
        }

        // Initialize weight type arrays.
        int[] weightTypes = new int[numSources];
        for(int i = 0; i < numSources; i++) {
            weightTypes[i] = WEIGHT_TYPE_THRESHOLD;
            if(alfa[i] != null) {
                weightTypes[i] = WEIGHT_TYPE_ALPHA;
            } else if(sourceROI != null && sourceROI[i] != null) {
                weightTypes[i] = WEIGHT_TYPE_ROI;
            }
        }

        // Set up source offset and data variabls.
        int[] sLineOffsets = new int[numSources];
        int[] sPixelOffsets = new int[numSources];
        byte[][] sBandData = new byte[numSources][];

        // Set up alpha offset and data variabls.
        int[] aLineOffsets = null;
        int[] aPixelOffsets = null;
        byte[][] aBandData = null;
        if(hasAlpha) {
            aLineOffsets = new int[numSources];
            aPixelOffsets = new int[numSources];
            aBandData = new byte[numSources][];
        }

        for(int b = 0; b < dstBands; b++) {
            // Initialize source and alpha band array and line offsets.
            for(int s = 0; s < numSources; s++) {
                if(src[s] != null) {
                    sBandData[s] = srcData[s][b];
                    sLineOffsets[s] = srcBandOffsets[s][b];
                }
                if(weightTypes[s] == WEIGHT_TYPE_ALPHA) {
                    aBandData[s] = alfaData[s][0];
                    aLineOffsets[s] = alfaBandOffsets[s][0];
                }
            }

            // Initialize destination band array and line offsets.
            byte[] dBandData = dstData[b];
            int dLineOffset = dstBandOffsets[b];

            if(mosaicType == MosaicDescriptor.MOSAIC_TYPE_OVERLAY) {
                for(int dstY = dstMinY; dstY < dstMaxY; dstY++) {
                    // Initialize source and alpha pixel offsets and
                    // update line offsets.
                    for(int s = 0; s < numSources; s++) {
                        if(src[s] != null) {
                            sPixelOffsets[s] = sLineOffsets[s];
                            sLineOffsets[s] += srcLineStride[s];
                        }
                        if(alfa[s] != null) {
                            aPixelOffsets[s] = aLineOffsets[s];
                            aLineOffsets[s] += alfaLineStride[s];
                        }
                    }

                    // Initialize destination pixel offset and update
                    // line offset.
                    int dPixelOffset = dLineOffset;
                    dLineOffset += dstLineStride;

                    for(int dstX = dstMinX; dstX < dstMaxX; dstX++) {

                        // Unset destination update flag.
                        boolean setDestValue = false;

                        // Loop over source until a non-zero weight is
                        // encountered.
                        for(int s = 0; s < numSources; s++) {
                            if(src[s] == null) continue;

                            byte sourceValue =
                                sBandData[s][sPixelOffsets[s]];
                            sPixelOffsets[s] += srcPixelStride[s];

                            switch(weightTypes[s]) {
                            case WEIGHT_TYPE_ALPHA:
                                setDestValue =
                                    aBandData[s][aPixelOffsets[s]] != 0;
                                aPixelOffsets[s] += alfaPixelStride[s];
                                break;
                            case WEIGHT_TYPE_ROI:
                                setDestValue =
                                    roi[s].getSample(dstX, dstY, 0) > 0;
                                break;
                            default: // WEIGHT_TYPE_THRESHOLD
                                setDestValue =
                                    (sourceValue&0xff) >=
                                    sourceThreshold[s][b];
                            }

                            // Set the destination value if a non-zero
                            // weight was found.
                            if(setDestValue) {
                                dBandData[dPixelOffset] = sourceValue;

                                // Increment offset of subsequent sources.
                                for(int k = s + 1; k < numSources; k++) {
                                    if(src[k] != null) {
                                        sPixelOffsets[k] += srcPixelStride[k];
                                    }
                                    if(alfa[k] != null) {
                                        aPixelOffsets[k] += alfaPixelStride[k];
                                    }
                                }
                                break;
                            }
                        }

                        // Set the destination value to the background if
                        // no value was set.
                        if(!setDestValue) {
                            dBandData[dPixelOffset] = (byte)background[b];
                        }

                        dPixelOffset += dstPixelStride;
                    }
                }
            } else { // mosaicType == MosaicDescriptor.MOSAIC_TYPE_BLEND
                for(int dstY = dstMinY; dstY < dstMaxY; dstY++) {
                    // Initialize source and alpha pixel offsets and
                    // update line offsets.
                    for(int s = 0; s < numSources; s++) {
                        if(src[s] != null) {
                            sPixelOffsets[s] = sLineOffsets[s];
                            sLineOffsets[s] += srcLineStride[s];
                        }
                        if(weightTypes[s] == WEIGHT_TYPE_ALPHA) {
                            aPixelOffsets[s] = aLineOffsets[s];
                            aLineOffsets[s] += alfaLineStride[s];
                        }
                    }

                    // Initialize destination pixel offset and update
                    // line offset.
                    int dPixelOffset = dLineOffset;
                    dLineOffset += dstLineStride;

                    for(int dstX = dstMinX; dstX < dstMaxX; dstX++) {

                        // Clear values for blending ratio.
                        float numerator = 0.0F;
                        float denominator = 0.0F;

                        // Accumulate into numerator and denominator.
                        for(int s = 0; s < numSources; s++) {
                            if(src[s] == null) continue;

                            byte sourceValue =
                                sBandData[s][sPixelOffsets[s]];
                            sPixelOffsets[s] += srcPixelStride[s];

                            float weight = 0.0F;
                            switch(weightTypes[s]) {
                            case WEIGHT_TYPE_ALPHA:
                                weight = (aBandData[s][aPixelOffsets[s]]&0xff);
                                if(weight > 0.0F && isAlphaBitmask) {
                                    weight = 1.0F;
                                } else {
                                    weight /= 255.0F;
                                }
                                aPixelOffsets[s] += alfaPixelStride[s];
                                break;
                            case WEIGHT_TYPE_ROI:
                                weight =
                                    roi[s].getSample(dstX, dstY, 0) > 0 ?
                                    1.0F : 0.0F;
                                break;
                            default: // WEIGHT_TYPE_THRESHOLD
                                weight =
                                    (sourceValue&0xff) >=
                                    sourceThreshold[s][b] ?
                                    1.0F : 0.0F;
                            }

                            // Update numerator and denominator.
                            numerator += (weight*(sourceValue&0xff));
                            denominator += weight;
                        }

                        // Clear the background if all weights were zero,
                        // otherwise blend the values.
                        if(denominator == 0.0) {
                            dBandData[dPixelOffset] = (byte)background[b];
                        } else {
                            dBandData[dPixelOffset] =
                                ImageUtil.clampRoundByte(numerator /
                                                         denominator);
                        }

                        dPixelOffset += dstPixelStride;
                    }
                }
            }
        }
    }

    private void computeRectUShort(RasterAccessor[] src,
                                   RasterAccessor dst,
                                   RasterAccessor[] alfa,
                                   Raster[] roi) {
        // Save the source count.
        int numSources = src.length;

        // Allocate stride, offset, and data arrays for sources.
        int[] srcLineStride = new int[numSources];
        int[] srcPixelStride = new int[numSources];
        int[][] srcBandOffsets = new int[numSources][];
        short[][][] srcData = new short[numSources][][];

        // Initialize stride, offset, and data arrays for sources.
        for(int i = 0; i < numSources; i++) {
            if(src[i] != null) {
                srcLineStride[i] = src[i].getScanlineStride();
                srcPixelStride[i] = src[i].getPixelStride();
                srcBandOffsets[i] = src[i].getBandOffsets();
                srcData[i] = src[i].getShortDataArrays();
            }
        }

        // Initialize destination variables.
        int dstMinX = dst.getX();
        int dstMinY = dst.getY();
        int dstWidth = dst.getWidth();
        int dstHeight = dst.getHeight();
        int dstMaxX = dstMinX + dstWidth;  // x max exclusive
        int dstMaxY = dstMinY + dstHeight; // y max exclusive
        int dstBands = dst.getNumBands();
        int dstLineStride = dst.getScanlineStride();
        int dstPixelStride = dst.getPixelStride();
        int[] dstBandOffsets = dst.getBandOffsets();
        short[][] dstData = dst.getShortDataArrays();

        // Check for alpha.
        boolean hasAlpha = false;
        for(int i = 0; i < numSources; i++) {
            if(alfa[i] != null) {
                hasAlpha = true;
                break;
            }
        }

        // Declare alpha channel arrays.
        int[] alfaLineStride = null;
        int[] alfaPixelStride = null;
        int[][] alfaBandOffsets = null;
        short[][][] alfaData = null;

        if(hasAlpha) {
            // Allocate stride, offset, and data arrays for alpha channels.
            alfaLineStride = new int[numSources];
            alfaPixelStride = new int[numSources];
            alfaBandOffsets = new int[numSources][];
            alfaData = new short[numSources][][];

            // Initialize stride, offset, and data arrays for alpha channels.
            for(int i = 0; i < numSources; i++) {
                if(alfa[i] != null) {
                    alfaLineStride[i] = alfa[i].getScanlineStride();
                    alfaPixelStride[i] = alfa[i].getPixelStride();
                    alfaBandOffsets[i] = alfa[i].getBandOffsets();
                    alfaData[i] = alfa[i].getShortDataArrays();
                }
            }
        }

        // Initialize weight type arrays.
        int[] weightTypes = new int[numSources];
        for(int i = 0; i < numSources; i++) {
            weightTypes[i] = WEIGHT_TYPE_THRESHOLD;
            if(alfa[i] != null) {
                weightTypes[i] = WEIGHT_TYPE_ALPHA;
            } else if(sourceROI != null && sourceROI[i] != null) {
                weightTypes[i] = WEIGHT_TYPE_ROI;
            }
        }

        // Set up source offset and data variabls.
        int[] sLineOffsets = new int[numSources];
        int[] sPixelOffsets = new int[numSources];
        short[][] sBandData = new short[numSources][];

        // Set up alpha offset and data variabls.
        int[] aLineOffsets = null;
        int[] aPixelOffsets = null;
        short[][] aBandData = null;
        if(hasAlpha) {
            aLineOffsets = new int[numSources];
            aPixelOffsets = new int[numSources];
            aBandData = new short[numSources][];
        }

        for(int b = 0; b < dstBands; b++) {
            // Initialize source and alpha band array and line offsets.
            for(int s = 0; s < numSources; s++) {
                if(src[s] != null) {
                    sBandData[s] = srcData[s][b];
                    sLineOffsets[s] = srcBandOffsets[s][b];
                }
                if(weightTypes[s] == WEIGHT_TYPE_ALPHA) {
                    aBandData[s] = alfaData[s][0];
                    aLineOffsets[s] = alfaBandOffsets[s][0];
                }
            }

            // Initialize destination band array and line offsets.
            short[] dBandData = dstData[b];
            int dLineOffset = dstBandOffsets[b];

            if(mosaicType == MosaicDescriptor.MOSAIC_TYPE_OVERLAY) {
                for(int dstY = dstMinY; dstY < dstMaxY; dstY++) {
                    // Initialize source and alpha pixel offsets and
                    // update line offsets.
                    for(int s = 0; s < numSources; s++) {
                        if(src[s] != null) {
                            sPixelOffsets[s] = sLineOffsets[s];
                            sLineOffsets[s] += srcLineStride[s];
                        }
                        if(alfa[s] != null) {
                            aPixelOffsets[s] = aLineOffsets[s];
                            aLineOffsets[s] += alfaLineStride[s];
                        }
                    }

                    // Initialize destination pixel offset and update
                    // line offset.
                    int dPixelOffset = dLineOffset;
                    dLineOffset += dstLineStride;

                    for(int dstX = dstMinX; dstX < dstMaxX; dstX++) {

                        // Unset destination update flag.
                        boolean setDestValue = false;

                        // Loop over source until a non-zero weight is
                        // encountered.
                        for(int s = 0; s < numSources; s++) {
                            if(src[s] == null) continue;

                            short sourceValue =
                                sBandData[s][sPixelOffsets[s]];
                            sPixelOffsets[s] += srcPixelStride[s];

                            switch(weightTypes[s]) {
                            case WEIGHT_TYPE_ALPHA:
                                setDestValue =
                                    aBandData[s][aPixelOffsets[s]] != 0;
                                aPixelOffsets[s] += alfaPixelStride[s];
                                break;
                            case WEIGHT_TYPE_ROI:
                                setDestValue =
                                    roi[s].getSample(dstX, dstY, 0) > 0;
                                break;
                            default: // WEIGHT_TYPE_THRESHOLD
                                setDestValue =
                                    (sourceValue&0xffff) >=
                                    sourceThreshold[s][b];
                            }

                            // Set the destination value if a non-zero
                            // weight was found.
                            if(setDestValue) {
                                dBandData[dPixelOffset] = sourceValue;

                                // Increment offset of subsequent sources.
                                for(int k = s + 1; k < numSources; k++) {
                                    if(src[k] != null) {
                                        sPixelOffsets[k] += srcPixelStride[k];
                                    }
                                    if(alfa[k] != null) {
                                        aPixelOffsets[k] += alfaPixelStride[k];
                                    }
                                }
                                break;
                            }
                        }

                        // Set the destination value to the background if
                        // no value was set.
                        if(!setDestValue) {
                            dBandData[dPixelOffset] = (short)background[b];
                        }

                        dPixelOffset += dstPixelStride;
                    }
                }
            } else { // mosaicType == MosaicDescriptor.MOSAIC_TYPE_BLEND
                for(int dstY = dstMinY; dstY < dstMaxY; dstY++) {
                    // Initialize source and alpha pixel offsets and
                    // update line offsets.
                    for(int s = 0; s < numSources; s++) {
                        if(src[s] != null) {
                            sPixelOffsets[s] = sLineOffsets[s];
                            sLineOffsets[s] += srcLineStride[s];
                        }
                        if(weightTypes[s] == WEIGHT_TYPE_ALPHA) {
                            aPixelOffsets[s] = aLineOffsets[s];
                            aLineOffsets[s] += alfaLineStride[s];
                        }
                    }

                    // Initialize destination pixel offset and update
                    // line offset.
                    int dPixelOffset = dLineOffset;
                    dLineOffset += dstLineStride;

                    for(int dstX = dstMinX; dstX < dstMaxX; dstX++) {

                        // Clear values for blending ratio.
                        float numerator = 0.0F;
                        float denominator = 0.0F;

                        // Accumulate into numerator and denominator.
                        for(int s = 0; s < numSources; s++) {
                            if(src[s] == null) continue;

                            short sourceValue =
                                sBandData[s][sPixelOffsets[s]];
                            sPixelOffsets[s] += srcPixelStride[s];

                            float weight = 0.0F;
                            switch(weightTypes[s]) {
                            case WEIGHT_TYPE_ALPHA:
                                weight = (aBandData[s][aPixelOffsets[s]]&0xffff);
                                if(weight > 0.0F && isAlphaBitmask) {
                                    weight = 1.0F;
                                } else {
                                    weight /= 65535.0F;
                                }
                                aPixelOffsets[s] += alfaPixelStride[s];
                                break;
                            case WEIGHT_TYPE_ROI:
                                weight =
                                    roi[s].getSample(dstX, dstY, 0) > 0 ?
                                    1.0F : 0.0F;
                                break;
                            default: // WEIGHT_TYPE_THRESHOLD
                                weight =
                                    (sourceValue&0xffff) >=
                                    sourceThreshold[s][b] ?
                                    1.0F : 0.0F;
                            }

                            // Update numerator and denominator.
                            numerator += (weight*(sourceValue&0xffff));
                            denominator += weight;
                        }

                        // Clear the background if all weights were zero,
                        // otherwise blend the values.
                        if(denominator == 0.0) {
                            dBandData[dPixelOffset] = (short)background[b];
                        } else {
                            dBandData[dPixelOffset] =
                                ImageUtil.clampRoundUShort(numerator /
                                                           denominator);
                        }

                        dPixelOffset += dstPixelStride;
                    }
                }
            }
        }
    }

    private void computeRectShort(RasterAccessor[] src,
                                  RasterAccessor dst,
                                  RasterAccessor[] alfa,
                                  Raster[] roi) {
        // Save the source count.
        int numSources = src.length;

        // Allocate stride, offset, and data arrays for sources.
        int[] srcLineStride = new int[numSources];
        int[] srcPixelStride = new int[numSources];
        int[][] srcBandOffsets = new int[numSources][];
        short[][][] srcData = new short[numSources][][];

        // Initialize stride, offset, and data arrays for sources.
        for(int i = 0; i < numSources; i++) {
            if(src[i] != null) {
                srcLineStride[i] = src[i].getScanlineStride();
                srcPixelStride[i] = src[i].getPixelStride();
                srcBandOffsets[i] = src[i].getBandOffsets();
                srcData[i] = src[i].getShortDataArrays();
            }
        }

        // Initialize destination variables.
        int dstMinX = dst.getX();
        int dstMinY = dst.getY();
        int dstWidth = dst.getWidth();
        int dstHeight = dst.getHeight();
        int dstMaxX = dstMinX + dstWidth;  // x max exclusive
        int dstMaxY = dstMinY + dstHeight; // y max exclusive
        int dstBands = dst.getNumBands();
        int dstLineStride = dst.getScanlineStride();
        int dstPixelStride = dst.getPixelStride();
        int[] dstBandOffsets = dst.getBandOffsets();
        short[][] dstData = dst.getShortDataArrays();

        // Check for alpha.
        boolean hasAlpha = false;
        for(int i = 0; i < numSources; i++) {
            if(alfa[i] != null) {
                hasAlpha = true;
                break;
            }
        }

        // Declare alpha channel arrays.
        int[] alfaLineStride = null;
        int[] alfaPixelStride = null;
        int[][] alfaBandOffsets = null;
        short[][][] alfaData = null;

        if(hasAlpha) {
            // Allocate stride, offset, and data arrays for alpha channels.
            alfaLineStride = new int[numSources];
            alfaPixelStride = new int[numSources];
            alfaBandOffsets = new int[numSources][];
            alfaData = new short[numSources][][];

            // Initialize stride, offset, and data arrays for alpha channels.
            for(int i = 0; i < numSources; i++) {
                if(alfa[i] != null) {
                    alfaLineStride[i] = alfa[i].getScanlineStride();
                    alfaPixelStride[i] = alfa[i].getPixelStride();
                    alfaBandOffsets[i] = alfa[i].getBandOffsets();
                    alfaData[i] = alfa[i].getShortDataArrays();
                }
            }
        }

        // Initialize weight type arrays.
        int[] weightTypes = new int[numSources];
        for(int i = 0; i < numSources; i++) {
            weightTypes[i] = WEIGHT_TYPE_THRESHOLD;
            if(alfa[i] != null) {
                weightTypes[i] = WEIGHT_TYPE_ALPHA;
            } else if(sourceROI != null && sourceROI[i] != null) {
                weightTypes[i] = WEIGHT_TYPE_ROI;
            }
        }

        // Set up source offset and data variabls.
        int[] sLineOffsets = new int[numSources];
        int[] sPixelOffsets = new int[numSources];
        short[][] sBandData = new short[numSources][];

        // Set up alpha offset and data variabls.
        int[] aLineOffsets = null;
        int[] aPixelOffsets = null;
        short[][] aBandData = null;
        if(hasAlpha) {
            aLineOffsets = new int[numSources];
            aPixelOffsets = new int[numSources];
            aBandData = new short[numSources][];
        }

        for(int b = 0; b < dstBands; b++) {
            // Initialize source and alpha band array and line offsets.
            for(int s = 0; s < numSources; s++) {
                if(src[s] != null) {
                    sBandData[s] = srcData[s][b];
                    sLineOffsets[s] = srcBandOffsets[s][b];
                }
                if(weightTypes[s] == WEIGHT_TYPE_ALPHA) {
                    aBandData[s] = alfaData[s][0];
                    aLineOffsets[s] = alfaBandOffsets[s][0];
                }
            }

            // Initialize destination band array and line offsets.
            short[] dBandData = dstData[b];
            int dLineOffset = dstBandOffsets[b];

            if(mosaicType == MosaicDescriptor.MOSAIC_TYPE_OVERLAY) {
                for(int dstY = dstMinY; dstY < dstMaxY; dstY++) {
                    // Initialize source and alpha pixel offsets and
                    // update line offsets.
                    for(int s = 0; s < numSources; s++) {
                        if(src[s] != null) {
                            sPixelOffsets[s] = sLineOffsets[s];
                            sLineOffsets[s] += srcLineStride[s];
                        }
                        if(alfa[s] != null) {
                            aPixelOffsets[s] = aLineOffsets[s];
                            aLineOffsets[s] += alfaLineStride[s];
                        }
                    }

                    // Initialize destination pixel offset and update
                    // line offset.
                    int dPixelOffset = dLineOffset;
                    dLineOffset += dstLineStride;

                    for(int dstX = dstMinX; dstX < dstMaxX; dstX++) {

                        // Unset destination update flag.
                        boolean setDestValue = false;

                        // Loop over source until a non-zero weight is
                        // encountered.
                        for(int s = 0; s < numSources; s++) {
                            if(src[s] == null) continue;

                            short sourceValue =
                                sBandData[s][sPixelOffsets[s]];
                            sPixelOffsets[s] += srcPixelStride[s];

                            switch(weightTypes[s]) {
                            case WEIGHT_TYPE_ALPHA:
                                setDestValue =
                                    aBandData[s][aPixelOffsets[s]] != 0;
                                aPixelOffsets[s] += alfaPixelStride[s];
                                break;
                            case WEIGHT_TYPE_ROI:
                                setDestValue =
                                    roi[s].getSample(dstX, dstY, 0) > 0;
                                break;
                            default: // WEIGHT_TYPE_THRESHOLD
                                setDestValue =
                                    sourceValue >=
                                    sourceThreshold[s][b];
                            }

                            // Set the destination value if a non-zero
                            // weight was found.
                            if(setDestValue) {
                                dBandData[dPixelOffset] = sourceValue;

                                // Increment offset of subsequent sources.
                                for(int k = s + 1; k < numSources; k++) {
                                    if(src[k] != null) {
                                        sPixelOffsets[k] += srcPixelStride[k];
                                    }
                                    if(alfa[k] != null) {
                                        aPixelOffsets[k] += alfaPixelStride[k];
                                    }
                                }
                                break;
                            }
                        }

                        // Set the destination value to the background if
                        // no value was set.
                        if(!setDestValue) {
                            dBandData[dPixelOffset] = (short)background[b];
                        }

                        dPixelOffset += dstPixelStride;
                    }
                }
            } else { // mosaicType == MosaicDescriptor.MOSAIC_TYPE_BLEND
                for(int dstY = dstMinY; dstY < dstMaxY; dstY++) {
                    // Initialize source and alpha pixel offsets and
                    // update line offsets.
                    for(int s = 0; s < numSources; s++) {
                        if(src[s] != null) {
                            sPixelOffsets[s] = sLineOffsets[s];
                            sLineOffsets[s] += srcLineStride[s];
                        }
                        if(weightTypes[s] == WEIGHT_TYPE_ALPHA) {
                            aPixelOffsets[s] = aLineOffsets[s];
                            aLineOffsets[s] += alfaLineStride[s];
                        }
                    }

                    // Initialize destination pixel offset and update
                    // line offset.
                    int dPixelOffset = dLineOffset;
                    dLineOffset += dstLineStride;

                    for(int dstX = dstMinX; dstX < dstMaxX; dstX++) {

                        // Clear values for blending ratio.
                        float numerator = 0.0F;
                        float denominator = 0.0F;

                        // Accumulate into numerator and denominator.
                        for(int s = 0; s < numSources; s++) {
                            if(src[s] == null) continue;

                            short sourceValue =
                                sBandData[s][sPixelOffsets[s]];
                            sPixelOffsets[s] += srcPixelStride[s];

                            float weight = 0.0F;
                            switch(weightTypes[s]) {
                            case WEIGHT_TYPE_ALPHA:
                                weight = aBandData[s][aPixelOffsets[s]];
                                if(weight > 0.0F && isAlphaBitmask) {
                                    weight = 1.0F;
                                } else {
                                    weight /= (float)Short.MAX_VALUE;
                                }
                                aPixelOffsets[s] += alfaPixelStride[s];
                                break;
                            case WEIGHT_TYPE_ROI:
                                weight =
                                    roi[s].getSample(dstX, dstY, 0) > 0 ?
                                    1.0F : 0.0F;
                                break;
                            default: // WEIGHT_TYPE_THRESHOLD
                                weight =
                                    sourceValue >=
                                    sourceThreshold[s][b] ?
                                    1.0F : 0.0F;
                            }

                            // Update numerator and denominator.
                            numerator += weight*sourceValue;
                            denominator += weight;
                        }

                        // Clear the background if all weights were zero,
                        // otherwise blend the values.
                        if(denominator == 0.0) {
                            dBandData[dPixelOffset] = (short)background[b];
                        } else {
                            dBandData[dPixelOffset] =
                                ImageUtil.clampRoundShort(numerator /
                                                          denominator);
                        }

                        dPixelOffset += dstPixelStride;
                    }
                }
            }
        }
    }

    private void computeRectInt(RasterAccessor[] src,
                                RasterAccessor dst,
                                RasterAccessor[] alfa,
                                Raster[] roi) {
        // Save the source count.
        int numSources = src.length;

        // Allocate stride, offset, and data arrays for sources.
        int[] srcLineStride = new int[numSources];
        int[] srcPixelStride = new int[numSources];
        int[][] srcBandOffsets = new int[numSources][];
        int[][][] srcData = new int[numSources][][];

        // Initialize stride, offset, and data arrays for sources.
        for(int i = 0; i < numSources; i++) {
            if(src[i] != null) {
                srcLineStride[i] = src[i].getScanlineStride();
                srcPixelStride[i] = src[i].getPixelStride();
                srcBandOffsets[i] = src[i].getBandOffsets();
                srcData[i] = src[i].getIntDataArrays();
            }
        }

        // Initialize destination variables.
        int dstMinX = dst.getX();
        int dstMinY = dst.getY();
        int dstWidth = dst.getWidth();
        int dstHeight = dst.getHeight();
        int dstMaxX = dstMinX + dstWidth;  // x max exclusive
        int dstMaxY = dstMinY + dstHeight; // y max exclusive
        int dstBands = dst.getNumBands();
        int dstLineStride = dst.getScanlineStride();
        int dstPixelStride = dst.getPixelStride();
        int[] dstBandOffsets = dst.getBandOffsets();
        int[][] dstData = dst.getIntDataArrays();

        // Check for alpha.
        boolean hasAlpha = false;
        for(int i = 0; i < numSources; i++) {
            if(alfa[i] != null) {
                hasAlpha = true;
                break;
            }
        }

        // Declare alpha channel arrays.
        int[] alfaLineStride = null;
        int[] alfaPixelStride = null;
        int[][] alfaBandOffsets = null;
        int[][][] alfaData = null;

        if(hasAlpha) {
            // Allocate stride, offset, and data arrays for alpha channels.
            alfaLineStride = new int[numSources];
            alfaPixelStride = new int[numSources];
            alfaBandOffsets = new int[numSources][];
            alfaData = new int[numSources][][];

            // Initialize stride, offset, and data arrays for alpha channels.
            for(int i = 0; i < numSources; i++) {
                if(alfa[i] != null) {
                    alfaLineStride[i] = alfa[i].getScanlineStride();
                    alfaPixelStride[i] = alfa[i].getPixelStride();
                    alfaBandOffsets[i] = alfa[i].getBandOffsets();
                    alfaData[i] = alfa[i].getIntDataArrays();
                }
            }
        }

        // Initialize weight type arrays.
        int[] weightTypes = new int[numSources];
        for(int i = 0; i < numSources; i++) {
            weightTypes[i] = WEIGHT_TYPE_THRESHOLD;
            if(alfa[i] != null) {
                weightTypes[i] = WEIGHT_TYPE_ALPHA;
            } else if(sourceROI != null && sourceROI[i] != null) {
                weightTypes[i] = WEIGHT_TYPE_ROI;
            }
        }

        // Set up source offset and data variabls.
        int[] sLineOffsets = new int[numSources];
        int[] sPixelOffsets = new int[numSources];
        int[][] sBandData = new int[numSources][];

        // Set up alpha offset and data variabls.
        int[] aLineOffsets = null;
        int[] aPixelOffsets = null;
        int[][] aBandData = null;
        if(hasAlpha) {
            aLineOffsets = new int[numSources];
            aPixelOffsets = new int[numSources];
            aBandData = new int[numSources][];
        }

        for(int b = 0; b < dstBands; b++) {
            // Initialize source and alpha band array and line offsets.
            for(int s = 0; s < numSources; s++) {
                if(src[s] != null) {
                    sBandData[s] = srcData[s][b];
                    sLineOffsets[s] = srcBandOffsets[s][b];
                }
                if(weightTypes[s] == WEIGHT_TYPE_ALPHA) {
                    aBandData[s] = alfaData[s][0];
                    aLineOffsets[s] = alfaBandOffsets[s][0];
                }
            }

            // Initialize destination band array and line offsets.
            int[] dBandData = dstData[b];
            int dLineOffset = dstBandOffsets[b];

            if(mosaicType == MosaicDescriptor.MOSAIC_TYPE_OVERLAY) {
                for(int dstY = dstMinY; dstY < dstMaxY; dstY++) {
                    // Initialize source and alpha pixel offsets and
                    // update line offsets.
                    for(int s = 0; s < numSources; s++) {
                        if(src[s] != null) {
                            sPixelOffsets[s] = sLineOffsets[s];
                            sLineOffsets[s] += srcLineStride[s];
                        }
                        if(alfa[s] != null) {
                            aPixelOffsets[s] = aLineOffsets[s];
                            aLineOffsets[s] += alfaLineStride[s];
                        }
                    }

                    // Initialize destination pixel offset and update
                    // line offset.
                    int dPixelOffset = dLineOffset;
                    dLineOffset += dstLineStride;

                    for(int dstX = dstMinX; dstX < dstMaxX; dstX++) {

                        // Unset destination update flag.
                        boolean setDestValue = false;

                        // Loop over source until a non-zero weight is
                        // encountered.
                        for(int s = 0; s < numSources; s++) {
                            if(src[s] == null) continue;

                            int sourceValue =
                                sBandData[s][sPixelOffsets[s]];
                            sPixelOffsets[s] += srcPixelStride[s];

                            switch(weightTypes[s]) {
                            case WEIGHT_TYPE_ALPHA:
                                setDestValue =
                                    aBandData[s][aPixelOffsets[s]] != 0;
                                aPixelOffsets[s] += alfaPixelStride[s];
                                break;
                            case WEIGHT_TYPE_ROI:
                                setDestValue =
                                    roi[s].getSample(dstX, dstY, 0) > 0;
                                break;
                            default: // WEIGHT_TYPE_THRESHOLD
                                setDestValue =
                                    sourceValue >=
                                    sourceThreshold[s][b];
                            }

                            // Set the destination value if a non-zero
                            // weight was found.
                            if(setDestValue) {
                                dBandData[dPixelOffset] = sourceValue;

                                // Increment offset of subsequent sources.
                                for(int k = s + 1; k < numSources; k++) {
                                    if(src[k] != null) {
                                        sPixelOffsets[k] += srcPixelStride[k];
                                    }
                                    if(alfa[k] != null) {
                                        aPixelOffsets[k] += alfaPixelStride[k];
                                    }
                                }
                                break;
                            }
                        }

                        // Set the destination value to the background if
                        // no value was set.
                        if(!setDestValue) {
                            dBandData[dPixelOffset] = (int)background[b];
                        }

                        dPixelOffset += dstPixelStride;
                    }
                }
            } else { // mosaicType == MosaicDescriptor.MOSAIC_TYPE_BLEND
                for(int dstY = dstMinY; dstY < dstMaxY; dstY++) {
                    // Initialize source and alpha pixel offsets and
                    // update line offsets.
                    for(int s = 0; s < numSources; s++) {
                        if(src[s] != null) {
                            sPixelOffsets[s] = sLineOffsets[s];
                            sLineOffsets[s] += srcLineStride[s];
                        }
                        if(weightTypes[s] == WEIGHT_TYPE_ALPHA) {
                            aPixelOffsets[s] = aLineOffsets[s];
                            aLineOffsets[s] += alfaLineStride[s];
                        }
                    }

                    // Initialize destination pixel offset and update
                    // line offset.
                    int dPixelOffset = dLineOffset;
                    dLineOffset += dstLineStride;

                    for(int dstX = dstMinX; dstX < dstMaxX; dstX++) {

                        // Clear values for blending ratio.
                        double numerator = 0.0;
                        double denominator = 0.0;

                        // Accumulate into numerator and denominator.
                        for(int s = 0; s < numSources; s++) {
                            if(src[s] == null) continue;

                            int sourceValue =
                                sBandData[s][sPixelOffsets[s]];
                            sPixelOffsets[s] += srcPixelStride[s];

                            double weight = 0.0;
                            switch(weightTypes[s]) {
                            case WEIGHT_TYPE_ALPHA:
                                weight = aBandData[s][aPixelOffsets[s]];
                                if(weight > 0.0F && isAlphaBitmask) {
                                    weight = 1.0F;
                                } else {
                                    weight /= Integer.MAX_VALUE;
                                }
                                aPixelOffsets[s] += alfaPixelStride[s];
                                break;
                            case WEIGHT_TYPE_ROI:
                                weight =
                                    roi[s].getSample(dstX, dstY, 0) > 0 ?
                                    1.0F : 0.0F;
                                break;
                            default: // WEIGHT_TYPE_THRESHOLD
                                weight =
                                    sourceValue >=
                                    sourceThreshold[s][b] ?
                                    1.0F : 0.0F;
                            }

                            // Update numerator and denominator.
                            numerator += weight*sourceValue;
                            denominator += weight;
                        }

                        // Clear the background if all weights were zero,
                        // otherwise blend the values.
                        if(denominator == 0.0) {
                            dBandData[dPixelOffset] = (int)background[b];
                        } else {
                            dBandData[dPixelOffset] =
                                ImageUtil.clampRoundInt(numerator /
                                                        denominator);
                        }

                        dPixelOffset += dstPixelStride;
                    }
                }
            }
        }
    }

    private void computeRectFloat(RasterAccessor[] src,
                                  RasterAccessor dst,
                                  RasterAccessor[] alfa,
                                  Raster[] roi) {
        // Save the source count.
        int numSources = src.length;

        // Allocate stride, offset, and data arrays for sources.
        int[] srcLineStride = new int[numSources];
        int[] srcPixelStride = new int[numSources];
        int[][] srcBandOffsets = new int[numSources][];
        float[][][] srcData = new float[numSources][][];

        // Initialize stride, offset, and data arrays for sources.
        for(int i = 0; i < numSources; i++) {
            if(src[i] != null) {
                srcLineStride[i] = src[i].getScanlineStride();
                srcPixelStride[i] = src[i].getPixelStride();
                srcBandOffsets[i] = src[i].getBandOffsets();
                srcData[i] = src[i].getFloatDataArrays();
            }
        }

        // Initialize destination variables.
        int dstMinX = dst.getX();
        int dstMinY = dst.getY();
        int dstWidth = dst.getWidth();
        int dstHeight = dst.getHeight();
        int dstMaxX = dstMinX + dstWidth;  // x max exclusive
        int dstMaxY = dstMinY + dstHeight; // y max exclusive
        int dstBands = dst.getNumBands();
        int dstLineStride = dst.getScanlineStride();
        int dstPixelStride = dst.getPixelStride();
        int[] dstBandOffsets = dst.getBandOffsets();
        float[][] dstData = dst.getFloatDataArrays();

        // Check for alpha.
        boolean hasAlpha = false;
        for(int i = 0; i < numSources; i++) {
            if(alfa[i] != null) {
                hasAlpha = true;
                break;
            }
        }

        // Declare alpha channel arrays.
        int[] alfaLineStride = null;
        int[] alfaPixelStride = null;
        int[][] alfaBandOffsets = null;
        float[][][] alfaData = null;

        if(hasAlpha) {
            // Allocate stride, offset, and data arrays for alpha channels.
            alfaLineStride = new int[numSources];
            alfaPixelStride = new int[numSources];
            alfaBandOffsets = new int[numSources][];
            alfaData = new float[numSources][][];

            // Initialize stride, offset, and data arrays for alpha channels.
            for(int i = 0; i < numSources; i++) {
                if(alfa[i] != null) {
                    alfaLineStride[i] = alfa[i].getScanlineStride();
                    alfaPixelStride[i] = alfa[i].getPixelStride();
                    alfaBandOffsets[i] = alfa[i].getBandOffsets();
                    alfaData[i] = alfa[i].getFloatDataArrays();
                }
            }
        }

        // Initialize weight type arrays.
        int[] weightTypes = new int[numSources];
        for(int i = 0; i < numSources; i++) {
            weightTypes[i] = WEIGHT_TYPE_THRESHOLD;
            if(alfa[i] != null) {
                weightTypes[i] = WEIGHT_TYPE_ALPHA;
            } else if(sourceROI != null && sourceROI[i] != null) {
                weightTypes[i] = WEIGHT_TYPE_ROI;
            }
        }

        // Set up source offset and data variabls.
        int[] sLineOffsets = new int[numSources];
        int[] sPixelOffsets = new int[numSources];
        float[][] sBandData = new float[numSources][];

        // Set up alpha offset and data variabls.
        int[] aLineOffsets = null;
        int[] aPixelOffsets = null;
        float[][] aBandData = null;
        if(hasAlpha) {
            aLineOffsets = new int[numSources];
            aPixelOffsets = new int[numSources];
            aBandData = new float[numSources][];
        }

        for(int b = 0; b < dstBands; b++) {
            // Initialize source and alpha band array and line offsets.
            for(int s = 0; s < numSources; s++) {
                if(src[s] != null) {
                    sBandData[s] = srcData[s][b];
                    sLineOffsets[s] = srcBandOffsets[s][b];
                }
                if(weightTypes[s] == WEIGHT_TYPE_ALPHA) {
                    aBandData[s] = alfaData[s][0];
                    aLineOffsets[s] = alfaBandOffsets[s][0];
                }
            }

            // Initialize destination band array and line offsets.
            float[] dBandData = dstData[b];
            int dLineOffset = dstBandOffsets[b];

            if(mosaicType == MosaicDescriptor.MOSAIC_TYPE_OVERLAY) {
                for(int dstY = dstMinY; dstY < dstMaxY; dstY++) {
                    // Initialize source and alpha pixel offsets and
                    // update line offsets.
                    for(int s = 0; s < numSources; s++) {
                        if(src[s] != null) {
                            sPixelOffsets[s] = sLineOffsets[s];
                            sLineOffsets[s] += srcLineStride[s];
                        }
                        if(alfa[s] != null) {
                            aPixelOffsets[s] = aLineOffsets[s];
                            aLineOffsets[s] += alfaLineStride[s];
                        }
                    }

                    // Initialize destination pixel offset and update
                    // line offset.
                    int dPixelOffset = dLineOffset;
                    dLineOffset += dstLineStride;

                    for(int dstX = dstMinX; dstX < dstMaxX; dstX++) {

                        // Unset destination update flag.
                        boolean setDestValue = false;

                        // Loop over source until a non-zero weight is
                        // encountered.
                        for(int s = 0; s < numSources; s++) {
                            if(src[s] == null) continue;

                            float sourceValue =
                                sBandData[s][sPixelOffsets[s]];
                            sPixelOffsets[s] += srcPixelStride[s];

                            switch(weightTypes[s]) {
                            case WEIGHT_TYPE_ALPHA:
                                setDestValue =
                                    aBandData[s][aPixelOffsets[s]] != 0;
                                aPixelOffsets[s] += alfaPixelStride[s];
                                break;
                            case WEIGHT_TYPE_ROI:
                                setDestValue =
                                    roi[s].getSample(dstX, dstY, 0) > 0;
                                break;
                            default: // WEIGHT_TYPE_THRESHOLD
                                setDestValue =
                                    sourceValue >=
                                    sourceThreshold[s][b];
                            }

                            // Set the destination value if a non-zero
                            // weight was found.
                            if(setDestValue) {
                                dBandData[dPixelOffset] = sourceValue;

                                // Increment offset of subsequent sources.
                                for(int k = s + 1; k < numSources; k++) {
                                    if(src[k] != null) {
                                        sPixelOffsets[k] += srcPixelStride[k];
                                    }
                                    if(alfa[k] != null) {
                                        aPixelOffsets[k] += alfaPixelStride[k];
                                    }
                                }
                                break;
                            }
                        }

                        // Set the destination value to the background if
                        // no value was set.
                        if(!setDestValue) {
                            dBandData[dPixelOffset] = (float)backgroundValues[b];
                        }

                        dPixelOffset += dstPixelStride;
                    }
                }
            } else { // mosaicType == MosaicDescriptor.MOSAIC_TYPE_BLEND
                for(int dstY = dstMinY; dstY < dstMaxY; dstY++) {
                    // Initialize source and alpha pixel offsets and
                    // update line offsets.
                    for(int s = 0; s < numSources; s++) {
                        if(src[s] != null) {
                            sPixelOffsets[s] = sLineOffsets[s];
                            sLineOffsets[s] += srcLineStride[s];
                        }
                        if(weightTypes[s] == WEIGHT_TYPE_ALPHA) {
                            aPixelOffsets[s] = aLineOffsets[s];
                            aLineOffsets[s] += alfaLineStride[s];
                        }
                    }

                    // Initialize destination pixel offset and update
                    // line offset.
                    int dPixelOffset = dLineOffset;
                    dLineOffset += dstLineStride;

                    for(int dstX = dstMinX; dstX < dstMaxX; dstX++) {

                        // Clear values for blending ratio.
                        float numerator = 0.0F;
                        float denominator = 0.0F;

                        // Accumulate into numerator and denominator.
                        for(int s = 0; s < numSources; s++) {
                            if(src[s] == null) continue;

                            float sourceValue =
                                sBandData[s][sPixelOffsets[s]];
                            sPixelOffsets[s] += srcPixelStride[s];

                            float weight = 0.0F;
                            switch(weightTypes[s]) {
                            case WEIGHT_TYPE_ALPHA:
                                weight = aBandData[s][aPixelOffsets[s]];
                                if(weight > 0.0F && isAlphaBitmask) {
                                    weight = 1.0F;
                                }
                                aPixelOffsets[s] += alfaPixelStride[s];
                                break;
                            case WEIGHT_TYPE_ROI:
                                weight =
                                    roi[s].getSample(dstX, dstY, 0) > 0 ?
                                    1.0F : 0.0F;
                                break;
                            default: // WEIGHT_TYPE_THRESHOLD
                                weight =
                                    sourceValue >=
                                    sourceThreshold[s][b] ?
                                    1.0F : 0.0F;
                            }

                            // Update numerator and denominator.
                            numerator += weight*sourceValue;
                            denominator += weight;
                        }

                        // Clear the background if all weights were zero,
                        // otherwise blend the values.
                        if(denominator == 0.0) {
                            dBandData[dPixelOffset] = (float)backgroundValues[b];
                        } else {
                            dBandData[dPixelOffset] =
                                numerator /
                                denominator;
                        }

                        dPixelOffset += dstPixelStride;
                    }
                }
            }
        }
    }

    private void computeRectDouble(RasterAccessor[] src,
                                   RasterAccessor dst,
                                   RasterAccessor[] alfa,
                                   Raster[] roi) {
        // Save the source count.
        int numSources = src.length;

        // Allocate stride, offset, and data arrays for sources.
        int[] srcLineStride = new int[numSources];
        int[] srcPixelStride = new int[numSources];
        int[][] srcBandOffsets = new int[numSources][];
        double[][][] srcData = new double[numSources][][];

        // Initialize stride, offset, and data arrays for sources.
        for(int i = 0; i < numSources; i++) {
            if(src[i] != null) {
                srcLineStride[i] = src[i].getScanlineStride();
                srcPixelStride[i] = src[i].getPixelStride();
                srcBandOffsets[i] = src[i].getBandOffsets();
                srcData[i] = src[i].getDoubleDataArrays();
            }
        }

        // Initialize destination variables.
        int dstMinX = dst.getX();
        int dstMinY = dst.getY();
        int dstWidth = dst.getWidth();
        int dstHeight = dst.getHeight();
        int dstMaxX = dstMinX + dstWidth;  // x max exclusive
        int dstMaxY = dstMinY + dstHeight; // y max exclusive
        int dstBands = dst.getNumBands();
        int dstLineStride = dst.getScanlineStride();
        int dstPixelStride = dst.getPixelStride();
        int[] dstBandOffsets = dst.getBandOffsets();
        double[][] dstData = dst.getDoubleDataArrays();

        // Check for alpha.
        boolean hasAlpha = false;
        for(int i = 0; i < numSources; i++) {
            if(alfa[i] != null) {
                hasAlpha = true;
                break;
            }
        }

        // Declare alpha channel arrays.
        int[] alfaLineStride = null;
        int[] alfaPixelStride = null;
        int[][] alfaBandOffsets = null;
        double[][][] alfaData = null;

        if(hasAlpha) {
            // Allocate stride, offset, and data arrays for alpha channels.
            alfaLineStride = new int[numSources];
            alfaPixelStride = new int[numSources];
            alfaBandOffsets = new int[numSources][];
            alfaData = new double[numSources][][];

            // Initialize stride, offset, and data arrays for alpha channels.
            for(int i = 0; i < numSources; i++) {
                if(alfa[i] != null) {
                    alfaLineStride[i] = alfa[i].getScanlineStride();
                    alfaPixelStride[i] = alfa[i].getPixelStride();
                    alfaBandOffsets[i] = alfa[i].getBandOffsets();
                    alfaData[i] = alfa[i].getDoubleDataArrays();
                }
            }
        }

        // Initialize weight type arrays.
        int[] weightTypes = new int[numSources];
        for(int i = 0; i < numSources; i++) {
            weightTypes[i] = WEIGHT_TYPE_THRESHOLD;
            if(alfa[i] != null) {
                weightTypes[i] = WEIGHT_TYPE_ALPHA;
            } else if(sourceROI != null && sourceROI[i] != null) {
                weightTypes[i] = WEIGHT_TYPE_ROI;
            }
        }

        // Set up source offset and data variabls.
        int[] sLineOffsets = new int[numSources];
        int[] sPixelOffsets = new int[numSources];
        double[][] sBandData = new double[numSources][];

        // Set up alpha offset and data variabls.
        int[] aLineOffsets = null;
        int[] aPixelOffsets = null;
        double[][] aBandData = null;
        if(hasAlpha) {
            aLineOffsets = new int[numSources];
            aPixelOffsets = new int[numSources];
            aBandData = new double[numSources][];
        }

        for(int b = 0; b < dstBands; b++) {
            // Initialize source and alpha band array and line offsets.
            for(int s = 0; s < numSources; s++) {
                if(src[s] != null) {
                    sBandData[s] = srcData[s][b];
                    sLineOffsets[s] = srcBandOffsets[s][b];
                }
                if(weightTypes[s] == WEIGHT_TYPE_ALPHA) {
                    aBandData[s] = alfaData[s][0];
                    aLineOffsets[s] = alfaBandOffsets[s][0];
                }
            }

            // Initialize destination band array and line offsets.
            double[] dBandData = dstData[b];
            int dLineOffset = dstBandOffsets[b];

            if(mosaicType == MosaicDescriptor.MOSAIC_TYPE_OVERLAY) {
                for(int dstY = dstMinY; dstY < dstMaxY; dstY++) {
                    // Initialize source and alpha pixel offsets and
                    // update line offsets.
                    for(int s = 0; s < numSources; s++) {
                        if(src[s] != null) {
                            sPixelOffsets[s] = sLineOffsets[s];
                            sLineOffsets[s] += srcLineStride[s];
                        }
                        if(alfa[s] != null) {
                            aPixelOffsets[s] = aLineOffsets[s];
                            aLineOffsets[s] += alfaLineStride[s];
                        }
                    }

                    // Initialize destination pixel offset and update
                    // line offset.
                    int dPixelOffset = dLineOffset;
                    dLineOffset += dstLineStride;

                    for(int dstX = dstMinX; dstX < dstMaxX; dstX++) {

                        // Unset destination update flag.
                        boolean setDestValue = false;

                        // Loop over source until a non-zero weight is
                        // encountered.
                        for(int s = 0; s < numSources; s++) {
                            if(src[s] == null) continue;

                            double sourceValue =
                                sBandData[s][sPixelOffsets[s]];
                            sPixelOffsets[s] += srcPixelStride[s];

                            switch(weightTypes[s]) {
                            case WEIGHT_TYPE_ALPHA:
                                setDestValue =
                                    aBandData[s][aPixelOffsets[s]] != 0;
                                aPixelOffsets[s] += alfaPixelStride[s];
                                break;
                            case WEIGHT_TYPE_ROI:
                                setDestValue =
                                    roi[s].getSample(dstX, dstY, 0) > 0;
                                break;
                            default: // WEIGHT_TYPE_THRESHOLD
                                setDestValue =
                                    sourceValue >=
                                    sourceThreshold[s][b];
                            }

                            // Set the destination value if a non-zero
                            // weight was found.
                            if(setDestValue) {
                                dBandData[dPixelOffset] = sourceValue;

                                // Increment offset of subsequent sources.
                                for(int k = s + 1; k < numSources; k++) {
                                    if(src[k] != null) {
                                        sPixelOffsets[k] += srcPixelStride[k];
                                    }
                                    if(alfa[k] != null) {
                                        aPixelOffsets[k] += alfaPixelStride[k];
                                    }
                                }
                                break;
                            }
                        }

                        // Set the destination value to the background if
                        // no value was set.
                        if(!setDestValue) {
                            dBandData[dPixelOffset] = backgroundValues[b];
                        }

                        dPixelOffset += dstPixelStride;
                    }
                }
            } else { // mosaicType == MosaicDescriptor.MOSAIC_TYPE_BLEND
                for(int dstY = dstMinY; dstY < dstMaxY; dstY++) {
                    // Initialize source and alpha pixel offsets and
                    // update line offsets.
                    for(int s = 0; s < numSources; s++) {
                        if(src[s] != null) {
                            sPixelOffsets[s] = sLineOffsets[s];
                            sLineOffsets[s] += srcLineStride[s];
                        }
                        if(weightTypes[s] == WEIGHT_TYPE_ALPHA) {
                            aPixelOffsets[s] = aLineOffsets[s];
                            aLineOffsets[s] += alfaLineStride[s];
                        }
                    }

                    // Initialize destination pixel offset and update
                    // line offset.
                    int dPixelOffset = dLineOffset;
                    dLineOffset += dstLineStride;

                    for(int dstX = dstMinX; dstX < dstMaxX; dstX++) {

                        // Clear values for blending ratio.
                        double numerator = 0.0F;
                        double denominator = 0.0F;

                        // Accumulate into numerator and denominator.
                        for(int s = 0; s < numSources; s++) {
                            if(src[s] == null) continue;

                            double sourceValue =
                                sBandData[s][sPixelOffsets[s]];
                            sPixelOffsets[s] += srcPixelStride[s];

                            double weight = 0.0F;
                            switch(weightTypes[s]) {
                            case WEIGHT_TYPE_ALPHA:
                                weight = aBandData[s][aPixelOffsets[s]];
                                if(weight > 0.0F && isAlphaBitmask) {
                                    weight = 1.0F;
                                }
                                aPixelOffsets[s] += alfaPixelStride[s];
                                break;
                            case WEIGHT_TYPE_ROI:
                                weight =
                                    roi[s].getSample(dstX, dstY, 0) > 0 ?
                                    1.0F : 0.0F;
                                break;
                            default: // WEIGHT_TYPE_THRESHOLD
                                weight =
                                    sourceValue >=
                                    sourceThreshold[s][b] ?
                                    1.0F : 0.0F;
                            }

                            // Update numerator and denominator.
                            numerator += weight*sourceValue;
                            denominator += weight;
                        }

                        // Clear the background if all weights were zero,
                        // otherwise blend the values.
                        if(denominator == 0.0) {
                            dBandData[dPixelOffset] = backgroundValues[b];
                        } else {
                            dBandData[dPixelOffset] =
                                numerator /
                                denominator;
                        }

                        dPixelOffset += dstPixelStride;
                    }
                }
            }
        }
    }
}
