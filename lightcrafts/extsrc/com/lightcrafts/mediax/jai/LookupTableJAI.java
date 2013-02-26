/*
 * $RCSfile: LookupTableJAI.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:12 $
 * $State: Exp $
 */
package com.lightcrafts.mediax.jai;
import java.awt.Rectangle;
import java.awt.Point;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferShort;
import java.awt.image.DataBufferUShort;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import com.lightcrafts.mediax.jai.remote.SerializableState;
import com.lightcrafts.mediax.jai.remote.SerializerFactory;
import com.lightcrafts.media.jai.util.DataBufferUtils;

/**
 * A lookup table object associated with the "Lookup" operation.  The
 * "Lookup" operation is described in
 * <code>com.lightcrafts.mediax.jai.operator.LookupDescriptor</code>.
 *
 * <p>This object represents a single- or multi-banded table of any
 * JAI supported data type. A single- or multi-banded source image
 * of integral data types is passed through the table and transformed
 * into a single- or multi-banded destination image of either integral
 * and floating point data types.
 *
 * <p>The table data may cover only a subrange of the legal range of the
 * input data type. The subrange is selected by means of an offset parameter
 * which is to be subtracted from the input value before indexing into the
 * table array. When only a subranged table is used with a source image, it
 * is up to the user to make certain that the source image does not have
 * pixel values outside of the table range. Otherwise,
 * an ArrayIndexOutOfBoundsException can occur.
 *
 * <p>The table data is saved by reference only.
 *
 * @see com.lightcrafts.mediax.jai.operator.LookupDescriptor
 *
 */
public class LookupTableJAI extends Object implements Serializable {

    /** The table data. */
    transient DataBuffer data;

    /** The band offset values */
    private int[] tableOffsets;

    /**
     * Constructs a single-banded byte lookup table. The index offset is 0.
     *
     * @param data  The single-banded byte data.
     * @throws IllegalArgumentException if data is null.
     */
    public LookupTableJAI(byte[] data) {
        if ( data == null ) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        this.data = new DataBufferByte(data, data.length);
        this.initOffsets(1, 0);
    }

    /**
     * Constructs a single-banded byte lookup table with an index offset.
     *
     * @param data    The single-banded byte data.
     * @param offset  The offset.
     * @throws IllegalArgumentException if data is null.
     */
    public LookupTableJAI(byte[] data, int offset) {
        if ( data == null ) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        this.initOffsets(1, offset);
        this.data = new DataBufferByte(data, data.length);
    }

    /**
     * Constructs a multi-banded byte lookup table. The index offset for
     * each band is 0.
     *
     * @param data  The multi-banded byte data in [band][index] format.
     * @throws IllegalArgumentException if data is null.
     */
    public LookupTableJAI(byte[][] data) {
        if ( data == null ) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        this.initOffsets(data.length, 0);
        this.data = new DataBufferByte(data, data[0].length);
    }

    /**
     * Constructs a multi-banded byte lookup table where all bands have
     * the same index offset.
     *
     * @param data    The multi-banded byte data in [band][index] format.
     * @param offset  The common offset for all bands.
     * @throws IllegalArgumentException if data is null.
     */
    public LookupTableJAI(byte[][] data, int offset) {
        if ( data == null ) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        this.initOffsets(data.length, offset);
        this.data = new DataBufferByte(data, data[0].length);
    }

    /**
     * Constructs a multi-banded byte lookup table where each band has
     * a different index offset.
     *
     * @param data     The multi-banded byte data in [band][index] format.
     * @param offsets  The offsets for the bands.
     * @throws IllegalArgumentException if data is null.
     */
    public LookupTableJAI(byte[][] data, int[] offsets) {
        if ( data == null ) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        this.initOffsets(data.length, offsets);
        this.data = new DataBufferByte(data, data[0].length);
    }

    /**
     * Constructs a single-banded short or unsigned short lookup table.
     * The index offset is 0.
     *
     * @param data      The single-banded short data.
     * @param isUShort  True if data type is DataBuffer.TYPE_USHORT;
     *                  false if data type is DataBuffer.TYPE_SHORT.
     * @throws IllegalArgumentException if data is null.
     */
    public LookupTableJAI(short[] data, boolean isUShort) {
        if ( data == null ) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        this.initOffsets(1, 0);
        if (isUShort) {
            this.data = new DataBufferUShort(data, data.length);
        } else {
            this.data = new DataBufferShort(data, data.length);
        }
    }

    /**
     * Constructs a single-banded short or unsigned short lookup table with
     * an index offset.
     *
     * @param data      The single-banded short data.
     * @param offset    The offset.
     * @param isUShort  True if data type is DataBuffer.TYPE_USHORT;
     *                  false if data type is DataBuffer.TYPE_SHORT.
     * @throws IllegalArgumentException if data is null.
     */
    public LookupTableJAI(short[] data, int offset, boolean isUShort) {
        if ( data == null ) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        this.initOffsets(1, offset);
        if (isUShort) {
            this.data = new DataBufferUShort(data, data.length);
        } else {
            this.data = new DataBufferShort(data, data.length);
        }
    }

    /**
     * Constructs a multi-banded short or unsigned short lookup table.
     * The index offset for each band is 0.
     *
     * @param data      The multi-banded short data in [band][index] format.
     * @param isUShort  True if data type is DataBuffer.TYPE_USHORT;
     *                  false if data type is DataBuffer.TYPE_SHORT.
     * @throws IllegalArgumentException if data is null.
     */
    public LookupTableJAI(short[][] data, boolean isUShort) {
        if ( data == null ) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        this.initOffsets(data.length, 0);
        if (isUShort) {
            this.data = new DataBufferUShort(data, data[0].length);
        } else {
            this.data = new DataBufferShort(data, data[0].length);
        }
    }

    /**
     * Constructs a multi-banded short or unsigned short lookup table where all
     * bands have the same index offset.
     *
     * @param data      The multi-banded short data in [band][index] format.
     * @param offset    The common offset for all bands.
     * @param isUShort  True if data type is DataBuffer.TYPE_USHORT;
     *                  false if data type is DataBuffer.TYPE_SHORT.
     * @throws IllegalArgumentException if data is null.
     */
    public LookupTableJAI(short[][] data, int offset, boolean isUShort) {
        if ( data == null ) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        this.initOffsets(data.length, offset);
        if (isUShort) {
            this.data = new DataBufferUShort(data, data[0].length);
        } else {
            this.data = new DataBufferShort(data, data[0].length);
        }
    }

    /**
     * Constructs a multi-banded short or unsigned short lookup table where
     * each band has a different index offset.
     *
     * @param data      The multi-banded short data in [band][index] format.
     * @param offsets   The offsets for the bands.
     * @param isUShort  True if data type is DataBuffer.TYPE_USHORT;
     *                  false if data type is DataBuffer.TYPE_SHORT.
     * @throws IllegalArgumentException if data is null.
     */
    public LookupTableJAI(short[][] data, int[] offsets, boolean isUShort) {
        if ( data == null ) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        this.initOffsets(data.length, offsets);

        if (isUShort) {
            this.data = new DataBufferUShort(data, data[0].length);
        } else {
            this.data = new DataBufferShort(data, data[0].length);
        }
    }

    /**
     * Constructs a single-banded int lookup table. The index offset is 0.
     *
     * @param data  The single-banded int data.
     * @throws IllegalArgumentException if data is null.
     */
    public LookupTableJAI(int[] data) {
        if ( data == null ) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        this.initOffsets(1, 0);
        this.data = new DataBufferInt(data, data.length);
    }

    /**
     * Constructs a single-banded int lookup table with an index offset.
     *
     * @param data    The single-banded int data.
     * @param offset  The offset.
     * @throws IllegalArgumentException if data is null.
     */
    public LookupTableJAI(int[] data, int offset) {
        if ( data == null ) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        this.initOffsets(1, offset);
        this.data = new DataBufferInt(data, data.length);
    }

    /**
     * Constructs a multi-banded int lookup table. The index offset for
     * each band is 0.
     *
     * @param data  The multi-banded int data in [band][index] format.
     * @throws IllegalArgumentException if data is null.
     */
    public LookupTableJAI(int[][] data) {
        if ( data == null ) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        this.initOffsets(data.length, 0);
        this.data = new DataBufferInt(data, data[0].length);
    }

    /**
     * Constructs a multi-banded int lookup table where all bands have
     * the same index offset.
     *
     * @param data    The multi-banded int data in [band][index] format.
     * @param offset  The common offset for all bands.
     * @throws IllegalArgumentException if data is null.
     */
    public LookupTableJAI(int[][] data, int offset) {
        if ( data == null ) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        this.initOffsets(data.length, offset);
        this.data = new DataBufferInt(data, data[0].length);
    }

    /**
     * Constructs a multi-banded int lookup table where each band has
     * a different index offset.
     *
     * @param data     The multi-banded int data in [band][index] format.
     * @param offsets  The offsets for the bands.
     * @throws IllegalArgumentException if data is null.
     */
    public LookupTableJAI(int[][] data, int[] offsets) {
        if ( data == null ) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        this.initOffsets(data.length, offsets);
        this.data = new DataBufferInt(data, data[0].length);
    }

    /**
     * Constructs a single-banded float lookup table. The index offset is 0.
     *
     * @param data  The single-banded float data.
     * @throws IllegalArgumentException if data is null.
     */
    public LookupTableJAI(float[] data) {
        if ( data == null ) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        this.initOffsets(1, 0);
        this.data = DataBufferUtils.createDataBufferFloat(data, data.length);
    }

    /**
     * Constructs a single-banded float lookup table with an index offset.
     *
     * @param data    The single-banded float data.
     * @param offset  The offset.
     * @throws IllegalArgumentException if data is null.
     */
    public LookupTableJAI(float[] data, int offset) {
        if ( data == null ) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        this.initOffsets(1, offset);
        this.data = DataBufferUtils.createDataBufferFloat(data, data.length);
    }

    /**
     * Constructs a multi-banded float lookup table. The index offset for
     * each band is 0.
     *
     * @param data  The multi-banded float data in [band][index] format.
     * @throws IllegalArgumentException if data is null.
     */
    public LookupTableJAI(float[][] data) {
        if ( data == null ) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        this.initOffsets(data.length, 0);
        this.data = DataBufferUtils.createDataBufferFloat(data, data[0].length);
    }

    /**
     * Constructs a multi-banded float lookup table where all bands have
     * the same index offset.
     *
     * @param data    The multi-banded float data in [band][index] format.
     * @param offset  The common offset for all bands.
     * @throws IllegalArgumentException if data is null.
     */
    public LookupTableJAI(float[][] data, int offset) {
        if ( data == null ) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        this.initOffsets(data.length, offset);
        this.data = DataBufferUtils.createDataBufferFloat(data, data[0].length);
    }

    /**
     * Constructs a multi-banded float lookup table where each band has
     * a different index offset.
     *
     * @param data     The multi-banded float data in [band][index] format.
     * @param offsets  The offsets for the bands.
     * @throws IllegalArgumentException if data is null.
     */
    public LookupTableJAI(float[][] data, int[] offsets) {
        if ( data == null ) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        this.initOffsets(data.length, offsets);
        this.data = DataBufferUtils.createDataBufferFloat(data, data[0].length);
    }

    /**
     * Constructs a single-banded double lookup table. The index offset is 0.
     *
     * @param data  The single-banded double data.
     * @throws IllegalArgumentException if data is null.
     */
    public LookupTableJAI(double[] data) {
        if ( data == null ) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        this.initOffsets(1, 0);
        this.data = DataBufferUtils.createDataBufferDouble(data, data.length);
    }

    /**
     * Constructs a single-banded double lookup table with an index offset.
     *
     * @param data    The single-banded double data.
     * @param offset  The offset.
     * @throws IllegalArgumentException if data is null.
     */
    public LookupTableJAI(double[] data, int offset) {
        if ( data == null ) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        this.initOffsets(1, offset);
        this.data = DataBufferUtils.createDataBufferDouble(data, data.length);
    }

    /**
     * Constructs a multi-banded double lookup table. The index offset for
     * each band is 0.
     *
     * @param data  The multi-banded double data in [band][index] format.
     * @throws IllegalArgumentException if data is null.
     */
    public LookupTableJAI(double[][] data) {
        if ( data == null ) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        this.initOffsets(data.length, 0);
        this.data = DataBufferUtils.createDataBufferDouble(data, data[0].length);
    }

    /**
     * Constructs a multi-banded double lookup table where all bands have
     * the same index offset.
     *
     * @param data    The multi-banded double data in [band][index] format.
     * @param offset  The common offset for all bands.
     * @throws IllegalArgumentException if data is null.
     */
    public LookupTableJAI(double[][] data, int offset) {
        if ( data == null ) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        this.initOffsets(data.length, offset);
        this.data = DataBufferUtils.createDataBufferDouble(data, data[0].length);
    }

    /**
     * Constructs a multi-banded double lookup table where each band has
     * a different index offset.
     *
     * @param data     The multi-banded double data in [band][index] format.
     * @param offsets  The offsets for the bands.
     * @throws IllegalArgumentException if data is null.
     */
    public LookupTableJAI(double[][] data, int[] offsets) {
        if ( data == null ) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        this.initOffsets(data.length, offsets);
        this.data = DataBufferUtils.createDataBufferDouble(data, data[0].length);
    }

    /**
     * Returns the table data as a DataBuffer.
     */
    public DataBuffer getData() {
        return data;
    }

    /**
     * Returns the byte table data in array format, or null if the
     * table's data type is not byte.
     */
    public byte[][] getByteData() {
        return data instanceof DataBufferByte ?
               ((DataBufferByte)data).getBankData() : null;
    }

    /**
     * Returns the byte table data of a specific band in array format,
     * or null if the table's data type is not byte.
     */
    public byte[] getByteData(int band) {
        return data instanceof DataBufferByte ?
               ((DataBufferByte)data).getData(band) : null;
    }

    /**
     * Returns the short table data in array format, or null if the
     * table's data type is not short. This includes both signed and
     * unsigned short table data.
     *
     */
    public short[][] getShortData() {
        if (data instanceof DataBufferUShort) {
            return ((DataBufferUShort)data).getBankData();
        } else if (data instanceof DataBufferShort) {
            return ((DataBufferShort)data).getBankData();
        } else {
            return null;
        }
    }

    /**
     * Returns the short table data of a specific band in array format,
     * or null if the table's data type is not short.
     *
     */
    public short[] getShortData(int band) {
        if (data instanceof DataBufferUShort) {
            return ((DataBufferUShort)data).getData(band);
        } else if (data instanceof DataBufferShort) {
            return ((DataBufferShort)data).getData(band);
        } else {
            return null;
        }
    }

    /**
     * Returns the integer table data in array format, or null if the
     * table's data type is not int.
     *
     */
    public int[][] getIntData() {
        return data instanceof DataBufferInt ?
               ((DataBufferInt)data).getBankData() : null;
    }

    /**
     * Returns the integer table data of a specific band in array format,
     * or null if table's data type is not int.
     *
     */
    public int[] getIntData(int band) {
        return data instanceof DataBufferInt ?
               ((DataBufferInt)data).getData(band) : null;
    }

    /**
     * Returns the float table data in array format, or null if the
     * table's data type is not float.
     *
     */
    public float[][] getFloatData() {
        return data.getDataType() == DataBuffer.TYPE_FLOAT ?
               DataBufferUtils.getBankDataFloat(data) : null;
    }

    /**
     * Returns the float table data of a specific band in array format,
     * or null if table's data type is not float.
     *
     */
    public float[] getFloatData(int band) {
        return data.getDataType() == DataBuffer.TYPE_FLOAT ?
               DataBufferUtils.getDataFloat(data, band) : null;
    }

    /**
     * Returns the double table data in array format, or null if the
     * table's data type is not double.
     *
     */
    public double[][] getDoubleData() {
        return data.getDataType() == DataBuffer.TYPE_DOUBLE ?
               DataBufferUtils.getBankDataDouble(data) : null;
    }

    /**
     * Returns the double table data of a specific band in array format,
     * or null if table's data type is not double.
     *
     */
    public double[] getDoubleData(int band) {
        return data.getDataType() == DataBuffer.TYPE_DOUBLE ?
               DataBufferUtils.getDataDouble(data, band) : null;
    }

    /** Returns the index offsets of entry 0 for all bands. */
    public int[] getOffsets() {
        return tableOffsets;
    }

    /**
     * Returns the index offset of entry 0 for the default band.
     *
     */
    public int getOffset() {
        return tableOffsets[0];
    }

    /**
     * Returns the index offset of entry 0 for a specific band.
     *
     */
    public int getOffset(int band) {
        return tableOffsets[band];
    }

    /** Returns the number of bands of the table. */
    public int getNumBands() {
        return data.getNumBanks();
    }

    /**
     * Returns the number of entries per band of the table.
     *
     */
    public int getNumEntries() {
        return data.getSize();
    }

    /** Returns the data type of the table data.
     *
     */
    public int getDataType() {
        return data.getDataType();
    }

    /**
     * Returns the number of bands of the destination image, based on
     * the number of bands of the source image and lookup table.
     *
     * @param srcNumBands  The number of bands of the source image.
     * @return the number of bands in destination image.
     */
    public int getDestNumBands(int srcNumBands) {
        int tblNumBands = getNumBands();
        return srcNumBands == 1 ? tblNumBands : srcNumBands;
    }

    /**
     * Returns a <code>SampleModel</code> suitable for holding the output
     * of a lookup operation on the source data described by a given
     * SampleModel with this table. The width and height of the destination
     * SampleModel are the same as that of the source. This method will
     * return null if the source SampleModel has a non-integral data type.
     *
     * @param srcSampleModel  The SampleModel of the source image.
     *
     * @throws IllegalArgumentException if srcSampleModel is null.
     * @return sampleModel suitable for the destination image.
     */
    public SampleModel getDestSampleModel(SampleModel srcSampleModel) {
        if ( srcSampleModel == null ) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        return getDestSampleModel(srcSampleModel,
                                  srcSampleModel.getWidth(),
                                  srcSampleModel.getHeight());
    }

    /**
     * Returns a <code>SampleModel</code> suitable for holding the output
     * of a lookup operation on the source data described by a given
     * SampleModel with this table. This method will return null if the
     * source SampleModel has a non-integral data type.
     *
     * @param srcSampleModel  The SampleModel of the source image.
     * @param width           The width of the destination SampleModel.
     * @param height          The height of the destination SampleModel.
     *
     * @throws IllegalArgumentException if srcSampleModel is null.
     * @return sampleModel suitable for the destination image.
     */
    public SampleModel getDestSampleModel(SampleModel srcSampleModel,
                                          int width,
                                          int height) {
        if ( srcSampleModel == null ) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        if (!isIntegralDataType(srcSampleModel)) {
            return null;	// source has non-integral data type
        }

        return RasterFactory.createComponentSampleModel(srcSampleModel,
                       getDataType(), width, height,
                       getDestNumBands(srcSampleModel.getNumBands()));
    }

    /**
     * Validates data type. Returns true if it's one of the integral
     * data types; false otherwise.
     *
     * @throws IllegalArgumentException if sampleModel is null.
     */
    public boolean isIntegralDataType(SampleModel sampleModel) {
        if ( sampleModel == null ) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        return isIntegralDataType(sampleModel.getTransferType());
    }

    /**
     * Returns <code>true</code> if the specified data type is
     * an integral data type, such as byte, ushort, short, or int.
     */
    public boolean isIntegralDataType(int dataType) {
        if ((dataType == DataBuffer.TYPE_BYTE) ||
            (dataType == DataBuffer.TYPE_USHORT) ||
            (dataType == DataBuffer.TYPE_SHORT) ||
            (dataType == DataBuffer.TYPE_INT)){
            return true;
        } else {
            return false;
        }
    }

    /**
     * Performs lookup on a given value belonging to a given source
     * band, and returns the result as an int.
     *
     * @param band   The source band the value is from.
     * @param value  The source value to be placed through the lookup table.
     */
    public int lookup(int band, int value) {
        return data.getElem(band, value-tableOffsets[band]);
    }

    /**
     * Performs lookup on a given value belonging to a given source
     * band, and returns the result as a float.
     *
     * @param band   The source band the value is from.
     * @param value  The source value to be placed through the lookup table.
     */
    public float lookupFloat(int band, int value) {
        return data.getElemFloat(band, value-tableOffsets[band]);
    }

    /**
     * Performs lookup on a given value belonging to a given source
     * band, and returns the result as a double.
     *
     * @param band   The source band the value is from.
     * @param value  The source value to be placed through the lookup table.
     */
    public double lookupDouble(int band, int value) {
        return data.getElemDouble(band, value-tableOffsets[band]);
    }

    /**
     * Performs table lookup in place on a given WritableRaster. The
     * The lookup operation must preserve the data type and
     * SampleModel of the source.  A reference to the supplied
     * WritableRaster will be returned.
     *
     * @throws IllegalArgumentException if the src is null.
     * @throws IllegalArgumentException if the source's SampleModel
     *         is not of integral type.
     * @throws IllegalArgumentException if the lookup operation would
     *         result in a change in the data type or number of bands
     *         of the Raster.
     */
    public WritableRaster lookup(WritableRaster src) {
        if ( src == null ) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        return lookup(src, src, src.getBounds());
    }

    /**
     * Performs table lookup on a source Raster, writing the result
     * into a supplied WritableRaster. The destination must have a
     * data type and SampleModel appropriate to the results of the
     * lookup operation.  The table lookup operation is performed
     * within a specified rectangle.
     *
     * <p> The <code>dst</code> argument may be null, in which case a new
     * WritableRaster is created using the appropriate SampleModel.
     *
     * <p> The rectangle of interest may be null, in which case the
     * operation will be performed on the intersection of the source
     * and destination bounding rectangles.
     *
     * @param src   A Raster containing the source pixel data.
     * @param dst   The WritableRaster to be computed, or null.
     *              If supplied, its data type and number of bands must
     *              be suitable for the source and lookup table.
     * @param rect  The rectangle within the tile to be computed.
     *              If rect is null, the intersection of the source and
     *              destination bounds will be used.  Otherwise, it
     *              will be clipped to the intersection of the source
     *              and destination bounds.
     * @return      A reference to the supplied WritableRaster, or to a
     *              new WritableRaster if the supplied one was null.
     *
     * @throws IllegalArgumentException if the source is null.
     * @throws IllegalArgumentException if the source's SampleModel
     *         is not of integral type.
     * @throws IllegalArgumentException if the destination's data type
     *         or number of bands differ from those returned by
     *         getDataType() and getDestNumBands().
     */
    public WritableRaster lookup(Raster src,
                                 WritableRaster dst,
                                 Rectangle rect) {
        // Validate source.
        if (src == null) {
            throw
	      new IllegalArgumentException(JaiI18N.getString("LookupTableJAI1"));
        }

        SampleModel srcSampleModel = src.getSampleModel();
        if (!isIntegralDataType(srcSampleModel)) {
            throw
	      new IllegalArgumentException(JaiI18N.getString("LookupTableJAI2"));
        }

        // Validate rectangle.
        if (rect == null) {
            rect = src.getBounds();
        } else {
            rect = rect.intersection(src.getBounds());
        }

        if (dst != null) {
            rect = rect.intersection(dst.getBounds());
        }

        // Validate destination.
        SampleModel dstSampleModel;
        if (dst == null) {	// create dst according to table
            dstSampleModel = getDestSampleModel(srcSampleModel,
                                                rect.width, rect.height);
            dst =
                RasterFactory.createWritableRaster(dstSampleModel,
                                                   new Point(rect.x, rect.y));
        } else {
            dstSampleModel = dst.getSampleModel();

            if (dstSampleModel.getTransferType() != getDataType() ||
                dstSampleModel.getNumBands() !=
                getDestNumBands(srcSampleModel.getNumBands())) {
                throw new
		  IllegalArgumentException(JaiI18N.getString("LookupTableJAI3"));
            }
        }

	// Add bit support?
        int sTagID = RasterAccessor.findCompatibleTag(null, srcSampleModel);
        int dTagID = RasterAccessor.findCompatibleTag(null, dstSampleModel);

        RasterFormatTag sTag = new RasterFormatTag(srcSampleModel,sTagID);
        RasterFormatTag dTag = new RasterFormatTag(dstSampleModel,dTagID);

        RasterAccessor s = new RasterAccessor(src, rect, sTag, null);
        RasterAccessor d = new RasterAccessor(dst, rect, dTag, null);

        int srcNumBands = s.getNumBands();
        int srcDataType = s.getDataType();

        int tblNumBands = getNumBands();
        int tblDataType = getDataType();

        int dstWidth = d.getWidth();
        int dstHeight = d.getHeight();
        int dstNumBands = d.getNumBands();
        int dstDataType = d.getDataType();

        // Source information.
        int srcLineStride = s.getScanlineStride();
        int srcPixelStride = s.getPixelStride();
        int[] srcBandOffsets = s.getBandOffsets();

        byte[][] bSrcData = s.getByteDataArrays();
        short[][] sSrcData = s.getShortDataArrays();
        int[][] iSrcData = s.getIntDataArrays();

        if (srcNumBands < dstNumBands) {
            int offset0 = srcBandOffsets[0];
            srcBandOffsets = new int[dstNumBands];
            for (int i = 0; i < dstNumBands; i++) {
                srcBandOffsets[i] = offset0;
            }

            switch (srcDataType) {
            case DataBuffer.TYPE_BYTE:
                byte[] bData0 = bSrcData[0];
                bSrcData = new byte[dstNumBands][];
                for (int i = 0; i < dstNumBands; i++) {
                    bSrcData[i] = bData0;
                }
                break;
            case DataBuffer.TYPE_USHORT:
            case DataBuffer.TYPE_SHORT:
                short[] sData0 = sSrcData[0];
                sSrcData = new short[dstNumBands][];
                for (int i = 0; i < dstNumBands; i++) {
                    sSrcData[i] = sData0;
                }
                break;
            case DataBuffer.TYPE_INT:
                int[] iData0 = iSrcData[0];
                iSrcData = new int[dstNumBands][];
                for (int i = 0; i < dstNumBands; i++) {
                    iSrcData[i] = iData0;
                }
                break;
            }
        }

        // Table information.
        int[] tblOffsets = getOffsets();

        byte[][] bTblData = getByteData();
        short[][] sTblData = getShortData();
        int[][] iTblData = getIntData();
        float[][] fTblData = getFloatData();
        double[][] dTblData = getDoubleData();

        if (tblNumBands < dstNumBands) {
            int offset0 = tblOffsets[0];
            tblOffsets = new int[dstNumBands];
            for (int i = 0; i < dstNumBands; i++) {
                tblOffsets[i] = offset0;
            }

            switch (tblDataType) {
            case DataBuffer.TYPE_BYTE:
                byte[] bData0 = bTblData[0];
                bTblData = new byte[dstNumBands][];
                for (int i = 0; i < dstNumBands; i++) {
                    bTblData[i] = bData0;
                }
                break;
            case DataBuffer.TYPE_USHORT:
            case DataBuffer.TYPE_SHORT:
                short[] sData0 = sTblData[0];
                sTblData = new short[dstNumBands][];
                for (int i = 0; i < dstNumBands; i++) {
                    sTblData[i] = sData0;
                }
                break;
            case DataBuffer.TYPE_INT:
                int[] iData0 = iTblData[0];
                iTblData = new int[dstNumBands][];
                for (int i = 0; i < dstNumBands; i++) {
                    iTblData[i] = iData0;
                }
                break;
            case DataBuffer.TYPE_FLOAT:
                float[] fData0 = fTblData[0];
                fTblData = new float[dstNumBands][];
                for (int i = 0; i < dstNumBands; i++) {
                    fTblData[i] = fData0;
                }
                break;
            case DataBuffer.TYPE_DOUBLE:
                double[] dData0 = dTblData[0];
                dTblData = new double[dstNumBands][];
                for (int i = 0; i < dstNumBands; i++) {
                    dTblData[i] = dData0;
                }
            }
        }

        // Destination information.
        int dstLineStride = d.getScanlineStride();
        int dstPixelStride = d.getPixelStride();
        int[] dstBandOffsets = d.getBandOffsets();

        byte[][] bDstData = d.getByteDataArrays();
        short[][] sDstData = d.getShortDataArrays();
        int[][] iDstData = d.getIntDataArrays();
        float[][] fDstData = d.getFloatDataArrays();
        double[][] dDstData = d.getDoubleDataArrays();

        switch (dstDataType) {
        case DataBuffer.TYPE_BYTE:
            switch (srcDataType) {
            case DataBuffer.TYPE_BYTE:
                lookup(srcLineStride, srcPixelStride,
                       srcBandOffsets, bSrcData,
                       dstWidth, dstHeight, dstNumBands,
                       dstLineStride, dstPixelStride,
                       dstBandOffsets, bDstData,
                       tblOffsets, bTblData);
                break;

            case DataBuffer.TYPE_USHORT:
                lookupU(srcLineStride, srcPixelStride,
                        srcBandOffsets, sSrcData,
                        dstWidth, dstHeight, dstNumBands,
                        dstLineStride, dstPixelStride,
                        dstBandOffsets, bDstData,
                        tblOffsets, bTblData);
                break;

            case DataBuffer.TYPE_SHORT:
                lookup(srcLineStride, srcPixelStride,
                       srcBandOffsets, sSrcData,
                       dstWidth, dstHeight, dstNumBands,
                       dstLineStride, dstPixelStride,
                       dstBandOffsets, bDstData,
                       tblOffsets, bTblData);
                break;

            case DataBuffer.TYPE_INT:
                lookup(srcLineStride, srcPixelStride,
                       srcBandOffsets, iSrcData,
                       dstWidth, dstHeight, dstNumBands,
                       dstLineStride, dstPixelStride,
                       dstBandOffsets, bDstData,
                       tblOffsets, bTblData);
                break;
            }
            break;

        case DataBuffer.TYPE_USHORT:
        case DataBuffer.TYPE_SHORT:
            switch (srcDataType) {
            case DataBuffer.TYPE_BYTE:
                lookup(srcLineStride, srcPixelStride,
                       srcBandOffsets, bSrcData,
                       dstWidth, dstHeight, dstNumBands,
                       dstLineStride, dstPixelStride,
                       dstBandOffsets, sDstData,
                       tblOffsets, sTblData);
                break;

            case DataBuffer.TYPE_USHORT:
                lookupU(srcLineStride, srcPixelStride,
                        srcBandOffsets, sSrcData,
                        dstWidth, dstHeight, dstNumBands,
                        dstLineStride, dstPixelStride,
                        dstBandOffsets, sDstData,
                        tblOffsets, sTblData);
                break;

            case DataBuffer.TYPE_SHORT:
                lookup(srcLineStride, srcPixelStride,
                       srcBandOffsets, sSrcData,
                       dstWidth, dstHeight, dstNumBands,
                       dstLineStride, dstPixelStride,
                       dstBandOffsets, sDstData,
                       tblOffsets, sTblData);
                break;

            case DataBuffer.TYPE_INT:
                lookup(srcLineStride, srcPixelStride,
                       srcBandOffsets, iSrcData,
                       dstWidth, dstHeight, dstNumBands,
                       dstLineStride, dstPixelStride,
                       dstBandOffsets, sDstData,
                       tblOffsets, sTblData);
                break;
            }
            break;

        case DataBuffer.TYPE_INT:
            switch (srcDataType) {
            case DataBuffer.TYPE_BYTE:
                lookup(srcLineStride, srcPixelStride,
                       srcBandOffsets, bSrcData,
                       dstWidth, dstHeight, dstNumBands,
                       dstLineStride, dstPixelStride,
                       dstBandOffsets, iDstData,
                       tblOffsets, iTblData);
                break;

            case DataBuffer.TYPE_USHORT:
                lookupU(srcLineStride, srcPixelStride,
                        srcBandOffsets, sSrcData,
                        dstWidth, dstHeight, dstNumBands,
                        dstLineStride, dstPixelStride,
                        dstBandOffsets, iDstData,
                        tblOffsets, iTblData);
                break;

            case DataBuffer.TYPE_SHORT:
                lookup(srcLineStride, srcPixelStride,
                       srcBandOffsets, sSrcData,
                       dstWidth, dstHeight, dstNumBands,
                       dstLineStride, dstPixelStride,
                       dstBandOffsets, iDstData,
                       tblOffsets, iTblData);
                break;

            case DataBuffer.TYPE_INT:
                lookup(srcLineStride, srcPixelStride,
                       srcBandOffsets, iSrcData,
                       dstWidth, dstHeight, dstNumBands,
                       dstLineStride, dstPixelStride,
                       dstBandOffsets, iDstData,
                       tblOffsets, iTblData);
                break;
            }
            break;

        case DataBuffer.TYPE_FLOAT:
            switch (srcDataType) {
            case DataBuffer.TYPE_BYTE:
                lookup(srcLineStride, srcPixelStride,
                       srcBandOffsets, bSrcData,
                       dstWidth, dstHeight, dstNumBands,
                       dstLineStride, dstPixelStride,
                       dstBandOffsets, fDstData,
                       tblOffsets, fTblData);
                break;

            case DataBuffer.TYPE_USHORT:
                lookupU(srcLineStride, srcPixelStride,
                        srcBandOffsets, sSrcData,
                        dstWidth, dstHeight, dstNumBands,
                        dstLineStride, dstPixelStride,
                        dstBandOffsets, fDstData,
                        tblOffsets, fTblData);
                break;

            case DataBuffer.TYPE_SHORT:
                lookup(srcLineStride, srcPixelStride,
                       srcBandOffsets, sSrcData,
                       dstWidth, dstHeight, dstNumBands,
                       dstLineStride, dstPixelStride,
                       dstBandOffsets, fDstData,
                       tblOffsets, fTblData);
                break;

            case DataBuffer.TYPE_INT:
                lookup(srcLineStride, srcPixelStride,
                       srcBandOffsets, iSrcData,
                       dstWidth, dstHeight, dstNumBands,
                       dstLineStride, dstPixelStride,
                       dstBandOffsets, fDstData,
                       tblOffsets, fTblData);
                break;
            }
            break;

        case DataBuffer.TYPE_DOUBLE:
            switch (srcDataType) {
            case DataBuffer.TYPE_BYTE:
                lookup(srcLineStride, srcPixelStride,
                       srcBandOffsets, bSrcData,
                       dstWidth, dstHeight, dstNumBands,
                       dstLineStride, dstPixelStride,
                       dstBandOffsets, dDstData,
                       tblOffsets, dTblData);
                break;

            case DataBuffer.TYPE_USHORT:
                lookupU(srcLineStride, srcPixelStride,
                        srcBandOffsets, sSrcData,
                        dstWidth, dstHeight, dstNumBands,
                        dstLineStride, dstPixelStride,
                        dstBandOffsets, dDstData,
                        tblOffsets, dTblData);
                break;

            case DataBuffer.TYPE_SHORT:
                lookup(srcLineStride, srcPixelStride,
                       srcBandOffsets, sSrcData,
                       dstWidth, dstHeight, dstNumBands,
                       dstLineStride, dstPixelStride,
                       dstBandOffsets, dDstData,
                       tblOffsets, dTblData);
                break;

            case DataBuffer.TYPE_INT:
                lookup(srcLineStride, srcPixelStride,
                       srcBandOffsets, iSrcData,
                       dstWidth, dstHeight, dstNumBands,
                       dstLineStride, dstPixelStride,
                       dstBandOffsets, dDstData,
                       tblOffsets, dTblData);
                break;
            }
            break;
        }

        d.copyDataToRaster();

        return dst;
    }

    // byte to byte
    private void lookup(int srcLineStride, int srcPixelStride,
                        int[] srcBandOffsets, byte[][] srcData,
                        int width, int height, int bands,
                        int dstLineStride, int dstPixelStride,
                        int[] dstBandOffsets, byte[][] dstData,
                        int[] tblOffsets, byte[][] tblData) {
        for (int b = 0; b < bands; b++) {
            byte[] s = srcData[b];
            byte[] d = dstData[b];
            byte[] t = tblData[b];

            int srcLineOffset = srcBandOffsets[b];
            int dstLineOffset = dstBandOffsets[b];
            int tblOffset = tblOffsets[b];

            for (int h = 0; h < height; h++) {
                int srcPixelOffset = srcLineOffset;
                int dstPixelOffset = dstLineOffset;

                srcLineOffset += srcLineStride;
                dstLineOffset += dstLineStride;

                for (int w = 0; w < width; w++) {
                    d[dstPixelOffset] = t[(s[srcPixelOffset]&0xFF) - tblOffset];

                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }
            }
        }
    }

    // ushort to byte
    private void lookupU(int srcLineStride, int srcPixelStride,
                         int[] srcBandOffsets, short[][] srcData,
                         int width, int height, int bands,
                         int dstLineStride, int dstPixelStride,
                         int[] dstBandOffsets, byte[][] dstData,
                         int[] tblOffsets, byte[][] tblData) {
        for (int b = 0; b < bands; b++) {
            short[] s = srcData[b];
            byte[] d = dstData[b];
            byte[] t = tblData[b];

            int srcLineOffset = srcBandOffsets[b];
            int dstLineOffset = dstBandOffsets[b];
            int tblOffset = tblOffsets[b];

            for (int h = 0; h < height; h++) {
                int srcPixelOffset = srcLineOffset;
                int dstPixelOffset = dstLineOffset;

                srcLineOffset += srcLineStride;
                dstLineOffset += dstLineStride;

                for (int w = 0; w < width; w++) {
                    d[dstPixelOffset] =
                        t[(s[srcPixelOffset]&0xFFFF) - tblOffset];

                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }
            }
        }
    }

    // short to byte
    private void lookup(int srcLineStride, int srcPixelStride,
                        int[] srcBandOffsets, short[][] srcData,
                        int width, int height, int bands,
                        int dstLineStride, int dstPixelStride,
                        int[] dstBandOffsets, byte[][] dstData,
                        int[] tblOffsets, byte[][] tblData) {
        for (int b = 0; b < bands; b++) {
            short[] s = srcData[b];
            byte[] d = dstData[b];
            byte[] t = tblData[b];

            int srcLineOffset = srcBandOffsets[b];
            int dstLineOffset = dstBandOffsets[b];
            int tblOffset = tblOffsets[b];

            for (int h = 0; h < height; h++) {
                int srcPixelOffset = srcLineOffset;
                int dstPixelOffset = dstLineOffset;

                srcLineOffset += srcLineStride;
                dstLineOffset += dstLineStride;

                for (int w = 0; w < width; w++) {
                    d[dstPixelOffset] = t[s[srcPixelOffset] - tblOffset];

                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }
            }
        }
    }

    // int to byte
    private void lookup(int srcLineStride, int srcPixelStride,
                        int[] srcBandOffsets, int[][] srcData,
                        int width, int height, int bands,
                        int dstLineStride, int dstPixelStride,
                        int[] dstBandOffsets, byte[][] dstData,
                        int[] tblOffsets, byte[][] tblData) {
        for (int b = 0; b < bands; b++) {
            int[] s = srcData[b];
            byte[] d = dstData[b];
            byte[] t = tblData[b];

            int srcLineOffset = srcBandOffsets[b];
            int dstLineOffset = dstBandOffsets[b];
            int tblOffset = tblOffsets[b];

            for (int h = 0; h < height; h++) {
                int srcPixelOffset = srcLineOffset;
                int dstPixelOffset = dstLineOffset;

                srcLineOffset += srcLineStride;
                dstLineOffset += dstLineStride;

                for (int w = 0; w < width; w++) {
                    d[dstPixelOffset] = t[s[srcPixelOffset] - tblOffset];

                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }
            }
        }
    }

    // byte to short or ushort
    private void lookup(int srcLineStride, int srcPixelStride,
                        int[] srcBandOffsets, byte[][] srcData,
                        int width, int height, int bands,
                        int dstLineStride, int dstPixelStride,
                        int[] dstBandOffsets, short[][] dstData,
                        int[] tblOffsets, short[][] tblData) {
        for (int b = 0; b < bands; b++) {
            byte[] s = srcData[b];
            short[] d = dstData[b];
            short[] t = tblData[b];

            int srcLineOffset = srcBandOffsets[b];
            int dstLineOffset = dstBandOffsets[b];
            int tblOffset = tblOffsets[b];

            for (int h = 0; h < height; h++) {
                int srcPixelOffset = srcLineOffset;
                int dstPixelOffset = dstLineOffset;

                srcLineOffset += srcLineStride;
                dstLineOffset += dstLineStride;

                for (int w = 0; w < width; w++) {
                    d[dstPixelOffset] = t[(s[srcPixelOffset]&0xFF) - tblOffset];

                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }
            }
        }
    }

    // ushort to short or ushort
    private void lookupU(int srcLineStride, int srcPixelStride,
                         int[] srcBandOffsets, short[][] srcData,
                         int width, int height, int bands,
                         int dstLineStride, int dstPixelStride,
                         int[] dstBandOffsets, short[][] dstData,
                         int[] tblOffsets, short[][] tblData) {
        for (int b = 0; b < bands; b++) {
            short[] s = srcData[b];
            short[] d = dstData[b];
            short[] t = tblData[b];

            int srcLineOffset = srcBandOffsets[b];
            int dstLineOffset = dstBandOffsets[b];
            int tblOffset = tblOffsets[b];

            for (int h = 0; h < height; h++) {
                int srcPixelOffset = srcLineOffset;
                int dstPixelOffset = dstLineOffset;

                srcLineOffset += srcLineStride;
                dstLineOffset += dstLineStride;

                for (int w = 0; w < width; w++) {
                    d[dstPixelOffset] =
                        t[(s[srcPixelOffset]&0xFFFF) - tblOffset];

                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }
            }
        }
    }

    // short to short or ushort
    private void lookup(int srcLineStride, int srcPixelStride,
                        int[] srcBandOffsets, short[][] srcData,
                        int width, int height, int bands,
                        int dstLineStride, int dstPixelStride,
                        int[] dstBandOffsets, short[][] dstData,
                        int[] tblOffsets, short[][] tblData) {
        for (int b = 0; b < bands; b++) {
            short[] s = srcData[b];
            short[] d = dstData[b];
            short[] t = tblData[b];

            int srcLineOffset = srcBandOffsets[b];
            int dstLineOffset = dstBandOffsets[b];
            int tblOffset = tblOffsets[b];

            for (int h = 0; h < height; h++) {
                int srcPixelOffset = srcLineOffset;
                int dstPixelOffset = dstLineOffset;

                srcLineOffset += srcLineStride;
                dstLineOffset += dstLineStride;

                for (int w = 0; w < width; w++) {
                    d[dstPixelOffset] = t[s[srcPixelOffset] - tblOffset];

                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }
            }
        }
    }

    // int to short or ushort
    private void lookup(int srcLineStride, int srcPixelStride,
                        int[] srcBandOffsets, int[][] srcData,
                        int width, int height, int bands,
                        int dstLineStride, int dstPixelStride,
                        int[] dstBandOffsets, short[][] dstData,
                        int[] tblOffsets, short[][] tblData) {
        for (int b = 0; b < bands; b++) {
            int[] s = srcData[b];
            short[] d = dstData[b];
            short[] t = tblData[b];

            int srcLineOffset = srcBandOffsets[b];
            int dstLineOffset = dstBandOffsets[b];
            int tblOffset = tblOffsets[b];

            for (int h = 0; h < height; h++) {
                int srcPixelOffset = srcLineOffset;
                int dstPixelOffset = dstLineOffset;

                srcLineOffset += srcLineStride;
                dstLineOffset += dstLineStride;

                for (int w = 0; w < width; w++) {
                    d[dstPixelOffset] = t[s[srcPixelOffset] - tblOffset];

                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }
            }
        }
    }

    // byte to int
    private void lookup(int srcLineStride, int srcPixelStride,
                        int[] srcBandOffsets, byte[][] srcData,
                        int width, int height, int bands,
                        int dstLineStride, int dstPixelStride,
                        int[] dstBandOffsets, int[][] dstData,
                        int[] tblOffsets, int[][] tblData) {
        if (tblData == null) {
            for (int b = 0; b < bands; b++) {
                byte[] s = srcData[b];
                int[] d = dstData[b];

                int srcLineOffset = srcBandOffsets[b];
                int dstLineOffset = dstBandOffsets[b];

                for (int h = 0; h < height; h++) {
                    int srcPixelOffset = srcLineOffset;
                    int dstPixelOffset = dstLineOffset;

                    srcLineOffset += srcLineStride;
                    dstLineOffset += dstLineStride;

                    for (int w = 0; w < width; w++) {
                        d[dstPixelOffset] =
                            data.getElem(b, s[srcPixelOffset]&0xFF);

                        srcPixelOffset += srcPixelStride;
                        dstPixelOffset += dstPixelStride;
                    }
                }
            }
        } else {
            for (int b = 0; b < bands; b++) {
                byte[] s = srcData[b];
                int[] d = dstData[b];
                int[] t = tblData[b];

                int srcLineOffset = srcBandOffsets[b];
                int dstLineOffset = dstBandOffsets[b];
                int tblOffset = tblOffsets[b];

                for (int h = 0; h < height; h++) {
                    int srcPixelOffset = srcLineOffset;
                    int dstPixelOffset = dstLineOffset;

                    srcLineOffset += srcLineStride;
                    dstLineOffset += dstLineStride;

                    for (int w = 0; w < width; w++) {
                        d[dstPixelOffset] =
                            t[(s[srcPixelOffset]&0xFF) - tblOffset];

                        srcPixelOffset += srcPixelStride;
                        dstPixelOffset += dstPixelStride;
                    }
                }
            }
        }
    }

    // ushort to int
    private void lookupU(int srcLineStride, int srcPixelStride,
                         int[] srcBandOffsets, short[][] srcData,
                         int width, int height, int bands,
                         int dstLineStride, int dstPixelStride,
                         int[] dstBandOffsets, int[][] dstData,
                         int[] tblOffsets, int[][] tblData) {
        if (tblData == null) {
            for (int b = 0; b < bands; b++) {
                short[] s = srcData[b];
                int[] d = dstData[b];

                int srcLineOffset = srcBandOffsets[b];
                int dstLineOffset = dstBandOffsets[b];

                for (int h = 0; h < height; h++) {
                    int srcPixelOffset = srcLineOffset;
                    int dstPixelOffset = dstLineOffset;

                    srcLineOffset += srcLineStride;
                    dstLineOffset += dstLineStride;

                    for (int w = 0; w < width; w++) {
                        d[dstPixelOffset] =
                            data.getElem(b, s[srcPixelOffset]&0xFFFF);

                        srcPixelOffset += srcPixelStride;
                        dstPixelOffset += dstPixelStride;
                    }
                }
            }
        } else {
            for (int b = 0; b < bands; b++) {
                short[] s = srcData[b];
                int[] d = dstData[b];
                int[] t = tblData[b];

                int srcLineOffset = srcBandOffsets[b];
                int dstLineOffset = dstBandOffsets[b];
                int tblOffset = tblOffsets[b];

                for (int h = 0; h < height; h++) {
                    int srcPixelOffset = srcLineOffset;
                    int dstPixelOffset = dstLineOffset;

                    srcLineOffset += srcLineStride;
                    dstLineOffset += dstLineStride;

                    for (int w = 0; w < width; w++) {
                        d[dstPixelOffset] =
                            t[(s[srcPixelOffset]&0xFFFF) - tblOffset];

                        srcPixelOffset += srcPixelStride;
                        dstPixelOffset += dstPixelStride;
                    }
                }
            }
        }
    }

    // short to int
    private void lookup(int srcLineStride, int srcPixelStride,
                        int[] srcBandOffsets, short[][] srcData,
                        int width, int height, int bands,
                        int dstLineStride, int dstPixelStride,
                        int[] dstBandOffsets, int[][] dstData,
                        int[] tblOffsets, int[][] tblData) {
        if (tblData == null) {
            for (int b = 0; b < bands; b++) {
                short[] s = srcData[b];
                int[] d = dstData[b];

                int srcLineOffset = srcBandOffsets[b];
                int dstLineOffset = dstBandOffsets[b];

                for (int h = 0; h < height; h++) {
                    int srcPixelOffset = srcLineOffset;
                    int dstPixelOffset = dstLineOffset;

                    srcLineOffset += srcLineStride;
                    dstLineOffset += dstLineStride;

                    for (int w = 0; w < width; w++) {
                        d[dstPixelOffset] =
                            data.getElem(b, s[srcPixelOffset]);

                        srcPixelOffset += srcPixelStride;
                        dstPixelOffset += dstPixelStride;
                    }
                }
            }
        } else {
            for (int b = 0; b < bands; b++) {
                short[] s = srcData[b];
                int[] d = dstData[b];
                int[] t = tblData[b];

                int srcLineOffset = srcBandOffsets[b];
                int dstLineOffset = dstBandOffsets[b];
                int tblOffset = tblOffsets[b];

                for (int h = 0; h < height; h++) {
                    int srcPixelOffset = srcLineOffset;
                    int dstPixelOffset = dstLineOffset;

                    srcLineOffset += srcLineStride;
                    dstLineOffset += dstLineStride;

                    for (int w = 0; w < width; w++) {
                        d[dstPixelOffset] = t[s[srcPixelOffset] - tblOffset];

                        srcPixelOffset += srcPixelStride;
                        dstPixelOffset += dstPixelStride;
                    }
                }
            }
        }
    }

    // int to int
    private void lookup(int srcLineStride, int srcPixelStride,
                        int[] srcBandOffsets, int[][] srcData,
                        int width, int height, int bands,
                        int dstLineStride, int dstPixelStride,
                        int[] dstBandOffsets, int[][] dstData,
                        int[] tblOffsets, int[][] tblData) {
        if (tblData == null) {
            for (int b = 0; b < bands; b++) {
                int[] s = srcData[b];
                int[] d = dstData[b];

                int srcLineOffset = srcBandOffsets[b];
                int dstLineOffset = dstBandOffsets[b];

                for (int h = 0; h < height; h++) {
                    int srcPixelOffset = srcLineOffset;
                    int dstPixelOffset = dstLineOffset;

                    srcLineOffset += srcLineStride;
                    dstLineOffset += dstLineStride;

                    for (int w = 0; w < width; w++) {
                        d[dstPixelOffset] = data.getElem(b, s[srcPixelOffset]);

                        srcPixelOffset += srcPixelStride;
                        dstPixelOffset += dstPixelStride;
                    }
                }
            }
        } else {
            for (int b = 0; b < bands; b++) {
                int[] s = srcData[b];
                int[] d = dstData[b];
                int[] t = tblData[b];

                int srcLineOffset = srcBandOffsets[b];
                int dstLineOffset = dstBandOffsets[b];
                int tblOffset = tblOffsets[b];

                for (int h = 0; h < height; h++) {
                    int srcPixelOffset = srcLineOffset;
                    int dstPixelOffset = dstLineOffset;

                    srcLineOffset += srcLineStride;
                    dstLineOffset += dstLineStride;

                    for (int w = 0; w < width; w++) {
                        d[dstPixelOffset] = t[s[srcPixelOffset] - tblOffset];

                        srcPixelOffset += srcPixelStride;
                        dstPixelOffset += dstPixelStride;
                    }
                }
            }
        }
    }

    // byte to float
    private void lookup(int srcLineStride, int srcPixelStride,
                        int[] srcBandOffsets, byte[][] srcData,
                        int width, int height, int bands,
                        int dstLineStride, int dstPixelStride,
                        int[] dstBandOffsets, float[][] dstData,
                        int[] tblOffsets, float[][] tblData) {
        for (int b = 0; b < bands; b++) {
            byte[] s = srcData[b];
            float[] d = dstData[b];
            float[] t = tblData[b];

            int srcLineOffset = srcBandOffsets[b];
            int dstLineOffset = dstBandOffsets[b];
            int tblOffset = tblOffsets[b];

            for (int h = 0; h < height; h++) {
                int srcPixelOffset = srcLineOffset;
                int dstPixelOffset = dstLineOffset;

                srcLineOffset += srcLineStride;
                dstLineOffset += dstLineStride;

                for (int w = 0; w < width; w++) {
                    d[dstPixelOffset] = t[(s[srcPixelOffset]&0xFF) - tblOffset];

                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }
            }
        }
    }

    // ushort to float
    private void lookupU(int srcLineStride, int srcPixelStride,
                         int[] srcBandOffsets, short[][] srcData,
                         int width, int height, int bands,
                         int dstLineStride, int dstPixelStride,
                         int[] dstBandOffsets, float[][] dstData,
                         int[] tblOffsets, float[][] tblData) {
        for (int b = 0; b < bands; b++) {
            short[] s = srcData[b];
            float[] d = dstData[b];
            float[] t = tblData[b];

            int srcLineOffset = srcBandOffsets[b];
            int dstLineOffset = dstBandOffsets[b];
            int tblOffset = tblOffsets[b];

            for (int h = 0; h < height; h++) {
                int srcPixelOffset = srcLineOffset;
                int dstPixelOffset = dstLineOffset;

                srcLineOffset += srcLineStride;
                dstLineOffset += dstLineStride;

                for (int w = 0; w < width; w++) {
                    d[dstPixelOffset] =
                        t[(s[srcPixelOffset]&0xFFFF) - tblOffset];

                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }
            }
        }
    }

    // short to float
    private void lookup(int srcLineStride, int srcPixelStride,
                        int[] srcBandOffsets, short[][] srcData,
                        int width, int height, int bands,
                        int dstLineStride, int dstPixelStride,
                        int[] dstBandOffsets, float[][] dstData,
                        int[] tblOffsets, float[][] tblData) {
        for (int b = 0; b < bands; b++) {
            short[] s = srcData[b];
            float[] d = dstData[b];
            float[] t = tblData[b];

            int srcLineOffset = srcBandOffsets[b];
            int dstLineOffset = dstBandOffsets[b];
            int tblOffset = tblOffsets[b];

            for (int h = 0; h < height; h++) {
                int srcPixelOffset = srcLineOffset;
                int dstPixelOffset = dstLineOffset;

                srcLineOffset += srcLineStride;
                dstLineOffset += dstLineStride;

                for (int w = 0; w < width; w++) {
                    d[dstPixelOffset] = t[s[srcPixelOffset] - tblOffset];

                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }
            }
        }
    }

    // int to float
    private void lookup(int srcLineStride, int srcPixelStride,
                        int[] srcBandOffsets, int[][] srcData,
                        int width, int height, int bands,
                        int dstLineStride, int dstPixelStride,
                        int[] dstBandOffsets, float[][] dstData,
                        int[] tblOffsets, float[][] tblData) {
        for (int b = 0; b < bands; b++) {
            int[] s = srcData[b];
            float[] d = dstData[b];
            float[] t = tblData[b];

            int srcLineOffset = srcBandOffsets[b];
            int dstLineOffset = dstBandOffsets[b];
            int tblOffset = tblOffsets[b];

            for (int h = 0; h < height; h++) {
                int srcPixelOffset = srcLineOffset;
                int dstPixelOffset = dstLineOffset;

                srcLineOffset += srcLineStride;
                dstLineOffset += dstLineStride;

                for (int w = 0; w < width; w++) {
                    d[dstPixelOffset] = t[s[srcPixelOffset] - tblOffset];

                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }
            }
        }
    }

    // byte to double
    private void lookup(int srcLineStride, int srcPixelStride,
                        int[] srcBandOffsets, byte[][] srcData,
                        int width, int height, int bands,
                        int dstLineStride, int dstPixelStride,
                        int[] dstBandOffsets, double[][] dstData,
                        int[] tblOffsets, double[][] tblData) {
        for (int b = 0; b < bands; b++) {
            byte[] s = srcData[b];
            double[] d = dstData[b];
            double[] t = tblData[b];

            int srcLineOffset = srcBandOffsets[b];
            int dstLineOffset = dstBandOffsets[b];
            int tblOffset = tblOffsets[b];

            for (int h = 0; h < height; h++) {
                int srcPixelOffset = srcLineOffset;
                int dstPixelOffset = dstLineOffset;

                srcLineOffset += srcLineStride;
                dstLineOffset += dstLineStride;

                for (int w = 0; w < width; w++) {
                    d[dstPixelOffset] = t[(s[srcPixelOffset]&0xFF) - tblOffset];

                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }
            }
        }
    }

    // ushort to double
    private void lookupU(int srcLineStride, int srcPixelStride,
                         int[] srcBandOffsets, short[][] srcData,
                         int width, int height, int bands,
                         int dstLineStride, int dstPixelStride,
                         int[] dstBandOffsets, double[][] dstData,
                         int[] tblOffsets, double[][] tblData) {
        for (int b = 0; b < bands; b++) {
            short[] s = srcData[b];
            double[] d = dstData[b];
            double[] t = tblData[b];

            int srcLineOffset = srcBandOffsets[b];
            int dstLineOffset = dstBandOffsets[b];
            int tblOffset = tblOffsets[b];

            for (int h = 0; h < height; h++) {
                int srcPixelOffset = srcLineOffset;
                int dstPixelOffset = dstLineOffset;

                srcLineOffset += srcLineStride;
                dstLineOffset += dstLineStride;

                for (int w = 0; w < width; w++) {
                    d[dstPixelOffset] =
                        t[(s[srcPixelOffset]&0xFFFF) - tblOffset];

                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }
            }
        }
    }

    // short to double
    private void lookup(int srcLineStride, int srcPixelStride,
                        int[] srcBandOffsets, short[][] srcData,
                        int width, int height, int bands,
                        int dstLineStride, int dstPixelStride,
                        int[] dstBandOffsets, double[][] dstData,
                        int[] tblOffsets, double[][] tblData) {
        for (int b = 0; b < bands; b++) {
            short[] s = srcData[b];
            double[] d = dstData[b];
            double[] t = tblData[b];

            int srcLineOffset = srcBandOffsets[b];
            int dstLineOffset = dstBandOffsets[b];
            int tblOffset = tblOffsets[b];

            for (int h = 0; h < height; h++) {
                int srcPixelOffset = srcLineOffset;
                int dstPixelOffset = dstLineOffset;

                srcLineOffset += srcLineStride;
                dstLineOffset += dstLineStride;

                for (int w = 0; w < width; w++) {
                    d[dstPixelOffset] = t[s[srcPixelOffset] - tblOffset];

                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }
            }
        }
    }

    // int to double
    private void lookup(int srcLineStride, int srcPixelStride,
                        int[] srcBandOffsets, int[][] srcData,
                        int width, int height, int bands,
                        int dstLineStride, int dstPixelStride,
                        int[] dstBandOffsets, double[][] dstData,
                        int[] tblOffsets, double[][] tblData) {
        for (int b = 0; b < bands; b++) {
            int[] s = srcData[b];
            double[] d = dstData[b];
            double[] t = tblData[b];

            int srcLineOffset = srcBandOffsets[b];
            int dstLineOffset = dstBandOffsets[b];
            int tblOffset = tblOffsets[b];

            for (int h = 0; h < height; h++) {
                int srcPixelOffset = srcLineOffset;
                int dstPixelOffset = dstLineOffset;

                srcLineOffset += srcLineStride;
                dstLineOffset += dstLineStride;

                for (int w = 0; w < width; w++) {
                    d[dstPixelOffset] = t[s[srcPixelOffset] - tblOffset];

                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }
            }
        }
    }

    /**
     * Determine which entry in the <code>LookupTableJAI</code> is closest
     * in Euclidean distance to the argument pixel.
     *
     * @param pixel The pixel the closest entry to which is to be found.
     *
     * @return the index of the closest entry. If the data array of the
     * lookup table is in the format data[numBands][numEntries], then the
     * value <i>v</i> for band <i>b</i> of the closest entry is
     * <pre>
     *     v = data[b][index - lookup.getOffset()]
     * </pre>
     * where <i>index</i> is the returned value of this method.
     *
     * @throws IllegalArgumentException if pixel is null.
     */
    public int findNearestEntry(float[] pixel) {

        if ( pixel == null ) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        int dataType = data.getDataType();
        int numBands = getNumBands();
        int numEntries = getNumEntries();
        int index = -1;

        if(dataType == DataBuffer.TYPE_BYTE) {
            byte buffer[][] = getByteData();

            // Find the distance to the first entry and set result to 0.
            float minDistance = 0.0F;
            index = 0;
            for(int b = 0; b < numBands; b++) {
                float delta = pixel[b] - (float)(buffer[b][0] & 0xff);
                minDistance += delta*delta;
            }

            // Find the distance to each entry and set the result to
            // the index which is closest to the argument.
            for(int i = 1; i < numEntries; i++) {
                float distance = 0.0F;
                for(int b = 0; b < numBands; b++) {
                    float delta =
                        pixel[b] - (float)(buffer[b][i] & 0xff);
                    distance += delta*delta;
                }

                if(distance < minDistance) {
                    minDistance = distance;
                    index = i;
                }
            }
        } else if(dataType == DataBuffer.TYPE_SHORT) {
            short buffer[][] = getShortData();

            // Find the distance to the first entry and set result to 0.
            float minDistance = 0.0F;
            index = 0;
            for(int b = 0; b < numBands; b++) {
                float delta = pixel[b] - buffer[b][0];
                minDistance += delta*delta;
            }

            // Find the distance to each entry and set the result to
            // the index which is closest to the argument.
            for(int i = 1; i < numEntries; i++) {
                float distance = 0.0F;
                for(int b = 0; b < numBands; b++) {
                    float delta = pixel[b] - buffer[b][i];
                    distance += delta*delta;
                }

                if(distance < minDistance) {
                    minDistance = distance;
                    index = i;
                }
            }
        } else if(dataType == DataBuffer.TYPE_USHORT) {
            short buffer[][] = getShortData();

            // Find the distance to the first entry and set result to 0.
            float minDistance = 0.0F;
            index = 0;
            for(int b = 0; b < numBands; b++) {
                float delta = pixel[b] - (float)(buffer[b][0] & 0xffff);
                minDistance += delta*delta;
            }

            // Find the distance to each entry and set the result to
            // the index which is closest to the argument.
            for(int i = 1; i < numEntries; i++) {
                float distance = 0.0F;
                for(int b = 0; b < numBands; b++) {
                    float delta =
                        pixel[b] - (float)(buffer[b][i] & 0xffff);
                    distance += delta*delta;
                }

                if(distance < minDistance) {
                    minDistance = distance;
                    index = i;
                }
            }
        } else if(dataType == DataBuffer.TYPE_INT) {
            int buffer[][] = getIntData();

            // Find the distance to the first entry and set result to 0.
            float minDistance = 0.0F;
            index = 0;
            for(int b = 0; b < numBands; b++) {
                float delta = pixel[b] - buffer[b][0];
                minDistance += delta*delta;
            }

            // Find the distance to each entry and set the result to
            // the index which is closest to the argument.
            for(int i = 1; i < numEntries; i++) {
                float distance = 0.0F;
                for(int b = 0; b < numBands; b++) {
                    float delta = pixel[b] - buffer[b][i];
                    distance += delta*delta;
                }

                if(distance < minDistance) {
                    minDistance = distance;
                    index = i;
                }
            }
        } else if(dataType == DataBuffer.TYPE_FLOAT) {
            float buffer[][] = getFloatData();

            // Find the distance to the first entry and set result to 0.
            float minDistance = 0.0F;
            index = 0;
            for(int b = 0; b < numBands; b++) {
                float delta = pixel[b] - buffer[b][0];
                minDistance += delta*delta;
            }

            // Find the distance to each entry and set the result to
            // the index which is closest to the argument.
            for(int i = 1; i < numEntries; i++) {
                float distance = 0.0F;
                for(int b = 0; b < numBands; b++) {
                    float delta = pixel[b] - buffer[b][i];
                    distance += delta*delta;
                }

                if(distance < minDistance) {
                    minDistance = distance;
                    index = i;
                }
            }
        } else if(dataType == DataBuffer.TYPE_DOUBLE) {
            double buffer[][] = getDoubleData();

            // Find the distance to the first entry and set result to 0.
            double minDistance = 0.0F;
            index = 0;
            for(int b = 0; b < numBands; b++) {
                double delta = pixel[b] - buffer[b][0];
                minDistance += delta*delta;
            }

            // Find the distance to each entry and set the result to
            // the index which is closest to the argument.
            for(int i = 1; i < numEntries; i++) {
                double distance = 0.0F;
                for(int b = 0; b < numBands; b++) {
                    double delta = pixel[b] - buffer[b][i];
                    distance += delta*delta;
                }

                if(distance < minDistance) {
                    minDistance = distance;
                    index = i;
                }
            }
        } else {
            // This can't happen since we control the type of data
            throw new RuntimeException(JaiI18N.getString("LookupTableJAI0"));
        }

        // Return the index of the closest color plus the offset of the
        // default band or -1 on error.
        return index == -1 ? index : index + getOffset();
    }

    /**
      * Serialize the <code>LookupTableJAI</code>.
      *
      * @param out The <code>ObjectOutputStream</code>.
      */
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        out.writeObject(SerializerFactory.getState(data));
    }

    /**
      * Deserialize the <code>LookupTableJAI</code>.
      *
      * @param in The <code>ObjectInputStream</code>.
      */
    private void readObject(ObjectInputStream in)
        throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        Object object = in.readObject();
        SerializableState ss = (SerializableState)object;
        data = (DataBuffer)ss.getObject();
    }

    private void initOffsets(int nbands, int offset) {
        tableOffsets = new int[nbands];
        for (int i=0; i<nbands; i++) {
            tableOffsets[i] = offset;
        }
    }

    private void initOffsets(int nbands, int[] offset) {
        tableOffsets = new int[nbands];
        for (int i=0; i<nbands; i++) {
            tableOffsets[i] = offset[i];
        }
    }


}


