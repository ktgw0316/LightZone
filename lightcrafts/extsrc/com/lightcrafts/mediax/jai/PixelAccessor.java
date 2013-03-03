/*
 * $RCSfile: PixelAccessor.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:15 $
 * $State: Exp $
 */
package com.lightcrafts.mediax.jai;
import java.awt.Rectangle;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferShort;
import java.awt.image.DataBufferUShort;
import java.awt.image.IndexColorModel;
import java.awt.image.MultiPixelPackedSampleModel;
import java.awt.image.PackedColorModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.SinglePixelPackedSampleModel;
import java.awt.image.WritableRaster;
import java.util.Vector;
import com.lightcrafts.media.jai.util.DataBufferUtils;
import com.lightcrafts.media.jai.util.ImageUtil;
import com.lightcrafts.media.jai.util.JDKWorkarounds;

/**
 * This is a utility class that may be used to access the pixel data
 * stored in a <code>RenderedImage</code>'s <code>Raster</code>s, as
 * well as performing pixel-to-color data translation based on the
 * image's <code>SampleModel</code> and <code>ColorModel</code>.  It
 * also provides several static methods to determine information about
 * the image data.
 *
 * <p> This class is intended to help classes that need to access the
 * pixel and/or color data of a <code>RenderedImage</code>, such as an
 * <code>OpImage</code>, in an optimized fashion.  Most of the variables
 * defined in this class are public so that other classes may use them
 * directly.  However, the variables are also declared <code>final</code>
 * so that other classes can not modify their values.
 *
 * <p> In general, the pixel data of a <code>RenderedImage</code> may
 * be obtained by calling the <code>getPixels()</code> method.  By
 * definition, the <i>pixel data</i> of an image are the data described
 * by the image's <code>SampleModel</code> and stored in the image's
 * <code>Raster</code>s.  No consideration of any kind is given to the
 * image's <code>ColorModel</code>.  If no error is found, the pixel
 * data are returned in the primitive arrays of the type specified by
 * the caller in an unpacked format, along with access information.
 * Therefore, the specified data type must be one of the valid types
 * defined in <code>DataBuffer</code> and large enough (in bit depth)
 * to hold the pixel data of the image.
 *
 * <p> The pixel data of a binary image may be obtained in a packed
 * format by calling the <code>getPackedPixels()</code> method.  It
 * returns the data in a packed <code>byte</code> array, with 8 pixels
 * packed into 1 byte.  The format of the data in the array is
 * similar to the format described by the
 * <code>MultiPixelPackedSampleModel</code>, where the end of each
 * scanline is padded to the end of the byte if necessary.  Note that this
 * method returns a valid result only if and only if the image is a
 * single-band bit image, that is, each pixel has only 1 sample with a
 * sample size of 1 bit.
 *
 * <p> Two corresponding "set" methods are also provided for setting the
 * computed pixel data back into the <code>Raster</code>'s
 * <code>DataBuffer</code>: <code>setPixels()</code> for unpacked data,
 * and <code>setPackedPixels()</code> for packed data.  It is very
 * important that the caller uses the correct "set" method that matches
 * the "get" method used to obtain the data, or errors will occur.
 *
 * <p> The color/alpha data of the <code>RenderedImage</code> may be
 * obtained by calling the <code>getComponents()</code> method which
 * returns the unnormalized data in the <code>ColorSpace</code> specified
 * in the <code>ColorModel</code>, or the <code>getComponentsRGB()</code>
 * method which returns the data scaled from 0 to 255 in the default sRGB
 * <code>ColorSpace</code>.  These methods retrieve the pixel data from
 * the <code>Raster</code>, and perform the pixel-to-color translation.
 * Therefore, in order for these two methods to return a valid result, the
 * image must have a valid <code>ColorModel</code>.
 *
 * <p> Similarly, two "set" methods may be used to perform the
 * color-to-pixel translation, and set the pixel data back to the
 * <code>Raster</code>'s <code>DataBuffer</code>.  Again, it is important
 * that the "get" and "set" methods are matched up correctly.
 *
 * <p> In addition, several static methods are included in this class
 * for the convenience of <code>OpImage</code> developers, who may use them to
 * help determine the appropriate destination <code>SampleModel</code>
 * type.
 *
 * @since JAI 1.1
 *
 */
public final class PixelAccessor {

    /** Tag for single-bit data type. */
    public static final int TYPE_BIT = -1;

    /** The image's <code>SampleModel</code>. */
    public final SampleModel sampleModel;

    /** The image's <code>ColorModel</code>. */
    public final ColorModel colorModel;

    // The following information comes from the image's SampleModel.

    /**
     * <code>true</code> if the image has a
     * <code>ComponentSampleModel</code>;
     * <code>false</code> otherwise.
     */
    public final boolean isComponentSM;

    /**
     * <code>true</code> if the image has a
     * <code>MultiPixelPackedSampleModel</code>;
     * <code>false</code> otherwise.
     */
    public final boolean isMultiPixelPackedSM;

    /**
     * <code>true</code> if the image has a
     * <code>SinglePixelPackedSampleModel</code>;
     * <code>false</code> otherwise.
     */
    public final boolean isSinglePixelPackedSM;

    /** The data type of the pixel samples, determined based on the sample size. */
    public final int sampleType;

    /**
     * The type of the <code>DataBuffer</code>'s data array used to store the
     * pixel data by the image's <code>SampleModel</code>.  This is the same
     * value as that returned by <code>SampleModel.getDataType()</code>.
     */
    public final int bufferType;

    /**
     * The type of the primitive array used to transfer the pixel data by
     * the image's <code>SampleModel</code>.  This is the same value as
     * that returned by <code>SampleModel.getTransferType()</code>.
     */
    public final int transferType;

    /**
     * The number of bands (samples) per pixel.  This is the same value
     * as that returned by <code>SampleModel.getNumBands()</code>.
     */
    public final int numBands;

    /**
     * The size, in number of bits, of all the pixel samples.  This is
     * the same array as that returned by
     * <code>SampleModel.getSampleSize()</code>.
     */
    public final int[] sampleSize;

    /**
     * Set to <code>true</code> if the pixel data of this image may be
     * packed into a <code>byte</code> array.  That is, each pixel has
     * 1 sample (1 band) with a sample size of 1 bit.  If this variable
     * is <code>true</code>, <code>getPackedPixels()</code> should return
     * a valid result, with 8 pixels packed into 1 byte.
     */
    public final boolean isPacked;

    // The following information come from the image's ColorModel.

    /**
     * Set to <code>true</code> if the image has a non-null
     * <code>ColorModel</code> which is compatible with the image's
     * <code>SampleModel</code>; <code>false</code> otherwise.
     */
    public final boolean hasCompatibleCM;

    /**
     * Set to <code>true</code> if the image has a
     * <code>ComponentColorModel</code>;
     * <code>false</code> otherwise.
     */
    public final boolean isComponentCM;

    /**
     * Set to <code>true</code> if the image has an
     * <code>IndexColorModel</code>;
     * <code>false</code> otherwise.
     */
    public final boolean isIndexCM;

    /**
     * Set to <code>true</code> if the image has a
     * <code>PackedColorModel</code>;
     * <code>false</code> otherwise.
     */
    public final boolean isPackedCM;

    /**
     * The type of the color/alpha components, determined based on the
     * component size.
     */
    public final int componentType;

    /**
     * The total number of color/alpha components in the image's
     * <code>ColorModel</code>.  This is the same value as that
     * returned by <code>ColorModel.getNumComponents()</code>.
     */
    public final int numComponents;

    /**
     * The size, in number of bits, of all the color/alpha components.
     * This is the same array as that returned by
     * <code>ColorModel.getComponentSize()</code>.
     */
    public final int[] componentSize;

    /**
     * Returns the image's <code>SampleModel</code>.
     *
     * @throws IllegalArgumentException if <code>image</code> is
     *         <code>null</code>.
     */
    private static SampleModel getSampleModel(RenderedImage image) {
        if(image == null) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }
        return image.getSampleModel();
    }

    /**
     * Constructs a <code>PixelAccessor</code> from a
     * <code>RenderedImage</code>.
     * The <code>RenderedImage</code> must have a valid
     * <code>SampleModel</code>, but may or may not have a valid
     * <code>ColorModel</code>.
     *
     * @param image  The image whose data are to be accessed.
     *
     * @throws IllegalArgumentException  If <code>image</code> is
     *         <code>null</code>, or if the image does not have a valid
     *         <code>SampleModel</code>.
     */
    public PixelAccessor(RenderedImage image) {
        this(getSampleModel(image), image.getColorModel());
    }

    /**
     * Constructs a <code>PixelAccessor</code> given a valid
     * <code>SampleModel</code> and a (possibly <code>null</code>)
     * <code>ColorModel</code>.
     *
     * @param sm The <code>SampleModel</code> for the image to be accessed.
     *           Must be valid.
     * @param cm The <code>ColorModel</code> for the image to be accessed.
     *           May be null.
     * @throws IllegalArgumentException  If <code>sm</code> is <code>null</code>.
     */
    public PixelAccessor(SampleModel sm, ColorModel cm) {

        if ( sm == null ) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        sampleModel = sm;
        colorModel = cm;

        // Information from the SampleModel.
        isComponentSM = sampleModel instanceof ComponentSampleModel;
        isMultiPixelPackedSM = sampleModel instanceof
                               MultiPixelPackedSampleModel;
        isSinglePixelPackedSM = sampleModel instanceof
                                SinglePixelPackedSampleModel;

        bufferType = sampleModel.getDataType();
        transferType = sampleModel.getTransferType();
        numBands = sampleModel.getNumBands();
        sampleSize = sampleModel.getSampleSize();
        sampleType = isComponentSM ? bufferType : getType(sampleSize);

        // Indicates whether the pixel data may be stored in packed format.
        isPacked = sampleType == TYPE_BIT && numBands == 1;

        // Information from the ColorModel.
        hasCompatibleCM = colorModel != null &&
            JDKWorkarounds.areCompatibleDataModels(sampleModel, colorModel);

        if (hasCompatibleCM) {
            isComponentCM = colorModel instanceof ComponentColorModel;
            isIndexCM = colorModel instanceof IndexColorModel;
            isPackedCM = colorModel instanceof PackedColorModel;

            numComponents = colorModel.getNumComponents();
            componentSize = colorModel.getComponentSize();
            int tempType = getType(componentSize);

            componentType = (tempType == TYPE_BIT) ?
                             DataBuffer.TYPE_BYTE : tempType;
        } else {
            isComponentCM = false;
            isIndexCM = false;
            isPackedCM = false;
            numComponents = numBands;
            componentSize = sampleSize;
            componentType = sampleType;
        }
    }

    /**
     * Determines the data type based on the data sizes in number of bits.
     * Note that for data size between 9 and 16, this method returns
     * <code>TYPE_USHORT</code>, and for size between 17 and 32, this
     * method returns <code>TYPE_INT</code>.  The minimum valid size is 1,
     * and the maximum valid size is 64.
     *
     * @param size An array containing the bit width of each band.
     * @return The minimum size data type which can hold any band.
     */
    private static int getType(int[] size) {
        int maxSize = size[0];		// maximum sample size
        for (int i = 1; i < size.length; i++) {
            maxSize = Math.max(maxSize, size[i]);
        }

        int type;
        if (maxSize < 1) {
            type = DataBuffer.TYPE_UNDEFINED;
        } else if (maxSize == 1) {
            type = TYPE_BIT;
        } else if (maxSize <= 8) {
            type = DataBuffer.TYPE_BYTE;
        } else if (maxSize <= 16) {
            type = DataBuffer.TYPE_USHORT;
        } else if (maxSize <= 32) {
            type = DataBuffer.TYPE_INT;
        } else if (maxSize <= 64) {
            type = DataBuffer.TYPE_DOUBLE;
        } else {
            type = DataBuffer.TYPE_UNDEFINED;
        }
        return type;
    }

    /**
     * Determines the pixel type based on the <code>SampleModel</code>.
     * The pixel type signifies the data type for a <code>PixelAccessor</code>.
     * For <code>ComponentSampleModel</code>, the pixel type is the same
     * as the type of the <code>DataBuffer</code> used to store the pixel
     * data.  For all other types of <code>SampleModel</code>, the pixel
     * type is determined based on the sample sizes.
     *
     * @param sm The <code>SampleModel</code> of the image.
     * @return The pixel type for this sample model.
     */
    public static int getPixelType(SampleModel sm) {
        return sm instanceof ComponentSampleModel ?
               sm.getDataType() : getType(sm.getSampleSize());
    }

    /**
     * Returns the largest data type of all the sources.  This method
     * may be used to determine the pixel sample type of a destination
     * in the default situation.  It guarantees that the destination can
     * store the resulting pixel values without losing any precision.
     * The pixel type signifies the data type for a <code>PixelAccessor</code>.
     *
     * <p> If all the sources are single-bit images, this method returns
     * <code>TYPE_BIT</code> (defined in this class) so that the
     * destination does not use unnecessary memory for some operations.
     * This includes all images whose <code>SampleModel</code> is
     * single-banded and whose sample size is 1, regardless of the type
     * of <code>ColorModel</code> the image may have.
     * If an operation does not wish to deal with packed data, it
     * should use <code>TYPE_BYTE</code> for pixel computation.
     *
     * <p> If there is no object in the source <code>Vector</code>, this
     * method returns <code>TYPE_UNDEFINED</code>.  All the objects in
     * the source <code>Vector</code> must be <code>RenderedImage</code>s.
     *
     * <p> When determining the result, only information from each image's
     * <code>SampleModel</code> is used.  No consideration is given to the
     * image's <code>ColorModel</code>.
     *
     * @param sources A <code>Vector</code> of <code>RenderedImage</code>
     *                sources.
     * @return The largest data type which can accomodate all sources.
     * @throws IllegalArgumentException  If <code>sources</code> is
     *         <code>null</code>.
     * @throws ClassCastException  If any object in <code>sources</code>
     *         is not a <code>RenderedImage</code>.
     */
    public static int getDestPixelType(Vector sources) {

        if ( sources == null ) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        int type = DataBuffer.TYPE_UNDEFINED;
        int size = sources.size();

        if (size > 0) {
            RenderedImage src = (RenderedImage)sources.get(0);
            SampleModel sm = src.getSampleModel();

            type = getPixelType(sm);

            for (int i = 1; i < size; i++) {
                src = (RenderedImage)sources.get(i);
                sm = src.getSampleModel();

                int t = getPixelType(sm);

                // Only int can handle ushort/short combination.
                type = (type == DataBuffer.TYPE_USHORT &&
                        t == DataBuffer.TYPE_SHORT) ||
                       (type == DataBuffer.TYPE_SHORT &&
                        t == DataBuffer.TYPE_USHORT) ?
                       DataBuffer.TYPE_INT : Math.max(type, t);
            }
        }
        return type;
    }

    /**
     * Returns the smallest number of bands of all the sources.
     * This method may be used to determine the number of bands a
     * destination should have in the default situation.  It guarantees
     * that every destination band has a corresponding source band.
     *
     * <p> In general, if an operation has multiple sources, and some
     * sources have 1 band and others have multiple bands, the single
     * band may be applied to the multiple bands one at a time. (An
     * example of this would be the <code>MultiplyOpImage</code>). Therefore,
     * in such a case, this method returns the smallest band count among the
     * multi-band sources.
     *
     * <p> If there is no object in the source <code>Vector</code>, this
     * method returns 0.  All the objects in the source <code>Vector</code>
     * must be <code>RenderedImage</code>s.
     *
     * <p> When determining the result, only information from each image's
     * <code>SampleModel</code> are used.  No consideration is given to the
     * image's <code>ColorModel</code>.
     *
     * @param sources A <code>Vector</code> of <code>RenderedImage</code>
     *                sources.
     * @return The minimum number of destination bands.
     * @throws IllegalArgumentException  If <code>sources</code> is
     *         <code>null</code>.
     * @throws ClassCastException  If any object in <code>sources</code>
     *         is not a <code>RenderedImage</code>.
     */
    public static int getDestNumBands(Vector sources) {

        if ( sources == null ) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        int bands = 0;
        int size = sources.size();

        if (size > 0) {
            RenderedImage src = (RenderedImage)sources.get(0);
            SampleModel sm = src.getSampleModel();

            bands = sm.getNumBands();

            for (int i = 1; i < size; i++) {
                src = (RenderedImage)sources.get(i);
                sm = src.getSampleModel();

                int b = sm.getNumBands();

                bands = bands == 1 || b == 1 ?
                        Math.max(bands, b) : Math.min(bands, b);
            }
        }
        return bands;
    }

    /**
     * Returns <code>true</code> if the destination and/or all the
     * sources are single-bit, single-band images, and their pixel
     * data may be packed into a <code>byte</code> array.
     * If so, then the operations may be done in the packed format.
     * @param srcs The array of source <code>PixelAccesor</code>s.
     * @param dst The destination <code>PixelAccesor</code>.
     * @return <code>true</code> if a packed operation is possible.
     */
    public static boolean isPackedOperation(PixelAccessor[] srcs,
                                            PixelAccessor dst) {
        boolean canBePacked = dst.isPacked;
        if (canBePacked && srcs != null) {
            for (int i = 0; i < srcs.length; i++) {
                canBePacked = canBePacked && srcs[i].isPacked;
                if (!canBePacked) {	// no need to check further
                    break;
                }
            }
        }
        return canBePacked;
    }

    /**
     * Returns <code>true</code> if the destination and the source
     * are both single-bit, single-band images, and their pixel
     * data may be packed into a <code>byte</code> array.
     * If so, then the operations may be done in the packed format.
     * @param srcs The source <code>PixelAccesor</code>.
     * @param dst The destination <code>PixelAccesor</code>.
     * @return <code>true</code> if a packed operation is possible.
     */
    public static boolean isPackedOperation(PixelAccessor src,
                                            PixelAccessor dst) {
        return src.isPacked && dst.isPacked;
    }

    /**
     * Returns <code>true</code> if the destination and both sources
     * are all single-bit, single-band images, and their pixel
     * data may be packed into a <code>byte</code> array.
     * If so, then the operations may be done in the packed format.
     * @param src1 The first source <code>PixelAccesor</code>.
     * @param src2 The second source <code>PixelAccesor</code>.
     * @param dst The destination <code>PixelAccesor</code>.
     * @return <code>true</code> if a packed operation is possible.
     */
    public static boolean isPackedOperation(PixelAccessor src1,
                                            PixelAccessor src2,
                                            PixelAccessor dst) {
        return src1.isPacked && src2.isPacked && dst.isPacked;
    }

    /**
     * Returns a region of the pixel data within a <code>Raster</code>
     * in an unpacked primitive array.  The returned data are
     * retrieved from the <code>Raster</code>'s <code>DataBuffer</code>;
     * no pixel-to-color translation is performed.
     *
     * <p> The primitive array is of the type specified by the
     * <code>type</code> argument.  It must be one of the valid data
     * types defined in <code>DataBuffer</code> and large (in bit depth)
     * enough to hold the pixel samples, or an exception will be thrown.
     * This means <code>type</code> should be greater than or equal to
     * <code>sampleType</code>.
     *
     * <p> The <code>Rectangle</code> specifies the region of interest
     * within which the pixel data are to be retrieved.  It must be
     * completely inside the <code>Raster</code>'s boundary, or else
     * this method throws an exception.
     *
     * <p> This method tries to avoid copying data as much as possible.
     * If it is unable to reformat the pixel data in the way requested,
     * or if the pixels do not have enough data to satisfy the request,
     * this method throws an exception.
     *
     * @param raster  The <code>Raster</code> that contains the pixel data.
     * @param rect  The region of interest within the <code>Raster</code>
     *        where the pixels are accessed.
     * @param type  The type of the primitive array used to return the
     *        pixel samples.
     * @param isDest  Indicates whether this <code>Raster</code> is a
     *        destination <code>Raster</code>.  That is, its pixels have
     *        not been computed.
     *
     * @return The pixel data in an <code>UnpackedImageData</code> object.
     * @throws IllegalArgumentException  If <code>type</code> is not a
     *         valid data type defined in <code>DataBuffer</code>, or
     *         is not large enough to hold the pixel samples from the
     *         specified <code>Raster</code>.
     * @throws IllegalArgumentException  If <code>rect</code> is not
     *         contained by the bounds of the specified <code>Raster</code>.
     */
    public UnpackedImageData getPixels(Raster raster,
                                       Rectangle rect,
                                       int type,
                                       boolean isDest) {
        if (!raster.getBounds().contains(rect)) {
            throw new IllegalArgumentException(
                JaiI18N.getString("PixelAccessor0"));
        }

        if (type < DataBuffer.TYPE_BYTE ||
            type > DataBuffer.TYPE_DOUBLE) {	// unknown data type
            throw new IllegalArgumentException(
                JaiI18N.getString("PixelAccessor1"));
        }

        if (type < sampleType ||
            (sampleType == DataBuffer.TYPE_USHORT &&
             type == DataBuffer.TYPE_SHORT)) {	// type not large enough
            throw new IllegalArgumentException(
                JaiI18N.getString("PixelAccessor2"));
        }

        if (isComponentSM) {
            return getPixelsCSM(raster, rect, type, isDest);

        } else {
            // The total number of data elements needed.
            int size = rect.width * rect.height * numBands;

            Object data = null;

            switch (type) {
            case DataBuffer.TYPE_BYTE:
                byte[] bd;

                if (isDest) {
                    bd = new byte[size];
                } else {
                    if (isMultiPixelPackedSM &&
                        transferType == DataBuffer.TYPE_BYTE) {
                        bd = (byte[])raster.getDataElements(
                                            rect.x, rect.y,
                                            rect.width, rect.height, null);
                    } else {
                        bd = new byte[size];
                        int[] d = raster.getPixels(rect.x, rect.y, rect.width,
                                                   rect.height, (int[])null);
                        for (int i = 0; i < size; i++) {
                            bd[i] = (byte)(d[i] & 0xff);
                        }
                    }
                }

                data = repeatBand(bd, numBands);
                break;

            case DataBuffer.TYPE_USHORT:
                short[] usd;

                if (isDest) {
                    usd = new short[size];
                } else {
                    if (isMultiPixelPackedSM &&
                        transferType == DataBuffer.TYPE_USHORT) {
                        usd = (short[])raster.getDataElements(
                                              rect.x, rect.y,
                                              rect.width, rect.height, null);
                    } else {
                        usd = new short[size];
                        int[] d = raster.getPixels(rect.x, rect.y, rect.width,
                                                   rect.height, (int[])null);
                        for (int i = 0; i < size; i++) {
                            usd[i] = (short)(d[i] & 0xffff);
                        }
                    }
                }

                data = repeatBand(usd, numBands);
                break;

            case DataBuffer.TYPE_SHORT:
                short[] sd = new short[size];

                if (!isDest) {
                    int[] d = raster.getPixels(rect.x, rect.y, rect.width,
                                               rect.height, (int[])null);
                    for (int i = 0; i < size; i++) {
                        sd[i] = (short)d[i];
                    }
                }

                data = repeatBand(sd, numBands);
                break;

            case DataBuffer.TYPE_INT:
                return getPixelsInt(raster, rect, isDest);

            case DataBuffer.TYPE_FLOAT:
                return getPixelsFloat(raster, rect, isDest);

            case DataBuffer.TYPE_DOUBLE:
                return getPixelsDouble(raster, rect, isDest);
            }

            return new UnpackedImageData(
                       raster, rect,
                       type, data,
                       numBands, numBands * rect.width,
                       getInterleavedOffsets(numBands),
                       isDest & (raster instanceof WritableRaster));
        }
    }

    /**
     * Returns the pixel data in a pixel-interleaved, unpacked array
     * where the <code>Raster</code> has a <code>ComponentSampleModel</code>.
     * @return The pixel data in an <code>UnpackedImageData</code> object.
     */
    private UnpackedImageData getPixelsCSM(Raster raster,
                                           Rectangle rect,
                                           int type,
                                           boolean isDest) {
        Object data = null;
        int pixelStride, lineStride;
        int[] offsets;
        boolean set;

        //ComponentSampleModel sm = (ComponentSampleModel)sampleModel;
        // For bug 4696966: when the raster bounds is not coincide with a
        // tile bounds.
        ComponentSampleModel sm = (ComponentSampleModel)raster.getSampleModel();

        if (type == sampleType) {
            // Data are stored in the requested array type; no need to copy.

            DataBuffer db = raster.getDataBuffer();
            int[] bankIndices = sm.getBankIndices();

            switch (sampleType) {
            case DataBuffer.TYPE_BYTE:
                byte[][] bbd = ((DataBufferByte)db).getBankData();
                byte[][] bd = new byte[numBands][];

                for (int b = 0; b < numBands; b++) {
                    bd[b] = bbd[bankIndices[b]];
                }
                data = bd;
                break;

            case DataBuffer.TYPE_USHORT:
            case DataBuffer.TYPE_SHORT:
                short[][] sbd = sampleType == DataBuffer.TYPE_USHORT ?
                                ((DataBufferUShort)db).getBankData() :
                                ((DataBufferShort)db).getBankData();
                short[][] sd = new short[numBands][];

                for (int b = 0; b < numBands; b++) {
                    sd[b] = sbd[bankIndices[b]];
                }
                data = sd;
                break;

            case DataBuffer.TYPE_INT:
                int[][] ibd = ((DataBufferInt)db).getBankData();
                int[][] id = new int[numBands][];

                for (int b = 0; b < numBands; b++) {
                    id[b] = ibd[bankIndices[b]];
                }
                data = id;
                break;

            case DataBuffer.TYPE_FLOAT:
                float[][] fbd = DataBufferUtils.getBankDataFloat(db);
                float[][] fd = new float[numBands][];

                for (int b = 0; b < numBands; b++) {
                    fd[b] = fbd[bankIndices[b]];
                }
                data = fd;
                break;

            case DataBuffer.TYPE_DOUBLE:
                double[][] dbd = DataBufferUtils.getBankDataDouble(db);
                double[][] dd = new double[numBands][];

                for (int b = 0; b < numBands; b++) {
                    dd[b] = dbd[bankIndices[b]];
                }
                data = dd;
                break;
            }

            pixelStride = sm.getPixelStride();
            lineStride = sm.getScanlineStride();

            // Determine offsets.
            int[] dbOffsets = db.getOffsets();	// DataBuffer offsets
            int x = rect.x - raster.getSampleModelTranslateX();
            int y = rect.y - raster.getSampleModelTranslateY();

            offsets = new int[numBands];
            for (int b = 0; b < numBands; b++) {
                offsets[b] = sm.getOffset(x, y, b) + dbOffsets[bankIndices[b]];
            }

            set = false;			// no need to copy

        } else {	// need to reformat data
            switch (type) {
            case DataBuffer.TYPE_INT:
                return getPixelsInt(raster, rect, isDest);

            case DataBuffer.TYPE_FLOAT:
                return getPixelsFloat(raster, rect, isDest);

            case DataBuffer.TYPE_DOUBLE:
                return getPixelsDouble(raster, rect, isDest);

            /*
             * Since the requested type must be greater than or equal to
             * sampleType, if type is byte, sampleType must also be byte,
             * because the smallest sampleType of ComponentSampleModel is
             * byte.  This case falls into the above uncopied case.
             *
             * If the Raster is a destination, then the pixel data have
             * not been computed and stored in the buffer yet.
             * Just create a new array, but no need to copy anything.
             */
            default:		// byte to ushort or short
                // The total number of data elements needed.
                int size = rect.width * rect.height * numBands;

                short[] sd = new short[size];

                if (!isDest) {			// need to copy byte data
                    // Only byte is smaller than short or ushort.
                    UnpackedImageData uid = getPixelsCSM(raster, rect,
                                                         sampleType, isDest);
                    byte[][] bdata = uid.getByteData();

                    for (int b = 0; b < numBands; b++) {
                        byte[] bd = bdata[b];		// band data
                        int lo = uid.getOffset(b);	// line offset

                        for (int i = b, h = 0; h < rect.height; h++) {
                            int po = lo;		// pixel offset
                            lo += uid.lineStride;

                            for (int w = 0; w < rect.width; w++) {
                                sd[i] = (short)(bd[po] & 0xff);

                                po += uid.pixelStride;
                                i += numBands;
                            }
                        }
                    }
                }

                data = repeatBand(sd, numBands);
                break;
            }

            pixelStride = numBands;
            lineStride = pixelStride * rect.width;
            offsets = getInterleavedOffsets(numBands);
            set = isDest & (raster instanceof WritableRaster);
        }

        return new UnpackedImageData(raster, rect,
                                     type, data,
                                     pixelStride, lineStride, offsets,
                                     set);
    }

    /**
     * Returns the pixel data in an pixel-interleaved, unpacked,
     * integer array.
     * @return The pixel data in an <code>UnpackedImageData</code> object.
     */
    private UnpackedImageData getPixelsInt(Raster raster,
                                           Rectangle rect,
                                           boolean isDest) {
        // The total number of data elements needed.
        int size = rect.width * rect.height * numBands;

        /*
         * If the Raster is destination, then the pixel data have
         * not been computed and stored in the buffer yet.
         * Just create a new array, but no need to copy anything.
         * Otherwise, copy the data from the Raster.
         */
        int[] d = isDest ? new int[size] :
                  raster.getPixels(rect.x, rect.y, rect.width,
                                   rect.height, (int[])null);

        return new UnpackedImageData(
                   raster, rect,
                   DataBuffer.TYPE_INT, repeatBand(d, numBands),
                   numBands, numBands * rect.width,
                   getInterleavedOffsets(numBands),
                   isDest & (raster instanceof WritableRaster));
    }

    /**
     * Returns the pixel data in an pixel-interleaved, unpacked,
     * float array.
     * @return The pixel data in an <code>UnpackedImageData</code> object.
     */
    private UnpackedImageData getPixelsFloat(Raster raster,
                                             Rectangle rect,
                                             boolean isDest) {
        // The total number of data elements needed.
        int size = rect.width * rect.height * numBands;

        /*
         * If the Raster is destination, then the pixel data have
         * not been computed and stored in the buffer yet.
         * Just create a new array, but no need to copy anything.
         * Otherwise, copy the data from the Raster.
         */
        float[] d = isDest ? new float[size] :
                    raster.getPixels(rect.x, rect.y, rect.width,
                                     rect.height, (float[])null);

        return new UnpackedImageData(
                   raster, rect,
                   DataBuffer.TYPE_FLOAT, repeatBand(d, numBands),
                   numBands, numBands * rect.width,
                   getInterleavedOffsets(numBands),
                   isDest & (raster instanceof WritableRaster));
    }

    /**
     * Returns the pixel data in an pixel-interleaved, unpacked,
     * double array.
     * @return The pixel data in an <code>UnpackedImageData</code> object.
     */
    private UnpackedImageData getPixelsDouble(Raster raster,
                                              Rectangle rect,
                                              boolean isDest) {
        // The total number of data elements needed.
        int size = rect.width * rect.height * numBands;

        /*
         * If the Raster is destination, then the pixel data have
         * not been computed and stored in the buffer yet.
         * Just create a new array, but no need to copy anything.
         * Otherwise, copy the data from the Raster.
         */
        double[] d = isDest ? new double[size] :
                     raster.getPixels(rect.x, rect.y, rect.width,
                                      rect.height, (double[])null);

        return new UnpackedImageData(
                   raster, rect,
                   DataBuffer.TYPE_DOUBLE, repeatBand(d, numBands),
                   numBands, numBands * rect.width,
                   getInterleavedOffsets(numBands),
                   isDest & (raster instanceof WritableRaster));
    }

    /** Repeats a one-dimensional array into a two-dimensional array. */
    private byte[][] repeatBand(byte[] d, int numBands) {
        byte[][] data = new byte[numBands][];
        for (int i = 0; i < numBands; i++) {
            data[i] = d;
        }
        return data;
    }

    private short[][] repeatBand(short[] d, int numBands) {
        short[][] data = new short[numBands][];
        for (int i = 0; i < numBands; i++) {
            data[i] = d;
        }
        return data;
    }

    private int[][] repeatBand(int[] d, int numBands) {
        int[][] data = new int[numBands][];
        for (int i = 0; i < numBands; i++) {
            data[i] = d;
        }
        return data;
    }

    private float[][] repeatBand(float[] d, int numBands) {
        float[][] data = new float[numBands][];
        for (int i = 0; i < numBands; i++) {
            data[i] = d;
        }
        return data;
    }

    private double[][] repeatBand(double[] d, int numBands) {
        double[][] data = new double[numBands][];
        for (int i = 0; i < numBands; i++) {
            data[i] = d;
        }
        return data;
    }

    /** Returns pixel interleaved offsets for copy case. */
    private int[] getInterleavedOffsets(int numBands) {
        int[] offsets = new int[numBands];
        for (int i = 0; i < numBands; i++) {
            offsets[i] = i;
        }
        return offsets;
    }

    /**
     * Sets a region of the pixel data within a <code>Raster</code>
     * using a primitive array.  This method copies data only if
     * the <code>set</code> flag in <code>UnpackedImageData</code> is
     * <code>true</code>.  Performs clamping by default.
     *
     * <p> The <code>UnpackedImageData</code> should be obtained by
     * calling the <code>getPixels()</code> method.
     *
     * @param uid The <code>UnpackedImageData</code> object to set.
     * @throws IllegalArgumentException  If the <code>uid</code> is
     *         <code>null</code>.
     */
    public void setPixels(UnpackedImageData uid) {

        if ( uid == null ) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        setPixels(uid, true);
    }

    /**
     * Sets a region of the pixel data within a <code>Raster</code>
     * using a primitive array.  This method only copies data only if
     * the <code>set</code> flag in <code>UnpackedImageData</code> is
     * <code>true</code>.
     *
     * <p> The <code>UnpackedImageData</code> should be obtained by
     * calling the <code>getPixels()</code> method.
     *
     * @param uid The <code>UnpackedImageData</code> object to set.
     * @param clamp A <code>boolean</code> set to true if clamping
     *              is to be performed.
     * @throws IllegalArgumentException  If the <code>uid</code> is
     *         <code>null</code>.
     */
    public void setPixels(UnpackedImageData uid, boolean clamp) {

        if ( uid == null ) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        if (!uid.convertToDest) {
            return;
        }

        if (clamp) {	// clamp all array elements
            switch (sampleType) {
            case DataBuffer.TYPE_BYTE:
                clampByte(uid.data, uid.type);
                break;
            case DataBuffer.TYPE_USHORT:
                clampUShort(uid.data, uid.type);
                break;
            case DataBuffer.TYPE_SHORT:
                clampShort(uid.data, uid.type);
                break;
            case DataBuffer.TYPE_INT:
                clampInt(uid.data, uid.type);
                break;
            case DataBuffer.TYPE_FLOAT:
                clampFloat(uid.data, uid.type);
                break;
            }
        }

        WritableRaster raster = (WritableRaster)uid.raster;
        Rectangle rect = uid.rect;
        int type = uid.type;

        switch (type) {
        case DataBuffer.TYPE_BYTE:
            byte[] bd = uid.getByteData(0);

            if (isMultiPixelPackedSM &&
                transferType == DataBuffer.TYPE_BYTE) {
                raster.setDataElements(rect.x, rect.y,
                                       rect.width, rect.height, bd);
            } else {
                int size = bd.length;
                int[] d = new int[size];
                for (int i = 0; i < size; i++) {
                    d[i] = bd[i] & 0xff;
                }
                raster.setPixels(rect.x, rect.y, rect.width, rect.height, d);
            }
            break;

        case DataBuffer.TYPE_USHORT:
        case DataBuffer.TYPE_SHORT:
            short[] sd = uid.getShortData(0);

            if (isComponentSM) {
                // The only time this needs to set to is a byte buffer.
                UnpackedImageData buid = getPixelsCSM(raster, rect,
                                                      DataBuffer.TYPE_BYTE,
                                                      true);
                byte[][] bdata = buid.getByteData();

                for (int b = 0; b < numBands; b++) {
                    byte[] d = bdata[b];
                    int lo = buid.getOffset(b);

                    for (int i = b, h = 0; h < rect.height; h++) {
                        int po = lo;
                        lo += buid.lineStride;

                        for (int w = 0; w < rect.width; w++) {
                            d[po] = (byte)sd[i];

                            po += buid.pixelStride;
                            i += numBands;
                        }
                    }
                }
            } else if (isMultiPixelPackedSM &&
                       transferType == DataBuffer.TYPE_USHORT) {
                raster.setDataElements(rect.x, rect.y,
                                       rect.width, rect.height, sd);
            } else {
                int size = sd.length;
                int[] d = new int[size];
                if (type == DataBuffer.TYPE_USHORT) {
                    for (int i = 0; i < size; i++) {
                        d[i] = sd[i] & 0xffff;
                    }
                } else {
                    for (int i = 0; i < size; i++) {
                        d[i] = sd[i];
                    }
                }
                raster.setPixels(rect.x, rect.y, rect.width, rect.height, d);
            }
            break;

        case DataBuffer.TYPE_INT:
            raster.setPixels(rect.x, rect.y, rect.width, rect.height,
                             uid.getIntData(0));
            break;

        case DataBuffer.TYPE_FLOAT:
            raster.setPixels(rect.x, rect.y, rect.width, rect.height,
                             uid.getFloatData(0));
            break;

        case DataBuffer.TYPE_DOUBLE:
            raster.setPixels(rect.x, rect.y, rect.width, rect.height,
                             uid.getDoubleData(0));
            break;
        }
    }

    /** Clamps the data array. */
    private void clampByte(Object data, int type) {
        int bands, size;
        switch (type) {
        case DataBuffer.TYPE_USHORT:
            short[][] usd = (short[][])data;
            bands = usd.length;

            for (int j = 0; j < bands; j++) {
                short[] d = usd[j];
                size = d.length;

                for (int i = 0; i < size; i++) {
                    int n = d[i] & 0xffff;
                    d[i] = (short)(n > 0xff ? 0xff : n);
                }
            }
            break;

        case DataBuffer.TYPE_SHORT:
            short[][] sd = (short[][])data;
            bands = sd.length;

            for (int j = 0; j < bands; j++) {
                short[] d = sd[j];
                size = d.length;

                for (int i = 0; i < size; i++) {
                    int n = d[i];
                    d[i] = (short)(n > 0xff ? 0xff : (n < 0 ? 0 : n));
                }
            }
            break;

        case DataBuffer.TYPE_INT:
            int[][] id = (int[][])data;
            bands = id.length;

            for (int j = 0; j < bands; j++) {
                int[] d = id[j];
                size = d.length;

                for (int i = 0; i < size; i++) {
                    int n = d[i];
                    d[i] = n > 0xff ? 0xff : (n < 0 ? 0 : n);
                }
            }
            break;

        case DataBuffer.TYPE_FLOAT:
            float[][] fd = (float[][])data;
            bands = fd.length;

            for (int j = 0; j < bands; j++) {
                float[] d = fd[j];
                size = d.length;

                for (int i = 0; i < size; i++) {
                    float n = d[i];
                    d[i] = n > 0xff ? 0xff : (n < 0 ? 0 : n);
                }
            }
            break;

        case DataBuffer.TYPE_DOUBLE:
            double[][] dd = (double[][])data;
            bands = dd.length;

            for (int j = 0; j < bands; j++) {
                double[] d = dd[j];
                size = d.length;

                for (int i = 0; i < size; i++) {
                    double n = d[i];
                    d[i] = n > 0xff ? 0xff : (n < 0 ? 0 : n);
                }
            }
            break;
        }
    }

    private void clampUShort(Object data, int type) {
        int bands, size;
        switch (type) {
        case DataBuffer.TYPE_INT:
            int[][] id = (int[][])data;
            bands = id.length;

            for (int j = 0; j < bands; j++) {
                int[]d = id[j];
                size = d.length;

                for (int i = 0; i < size; i++) {
                    int n = d[i];
                    d[i] = n > 0xffff ? 0xffff : (n < 0 ? 0 : n);
                }
            }
            break;

        case DataBuffer.TYPE_FLOAT:
            float[][] fd = (float[][])data;
            bands = fd.length;

            for (int j = 0; j < bands; j++) {
                float[] d = fd[j];
                size = d.length;

                for (int i = 0; i < size; i++) {
                    float n = d[i];
                    d[i] = n > 0xffff ? 0xffff : (n < 0 ? 0 : n);
                }
            }
            break;

        case DataBuffer.TYPE_DOUBLE:
            double[][] dd = (double[][])data;
            bands = dd.length;

            for (int j = 0; j < bands; j++) {
                double[] d = dd[j];
                size = d.length;

                for (int i = 0; i < size; i++) {
                    double n = d[i];
                    d[i] = n > 0xffff ? 0xffff : (n < 0 ? 0 : n);
                }
            }
            break;
        }
    }

    private void clampShort(Object data, int type) {
        int bands, size;
        switch (type) {
        case DataBuffer.TYPE_INT:
            int[][] id = (int[][])data;
            bands = id.length;

            for (int j = 0; j < bands; j++) {
                int[] d = id[j];
                size = d.length;

                for (int i = 0; i < size; i++) {
                    int n = d[i];
                    d[i] = n > Short.MAX_VALUE ? Short.MAX_VALUE :
                           (n < Short.MIN_VALUE ? Short.MIN_VALUE : n);
                }
            }
            break;

        case DataBuffer.TYPE_FLOAT:
            float[][] fd = (float[][])data;
            bands = fd.length;

            for (int j = 0; j < bands; j++) {
                float[] d = fd[j];
                size = d.length;

                for (int i = 0; i < size; i++) {
                    float n = d[i];
                    d[i] = n > Short.MAX_VALUE ? Short.MAX_VALUE :
                           (n < Short.MIN_VALUE ? Short.MIN_VALUE : n);
                }
            }
            break;

        case DataBuffer.TYPE_DOUBLE:
            double[][] dd = (double[][])data;
            bands = dd.length;

            for (int j = 0; j < bands; j++) {
                double[] d = dd[j];
                size = d.length;

                for (int i = 0; i < size; i++) {
                    double n = d[i];
                    d[i] = n > Short.MAX_VALUE ? Short.MAX_VALUE :
                           (n < Short.MIN_VALUE ? Short.MIN_VALUE : n);
                }
            }
            break;
        }
    }

    private void clampInt(Object data, int type) {
        int bands, size;
        switch (type) {
        case DataBuffer.TYPE_FLOAT:
            float[][] fd = (float[][])data;
            bands = fd.length;

            for (int j = 0; j < bands; j++) {
                float[] d = fd[j];
                size = d.length;

                for (int i = 0; i < size; i++) {
                    float n = d[i];
                    d[i] = n > Integer.MAX_VALUE ? Integer.MAX_VALUE :
                           (n < Integer.MIN_VALUE ? Integer.MIN_VALUE : n);
                }
            }
            break;

        case DataBuffer.TYPE_DOUBLE:
            double[][] dd = (double[][])data;
            bands = dd.length;

            for (int j = 0; j < bands; j++) {
                double[] d = dd[j];
                size = d.length;

                for (int i = 0; i < size; i++) {
                    double n = d[i];
                    d[i] = n > Integer.MAX_VALUE ? Integer.MAX_VALUE :
                           (n < Integer.MIN_VALUE ? Integer.MIN_VALUE : n);
                }
            }
            break;
        }
    }

    private void clampFloat(Object data, int type) {
        int bands, size;
        switch (type) {
        case DataBuffer.TYPE_DOUBLE:
            double[][] dd = (double[][])data;
            bands = dd.length;

            for (int j = 0; j < bands; j++) {
                double[] d = dd[j];
                size = d.length;

                for (int i = 0; i < size; i++) {
                    double n = d[i];
                    d[i] = n > Float.MAX_VALUE ? Float.MAX_VALUE :
                           (n < -Float.MAX_VALUE ? -Float.MAX_VALUE : n);
                }
            }
            break;
        }
    }

    /**
     * Returns a region of the pixel data within a <code>Raster</code>
     * in a packed <code>byte</code> array.  The returned data are
     * retrieved from the <code>Raster</code>'s <code>DataBuffer</code>;
     * no pixel-to-color translation is performed.
     *
     * <p> This method only returns a valid result when the pixels are
     * single-band and single-bit.  All other types of data result in
     * an exception.  The data are packed in such a format that eight
     * pixels are packed into one byte, and the end of each scanline is
     * padded with zeros to the end of the byte.
     *
     * <p> In general, this method is called when operations are to be
     * performed on the bit data in a packed format directly, to save
     * memory usage.  The static method <code>isPackedOperation</code>
     * should be used to determine whether the destination and/or its sources
     * are suitable for performing operations to a packed array.
     *
     * <p> The <code>Rectangle</code> specifies the region of interest
     * within which the pixel data are to be retrieved.  It must be
     * completely inside the <code>Raster</code>'s boundary, or
     * this method will throw an exception.
     *
     * @param raster  The <code>Raster</code> that contains the pixel data.
     * @param rect  The region of interest within the <code>Raster</code>
     *        where the pixels are accessed.
     * @param isDest  Indicates whether this <code>Raster</code> is a
     *        destination <code>Raster</code>.  That is, its pixels have
     *        not been computed.
     * @param coerceZeroOffset If <code>true</code> the returned
     *        <code>PackedImageData</code> will be forced to have a
     *        <code>bitOffset</code> and <code>offset</code> of zero
     *        and a <code>lineStride</code> of <code>(rect.width+7)/8</code>.
     *        The <code>coercedZeroOffset</code> field of the returned
     *        <code>PackedImageData</code> will be set to <code>true</code>.
     * @return The <code>PackedImageData</code> with its data filled in.
     *
     * @throws IllegalArgumentException  If data described by the
     *         <code>Raster</code>'s <code>SampleModel</code> are not
     *         single-band and single-bit.
     * @throws IllegalArgumentException  If <code>rect</code> is not
     *         within the bounds of the specified <code>Raster</code>.
     */
    public PackedImageData getPackedPixels(Raster raster,
                                           Rectangle rect,
                                           boolean isDest,
					   boolean coerceZeroOffset) {
        if (!isPacked) {
            throw new IllegalArgumentException(
                JaiI18N.getString("PixelAccessor3"));
        }

        if (!raster.getBounds().contains(rect)) {
            throw new IllegalArgumentException(
                JaiI18N.getString("PixelAccessor0"));
        }

        byte[] data;		// packed pixels
        int lineStride, offset, bitOffset;	// access information
        boolean set;		 // true if need to and can set data

        if (isMultiPixelPackedSM) {

            set = isDest;

            if(coerceZeroOffset) {

                data = ImageUtil.getPackedBinaryData(raster, rect);
                lineStride = (rect.width + 7)/8;
                offset = bitOffset = 0;

            } else {

                MultiPixelPackedSampleModel sm =
                    (MultiPixelPackedSampleModel)sampleModel;

                DataBuffer db = raster.getDataBuffer();
                int dbOffset = db.getOffset();

                int x = rect.x - raster.getSampleModelTranslateX();
                int y = rect.y - raster.getSampleModelTranslateY();

                int smLineStride = sm.getScanlineStride();
                int minOffset = sm.getOffset(x, y) + dbOffset;
                int maxOffset = sm.getOffset(x + rect.width - 1, y) + dbOffset;
                int numElements = maxOffset - minOffset + 1;	// per line
                int smBitOffset = sm.getBitOffset(x);

                switch (bufferType) {	// DataBuffer type
                case DataBuffer.TYPE_BYTE:	// no need to copy
                    data = ((DataBufferByte)db).getData();
                    lineStride = smLineStride;
                    offset = minOffset;
                    bitOffset = smBitOffset;
                    set = false;	     // no need to set for destination
                    break;

                    // Copy even if it's destination so they can easily
                    // be set back.
                case DataBuffer.TYPE_USHORT:
                    lineStride = numElements * 2;   // 2 bytes for each ushort
                    offset = smBitOffset / 8;
                    bitOffset = smBitOffset % 8;
                    data = new byte[lineStride * rect.height];

                    short[] sd = ((DataBufferUShort)db).getData();
                    for (int i = 0, h = 0; h < rect.height; h++) {
                        for (int w = minOffset; w <= maxOffset; w++) {
                            short d = sd[w];
                            data[i++] = (byte)((d >>> 8) & 0xff);
                            data[i++] = (byte)(d & 0xff);
                        }
                        minOffset += smLineStride;
                        maxOffset += smLineStride;
                    }
                    break;

                case DataBuffer.TYPE_INT:
                    lineStride = numElements * 4;       // 4 bytes for each int
                    offset = smBitOffset / 8;
                    bitOffset = smBitOffset % 8;
                    data = new byte[lineStride * rect.height];

                    int[] id = ((DataBufferInt)db).getData();
                    for (int i = 0, h = 0; h < rect.height; h++) {
                        for (int w = minOffset; w <= maxOffset; w++) {
                            int d = id[w];
                            data[i++] = (byte)((d >>> 24) & 0xff);
                            data[i++] = (byte)((d >>> 16) & 0xff);
                            data[i++] = (byte)((d >>> 8) & 0xff);
                            data[i++] = (byte)(d & 0xff);
                        }
                        minOffset += smLineStride;
                        maxOffset += smLineStride;
                    }
                    break;

                default:
                    throw new RuntimeException();    // should never get here
                }
            }

        } else {		// unknown SampleModel
            lineStride = (rect.width + 7) / 8;
            offset = 0;
            bitOffset = 0;
            set = isDest & (raster instanceof WritableRaster);
            data = new byte[lineStride * rect.height];

            if (!isDest) {			// copy one line at a time to
                int size = lineStride * 8;	// avoid using too much memory
                int[] p = new int[size];
                for (int i = 0, h = 0; h < rect.height; h++) {
                    p = raster.getPixels(rect.x, rect.y + h,
                                         rect.width, 1, p);
                    for (int w = 0; w < size; w += 8) {
                        data[i++] = (byte)(p[w] << 7 | p[w+1] << 6 |
                                           p[w+2] << 5 | p[w+3] << 4 |
                                           p[w+4] << 3 | p[w+5] << 2 |
                                           p[w+6] << 1 | p[w+7]);
                    }
                }
            }
        }

        return new PackedImageData(raster, rect,
                                   data, lineStride, offset, bitOffset,
                                   coerceZeroOffset, set);
    }

    /**
     * Sets a region of the pixel data within a <code>Raster</code>
     * using a primitive array.  This method copies data only if
     * the <code>set</code> flag in <code>PackedImageData</code> is
     * <code>true</code>.
     *
     * <p> The <code>PackedImageData</code> should be obtained by
     * calling the <code>getPackedPixels()</code> method.
     *
     * @param pid The </code>PackedImageData</code> object whose pixels
     *            are to be written.
     * @throws IllegalArgumentException  If the <code>pid</code> is
     *         <code>null</code>.
     */
    public void setPackedPixels(PackedImageData pid) {

        if ( pid == null ) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        if (!pid.convertToDest) {
            return;
        }

        Raster raster = pid.raster;
        Rectangle rect = pid.rect;
        byte[] data = pid.data;

        if (isMultiPixelPackedSM) {

            if(pid.coercedZeroOffset) {
                ImageUtil.setPackedBinaryData(data,
                                              (WritableRaster)raster,
                                              rect);
            } else {
                MultiPixelPackedSampleModel sm =
                    (MultiPixelPackedSampleModel)sampleModel;

                DataBuffer db = raster.getDataBuffer();
                int dbOffset = db.getOffset();

                int x = rect.x - raster.getSampleModelTranslateX();
                int y = rect.y - raster.getSampleModelTranslateY();

                int lineStride = sm.getScanlineStride();
                int minOffset = sm.getOffset(x, y) + dbOffset;
                int maxOffset = sm.getOffset(x + rect.width - 1, y) + dbOffset;

                // Only need to set for buffer types of ushort and int.
                switch (bufferType) {
                case DataBuffer.TYPE_USHORT:
                    short[] sd = ((DataBufferUShort)db).getData();
                    for (int i = 0, h = 0; h < rect.height; h++) {
                        for (int w = minOffset; w <= maxOffset; w++) {
                            sd[w] = (short)(data[i++] << 8 | data[i++]);
                        }
                        minOffset += lineStride;
                        maxOffset += lineStride;
                    }
                    break;

                case DataBuffer.TYPE_INT:
                    int[] id = ((DataBufferInt)db).getData();
                    for (int i = 0, h = 0; h < rect.height; h++) {
                        for (int w = minOffset; w <= maxOffset; w++) {
                            id[w] = data[i++] << 24 | data[i++] << 16 |
                                data[i++] << 8 | data[i++];
                        }
                        minOffset += lineStride;
                        maxOffset += lineStride;
                    }
                    break;
                }
            }

        } else {
            /*
             * The getPackedData() method should set "set" to false if
             * the Raster is not writable.
             * Copy one line at a time to avoid using too much memory.
             */
            WritableRaster wr = (WritableRaster)raster;
            int size = pid.lineStride * 8;
            int[] p = new int[size];

            for (int i = 0, h = 0; h < rect.height; h++) {
                for (int w = 0; w < size; w += 8) {
                    p[w] = (data[i] >>> 7) & 0x1;
                    p[w+1] = (data[i] >>> 6) & 0x1;
                    p[w+2] = (data[i] >>> 5) & 0x1;
                    p[w+3] = (data[i] >>> 4) & 0x1;
                    p[w+4] = (data[i] >>> 3) & 0x1;
                    p[w+5] = (data[i] >>> 2) & 0x1;
                    p[w+6] = (data[i] >>> 1) & 0x1;
                    p[w+7] = data[i] & 0x1;
                    i++;
                }
                wr.setPixels(rect.x, rect.y + h, rect.width, 1, p);
            }
        }
    }

    /**
     * Returns an array of unnormalized color/alpha components in the
     * <code>ColorSpace</code> defined in the image's
     * <code>ColorModel</code>.  This method retrieves the pixel data
     * within the specified rectangular region from the
     * <code>Raster</code>, performs the pixel-to-color translation based
     * on the image's <code>ColorModel</code>, and returns the components
     * in the order specified by the <code>ColorSpace</code>.
     *
     * <p> In order for this method to return a valid result, the
     * image must have a valid <code>ColorModel</code> that is compatible
     * with the image's <code>SampleModel</code>.  Further, the
     * <code>SampleModel</code> and <code>ColorModel</code> must have
     * the same <code>transferType</code>.
     *
     * <p> The component data are stored in a primitive array of the
     * type specified by the <code>type</code> argument.  It must be one
     * of the valid data types defined in <code>DataBuffer</code> and
     * large (in bit depth) enough to hold the color/alpha components,
     * or an exception is thrown.  This means <code>type</code> should
     * be greater than or equal to <code>componentType</code>.  To avoid
     * extra array copy, it is best to use
     * <code>DataBuffer.TYPE_INT</code> for this argument.
     *
     * <p> The <code>Rectangle</code> specifies the region of interest
     * within which the pixel data are to be retrieved.  It must be
     * completely inside the <code>Raster</code>'s boundary, or else
     * this method throws an exception.
     *
     * @param raster  The <code>Raster</code> that contains the pixel data.
     * @param rect  The region of interest within the <code>Raster</code>
     *        where the pixels are accessed.
     * @param type  The type of the primitive array used to return the
     *        color/alpha components with.
     * @return The <code>UnpackedImageData</code> with its data filled in.
     *
     * @throws IllegalArgumentException  If the image does not have a valid
     *         <code>ColorModel</code> that is compatible with its
     *         <code>SampleModel</code>.
     * @throws IllegalArgumentException  If <code>type</code> is not a
     *         valid data type defined in <code>DataBuffer</code>, or
     *         is not large enough to hold the translated color/alpha
     *         components.
     * @throws IllegalArgumentException  If <code>rect</code> is not
     *         contained by the bounds of the specified <code>Raster</code>.
     */
    public UnpackedImageData getComponents(Raster raster,
                                           Rectangle rect,
                                           int type) {
        if (!hasCompatibleCM) {
            throw new IllegalArgumentException(
                JaiI18N.getString("PixelAccessor5"));
        }

        if (!raster.getBounds().contains(rect)) {
            throw new IllegalArgumentException(
                JaiI18N.getString("PixelAccessor0"));
        }

        if (type < DataBuffer.TYPE_BYTE ||
            type > DataBuffer.TYPE_DOUBLE) {	// unknown data type
            throw new IllegalArgumentException(
                JaiI18N.getString("PixelAccessor1"));
        }

        if (type < componentType ||
            (componentType == DataBuffer.TYPE_USHORT &&
             type == DataBuffer.TYPE_SHORT)) {	// type not large enough
            throw new IllegalArgumentException(
                JaiI18N.getString("PixelAccessor4"));
        }

        // Get color/alpha components in an integer array.
        int size = rect.width * rect.height * numComponents;
        int[] ic = new int[size];
        int width = rect.x + rect.width;
        int height = rect.y + rect.height;

        for (int i = 0, y = rect.y; y < height; y++) {
            for (int x = rect.x; x < width; x++) {
                Object p = raster.getDataElements(x, y, null);
                colorModel.getComponents(p, ic, i);
                i += numComponents;
            }
        }

        // Reformat components into the specified data type.
        Object data = null;
        switch (type) {
        case DataBuffer.TYPE_BYTE:
            byte[] bc = new byte[size];
            for (int i = 0; i < size; i++) {
                bc[i] = (byte)(ic[i] & 0xff);
            }
            data = repeatBand(bc, numComponents);
            break;

        case DataBuffer.TYPE_USHORT:
            short[] usc = new short[size];
            for (int i = 0; i < size; i++) {
                usc[i] = (short)(ic[i] & 0xffff);
            }
            data = repeatBand(usc, numComponents);
            break;

        case DataBuffer.TYPE_SHORT:
            short[] sc = new short[size];
            for (int i = 0; i < size; i++) {
                sc[i] = (short)ic[i];
            }
            data = repeatBand(sc, numComponents);
            break;

        case DataBuffer.TYPE_INT:
            data = repeatBand(ic, numComponents);
            break;

        case DataBuffer.TYPE_FLOAT:
            float[] fc = new float[size];
            for (int i = 0; i < size; i++) {
                fc[i] = ic[i];
            }
            data = repeatBand(fc, numComponents);
            break;

        case DataBuffer.TYPE_DOUBLE:
            double[] dc = new double[size];
            for (int i = 0; i < size; i++) {
                dc[i] = ic[i];
            }
            data = repeatBand(dc, numComponents);
            break;
        }

        return new UnpackedImageData(
                   raster, rect,
                   type, data,
                   numComponents, numComponents * rect.width,
                   getInterleavedOffsets(numComponents),
                   raster instanceof WritableRaster);
    }

    /**
     * Given an array of unnormalized color/alpha components, this
     * method performs color-to-pixel translation, and sets the
     * translated pixel data back to the <code>Raster</code> within
     * a specific region.  It is very important that the components
     * array along with access information are obtained by calling
     * the <code>getComponents()</code> method, or errors
     * will occur.
     *
     * <p> In order for this method to return a valid result, the
     * image must have a valid <code>ColorModel</code> that is compatible
     * with the image's <code>SampleModel</code>.  Further, the
     * <code>SampleModel</code> and <code>ColorModel</code> must have
     * the same <code>transferType</code>.
     *
     * <p> This method sets data only if the <code>set</code> flag in
     * <code>UnpackedImageData</code> is <code>true</code>.
     *
     * @param uid The <code>UnpackedImageData</code> whose data is to be set.
     * @throws IllegalArgumentException  If the <code>uid</code> is
     *         <code>null</code>.
     */
    public void setComponents(UnpackedImageData uid) {

        if ( uid == null ) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        if (!uid.convertToDest) {
            return;
        }

        WritableRaster raster = (WritableRaster)uid.raster;
        Rectangle rect = uid.rect;
        int type = uid.type;

        int size = rect.width * rect.height * numComponents;
        int[] ic = null;

        switch (type) {
        case DataBuffer.TYPE_BYTE:
            byte[] bc = uid.getByteData(0);
            ic = new int[size];
            for (int i = 0; i < size; i++) {
                ic[i] = bc[i] & 0xff;
            }
            break;

        case DataBuffer.TYPE_USHORT:
            short[] usc = uid.getShortData(0);
            ic = new int[size];
            for (int i = 0; i < size; i++) {
                ic[i] = usc[i] & 0xffff;
            }
            break;

        case DataBuffer.TYPE_SHORT:
            short[] sc = uid.getShortData(0);
            ic = new int[size];
            for (int i = 0; i < size; i++) {
                ic[i] = sc[i];
            }
            break;

        case DataBuffer.TYPE_INT:
            ic = uid.getIntData(0);
            break;

        case DataBuffer.TYPE_FLOAT:
            float[] fc = uid.getFloatData(0);
            ic = new int[size];
            for (int i = 0; i < size; i++) {
                ic[i] = (int)fc[i];;
            }
            break;

        case DataBuffer.TYPE_DOUBLE:
            double[] dc = uid.getDoubleData(0);
            ic = new int[size];
            for (int i = 0; i < size; i++) {
                ic[i] = (int)dc[i];
            }
            break;
        }

        int width = rect.x + rect.width;
        int height = rect.y + rect.height;

        for (int i = 0, y = rect.y; y < height; y++) {
            for (int x = rect.x; x < width; x++) {
                Object p = colorModel.getDataElements(ic, i, null);
                raster.setDataElements(x, y, p);
                i += numComponents;
            }
        }
    }

    /**
     * Returns an array of color/alpha components scaled from 0 to 255
     * in the default sRGB <code>ColorSpace</code>.  This method
     * retrieves the pixel data within the specified rectangular region
     * from the <code>Raster</code>, performs the pixel-to-color translation
     * based on the image's <code>ColorModel</code>, and returns the
     * components in the order specified by the <code>ColorSpace</code>.
     *
     * <p> In order for this method to return a valid result, the
     * image must have a valid <code>ColorModel</code> that is compatible
     * with the image's <code>SampleModel</code>.  Further, the
     * <code>SampleModel</code> and <code>ColorModel</code> must have
     * the same <code>transferType</code>.
     *
     * <p> The component data are stored in a two-dimensional,
     * band-interleaved, <code>byte</code> array, because the components
     * are always scaled from 0 to 255.  Red is band 0, green is band 1,
     * blue is band 2, and alpha is band 3.
     *
     * <p> The <code>Rectangle</code> specifies the region of interest
     * within which the pixel data are to be retrieved.  It must be
     * completely inside the <code>Raster</code>'s boundary, or
     * this method will throw an exception.
     *
     * @param raster  The <code>Raster</code> that contains the pixel data.
     * @param rect  The region of interest within the <code>Raster</code>
     *        where the pixels are accessed.
     * @return The <code>UnpackedImageData</code> with its data filled in.
     *
     * @throws IllegalArgumentException  If the image does not have a valid
     *         <code>ColorModel</code> that is compatible with its
     *         <code>SampleModel</code>.
     * @throws IllegalArgumentException  If <code>rect</code> is not
     *         contained by the bounds of the specified <code>Raster</code>.
     */
    public UnpackedImageData getComponentsRGB(Raster raster,
                                              Rectangle rect) {
        if (!hasCompatibleCM) {
            throw new IllegalArgumentException(
                JaiI18N.getString("PixelAccessor5"));
        }

        if (!raster.getBounds().contains(rect)) {
            throw new IllegalArgumentException(
                JaiI18N.getString("PixelAccessor0"));
        }

        int size = rect.width * rect.height;

        byte[][] data = new byte[4][size];
        byte[] r = data[0];	// red
        byte[] g = data[1];	// green
        byte[] b = data[2];	// blue
        byte[] a = data[3];	// alpha

        // Get color/alpha components in an integer array.
        int maxX = rect.x + rect.width;
        int maxY = rect.y + rect.height;

        if(isIndexCM) {
            // Cast the CM and get the size of the ICM tables.
            IndexColorModel icm = (IndexColorModel)colorModel;
            int mapSize = icm.getMapSize();

            // Load the ICM tables.
            byte[] reds = new byte[mapSize];
            icm.getReds(reds);
            byte[] greens = new byte[mapSize];
            icm.getGreens(greens);
            byte[] blues = new byte[mapSize];
            icm.getBlues(blues);
            byte[] alphas = null;
            if(icm.hasAlpha()) {
                alphas = new byte[mapSize];
                icm.getAlphas(alphas);
            }

            // Get the index values.
            int[] indices = raster.getPixels(rect.x, rect.y,
                                             rect.width, rect.height,
                                             (int[])null);

            // Use the ICM tables to get the [A]RGB values.
            if(alphas == null) {
                // No alpha.
                for (int i = 0, y = rect.y; y < maxY; y++) {
                    for (int x = rect.x; x < maxX; x++) {
                        int index = indices[i];

                        r[i] = reds[index];
                        g[i] = greens[index];
                        b[i] = blues[index];

                        i++;
                    }
                }
            } else {
                // Alpha.
                for (int i = 0, y = rect.y; y < maxY; y++) {
                    for (int x = rect.x; x < maxX; x++) {
                        int index = indices[i];

                        r[i] = reds[index];
                        g[i] = greens[index];
                        b[i] = blues[index];
                        a[i] = alphas[index];

                        i++;
                    }
                }
            }
        } else {
            // XXX If ColorSpaceJAI is implemented use the
            // Raster-based methods here.
            // Not an IndexColorModel: use the "slow method".
            for (int i = 0, y = rect.y; y < maxY; y++) {
                for (int x = rect.x; x < maxX; x++) {
                    Object p = raster.getDataElements(x, y, null);

                    r[i] = (byte)colorModel.getRed(p);
                    g[i] = (byte)colorModel.getGreen(p);
                    b[i] = (byte)colorModel.getBlue(p);
                    a[i] = (byte)colorModel.getAlpha(p);
                    i++;
                }
            }
        }

        return new UnpackedImageData(
                   raster, rect,
                   DataBuffer.TYPE_BYTE, data,
                   1, rect.width,
                   new int[4],	// all entries automatically initialized to 0
                   raster instanceof WritableRaster);
    }

    /**
     * Given an array of normalized (between 0 and 255) alpha/RGB color
     * components, this method performs color-to-pixel translation, and
     * sets the translated pixel data back to the <code>Raster</code>
     * within a specific region.  It is very important that the components
     * array along with access information are obtained by calling
     * the <code>getComponentsRGB()</code> method, or errors
     * will occur.
     *
     * <p> In order for this method to return a valid result, the
     * image must have a valid <code>ColorModel</code> that is compatible
     * with the image's <code>SampleModel</code>.  Furthermore, the
     * <code>SampleModel</code> and <code>ColorModel</code> must have
     * the same <code>transferType</code>.
     *
     * <p> This method sets data only if the <code>set</code> flag in
     * <code>UnpackedImageData</code> is <code>true</code>.
     *
     * @param uid The <code>UnpackedImageData</code> to set.
     * @throws IllegalArgumentException  If the <code>uid</code> is
     *         <code>null</code>.
     */
    public void setComponentsRGB(UnpackedImageData uid) {

        if ( uid == null ) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        if (!uid.convertToDest) {
            return;
        }

        byte[][] data = uid.getByteData();
        byte[] r = data[0];	// red
        byte[] g = data[1];	// green
        byte[] b = data[2];	// blue
        byte[] a = data[3];	// alpha

        WritableRaster raster = (WritableRaster)uid.raster;
        Rectangle rect = uid.rect;

        int maxX = rect.x + rect.width;
        int maxY = rect.y + rect.height;

        for (int i = 0, y = rect.y; y < maxY; y++) {
            for (int x = rect.x; x < maxX; x++) {
                int rgb = (a[i] << 24) | (b[i] << 16) |
                          (g[i] << 8) | r[i];

                Object p = colorModel.getDataElements(rgb, null);
                raster.setDataElements(x, y, p);
                i++;
            }
        }
    }
}
