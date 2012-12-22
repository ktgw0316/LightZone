/*
 * $RCSfile: MlibLookupOpImage.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:55:58 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.mlib;
import java.awt.Rectangle;
import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.util.Map;
import com.lightcrafts.mediax.jai.ImageLayout;
import com.lightcrafts.mediax.jai.LookupTableJAI;
import com.lightcrafts.mediax.jai.PointOpImage;
import com.lightcrafts.media.jai.util.ImageUtil;
import com.lightcrafts.media.jai.util.JDKWorkarounds;
import com.sun.medialib.mlib.*;

final class MlibLookupOpImage extends PointOpImage {
    private LookupTableJAI table;

    public MlibLookupOpImage(RenderedImage source,
                             Map config,
                             ImageLayout layout,
                             LookupTableJAI table) {
        super(source, layout, config, true);

        this.table = table;

        SampleModel sm = source.getSampleModel();	// source sample model

        if (sampleModel.getTransferType() != table.getDataType() ||
            sampleModel.getNumBands() !=
                table.getDestNumBands(sm.getNumBands())) {
            /*
             * The current SampleModel is not suitable for the supplied
             * source and lookup table. Create a suitable SampleModel
             * and ColorModel for the destination image.
             */
            sampleModel = table.getDestSampleModel(sm, tileWidth, tileHeight);
            if(colorModel != null &&
               !JDKWorkarounds.areCompatibleDataModels(sampleModel,
                                                       colorModel)) {
                colorModel = ImageUtil.getCompatibleColorModel(sampleModel,
                                                               config);
            }
        }
        // Set flag to permit in-place operation.
        permitInPlaceOperation();
    }

    protected void computeRect(Raster[] sources,
                               WritableRaster dest,
                               Rectangle destRect) {
        Raster source = sources[0];
        Rectangle srcRect = mapDestRect(destRect, 0);

        int srcTag = MediaLibAccessor.findCompatibleTag(null, source);
        int dstTag = MediaLibAccessor.findCompatibleTag(null, dest);

        SampleModel sm = source.getSampleModel();
        if (sm.getNumBands() > 1) {
            int srcCopy = srcTag & MediaLibAccessor.COPY_MASK;
            int dstCopy = dstTag & MediaLibAccessor.COPY_MASK;

            int srcDtype = srcTag & MediaLibAccessor.DATATYPE_MASK;
            int dstDtype = dstTag & MediaLibAccessor.DATATYPE_MASK;

            if (srcCopy == MediaLibAccessor.UNCOPIED &&
                dstCopy == MediaLibAccessor.UNCOPIED &&
                MediaLibAccessor.isPixelSequential(sm) &&
                MediaLibAccessor.isPixelSequential(sampleModel) &&
                MediaLibAccessor.hasMatchingBandOffsets(
                                 (ComponentSampleModel)sm,
                                 (ComponentSampleModel)sampleModel)) {
            } else {
                srcTag = srcDtype | MediaLibAccessor.COPIED;
                dstTag = dstDtype | MediaLibAccessor.COPIED;
            }
        }

        MediaLibAccessor src = new MediaLibAccessor(source, srcRect, srcTag);
        MediaLibAccessor dst = new MediaLibAccessor(dest, destRect, dstTag);

        mediaLibImage[] srcMLI = src.getMediaLibImages();
        mediaLibImage[] dstMLI = dst.getMediaLibImages();

        if (srcMLI.length < dstMLI.length) {
            mediaLibImage srcMLI0 = srcMLI[0];
            srcMLI = new mediaLibImage[dstMLI.length];

            for (int i = 0 ; i < dstMLI.length; i++) {
                srcMLI[i] = srcMLI0;
            }
        }

        int[] bandOffsets = dst.getBandOffsets();
        Object table = getTableData(bandOffsets);
        int[] offsets = getTableOffsets(bandOffsets);

        for (int i = 0 ; i < dstMLI.length; i++) {
            Image.LookUp2(dstMLI[i], srcMLI[i],
                                          table, offsets);
        }

        if (dst.isDataCopy()) {
            dst.copyDataToRaster();
        }
    }

    private Object getTableData(int[] bandOffsets) {
        int tbands = table.getNumBands();
        int dbands = sampleModel.getNumBands();
        Object data = null;

        switch (table.getDataType()) {
        case DataBuffer.TYPE_BYTE:
            byte[][] bdata = new byte[dbands][];
            if (tbands < dbands) {
                for (int i = 0; i < dbands; i++) {
                    bdata[i] = table.getByteData(0);
                }
            } else {
                for (int i = 0; i < dbands; i++) {
                        bdata[i] = table.getByteData(bandOffsets[i]);
                }
            }
            data = bdata;
            break;
        case DataBuffer.TYPE_USHORT:
        case DataBuffer.TYPE_SHORT:
            short[][] sdata = new short[dbands][];
            if (tbands < dbands) {
                for (int i = 0; i < dbands; i++) {
                    sdata[i] = table.getShortData(0);
                }
            } else {
                for (int i = 0; i < dbands; i++) {
                    sdata[i] = table.getShortData(bandOffsets[i]);
                }
            }
            data = sdata;
            break;
        case DataBuffer.TYPE_INT:
            int[][] idata = new int[dbands][];
            if (tbands < dbands) {
                for (int i = 0; i < dbands; i++) {
                    idata[i] = table.getIntData(0);
                }
            } else {
                for (int i = 0; i < dbands; i++) {
                    idata[i] = table.getIntData(bandOffsets[i]);
                }
            }
            data = idata;
            break;
        case DataBuffer.TYPE_FLOAT:
            float[][] fdata = new float[dbands][];
            if (tbands < dbands) {
                for (int i = 0; i < dbands; i++) {
                    fdata[i] = table.getFloatData(0);
                }
            } else {
                for (int i = 0; i < dbands; i++) {
                    fdata[i] = table.getFloatData(bandOffsets[i]);
                }
            }
            data = fdata;
            break;
        case DataBuffer.TYPE_DOUBLE:
            double[][] ddata = new double[dbands][];
            if (tbands < dbands) {
                for (int i = 0; i < dbands; i++) {
                    ddata[i] = table.getDoubleData(0);
                }
            } else {
                for (int i = 0; i < dbands; i++) {
                    ddata[i] = table.getDoubleData(bandOffsets[i]);
                }
            }
            data = ddata;
            break;
        }

        return data;
    }

    private int[] getTableOffsets(int[] bandOffsets) {
        int tbands = table.getNumBands();
        int dbands = sampleModel.getNumBands();
        int[] offsets = new int[dbands];

        if (tbands < dbands) {
            for (int i = 0; i < dbands; i++) {
                offsets[i] = table.getOffset(0);
            }
        } else {
            for (int i = 0; i < dbands; i++) {
                offsets[i] = table.getOffset(bandOffsets[i]);
            }
        }

        return offsets;
    }
}
