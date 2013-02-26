/*
 * $RCSfile: InterpolationTable.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:11 $
 * $State: Exp $
 */
package com.lightcrafts.mediax.jai;

/**
 * A subclass of Interpolation that uses tables to store the
 * interpolation kernels.  The set of subpixel positions is broken up
 * into a fixed number of "bins" and a distinct kernel is used for
 * each bin.  The number of bins must be a power of two.
 *
 * <p> An InterpolationTable defines a separable interpolation, with a
 * set of kernels for each dimension.  The number of bins may vary
 * between the two dimensions. Both the horizontal and vertical
 * interpolation kernels have a "key" element. This element is positioned
 * over the 
 which The kernels are stored in double precision,
 * floating- and fixed-point form.  The fixed point representation has
 * a user-specified fractional precision.  It is the user's
 * responsibility to specify an appropriate level of precision that
 * will not cause overflow when accumulating the results of a
 * convolution against a set of source pixels, using 32-bit integer
 * arithmetic.
 */
public class InterpolationTable extends Interpolation {

    /** The number of fractional bits used to describe filter coefficients. */
    protected int precisionBits;

    /** The scaled (by 2<sup>precisionBits</sup>) value of 0.5 for rounding */
    private int round;

    /** The number of horizontal subpixel positions within a pixel. */
    private int numSubsamplesH;

    /** The number of vertical subpixel positions within a pixel. */
    private int numSubsamplesV;

    /** The horizontal coefficient data in double format. */
    protected double[] dataHd;

    /** The vertical coefficient data in double format. */
    protected double[] dataVd;

    /** The horizontal coefficient data in floating-point format. */
    protected float[] dataHf;

    /** The vertical coefficient data in floating-point format. */
    protected float[] dataVf;

    /** The horizontal coefficient data in fixed-point format. */
    protected int[] dataHi;

    /** The vertical coefficient data in fixed-point format. */
    protected int[] dataVi;

    /**
     * Constructs an InterpolationTable with specified horizontal and
     * vertical extents (support), number of horizontal and vertical
     * bins, fixed-point fractional precision, and int kernel entries.
     * The kernel data values are organized as 
     * <code>2<sup>subsampleBits</sup></code> entries each
     * containing width ints.
     *
     * <p> dataH and dataV are required to contain width * <code>2<sup>subsampleBitsH</sup></code>
     * and height * <code>2<sup>subsampleBitsV</sup></code> entries respectively, otherwise
     * an IllegalArgumentException will be thrown.
     *
     * <p> If dataV is null, it is assumed to be a copy of dataH
     * and the keyY, height, and subsampleBitsV parameters
     * are ignored.
     *
     * @param keyX The array offset of the horizontal resampling kernel center
     * @param keyY The array offset of the vertical resampling kernel center
     * @param width the width of a horizontal resampling kernel.
     * @param height the height of a vertical resampling kernel.  Ignored
     *        if dataV is null.
     * @param subsampleBitsH the log (base 2) of the number of horizontal
     *        subsample positions. Must be positive.
     * @param subsampleBitsV the log (base 2) of the number of vertical
     *        subsample positions. Must be positive. Ignored if dataV is null. 
     * @param precisionBits the number of bits of fractional precision
     *        to be used when resampling integral sample values. Must be positive.
     *        The same value is used for both horizontal and vertical
     *        resampling.
     * @param dataH the horizontal table entries, as an int array of
     *        <code>2<sup>subsampleBitsH</sup></code> entries each of length width. The array is cloned internally.
     * @param dataV the vertical table entries, as an int array of
     *        <code>2<sup>subsampleBitsV</sup></code> entries each of length height, or null. The array is cloned internally.
     *        If null, the dataH table is used for vertical interpolation
     *        as well and the keyY, height, and subsampleBitsV
     *        parameters are ignored.
     * @throws IllegalArgumentException if the size of the data arrays
     *         are incorrect.
     */
    public InterpolationTable(int keyX,
                              int keyY,
                              int width,
                              int height,
                              int subsampleBitsH,
                              int subsampleBitsV,
                              int precisionBits,
                              int[] dataH,
                              int[] dataV) {
        // dataH has width*2^subsampleBitsH entries
        // dataV has height*2^subsampleBitsV entries

        super();

        this.leftPadding = keyX;
        this.topPadding = keyY;
        this.width = width;
        this.rightPadding = width - keyX - 1;

        this.precisionBits = precisionBits;
        if (precisionBits > 0) {
            round = 1 << (precisionBits - 1);
        }

        this.subsampleBitsH = subsampleBitsH;
        this.numSubsamplesH = (1 << subsampleBitsH);
        int entriesH = width*numSubsamplesH;
	if (dataH.length != entriesH) {
	    throw new 
              IllegalArgumentException(JaiI18N.getString("InterpolationTable0"));
	}

        double prec = (double)(1 << precisionBits);
        int i;

        this.dataHi = (int[])dataH.clone();
        this.dataHf = new float[entriesH];
	this.dataHd = new double[entriesH];

        for (i = 0; i < entriesH; i++) {
            double d = (double)dataHi[i] / prec;
            this.dataHf[i] = (float)d;
            this.dataHd[i] = d;
        }

        if (dataV != null) {
            this.height = height;
            this.subsampleBitsV = subsampleBitsV;
            this.numSubsamplesV = (1 << subsampleBitsV);
            int entriesV = height*numSubsamplesV;
	    if (dataV.length != entriesV) {
		throw new 
              IllegalArgumentException(JaiI18N.getString("InterpolationTable1"));
	    }

            this.dataVi = (int[])dataV.clone();
            this.dataVf = new float[entriesV];
            this.dataVd = new double[entriesV];
            for (i = 0; i < entriesV; i++) {
                double d = (double)dataVi[i] / prec;
                this.dataVf[i] = (float)d;
                this.dataVd[i] = d;
            }
        } else {
            this.height = width; 
            this.subsampleBitsV = subsampleBitsH;
            this.numSubsamplesV = numSubsamplesH;
            this.dataVf = dataHf;
            this.dataVi = dataHi;
            this.dataVd = dataHd;
        }
        this.bottomPadding = this.height - keyY - 1;
    }

    /**
     * Constructs an InterpolationTable with identical horizontal and
     * vertical resampling kernels.
     *
     * @param key The array offset of the central sample to be used during resampling.
     * @param width the width or height of a resampling kernel.
     * @param subsampleBits the log (base 2) of the number of
     *        subsample positions. Must be positive. 
     * @param precisionBits the number of bits of fractional precision
     *        to be used when resampling integral sample values. Must be positive. 
     * @param data the kernel entries, as an int array of
     *        width*<code>2<sup>subsampleBits</sup></code> entries
     */
    public InterpolationTable(int key,
                              int width,
                              int subsampleBits,
                              int precisionBits,
                              int[] data) {

        this(key, key, width, width,
             subsampleBits, subsampleBits, precisionBits,
             data, null);
    }

    /**
     * Constructs an InterpolationTable with specified horizontal and
     * vertical extents (support), number of horizontal and vertical
     * bins, fixed-point fractional precision, and float kernel entries.
     * The kernel data values are organized as <code>2<sup>subsampleBits</sup></code> entries each
     * containing width floats.
     *
     * <p> dataH and dataV are required to contain width * <code>2<sup>subsampleBitsH</sup></code>
     * and height * <code>2<sup>subsampleBitsV</sup></code> entries respectively, otherwise
     * an IllegalArgumentException will be thrown.
     *
     * <p> If dataV is null, it is assumed to be a copy of dataH
     * and the keyY, height, and subsampleBitsV parameters
     * are ignored.
     *
     * @param keyX The array offset of the horizontal resampling kernel center
     * @param keyY The array offset of the vertical resampling kernel center
     * @param width the width of a horizontal resampling kernel.
     * @param height the height of a vertical resampling kernel.  Ignored
     *        if dataV is null.
     * @param subsampleBitsH the log (base 2) of the number of horizontal
     *        subsample positions. Must be positive. 
     * @param subsampleBitsV the log (base 2) of the number of vertical
     *        subsample positions. Must be positive.   Ignored if dataV is null.
     * @param precisionBits the number of bits of fractional precision
     *        to be used when resampling integral sample values.
     *        The same value is used for both horizontal and vertical
     *        resampling. Must be positive. 
     * @param dataH the horizontal table entries, as a float array of
     *        <code>2<sup>subsampleBitsH</sup></code> entries each of length width.
     * @param dataV the vertical table entries, as a float array of
     *        <code>2<sup>subsampleBitsV</sup></code> entries each of length height, or null.
     *        If null, the dataH table is used for vertical interpolation
     *        as well and the keyY, height, and subsampleBitsV
     *        parameters are ignored.
     * @throws IllegalArgumentException if the size of the data arrays
     *         are incorrect.
     */
    public InterpolationTable(int keyX,
                              int keyY,
                              int width,
                              int height,
                              int subsampleBitsH,
                              int subsampleBitsV,
                              int precisionBits,
                              float[] dataH,
                              float[] dataV) {
        // dataH has width*2^subsampleBitsH entries
        // dataV has height*2^subsampleBitsV entries

        super();

        this.leftPadding = keyX;
        this.topPadding = keyY;
        this.width = width;
        this.rightPadding = width - keyX - 1;

        this.precisionBits = precisionBits;
        if (precisionBits > 0) {
            round = 1 << (precisionBits - 1);
        } 

        this.subsampleBitsH = subsampleBitsH;
        this.numSubsamplesH = (1 << subsampleBitsH);
        int entriesH = width*numSubsamplesH;
	if (dataH.length != entriesH) {
	    throw new 
              IllegalArgumentException(JaiI18N.getString("InterpolationTable0"));
	}

        float prec = (float)(1 << precisionBits);
        int i;

        this.dataHf = (float[])dataH.clone();
        this.dataHi = new int[entriesH];
	this.dataHd = new double[entriesH];

        for (i = 0; i < entriesH; i++) {
	    float f = dataHf[i];
            this.dataHi[i] = Math.round(f * prec);
	    this.dataHd[i] = f;
        }

        if (dataV != null) {
            this.height = height;
            this.subsampleBitsV = subsampleBitsV;
            this.numSubsamplesV = (1 << subsampleBitsV);
            int entriesV = height*numSubsamplesV;
	    if (dataV.length != entriesV) {
		throw new 
              IllegalArgumentException(JaiI18N.getString("InterpolationTable1"));
	    }

            this.dataVf = (float[])dataV.clone();
            this.dataVi = new int[entriesV];
            this.dataVd = new double[entriesV];
            for (i = 0; i < entriesV; i++) {
		float f = dataVf[i];
                this.dataVi[i] = Math.round(f * prec);
		this.dataVd[i] = f;
            }
        } else {
            this.height = width; 
            this.subsampleBitsV = subsampleBitsH;
            this.numSubsamplesV = numSubsamplesH;
            this.dataVf = dataHf;
            this.dataVi = dataHi;
            this.dataVd = dataHd;
        }
        this.bottomPadding = this.height - keyY - 1;
    }

    /**
     * Constructs an InterpolationTable with identical horizontal and
     * vertical resampling kernels.
     *
     * @param key The number of samples to the left or above the
     *                    central sample to be used during resampling.
     * @param width the width or height of a resampling kernel.
     * @param subsampleBits the log (base 2) of the number of
     *        subsample positions. Must be positive. 
     * @param precisionBits the number of bits of fractional precision
     *        to be used when resampling integral sample values. Must be positive. 
     * @param data the kernel entries, as a float array of
     *        width*<code>2<sup>subsampleBits</sup></code> entries
     */
    public InterpolationTable(int key,
                              int width,
                              int subsampleBits,
                              int precisionBits,
                              float[] data) {

        this(key, key, width, width,
             subsampleBits, subsampleBits, precisionBits,
             data, null);
    }

    /**
     * Constructs an InterpolationTable with specified horizontal and
     * vertical extents (support), number of horizontal and vertical
     * bins, fixed-point fractional precision, and double kernel entries.
     * The kernel data values are organized as <code>2<sup>subsampleBits</sup></code> entries each
     * containing width doubles.
     *
     * <p> dataH and dataV are required to contain width * <code>2<sup>subsampleBitsH</sup></code>
     * and height * <code>2<sup>subsampleBitsV</sup></code> entries respectively, otherwise
     * an IllegalArgumentException will be thrown.
     *
     * <p> If dataV is null, it is assumed to be a copy of dataH
     * and the keyY, height, and subsampleBitsV parameters
     * are ignored.
     *
     * @param keyX The array offset of the horizontal resampling kernel center
     * @param keyY The array offset of the vertical resampling kernel center
     * @param width the width of a horizontal resampling kernel.
     * @param height the height of a vertical resampling kernel.  Ignored
     *        if dataV is null.
     * @param subsampleBitsH the log (base 2) of the number of horizontal
     *        subsample positions. Must be positive. 
     * @param subsampleBitsV the log (base 2) of the number of vertical
     *        subsample positions. Must be positive.   Ignored if dataV is null.
     * @param precisionBits the number of bits of fractional precision
     *        to be used when resampling integral sample values.
     *        The same value is used for both horizontal and vertical
     *        resampling. Must be positive. 
     * @param dataH the horizontal table entries, as a double array of
     *        <code>2<sup>subsampleBitsH</sup></code> entries each of length width.
     * @param dataV the vertical table entries, as a double array of
     *        <code>2<sup>subsampleBitsV</sup></code> entries each of length height, or null.
     *        If null, the dataH table is used for vertical interpolation
     *        as well and the keyY, height, and subsampleBitsV
     *        parameters are ignored.
     */
    public InterpolationTable(int keyX,
                              int keyY,
                              int width,
                              int height,
                              int subsampleBitsH,
                              int subsampleBitsV,
                              int precisionBits,
                              double[] dataH,
                              double[] dataV) {
        // dataH has width*2^subsampleBitsH entries
        // dataV has height*2^subsampleBitsV entries

        super();

        this.leftPadding = keyX;
        this.topPadding = keyY;
        this.width = width;
        this.rightPadding = width - keyX - 1;

        this.precisionBits = precisionBits;
        if (precisionBits > 0) {
            round = 1 << (precisionBits - 1);
        } 

        this.subsampleBitsH = subsampleBitsH;
        this.numSubsamplesH = (1 << subsampleBitsH);
        int entriesH = width*numSubsamplesH;
	if (dataH.length != entriesH) {
	    throw new 
              IllegalArgumentException(JaiI18N.getString("InterpolationTable0"));
	}

        double prec = (double)(1 << precisionBits);
        int i;

        this.dataHd = (double[])dataH.clone();
        this.dataHi = new int[entriesH];
        this.dataHf = new float[entriesH];
	for (i = 0; i < entriesH; i++) {
	    double d = dataHd[i];
            this.dataHi[i] = (int)Math.round(d*prec);
	    this.dataHf[i] = (float)d;
        }

        if (dataV != null) {
            this.height = height;
            this.subsampleBitsV = subsampleBitsV;
            this.numSubsamplesV = (1 << subsampleBitsV);
            int entriesV = height*numSubsamplesV;
	    if (dataV.length != entriesV) {
		throw new 
              IllegalArgumentException(JaiI18N.getString("InterpolationTable1"));
	    }
	    
            this.dataVd = (double[])dataV.clone();
            this.dataVi = new int[entriesV];
            this.dataVf = new float[entriesV];
            for (i = 0; i < entriesV; i++) {
		double d = dataVd[i];
                this.dataVi[i] = (int)Math.round(d * prec);
		this.dataVf[i] = (float)d;
            }
        } else {
            this.height = width; 
            this.subsampleBitsV = subsampleBitsH;
            this.numSubsamplesV = numSubsamplesH;
	    this.dataVd = dataHd;
            this.dataVf = dataHf;
            this.dataVi = dataHi;
        }
        this.bottomPadding = this.height - keyY - 1;
    }

    /**
     * Constructs an InterpolationTable with identical horizontal and
     * vertical resampling kernels.
     *
     * @param key The number of samples to the left or above the
     *                    central sample to be used during resampling.
     * @param width the width or height of a resampling kernel.
     * @param subsampleBits the log (base 2) of the number of
     *        subsample positions. Must be positive. 
     * @param precisionBits the number of bits of fractional precision
     *        to be used when resampling integral sample values. Must be positive. 
     * @param data the kernel entries, as a double array of
     *        width*<code>2<sup>subsampleBitsH</sup></code> entries
     */
    public InterpolationTable(int key,
                              int width,
                              int subsampleBits,
                              int precisionBits,
                              double[] data) {

        this(key, key, width, width,
             subsampleBits, subsampleBits, precisionBits,
             data, null);
    }

    /**
     * Returns the number of bits of fractional precision used to
     * store the fixed-point table entries.
     */
    public int getPrecisionBits() {
        return precisionBits;
    }

    /**
     * Returns the integer (fixed-point) horizontal table data.  The
     * output is an <code>int</code> array of length
     * <code>getWidth() * 2<sup>getSubsampleBitsH()</sup></code>.
     * 
     * <p> The following code, given an instance <code>interp</code>
     * of class <code>InterpolationTable</code>, will perform
     * interpolation of a set of <code>getWidth()</code> samples
     * at a given fractional position (bin) <code>xfrac</code>
     * between <code>0</code> and <code>2<sup>getSubsampleBitsH() - 1</sup></code>:
     *
     * <pre>
     * int interpolateH(InterpolationTable interp, int[] samples, int xfrac) {
     *     int[] dataH = interp.getHorizontalTableData();
     *     int precisionBits = interp.getPrecisionBits();
     *     int round = 1 << (precisionBits - 1);
     *     int width = interp.getWidth();
     *     int offset = width*xfrac;
     *
     *     int sum = 0;
     *     for (int i = 0; i < width; i++) {
     *         sum += dataH[offset + i]*samples[i];
     *     }
     *     return (sum + round) >> precisionBits;
     * }
     * </pre>
     *
     * <p> In practice, the values <code>dataH</code>,
     * <code>precisionBits</code>, etc., may be extracted once and
     * reused to interpolate multiple output pixels.
     *
     * @return An array of <code>int</code>s.
     */
    public int[] getHorizontalTableData() {
        return dataHi;
    }

    /**
     * Returns the integer (fixed-point) vertical table data.  The
     * output is an <code>int</code> array of length
     * <code>getHeight() * 2<sup>getSubsampleBitsV()</sup></code>.
     * 
     * <p> The following code, given an instance <code>interp</code>
     * of class <code>InterpolationTable</code>, will perform
     * interpolation of a set of <code>getHeight()</code> samples
     * at a given fractional position (bin) <code>yfrac</code>
     * between <code>0</code> and <code>2<sup>getSubsampleBitsV() - 1</sup></code>:
     *
     * <pre>
     * int interpolateV(InterpolationTable interp, int[] samples, int yfrac) {
     *     int[] dataV = interp.getVerticalTableData();
     *     int precisionBits = interp.getPrecisionBits();
     *     int round = 1 << (precisionBits - 1);
     *     int height = interp.getHeight();
     *     int offset = height*yfrac;
     *
     *     int sum = 0;
     *     for (int i = 0; i < height; i++) {
     *         sum += dataV[offset + i]*samples[i];
     *     }
     *     return (sum + round) >> precisionBits;
     * }
     * </pre>
     *
     * <p> In practice, the values <code>dataV</code>,
     * <code>precisionBits</code>, etc., may be extracted once and
     * reused to interpolate multiple output pixels.
     *
     * @return An array of <code>int</code>s.
     */
    public int[] getVerticalTableData() {
        return dataVi;
    }

    /**
     * Returns the floating-point horizontal table data.  The output is a
     * <code>float</code> array of length
     * <code>getWidth() * 2<sup>getSubsampleBitsH()</sup></code>.
     * 
     * <p> The following code, given an instance <code>interp</code>
     * of class <code>InterpolationTable</code>, will perform
     * interpolation of a set of <code>getWidth()</code>
     * floating-point samples at a given fractional position
     * <code>xfrac</code> between <code>0.0F</code> and <code>1.0F</code>:
     *
     * <pre>
     * float interpolateH(InterpolationTable interp,
     *                    float[] samples, float xfrac) {
     *     float[] dataH = interp.getHorizontalTableDataFloat();
     *     int width = interp.getWidth();
     *     int numSubsamplesH = 1 << getSubsampleBitsH();
     *     int ifrac = (int)(xfrac*numSubsamplesH);
     *     int offset = width*ifrac;
     *
     *     float sum = 0.0F;
     *     for (int i = 0; i < width; i++) {
     *         sum += dataH[offset + i]*samples[i];
     *     }
     *     return sum;
     * }
     * </pre>
     *
     * <p> In practice, the values <code>dataH</code>,
     * <code>numSubsamplesH</code>, etc., may be extracted once and
     * reused to interpolate multiple output pixels.
     *
     * @return An array of <code>float</code>s.
     */
    public float[] getHorizontalTableDataFloat() {
        return dataHf;
    }

    /**
     * Returns the floating-point vertical table data.  The output is a
     * <code>float</code> array of length
     * <code>getWidth() * 2<sup>getSubsampleBitsV()</sup></code>.
     * 
     * <p> The following code, given an instance <code>interp</code>
     * of class <code>InterpolationTable</code>, will perform
     * interpolation of a set of <code>getHeight()</code>
     * floating-point samples at a given fractional position
     * <code>yfrac</code> between <code>0.0F</code> and <code>1.0F</code>:
     *
     * <pre>
     * float interpolateV(InterpolationTable interp,
     *                    float[] samples, float yfrac) {
     *     float[] dataV = interp.getVerticalTableDataFloat();
     *     int height = interp.getHeight();
     *     int numSubsamplesV = 1 << getSubsampleBitsV();
     *     int ifrac = (int)(yfrac*numSubsamplesV);
     *     int offset = height*ifrac;
     *
     *     float sum = 0.0F;
     *     for (int i = 0; i < height; i++) {
     *         sum += dataV[offset + i]*samples[i];
     *     }
     *     return sum;
     * }
     * </pre>
     *
     * <p> In practice, the values <code>dataV</code>,
     * <code>numSubsamplesV</code>, etc., may be extracted once and
     * reused to interpolate multiple output pixels.
     *
     * @return An array of <code>float</code>s.
     */
    public float[] getVerticalTableDataFloat() {
        return dataVf;
    }

    /**
     * Returns the double horizontal table data.  The output is a
     * <code>double</code> array of length
     * <code>getWidth() * 2<sup>getSubsampleBitsH()</sup></code>.
     * 
     * <p> The following code, given an instance <code>interp</code>
     * of class <code>InterpolationTable</code>, will perform
     * interpolation of a set of <code>getWidth()</code>
     * double samples at a given fractional position
     * <code>xfrac</code> between <code>0.0F</code> and <code>1.0F</code>:
     *
     * <pre>
     * double interpolateH(InterpolationTable interp,
     *                     double[] samples, float xfrac) {
     *     double[] dataH = interp.getHorizontalTableDataDouble();
     *     int width = interp.getWidth();
     *     int numSubsamplesH = 1 << getSubsampleBitsH();
     *     int ifrac = (int)(xfrac*numSubsamplesH);
     *     int offset = width*ifrac;
     *
     *     double sum = 0.0;
     *     for (int i = 0; i < width; i++) {
     *         sum += dataH[offset + i]*samples[i];
     *     }
     *     return sum;
     * }
     * </pre>
     *
     * <p> In practice, the values <code>dataH</code>,
     * <code>numSubsamplesH</code>, etc., may be extracted once and
     * reused to interpolate multiple output pixels.
     *
     * @return An array of <code>double</code>s.
     */
    public double[] getHorizontalTableDataDouble() {
        return dataHd;
    }

    /**
     * Returns the double vertical table data.  The output is a
     * <code>double</code> array of length
     * <code>getHeight() * 2<sup>getSubsampleBitsV()</sup></code>).
     * 
     * <p> The following code, given an instance <code>interp</code>
     * of class <code>InterpolationTable</code>, will perform
     * interpolation of a set of <code>getHeight()</code>
     * double samples at a given fractional position
     * <code>yfrac</code> between <code>0.0F</code> and <code>1.0F</code>:
     *
     * <pre>
     * double interpolateV(InterpolationTable interp,
     *                     double[] samples, float yfrac) {
     *     double[] dataV = interp.getVerticalTableDataDouble();
     *     int height = interp.getHeight();
     *     int numSubsamplesV = 1 << getSubsampleBitsV();
     *     int ifrac = (int)(yfrac*numSubsamplesV);
     *     int offset = height*ifrac;
     *
     *     double sum = 0.0;
     *     for (int i = 0; i < height; i++) {
     *         sum += dataV[offset + i]*samples[i];
     *     }
     *     return sum;
     * }
     * </pre>
     *
     * <p> In practice, the values <code>dataV</code>,
     * <code>numSubsamplesV</code>, etc., may be extracted once and
     * reused to interpolate multiple output pixels.
     *
     * @return An array of <code>double</code>s.
     */
    public double[] getVerticalTableDataDouble() {
        return dataVd;
    }

    /**
     * Performs horizontal interpolation on a one-dimensional array of
     * integral samples.
     *
     * If xfrac does not lie between 0 and <code>2<sup>subsampleBitsH-1</sup></code>, an
     * ArrayIndexOutOfBoundsException may occur, where width is the width 
     * of the horizontal resampling kernel.
     *
     * @param samples an array of ints.
     * @param xfrac the subsample position, multiplied by <code>2<sup>subsampleBitsH</sup></code>.
     * @return the interpolated value as an int.
     * @throws ArrayIndexOutOfBoundsException if xfrac is out of bounds.
     */
    public int interpolateH(int[] samples, int xfrac) {
        int sum = 0;
        int offset = width*xfrac;

        for (int i = 0; i < width; i++) {
            sum += dataHi[offset + i]*samples[i];
        }
        return (sum + round) >> precisionBits;
    }

    /**
     * Performs vertical interpolation on a one-dimensional array of
     * integral samples.
     *
     * If yfrac does not lie between 0 and <code>2<sup>subsampleBitsV-1</sup></code>, an
     * ArrayIndexOutOfBoundsException may occur, where height is the 
     * height of the vertical resampling kernel.
     *
     * @param samples an array of ints.
     * @param yfrac the Y subsample position, multiplied by <code>2<sup>subsampleBitsV</sup></code>.
     * @return the interpolated value as an int.
     * @throws ArrayIndexOutOfBoundsException if yfrac is out of bounds.
     */
    public int interpolateV(int[] samples, int yfrac) {
        int sum = 0;
        int offset = width*yfrac;

        for (int i = 0; i < width; i++) {
            sum += dataVi[offset + i]*samples[i];
        }
        return (sum + round) >> precisionBits;
    }

    /**
     * Performs horizontal interpolation on a pair of integral samples.
     * This method may be used instead of the array version for speed.
     * It should only be called if width == 2.
     *
     * If xfrac does not lie between 0 and <code>2<sup>subsampleBitsH-1</sup></code>, an
     * ArrayIndexOutOfBoundsException may occur, where width is the width 
     * of the horizontal resampling kernel.
     *
     * @param s0 the central sample.
     * @param s1 the sample to the right of the central sample.
     * @param xfrac the subsample position, multiplied by <code>2<sup>subsampleBitsH</sup></code>.
     * @return the interpolated value as an int.
     * @throws ArrayIndexOutOfBoundsException if xfrac is out of bounds.
     */
    public int interpolateH(int s0, int s1, int xfrac) {
        // Assume width == 2
        int offset = 2*xfrac;
        int sum = dataHi[offset]*s0;
        sum += dataHi[offset + 1]*s1;
        return (sum + round) >> precisionBits;
    }

    /**
     * Performs horizontal interpolation on a quadruple of integral samples.
     * This method may be used instead of the array version for speed.
     * It should only be called if width == 4 and keyX == 1.
     *
     * If xfrac does not lie between 0 and <code>2<sup>subsampleBitsH-1</sup></code>, an
     * ArrayIndexOutOfBoundsException may occur, where width is the width 
     * of the horizontal resampling kernel.
     *
     * @param s_ the sample to the left of the central sample.
     * @param s0 the central sample.
     * @param s1 the sample to the right of the central sample.
     * @param s2 the sample to the right of s1.
     * @param xfrac the subsample position, multiplied by <code>2<sup>subsampleBitsH</sup></code>.
     * @return the interpolated value as an int.
     * @throws ArrayIndexOutOfBoundsException if xfrac is out of bounds.
     */
    public int interpolateH(int s_, int s0, int s1, int s2, int xfrac) {
        // Assume width == 4
        int offset = 4*xfrac;
        int sum = dataHi[offset]*s_;
        sum += dataHi[offset + 1]*s0;
        sum += dataHi[offset + 2]*s1;
        sum += dataHi[offset + 3]*s2;
        return (sum + round) >> precisionBits;
    }

    /**
     * Performs vertical interpolation on a pair of integral samples.
     * This method may be used instead of the array version for speed.
     * It should only be called if height == 2 and keyY == 0.
     *
     * If yfrac does not lie between 0 and <code>2<sup>subsampleBitsV-1</sup></code>, an
     * ArrayIndexOutOfBoundsException may occur, where height is the  
     * height of the vertical resampling kernel.
     *
     * @param s0 the central sample.
     * @param s1 the sample below the central sample.
     * @param yfrac the Y subsample position, multiplied by <code>2<sup>subsampleBitsV</sup></code>.
     * @return the interpolated value as an int.
     * @throws ArrayIndexOutOfBoundsException if yfrac is out of bounds.
     */
    public int interpolateV(int s0, int s1, int yfrac) {
        // Assume width == 2
        int offset = 2*yfrac;
        int sum = dataVi[offset]*s0;
        sum += dataVi[offset + 1]*s1;
        return (sum + round) >> precisionBits;
    }

    /**
     * Performs vertical interpolation on a quadruple of integral samples.
     * This method may be used instead of the array version for speed.
     * It should only be called if height == 4 and keyY == 1.
     *
     * If yfrac does not lie between 0 and <code>2<sup>subsampleBitsV-1</sup></code>, an
     * ArrayIndexOutOfBoundsException may occur, where height is the 
     * height of the vertical resampling kernel.
     *
     * @param s_ the sample above the central sample.
     * @param s0 the central sample.
     * @param s1 the sample below the central sample.
     * @param s2 the sample below s1.
     * @param yfrac the Y subsample position, multiplied by <code>2<sup>subsampleBitsV</sup></code>.
     * @return the interpolated value as an int.
     * @throws ArrayIndexOutOfBoundsException if yfrac is out of bounds.
     */
    public int interpolateV(int s_, int s0, int s1, int s2, int yfrac) {
        // Assume width == 4
        int offset = 4*yfrac;
        int sum = dataVi[offset]*s_;
        sum += dataVi[offset + 1]*s0;
        sum += dataVi[offset + 2]*s1;
        sum += dataVi[offset + 3]*s2;
        return (sum + round) >> precisionBits;
    }

    /**
     * Performs interpolation on a 2x2 grid of integral samples.
     * It should only be called if width == height == 2 and
     * keyX == keyY == 0.
     *
     * If xfrac does not lie between 0 and <code>2<sup>subsampleBitsH-1</sup></code>, or
     * yfrac does not lie between 0 and <code>2<sup>subsampleBitsV-1</sup></code>, an 
     * ArrayIndexOutOfBoundsException may occur, where width and height 
     * are the width and height of the horizontal and vertical resampling
     * kernels respectively.
     *
     * @param s00 the central sample.
     * @param s01 the sample to the right of the central sample.
     * @param s10 the sample below the central sample.
     * @param s11 the sample below and to the right of the central sample.
     * @param xfrac the X subsample position, multiplied by <code>2<sup>subsampleBitsH</sup></code>.
     * @param yfrac the Y subsample position, multiplied by <code>2<sup>subsampleBitsV</sup></code>.
     * @return the interpolated value as an int.
     * @throws ArrayIndexOutOfBoundsException if xfrac or yfrac are out of bounds.
     */
    public int interpolate(int s00, int s01,
                           int s10, int s11,
                           int xfrac, int yfrac) {
        // Interpolate in X
        int offsetX = 2*xfrac;
        int sum0 = dataHi[offsetX]*s00 + dataHi[offsetX + 1]*s01;
        int sum1 = dataHi[offsetX]*s10 + dataHi[offsetX + 1]*s11;

        // Intermediate rounding
        sum0 = (sum0 + round) >> precisionBits;
        sum1 = (sum1 + round) >> precisionBits;

        // Interpolate in Y
        int offsetY = 2*yfrac;
        int sum = dataVi[offsetY]*sum0 + dataVi[offsetY + 1]*sum1;

        return (sum + round) >> precisionBits;
    }

    /**
     * Performs interpolation on a 4x4 grid of integral samples.
     * It should only be called if width == height == 4 and
     * keyX == keyY == 1.
     *
     * If xfrac does not lie between 0 and <code>2<sup>subsampleBitsH-1</sup></code>, or
     * yfrac does not lie between 0 and <code>2<sup>subsampleBitsV-1</sup></code>, an
     * ArrayIndexOutOfBoundsException may occur, where width and height 
     * are the the width and height of the horizontal and vertical 
     * resampling kernels respectively.
     *
     * @param s__ the sample above and to the left of the central sample.
     * @param s_0 the sample above the central sample.
     * @param s_1 the sample above and one to the right of the central sample.
     * @param s_2 the sample above and two to the right of the central sample.
     * @param s0_ the sample to the left of the central sample.
     * @param s00 the central sample.
     * @param s01 the sample to the right of the central sample.
     * @param s02 the sample two to the right of the central sample.
     * @param s1_ the sample below and one to the left of the central sample.
     * @param s10 the sample below the central sample.
     * @param s11 the sample below and one to the right of the central sample.
     * @param s12 the sample below and two to the right of the central sample.
     * @param s2_ the sample two below and one to the left of the central sample.
     * @param s20 the sample two below the central sample.
     * @param s21 the sample two below and one to the right of the central sample.
     * @param s22 the sample two below and two to the right of the central sample.
     * @param xfrac the X subsample position, multiplied by <code>2<sup>subsampleBitsH</sup></code>.
     * @param yfrac the Y subsample position, multiplied by <code>2<sup>subsampleBitsV</sup></code>.
     * @return the interpolated value as an int.
     * @throws ArrayIndexOutOfBoundsException if xfrac or yfrac are out of bounds.
     */
    public int interpolate(int s__, int s_0, int s_1, int s_2,
                           int s0_, int s00, int s01, int s02,
                           int s1_, int s10, int s11, int s12,
                           int s2_, int s20, int s21, int s22,
                           int xfrac, int yfrac) {

	// Interpolate in X
        int offsetX = 4*xfrac;
	int offsetX1 = offsetX + 1;
	int offsetX2 = offsetX + 2;
	int offsetX3 = offsetX + 3;

	long sum_ = (long)dataHi[offsetX]*s__;
        sum_ += (long)dataHi[offsetX1]*s_0;
        sum_ += (long)dataHi[offsetX2]*s_1;
        sum_ += (long)dataHi[offsetX3]*s_2;

        long sum0 = (long)dataHi[offsetX]*s0_;
        sum0 += (long)dataHi[offsetX1]*s00;
        sum0 += (long)dataHi[offsetX2]*s01;
        sum0 += (long)dataHi[offsetX3]*s02;

        long sum1 = (long)dataHi[offsetX]*s1_;
        sum1 += (long)dataHi[offsetX1]*s10;
        sum1 += (long)dataHi[offsetX2]*s11;
        sum1 += (long)dataHi[offsetX3]*s12;

        long sum2 = (long)dataHi[offsetX]*s2_;
        sum2 += (long)dataHi[offsetX1]*s20;
        sum2 += (long)dataHi[offsetX2]*s21;
        sum2 += (long)dataHi[offsetX3]*s22;

        // Intermediate rounding
        sum_ = (sum_ + round) >> precisionBits;
        sum0 = (sum0 + round) >> precisionBits;
        sum1 = (sum1 + round) >> precisionBits;
        sum2 = (sum2 + round) >> precisionBits;

        // Interpolate in Y
        int offsetY = 4*yfrac;
        long sum = (long)dataVi[offsetY]*sum_;
        sum += (long)dataVi[offsetY + 1]*sum0;
        sum += (long)dataVi[offsetY + 2]*sum1;
        sum += (long)dataVi[offsetY + 3]*sum2;

	return (int)((sum + round) >> precisionBits);
    }

    /**
     * Performs interpolation on a 4x4 grid of integral samples. All
     * internal calculations are performed in floating-point.
     * It should only be called if width == height == 4 and
     * keyX == keyY == 1.
     *
     * If xfrac does not lie between 0 and <code>2<sup>subsampleBitsH-1</sup></code>, or
     * yfrac does not lie between 0 and <code>2<sup>subsampleBitsV-1</sup></code>, an
     * ArrayIndexOutOfBoundsException may occur, where width and height 
     * are the width and height of horizontal and vertical resampling
     * kernels respectively.
     *
     * @param s__ the sample above and to the left of the central sample.
     * @param s_0 the sample above the central sample.
     * @param s_1 the sample above and one to the right of the central sample.
     * @param s_2 the sample above and two to the right of the central sample.
     * @param s0_ the sample to the left of the central sample.
     * @param s00 the central sample.
     * @param s01 the sample to the right of the central sample.
     * @param s02 the sample two to the right of the central sample.
     * @param s1_ the sample below and one to the left of the central sample.
     * @param s10 the sample below the central sample.
     * @param s11 the sample below and one to the right of the central sample.
     * @param s12 the sample below and two to the right of the central sample.
     * @param s2_ the sample two below and one to the left of the central sample.
     * @param s20 the sample two below the central sample.
     * @param s21 the sample two below and one to the right of the central sample.
     * @param s22 the sample two below and two to the right of the central sample.
     * @param xfrac the X subsample position, multiplied by <code>2<sup>subsampleBitsH</sup></code>.
     * @param yfrac the Y subsample position, multiplied by <code>2<sup>subsampleBitsV</sup></code>.
     * @return the interpolated value as an int.
     * @throws ArrayIndexOutOfBoundsException if xfrac or yfrac are out of bounds.
     */
    public int interpolateF(int s__, int s_0, int s_1, int s_2,
			    int s0_, int s00, int s01, int s02,
			    int s1_, int s10, int s11, int s12,
			    int s2_, int s20, int s21, int s22,
			    int xfrac, int yfrac) {
	
	// Interpolate in X
        int offsetX = 4*xfrac;

        float sum_ = dataHf[offsetX]*s__;
        sum_ += dataHf[offsetX + 1]*s_0;
        sum_ += dataHf[offsetX + 2]*s_1;
        sum_ += dataHf[offsetX + 3]*s_2;

        float sum0 = dataHf[offsetX]*s0_;
        sum0 += dataHf[offsetX + 1]*s00;
        sum0 += dataHf[offsetX + 2]*s01;
        sum0 += dataHf[offsetX + 3]*s02;

        float sum1 = dataHf[offsetX]*s1_;
        sum1 += dataHf[offsetX + 1]*s10;
        sum1 += dataHf[offsetX + 2]*s11;
        sum1 += dataHf[offsetX + 3]*s12;

        float sum2 = dataHf[offsetX]*s2_;
        sum2 += dataHf[offsetX + 1]*s20;
        sum2 += dataHf[offsetX + 2]*s21;
        sum2 += dataHf[offsetX + 3]*s22;

        // Interpolate in Y
        int offsetY = 4*yfrac;
        float sum = dataVf[offsetY]*sum_;
        sum += dataVf[offsetY + 1]*sum0;
        sum += dataVf[offsetY + 2]*sum1;
        sum += dataVf[offsetY + 3]*sum2;

	int isum = (int)sum;

	return isum;
    }

    /**
     * Performs horizontal interpolation on a one-dimensional array of
     * floating-point samples representing a row of samples.
     *
     * If xfrac does not lie between the range [0.0, 1.0F), an
     * ArrayIndexOutOfBoundsException may occur.
     *
     * @param samples an array of floats.
     * @param xfrac the X subsample position, in the range [0.0F, 1.0F).
     * @return the interpolated value as a float.
     * @throws ArrayIndexOutOfBoundsException if xfrac is out of bounds.
     */
    public float interpolateH(float[] samples, float xfrac) {
        float sum = 0.0F;
        int ifrac = (int)(xfrac*numSubsamplesH);
        int offset = width*ifrac;

        for (int i = 0; i < width; i++) {
            sum += dataHf[offset + i]*samples[i];
        }
        return sum;
    }

    /**
     * Performs vertical interpolation on a one-dimensional array of
     * floating-point samples representing a column of samples.
     *
     * If yfrac does not lie between the range [0.0, 1.0F), an
     * ArrayIndexOutOfBoundsException may occur.
     *
     * @param samples an array of floats.
     * @param yfrac the Y subsample position, in the range [0.0F, 1.0F).
     * @return the interpolated value as a float.
     * @throws ArrayIndexOutOfBoundsException if yfrac is out of bounds.
     */
    public float interpolateV(float[] samples, float yfrac) {
        float sum = 0.0F;
        int ifrac = (int)(yfrac*numSubsamplesV);
        int offset = width*ifrac;

        for (int i = 0; i < width; i++) {
            sum += dataVf[offset + i]*samples[i];
        }
        return sum;
    }

    /**
     * Performs horizontal interpolation on a pair of floating-point samples.
     * This method may be used instead of the array version for speed.
     * It should only be called if width == 2.
     *
     * If xfrac does not lie between the range [0.0, 1.0F), an
     * ArrayIndexOutOfBoundsException may occur.
     *
     * @param s0 the central sample.
     * @param s1 the sample to the right of the central sample.
     * @param xfrac the X subsample position, in the range [0.0F, 1.0F).
     * @return the interpolated value as a float.
     * @throws ArrayIndexOutOfBoundsException if xfrac is out of bounds.
     */
    public float interpolateH(float s0, float s1, float xfrac) {
	float sum = 0.0F;
	int ifrac = (int)(xfrac * numSubsamplesH);
        // Assume width == 2
	int offset = 2 * ifrac;

	sum = dataHf[offset] * s0 + dataHf[offset+1] * s1;
        return sum;
    }

    /**
     * Performs horizontal interpolation on a quadruple of floating-point
     * samples. This method may be used instead of the array version for
     * speed. It should only be called if width == 4 and keyX == 1.
     *
     * If xfrac does not lie between the range [0.0, 1.0F), an
     * ArrayIndexOutOfBoundsException may occur.
     *
     * @param s_ the sample to the left of the central sample.
     * @param s0 the central sample.
     * @param s1 the sample to the right of the central sample.
     * @param s2 the sample to the right of s1.
     * @param xfrac the X subsample position, in the range [0.0F, 1.0F).
     * @return the interpolated value as a float.
     * @throws ArrayIndexOutOfBoundsException if xfrac is out of bounds.
     */
    public float interpolateH(float s_, float s0, float s1, float s2,
			      float xfrac) {
	int ifrac = (int)(xfrac * numSubsamplesH);
        // Assume width == 4
        int offset = 4 * ifrac;

        float sum = dataHf[offset] * s_;
        sum += dataHf[offset + 1] * s0;
        sum += dataHf[offset + 2] * s1;
        sum += dataHf[offset + 3] * s2;
        return sum;
    }

    /**
     * Performs vertical interpolation on a pair of floating-point samples.
     * This method may be used instead of the array version for speed.
     * It should only be called if height == 2 and keyY == 0.
     *
     * If yfrac does not lie between the range [0.0, 1.0F), an
     * ArrayIndexOutOfBoundsException may occur.
     *
     * @param s0 the central sample.
     * @param s1 the sample below the central sample.
     * @param yfrac the Y subsample position, in the range [0.0F, 1.0F).
     * @return the interpolated value as a float.
     * @throws ArrayIndexOutOfBoundsException if yfrac is out of bounds.
     */
    public float interpolateV(float s0, float s1, float yfrac) {
	int ifrac = (int)(yfrac * numSubsamplesV);
        // Assume width == 2
        int offset = 2 * ifrac;
        float sum = dataVf[offset] * s0;
        sum += dataVf[offset + 1] * s1;
        return sum;
    }

    /**
     * Performs vertical interpolation on a quadruple of floating-point
     * samples. This method may be used instead of the array version for
     * speed. It should only be called if height == 4 and keyY == 1.
     *
     * If yfrac does not lie between the range [0.0, 1.0F), an
     * ArrayIndexOutOfBoundsException may occur.
     *
     * @param s_ the sample above the central sample.
     * @param s0 the central sample.
     * @param s1 the sample below the central sample.
     * @param s2 the sample below s1.
     * @param yfrac the Y subsample position, in the range [0.0F, 1.0F).
     * @return the interpolated value as a float.
     * @throws ArrayIndexOutOfBoundsException if yfrac is out of bounds.
     */
    public float interpolateV(float s_, float s0, float s1, float s2,
			      float yfrac) {
	int ifrac = (int)(yfrac * numSubsamplesV);
        // Assume width == 4
        int offset = 4 * ifrac;
        float sum = dataVf[offset] * s_;
        sum += dataVf[offset + 1] * s0;
        sum += dataVf[offset + 2] * s1;
        sum += dataVf[offset + 3] * s2;
        return sum;
    }

    /**
     * Performs interpolation on a 2x2 grid of floating-point samples.
     * It should only be called if width == height == 2 and
     * keyX == keyY == 0.
     *
     * If either xfrac or yfrac does not lie between the range [0.0, 1.0F), an
     * ArrayIndexOutOfBoundsException may occur.
     *
     * @param s00 the central sample.
     * @param s01 the sample to the right of the central sample.
     * @param s10 the sample below the central sample.
     * @param s11 the sample below and to the right of the central sample.
     * @param xfrac the X subsample position, in the range [0.0F, 1.0F).
     * @param yfrac the Y subsample position, in the range [0.0F, 1.0F).
     * @return the interpolated value as a float.
     * @throws ArrayIndexOutOfBoundsException if xfrac or yfrac are out of bounds.
     */
    public float interpolate(float s00, float s01,
			     float s10, float s11,
			     float xfrac, float yfrac) {
	int ifrac = (int)(xfrac * numSubsamplesH);
        // Interpolate in X
        int offsetX = 2 * ifrac;
        float sum0 = dataHf[offsetX]*s00 + dataHf[offsetX + 1]*s01;
        float sum1 = dataHf[offsetX]*s10 + dataHf[offsetX + 1]*s11;

        // Interpolate in Y
	ifrac = (int)(yfrac * numSubsamplesV);
        int offsetY = 2 * ifrac;
        float sum = dataVf[offsetY]*sum0 + dataVf[offsetY + 1]*sum1;

        return sum;
    }

    /**
     * Performs interpolation on a 4x4 grid of floating-point samples.
     * It should only be called if width == height == 4 and
     * keyX == keyY == 1.
     *
     * If either xfrac or yfrac does not lie between the range [0.0, 1.0F), an
     * ArrayIndexOutOfBoundsException may occur.
     *
     * @param s__ the sample above and to the left of the central sample.
     * @param s_0 the sample above the central sample.
     * @param s_1 the sample above and one to the right of the central sample.
     * @param s_2 the sample above and two to the right of the central sample.
     * @param s0_ the sample to the left of the central sample.
     * @param s00 the central sample.
     * @param s01 the sample to the right of the central sample.
     * @param s02 the sample two to the right of the central sample.
     * @param s1_ the sample below and one to the left of the central sample.
     * @param s10 the sample below the central sample.
     * @param s11 the sample below and one to the right of the central sample.
     * @param s12 the sample below and two to the right of the central sample.
     * @param s2_ the sample two below and one to the left of the central sample.
     * @param s20 the sample two below the central sample.
     * @param s21 the sample two below and one to the right of the central sample.
     * @param s22 the sample two below and two to the right of the central sample.
     * @param xfrac the X subsample position, in the range [0.0F, 1.0F).
     * @param yfrac the Y subsample position, in the range [0.0F, 1.0F).
     * @return the interpolated value as a float.
     * @throws ArrayIndexOutOfBoundsException if xfrac or yfrac are out of bounds.
     */
    public float interpolate(float s__, float s_0, float s_1, float s_2,
			     float s0_, float s00, float s01, float s02,
			     float s1_, float s10, float s11, float s12,
			     float s2_, float s20, float s21, float s22,
			     float xfrac, float yfrac) {

	int ifrac = (int)(xfrac * numSubsamplesH);
	// Interpolate in X
        int offsetX = 4 * ifrac;
	int offsetX1 = offsetX + 1;
	int offsetX2 = offsetX + 2;
	int offsetX3 = offsetX + 3;

	float sum_ = dataHf[offsetX]*s__;
        sum_ += dataHf[offsetX1]*s_0;
        sum_ += dataHf[offsetX2]*s_1;
        sum_ += dataHf[offsetX3]*s_2;

        float sum0 = dataHf[offsetX]*s0_;
        sum0 += dataHf[offsetX1]*s00;
        sum0 += dataHf[offsetX2]*s01;
        sum0 += dataHf[offsetX3]*s02;

        float sum1 = dataHf[offsetX]*s1_;
        sum1 += dataHf[offsetX1]*s10;
        sum1 += dataHf[offsetX2]*s11;
        sum1 += dataHf[offsetX3]*s12;

        float sum2 = dataHf[offsetX]*s2_;
        sum2 += dataHf[offsetX1]*s20;
        sum2 += dataHf[offsetX2]*s21;
        sum2 += dataHf[offsetX3]*s22;

        // Interpolate in Y
	ifrac = (int)(yfrac * numSubsamplesV);
        int offsetY = 4 * ifrac;
        float sum = dataVf[offsetY]*sum_;
        sum += dataVf[offsetY + 1]*sum0;
        sum += dataVf[offsetY + 2]*sum1;
        sum += dataVf[offsetY + 3]*sum2;

	return sum;
    }

    /**
     * Performs horizontal interpolation on a one-dimensional array of
     * double samples representing a row of samples.
     *
     * If xfrac does not lie between the range [0.0, 1.0F), an
     * ArrayIndexOutOfBoundsException may occur.
     *
     * @param samples an array of doubles.
     * @param xfrac the X subsample position, in the range [0.0F, 1.0F).
     * @return the interpolated value as a double.
     * @throws ArrayIndexOutOfBoundsException if xfrac is out of bounds.
     */
    public double interpolateH(double[] samples, float xfrac) {
        double sum = 0.0;
        int ifrac = (int)(xfrac * numSubsamplesH);
        int offset = width * ifrac;

        for (int i = 0; i < width; i++) {
            sum += dataHd[offset+i] * samples[i];
        }
        return sum;
    }

    /**
     * Performs vertical interpolation on a one-dimensional array of
     * double samples representing a column of samples.
     *
     * If yfrac does not lie between the range [0.0, 1.0F), an
     * ArrayIndexOutOfBoundsException may occur.
     *
     * @param samples an array of doubles.
     * @param yfrac the Y subsample position, in the range [0.0F, 1.0F).
     * @return the interpolated value as a double.
     * @throws ArrayIndexOutOfBoundsException if yfrac is out of bounds.
     */
    public double interpolateV(double[] samples, float yfrac) {
        double sum = 0.0;
        int ifrac = (int)(yfrac * numSubsamplesV);
        int offset = width * ifrac;

        for (int i = 0; i < width; i++) {
            sum += dataVd[offset+i] * samples[i];
        }
        return sum;
    }

    /**
     * Performs horizontal interpolation on a pair of double samples.
     * This method may be used instead of the array version for speed.
     * It should only be called if width == 2.
     *
     * If xfrac does not lie between the range [0.0, 1.0F), an
     * ArrayIndexOutOfBoundsException may occur.
     *
     * @param s0 the central sample.
     * @param s1 the sample to the right of the central sample.
     * @param xfrac the X subsample position, in the range [0.0F, 1.0F).
     * @return the interpolated value as a double.
     * @throws ArrayIndexOutOfBoundsException if xfrac is out of bounds.
     */
    public double interpolateH(double s0, double s1, float xfrac) {
	double sum = 0.0F;
	int ifrac = (int)(xfrac * numSubsamplesH);
        // Assume width == 2
	int offset = 2 * ifrac;

	sum = dataHd[offset] * s0 + dataHd[offset+1] * s1;
        return sum;
    }

    /**
     * Performs horizontal interpolation on a quadruple of double
     * samples. This method may be used instead of the array version for
     * speed. It should only be called if width == 4 and keyX == 1.
     *
     * If xfrac does not lie between the range [0.0, 1.0F), an
     * ArrayIndexOutOfBoundsException may occur.
     *
     * @param s_ the sample to the left of the central sample.
     * @param s0 the central sample.
     * @param s1 the sample to the right of the central sample.
     * @param s2 the sample to the right of s1.
     * @param xfrac the X subsample position, in the range [0.0F, 1.0F).
     * @return the interpolated value as a double.
     * @throws ArrayIndexOutOfBoundsException if xfrac is out of bounds.
     */
    public double interpolateH(double s_, double s0, double s1, double s2,
			       float xfrac) {
	int ifrac = (int)(xfrac * numSubsamplesH);
        // Assume width == 4
        int offset = 4 * ifrac;

        double sum = dataHd[offset] * s_;
        sum += dataHd[offset + 1] * s0;
        sum += dataHd[offset + 2] * s1;
        sum += dataHd[offset + 3] * s2;
        return sum;
    }

    /**
     * Performs vertical interpolation on a pair of double samples.
     * This method may be used instead of the array version for speed.
     * It should only be called if height == 2 and keyY == 0.
     *
     * If yfrac does not lie between the range [0.0, 1.0F), an
     * ArrayIndexOutOfBoundsException may occur.
     *
     * @param s0 the central sample.
     * @param s1 the sample below the central sample.
     * @param yfrac the Y subsample position, in the range [0.0F, 1.0F).
     * @return the interpolated value as a double.
     * @throws ArrayIndexOutOfBoundsException if yfrac is out of bounds.
     */
    public double interpolateV(double s0, double s1, float yfrac) {
	int ifrac = (int)(yfrac * numSubsamplesV);
        // Assume width == 2
        int offset = 2 * ifrac;
        double sum = dataVd[offset] * s0;
        sum += dataVd[offset + 1] * s1;
        return sum;
    }

    /**
     * Performs vertical interpolation on a quadruple of double
     * samples. This method may be used instead of the array version for
     * speed. It should only be called if height == 4 and keyY == 1.
     *
     * If yfrac does not lie between the range [0.0, 1.0F), an
     * ArrayIndexOutOfBoundsException may occur.
     *
     * @param s_ the sample above the central sample.
     * @param s0 the central sample.
     * @param s1 the sample below the central sample.
     * @param s2 the sample below s1.
     * @param yfrac the Y subsample position, in the range [0.0F, 1.0F).
     * @return the interpolated value as a double.
     * @throws ArrayIndexOutOfBoundsException if yfrac is out of bounds.
     */
    public double interpolateV(double s_, double s0, double s1, double s2,
			       float yfrac) {
	int ifrac = (int)(yfrac * numSubsamplesV);
        // Assume width == 4
        int offset = 4 * ifrac;
        double sum = dataVd[offset] * s_;
        sum += dataVd[offset + 1] * s0;
        sum += dataVd[offset + 2] * s1;
        sum += dataVd[offset + 3] * s2;
        return sum;
    }

    /**
     * Performs interpolation on a 2x2 grid of double samples.
     * It should only be called if width == height == 2 and
     * keyX == keyY == 0.
     *
     * If either xfrac or yfrac does not lie between the range [0.0, 1.0F), an
     * ArrayIndexOutOfBoundsException may occur.
     *
     * @param s00 the central sample.
     * @param s01 the sample to the right of the central sample.
     * @param s10 the sample below the central sample.
     * @param s11 the sample below and to the right of the central sample.
     * @param xfrac the X subsample position, in the range [0.0F, 1.0F).
     * @param yfrac the Y subsample position, in the range [0.0F, 1.0F).
     * @return the interpolated value as a double.
     * @throws ArrayIndexOutOfBoundsException if xfrac or yfrac are out of bounds.
     */
    public double interpolate(double s00, double s01,
			      double s10, double s11,
			      float xfrac, float yfrac) {
	int ifrac = (int)(xfrac * numSubsamplesH);
        // Interpolate in X
        int offsetX = 2 * ifrac;
        double sum0 = dataHd[offsetX]*s00 + dataHd[offsetX + 1]*s01;
        double sum1 = dataHd[offsetX]*s10 + dataHd[offsetX + 1]*s11;

        // Interpolate in Y
	ifrac = (int)(yfrac * numSubsamplesV);
        int offsetY = 2 * ifrac;
        double sum = dataVd[offsetY]*sum0 + dataVd[offsetY + 1]*sum1;

        return sum;
    }

    /**
     * Performs interpolation on a 4x4 grid of double samples.
     * It should only be called if width == height == 4 and
     * keyX == keyY == 1.
     *
     * If either xfrac or yfrac does not lie between the range [0.0, 1.0F), an
     * ArrayIndexOutOfBoundsException may occur.
     *
     * @param s__ the sample above and to the left of the central sample.
     * @param s_0 the sample above the central sample.
     * @param s_1 the sample above and one to the right of the central sample.
     * @param s_2 the sample above and two to the right of the central sample.
     * @param s0_ the sample to the left of the central sample.
     * @param s00 the central sample.
     * @param s01 the sample to the right of the central sample.
     * @param s02 the sample two to the right of the central sample.
     * @param s1_ the sample below and one to the left of the central sample.
     * @param s10 the sample below the central sample.
     * @param s11 the sample below and one to the right of the central sample.
     * @param s12 the sample below and two to the right of the central sample.
     * @param s2_ the sample two below and one to the left of the central sample.
     * @param s20 the sample two below the central sample.
     * @param s21 the sample two below and one to the right of the central sample.
     * @param s22 the sample two below and two to the right of the central sample.
     * @param xfrac the X subsample position, in the range [0.0F, 1.0F).
     * @param yfrac the Y subsample position, in the range [0.0F, 1.0F).
     * @return the interpolated value as a double.
     * @throws ArrayIndexOutOfBoundsException if xfrac or yfrac are out of bounds.
     */
    public double interpolate(double s__, double s_0, double s_1, double s_2,
			      double s0_, double s00, double s01, double s02,
			      double s1_, double s10, double s11, double s12,
			      double s2_, double s20, double s21, double s22,
			      float xfrac, float yfrac) {

	int ifrac = (int)(xfrac * numSubsamplesH);
	// Interpolate in X
        int offsetX = 4 * ifrac;
	int offsetX1 = offsetX + 1;
	int offsetX2 = offsetX + 2;
	int offsetX3 = offsetX + 3;

	double sum_ = dataHd[offsetX]*s__;
        sum_ += dataHd[offsetX1]*s_0;
        sum_ += dataHd[offsetX2]*s_1;
        sum_ += dataHd[offsetX3]*s_2;

        double sum0 = dataHd[offsetX]*s0_;
        sum0 += dataHd[offsetX1]*s00;
        sum0 += dataHd[offsetX2]*s01;
        sum0 += dataHd[offsetX3]*s02;

        double sum1 = dataHd[offsetX]*s1_;
        sum1 += dataHd[offsetX1]*s10;
        sum1 += dataHd[offsetX2]*s11;
        sum1 += dataHd[offsetX3]*s12;

        double sum2 = dataHd[offsetX]*s2_;
        sum2 += dataHd[offsetX1]*s20;
        sum2 += dataHd[offsetX2]*s21;
        sum2 += dataHd[offsetX3]*s22;

        // Interpolate in Y
	ifrac = (int)(yfrac * numSubsamplesV);
        int offsetY = 4 * ifrac;
        double sum = dataVd[offsetY]*sum_;
        sum += dataVd[offsetY + 1]*sum0;
        sum += dataVd[offsetY + 2]*sum1;
        sum += dataVd[offsetY + 3]*sum2;

	return sum;
    }
}





