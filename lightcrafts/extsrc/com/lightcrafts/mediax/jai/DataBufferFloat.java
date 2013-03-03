/*
 * $RCSfile: DataBufferFloat.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:07 $
 * $State: Exp $
 */
package com.lightcrafts.mediax.jai;
import java.awt.image.DataBuffer;

/**
 * An extension of <code>DataBuffer</code> that stores data internally in
 * <code>float</code> form.
 *
 * @see java.awt.image.DataBuffer
 */
public class DataBufferFloat extends DataBuffer {

    /** The array of data banks. */
    protected float bankdata[][];

    /** A reference to the default data bank. */
    protected float data[];

    /**
     * Constructs a <code>float</code>-based <code>DataBuffer</code>
     * with a specified size.
     *
     * @param size The number of elements in the <code>DataBuffer</code>.
     */
    public DataBufferFloat(int size) {
        super(TYPE_FLOAT, size);
        data = new float[size];
        bankdata = new float[1][];
        bankdata[0] = data;
    }

    /**
     * Constructs a <code>float</code>-based <code>DataBuffer</code>
     * with a specified number of banks, all of which are of a
     * specified size.
     *
     * @param size The number of elements in each bank of the
     * <code>DataBuffer</code>.
     * @param numBanks The number of banks in the
     *        <code>DataBuffer</code>.
     */
    public DataBufferFloat(int size, int numBanks) {
        super(TYPE_FLOAT, size, numBanks);
        bankdata = new float[numBanks][];
        for (int i= 0; i < numBanks; i++) {
            bankdata[i] = new float[size];
        }
        data = bankdata[0];
    }

    /**
     * Constructs a <code>float</code>-based <code>DataBuffer</code>
     * with the specified data array.  Only the first
     * <code>size</code> elements are available for use by this
     * <code>DataBuffer</code>.  The array must be large enough to
     * hold <code>size</code> elements.
     *
     * @param dataArray An array of <code>float</code>s to be used as the
     *                  first and only bank of this <code>DataBuffer</code>.
     * @param size The number of elements of the array to be used.
     */
    public DataBufferFloat(float dataArray[], int size) {
        super(TYPE_FLOAT, size);
	if (dataArray.length < size)
	  throw new RuntimeException(JaiI18N.getString("DataBuffer0"));
        data = dataArray;
        bankdata = new float[1][];
        bankdata[0] = data;
    }

    /**
     * Constructs a <code>float</code>-based <code>DataBuffer</code>
     * with the specified data array.  Only the elements between
     * <code>offset</code> and <code>offset + size - 1</code> are
     * available for use by this <code>DataBuffer</code>.  The array
     * must be large enough to hold <code>offset + size</code>
     * elements.
     *
     * @param dataArray An array of <code>float</code>s to be used as the
     *                  first and only bank of this <code>DataBuffer</code>.
     * @param size The number of elements of the array to be used.
     * @param offset The offset of the first element of the array
     *               that will be used.
     */
    public DataBufferFloat(float dataArray[], int size, int offset) {
        super(TYPE_FLOAT, size, 1, offset);
	if (dataArray.length < size)
	  throw new RuntimeException(JaiI18N.getString("DataBuffer1"));
        data = dataArray;
        bankdata = new float[1][];
        bankdata[0] = data;
    }

    /**
     * Constructs a <code>float</code>-based <code>DataBuffer</code>
     * with the specified data arrays.  Only the first
     * <code>size</code> elements of each array are available for use
     * by this <code>DataBuffer</code>.  The number of banks will be
     * equal to <code>dataArray.length</code>.
     *
     * @param dataArray An array of arrays of <code>float</code>s to be
     *                  used as the banks of this <code>DataBuffer</code>.
     * @param size The number of elements of each array to be used.
     */
    public DataBufferFloat(float dataArray[][], int size) {
        super(TYPE_FLOAT, size, dataArray.length);
        bankdata = dataArray;
        data = bankdata[0];
    }

    /**
     * Constructs a <code>float</code>-based <code>DataBuffer</code>
     * with the specified data arrays, size, and per-bank offsets.
     * The number of banks is equal to <code>dataArray.length</code>.
     * Each array must be at least as large as <code>size</code> plus the
     * corresponding offset.  There must be an entry in the offsets
     * array for each data array.
     *
     * @param dataArray An array of arrays of <code>float</code>s to be
     *                  used as the banks of this <code>DataBuffer</code>.
     * @param size The number of elements of each array to be used.
     * @param offsets An array of integer offsets, one for each bank.
     */
    public DataBufferFloat(float dataArray[][], int size, int offsets[]) {
        super(TYPE_FLOAT, size,dataArray.length, offsets);
        bankdata = dataArray;
        data = bankdata[0];
    }

    /** Returns the <code>float</code> data array of the default(first) bank. */
    public float[] getData() {
        return data;
    }

    /** Returns the data array for the specified bank. */
    public float[] getData(int bank) {
        return bankdata[bank];
    }

    /** Returns the data array for all banks. */
    public float[][] getBankData() {
        return bankdata;
    }
    
    /**
     * Returns the requested data array element from the first
     * (default) bank rounded off as an <code>int</code>.
     *
     * @param i The desired data array element.
     *
     * @return The data entry as an <code>int</code>.
     */
    public int getElem(int i) {
        return Math.round(data[i+offset]);
    }

    /**
     * Returns the requested data array element from the specified
     * bank rounded off as an <code>int</code>.
     *
     * @param bank The bank number.
     * @param i The desired data array element.
     *
     * @return The data entry as an <code>int</code>.
     */
    public int getElem(int bank, int i) {
        return Math.round(bankdata[bank][i+offsets[bank]]);
    }

    /**
     * Sets the requested data array element in the first (default)
     * bank to the given <code>int</code>.
     *
     * @param i The desired data array element.
     * @param val The value to be set.
     */
    public void setElem(int i, int val) {
        data[i+offset] = (float)val;
    }

    /**
     * Sets the requested data array element in the specified bank to
     * the given <code>int</code>.
     *
     * @param bank The bank number.
     * @param i The desired data array element.
     * @param val The value to be set.
     */
    public void setElem(int bank, int i, int val) {
        bankdata[bank][i+offsets[bank]] = (float)val;
    }

    /**
     * Returns the requested data array element from the first
     * (default) bank as a <code>float</code>.
     *
     * @param i The desired data array element.
     *
     * @return The data entry as a <code>float</code>.
     */
    public float getElemFloat(int i) {
        return data[i+offset];
    }
 
    /**
     * Returns the requested data array element from the specified
     * bank as a <code>float</code>.
     *
     * @param bank The bank number.
     * @param i The desired data array element.
     *
     * @return The data entry as a <code>float</code>.
     */
    public float getElemFloat(int bank, int i) {
        return bankdata[bank][i+offsets[bank]];
    }
 
    /**
     * Sets the requested data array element in the first (default)
     * bank to the given <code>float</code>.
     *
     * @param i The desired data array element.
     * @param val The value to be set.
     */
    public void setElemFloat(int i, float val) {
        data[i+offset] = val;
    }
 
    /**
     * Sets the requested data array element in the specified bank to
     * the given <code>float</code>.
     *
     * @param bank The bank number.
     * @param i The desired data array element.
     * @param val The value to be set.
     */
    public void setElemFloat(int bank, int i, float val) {
        bankdata[bank][i+offsets[bank]] = val;
    }

    /**
     * Returns the requested data array element from the first
     * (default) bank as a <code>double</code>.
     *
     * @param i The desired data array element.
     *
     * @return The data entry as a <code>double</code>.
     */
    public double getElemDouble(int i) {
        return (double)data[i+offset];
    }
 
    /**
     * Returns the requested data array element from the specified
     * bank as a <code>double</code>.
     *
     * @param bank The bank number.
     * @param i The desired data array element.
     *
     * @return The data entry as a <code>double</code>.
     */
    public double getElemDouble(int bank, int i) {
        return (double)bankdata[bank][i+offsets[bank]];
    }
 
    /**
     * Sets the requested data array element in the first (default)
     * bank to the given <code>double</code>.
     *
     * @param i The desired data array element.
     * @param val The value to be set.
     */
    public void setElemDouble(int i, double val) {
        data[i+offset] = (float)val;
    }
 
    /**
     * Sets the requested data array element in the specified bank to
     * the given <code>double</code>.
     *
     * @param bank The bank number.
     * @param i The desired data array element.
     * @param val The value to be set.
     */
    public void setElemDouble(int bank, int i, double val) {
        bankdata[bank][i+offsets[bank]] = (float)val;
    }
}
