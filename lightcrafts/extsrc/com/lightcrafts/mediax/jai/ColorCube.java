/*
 * $RCSfile: ColorCube.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:06 $
 * $State: Exp $
 */
package com.lightcrafts.mediax.jai;
import java.awt.image.DataBuffer;
import com.lightcrafts.mediax.jai.LookupTableJAI;


/**
 * A subclass of <code>LookupTableJAI</code> which represents a lookup
 * table which is a color cube.  A color cube provides a fixed,
 * invertible mapping between table indices and sample values.
 * This allows the <code>findNearestEntry</code> method to be implemented
 * more efficiently than in the general case.
 *
 * <p> All constructors are protected. The correct way to create a
 * <code>ColorCube</code> is to use one of the static
 * <code>create</code> methods defined in this class.
 *
 * @see com.lightcrafts.mediax.jai.LookupTableJAI
 *
 */
public class ColorCube extends LookupTableJAI {
    /**
     * A <code>ColorCube</code> for dithering RGB byte data into 216 colors.
     * The offset of this <code>ColorCube</code> is 38.
     */
    public static final ColorCube BYTE_496 =
        ColorCube.createColorCube(DataBuffer.TYPE_BYTE,
                                  38, new int[] {4, 9, 6});

    /**
     * A <code>ColorCube</code> for dithering YCC byte data into 200 colors.
     * The offset of this <code>ColorCube</code> is 54.
     */
    public static final ColorCube BYTE_855 =
        ColorCube.createColorCube(DataBuffer.TYPE_BYTE,
                                  54, new int[] {8, 5, 5});

    /** The signed array of sizes used to create the <code>ColorCube</code>. */
    private int[] dimension;

    /**
     * An array of positive values each of whose elements is one less than the
     * absolute value of the corresponding element of the dimension array.
     */
    private int[] dimsLessOne;

    /**
     * An array of multipliers.
     *
     * <p> The magnitudes of the elements of the multiplier array are
     * defined as <code>multipliers[0] = 1</code> and
     * <code>multipliers[i] =
     * multipliers[i-1]*Math.abs(dimension[i-1])</code> where <code>i
     * > 0</code>. The elements are subsequently assigned the same
     * sign (positive or negative) as the corresponding elements of
     * the dimension array.
     */
    private int[] multipliers;

    /**
     * An offset into the lookup table, accounting for negative dimensions.
     */
    private int adjustedOffset;

    /**
     * The data type cached to accelerate findNearestEntry().
     */
    private int dataType;

    /**
     * The number of bands cached to accelerate findNearestEntry().
     */
    private int numBands;

    /**
     * Returns a multi-banded <code>ColorCube</code> of a specified data type.
     *
     * @param dataType The data type of the <code>ColorCube</code>,
     *        one of <code>DataBuffer.TYPE_BYTE</code>,
     *        <code>TYPE_SHORT</code>,
     *        <code>TYPE_USHORT</code>,
     *        <code>TYPE_INT</code>,
     *        <code>TYPE_FLOAT</code>,
     *        or <code>TYPE_DOUBLE</code>.
     * @param offset The common offset for all bands.
     * @param dimension The signed dimension of each band.
     *
     * @throws RuntimeExceptions for unsupported data types
     * @return An appropriate <code>ColorCube</code>.
     */
    public static ColorCube createColorCube(int dataType,
                                            int offset,
                                            int dimension[]) {
        ColorCube colorCube;

        switch(dataType) {
        case DataBuffer.TYPE_BYTE:
            colorCube = createColorCubeByte(offset, dimension);
            break;
        case DataBuffer.TYPE_SHORT:
            colorCube = createColorCubeShort(offset, dimension);
            break;
        case DataBuffer.TYPE_USHORT:
            colorCube = createColorCubeUShort(offset, dimension);
            break;
        case DataBuffer.TYPE_INT:
            colorCube = createColorCubeInt(offset, dimension);
            break;
        case DataBuffer.TYPE_FLOAT:
            colorCube = createColorCubeFloat(offset, dimension);
            break;
        case DataBuffer.TYPE_DOUBLE:
            colorCube = createColorCubeDouble(offset, dimension);
            break;
        default:
            throw new RuntimeException(JaiI18N.getString("ColorCube0"));
        }

        return colorCube;
    }

    /**
     * Returns a multi-banded <code>ColorCube</code> of a specified
     * data type with zero offset for all bands.
     *
     * @param dataType The data type of the <code>ColorCube</code>.
     * @param dimension The signed dimension of each band.
     *
     * @throws IllegalArgumentException if dimension is null.
     * @return An appropriate <code>ColorCube</code>.
     */
    public static ColorCube createColorCube(int dataType, int dimension[]) {

        if ( dimension == null ) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        return createColorCube(dataType, 0, dimension);
    }

    /**
     * Returns a multi-banded byte <code>ColorCube</code> with an index
     * offset common to all bands.
     *
     * @param offset The common offset for all bands.
     * @param dimension An array of signed sizes of each side of the color
     * cube. The color ramp in each dimension will be increasing or decreasing
     * according to whether the sign of the corresponding element of the
     * <code>dimension</code> array is positive or negative, respectively.
     *
     * @return A multi-banded byte <code>ColorCube</code> with offset.
     */
    private static ColorCube createColorCubeByte(int offset,
                                                 int dimension[]) {
        ColorCube colorCube =
            new ColorCube(createDataArrayByte(offset, dimension), offset);
        colorCube.initFields(offset, dimension);
        return colorCube;
    }

    /**
     * Returns a multi-banded short <code>ColorCube</code> with an index
     * offset common to all bands.
     *
     * @param offset The common offset for all bands.
     * @param dimension An array of signed sizes of each side of the color
     * cube. The color ramp in each dimension will be increasing or decreasing
     * according to whether the sign of the corresponding element of the
     * <code>dimension</code> array is positive or negative, respectively.
     *
     * @return A multi-banded short <code>ColorCube</code> with offset.
     */
    private static ColorCube createColorCubeShort(int offset,
                                                  int dimension[]) {
        ColorCube colorCube =
            new ColorCube(createDataArrayShort(offset, dimension), offset,
                          false);
        colorCube.initFields(offset, dimension);
        return colorCube;
    }

    /**
     * Returns a multi-banded unsigned short <code>ColorCube</code> with an
     * index offset common to all bands.
     *
     * @param offset The common offset for all bands.
     * @param dimension An array of signed sizes of each side of the color
     * cube. The color ramp in each dimension will be increasing or decreasing
     * according to whether the sign of the corresponding element of the
     * <code>dimension</code> array is positive or negative, respectively.
     *
     * @return A multi-banded unsigned short <code>ColorCube</code> with
     * offset.
     */
    private static ColorCube createColorCubeUShort(int offset,
                                                   int dimension[]) {
        ColorCube colorCube =
            new ColorCube(createDataArrayUShort(offset, dimension), offset,
                          true);
        colorCube.initFields(offset, dimension);
        return colorCube;
    }

    /**
     * Returns a multi-banded int <code>ColorCube</code> with an index
     * offset common to all bands.
     *
     * @param offset The common offset for all bands.
     * @param dimension An array of signed sizes of each side of the color
     * cube. The color ramp in each dimension will be increasing or decreasing
     * according to whether the sign of the corresponding element of the
     * <code>dimension</code> array is positive or negative, respectively.
     *
     * @return A multi-banded int <code>ColorCube</code> with offset.
     */
    private static ColorCube createColorCubeInt(int offset,
                                                int dimension[]) {
        ColorCube colorCube =
            new ColorCube(createDataArrayInt(offset, dimension), offset);
        colorCube.initFields(offset, dimension);
        return colorCube;
    }

    /**
     * Returns a multi-banded float <code>ColorCube</code> with an index
     * offset common to all bands.
     *
     * @param offset The common offset for all bands.
     * @param dimension An array of signed sizes of each side of the color
     * cube. The color ramp in each dimension will be increasing or decreasing
     * according to whether the sign of the corresponding element of the
     * <code>dimension</code> array is positive or negative, respectively.
     *
     * @return A multi-banded float <code>ColorCube</code> with offset.
     */
    private static ColorCube createColorCubeFloat(int offset,
                                                  int dimension[]) {
        ColorCube colorCube =
            new ColorCube(createDataArrayFloat(offset, dimension), offset);
        colorCube.initFields(offset, dimension);
        return colorCube;
    }

    /**
     * Returns a multi-banded double <code>ColorCube</code> with an index
     * offset common to all bands.
     *
     * @param offset The common offset for all bands.
     * @param dimension An array of signed sizes of each side of the color
     * cube. The color ramp in each dimension will be increasing or decreasing
     * according to whether the sign of the corresponding element of the
     * <code>dimension</code> array is positive or negative, respectively.
     *
     * @return A multi-banded double <code>ColorCube</code>.
     */
    private static ColorCube createColorCubeDouble(int offset,
                                                   int dimension[]) {
        ColorCube colorCube =
            new ColorCube(createDataArrayDouble(offset, dimension),
                          offset);
        colorCube.initFields(offset, dimension);
        return colorCube;
    }

    /**
     * Constructs a two-dimensional array of the requested data type which
     * represents the contents of a color cube.
     *
     * @param dataType The data type as defined by the static TYPE fields of
     * <code>DataBuffer</code>, e.g., <code>DataBuffer.TYPE_BYTE</code>.
     * @param offset The initial offset into the data array.
     * @param dimension An array of signed sizes of each side of the color
     * cube. The color ramp in each dimension will be increasing or decreasing
     * according to whether the sign of the corresponding element of the
     * <code>dimension</code> array is positive or negative, respectively.
     *
     * @return A two-dimensional array of the requested data type laid
     * out in color cube format.
     * 
     * @throws RuntimeException for data types other than
     *     DataBuffer.TYPE_BYTE DataBuffer.TYPE_USHORT
     *     DataBuffer.TYPE_SHORT DataBuffer.TYPE_INT
     *     DataBuffer.TYPE_FLOAT DataBuffer.TYPE_DOUBLE
     * @see java.awt.image.DataBuffer
     */
    private static Object createDataArray(int dataType,
                                          int offset,
                                          int dimension[]) {
        // Make sure that the dimension array has non-zero length.
        int nbands = dimension.length;
        if (nbands == 0) {
            throw new RuntimeException(JaiI18N.getString("ColorCube1"));
        }

        // Ascertain that all dimension are non-zero.
        for (int band = 0; band < nbands; band++) {
            if (dimension[band] == 0) {
                throw new RuntimeException(JaiI18N.getString("ColorCube2"));
            }
        }

        // Copy the dimension into an array of dimension magnitudes.
        int[] dimensionAbs = new int[nbands];
        for (int band = 0; band < nbands; band++) {
            dimensionAbs[band] = Math.abs(dimension[band]);
        }

        // Check that the color cube is not too large for the machine.
        double floatSize = dimensionAbs[0];
        for (int band = 1; band < nbands; band++) {
            floatSize *= (double)dimensionAbs[band];
        }
        if (floatSize > (double)Integer.MAX_VALUE) {
            //
            //  Color cube is too large for 32 bit addressability
            //
            throw new RuntimeException(JaiI18N.getString("ColorCube3"));
        }
        int size = (int)floatSize;
    
        // Initialize the data array and extrema for this data type.
        double dataMin;
        double dataMax;
        Object dataArray;
        switch(dataType) {
            case DataBuffer.TYPE_BYTE:
                dataMin = 0.0;
                dataMax = 255.0;
                dataArray = (Object)new byte[nbands][size];
                break;
            case DataBuffer.TYPE_SHORT:
                dataMin = Short.MIN_VALUE;
                dataMax = Short.MAX_VALUE;
                dataArray = (Object)new short[nbands][size];
                break;
            case DataBuffer.TYPE_USHORT:
                dataMin = 0;
                dataMax = 65535;
                dataArray = (Object)new short[nbands][size];
                break;
            case DataBuffer.TYPE_INT:
                dataMin = Integer.MIN_VALUE;
                dataMax = Integer.MAX_VALUE;
                dataArray = (Object)new int[nbands][size];
                break;
            case DataBuffer.TYPE_FLOAT:
                dataMin = -Float.MAX_VALUE;
                dataMax = Float.MAX_VALUE;
                dataArray = (Object)new float[nbands][size];
                break;
            case DataBuffer.TYPE_DOUBLE:
                dataMin = -Double.MAX_VALUE;
                dataMax = Double.MAX_VALUE;
                dataArray = (Object)new double[nbands][size];
                break;
            default:
                throw new RuntimeException(JaiI18N.getString("ColorCube7"));
        }

        // Ensure that the parameters don't go out of range.
        if ((double)(size + offset) > dataMax) {
            throw new RuntimeException(JaiI18N.getString("ColorCube4"));
        }

        // Initialize the multipliers
        int[] multipliers = new int[nbands];
        multipliers[0] = 1;
        for (int band = 1; band < nbands; band++) {
            multipliers[band] = multipliers[band-1]*dimensionAbs[band-1];
        }

        // Populate each band of the lookup table data.
        for (int band = 0; band < nbands; band++) {
            // Determine the step size for this band.
            int idimension = dimensionAbs[band];
            double delta;
            if (idimension == 1) {
                // A dimension of one means all entries will be the same
                delta = 0.0;
            } else if (dataType == DataBuffer.TYPE_FLOAT ||
                       dataType == DataBuffer.TYPE_DOUBLE) {
                delta = 1.0/(idimension - 1);
            } else {
                delta = (dataMax - dataMin)/(idimension - 1);
            }

            // Set the starting value and index step.
            double start;
            if (dimension[band] < 0) {
                delta = -delta;
                start = dataMax;
            } else {
                start = dataMin;
            }
            int repeatCount = multipliers[band];

            // Load the actual lookup table values for this band
            int index;
            switch(dataType) {
                case DataBuffer.TYPE_BYTE:
                    byte[][] byteData = (byte[][])dataArray;
                    index = 0;
                    while (index < size) {
                        double val = start;
                        for (int i = 0; i < idimension; i++) {
                            for (int j = 0; j < repeatCount; j++) {
                                byteData[band][index] =
                                    (byte)((int)(val + 0.5) & 0x000000ff);
                                index++;
                            }
                            val += delta;
                        }
                    }
                    break;

                case DataBuffer.TYPE_SHORT:
                case DataBuffer.TYPE_USHORT:
                    short[][] shortData = (short[][])dataArray;
                    index = 0;
                    while (index < size) {
                        double val = start;
                        for (int i = 0; i < idimension; i++) {
                            for (int j = 0; j < repeatCount; j++) {
                                shortData[band][index] = (short)(val + 0.5);
                                index++;
                            }
                            val += delta;
                        }
                    }
                    break;

                case DataBuffer.TYPE_INT:
                    int[][] intData = (int[][])dataArray;
                    index = 0;
                    while (index < size) {
                        double val = start;
                        for (int i = 0; i < idimension; i++) {
                            for (int j = 0; j < repeatCount; j++) {
                                intData[band][index] = (int)(val + 0.5);
                                index++;
                            }
                            val += delta;
                        }
                    }
                    break;

                case DataBuffer.TYPE_FLOAT:
                    float[][] floatData = (float[][])dataArray;
                    index = 0;
                    while (index < size) {
                        double val = start;
                        for (int i = 0; i < idimension; i++) {
                            for (int j = 0; j < repeatCount; j++) {
                                floatData[band][index] = (float)val;
                                index++;
                            }
                            val += delta;
                        }
                    }
                    break;

                case DataBuffer.TYPE_DOUBLE:
                    double[][] doubleData = (double[][])dataArray;
                    index = 0;
                    while (index < size) {
                        double val = start;
                        for (int i = 0; i < idimension; i++) {
                            for (int j = 0; j < repeatCount; j++) {
                                doubleData[band][index] = val;
                                index++;
                            }
                            val += delta;
                        }
                    }
                    break;
                default:
                    throw new RuntimeException(JaiI18N.getString("ColorCube5"));
            }
        }

        return dataArray;
    }

    /**
     * Constructs a two-dimensional array of byte data which represent the
     * contents of a color cube.
     *
     * @param offset The initial offset into the data array.
     * @param dimension An array of signed sizes of each side of the color
     * cube. The color ramp in each dimension will be increasing or decreasing
     * according to whether the sign of the corresponding element of the
     * <code>dimension</code> array is positive or negative, respectively.
     *
     * @return A two-dimensional byte array of color cube data.
     */
    private static byte[][] createDataArrayByte(int offset,
                                                int dimension[]) {
        return (byte[][])createDataArray(DataBuffer.TYPE_BYTE,
                                         offset, dimension);
    }

    /**
     * Constructs a two-dimensional array of short data which represent the
     * contents of a color cube.
     *
     * @param offset The initial offset into the data array.
     * @param dimension an array of signed sizes of each side of the color
     * cube. The color ramp in each dimension will be increasing or decreasing
     * according to whether the sign of the corresponding element of the
     * <code>dimension</code> array is positive or negative, respectively.
     *
     * @return A two-dimensional short array of color cube data.
     */
    private static short[][] createDataArrayShort(int offset,
                                                  int dimension[]) {
        return (short[][])createDataArray(DataBuffer.TYPE_SHORT,
                                          offset, dimension);
    }

    /**
     * Constructs a two-dimensional array of unsigned short data which
     * represent the contents of a color cube.
     *
     * @param offset The initial offset into the data array.
     * @param dimension an array of signed sizes of each side of the color
     * cube. The color ramp in each dimension will be increasing or decreasing
     * according to whether the sign of the corresponding element of the
     * <code>dimension</code> array is positive or negative, respectively.
     *
     * @return A two-dimensional short array of color cube data.
     */
    private static short[][] createDataArrayUShort(int offset,
                                                   int dimension[]) {
        return (short[][])createDataArray(DataBuffer.TYPE_USHORT,
                                          offset, dimension);
    }

    /**
     * Constructs a two-dimensional array of int data which represent the
     * contents of a color cube.
     *
     * @param offset The initial offset into the data array.
     * @param dimension an array of signed sizes of each side of the color
     * cube. The color ramp in each dimension will be increasing or decreasing
     * according to whether the sign of the corresponding element of the
     * <code>dimension</code> array is positive or negative, respectively.
     *
     * @return A two-dimensional int array of color cube data.
     */
    private static int[][] createDataArrayInt(int offset,
                                              int dimension[]) {
        return (int[][])createDataArray(DataBuffer.TYPE_INT,
                                        offset, dimension);
    }

    /**
     * Constructs a two-dimensional array of float data which represent the
     * contents of a color cube.
     *
     * @param offset The initial offset into the data array.
     * @param dimension an array of signed sizes of each side of the color
     * cube. The color ramp in each dimension will be increasing or decreasing
     * according to whether the sign of the corresponding element of the
     * <code>dimension</code> array is positive or negative, respectively.
     *
     * @return A two-dimensional float array of color cube data.
     */
    private static float[][] createDataArrayFloat(int offset,
                                                  int dimension[]) {
        return (float[][])createDataArray(DataBuffer.TYPE_FLOAT,
                                          offset, dimension);
    }

    /**
     * Constructs a two-dimensional array of double data which represent the
     * contents of a color cube.
     *
     * @param offset The initial offset into the data array.
     * @param dimension an array of signed sizes of each side of the color
     * cube. The color ramp in each dimension will be increasing or decreasing
     * according to whether the sign of the corresponding element of the
     * <code>dimension</code> array is positive or negative, respectively.
     *
     * @return A two-dimensional double array of color cube data.
     */
    private static double[][] createDataArrayDouble(int offset,
                                                    int dimension[]) {
        return (double[][])createDataArray(DataBuffer.TYPE_DOUBLE,
                                           offset, dimension);
    }

    /**
     * Returns a multi-banded byte <code>ColorCube</code> with an index
     * offset common to all bands.
     *
     * @param data The multi-banded byte data in [band][index] format.
     * @param offset The common offset for all bands.
     *
     * @throws IllegalArgumentException if data is null.
     */
    protected ColorCube(byte data[][], int offset) {
        super(data, offset);
    }

    /**
     * Returns a multi-banded short or unsigned short <code>ColorCube</code>
     * with an index offset common to all bands.
     *
     * @param data The multi-banded short data in [band][index] format.
     * @param offset The common offset for all bands.
     * @param isUShort  True if data type is DataBuffer.TYPE_USHORT;
     *                  false if data type is DataBuffer.TYPE_SHORT.
     *
     * @throws IllegalArgumentException if data is null.
     */
    protected ColorCube(short data[][], int offset, boolean isUShort) {
        super(data, offset, isUShort);
    }

    /**
     * Returns a multi-banded int <code>ColorCube</code> with an index
     * offset common to all bands.
     *
     * @param data The multi-banded int data in [band][index] format.
     * @param offset The common offset for all bands.
     *
     * @throws IllegalArgumentException if data is null.
     */
    protected ColorCube(int data[][], int offset) {
        super(data, offset);
    }

    /**
     * Returns a multi-banded float <code>ColorCube</code> with an index
     * offset common to all bands.
     *
     * @param data The multi-banded float data in [band][index] format.
     * @param offset The common offset for all bands.
     *
     * @throws IllegalArgumentException if data is null.
     */
    protected ColorCube(float data[][], int offset) {
        super(data, offset);
    }

    /**
     * Returns a multi-banded double <code>ColorCube</code> with an index
     * offset common to all bands.
     *
     * @param data The multi-banded double data in [band][index] format.
     * @param offset The common offset for all bands.
     *
     * @throws IllegalArgumentException if data is null.
     */
    protected ColorCube(double data[][], int offset) {
        super(data, offset);
    }

    /**
     * Initialize the fields of a <code>ColorCube</code>.
     *
     * @param offset The common offset for all bands.
     * @param dimension The signed dimension for each band.
     */
    private void initFields(int offset, int[] dimension) {
        // Save a reference to the dimension array.
        this.dimension = dimension;

        // Allocate memory
        multipliers = new int[dimension.length];
        dimsLessOne = new int[dimension.length];

        // Calculate multiplier magnitudes.
        multipliers[0] = 1;
        for (int i = 1; i < multipliers.length; i++) {
            multipliers[i] =
                multipliers[i-1]*Math.abs(dimension[i-1]);
        }

        // Set multiplier signs and initialize dimsLessOne.
        for (int i = 0; i < multipliers.length; i++) {
            if (dimension[i] < 0) {
                multipliers[i] = -multipliers[i];
            }
            dimsLessOne[i] = Math.abs(dimension[i]) - 1;
        }

        // Calculate adjusted offset.
        adjustedOffset = offset;
        for (int i = 0; i < dimension.length; i++) {
            if (dimension[i] > 1 && multipliers[i] < 0) {
                adjustedOffset += Math.abs(multipliers[i]) * dimsLessOne[i];
            }
        }

        // Cache the data type and number of bands to avoid repetitive calls
        // in findNearestEntry().
        dataType = getDataType();
        numBands = getNumBands();
    }

    /**
     * Returns the array of signed dimensions used to construct the
     * <code>ColorCube</code>.
     *
     */
    public int[] getDimension() {
        return dimension;
    }

    /**
     * Returns an array containing the signed dimensions, less one.
     *
     */
    public int[] getDimsLessOne() {
        return dimsLessOne;
    }

    /**
     * Returns the multipliers as an array.
     *
     */
    public int[] getMultipliers() {
        return multipliers;
    }

    /**
     * Returns the adjusted offset into the lookup table, accounting for
     * negative dimensions.
     *
     */
    public int getAdjustedOffset() {
        return adjustedOffset;
    }

    /**
     * Finds the index of the nearest color in the color map to the
     * pixel value argument.
     *
     * @param pixel a float array of all samples of a pixel.
     * @return the index of the nearest color.
     *
     * @throws RuntimeException for dataTypes other than
     *     DataBuffer.TYPE_BYTE DataBuffer.TYPE_USHORT
     *     DataBuffer.TYPE_SHORT DataBuffer.TYPE_INT
     *     DataBuffer.TYPE_FLOAT DataBuffer.TYPE_DOUBLE
     */
    public int findNearestEntry(float[] pixel) {
        int index = -1;

        index = adjustedOffset;

        switch(dataType) {
        case DataBuffer.TYPE_BYTE:
            for (int band = 0; band < numBands; band++) {
                int tmp = (int)(pixel[band] * dimsLessOne[band]);

                if ((tmp & 0xFF) > 127) {
                    tmp += (int)0x100;
                }

                index += (tmp>>8) * multipliers[band];
            }
            break;
        case DataBuffer.TYPE_SHORT:
            for (int band = 0; band < numBands; band++) {
                int tmp =
                    (int)(pixel[band] - Short.MIN_VALUE)*dimsLessOne[band];

                if ((tmp & 0xFFFF) > Short.MAX_VALUE) {
                    tmp += (int)0x10000;
                }

                index += (tmp>>16) * multipliers[band];
            }
            break;
        case DataBuffer.TYPE_USHORT:
            for (int band = 0; band < numBands; band++) {
                int tmp = (int)(pixel[band] * dimsLessOne[band]);

                if ((tmp & 0xFFFF) > Short.MAX_VALUE) {
                    tmp += (int)0x10000;
                }

                index += (tmp>>16) * multipliers[band];
            }
            break;
        case DataBuffer.TYPE_INT:
            for (int band = 0; band < numBands; band++) {
                long tmp =
                    (long)((pixel[band]-Integer.MIN_VALUE)*dimsLessOne[band]);

                if (tmp > Integer.MAX_VALUE) {
                    tmp += ((long)0xffffffff + 1L);
                }

                index += ((int)(tmp>>32)) * multipliers[band];
            }
            break;
        case DataBuffer.TYPE_FLOAT:
            for (int band = 0; band < numBands; band++) {
                float ftmp = pixel[band] * dimsLessOne[band];
                int itmp = (int)ftmp;

                if ((ftmp - itmp) >= 0.5F) {
                    itmp++;
                }

                index += itmp * multipliers[band];
            }
            break;
        case DataBuffer.TYPE_DOUBLE:
            for (int band = 0; band < numBands; band++) {
                double ftmp = pixel[band] * dimsLessOne[band];
                int itmp = (int)ftmp;

                if ((ftmp - itmp) >= 0.5) {
                    itmp++;
                }

                index += itmp * multipliers[band];
            }
            break;
        default:
            throw new RuntimeException(JaiI18N.getString("ColorCube6"));
        }

        return index;
    }
}
