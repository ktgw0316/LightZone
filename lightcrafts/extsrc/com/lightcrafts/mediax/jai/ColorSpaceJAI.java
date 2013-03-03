/*
 * $RCSfile: ColorSpaceJAI.java,v $
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

import java.awt.Point;
import java.awt.color.ColorSpace;
import java.awt.color.ICC_ColorSpace;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.awt.image.DataBuffer;
import java.awt.image.SampleModel;

/**
 * An abstract subclass of <code>ColorSpace</code> which adds methods to
 * transform colors represented as pixels in a <code>Raster</code> between
 * a specific color space and either sRGB or a well-defined C.I.E. X,Y,Z
 * color space.  As mentioned in the documentation of {@link ColorSpace},
 * sRGB is a proposed standard default RGB color space for the Internet.
 *
 * <p>This class is particularly applicable for use with color
 * spaces which are mathematically defined and for which no I.C.C. profile
 * is readily available.  (Note however that color conversions specified
 * by a simple matrix transformation might best be effected using the
 * "BandCombine" operation.)  The JAI "ColorConvert" operation recognizes when
 * an instance of <code>ColorSpaceJAI</code> is present and uses the
 * <code>Raster</code>-based conversion methods to improve performance.
 * This is possible because without the <code>ColorSpaceJAI</code> definition,
 * a <code>ColorSpace</code> which was not an {@link ICC_ColorSpace}
 * would permit color conversion only by means of pixel-by-pixel invocations
 * of <code>toCIEXYZ(float[])</code> and <code>fromCIEXYZ(float[])</code> (or,
 * equivalently <code>toRGB(float[])</code> and
 * <code>fromRGB(float[])</code>).</p>
 *
 * @see java.awt.color.ColorSpace
 * @see java.awt.color.ICC_ColorSpace
 * @see com.lightcrafts.mediax.jai.operator.ColorConvertDescriptor
 * @see com.lightcrafts.mediax.jai.operator.BandCombineDescriptor
 *
 * @since JAI 1.1
 */
public abstract class ColorSpaceJAI extends ColorSpace {
    /** Cache the maximum value for XYZ color space. */
    private static final double maxXYZ = 1 + 32767.0 / 32768.0;

    /** Cache the power value for XYZ to RGB */
    private static final double power1 = 1.0 / 2.4;

    /** The map from byte RGB to the step before matrix operation. */
    private static double[] LUT = new double[256];

    static {
        for (int i = 0; i < 256; i++) {
            double v = i / 255.0;
            if (v < 0.040449936)
                LUT[i] = v / 12.92;
            else
                LUT[i] = Math.pow((v + 0.055) / 1.055, 2.4);
        }
    }

    /**
     * Whether conversion to/from this <code>ColorSpaceJAI</code>
     * is more efficient using the sRGB methods.
     */
    private boolean isRGBPreferredIntermediary;

    /**
     * Transforms the pixel data in the source <code>Raster</code> from
     * CIEXYZ to sRGB.  It is assumed that the input XYZ values are
     * represented relative to the CIE D50 white point of the
     * <code>ColorSpace.CS_CIEXYZ</code> color space.  Integral data will
     * be normalized according to the number of bits specified for the
     * respective component; floating point data should be between 0.0 and
     * 1.0&nbsp;+&nbsp;(32767.0&nbsp;/&nbsp;32768.0).  All integral data
     * are assumed to be unsigned; signed data should be shifted by the
     * caller before invoking this method.
     *
     * <p> The exact sequence of transformations applied is as follows:</p>
     * <p><ol>
     * <li>If the source data are integral, convert the digital codes to
     * CIE XYZ values in the range <code>[0.0,&nbsp;F<sub>max</sub>]</code>
     * as:
     * <pre>
     * F<sub>d50</sub> = F<sub>max</sub> * I<sub>src</sub> / 2<sup>M<sub>src</sub></sup>
     * </pre>
     *
     * where
     *
     * <pre>
     * I<sub>src</sub> is the digital code of a source color component
     * M<sub>src</sub> is the number of significant bits per source color
     * component
     * F<sub>max</sub> is 1.0&nbsp;+&nbsp;(32767.0&nbsp;/&nbsp;32768.0)
     * F<sub>d50</sub> is the corresponding CIE XYZ value relative to CIE D50
     * </pre>
     *
     * If the source data are floating point no scaling is performed and it
     * is assumed that the data are already clipped to the range
     * <code>[0.0,&nbsp;1.0&nbsp;+&nbsp;(32767.0&nbsp;/&nbsp;32768.0)]</code>.</li>
     * <p><li>Perform chromatic adaptation from the CIE D50 white point to the
     * CIE D65 white point as described in {@link ICC_ColorSpace#fromCIEXYZ}:
     * <pre>
     * X<sub>d65</sub> = X<sub>d50</sub> * (X<sub>wd65</sub> / X<sub>wd50</sub>)
     * Y<sub>d65</sub> = Y<sub>d50</sub> * (Y<sub>wd65</sub> / Y<sub>wd50</sub>)
     * Z<sub>d65</sub> = Z<sub>d50</sub> * (Z<sub>wd65</sub> / Z<sub>wd50</sub>)
     * </pre>
     *
     * where
     *
     * <pre>
     * X<sub>d50</sub>, Y<sub>d50</sub>, Z<sub>d50</sub> are the XYZ values relative to CIE D50
     * X<sub>wd65</sub>, Y<sub>wd65</sub>, Z<sub>wd65</sub> are the CIE D65 white point values
     * X<sub>wd50</sub>, Y<sub>wd50</sub>, Z<sub>wd50</sub> are the CIE D50 white point values
     * X<sub>d65</sub>, Y<sub>d65</sub>, Z<sub>d65</sub> are the XYZ values relative to CIE D65
     * </pre>
     *
     * Substituting the actual CIE D50 and D65 white point values in the
     * above gives:
     *
     * <pre>
     * X<sub>d65</sub> = X<sub>d50</sub> * (0.3127/0.3457)
     * Y<sub>d65</sub> = Y<sub>d50</sub> * (0.3291/0.3585)
     * Z<sub>d65</sub> = Z<sub>d50</sub> * (0.3582/0.2958)
     * </pre></li></p>
     * <li>Calculate sRGB tristimulus values as:
     * <pre>
     * [ R<sub>sRGB</sub> ]   [  3.2406 -1.5372 -0.4986 ] [ X<sub>d65</sub> ]
     * [ G<sub>sRGB</sub> ] = [ -0.9689  1.8758  0.0415 ] [ Y<sub>d65</sub> ]
     * [ B<sub>sRGB</sub> ]   [  0.0557 -0.2040  1.0570 ] [ Z<sub>d65</sub> ]
     * </pre></li>
     * <p><li>
     * Clip sRGB tristimulus values to the range <code>[0.0,&nbsp;1.0]</code>.
     * </li></p>
     * <li>Transform sRGB tristimulus values to non-linear sR'G'B' values as:
     * <pre>
     * C'<sub>sRGB</sub> = 12.92*C<sub>sRGB</sub> if C<sub>sRGB</sub> <= 0.0031308
     * C'<sub>sRGB</sub> = 1.055*C<sub>sRGB</sub><sup>(1.0/2.4)</sup> - 0.055 if C<sub>sRGB</sub> > 0.0031308
     * </pre>
     *
     * where
     *
     * <pre>
     * C<sub>sRGB</sub> is a sRGB tristimulus value
     * C'<sub>sRGB</sub> is the corresponding non-linear sR'G'B' value
     * </pre></li>
     * <li>If the destination data are integral, convert the non-linear
     * sR'G'B' values to digital codes as:
     * <pre>
     * I<sub>dest</sub> = round(C'<sub>sRGB</sub> * 2<sup>M<sub>dest</sub></sup>)
     * </pre>
     *
     * where
     *
     * <pre>
     * C'<sub>sRGB</sub> is the a non-linear sR'G'B' value
     * M<sub>dest</sub> is the number of significant bits per destination color
     * component
     * I<sub>dest</sub> is the digital code of a destination color component
     * </pre>
     * If the destination data are floating point neither scaling nor rounding
     * is performed.
     * </li>
     * </ol></p>
     *
     * <p>If the destination <code>WritableRaster</code> is <code>null</code>,
     * a new <code>WritableRaster</code> will be created.  The
     * <code>Raster</code>s are treated as having no alpha channel, i.e.,
     * all bands are color bands.</p>
     *
     * <p> This method is provided for the convenience of extenders defining
     * a color space for which the conversion is defined with respect to
     * CIEXYZ.</p>
     *
     * <p><i> It should be noted that there is no official specification
     * of sRGB with respect to digital codes of depths other than 8 bits.
     * The present implementation for other bit depths is provided as an
     * extrapolation of the 8-bit sRGB standard and its use should be
     * recognized as such.  The extrapolation is implemented by replacing
     * the white digital count (<code>WDC</code>) and black digital count
     * (<code>KDC</code>) in the 8-bit sRGB specification with values
     * corresponding to the extrema of the data type in question when
     * treated as unsigned.  For all data types <code>KDC</code> is zero
     * and <code>WDC</code> is specified by the component size
     * parameters.</i></p>
     *
     * @param src the source <code>Raster</code> to be converted.
     * @param srcComponentSize array that specifies the number of significant
     *      bits per source color component; ignored for floating point data.
     *      If <code>null</code> defaults to the value returned by
     *      <code>src.getSampleModel().getSampleSize()</code>.
     * @param dest the destination <code>WritableRaster</code>,
     *        or <code>null</code>.
     * @param destComponentSize array that specifies the number of significant
     *       bits per destination color component; ignored for floating point
     *       data.  If <code>null</code>, defaults to the value returned by
     *       <code>dest.getSampleModel().getSampleSize()</code>, or the sample
     *	     size of the newly created destination WritableRaster if dest is
     *	     null.
     * @return <code>dest</code> color converted from <code>src</code>
     *         or a new, <code>WritableRaster</code> containing the converted
     *         pixels if <code>dest</code> is <code>null</code>.
     * @exception IllegalArgumentException if <code>src</code> is
     *            <code>null</code>, the number of source or destination
     *            bands is not 3, or either component size array
     *            is non-null and has length not equal to 3.
     */
    public static WritableRaster CIEXYZToRGB(Raster src,
                                             int[] srcComponentSize,
                                             WritableRaster dest,
                                             int[] destComponentSize) {

	// Validate the parameters
        checkParameters(src, srcComponentSize, dest,destComponentSize) ;

	SampleModel srcSampleModel = src.getSampleModel() ;

	/*if the parameter srcComponentSize is null, use the sample size
	 * of the source raster.
	 */
	if (srcComponentSize == null)
	    srcComponentSize = srcSampleModel.getSampleSize() ;

	// if the destination raster is null, create a new WritableRaster
	if (dest == null) {
	    Point origin = new Point(src.getMinX(), src.getMinY()) ;
	    dest = RasterFactory.createWritableRaster(srcSampleModel,
						      origin) ;
	}

	/* if the parameter dstComponentSize is null, use the sample size
	 * of the source raster.
	 */
	SampleModel dstSampleModel = dest.getSampleModel() ;
	if (destComponentSize == null)
	    destComponentSize = dstSampleModel.getSampleSize() ;

        PixelAccessor srcAcc = new PixelAccessor(srcSampleModel, null) ;
        UnpackedImageData srcUid
	    = srcAcc.getPixels(src, src.getBounds(),
                               srcSampleModel.getDataType(), false) ;

	switch (srcSampleModel.getDataType()) {

	case DataBuffer.TYPE_BYTE:
	     CIEXYZToRGBByte(srcUid, srcComponentSize,
			     dest, destComponentSize) ;
	     break ;
	case DataBuffer.TYPE_USHORT:
	case DataBuffer.TYPE_SHORT:
	     CIEXYZToRGBShort(srcUid, srcComponentSize,
			      dest, destComponentSize) ;
	     break ;
	case DataBuffer.TYPE_INT:
	     CIEXYZToRGBInt(srcUid, srcComponentSize,
			    dest, destComponentSize) ;
	     break ;
	case DataBuffer.TYPE_FLOAT:
	     CIEXYZToRGBFloat(srcUid, srcComponentSize,
			      dest, destComponentSize) ;
	     break ;
	case DataBuffer.TYPE_DOUBLE:
	     CIEXYZToRGBDouble(srcUid, srcComponentSize,
			       dest, destComponentSize) ;
	     break ;
	}

	return dest ;
    }

    /**
     * Verify that the parameters are compatible with the limitations
     * of the static methods {@link #CIEXYZToRGB} and {@link #RGBToCIEXYZ}.
     * If any of the parameters are not compatible with the requirements
     * of these methods, throw an <code>IllegalArgumentException</code>
     *
     * @throws IllegalArgumentException if <code>src</code> is
     *         <code>null</code>.
     * @throws IllegalArgumentException if <code>src.getNumBands()</code>
     *         does not return the value 3.
     * @throws IllegalArgumentException if <code>dst</code> is
     *         non-<code>null</code> and <code>dst.getNumBands()</code>
     *         does not return the value 3.
     * @throws IllegalArgumentException if <code>srcComponentSize</code> is
     *         non-<code>null</code> but its length is not 3.
     * @throws IllegalArgumentException if <code>destComponentSize</code> is
     *         non-<code>null</code> but its length is not 3.
     */
    protected static void checkParameters(Raster src,
                                          int[] srcComponentSize,
					  WritableRaster dest,
					  int[] destComponentSize) {

        if (src == null)
            throw new IllegalArgumentException(
                JaiI18N.getString("ColorSpaceJAI0")) ;
        if (src.getNumBands() != 3)
            throw new IllegalArgumentException(
                JaiI18N.getString("ColorSpaceJAI1")) ;
        if (dest != null && dest.getNumBands() != 3)
            throw new IllegalArgumentException(
                JaiI18N.getString("ColorSpaceJAI2")) ;
        if (srcComponentSize != null && srcComponentSize.length != 3)
            throw new IllegalArgumentException(
                JaiI18N.getString("ColorSpaceJAI3")) ;
        if (destComponentSize != null && destComponentSize.length != 3)
            throw new IllegalArgumentException(
                JaiI18N.getString("ColorSpaceJAI4")) ;
    }

    /**
     * After conversion, the range of a signed short is
     * <code>[0,&nbsp;Short.MAX_Value-Short.MIN_VALUE]</code> and that
     * of an integer is <code>[0,&nbsp;0xFFFFFFFFL]</code>.  To avoid
     * clamping, convert the value to a signed integer with the same
     * binary bits.  If the <code>dataType</code> parameter is either
     * <code>DataBuffer.TYPE_SHORT</code> or <code>DataBuffer.TYPE_INT</code>
     * then the array is modified in place; otherwise the method has no
     * effect.
     *
     * @param buf Array of post-conversion digital codes.
     * @param dataType The data type: one of the constants <code>TYPE_*</code>
     *        defined in {@link DataBuffer}.
     */
    static void convertToSigned(double[] buf, int dataType) {
	if (dataType == DataBuffer.TYPE_SHORT) {
	    for (int i = 0; i < buf.length; i++) {
		short temp = (short)(((int) buf[i])&0xFFFF) ;
		buf[i] = temp ;
	    }
	}
	else if (dataType == DataBuffer.TYPE_INT) {
	    for (int i = 0; i < buf.length; i++) {
	        int temp = (int)(((long)buf[i]) & 0xFFFFFFFFl) ;
		buf[i] = temp ;
	    }
	}
    }

    static void XYZ2RGB(float[] XYZ, float[] RGB) {
        RGB[0] = 2.9311227F * XYZ[0] - 1.4111496F * XYZ[1] - 0.6038046F * XYZ[2];
        RGB[1] = -0.87637005F * XYZ[0] + 1.7219844F * XYZ[1] + 0.0502565F * XYZ[2];
        RGB[2] = 0.05038065F * XYZ[0] - 0.187272F * XYZ[1] + 1.280027F * XYZ[2];

        for (int i = 0; i < 3; i++) {
            float v = RGB[i];

            if (v < 0.0F)
                v = 0.0F;

            if (v < 0.0031308F)
                RGB[i] = 12.92F * v;
            else {
                if (v > 1.0F)
                    v = 1.0F;

                RGB[i] = (float)(1.055 * Math.pow(v, power1) - 0.055);
            }
        }
    }

    private static void roundValues(double[] data) {
	for (int i = 0; i < data.length; i++)
	    data[i] = (long)(data[i] + 0.5);
    }

    // Convert a byte raster from CIEXYZ to RGB color space
    static void CIEXYZToRGBByte(UnpackedImageData src,
					int[] srcComponentSize,
                        		WritableRaster dest,
					int[] destComponentSize) {
        byte[] xBuf = src.getByteData(0) ;
        byte[] yBuf = src.getByteData(1) ;
        byte[] zBuf = src.getByteData(2) ;

	// maps the integral XYZ into floating-point
	float normx = (float)(maxXYZ / ((1L << srcComponentSize[0]) - 1));
	float normy = (float)(maxXYZ / ((1L << srcComponentSize[1]) - 1));
	float normz = (float)(maxXYZ / ((1L << srcComponentSize[2]) - 1));

	// the upper bounds for the red, green and blue bands
	double upperr = 1.0, upperg = 1.0, upperb = 1.0 ;

        int dstType = dest.getSampleModel().getDataType();

	// for the integer type, re-calculate the bounds
        if (dstType < DataBuffer.TYPE_FLOAT) {
	    upperr = (1L << destComponentSize[0]) - 1 ;
	    upperg = (1L << destComponentSize[1]) - 1 ;
	    upperb = (1L << destComponentSize[2]) - 1 ;
        }

	int height = dest.getHeight() ;
	int width = dest.getWidth() ;

	double[] dstPixels = new double[3 * height * width] ;

        int xStart = src.bandOffsets[0] ;
        int yStart = src.bandOffsets[1] ;
        int zStart = src.bandOffsets[2] ;
        int srcPixelStride = src.pixelStride;
        int srcLineStride  = src.lineStride;

        float[] XYZ = new float[3];
        float[] RGB = new float[3];

        int dIndex = 0 ;
        for (int j = 0 ; j < height; j++, xStart += srcLineStride,
                        yStart += srcLineStride, zStart += srcLineStride) {
            for (int i=0, xIndex = xStart, yIndex = yStart, zIndex = zStart ;
                i < width; i++, xIndex += srcPixelStride,
                        yIndex += srcPixelStride, zIndex += srcPixelStride) {
		XYZ[0] = (xBuf[xIndex] & 0xFF) * normx ;
		XYZ[1] = (yBuf[yIndex] & 0xFF) * normy ;
		XYZ[2] = (zBuf[zIndex] & 0xFF) * normz ;

                XYZ2RGB(XYZ, RGB);

		dstPixels[dIndex++] = upperr * RGB[0] ;
		dstPixels[dIndex++] = upperg * RGB[1] ;
		dstPixels[dIndex++] = upperb * RGB[2] ;
	    }
	}

	// Because of 4738524: setPixels should round the provided double 
	// value instead of casting
	// If it is fixed, then this piece of code can be removed.
	if (dstType < DataBuffer.TYPE_FLOAT)
	    roundValues(dstPixels);

	convertToSigned(dstPixels, dstType) ;
	dest.setPixels(dest.getMinX(), dest.getMinY(),width,height,dstPixels) ;
    }

    // convert a short type raster from CIEXYZ to RGB
    private static void CIEXYZToRGBShort(UnpackedImageData src,
			int[] srcComponentSize,
                        WritableRaster dest, int[] destComponentSize) {
        short[] xBuf = src.getShortData(0) ;
        short[] yBuf = src.getShortData(1) ;
        short[] zBuf = src.getShortData(2) ;

	// maps the integral XYZ into floating-point
	float normx = (float)(maxXYZ / ((1L << srcComponentSize[0]) - 1));
	float normy = (float)(maxXYZ / ((1L << srcComponentSize[1]) - 1));
	float normz = (float)(maxXYZ / ((1L << srcComponentSize[2]) - 1));

	// the upper bounds for the red, green and blue bands
	double upperr = 1.0, upperg = 1.0, upperb = 1.0 ;

        int dstType = dest.getSampleModel().getDataType();

	// for the integer type, re-calculate the norm and bands
        if (dstType < DataBuffer.TYPE_FLOAT) {
	    upperr = (1L << destComponentSize[0]) - 1 ;
	    upperg = (1L << destComponentSize[1]) - 1 ;
	    upperb = (1L << destComponentSize[2]) - 1 ;
        }

        int height = dest.getHeight() ;
        int width = dest.getWidth() ;

        double[] dstPixels = new double[3 * height * width] ;

        int xStart = src.bandOffsets[0] ;
        int yStart = src.bandOffsets[1] ;
        int zStart = src.bandOffsets[2] ;
        int srcPixelStride = src.pixelStride;
        int srcLineStride  = src.lineStride;

        float[] XYZ = new float[3];
        float[] RGB = new float[3];

        int dIndex = 0 ;
        for (int j = 0 ; j < height; j++, xStart += srcLineStride,
                        yStart += srcLineStride, zStart += srcLineStride) {
            for (int i = 0, xIndex = xStart, yIndex = yStart, zIndex = zStart ;
                i < width; i++, xIndex += srcPixelStride,
                        yIndex += srcPixelStride, zIndex += srcPixelStride) {
                XYZ[0] = (xBuf[xIndex] & 0xFFFF) * normx ;
                XYZ[1] = (yBuf[yIndex] & 0xFFFF) * normy ;
                XYZ[2] = (zBuf[zIndex] & 0xFFFF) * normz ;

                XYZ2RGB(XYZ, RGB);

		dstPixels[dIndex++] = upperr * RGB[0] ;
		dstPixels[dIndex++] = upperg * RGB[1] ;
		dstPixels[dIndex++] = upperb * RGB[2] ;
            }
        }

        // Because of 4738524: setPixels should round the provided double 
        // value instead of casting
        // If it is fixed, then this piece of code can be removed.
        if (dstType < DataBuffer.TYPE_FLOAT)
            roundValues(dstPixels);

        convertToSigned(dstPixels, dstType) ;
        dest.setPixels(dest.getMinX(), dest.getMinY(),width,height,dstPixels) ;
    }

    // convert an int type raster from CIEXYZ to RGB
    private static void CIEXYZToRGBInt(UnpackedImageData src,
			int[] srcComponentSize,
                        WritableRaster dest, int[] destComponentSize) {
        int[] xBuf = src.getIntData(0) ;
        int[] yBuf = src.getIntData(1) ;
        int[] zBuf = src.getIntData(2) ;

	// maps the integral XYZ into floating-point
	float normx = (float)(maxXYZ / ((1L << srcComponentSize[0]) - 1));
	float normy = (float)(maxXYZ / ((1L << srcComponentSize[1]) - 1));
	float normz = (float)(maxXYZ / ((1L << srcComponentSize[2]) - 1));

        // the upper bound for each band
        double upperr = 1.0, upperg = 1.0, upperb = 1.0 ;

        int dstType = dest.getSampleModel().getDataType();

	// for the integer type, re-calculate the bounds
        if (dstType < DataBuffer.TYPE_FLOAT) {
            upperr = (1L << destComponentSize[0]) - 1 ;
            upperg = (1L << destComponentSize[1]) - 1 ;
            upperb = (1L << destComponentSize[2]) - 1 ;
        }

        int height = dest.getHeight() ;
        int width = dest.getWidth() ;

        double[] dstPixels = new double[3 * height * width] ;

        int xStart = src.bandOffsets[0] ;
        int yStart = src.bandOffsets[1] ;
        int zStart = src.bandOffsets[2] ;
        int srcPixelStride = src.pixelStride;
        int srcLineStride  = src.lineStride;

        float[] XYZ = new float[3];
        float[] RGB = new float[3];

        int dIndex = 0 ;
        for (int j = 0 ; j < height; j++, xStart += srcLineStride,
                        yStart += srcLineStride, zStart += srcLineStride) {
            for (int i = 0, xIndex = xStart, yIndex = yStart, zIndex = zStart ;
                i < width; i++, xIndex += srcPixelStride,
                        yIndex += srcPixelStride, zIndex += srcPixelStride) {
                XYZ[0] = (xBuf[xIndex] & 0xFFFFFFFFl) * normx ;
                XYZ[1] = (yBuf[yIndex] & 0xFFFFFFFFl) * normy ;
                XYZ[2] = (zBuf[zIndex] & 0xFFFFFFFFl) * normz ;

                XYZ2RGB(XYZ, RGB);

		dstPixels[dIndex++] = upperr * RGB[0] ;
		dstPixels[dIndex++] = upperg * RGB[1] ;
		dstPixels[dIndex++] = upperb * RGB[2] ;
            }
        }

        // Because of 4738524: setPixels should round the provided double 
        // value instead of casting
        // If it is fixed, then this piece of code can be removed.
        if (dstType < DataBuffer.TYPE_FLOAT)
            roundValues(dstPixels);

        convertToSigned(dstPixels, dstType) ;
        dest.setPixels(dest.getMinX(), dest.getMinY(),width,height,dstPixels) ;
    }

    // convert a float type ratser from CIEXYZ to RGB
    private static void CIEXYZToRGBFloat(UnpackedImageData src,
					 int[] srcComponentSize,
                        		 WritableRaster dest,
					 int[] destComponentSize) {
        float[] xBuf = src.getFloatData(0) ;
        float[] yBuf = src.getFloatData(1) ;
        float[] zBuf = src.getFloatData(2) ;

	// the upper bounds for the 3 bands
        double upperr = 1.0, upperg = 1.0, upperb = 1.0 ;

        int dstType = dest.getSampleModel().getDataType();

	// for the integer type, re-calculate the bounds
        if (dstType < DataBuffer.TYPE_FLOAT) {
            upperr = (1L << destComponentSize[0]) - 1 ;
            upperg = (1L << destComponentSize[1]) - 1 ;
            upperb = (1L << destComponentSize[2]) - 1 ;
        }

        int height = dest.getHeight() ;
        int width = dest.getWidth() ;

        double[] dstPixels = new double[3 * height * width] ;

        int xStart = src.bandOffsets[0] ;
        int yStart = src.bandOffsets[1] ;
        int zStart = src.bandOffsets[2] ;
        int srcPixelStride = src.pixelStride;
        int srcLineStride  = src.lineStride;

        float[] XYZ = new float[3];
        float[] RGB = new float[3];

        int dIndex = 0 ;
        for (int j = 0 ; j < height; j++, xStart += srcLineStride,
                        yStart += srcLineStride, zStart += srcLineStride) {
            for (int i = 0, xIndex = xStart, yIndex = yStart, zIndex = zStart ;
                i < width; i++, xIndex += srcPixelStride,
                        yIndex += srcPixelStride, zIndex += srcPixelStride) {
                XYZ[0] = xBuf[xIndex];
                XYZ[1] = yBuf[yIndex];
                XYZ[2] = zBuf[zIndex];

                XYZ2RGB(XYZ, RGB);

		dstPixels[dIndex++] = upperr * RGB[0] ;
		dstPixels[dIndex++] = upperg * RGB[1] ;
		dstPixels[dIndex++] = upperb * RGB[2] ;
            }
        }

        // Because of 4738524: setPixels should round the provided double 
        // value instead of casting
        // If it is fixed, then this piece of code can be removed.
        if (dstType < DataBuffer.TYPE_FLOAT)
            roundValues(dstPixels);

        convertToSigned(dstPixels, dstType) ;
        dest.setPixels(dest.getMinX(), dest.getMinY(),width,height,dstPixels) ;
    }

    // convert a double type ratser form CIEXYZ to RGB color space
    private static void CIEXYZToRGBDouble(UnpackedImageData src,
					  int[] srcComponentSize,
                        		  WritableRaster dest,
					  int[] destComponentSize) {
        double[] xBuf = src.getDoubleData(0) ;
        double[] yBuf = src.getDoubleData(1) ;
        double[] zBuf = src.getDoubleData(2) ;

	// the upper bound of each band
        double upperr = 1.0, upperg = 1.0, upperb = 1.0 ;

        int dstType = dest.getSampleModel().getDataType();

	// for the integer type, re-calculate the bounds
        if (dstType < DataBuffer.TYPE_FLOAT) {
            upperr = (1L << destComponentSize[0]) - 1 ;
            upperg = (1L << destComponentSize[1]) - 1 ;
            upperb = (1L << destComponentSize[2]) - 1 ;
        }

        int height = dest.getHeight() ;
        int width = dest.getWidth() ;

        double[] dstPixels = new double[3 * height * width] ;

        int xStart = src.bandOffsets[0] ;
        int yStart = src.bandOffsets[1] ;
        int zStart = src.bandOffsets[2] ;
        int srcPixelStride = src.pixelStride;
        int srcLineStride  = src.lineStride;

        float[] XYZ = new float[3];
        float[] RGB = new float[3];

        int dIndex = 0 ;
        for (int j = 0 ; j < height; j++, xStart += srcLineStride,
                        yStart += srcLineStride, zStart += srcLineStride) {
            for (int i = 0, xIndex = xStart, yIndex = yStart, zIndex = zStart ;
                i < width; i++, xIndex += srcPixelStride,
                        yIndex += srcPixelStride, zIndex += srcPixelStride) {
                XYZ[0] = (float)xBuf[xIndex];
                XYZ[1] = (float)yBuf[yIndex];
                XYZ[2] = (float)zBuf[zIndex];

                XYZ2RGB(XYZ, RGB);

		dstPixels[dIndex++] = upperr * RGB[0] ;
		dstPixels[dIndex++] = upperg * RGB[1] ;
		dstPixels[dIndex++] = upperb * RGB[2] ;
            }
        }

        // Because of 4738524: setPixels should round the provided double 
        // value instead of casting
        // If it is fixed, then this piece of code can be removed.
        if (dstType < DataBuffer.TYPE_FLOAT)
            roundValues(dstPixels);

        convertToSigned(dstPixels, dstType) ;
        dest.setPixels(dest.getMinX(), dest.getMinY(),width,height,dstPixels) ;
    }

    /**
     * Transforms the pixel data in the source <code>Raster</code> from
     * sRGB to CIEXYZ.  The output XYZ values are represented relative to
     * the CIE D50 white point of the <code>ColorSpace.CS_CIEXYZ</code>
     * color space.  Integral data will be normalized according to the
     * number of bits specified for the respective component; floating
     * point data should be between 0.0 and 1.0.  All integral data are
     * assumed to be unsigned; signed data should be shifted by the caller
     * before invoking this method.
     *
     * <p> The exact sequence of transformations applied is as follows:</p>
     * <p><ol>
     * <li>If the source data are integral, convert the digital codes to
     * non-linear sRGB values in the range <code>[0.0,&nbsp;1.0]</code> as:
     * <pre>
     * C'<sub>sRGB</sub> = I<sub>src</sub> / 2<sup>M<sub>src</sub></sup>
     * </pre>
     *
     * where
     *
     * <pre>
     * I<sub>src</sub> is the digital code of a source color component
     * M<sub>src</sub> is the number of significant bits per source color
     * component
     * C'<sub>sRGB</sub> is the corresponding sR'G'B' value
     * </pre>
     *
     * If the source data are floating point no scaling is performed and it
     * is assumed that the data are already clipped to the range
     * <code>[0.0,&nbsp;1.0]</code>.</li>
     * <p><li>Transform non-linear sR'G'B' values to sRGB tristimulus values
     * as:
     * <pre>
     * C<sub>sRGB</sub> = C'<sub>sRGB</sub>/12.92 if C'<sub>sRGB</sub> <= 0.04045
     * C<sub>sRGB</sub> = [(C'<sub>sRGB</sub> + 0.055)/1.055]<sup>2.4</sup> if C'<sub>sRGB</sub> > 0.04045
     * </pre>
     *
     * where
     *
     * <pre>
     * C'<sub>sRGB</sub> is a non-linear sR'G'B' value
     * C<sub>sRGB</sub> is the corresponding sRGB tristimulus value
     * </pre></li></p>
     * <li>Calculate CIE XYZ D65-relative values as:
     * <pre>
     * [ X<sub>d65</sub> ]   [ 0.4124 0.3576 0.1805 ] [ R<sub>sRGB</sub> ]
     * [ Y<sub>d65</sub> ] = [ 0.2126 0.7152 0.0722 ] [ G<sub>sRGB</sub> ]
     * [ Z<sub>d65</sub> ]   [ 0.0193 0.1192 0.9505 ] [ B<sub>sRGB</sub> ]
     * </pre></li>
     * <li>Perform chromatic adaptation from the CIE D65 white point to the
     * CIE D50 white point as described in {@link ICC_ColorSpace#toCIEXYZ}:
     * <pre>
     * X<sub>d50</sub> = X<sub>d65</sub> * (X<sub>wd50</sub> / X<sub>wd65</sub>)
     * Y<sub>d50</sub> = Y<sub>d65</sub> * (Y<sub>wd50</sub> / Y<sub>wd65</sub>)
     * Z<sub>d50</sub> = Z<sub>d65</sub> * (Z<sub>wd50</sub> / Z<sub>wd65</sub>)
     * </pre>
     *
     * where
     *
     * <pre>
     * X<sub>d65</sub>, Y<sub>d65</sub>, Z<sub>d65</sub> are the XYZ values relative to CIE D65
     * X<sub>wd50</sub>, Y<sub>wd50</sub>, Z<sub>wd50</sub> are the CIE D50 white point values
     * X<sub>wd65</sub>, Y<sub>wd65</sub>, Z<sub>wd65</sub> are the CIE D65 white point values
     * X<sub>d50</sub>, Y<sub>d50</sub>, Z<sub>d50</sub> are the XYZ values relative to CIE D50
     * </pre>
     *
     * Substituting the actual CIE D50 and D65 white point values in the
     * above gives:
     *
     * <pre>
     * X<sub>d50</sub> = X<sub>d65</sub> * (0.3457/0.3127)
     * Y<sub>d50</sub> = Y<sub>d65</sub> * (0.3585/0.3291)
     * Z<sub>d50</sub> = Z<sub>d65</sub> * (0.2958/0.3582)
     * </pre></li>
     * <li>If the destination data are integral, convert the CIE XYZ
     * values to digital codes as:
     * <pre>
     * I<sub>dest</sub> = round(F<sub>d50</sub> * 2<sup>M<sub>dest</sub></sup> / F<sub>max</sub>)
     * </pre>
     *
     * where
     *
     * <pre>
     * F<sub>d50</sub> is a CIE XYZ value relative to CIE D50
     * M<sub>dest</sub> is the number of significant bits per destination color
     * component
     * F<sub>max</sub> is 1.0&nbsp;+&nbsp;(32767.0&nbsp;/&nbsp;32768.0)
     * I<sub>dest</sub> is the digital code of a destination color component
     * </pre>
     * If the destination data are floating point neither scaling nor rounding
     * is performed.
     * </li>
     * </ol></p>
     *
     * <p> If the destination <code>WritableRaster</code> is <code>null</code>,
     * a new <code>WritableRaster</code> will be created.  The
     * <code>Raster</code>s are treated as having no alpha channel, i.e.,
     * all bands are color bands.
     *
     * <p> This method is provided for the convenience of extenders defining
     * a color space for which the conversion is defined with respect to
     * sRGB.
     *
     * <p><i> It should be noted that there is no official specification
     * of sRGB with respect to digital codes of depths other than 8 bits.
     * The present implementation for other bit depths is provided as an
     * extrapolation of the 8-bit sRGB standard and its use should be
     * recognized as such.  The extrapolation is implemented by replacing
     * the white digital count (<code>WDC</code>) and black digital count
     * (<code>KDC</code>) in the 8-bit sRGB specification with values
     * corresponding to the extrema of the data type in question when
     * treated as unsigned.  For all data types <code>KDC</code> is zero
     * and <code>WDC</code> is specified by the component size
     * parameters.</i></p>
     *
     * @param src the source <code>Raster</code> to be converted.
     * @param srcComponentSize array that specifies the number of significant
     *      bits per source color component; ignored for floating point data.
     *      If <code>null</code> defaults to the value returned by
     *      <code>src.getSampleModel().getSampleSize()</code>.
     * @param dest the destination <code>WritableRaster</code>,
     *        or <code>null</code>.
     * @param destComponentSize array that specifies the number of significant
     *       bits per destination color component; ignored for floating point
     *       data.  If <code>null</code>, defaults to the value returned by
     *       <code>dest.getSampleModel().getSampleSize()</code>, or the sample
     *       size of the newly created destination WritableRaster if dest is
     *	     null.
     * @return <code>dest</code> color converted from <code>src</code>
     *         or a new, <code>WritableRaster</code> containing the converted
     *         pixels if <code>dest</code> is <code>null</code>.
     * @exception IllegalArgumentException if <code>src</code> is
     *            <code>null</code>, the number of source or destination
     *            bands is not 3, or either component size array
     *            is non-null and has length not equal to 3.
     */
    public static WritableRaster RGBToCIEXYZ(Raster src,
                                             int[] srcComponentSize,
                                             WritableRaster dest,
                                             int[] destComponentSize) {

        checkParameters(src, srcComponentSize, dest,destComponentSize) ;

        SampleModel srcSampleModel = src.getSampleModel() ;

	// if the srcComponentSize is not provided, use the sample sizes
	// from the source's sample model
        if (srcComponentSize == null)
            srcComponentSize = srcSampleModel.getSampleSize() ;

	// if the destination raster is not provided, create a new one
        if (dest == null) {
            Point origin = new Point(src.getMinX(), src.getMinY()) ;
            dest = RasterFactory.createWritableRaster(srcSampleModel,
                                        	      origin) ;
        }

        SampleModel dstSampleModel = dest.getSampleModel() ;

	//if the destComponentSize is not provided, use the sample sizes
	//from the destination's sample model
        if (destComponentSize == null)
            destComponentSize = dstSampleModel.getSampleSize() ;

        PixelAccessor srcAcc = new PixelAccessor(srcSampleModel, null) ;
        UnpackedImageData srcUid = srcAcc.getPixels(src, src.getBounds(),
                                    srcSampleModel.getDataType(),
                                    false) ;

        switch (srcSampleModel.getDataType()) {

        case DataBuffer.TYPE_BYTE:
             RGBToCIEXYZByte(srcUid, srcComponentSize, dest, destComponentSize) ;
             break ;
        case DataBuffer.TYPE_USHORT:
        case DataBuffer.TYPE_SHORT:
             RGBToCIEXYZShort(srcUid, srcComponentSize, dest, destComponentSize) ;
             break ;
        case DataBuffer.TYPE_INT:
             RGBToCIEXYZInt(srcUid, srcComponentSize, dest, destComponentSize) ;
             break ;
        case DataBuffer.TYPE_FLOAT:
             RGBToCIEXYZFloat(srcUid, srcComponentSize, dest, destComponentSize) ;
             break ;
        case DataBuffer.TYPE_DOUBLE:
             RGBToCIEXYZDouble(srcUid, srcComponentSize, dest, destComponentSize) ;
             break ;
        }

	return dest ;
    }

    static void RGB2XYZ(float[] RGB, float[] XYZ) {
        for (int i = 0; i < 3; i++) {
            if (RGB[i] < 0.040449936F)
              RGB[i] /= 12.92F;
            else
              RGB[i] = (float)(Math.pow((RGB[i] + 0.055) / 1.055, 2.4));
        }

        XYZ[0] =
            0.45593763F * RGB[0] + 0.39533819F * RGB[1] + 0.19954964F * RGB[2];
        XYZ[1] =
            0.23157515F * RGB[0] + 0.77905262F * RGB[1] + 0.07864978F * RGB[2];
        XYZ[2] =
            0.01593493F * RGB[0] + 0.09841772F * RGB[1] + 0.78488615F * RGB[2];
    }

    // convert a byte ratser from RGB to CIEXYZ
    private static void RGBToCIEXYZByte(UnpackedImageData src,
					int[] srcComponentSize,
                        		WritableRaster dest,
					int[] destComponentSize) {
        byte[] rBuf = src.getByteData(0) ;
        byte[] gBuf = src.getByteData(1) ;
        byte[] bBuf = src.getByteData(2) ;

	// used to left-shift the value to fill in all the 8-bits
        int normr = 8 - srcComponentSize[0] ;
        int normg = 8 - srcComponentSize[1] ;
        int normb = 8 - srcComponentSize[2] ;

	// the norms used to map the color value to the desired range
        double normx = 1.0, normy = normx, normz = normx ;

        int dstType = dest.getSampleModel().getDataType() ;
        boolean isInt = (dstType < DataBuffer.TYPE_FLOAT) ;

	// for the integer type, redefine the norms and upper bounds
	// because rgb={1.0, 1.0, 1.0} is xyz={0.950456, 1.0, 1.088754},
	// so for normx, normz, they are specially treated
        if (isInt) {
            normx = ((1L << destComponentSize[0]) - 1) / maxXYZ ;
            normy = ((1L << destComponentSize[1]) - 1) / maxXYZ;
            normz = ((1L << destComponentSize[2]) - 1) / maxXYZ ;
        }

        int height = dest.getHeight() ;
        int width = dest.getWidth() ;

        double[] dstPixels = new double[3 * height * width] ;

        int rStart = src.bandOffsets[0] ;
        int gStart = src.bandOffsets[1] ;
        int bStart = src.bandOffsets[2] ;
        int srcPixelStride = src.pixelStride;
        int srcLineStride  = src.lineStride;

        int dIndex = 0 ;
        for (int j = 0 ; j < height; j++, rStart += srcLineStride,
                        gStart += srcLineStride, bStart += srcLineStride) {
            for (int i = 0, rIndex = rStart, gIndex = gStart, bIndex = bStart ;
                i < width; i++, rIndex += srcPixelStride,
                        gIndex += srcPixelStride, bIndex += srcPixelStride) {
                double R = LUT[(rBuf[rIndex] & 0xFF) << normr] ;
                double G = LUT[(gBuf[gIndex] & 0xFF) << normg] ;
                double B = LUT[(bBuf[bIndex] & 0xFF) << normb] ;

		if (isInt) {
		    double X, Y, Z ;
                    dstPixels[dIndex++] =
                    (0.45593763 * R + 0.39533819 * G + 0.19954964 * B) * normx;
                    dstPixels[dIndex++] =
                    (0.23157515 * R + 0.77905262 * G + 0.07864978 * B) * normy;
                    dstPixels[dIndex++] =
                    (0.01593493 * R + 0.09841772 * G + 0.78488615 * B) * normz;
		} else {
                    dstPixels[dIndex++]
                        = 0.45593763 * R + 0.39533819 * G + 0.19954964 * B ;
                    dstPixels[dIndex++]
                        = 0.23157515 * R + 0.77905262 * G + 0.07864978 * B ;
                    dstPixels[dIndex++]
                        = 0.01593493 * R + 0.09841772 * G + 0.78488615 * B ;
		}
            }
        }

        // Because of 4738524: setPixels should round the provided double 
        // value instead of casting
        // If it is fixed, then this piece of code can be removed.
        if (dstType < DataBuffer.TYPE_FLOAT)
            roundValues(dstPixels);

        convertToSigned(dstPixels, dstType) ;
        dest.setPixels(dest.getMinX(), dest.getMinY(),width,height,dstPixels) ;
    }

    // convert a short ratser from RGB to CIEXYZ
    private static void RGBToCIEXYZShort(UnpackedImageData src,
					 int[] srcComponentSize,
                        		 WritableRaster dest,
					 int[] destComponentSize) {
        short[] rBuf = src.getShortData(0) ;
        short[] gBuf = src.getShortData(1) ;
        short[] bBuf = src.getShortData(2) ;

	// used to left-shift the input value to fill all the bits
        float normr = (1 << srcComponentSize[0]) - 1 ;
        float normg = (1 << srcComponentSize[1]) - 1 ;
        float normb = (1 << srcComponentSize[2]) - 1 ;

	// used to map the output to the desired range
        double normx = 1.0, normy = 1.0, normz = 1.0 ;

        int dstType = dest.getSampleModel().getDataType() ;
        boolean isInt = (dstType < DataBuffer.TYPE_FLOAT) ;

	// define the norms and upper bounds for the integer types
	// see the comments in RGBToCIEXYZByte
        if (isInt) {
            normx = ((1L << destComponentSize[0]) - 1) / maxXYZ ;
            normy = ((1L << destComponentSize[1]) - 1) / maxXYZ;
            normz = ((1L << destComponentSize[2]) - 1) / maxXYZ ;
        }

        int height = dest.getHeight() ;
        int width = dest.getWidth() ;

        double[] dstPixels = new double[3 * height * width] ;

        int rStart = src.bandOffsets[0] ;
        int gStart = src.bandOffsets[1] ;
        int bStart = src.bandOffsets[2] ;
        int srcPixelStride = src.pixelStride;
        int srcLineStride  = src.lineStride;

        float[] XYZ = new float[3];
        float[] RGB = new float[3];

        int dIndex = 0 ;
        for (int j = 0 ; j < height; j++, rStart += srcLineStride,
                        gStart += srcLineStride, bStart += srcLineStride) {
            for (int i = 0, rIndex = rStart, gIndex = gStart, bIndex = bStart ;
                i < width; i++, rIndex += srcPixelStride,
                        gIndex += srcPixelStride, bIndex += srcPixelStride) {
                RGB[0] = (rBuf[rIndex] & 0xFFFF) / normr ;
                RGB[1] = (gBuf[gIndex] & 0xFFFF) / normg ;
                RGB[2] = (bBuf[bIndex] & 0xFFFF) / normb ;

                RGB2XYZ(RGB, XYZ);

                if (isInt) {
                    dstPixels[dIndex++] = XYZ[0] * normx ;
                    dstPixels[dIndex++] = XYZ[1] * normy ;
                    dstPixels[dIndex++] = XYZ[2] * normz ;
                } else {
                    dstPixels[dIndex++] = XYZ[0] ;
                    dstPixels[dIndex++] = XYZ[1] ;
                    dstPixels[dIndex++] = XYZ[2] ;
                }
            }
        }

        // Because of 4738524: setPixels should round the provided double 
        // value instead of casting
        // If it is fixed, then this piece of code can be removed.
        if (dstType < DataBuffer.TYPE_FLOAT)
            roundValues(dstPixels);

        convertToSigned(dstPixels, dstType) ;
        dest.setPixels(dest.getMinX(), dest.getMinY(),width,height,dstPixels) ;
    }

    // convert a int type ratser from RGB to CIEXYZ
    private static void RGBToCIEXYZInt(UnpackedImageData src,
				       int[] srcComponentSize,
                        	       WritableRaster dest,
				       int[] destComponentSize) {
        int[] rBuf = src.getIntData(0) ;
        int[] gBuf = src.getIntData(1) ;
        int[] bBuf = src.getIntData(2) ;

	// used to left-shift the input to fill all the bits
        float normr = (1L << srcComponentSize[0]) - 1 ;
        float normg = (1L << srcComponentSize[1]) - 1 ;
        float normb = (1L << srcComponentSize[2]) - 1 ;

	// norms to map the output to the desired range
        double normx = 1.0, normy = 1.0, normz = 1.0 ;

        int dstType = dest.getSampleModel().getDataType() ;
        boolean isInt = (dstType < DataBuffer.TYPE_FLOAT) ;

	// define the norm and upper bounds for the integer output types
	// see also the comments in RGBToCIEXYZByte
        if (isInt) {
            normx = ((1L << destComponentSize[0]) - 1) / maxXYZ ;
            normy = ((1L << destComponentSize[1]) - 1) / maxXYZ;
            normz = ((1L << destComponentSize[2]) - 1) / maxXYZ ;
        }

        int height = dest.getHeight() ;
        int width = dest.getWidth() ;

        double[] dstPixels = new double[3 * height * width] ;

        int rStart = src.bandOffsets[0] ;
        int gStart = src.bandOffsets[1] ;
        int bStart = src.bandOffsets[2] ;
        int srcPixelStride = src.pixelStride;
        int srcLineStride  = src.lineStride;

        float[] XYZ = new float[3];
        float[] RGB = new float[3];

        int dIndex = 0 ;
        for (int j = 0 ; j < height; j++, rStart += srcLineStride,
                        gStart += srcLineStride, bStart += srcLineStride) {
            for (int i = 0, rIndex = rStart, gIndex = gStart, bIndex = bStart ;
                i < width; i++, rIndex += srcPixelStride,
                        gIndex += srcPixelStride, bIndex += srcPixelStride) {
                RGB[0] = (rBuf[rIndex] & 0xFFFFFFFFl) / normr ;
                RGB[1] = (gBuf[gIndex] & 0xFFFFFFFFl) / normg ;
                RGB[2] = (bBuf[bIndex] & 0xFFFFFFFFl) / normb ;

                RGB2XYZ(RGB, XYZ);

                if (isInt) {
                    dstPixels[dIndex++] = XYZ[0] * normx;
                    dstPixels[dIndex++] = XYZ[1] * normx;
                    dstPixels[dIndex++] = XYZ[2] * normx;
                } else {
                    dstPixels[dIndex++] = XYZ[0] ;
                    dstPixels[dIndex++] = XYZ[1] ;
                    dstPixels[dIndex++] = XYZ[2] ;
                }
            }
        }

        // Because of 4738524: setPixels should round the provided double 
        // value instead of casting
        // If it is fixed, then this piece of code can be removed.
        if (dstType < DataBuffer.TYPE_FLOAT)
            roundValues(dstPixels);

        convertToSigned(dstPixels, dstType) ;
        dest.setPixels(dest.getMinX(), dest.getMinY(),width,height,dstPixels) ;
    }

    // convert a float type ratser from RGB to CIEXYZ color space
    private static void RGBToCIEXYZFloat(UnpackedImageData src,
					 int[] srcComponentSize,
                        		 WritableRaster dest,
					 int[] destComponentSize) {
        float[] rBuf = src.getFloatData(0) ;
        float[] gBuf = src.getFloatData(1) ;
        float[] bBuf = src.getFloatData(2) ;

	// norms to map the output value to the desired range
        double normx = 1.0, normy = 1.0, normz = 1.0 ;

        int dstType = dest.getSampleModel().getDataType() ;
        boolean isInt = (dstType < DataBuffer.TYPE_FLOAT) ;

	// define the norms and upper bounds for the integer types
        if (isInt) {
            normx = ((1L << destComponentSize[0]) - 1) / maxXYZ ;
            normy = ((1L << destComponentSize[1]) - 1) / maxXYZ;
            normz = ((1L << destComponentSize[2]) - 1) / maxXYZ ;
        }

        int height = dest.getHeight() ;
        int width = dest.getWidth() ;

        double[] dstPixels = new double[3 * height * width] ;

        int rStart = src.bandOffsets[0] ;
        int gStart = src.bandOffsets[1] ;
        int bStart = src.bandOffsets[2] ;
        int srcPixelStride = src.pixelStride;
        int srcLineStride  = src.lineStride;

        float[] XYZ = new float[3];
        float[] RGB = new float[3];

        int dIndex = 0 ;
        for (int j = 0 ; j < height; j++, rStart += srcLineStride,
                        gStart += srcLineStride, bStart += srcLineStride) {
            for (int i = 0, rIndex = rStart, gIndex = gStart, bIndex = bStart ;
                i < width; i++, rIndex += srcPixelStride,
                        gIndex += srcPixelStride, bIndex += srcPixelStride) {
                RGB[0] = rBuf[rIndex] ;
                RGB[1] = gBuf[gIndex] ;
                RGB[2] = bBuf[bIndex] ;

                RGB2XYZ(RGB, XYZ);

                if (isInt) {
                    dstPixels[dIndex++] = XYZ[0] * normx;
                    dstPixels[dIndex++] = XYZ[1] * normx;
                    dstPixels[dIndex++] = XYZ[2] * normx;
                } else {
                    dstPixels[dIndex++] = XYZ[0] ;
                    dstPixels[dIndex++] = XYZ[1] ;
                    dstPixels[dIndex++] = XYZ[2] ;
                }
            }
        }

        // Because of 4738524: setPixels should round the provided double 
        // value instead of casting
        // If it is fixed, then this piece of code can be removed.
        if (dstType < DataBuffer.TYPE_FLOAT)
            roundValues(dstPixels);

        convertToSigned(dstPixels, dstType) ;
        dest.setPixels(dest.getMinX(), dest.getMinY(),width,height,dstPixels) ;
    }

    // convert a double type raster from RGB to CIEXYZ
    private static void RGBToCIEXYZDouble(UnpackedImageData src,
					  int[] srcComponentSize,
                        		  WritableRaster dest,
					  int[] destComponentSize) {
        double[] rBuf = src.getDoubleData(0) ;
        double[] gBuf = src.getDoubleData(1) ;
        double[] bBuf = src.getDoubleData(2) ;

	// norms to map the output to the desired range
        double normx = 1.0, normy = 1.0, normz = 1.0 ;

	int dstType = dest.getSampleModel().getDataType() ;
        boolean isInt = (dstType < DataBuffer.TYPE_FLOAT) ;

        if (isInt) {
            normx = ((1L << destComponentSize[0]) - 1) / maxXYZ ;
            normy = ((1L << destComponentSize[1]) - 1) / maxXYZ;
            normz = ((1L << destComponentSize[2]) - 1) / maxXYZ ;
        }

        int height = dest.getHeight() ;
        int width = dest.getWidth() ;

        double[] dstPixels = new double[3 * height * width] ;

        int rStart = src.bandOffsets[0] ;
        int gStart = src.bandOffsets[1] ;
        int bStart = src.bandOffsets[2] ;
        int srcPixelStride = src.pixelStride;
        int srcLineStride  = src.lineStride;

        float[] XYZ = new float[3];
        float[] RGB = new float[3];

        int dIndex = 0 ;
        for (int j = 0 ; j<height; j++, rStart += srcLineStride,
                        gStart += srcLineStride, bStart += srcLineStride) {
            for (int i = 0, rIndex = rStart, gIndex = gStart, bIndex = bStart ;
                i < width; i++, rIndex += srcPixelStride,
                        gIndex += srcPixelStride, bIndex += srcPixelStride) {
                RGB[0] = (float)rBuf[rIndex] ;
                RGB[1] = (float)gBuf[gIndex] ;
                RGB[2] = (float)bBuf[bIndex] ;

                RGB2XYZ(RGB, XYZ);

                if (isInt) {
                    dstPixels[dIndex++] = XYZ[0] * normx;
                    dstPixels[dIndex++] = XYZ[1] * normx;
                    dstPixels[dIndex++] = XYZ[2] * normx;
                } else {
                    dstPixels[dIndex++] = XYZ[0] ;
                    dstPixels[dIndex++] = XYZ[1] ;
                    dstPixels[dIndex++] = XYZ[2] ;
                }
            }
        }

        // Because of 4738524: setPixels should round the provided double 
        // value instead of casting
        // If it is fixed, then this piece of code can be removed.
        if (dstType < DataBuffer.TYPE_FLOAT)
            roundValues(dstPixels);

        convertToSigned(dstPixels, dstType) ;
        dest.setPixels(dest.getMinX(), dest.getMinY(),width,height,dstPixels) ;
    }

    /**
     * Constructs a <code>ColorSpaceJAI</code> object given the color space
     * type, the number of components, and an indicator of the preferred
     * intermediary or connection color space.
     *
     * @param type The color space type (<code>ColorSpace.TYPE_*</code>).
     * @param numComponents The number of color components.
     * @param isRGBPreferredIntermediary Whether sRGB (<code>true</code>)
     * or CIEXYZ (<code>false</code>) is the preferred connection
     * color space.
     *
     * @exception IllegalArgumentException if <code>numComponents</code>
     *            is non-positive.
     */
    protected ColorSpaceJAI(int type,
                            int numComponents,
                            boolean isRGBPreferredIntermediary) {
        super(type, numComponents);
        this.isRGBPreferredIntermediary = isRGBPreferredIntermediary;
    }

    /**
     * Whether sRGB is the preferred intermediary color space when converting
     * to another color space which is neither sRGB nor CIEXYZ.  This
     * serves to indicate the more efficient conversion pathway.
     *
     * @return <code>true</code> if sRGB is preferred, or <code>false</code>
     * if CIEXYZ is preferred.
     */
    public boolean isRGBPreferredIntermediary() {
	return this.isRGBPreferredIntermediary ;
    }

    /**
     * Transforms the pixel data in the source <code>Raster</code> from
     * CIEXYZ values with respect to the CIE D50 white point to the color
     * space represented by this class.  If the destination
     * <code>WritableRaster</code> is <code>null</code>, a new
     * <code>WritableRaster</code> will be created.  The
     * <code>Raster</code>s are treated as having no alpha channel, i.e.,
     * all bands are color bands.
     *
     * <p> If required by the underlying transformation, integral data will
     * be normalized according to the number of bits of the respective
     * component; floating point data should be between 0.0 and
     * 1.0&nbsp;+&nbsp;(32767.0&nbsp;/&nbsp;32768.0).
     * All integral data are assumed to be unsigned; signed data should be
     * shifted by the caller before invoking this method.
     *
     * @param src the source <code>Raster</code> to be converted.
     * @param srcComponentSize array that specifies the number of significant
     *      bits per source color component; ignored for floating point data.
     *      If <code>null</code> defaults to the value returned by
     *      <code>src.getSampleModel().getSampleSize()</code>.
     * @param dest the destination <code>WritableRaster</code>,
     *        or <code>null</code>.
     * @param destComponentSize array that specifies the number of significant
     *       bits per destination color component; ignored for floating point
     *       data.  If <code>null</code>, defaults to the value returned by
     *       <code>dest.getSampleModel().getSampleSize()</code>, or the sample
     *       size of the newly created destination WritableRaster if dest is
     *       null.
     * @return <code>dest</code> color converted from <code>src</code>
     *         or a new, <code>WritableRaster</code> containing the converted
     *         pixels if <code>dest</code> is <code>null</code>.
     * @exception IllegalArgumentException if <code>src</code> is
     *            <code>null</code>, the number of source or destination
     *            bands does not equal the number of components of the
     *            respective color space, or either component size array
     *            is non-null and has length not equal to the number of
     *            bands in the respective <code>Raster</code>.
     *
     */
    public abstract WritableRaster fromCIEXYZ(Raster src,
                                              int[] srcComponentSize,
                                              WritableRaster dest,
                                              int[] destComponentSize);

    /**
     * Transforms the pixel data in the source <code>Raster</code> from
     * sRGB to the color space represented by this class.
     * If the destination <code>WritableRaster</code> is <code>null</code>,
     * a new <code>WritableRaster</code> will be created.  The
     * <code>Raster</code>s are treated as having no alpha channel, i.e.,
     * all bands are color bands.
     *
     * <p> If required by the underlying transformation, integral data will
     * be normalized according to the number of bits of the respective
     * component; floating point data should be between 0.0 and 1.0.
     * All integral data are assumed to be unsigned; signed data should be
     * shifted by the caller before invoking this method.
     *
     * @param src the source <code>Raster</code> to be converted.
     * @param srcComponentSize array that specifies the number of significant
     *      bits per source color component; ignored for floating point data.
     *      If <code>null</code> defaults to the value returned by
     *      <code>src.getSampleModel().getSampleSize()</code>.
     * @param dest the destination <code>WritableRaster</code>,
     *        or <code>null</code>.
     * @param destComponentSize array that specifies the number of significant
     *       bits per destination color component; ignored for floating point
     *       data.  If <code>null</code>, defaults to the value returned by
     *       <code>dest.getSampleModel().getSampleSize()</code>, or the sample
     *       size of the newly created destination WritableRaster if dest is
     *       null.
     * @return <code>dest</code> color converted from <code>src</code>
     *         or a new, <code>WritableRaster</code> containing the converted
     *         pixels if <code>dest</code> is <code>null</code>.
     * @exception IllegalArgumentException if <code>src</code> is
     *            <code>null</code>, the number of source or destination
     *            bands does not equal the number of components of the
     *            respective color space, or either component size array
     *            is non-null and has length not equal to the number of
     *            bands in the respective <code>Raster</code>.
     *
     */
    public abstract WritableRaster fromRGB(Raster src,
                                           int[] srcComponentSize,
                                           WritableRaster dest,
                                           int[] destComponentSize);

    /**
     * Transforms the pixel data in the source <code>Raster</code> from
     * the color space represented by this class to CIEXYZ values with
     * respect to the CIE D50 white point.  If the destination
     * <code>WritableRaster</code> is <code>null</code>, a new
     * <code>WritableRaster</code> will be created.  The
     * <code>Raster</code>s are treated as having no alpha channel, i.e.,
     * all bands are color bands.
     *
     * <p> If required by the underlying transformation, integral data will
     * be normalized according to the number of bits of the respective
     * component; floating point data should be between 0.0 and 1.0.
     * All integral data are assumed to be unsigned; signed data should be
     * shifted by the caller before invoking this method.
     *
     * @param src the source <code>Raster</code> to be converted.
     * @param srcComponentSize array that specifies the number of significant
     *      bits per source color component; ignored for floating point data.
     *      If <code>null</code> defaults to the value returned by
     *      <code>src.getSampleModel().getSampleSize()</code>.
     * @param dest the destination <code>WritableRaster</code>,
     *        or <code>null</code>.
     * @param destComponentSize array that specifies the number of significant
     *       bits per destination color component; ignored for floating point
     *       data.  If <code>null</code>, defaults to the value returned by
     *       <code>dest.getSampleModel().getSampleSize()</code>, or the sample
     *       size of the newly created destination WritableRaster if dest is
     *       null.
     * @return <code>dest</code> color converted from <code>src</code>
     *         or a new, <code>WritableRaster</code> containing the converted
     *         pixels if <code>dest</code> is <code>null</code>.
     * @exception IllegalArgumentException if <code>src</code> is
     *            <code>null</code>, the number of source or destination
     *            bands does not equal the number of components of the
     *            respective color space, or either component size array
     *            is non-null and has length not equal to the number of
     *            bands in the respective <code>Raster</code>.
     *
     */
    public abstract WritableRaster toCIEXYZ(Raster src,
                                            int[] srcComponentSize,
                                            WritableRaster dest,
                                            int[] destComponentSize);

    /**
     * Transforms the pixel data in the source <code>Raster</code> from
     * the color space represented by this class to sRGB.
     * If the destination <code>WritableRaster</code> is <code>null</code>,
     * a new <code>WritableRaster</code> will be created.  The
     * <code>Raster</code>s are treated as having no alpha channel, i.e.,
     * all bands are color bands.
     *
     * <p> If required by the underlying transformation, integral data will
     * be normalized according to the number of bits of the respective
     * component; floating point data should be between 0.0 and 1.0.
     * All integral data are assumed to be unsigned; signed data should be
     * shifted by the caller before invoking this method.
     *
     * @param src the source <code>Raster</code> to be converted.
     * @param srcComponentSize array that specifies the number of significant
     *      bits per source color component; ignored for floating point data.
     *      If <code>null</code> defaults to the value returned by
     *      <code>src.getSampleModel().getSampleSize()</code>.
     * @param dest the destination <code>WritableRaster</code>,
     *        or <code>null</code>.
     * @param destComponentSize array that specifies the number of significant
     *       bits per destination color component; ignored for floating point
     *       data.  If <code>null</code>, defaults to the value returned by
     *       <code>dest.getSampleModel().getSampleSize()</code>, or the sample
     *       size of the newly created destination WritableRaster if dest is
     *       null.
     * @return <code>dest</code> color converted from <code>src</code>
     *         or a new, <code>WritableRaster</code> containing the converted
     *         pixels if <code>dest</code> is <code>null</code>.
     * @exception IllegalArgumentException if <code>src</code> is
     *            <code>null</code>, the number of source or destination
     *            bands does not equal the number of components of the
     *            respective color space, or either component size array
     *            is non-null and has length not equal to the number of
     *            bands in the respective <code>Raster</code>.
     *
     */
    public abstract WritableRaster toRGB(Raster src,
                                         int[] srcComponentSize,
                                         WritableRaster dest,
                                         int[] destComponentSize);
}
