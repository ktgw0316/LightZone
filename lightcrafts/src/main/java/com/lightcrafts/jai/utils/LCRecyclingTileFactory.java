/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.jai.utils;

/*
 * $RCSfile: RecyclingTileFactory.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:18 $
 * $State: Exp $
 */

import org.eclipse.imagen.TileFactory;
import org.eclipse.imagen.TileRecycler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.image.*;
import java.lang.ref.SoftReference;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Observable;

/**
 * A simple implementation of <code>TileFactory</code> wherein the tiles
 * returned from <code>createTile()</code> attempt to re-use primitive
 * arrays provided by the <code>TileRecycler</code> method
 * <code>recycleTile()</code>.
 *
 * <p>
 * A simple example of the use of this class is as follows wherein
 * image files are read, each image is filtered, and each output
 * written to a file:
 * <pre>
 * String[] sourceFiles; // source file paths
 * KernelImageN kernel; // filtering kernel
 *
 * // Create a RenderingHints object and set hints.
 * RenderingHints rh = new RenderingHints(null);
 * RecyclingTileFactory rtf = new RecyclingTileFactory();
 * rh.put(ImageN.KEY_TILE_RECYCLER, rtf);
 * rh.put(ImageN.KEY_TILE_FACTORY, rtf);
 * rh.put(ImageN.KEY_IMAGE_LAYOUT,
 *        new ImageLayout().setTileWidth(32).setTileHeight(32));
 *
 * int counter = 0;
 *
 * // Read each image, filter it, and save the output to a file.
 * for(int i = 0; i < sourceFiles.length; i++) {
 *     PlanarImage source = ImageN.create("fileload", sourceFiles[i]);
 *     ParameterBlock pb =
 *         (new ParameterBlock()).addSource(source).add(kernel);
 *
 *     // The TileFactory hint will cause tiles to be created by 'rtf'.
 *     RenderedOp dest = ImageN.create("convolve", pb, rh);
 *     String fileName = "image_"+(++counter)+".tif";
 *     ImageN.create("filestore", dest, fileName);
 *
 *     // The TileRecycler hint will cause arrays to be reused by 'rtf'.
 *     dest.dispose();
 * }
 * </pre>
 * In the above code, if the <code>SampleModel</code> of all source
 * images is identical, then data arrays should only be created in the
 * first iteration.
 * </p>
 *
 * @since ImageN 1.1.2
 */
public class LCRecyclingTileFactory extends Observable
    implements TileFactory, TileRecycler {

    private static final Logger logger = LoggerFactory.getLogger(LCRecyclingTileFactory.class);

    /**
     * Cache of recycled arrays.  The key in this mapping is a
     * <code>Long</code> which is formed for a given two-dimensional
     * array as
     *
     * <pre>
     * long type;     // DataBuffer.TYPE_*
     * long numBanks; // Number of banks
     * long size;     // Size of each bank
     * Long key = new Long((type << 56) | (numBanks << 32) | size);
     * </pre>
     *
     * where the value of <code>type</code> is one of the constants
     * <code>DataBuffer.TYPE_*</code>.  The value corresponding to each key
     * is an <code>ArrayList</code> of <code>SoftReferences</code> to the
     * internal data banks of <code>DataBuffer</code>s of tiles wherein the
     * data bank array has the type and dimensions implied by the key.
     */
    private HashMap recycledArrays = new HashMap(32);

    /**
     * The amount of memory currently used for array storage.
     */
    private long memoryUsed = 0L;

    // XXX Inline this method or make it public?
    private static long getBufferSizeCSM(ComponentSampleModel csm) {
        int[] bandOffsets = csm.getBandOffsets();
        int maxBandOff=bandOffsets[0];
        for (int i=1; i<bandOffsets.length; i++)
            maxBandOff = Math.max(maxBandOff,bandOffsets[i]);

        long size = 0;
        if (maxBandOff >= 0)
            size += maxBandOff+1;
        int pixelStride = csm.getPixelStride();
        if (pixelStride > 0)
            size += (long) pixelStride * (csm.getWidth() - 1);
        int scanlineStride = csm.getScanlineStride();
        if (scanlineStride > 0)
            size += (long) scanlineStride * (csm.getHeight() - 1);
        return size;
    }

    // XXX Inline this method or make it public?
    private static long getNumBanksCSM(ComponentSampleModel csm) {
        int[] bankIndices = csm.getBankIndices();
        int maxIndex = bankIndices[0];
        for(int i = 1; i < bankIndices.length; i++) {
            int bankIndex = bankIndices[i];
            if(bankIndex > maxIndex) {
                maxIndex = bankIndex;
            }
        }
        return maxIndex + 1;
    }

    /**
     * Returns a <code>SoftReference</code> to the internal bank
     * data of the <code>DataBuffer</code>.
     */
    private static SoftReference getBankReference(DataBuffer db) {
        Object array = null;

        switch(db.getDataType()) {
        case DataBuffer.TYPE_BYTE:
            array = ((DataBufferByte)db).getBankData();
            break;
        case DataBuffer.TYPE_USHORT:
            array = ((DataBufferUShort)db).getBankData();
            break;
        case DataBuffer.TYPE_SHORT:
            array = ((DataBufferShort)db).getBankData();
            break;
        case DataBuffer.TYPE_INT:
            array = ((DataBufferInt)db).getBankData();
            break;
        case DataBuffer.TYPE_FLOAT:
            array = ((DataBufferFloat)db).getBankData();
            break;
        case DataBuffer.TYPE_DOUBLE:
            array = ((DataBufferDouble)db).getBankData();
            break;
        default:
            throw new UnsupportedOperationException("Unsupported Data Type");

        }

        return new SoftReference(array);
    }

    /**
     * Returns the amount of memory (in bytes) used by the supplied data
     * bank array.
     */
    private static long getDataBankSize(int dataType, int numBanks, int size) {
        int bytesPerElement = 0;
        switch(dataType) {
        case DataBuffer.TYPE_BYTE:
            bytesPerElement = 1;
            break;
        case DataBuffer.TYPE_USHORT:
        case DataBuffer.TYPE_SHORT:
            bytesPerElement = 2;
            break;
        case DataBuffer.TYPE_INT:
        case DataBuffer.TYPE_FLOAT:
            bytesPerElement = 4;
            break;
        case DataBuffer.TYPE_DOUBLE:
            bytesPerElement = 8;
            break;
        default:
            throw new UnsupportedOperationException("Unsupported Data Type");

        }

        return (long) numBanks * size * bytesPerElement;
    }

    /**
     * Constructs a <code>RecyclingTileFactory</code>.
     */
    public LCRecyclingTileFactory() {}

    /**
     * Returns <code>true</code>.
     */
    public boolean canReclaimMemory() {
        return true;
    }

    /**
     * Returns <code>true</code>.
     */
    public boolean isMemoryCache() {
        return true;
    }

    public long getMemoryUsed() {
        return memoryUsed;
    }

    public void flush() {
        synchronized(recycledArrays) {
            recycledArrays.clear();
            memoryUsed = 0L;
        }
    }

    public WritableRaster createTile(SampleModel sampleModel,
                                     Point location) {

        if(sampleModel == null) {
            throw new IllegalArgumentException("sampleModel == null!");
        }

        if(location == null) {
           location = new Point(0,0);
        }

        DataBuffer db = null;

        int height = sampleModel.getHeight();
        int type = sampleModel.getTransferType();
        long numBanks = 0;
        long size = 0;

        if(sampleModel instanceof ComponentSampleModel) {
            ComponentSampleModel csm = (ComponentSampleModel)sampleModel;
            numBanks = getNumBanksCSM(csm);
            size = height == 1 ? csm.getScanlineStride()
                               : getBufferSizeCSM(csm);
        } else if(sampleModel instanceof MultiPixelPackedSampleModel) {
            MultiPixelPackedSampleModel mppsm =
                (MultiPixelPackedSampleModel)sampleModel;
            numBanks = 1;
            int dataTypeSize = DataBuffer.getDataTypeSize(type);
            size = (long) mppsm.getScanlineStride() * height +
                (mppsm.getDataBitOffset() + dataTypeSize - 1)/dataTypeSize;
        } else if(sampleModel instanceof SinglePixelPackedSampleModel) {
            SinglePixelPackedSampleModel sppsm =
                (SinglePixelPackedSampleModel)sampleModel;
            numBanks = 1;
            size = height == 1 ? sppsm.getScanlineStride()
                               : (long) sppsm.getScanlineStride() * (height - 1) +
                                       sppsm.getWidth();
        }

        if(size != 0) {
            Object array =
                getRecycledArray(type, numBanks, size);
            if(array != null) {
                switch(type) {
                case DataBuffer.TYPE_BYTE:
                    {
                        byte[][] bankData = (byte[][])array;
                        /*for(int i = 0; i < numBanks; i++) {
                            Arrays.fill(bankData[i], (byte)0);
                        }*/
                        db = new DataBufferByte(bankData, (int)size);
                    }
                    break;
                case DataBuffer.TYPE_USHORT:
                    {
                        short[][] bankData = (short[][])array;
                        /*for(int i = 0; i < numBanks; i++) {
                            Arrays.fill(bankData[i], (short)0);
                        }*/
                        db = new DataBufferUShort(bankData, (int)size);
                    }
                    break;
                case DataBuffer.TYPE_SHORT:
                    {
                        short[][] bankData = (short[][])array;
                        /*for(int i = 0; i < numBanks; i++) {
                            Arrays.fill(bankData[i], (short)0);
                        }*/
                        db = new DataBufferShort(bankData, (int)size);
                    }
                    break;
                case DataBuffer.TYPE_INT:
                    {
                        int[][] bankData = (int[][])array;
                        /*for(int i = 0; i < numBanks; i++) {
                            Arrays.fill(bankData[i], 0);
                        }*/
                        db = new DataBufferInt(bankData, (int)size);
                    }
                    break;
                case DataBuffer.TYPE_FLOAT:
                    {
                        float[][] bankData = (float[][])array;
                        /* for(int i = 0; i < numBanks; i++) {
                            Arrays.fill(bankData[i], 0.0F);
                        }*/
                        db = new DataBufferFloat(bankData, (int)size);
                    }
                    break;
                case DataBuffer.TYPE_DOUBLE:
                    {
                        double[][] bankData = (double[][])array;
                        /*for(int i = 0; i < numBanks; i++) {
                            Arrays.fill(bankData[i], 0.0);
                        }*/
                        db = new DataBufferDouble(bankData, (int)size);
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported Data Type");
                }

                logger.debug("{} Using a recycled array of type: {} array[{}][{}]",
                        getClass().getName(), type, numBanks, size);
            } else {
                logger.debug("{} No type {} array[{}][{}] available",
                        getClass().getName(), type, numBanks, size);
            }
        } else {
            logger.debug("{} Size is zero", getClass().getName());
        }

        if(db == null) {
            logger.debug("{} Creating new DataBuffer", getClass().getName());
            db = sampleModel.createDataBuffer();
        }

        return Raster.createWritableRaster(sampleModel, db, location);
    }

    /**
     * Recycle the given tile.
     */
    public void recycleTile(Raster tile) {
        DataBuffer db = tile.getDataBuffer();

        Long key = (long)db.getDataType() << 56
                | (long)db.getNumBanks() << 32
                | (long)db.getSize();

        logger.debug("Recycling array for: {} {} {}",
                db.getDataType(), db.getNumBanks(), db.getSize());

        synchronized(recycledArrays) {
            Object value = recycledArrays.get(key);
            ArrayList arrays = null;
            if(value != null) {
                arrays = (ArrayList)value;
            } else {
                arrays = new ArrayList();
            }

            memoryUsed += getDataBankSize(db.getDataType(),
                                          db.getNumBanks(),
                                          db.getSize());

            arrays.add(getBankReference(db));

            if(value == null) {
                recycledArrays.put(key, arrays);
            }
        }
    }

    /**
     * Retrieve an array of the specified type and length.
     */
    private Object getRecycledArray(int arrayType,
                                    long numBanks,
                                    long arrayLength) {
        final Long key = (long)arrayType << 56 | numBanks << 32 | arrayLength;

        logger.debug("Attempting to get array for: {} {} {}", arrayType, numBanks, arrayLength);

        synchronized(recycledArrays) {
            Object value = recycledArrays.get(key);

            if(value != null) {
                ArrayList arrays = (ArrayList)value;
                for(int idx = arrays.size() - 1; idx >= 0; idx--) {
                    SoftReference bankRef = (SoftReference)arrays.remove(idx);
                    memoryUsed -= getDataBankSize(arrayType,
                                                  (int)numBanks,
                                                  (int)arrayLength);
                    if(idx == 0) {
                        recycledArrays.remove(key);
                    }

                    Object array = bankRef.get();
                    if(array != null) {
                        return array;
                    }

                    logger.debug("null reference");
                }
            }
        }

        // array is null
        switch(arrayType) {
        case DataBuffer.TYPE_BYTE:
            return Array.newInstance(byte.class,
                                      new int[]{(int)numBanks, (int)arrayLength});
        case DataBuffer.TYPE_USHORT:
        case DataBuffer.TYPE_SHORT:
            return Array.newInstance(short.class,
                                      new int[]{(int)numBanks, (int)arrayLength});
        case DataBuffer.TYPE_INT:
            return Array.newInstance(int.class,
                                      new int[]{(int)numBanks, (int)arrayLength});
        case DataBuffer.TYPE_FLOAT:
            return Array.newInstance(float.class,
                                      new int[]{(int)numBanks, (int)arrayLength});
        case DataBuffer.TYPE_DOUBLE:
            return Array.newInstance(double.class,
                                      new int[]{(int)numBanks, (int)arrayLength});
        default:
            //throw new IllegalArgumentException("Unsupported Data Type");
            return null;
        }
    }
}
