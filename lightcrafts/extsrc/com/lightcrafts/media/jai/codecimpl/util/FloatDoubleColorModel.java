/*
 * $RCSfile: FloatDoubleColorModel.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.2 $
 * $Date: 2005/11/16 00:26:52 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.codecimpl.util;
import java.awt.Point;
import java.awt.color.ColorSpace;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;

/**
 * A <code>ColorModel</code> class that works with pixel values that
 * represent color and alpha information as separate samples, using
 * float or double elements.  This class can be used with an arbitrary
 * <code>ColorSpace</code>.  The number of color samples in the pixel
 * values must be same as the number of color components in the
 * <code>ColorSpace</code>.  There may be a single alpha sample.
 *
 * <p> Sample values are taken as ranging from 0.0 to 1.0; that is,
 * when converting to 8-bit RGB, a multiplication by 255 is performed
 * and values outside of the range 0-255 are clamped at the closest
 * endpoint.
 *
 * <p> For maximum efficiency, pixel data being interpreted by this
 * class should be in the sRGB color space.  This will result in 
 * only the trivial conversion (scaling by 255 and dividing by any
 * premultiplied alpha) to be performed.  Other color spaces require
 * more general conversions.
 *
 * <p> For those methods that use a primitive array pixel
 * representation of type <code>transferType</code>, the array length
 * is the same as the number of color and alpha samples.  Color
 * samples are stored first in the array followed by the alpha sample,
 * if present.  The order of the color samples is specified by the
 * <code>ColorSpace</code>.  Typically, this order reflects the name
 * of the color space type. For example, for <code>TYPE_RGB</code>,
 * index 0 corresponds to red, index 1 to green, and index 2 to blue.
 * The transfer types supported are
 * <code>DataBuffer.TYPE_FLOAT</code>,
 * <code>DataBuffer.TYPE_DOUBLE</code>.
 *
 * <p> The translation from pixel values to color/alpha components for
 * display or processing purposes is a one-to-one correspondence of
 * samples to components.
 *
 * <p> Methods that use a single int pixel representation throw an
 * <code>IllegalArgumentException</code>.
 *
 * <p> A <code>FloatDoubleColorModel</code> can be used in
 * conjunction with a <code>ComponentSampleModelJAI</code>.
 *
 * @see java.awt.image.ColorModel
 * @see java.awt.color.ColorSpace
 * @see java.awt.image.ComponentSampleModel
 * @see ComponentSampleModelJAI
 */
public class FloatDoubleColorModel extends ComponentColorModel {

    /**
     * The associated <code>ColorSpace</code>.
     *
     * @since JAI 1.1
     */
    protected ColorSpace colorSpace;

    /**
     * The type or family of the associated <code>ColorSpace</code>.
     *
     * @since JAI 1.1
     */
    protected int colorSpaceType;

    /**
     * The number of components of the associated <code>ColorSpace</code>.
     *
     * @since JAI 1.1
     */
    protected int numColorComponents;

    /**
     * The number of components represented by this <code>ColorModel</code>.
     * This will differ from the number of components of the associated
     * <code>ColorSpace</code> if there is an alpha channel.
     *
     * @since JAI 1.1
     */
    protected int numComponents;

    /**
     * Specifies what alpha values can be represented by this
     * <code>ColorModel</code>.
     *
     * @since JAI 1.1
     */
    protected int transparency;

    /**
     * Whether this <code>ColorModel</code> supports alpha.
     *
     * @since JAI 1.1
     */
    protected boolean hasAlpha;

    /**
     * Whether alpha is premultiplied.
     *
     * @since JAI 1.1
     */
    protected boolean isAlphaPremultiplied;

    private static int[] bitsHelper(int transferType,
                                    ColorSpace colorSpace,
                                    boolean hasAlpha) {
        int numBits = (transferType == DataBuffer.TYPE_FLOAT) ? 32 : 64;
        int numComponents = colorSpace.getNumComponents();
        if (hasAlpha) {
            ++numComponents;
        }
        int[] bits = new int[numComponents];
        for (int i = 0; i < numComponents; i++) {
            bits[i] = numBits;
        }

        return bits;
    }

    /**
     * Constructs a <code>ComponentColorModel</code> from the
     * specified parameters. Color components will be in the specified
     * <code>ColorSpace</code>.  <code>hasAlpha</code> indicates
     * whether alpha information is present.  If <code>hasAlpha</code>
     * is true, then the boolean <code>isAlphaPremultiplied</code>
     * specifies how to interpret color and alpha samples in pixel
     * values.  If the boolean is <code>true</code>, color samples are
     * assumed to have been multiplied by the alpha sample. The
     * <code>transparency</code> specifies what alpha values can be
     * represented by this color model.  The <code>transferType</code>
     * is the type of primitive array used to represent pixel values.
     *
     * @param colorSpace       The <code>ColorSpace</code> associated with
     *                         this color model.
     * @param hasAlpha         If true, this color model supports alpha.
     * @param isAlphaPremultiplied If true, alpha is premultiplied.
     * @param transparency     Specifies what alpha values can be represented
     *                         by this color model.
     * @param transferType     Specifies the type of primitive array used to
     *                         represent pixel values, one of
     *                         DataBuffer.TYPE_FLOAT or TYPE_DOUBLE.
     * @throws IllegalArgumentException If the transfer type is not
     *         DataBuffer.TYPE_FLOAT or TYPE_DOUBLE.
     *
     * @see java.awt.color.ColorSpace
     * @see java.awt.Transparency
     */
    public FloatDoubleColorModel(ColorSpace colorSpace,
                                 boolean hasAlpha,
                                 boolean isAlphaPremultiplied,
                                 int transparency,
                                 int transferType) {
        super(colorSpace, bitsHelper(transferType, colorSpace, hasAlpha),
              hasAlpha, isAlphaPremultiplied,
              transparency,
              transferType);

        if (transferType != DataBuffer.TYPE_FLOAT &&
            transferType != DataBuffer.TYPE_DOUBLE) {
            throw new IllegalArgumentException(JaiI18N.getString("FloatDoubleColorModel0"));
        }

        this.colorSpace = colorSpace;
        this.colorSpaceType = colorSpace.getType();
        this.numComponents =
            this.numColorComponents = colorSpace.getNumComponents();
        if (hasAlpha) {
            ++numComponents;
        }
        this.transparency = transparency;
        this.hasAlpha = hasAlpha;
        this.isAlphaPremultiplied = isAlphaPremultiplied;
    }

    /**
     * Throws an <code>IllegalArgumentException</code>, since pixel
     * values for this <code>ColorModel</code> are not conveniently
     * representable as a single <code>int</code>.
     */
    public int getRed(int pixel) {
        throw new IllegalArgumentException(JaiI18N.getString("FloatDoubleColorModel1"));
    }

    /**
     * Throws an <code>IllegalArgumentException</code>, since pixel
     * values for this <code>ColorModel</code> are not conveniently
     * representable as a single <code>int</code>.
     */
    public int getGreen(int pixel) {
        throw new IllegalArgumentException(JaiI18N.getString("FloatDoubleColorModel2"));
    }

    /**
     * Throws an <code>IllegalArgumentException</code>, since pixel
     * values for this <code>ColorModel</code> are not conveniently
     * representable as a single <code>int</code>.
     */
    public int getBlue(int pixel) {
        throw new IllegalArgumentException(JaiI18N.getString("FloatDoubleColorModel3"));
    }

    /**
     * Throws an <code>IllegalArgumentException</code>, since pixel
     * values for this <code>ColorModel</code> are not conveniently
     * representable as a single <code>int</code>.
     */
    public int getAlpha(int pixel) {
        throw new IllegalArgumentException(JaiI18N.getString("FloatDoubleColorModel4"));
    }

    /**
     * Throws an <code>IllegalArgumentException</code>, since pixel
     * values for this <code>ColorModel</code> are not conveniently
     * representable as a single <code>int</code>.
     */
    public int getRGB(int pixel) {
        throw new IllegalArgumentException(JaiI18N.getString("FloatDoubleColorModel5"));
    }

    private final int clamp(float value) {
        // Ensure NaN maps to 0
        return (value >= 0.0F) ? ((value > 255.0F) ? 255 : (int)value) : 0;
    }

    private final int clamp(double value) {
        // Ensure NaN maps to 0
        return (value >= 0.0) ? ((value > 255.0) ? 255 : (int)value) : 0;
    }

    private int getSample(Object inData, int sample) {
        boolean needAlpha = (hasAlpha && isAlphaPremultiplied);
        int type = colorSpaceType;

        boolean is_sRGB = colorSpace.isCS_sRGB();

        if (type == ColorSpace.TYPE_GRAY) {
            sample = 0;
            is_sRGB = true;
        }
		  

        if (is_sRGB) {
            if (transferType == DataBuffer.TYPE_FLOAT) {
                float[] fdata = (float[])inData;
                float fsample = fdata[sample]*255;
                if (needAlpha) {
                    float falp = fdata[numColorComponents];
		    if (falp == 0.0) return 0;
		    else
		      return clamp(fsample/falp);
                } else {
                    return clamp(fsample);
                }
            } else {
                double[] ddata = (double[])inData;
                double dsample = ddata[sample]*255.0;
                if (needAlpha) {
                    double dalp = ddata[numColorComponents];
		    if (dalp == 0.0) return 0;
		    else 
		      return clamp(dsample/dalp);
                } else {
                    return clamp(dsample);
                }
            }
        }

        // Not TYPE_GRAY or TYPE_RGB ColorSpace
        float[] norm;
        float[] rgb;
        if (transferType == DataBuffer.TYPE_FLOAT) {
            float[] fdata = (float[])inData;
            if (needAlpha) {
                float falp = fdata[numColorComponents];
		if (falp == 0.0) return 0;
                norm = new float[numColorComponents];
                for (int i = 0; i < numColorComponents; i++) {
                    norm[i] = fdata[i]/falp;
                }
                rgb = colorSpace.toRGB(norm);
            } else {
                rgb = colorSpace.toRGB(fdata);
            }
	    return (int)(rgb[sample]*255 + 0.5F); 
        } else {
            double[] ddata = (double[])inData;
            norm = new float[numColorComponents];
            if (needAlpha) {
                double dalp = ddata[numColorComponents];
		if (dalp == 0.0) return 0;
                for (int i = 0; i < numColorComponents; i++) {
                    norm[i] = (float)(ddata[i]/dalp);
                }
                rgb = colorSpace.toRGB(norm);
            } else {
                for (int i = 0; i < numColorComponents; i++) {
                    norm[i] = (float)ddata[i];
                }
                rgb = colorSpace.toRGB(norm);
            }
	    return (int)(rgb[sample]*255 + 0.5);
        }
    }

    /**
     * Returns the red color component for the specified pixel, scaled
     * from 0 to 255 in the default RGB <code>ColorSpace</code>, sRGB.  A color
     * conversion is done if necessary.  The <code>pixel</code> value
     * is specified by an array of data elements of type
     * <code>transferType</code> passed in as an object reference. The
     * returned value will be a non pre-multiplied value. If the alpha
     * is premultiplied, this method divides it out before returning
     * the value (if the alpha value is 0, the red value will be 0).
     *
     * @param inData The pixel from which to get the red
     * color component, specified by an array of data elements of type
     * <code>transferType</code>.
     *
     * @return The red color component for the specified pixel, as an
     * int.
     *
     * @throws ClassCastException If <code>inData</code> is not a
     * primitive array of type <code>transferType</code>.
     * @throws ArrayIndexOutOfBoundsException if <code>inData</code>
     * is not large enough to hold a pixel value for this
     * <code>ColorModel</code>.
     */
    public int getRed(Object inData) {
        return getSample(inData, 0);
    }

    /**
     * Returns the green color component for the specified pixel, scaled
     * from 0 to 255 in the default RGB <code>ColorSpace</code>, sRGB.  A color
     * conversion is done if necessary.  The <code>pixel</code> value
     * is specified by an array of data elements of type
     * <code>transferType</code> passed in as an object reference. The
     * returned value will be a non pre-multiplied value. If the alpha
     * is premultiplied, this method divides it out before returning
     * the value (if the alpha value is 0, the green value will be 0).
     *
     * @param inData The pixel from which to get the green
     * color component, specified by an array of data elements of type
     * <code>transferType</code>.
     *
     * @return The green color component for the specified pixel, as an
     * int.
     *
     * @throws ClassCastException If <code>inData</code> is not a
     * primitive array of type <code>transferType</code>.
     * @throws ArrayIndexOutOfBoundsException if <code>inData</code>
     * is not large enough to hold a pixel value for this
     * <code>ColorModel</code>.
     */
    public int getGreen(Object inData) {
        return getSample(inData, 1);
    }

    /**
     * Returns the blue color component for the specified pixel, scaled
     * from 0 to 255 in the default RGB <code>ColorSpace</code>, sRGB.  A color
     * conversion is done if necessary.  The <code>pixel</code> value
     * is specified by an array of data elements of type
     * <code>transferType</code> passed in as an object reference. The
     * returned value will be a non pre-multiplied value. If the alpha
     * is premultiplied, this method divides it out before returning
     * the value (if the alpha value is 0, the blue value will be 0).
     *
     * @param inData The pixel from which to get the blue
     * color component, specified by an array of data elements of type
     * <code>transferType</code>.
     *
     * @return The blue color component for the specified pixel, as an
     * int.
     *
     * @throws ClassCastException If <code>inData</code> is not a
     * primitive array of type <code>transferType</code>.
     * @throws ArrayIndexOutOfBoundsException if <code>inData</code>
     * is not large enough to hold a pixel value for this
     * <code>ColorModel</code>.
     */
    public int getBlue(Object inData) {
        return getSample(inData, 2);
    }

    /**
     * Returns the alpha component for the specified pixel, scaled
     * from 0 to 255.  The pixel value is specified by an array of
     * data elements of type <code>transferType</code> passed in as an
     * object reference.  If the <code>ColorModel</code> does not have
     * alpha, 255 is returned.
     *
     * @param inData The pixel from which to get the alpha
     * component, specified by an array of data elements of type
     * <code>transferType</code>.
     *
     * @return The alpha component for the specified pixel, as an int.
     *
     * @throws IllegalArgumentException if <code>inData</code> is
     * <code>null</code> and the <code>colorModel</code> has alpha.
     * @throws ClassCastException If <code>inData</code> is not a
     * primitive array of type <code>transferType</code> and the
     * <code>ColorModel</code> has alpha.
     * @throws ArrayIndexOutOfBoundsException if <code>inData</code>
     * is not large enough to hold a pixel value for this
     * <code>ColorModel</code> and the <code>ColorModel</code> has
     * alpha.
     */
    public int getAlpha(Object inData) {
        if ( inData == null ) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        if (hasAlpha == false) {
            return 255;
        }

        if (transferType == DataBuffer.TYPE_FLOAT) {
            float[] fdata = (float[])inData;
            return (int)(fdata[numColorComponents]*255.0F + 0.5F);
        } else {
            double[] ddata = (double[])inData;
            return (int)(ddata[numColorComponents]*255.0 + 0.5);
        }
    }

    /**
     * Returns the color/alpha components for the specified pixel in
     * the default RGB color model format.  A color conversion is done
     * if necessary.  The pixel value is specified by an array of data
     * elements of type <code>transferType</code> passed in as an
     * object reference.  The returned value is in a non
     * pre-multiplied format. If the alpha is premultiplied, this
     * method divides it out of the color components (if the alpha
     * value is 0, the color values will be 0).
     *
     * @param inData The pixel from which to get the
     * color/alpha components, specified by an array of data elements
     * of type <code>transferType</code>.
     *
     * @return The color/alpha components for the specified pixel, as an int.
     *
     * @throws ClassCastException If <code>inData</code> is not a
     * primitive array of type <code>transferType</code>.
     * @throws ArrayIndexOutOfBoundsException if <code>inData</code>
     * is not large enough to hold a pixel value for this
     * <code>ColorModel</code>.
     */
    public int getRGB(Object inData) {
        boolean needAlpha = (hasAlpha && isAlphaPremultiplied);
	int alpha = 255;
	int red, green, blue;

	if (colorSpace.isCS_sRGB()) {
            if (transferType == DataBuffer.TYPE_FLOAT) {
                float[] fdata = (float[])inData;
                float fred = fdata[0];
                float fgreen = fdata[1];
                float fblue = fdata[2];
                float fscale = 255.0F;
                if (needAlpha) {
                    float falpha = fdata[3];
                    fscale /= falpha;
                    alpha = clamp(255.0F*falpha);
                }

		red = clamp(fred*fscale);
		green = clamp(fgreen*fscale);
		blue = clamp(fblue*fscale);
            } else {
                double[] ddata = (double[])inData;
                double dred = ddata[0];
                double dgreen = ddata[1];
                double dblue = ddata[2];
                double dscale = 255.0;
                if (needAlpha) {
                    double dalpha = ddata[3];
                    dscale /= dalpha;
                    alpha = clamp(255.0*dalpha);
                }

		red = clamp(dred*dscale);
		green = clamp(dgreen*dscale);
		blue = clamp(dblue*dscale);
	    }
	} else if (colorSpaceType == ColorSpace.TYPE_GRAY) {
	    if (transferType == DataBuffer.TYPE_FLOAT) {
                float[] fdata = (float[])inData;
		float fgray = fdata[0];
                if (needAlpha) {
                    float falp = fdata[1];
		    red = green = blue = clamp(fgray*255.0F/falp);
                    alpha = clamp(255.0F*falp);
		} else {
		    red = green = blue = clamp(fgray*255.0F);
		}
	    } else {
                double[] ddata = (double[])inData;
		double dgray = ddata[0];
                if (needAlpha) {
                    double dalp = ddata[1];
		    red = green = blue = clamp(dgray*255.0/dalp);
                    alpha = clamp(255.0*dalp);
		} else {
		    red = green = blue = clamp(dgray*255.0);
		}
	    }
        } else {
	    // Not Gray or sRGB
	    float[] norm;
	    float[] rgb;
	    if (transferType == DataBuffer.TYPE_FLOAT) {
		float[] fdata = (float[])inData;
		if (needAlpha) {
		    float falp = fdata[numColorComponents];
		    float invfalp = 1.0F/falp;
		    norm = new float[numColorComponents];
		    for (int i = 0; i < numColorComponents; i++) {
			norm[i] = fdata[i]*invfalp;
		    }
                    alpha = clamp(255.0F*falp);
		} else {
		    norm = fdata;
		}
	    } else {
                double[] ddata = (double[])inData;
		norm = new float[numColorComponents];
		if (needAlpha) {
		    double dalp = ddata[numColorComponents];
		    double invdalp = 1.0/dalp;
		    for (int i = 0; i < numColorComponents; i++) {
			norm[i] = (float)(ddata[i]*invdalp);
		    }
                    alpha = clamp(255.0*dalp);
		} else {
		    for (int i = 0; i < numColorComponents; i++) {
			norm[i] = (float)ddata[i];
		    }
		}
	    }
            
            // Perform color conversion
	    rgb = colorSpace.toRGB(norm);

	    red = clamp(rgb[0]*255.0F);
	    green = clamp(rgb[1]*255.0F);
	    blue = clamp(rgb[2]*255.0F);
	}

	return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }


    /**
     * Returns a data element array representation of a pixel in this
     * <code>ColorModel</code>, given an integer pixel representation
     * in the default RGB color model.  This array can then be passed
     * to the <code>setDataElements</code> method of a
     * <code>WritableRaster</code> object.  If the <code>pixel</code>
     * parameter is null, a new array is allocated.
     * If the colorSpaceType is of TYPE_GRAY then the rgb components
     * are converted to gray using appropriate weights
     *
     * @param rgb An ARGB value packed into an int.
     * @param pixel The float or double array representation of the pixel.
     *
     * @throws ClassCastException If <code>pixel</code> is not null and 
     * is not a primitive array of type <code>transferType</code>.  
     *
     * @throws ArrayIndexOutOfBoundsException If <code>pixel</code> is 
     * not large enough to hold a pixel value for this
     * <code>ColorModel</code>. 
     */
    public Object getDataElements(int rgb, Object pixel) {
        if (transferType == DataBuffer.TYPE_FLOAT) {
            float[] floatPixel;

            if (pixel == null) {
                floatPixel = new float[numComponents];
            } else {
                if (!(pixel instanceof float[])) {
                    throw new ClassCastException(JaiI18N.getString("FloatDoubleColorModel7"));                
                }
                floatPixel = (float[])pixel;
                if (floatPixel.length < numComponents) {
                    throw new ArrayIndexOutOfBoundsException(JaiI18N.getString("FloatDoubleColorModel8"));
                }
            }

            float inv255 = 1.0F/255.0F;
            if (colorSpace.isCS_sRGB()) {
                int alp = (rgb >> 24) & 0xff;
                int red = (rgb >> 16) & 0xff;
                int grn = (rgb >>  8) & 0xff;
                int blu = (rgb      ) & 0xff;
                float norm = inv255;
                if (isAlphaPremultiplied) {
                    norm *= alp;
                }
                floatPixel[0] = red*norm;
                floatPixel[1] = grn*norm;
                floatPixel[2] = blu*norm;
                if (hasAlpha) {
                    floatPixel[3] = alp*inv255;
                }
            } else if (colorSpaceType == ColorSpace.TYPE_GRAY) {
                float gray = ((((rgb>>16)&0xff)*(.299F*inv255)) +
                              (((rgb>>8) &0xff)*(.587F*inv255)) +
                              (((rgb)    &0xff)*(.114F*inv255)));
                
                floatPixel[0] = gray;
                
                if (hasAlpha) {
                    int alpha = (rgb>>24) & 0xff;
                    floatPixel[1] = alpha*inv255;
                }
            } else {
                // Need to convert the color
                float[] norm = new float[3];
                norm[0] = ((rgb>>16) & 0xff)*inv255;
                norm[1] = ((rgb>>8)  & 0xff)*inv255;
                norm[2] = ((rgb)     & 0xff)*inv255;
                
                norm = colorSpace.fromRGB(norm);
                for (int i = 0; i < numColorComponents; i++) {
                    floatPixel[i] = norm[i];
                }
                if (hasAlpha) {
                    int alpha = (rgb>>24) & 0xff;
                    floatPixel[numColorComponents] = alpha*inv255;
                }
            }

            return floatPixel;
        } else { // transferType == DataBuffer.TYPE_DOUBLE
            double[] doublePixel;

            if (pixel == null) {
                doublePixel = new double[numComponents];
            } else {
                if (!(pixel instanceof double[])) {
                    throw new ClassCastException(JaiI18N.getString("FloatDoubleColorModel7"));                
                }
                doublePixel = (double[])pixel;
                if (doublePixel.length < numComponents) {
                    throw new ArrayIndexOutOfBoundsException(JaiI18N.getString("FloatDoubleColorModel8"));
                }
            }

            double inv255 = 1.0/255.0;
            if (colorSpace.isCS_sRGB()) {
                int alp = (rgb>>24) & 0xff;
                int red = (rgb>>16) & 0xff;
                int grn = (rgb>>8)  & 0xff;
                int blu = (rgb)     & 0xff;
                double norm = inv255;
                if (isAlphaPremultiplied) {
                    norm *= alp;
                }
                doublePixel[0] = red*norm;
                doublePixel[1] = grn*norm;
                doublePixel[2] = blu*norm;
                if (hasAlpha) {
                    doublePixel[3] = alp*inv255;
                }
            } else if (colorSpaceType == ColorSpace.TYPE_GRAY) {
                double gray = ((((rgb>>16) & 0xff)*(.299*inv255)) +
                               (((rgb>>8)  & 0xff)*(.587*inv255)) +
                               (((rgb)     & 0xff)*(.114*inv255)));
                
                doublePixel[0] = gray;
                
                if (hasAlpha) {
                    int alpha = (rgb>>24) & 0xff;
                    doublePixel[1] = alpha*inv255;
                }
            } else {
                float inv255F = 1.0F/255.0F;
                
                // Need to convert the color, need data in float form
                float[] norm = new float[3];
                norm[0] = ((rgb>>16) & 0xff)*inv255F;
                norm[1] = ((rgb>>8)  & 0xff)*inv255F;
                norm[2] = ((rgb)     & 0xff)*inv255F;
                
                norm = colorSpace.fromRGB(norm);
                for (int i = 0; i < numColorComponents; i++) {
                    doublePixel[i] = (double)norm[i];
                }
                if (hasAlpha) {
                    int alpha = (rgb>>24) & 0xff;
                    doublePixel[numColorComponents] = alpha*inv255;
                }
            }

            return doublePixel;
        }
    }

    /**
     * Throws an <code>IllegalArgumentException</code>, since pixel
     * values for this <code>ColorModel</code> are not conveniently
     * representable as a single <code>int</code>.
     */
    public int[] getComponents(int pixel, int[] components, int offset) {
        throw new
	   IllegalArgumentException(JaiI18N.getString("FloatDoubleColorModel9"));
    }
    
    /**
     * Throws an <code>IllegalArgumentException</code> since
     * the pixel values cannot be placed into an <code>int</code> array.
     */
    public int[] getComponents(Object pixel, int[] components, int offset) {
        throw new 
	   IllegalArgumentException(JaiI18N.getString("FloatDoubleColorModel9"));
    }
    
    /**
     * Throws an <code>IllegalArgumentException</code>, since pixel
     * values for this <code>ColorModel</code> are not conveniently
     * representable as a single <code>int</code>.
     */
    public int getDataElement(int[] components, int offset) {
        throw new
	   IllegalArgumentException(JaiI18N.getString("FloatDoubleColorModel9"));
    }
    
    /**
     * Returns a data element array representation of a pixel in this
     * <code>ColorModel</code>, given an array of unnormalized
     * color/alpha components. This array can then be passed to the
     * <code>setDataElements</code> method of a
     * <code>WritableRaster</code> object.
     * 
     * @param components An array of unnormalized color/alpha
     * components.
     * @param offset The integer offset into the
     * <code>components</code> array.
     * @param obj The object in which to store the data element array
     * representation of the pixel. If <code>obj</code> variable is
     * null, a new array is allocated.  If <code>obj</code> is not
     * null, it must be a primitive array of type
     * <code>transferType</code>. An
     * <code>ArrayIndexOutOfBoundsException</code> is thrown if
     * <code>obj</code> is not large enough to hold a pixel value for
     * this <code>ColorModel</code>.
     *
     * @return The data element array representation of a pixel 
     * in this <code>ColorModel</code>.
     *
     * @throws IllegalArgumentException If the components array
     * is not large enough to hold all the color and alpha components
     * (starting at offset).
     * @throws ClassCastException If <code>obj</code> is not null and
     * is not a primitive array of type <code>transferType</code>.
     * @throws ArrayIndexOutOfBoundsException If <code>obj</code> is
     * not large enough to hold a pixel value for this
     * <code>ColorModel</code>.
     */
    public Object getDataElements(int[] components, int offset, Object obj) {
        if ((components.length-offset) < numComponents) {
            throw new IllegalArgumentException(numComponents + " " + 
				   JaiI18N.getString("FloatDoubleColorModel10"));
        }
        if (transferType == DataBuffer.TYPE_FLOAT) {
            float[] pixel;
            if (obj == null) {
                pixel = new float[components.length];
            } else {
                pixel = (float[])obj;
            }
            for (int i=0; i < numComponents; i++) {
                pixel[i] = (float)(components[offset + i]);
            }

            return pixel;
        } else {
            double[] pixel;
            if (obj == null) {
                pixel = new double[components.length];
            } else {
                pixel = (double[])obj;
            }
            for (int i=0; i < numComponents; i++) {
                pixel[i] = (double)(components[offset + i]);
            }
            
            return pixel;
        }
    }

    /**
     * Forces the <code>raster</code> data to match the state specified in the
     * <code>isAlphaPremultiplied</code> variable, assuming the data 
     * is currently correctly described by this <code>ColorModel</code>.  
     * It may multiply or divide the color <code>raster</code> data by alpha, or 
     * do nothing if the data is in the correct state.  If the data needs 
     * to be coerced, this method also returns an instance of 
     * <code>FloatDoubleColorModel</code> with
     * the <code>isAlphaPremultiplied</code> flag set appropriately.
     *
     * @throws IllegalArgumentException if transfer type of
     * <code>raster</code> is not the same as that of this
     * <code>FloatDoubleColorModel</code>.
     */
    public ColorModel coerceData (WritableRaster raster, 
                                  boolean isAlphaPremultiplied) {
        if ((hasAlpha == false) ||
            (this.isAlphaPremultiplied == isAlphaPremultiplied))
        {
            // Nothing to do
            return this;
        }
        
        int w = raster.getWidth();
        int h = raster.getHeight();
        int aIdx = raster.getNumBands() - 1;
        int rminX = raster.getMinX();
        int rY = raster.getMinY();
        int rX;

        if (raster.getTransferType() != transferType) {
            throw new IllegalArgumentException(
				    JaiI18N.getString("FloatDoubleColorModel6"));
        }

        if (isAlphaPremultiplied) {
            switch (transferType) {
                case DataBuffer.TYPE_FLOAT: {
                    float pixel[] = null;
                    for (int y = 0; y < h; y++, rY++) {
                        rX = rminX;
                        for (int x = 0; x < w; x++, rX++) {
                            pixel = (float[])raster.getDataElements(rX, rY,
                                                                    pixel);
                            float fAlpha = pixel[aIdx];
                            if (fAlpha != 0) {
                                for (int c=0; c < aIdx; c++) {
                                    pixel[c] *= fAlpha;
                                }
                                raster.setDataElements(rX, rY, pixel);
                            }
                        }
                    }
                }
                break;
               
	    case DataBuffer.TYPE_DOUBLE: {
                    double pixel[] = null;
                    for (int y = 0; y < h; y++, rY++) {
                        rX = rminX;
                        for (int x = 0; x < w; x++, rX++) {
                            pixel = (double[])raster.getDataElements(rX, rY,
                                                                     pixel);
                            double dAlpha = pixel[aIdx];
                            if (dAlpha != 0) {
                                for (int c=0; c < aIdx; c++) {
                                    pixel[c] *= dAlpha;
                                }
                                raster.setDataElements(rX, rY, pixel);
                            }
                        }
                    }
                }
		break;
	    
	    default:
	      throw new RuntimeException(JaiI18N.getString("FloatDoubleColorModel0"));
        }

        if (isAlphaPremultiplied) {

            }
        }
        else {
            // We are premultiplied and want to divide it out
            switch (transferType) {
                case DataBuffer.TYPE_FLOAT: {
                    for (int y = 0; y < h; y++, rY++) {
                        rX = rminX;
                        for (int x = 0; x < w; x++, rX++) {
                            float pixel[] = null;
                            pixel = (float[])raster.getDataElements(rX, rY,
                                                                    pixel);
                            float fAlpha = pixel[aIdx];
                            if (fAlpha != 0) {
                                float invFAlpha = 1.0F/fAlpha;
                                for (int c=0; c < aIdx; c++) {
                                    pixel[c] *= invFAlpha;
                                }
                            }
                            raster.setDataElements(rX, rY, pixel);
                        }
                    }
                }
                break;
                
	    case DataBuffer.TYPE_DOUBLE: {
                    for (int y = 0; y < h; y++, rY++) {
                        rX = rminX;
                        for (int x = 0; x < w; x++, rX++) {
                            double pixel[] = null;
                            pixel = (double[])raster.getDataElements(rX, rY,
                                                                     pixel);
                            double dAlpha = pixel[aIdx];
                            if (dAlpha != 0) {
                                double invDAlpha = 1.0/dAlpha;
                                for (int c=0; c < aIdx; c++) {
                                    pixel[c] *= invDAlpha;
                                }
                            }
                            raster.setDataElements(rX, rY, pixel);
                        }
                    }
                }
                break;

	    default:
	      throw new RuntimeException(JaiI18N.getString("FloatDoubleColorModel0"));
            }
        }

        // Return a new color model
        return new FloatDoubleColorModel(colorSpace, hasAlpha,
                                         isAlphaPremultiplied, transparency,
                                         transferType);
    }

    /**
     * Returns <code>true</code> if the supplied <code>Raster</code>'s
     * <code>SampleModel</code> is compatible with this
     * <code>FloatDoubleColorModel</code>.
     *
     * @param raster a <code>Raster</code>to be checked for compatibility.
     */
    public boolean isCompatibleRaster(Raster raster) {
        SampleModel sm = raster.getSampleModel();
        return isCompatibleSampleModel(sm);
    }
    
    /**
     * Creates a <code>WritableRaster</code> with the specified width
     * and height, that has a data layout (<code>SampleModel</code>)
     * compatible with this <code>ColorModel</code>.  The returned
     * <code>WritableRaster</code>'s <code>SampleModel</code> will be
     * an instance of <code>ComponentSampleModel</code>.
     *
     * @param w The width of the <code>WritableRaster</code>
     * @param h The height of the <code>WritableRaster</code> 
     *
     * @return A <code>WritableRaster</code> that is compatible with
     * this <code>ColorModel</code>.
     *
     * @see java.awt.image.WritableRaster
     * @see java.awt.image.SampleModel
     */
    public WritableRaster createCompatibleWritableRaster(int w, int h) {
        SampleModel sm = createCompatibleSampleModel(w, h);
        return RasterFactory.createWritableRaster(sm, new Point(0, 0));
    }

    /**
     * Creates a <code>SampleModel</code> with the specified width and
     * height that has a data layout compatible with this
     * <code>ColorModel</code>.  The returned <code>SampleModel</code>
     * will be an instance of <code>ComponentSampleModel</code>.
     *
     * @param w The width of the <code>SampleModel</code>.
     * @param h The height of the <code>SampleModel</code>.
     *
     * @return A <code>SampleModel</code> that is compatible with this
     * <code>ColorModel</code>.
     *
     * @see java.awt.image.SampleModel	 
     * @see java.awt.image.ComponentSampleModel	 
     */
    public SampleModel createCompatibleSampleModel(int w, int h) {
        int[] bandOffsets = new int[numComponents];
        for (int i = 0; i < numComponents; i++) {
            bandOffsets[i] = i;
        }
        return new ComponentSampleModelJAI(transferType,
                                           w, h,
                                           numComponents,
                                           w*numComponents,
                                           bandOffsets);
    }
    
    /** 
     * Checks whether or not the specified <code>SampleModel</code> is
     * compatible with this <code>ColorModel</code>.  A
     * <code>SampleModel</code> is compatible if it is an instance of
     * <code>ComponentSampleModel</code>, has the sample number of
     * bands as the total number of components (including alpha) in
     * the <code>ColorSpace</code> used by this
     * <code>ColorModel</code>, and has the same data type (float or
     * double) as this <code>ColorModel</code>.
     *
     * @param sm The <code>SampleModel</code> to test for compatibility.
     *
     * @return <code>true</code> if the <code>SampleModel</code> is
     * compatible with this <code>ColorModel</code>,
     * <code>false</code> if it is not.
     *
     * @see java.awt.image.SampleModel
     * @see java.awt.image.ComponentSampleModel
     */
    public boolean isCompatibleSampleModel(SampleModel sm) {
        if (sm instanceof ComponentSampleModel) {
            if (sm.getNumBands() != getNumComponents()) {
                return false;
            }
            if (sm.getDataType() != transferType) {
                return false;
            }
            return true;
        } else {
            return false;
        }
    }

    /** Returns a <code>String</code> containing the values of all valid fields. */
    public String toString() {
        return "FloatDoubleColorModel: " + super.toString();
    }
}
