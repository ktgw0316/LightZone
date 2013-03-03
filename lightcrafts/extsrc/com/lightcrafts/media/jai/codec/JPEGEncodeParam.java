/*
 * $RCSfile: JPEGEncodeParam.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:55:31 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.codec;

/**
 * A class which encapsulates the most common functionality required for 
 * the parameters to a Jpeg encode operation. It does not include all of
 * the parameters of the <code>com.sun.image.codec.jpeg</code> classes.
 * Users needing that additional functionality should use those classes
 * directly, bearing in mind that they are part of an uncommitted non-core
 * interface that may be modified or removed in the future.
 *
 * This class makes very simple assumptions about the image colorspaces.
 * Images with a single band are assumed to be grayscale.
 * Images with three bands are assumed to be RGB and are encoded to YCbCr.
 *
 * <p><b> This class is not a committed part of the JAI API.  It may
 * be removed or changed in future releases of JAI.</b>
 */
public class JPEGEncodeParam implements ImageEncodeParam {

    private static int  JPEG_MAX_BANDS = 3;

    private int[]       hSamp;
    private int[]       vSamp;
    private int[][]     qTab;
    private int[]       qTabSlot;
    private float       qual;
    private int         rstInterval;
    private boolean     writeImageOnly;
    private boolean     writeTablesOnly;
    private boolean     writeJFIFHeader;
    private boolean     qualitySet;
    private boolean[]   qTabSet;
    
    /**
     * Constructs a JAI JPEGEncodeParam object with default settings.
     */
    public JPEGEncodeParam() {
        //
        // Set all the defaults
        //
        hSamp    = new int[JPEG_MAX_BANDS];
        vSamp    = new int[JPEG_MAX_BANDS];
        qTabSlot = new int[JPEG_MAX_BANDS];
        qTab     = new int[JPEG_MAX_BANDS][];
        qTabSet  = new boolean[JPEG_MAX_BANDS];

        // Y channel - full resolution sampling
        hSamp[0]    = 1;
        vSamp[0]    = 1;
        qTabSlot[0] = 0;
        qTab[0]     = null;
        qTabSet[0]  = false;

        // Cb channel - sample by 2 in each axis
        hSamp[1]    = 2;
        vSamp[1]    = 2;
        qTabSlot[1] = 1;
        qTab[1]     = null;
        qTabSet[1]  = false;

        // Cr channel - sample by 2 in each axis
        hSamp[2]    = 2;
        vSamp[2]    = 2;
        qTabSlot[2] = 1;
        qTab[2]     = null;
        qTabSet[2]  = false;

        qual           = 0.75F;
        rstInterval    = 0;
        writeImageOnly = false;
        writeTablesOnly = false;
        writeJFIFHeader = true;
    }

    /**
     * Sets the horizontal subsampling to be applied to an image band.
     * Defaults to 1 for grayscale and (1,2,2) for RGB.
     * @param component The band for which to set horizontal subsampling.
     * @param subsample The horizontal subsampling factor.
     */
    public void setHorizontalSubsampling(int component, 
                                         int subsample) {
        hSamp[component] = subsample;
    }

    /**
     * Get the horizontal subsampling factor for a band.
     * @param component The band of the image for which to retrieve subsampling.
     * @return The horizontal subsampling factor to be applied to this band
     */
    public int getHorizontalSubsampling(int component) {
        return hSamp[component];
    }

    
    /**
     * Sets the vertical subsampling to be applied to an image band.
     * Defaults to 1 for grayscale and (1,2,2) for RGB.
     * @param component The band for which to set vertical subsampling.
     * @param subsample The vertical subsampling factor.
     */
    public void setVerticalSubsampling(int component, 
                                       int subsample) {
        vSamp[component] = subsample;
    }
    
    /**
     * Get the vertical subsampling factor for a band.
     * @param component The band of the image for which to retrieve subsampling.
     * @return The vertical subsampling factor to be applied to this band
     */
    public int getVerticalSubsampling(int component) {
        return vSamp[component];
    }

    /** 
     * Sets the quantization table to be used for luminance data.
     * This is a convenience method which explicitly sets the
     * contents of quantization table 0. The length of the table must be 64.
     * This disables any quality setting.
     * @param qTable Quantization table values in "zig-zag" order.
     */	
    public void	setLumaQTable(int[] qTable) {
        setQTable(0, 0, qTable);
        qTabSet[0] = true;
        qualitySet = false;
    }
    
    /** 
     * Sets the quantization table to be used for chrominance data.
     * This is a convenience method which explicitly sets the
     * contents of quantization table 1. The length of the table must be 64.
     * This method assumes that all chroma components will use the same table.
     * This disables any quality setting.
     * @param qTable Quantization table values in "zig-zag" order.
     */	
    public void	setChromaQTable(int[] qTable) {
        setQTable(1, 1, qTable);
        setQTable(2, 1, qTable);
        qTabSet[1] = true;
        qTabSet[2] = true;
        qualitySet = false;
    }
    
    /**
     * Sets a quantization table to be used for a component.
     * This method allows up to four independent tables to be specified.
     * This disables any quality setting.
     * @param component The band to which this table applies.
     * @param tableSlot The table number that this table is assigned to (0 to 3).
     * @param qTable Quantization table values in "zig-zag" order.
     */
    public void setQTable(int component, int tableSlot, int[]qTable) {
        qTab[component] = (int[])(qTable.clone());
        qTabSlot[component] = tableSlot;
        qTabSet[component] = true;
        qualitySet = false;
    }

    /**
     * Tests if a Quantization table has been set.
     * @return Returns true is the specified quantization table has been set.
     */
    public boolean isQTableSet(int component) {
        return qTabSet[component];
    }

    /**
     * Retrieve the contents of the quantization table used for a component.
     * @param component The band to which this table applies.
     * @return The contents of the quantization table as a reference.
     * @throws IllegalStateException if table has not been previously set for this component.
     */
    public int[] getQTable(int component) {
        if (!qTabSet[component]) {
            throw new IllegalStateException(
               JaiI18N.getString("JPEGEncodeParam0"));
        }
        return qTab[component];
    }

    /**
     * Retrieve the quantization table slot used for a component.
     * @param component The band to which this table slot applies.
     * @return The table slot used for this band.
     * @throws IllegalStateException if table has not been previously set for this component.
     */
    public int getQTableSlot(int component) {
        if (!qTabSet[component]) {
            throw new IllegalStateException(
               JaiI18N.getString("JPEGEncodeParam0"));
        }
        return qTabSlot[component];
    }

    /**
     * Sets the restart interval in Minimum Coded Units (MCUs).
     * This can be useful in some environments to limit the effect
     * of bitstream errors to a single restart interval.
     * The default is zero (no restart interval markers).
     * @param restartInterval Number of MCUs between restart markers.
     */
    public void setRestartInterval(int restartInterval) {
        rstInterval = restartInterval;
    }

    /**
     * Gets the restart interval in Minimum Coded Units (MCUs).
     * @return The restart interval in MCUs (0 if not set).
     */
    public int getRestartInterval() {
        return rstInterval;
    }
    
    /**
     * This creates new quantization tables that replace the currently
     * installed quantization tables.  

     * The created quantization table varies from very high
     * compression, very low quality, (0.0) to low compression, very
     * high quality (1.0) based on the quality parameter.<P>

     * At a quality level of 1.0 the table will be all 1's which will
     * lead to no loss of data due to quantization (however chrominace
     * subsampling, if used, and roundoff error in the DCT will still
     * degrade the image some what).<P>

     * The default setting is 0.75 which provides high quality while
     * insuring a good compression ratio.

     * <pre>Some guidelines: 0.75 high quality
     *                 0.5  medium quality
     *                 0.25 low quality
     * </pre>
     * @param quality 0.0-1.0 setting of desired quality level.
     */
    public void setQuality(float quality) {
        qual = quality;
        // Reset custom Q tables
        for (int i=0; i<JPEG_MAX_BANDS; i++) {
            qTabSet[i] = false;
        }
        qualitySet = true;
    }

    /**
     * Tests if the quality parameter has been set in this JPEGEncodeParam.
     * @return True/false flag indicating if quality has been set.
     */
    public boolean isQualitySet() {
        return qualitySet;
    }

    /** 
     * Retrieve the quality setting for this encoding.
     * This is a number between 0.0 and 1.0.
     *
     * @return The specified quality setting (0.75 if not set).
     */
    public float getQuality() {
        return qual;
    }

    /**
     * Instructs the encoder to write only the table data to the output stream.
     * This is considered an abbreviated JPEG stream. Defaults to false -- normally
     * both tables and encoded image data are written.
     * @param tablesOnly If true, only the tables will be written.
     */
    public void setWriteTablesOnly(boolean tablesOnly) {
        writeTablesOnly = tablesOnly;
    }

    /**
     * Retrieve the setting of the writeTablesOnly flag.
     * @return The setting of the writeTablesOnly flag (false if not set).
     */
    public boolean getWriteTablesOnly() {
        return writeTablesOnly;
    }

    /**
     * Controls whether the encoder writes only the compressed image data 
     * to the output stream.
     * This is considered an abbreviated JPEG stream. Defaults to false -- normally
     * both tables and compressed image data are written.
     * @param imageOnly If true, only the compressed image will be written.
     */
    public void setWriteImageOnly(boolean imageOnly) {
        writeImageOnly = imageOnly;
    }

    /**
     * Retrieve the setting of the writeImageOnly flag.
     * @return The setting of the writeImageOnly flag (false if not set).
     */
    public boolean getWriteImageOnly() {
        return writeImageOnly;
    }

    /**
     * Controls whether the encoder writes a JFIF header using the APP0 marker.
     * By default an APP0 marker is written to create a JFIF file.
     * @param writeJFIF If true, writes a JFIF header.
     */
    public void setWriteJFIFHeader(boolean writeJFIF) {
        writeJFIFHeader = writeJFIF;
    }

    /**
     * Retrieve the setting of the writeJFIF flag.
     * @return The setting of the writeJFIF flag (true if not set).
     */
    public boolean getWriteJFIFHeader() {
        return writeJFIFHeader;
    }

    /**
     * Returns a copy of this <code>JPEGEncodeParam</code> object.
     */
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            // this shouldn't happen, since we are Cloneable
            throw new InternalError();
        }
    }
}
