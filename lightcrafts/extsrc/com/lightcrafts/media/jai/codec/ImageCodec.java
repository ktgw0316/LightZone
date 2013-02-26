/*
 * $RCSfile: ImageCodec.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.3 $
 * $Date: 2005/12/07 00:25:26 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.codec;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.ComponentColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import com.lightcrafts.media.jai.codecimpl.BMPCodec;
import com.lightcrafts.media.jai.codecimpl.FPXCodec;
import com.lightcrafts.media.jai.codecimpl.GIFCodec;
import com.lightcrafts.media.jai.codecimpl.JPEGCodec;
import com.lightcrafts.media.jai.codecimpl.PNGCodec;
import com.lightcrafts.media.jai.codecimpl.PNMCodec;
import com.lightcrafts.media.jai.codecimpl.TIFFCodec;
import com.lightcrafts.media.jai.codecimpl.WBMPCodec;
import com.lightcrafts.media.jai.codecimpl.ImagingListenerProxy;
import com.lightcrafts.media.jai.codecimpl.util.FloatDoubleColorModel;
import com.lightcrafts.media.jai.util.SimpleCMYKColorSpace;

/**
 * An abstract class allowing the creation of image decoders and
 * encoders.  Instances of <code>ImageCodec</code> may be registered.
 * Once a codec has been registered, the format name associated with
 * it may be used as the <code>name</code> parameter in the
 * <code>createImageEncoder()</code> and <code>createImageDecoder()</code>
 * methods.
 *
 * <p> Additionally, subclasses of <code>ImageCodec</code>
 * are able to perform recognition of their particular format,
 * wither by inspection of a fixed-length file header or by
 * arbitrary access to the source data stream.
 *
 * <p> Format recognition is performed by two variants of the
 * <code>isFormatRecognized()</code> method.  Which variant should be
 * called is determined by the output of the codec's
 * <codec>getNumHeaderBytes()</code> method, which returns 0 if
 * arbitrary access to the stream is required, and otherwise returns
 * the number of header bytes required to recognize the format.
 * Each subclass of <code>ImageCodec</code> needs to implement only
 * one of the two variants.
 *
 * <p><b> This class is not a committed part of the JAI API.  It may
 * be removed or changed in future releases of JAI.</b>
 */
public abstract class ImageCodec {

    private static Hashtable codecs = new Hashtable();

    /** Allow only subclasses to instantiate this class. */
    protected ImageCodec() {}

    /**
     * Load the JPEG and PNM codecs.
     */
    static {
        registerCodec(new BMPCodec());
        registerCodec(new GIFCodec());
        registerCodec(new FPXCodec());
        registerCodec(new JPEGCodec());
        registerCodec(new PNGCodec());
        registerCodec(new PNMCodec());
        registerCodec(new TIFFCodec());
        registerCodec(new WBMPCodec());
    }

    /**
     * Returns the <code>ImageCodec</code> associated with the given
     * name.  <code>null</code> is returned if no codec is registered
     * with the given name.  Case is not significant.
     *
     * @param name The name associated with the codec.
     * @return The associated <code>ImageCodec</code>, or <code>null</code>.
     */
    public static ImageCodec getCodec(String name) {
        return (ImageCodec)codecs.get(name.toLowerCase());
    }

    /**
     * Associates an <code>ImageCodec</code> with its format name, as
     * determined by its <code>getFormatName()</code> method.  Case is
     * not significant.  Any codec previously associated with the name
     * is discarded.
     *
     * @param codec The <code>ImageCodec</code> object to be registered.
     */
    public static void registerCodec(ImageCodec codec) {
        codecs.put(codec.getFormatName().toLowerCase(), codec);
    }

    /**
     * Unregisters the <code>ImageCodec</code> object currently
     * responsible for handling the named format.  Case is not
     * significant.
     *
     * @param name The name associated with the codec to be removed.
     */
    public static void unregisterCodec(String name) {
        codecs.remove(name.toLowerCase());
    }

    /**
     * Returns an <code>Enumeration</code> of all regstered
     * <code>ImageCodec</code> objects.
     */
    public static Enumeration getCodecs() {
        return codecs.elements();
    }

    /**
     * Returns an <code>ImageEncoder</code> object suitable for
     * encoding to the supplied <code>OutputStream</code>, using the
     * supplied <code>ImageEncoderParam</code> object.
     *
     * @param name The name associated with the codec.
     * @param dst An <code>OutputStream</code> to write to.
     * @param param An instance of <code>ImageEncoderParam</code> suitable
     *        for use with the named codec, or <code>null</code>.
     * @return An instance of <code>ImageEncoder</code>, or <code>null</code>.
     */
    public static ImageEncoder createImageEncoder(String name,
                                                  OutputStream dst,
                                                  ImageEncodeParam param) {
        ImageCodec codec = getCodec(name);
        if (codec == null) {
            return null;
        }
        return codec.createImageEncoder(dst, param);
    }

    /**
     * Returns an <code>ImageDecoder</code> object suitable for
     * decoding from the supplied <code>InputStream</code>, using the
     * supplied <code>ImageDecodeParam</code> object.
     *
     * @param name The name associated with the codec.
     * @param src An <code>InputStream</code> to read from.
     * @param param An instance of <code>ImageDecodeParam</code> suitable
     *        for use with the named codec, or <code>null</code>.
     * @return An instance of <code>ImageDecoder</code>, or <code>null</code>.
     */
    public static ImageDecoder createImageDecoder(String name,
                                                  InputStream src,
                                                  ImageDecodeParam param) {
        ImageCodec codec = getCodec(name);
        if (codec == null) {
            return null;
        }
        return codec.createImageDecoder(src, param);
    }

    /**
     * Returns an <code>ImageDecoder</code> object suitable for
     * decoding from the supplied <code>File</code>, using the
     * supplied <code>ImageDecodeParam</code> object.
     *
     * @param name The name associated with the codec.
     * @param src A <code>File</code> to read from.
     * @param param An instance of <code>ImageDecodeParam</code> suitable
     *        for use with the named codec, or <code>null</code>.
     * @return An instance of <code>ImageDecoder</code>, or <code>null</code>.
     */
    public static ImageDecoder createImageDecoder(String name,
                                                  File src,
                                                  ImageDecodeParam param)
        throws IOException {
        ImageCodec codec = getCodec(name);
        if (codec == null) {
            return null;
        }
        return codec.createImageDecoder(src, param);
    }

    /**
     * Returns an <code>ImageDecoder</code> object suitable for
     * decoding from the supplied <code>SeekableStream</code>, using the
     * supplied <code>ImageDecodeParam</code> object.
     *
     * @param name The name associated with the codec.
     * @param src A <code>SeekableStream</code> to read from.
     * @param param An instance of <code>ImageDecodeParam</code> suitable
     *        for use with the named codec, or <code>null</code>.
     * @return An instance of <code>ImageDecoder</code>, or <code>null</code>.
     */
    public static ImageDecoder createImageDecoder(String name,
                                                  SeekableStream src,
                                                  ImageDecodeParam param) {
        ImageCodec codec = getCodec(name);
        if (codec == null) {
            return null;
        }
        return codec.createImageDecoder(src, param);
    }

    private static String[] vectorToStrings(Vector nameVec) {
        int count = nameVec.size();
        String[] names = new String[count];
        for (int i = 0; i < count; i++) {
            names[i] = (String)nameVec.elementAt(i);
        }
        return names;
    }

    /**
     * Returns an array of <code>String</code>s indicating the names
     * of registered <code>ImageCodec</code>s that may be appropriate
     * for reading the given <code>SeekableStream</code>.
     *
     * <p> If the <code>src</code> <code>SeekableStream</code> does
     * not support seeking backwards (that is, its
     * <code>canSeekBackwards()</code> method returns
     * <code>false</code>) then only <code>FormatRecognizer</code>s
     * that require only a fixed-length header will be checked.
     *
     * <p> If the <code>src</code> stream does not support seeking
     * backwards, it must support marking, as determined by its
     * <code>markSupported()</code> method.
     *
     * @param src A <code>SeekableStream</code> which optionally supports
     *        seeking backwards.
     * @return An array of <code>String</code>s.
     *
     * @throws IllegalArgumentException if <code>src</code> supports
     *         neither seeking backwards nor marking.
     */
    public static String[] getDecoderNames(SeekableStream src) {
        if (!src.canSeekBackwards() && !src.markSupported()) {
            throw new IllegalArgumentException(JaiI18N.getString("ImageCodec2"));
        }

        Enumeration enumeration = codecs.elements();
        Vector nameVec = new Vector();

        String opName = null;
        while (enumeration.hasMoreElements()) {
            ImageCodec codec = (ImageCodec)enumeration.nextElement();

            int bytesNeeded = codec.getNumHeaderBytes();
            if ((bytesNeeded == 0) && !src.canSeekBackwards()) {
                continue;
            }

            try {
                if (bytesNeeded > 0) {
                    src.mark(bytesNeeded);
                    byte[] header = new byte[bytesNeeded];
                    src.readFully(header);
                    src.reset();

                    if (codec.isFormatRecognized(header)) {
                        nameVec.add(codec.getFormatName());
                    }
                } else {
                    long pointer = src.getFilePointer();
                    src.seek(0L);
                    if (codec.isFormatRecognized(src)) {
                        nameVec.add(codec.getFormatName());
                    }
                    src.seek(pointer);
                }
            } catch (IOException e) {
                ImagingListenerProxy.errorOccurred(JaiI18N.getString("ImageCodec3"),
                                       e, ImageCodec.class, false);
//                e.printStackTrace();
            }
        }

        return vectorToStrings(nameVec);
    }

    /**
     * Returns an array of <code>String</code>s indicating the names
     * of registered <code>ImageCodec</code>s that may be appropriate
     * for writing the given <code>RenderedImage</code>, using the
     * optional <code>ImageEncodeParam</code>, which may be
     * <code>null</code>.
     *
     * @param im A <code>RenderedImage</code> to be encodec.
     * @param param An <code>ImageEncodeParam</code>, or null.
     * @return An array of <code>String</code>s.
     */
    public static String[] getEncoderNames(RenderedImage im,
                                           ImageEncodeParam param) {
        Enumeration enumeration = codecs.elements();
        Vector nameVec = new Vector();

        String opName = null;
        while (enumeration.hasMoreElements()) {
            ImageCodec codec = (ImageCodec)enumeration.nextElement();

            if (codec.canEncodeImage(im, param)) {
                nameVec.add(codec.getFormatName());
            }
        }

        return vectorToStrings(nameVec);
    }

    /**
     * Returns the name of this image format.
     *
     * @return A <code>String</code> containing the name of the
     *         image format supported by this codec.
     */
    public abstract String getFormatName();

    /**
     * Returns the number of bytes of header needed to recognize the
     * format, or 0 if an arbitrary number of bytes may be needed.
     * The default implementation returns 0.
     *
     * <p> The return value must be a constant for all instances of
     * each particular subclass of <code>ImageCodec</code>.
     *
     * <p> Although it is legal to always return 0, in some cases
     * processing may be more efficient if the number of bytes needed
     * is known in advance.
     */
    public int getNumHeaderBytes() {
        return 0;
    }

    /**
     * Returns <code>true</code> if the format is recognized in the
     * initial portion of a stream.  The header will be passed in as a
     * <code>byte</code> array of length <code>getNumHeaderBytes()</code>.
     * This method should be called only if <code>getNumHeaderBytes()</code>
     * returns a value greater than 0.
     *
     * <p> The default implementation throws an exception to indicate
     * that it should never be called.
     *
     * @param header An array of <code>byte</code>s containing the input
     *        stream header.
     * @return <code>true</code> if the format is recognized.
     */
    public boolean isFormatRecognized(byte[] header) {
        throw new RuntimeException(JaiI18N.getString("ImageCodec0"));
    }

    /**
     * Returns <code>true</code> if the format is recognized in the
     * input data stream.  This method should be called only if
     * <code>getNumHeaderBytesNeeded()</code> returns 0.
     *
     * <p> The source <code>SeekableStream</code> is guaranteed to
     * support seeking backwards, and should be seeked to 0 prior
     * to calling this method.
     *
     * <p> The default implementation throws an exception to indicate
     * that it should never be called.
     *
     * @param src A <code>SeekableStream</code> containing the input
     *        data.
     * @return <code>true</code> if the format is recognized.
     */
    public boolean isFormatRecognized(SeekableStream src) throws IOException {
        throw new RuntimeException(JaiI18N.getString("ImageCodec1"));
    }

    /**
     * Returns a <code>Class</code> object indicating the proper
     * subclass of <code>ImageEncodeParam</code> to be used with this
     * <code>ImageCodec</code>.  If encoding is not supported by this
     * codec, <code>null</code> is returned.  If encoding is
     * supported, but a parameter object is not used during encoding,
     * Object.class is returned to signal this fact.
     */
    protected abstract Class getEncodeParamClass();

    /**
     * Returns a <code>Class</code> object indicating the proper
     * subclass of <code>ImageDecodeParam</code> to be used with this
     * <code>ImageCodec</code>.  If encoding is not supported by this
     * codec, <code>null</code> is returned.  If decoding is
     * supported, but a parameter object is not used during decoding,
     * Object.class is returned to signal this fact.
     */
    protected abstract Class getDecodeParamClass();

    /**
     * In a concrete subclass of <code>ImageCodec</code>, returns an
     * implementation of the <code>ImageEncoder</code> interface
     * appropriate for that codec.
     *
     * @param dst An <code>OutputStream</code> to write to.
     * @param param An instance of <code>ImageEncoderParam</code>
     *        suitable for use with the <code>ImageCodec</code>
     *        subclass, or <code>null</code>.
     * @return An instance of <code>ImageEncoder</code>.
     */
    protected abstract ImageEncoder createImageEncoder(OutputStream dst,
                                                       ImageEncodeParam param);

    /**
     * Returns <code>true</code> if the given image and encoder param
     * object are suitable for encoding by this <code>ImageCodec</code>.
     * For example, some codecs may only deal with images with a certain
     * number of bands; an attempt to encode an image with an unsupported
     * number of bands will fail.
     *
     * @param im a RenderedImage whose ability to be encoded is to be
     *        determined.
     * @param param a suitable <code>ImageEncodeParam</code> object,
     *        or <code>null</code>.
     */
    public abstract boolean canEncodeImage(RenderedImage im,
                                           ImageEncodeParam param);

    /**
     * Returns an implementation of the <code>ImageDecoder</code>
     * interface appropriate for that codec.  Subclasses of
     * <code>ImageCodec</code> may override this method if they wish
     * to accept data directly from an <code>InputStream</code>;
     * otherwise, this method will convert the source into a
     * backwards-seekable <code>SeekableStream</code> and call the
     * appropriate version of <code>createImageDecoder</code> for that
     * data type.
     *
     * <p> Instances of <code>ImageCodec</code> that do not require
     * the ability to seek backwards in their source
     * <code>SeekableStream</code> should override this method in
     * order to avoid the default call to
     * <code>SeekableStream.wrapInputStream(src, true)</code>.
     *
     * @param dst An <code>InputStream</code> to read from.
     * @param param An instance of <code>ImageDecodeParam</code>
     *        suitable for use with the <code>ImageCodec</code>
     *        subclass, or <code>null</code>.
     * @return An instance of <code>ImageDecoder</code>.
     */
    protected ImageDecoder createImageDecoder(InputStream src,
                                              ImageDecodeParam param) {
        SeekableStream stream = SeekableStream.wrapInputStream(src, true);
        return createImageDecoder(stream, param);
    }

    /**
     * Returns an implementation of the <code>ImageDecoder</code>
     * interface appropriate for that codec.  Subclasses of
     * <code>ImageCodec</code> may override this method if they wish
     * to accept data directly from a <code>File</code>;
     * otherwise, this method will convert the source into a
     * <code>SeekableStream</code> and call the appropriate
     * version of <code>createImageDecoder</code> for that data type.
     *
     * @param dst A <code>File</code> to read from.
     * @param param An instance of <code>ImageDecodeParam</code>
     *        suitable for use with the <code>ImageCodec</code>
     *        subclass, or <code>null</code>.
     * @return An instance of <code>ImageDecoder</code>.
     */
    protected ImageDecoder createImageDecoder(File src,
                                              ImageDecodeParam param)
        throws IOException {
        return createImageDecoder(new FileSeekableStream(src), param);
    }

    /**
     * In a concrete subclass of <code>ImageCodec</code>, returns an
     * implementation of the <code>ImageDecoder</code> interface
     * appropriate for that codec.
     *
     * @param dst A <code>SeekableStream</code> to read from.
     * @param param An instance of <code>ImageDecodeParam</code>
     *        suitable for use with the <code>ImageCodec</code>
     *        subclass, or <code>null</code>.
     * @return An instance of <code>ImageDecoder</code>.
     */
    protected abstract ImageDecoder createImageDecoder(SeekableStream src,
                                                       ImageDecodeParam param);

    // ColorModel utility functions

    private static final byte[][] grayIndexCmaps = {
        null,
        // 1 bit
        { (byte)0x00, (byte)0xff },
        // 2 bits
        { (byte)0x00, (byte)0x55, (byte)0xaa, (byte)0xff },
        null,
        // 4 bits
        { (byte)0x00, (byte)0x11, (byte)0x22, (byte)0x33,
          (byte)0x44, (byte)0x55, (byte)0x66, (byte)0x77,
          (byte)0x88, (byte)0x99, (byte)0xaa, (byte)0xbb,
          (byte)0xcc, (byte)0xdd, (byte)0xee, (byte)0xff }
    };

    /**
     * A convenience methods to create an instance of
     * <code>IndexColorModel</code> suitable for the given 1-banded
     * <code>SampleModel</code>.
     *
     * @param sm a 1-banded <code>SampleModel</code>.
     * @param blackIsZero <code>true</code> if the gray ramp should
     *        go from black to white, <code>false</code>otherwise.
     */
    public static ColorModel createGrayIndexColorModel(SampleModel sm,
                                                       boolean blackIsZero) {
        if (sm.getNumBands() != 1) {
            throw new IllegalArgumentException();
        }
        int sampleSize = sm.getSampleSize(0);

        byte[] cmap = null;
        if (sampleSize < 8) {
            cmap = grayIndexCmaps[sampleSize];
            if (!blackIsZero) {
                int length = cmap.length;
                byte[] newCmap = new byte[length];
                for (int i = 0; i < length; i++) {
                    newCmap[i] = cmap[length - i - 1];
                }
                cmap = newCmap;
            }
        } else {
            cmap = new byte[256];
            if (blackIsZero) {
                for (int i = 0; i < 256; i++) {
                    cmap[i] = (byte)i;
                }
            } else {
                for (int i = 0; i < 256; i++) {
                    cmap[i] = (byte)(255 - i);
                }
            }
        }

        return new IndexColorModel(sampleSize, cmap.length,
                                   cmap, cmap, cmap);
    }

    private static final int[] GrayBits8 = { 8 };
    private static final ComponentColorModel colorModelGray8 =
        new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_GRAY),
                                GrayBits8, false, false,
                                Transparency.OPAQUE,
                                DataBuffer.TYPE_BYTE);

    private static final int[] GrayAlphaBits8 = { 8, 8 };
    private static final ComponentColorModel colorModelGrayAlpha8 =
        new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_GRAY),
                                GrayAlphaBits8, true, false,
                                Transparency.TRANSLUCENT,
                                DataBuffer.TYPE_BYTE);

    private static final int[] GrayBits16 = { 16 };
    private static final ComponentColorModel colorModelGray16 =
        new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_GRAY),
                                GrayBits16, false, false,
                                Transparency.OPAQUE,
                                DataBuffer.TYPE_USHORT);

    private static final int[] GrayAlphaBits16 = { 16, 16 };
    private static final ComponentColorModel colorModelGrayAlpha16 =
        new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_GRAY),
                                GrayAlphaBits16, true, false,
                                Transparency.TRANSLUCENT,
                                DataBuffer.TYPE_USHORT);

    private static final int[] GrayBits32 = { 32 };
    private static final ComponentColorModel colorModelGray32 =
        new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_GRAY),
                                GrayBits32, false, false,
                                Transparency.OPAQUE,
                                DataBuffer.TYPE_INT);

    private static final int[] GrayAlphaBits32 = { 32, 32 };
    private static final ComponentColorModel colorModelGrayAlpha32 =
        new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_GRAY),
                                GrayAlphaBits32, true, false,
                                Transparency.TRANSLUCENT,
                                DataBuffer.TYPE_INT);

    private static final int[] RGBBits8 = { 8, 8, 8 };
    private static final ComponentColorModel colorModelRGB8 =
      new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB),
                              RGBBits8, false, false,
                              Transparency.OPAQUE,
                              DataBuffer.TYPE_BYTE);

    private static final int[] RGBABits8 = { 8, 8, 8, 8 };
    private static final ComponentColorModel colorModelRGBA8 =
      new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB),
                              RGBABits8, true, false,
                              Transparency.TRANSLUCENT,
                              DataBuffer.TYPE_BYTE);

    private static final int[] RGBBits16 = { 16, 16, 16 };
    private static final ComponentColorModel colorModelRGB16 =
      new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB),
                              RGBBits16, false, false,
                              Transparency.OPAQUE,
                              DataBuffer.TYPE_USHORT);

    private static final int[] RGBABits16 = { 16, 16, 16, 16 };
    private static final ComponentColorModel colorModelRGBA16 =
      new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB),
                              RGBABits16, true, false,
                              Transparency.TRANSLUCENT,
                              DataBuffer.TYPE_USHORT);

    private static final int[] RGBBits32 = { 32, 32, 32 };
    private static final ComponentColorModel colorModelRGB32 =
      new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB),
                              RGBBits32, false, false,
                              Transparency.OPAQUE,
                              DataBuffer.TYPE_INT);

    private static final int[] RGBABits32 = { 32, 32, 32, 32 };
    private static final ComponentColorModel colorModelRGBA32 =
      new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB),
                              RGBABits32, true, false,
                              Transparency.TRANSLUCENT,
                              DataBuffer.TYPE_INT);

    /**
     * A convenience method to create an instance of
     * <code>ComponentColorModel</code> suitable for use with the
     * given <code>SampleModel</code>.  The <code>SampleModel</code>
     * should have a data type of <code>DataBuffer.TYPE_BYTE</code>,
     * <code>TYPE_USHORT</code>, or <code>TYPE_INT</code> and between
     * 1 and 4 bands.  Depending on the number of bands of the
     * <code>SampleModel</code>, either a gray, gray+alpha, rgb, or
     * rgb+alpha <code>ColorModel</code> is returned.
     */
    public static ColorModel createComponentColorModel(SampleModel sm) {
        int type = sm.getDataType();
        int bands = sm.getNumBands();
        ComponentColorModel cm = null;

        if (type == DataBuffer.TYPE_BYTE) {
            switch (bands) {
            case 1:
                cm = colorModelGray8;
                break;
            case 2:
                cm = colorModelGrayAlpha8;
                break;
            case 3:
                cm = colorModelRGB8;
                break;
            case 4:
                cm = colorModelRGBA8;
                break;
            }
        } else if (type == DataBuffer.TYPE_USHORT) {
            switch (bands) {
            case 1:
                cm = colorModelGray16;
                break;
            case 2:
                cm = colorModelGrayAlpha16;
                break;
            case 3:
                cm = colorModelRGB16;
                break;
            case 4:
                cm = colorModelRGBA16;
                break;
            }
        } else if (type == DataBuffer.TYPE_INT) {
            switch (bands) {
            case 1:
                cm = colorModelGray32;
                break;
            case 2:
                cm = colorModelGrayAlpha32;
                break;
            case 3:
                cm = colorModelRGB32;
                break;
            case 4:
                cm = colorModelRGBA32;
                break;
            }
        } else if (type == DataBuffer.TYPE_FLOAT &&
                   bands >= 1 && bands <= 4) {
            ColorSpace cs = bands <= 2 ?
                ColorSpace.getInstance(ColorSpace.CS_GRAY) :
                ColorSpace.getInstance(ColorSpace.CS_sRGB);
            boolean hasAlpha = bands % 2 == 0;
            cm = new FloatDoubleColorModel(cs, hasAlpha, false,
                                           hasAlpha ?
                                           Transparency.TRANSLUCENT :
                                           Transparency.OPAQUE,
                                           DataBuffer.TYPE_FLOAT);
        }

        return cm;
    }

    /**
     * A convenience method to create an instance of
     * <code>ComponentColorModel</code> suitable for use with the
     * given <code>SampleModel</code> and <ColorSpace</code>.  The 
     * <code>SampleModel</code>
     * should have a data type of <code>DataBuffer.TYPE_BYTE</code>,
     * <code>TYPE_USHORT</code>, or <code>TYPE_INT</code> and between
     * 1 and 4 bands.  Depending on the number of bands of the
     * <code>SampleModel</code>, either a gray, gray+alpha, rgb, or
     * rgb+alpha <code>ColorModel</code> is returned.
     */
    public static ColorModel createComponentColorModel(SampleModel sm,
						       ColorSpace cp) {
	if (cp == null)
	    return createComponentColorModel(sm);
        int type = sm.getDataType();
	int bands = sm.getNumBands();
	ComponentColorModel cm = null;

	int[] bits = null;
        int transferType = -1;
        boolean hasAlpha = (bands % 2 == 0);
	if (cp instanceof SimpleCMYKColorSpace)
	    hasAlpha = false;
        int transparency = hasAlpha ? Transparency.TRANSLUCENT
                                        : Transparency.OPAQUE;
        if (type == DataBuffer.TYPE_BYTE) {
            transferType = DataBuffer.TYPE_BYTE;
            switch (bands) {
                case 1: 
		    bits = GrayBits8; 
		    break;
                case 2: 
		    bits = GrayAlphaBits8; 
		    break;
                case 3: 
		    bits = RGBBits8; 
		    break;
                case 4: 
		    bits = RGBABits8; 
		    break;
            }
        } else if (type == DataBuffer.TYPE_USHORT) {
            transferType = DataBuffer.TYPE_USHORT;
            switch (bands) {
                case 1: 
		    bits = GrayBits16; 
		    break;
                case 2: 
		    bits = GrayAlphaBits16; 
		    break;
                case 3: 
		    bits = RGBBits16; 
		    break;
                case 4: 
		    bits = RGBABits16; 
		    break;
            }
        } else if (type == DataBuffer.TYPE_INT) {
            transferType = DataBuffer.TYPE_INT;
            switch (bands) {
                case 1: 
		    bits = GrayBits32; 
		    break;
                case 2: 
		    bits = GrayAlphaBits32; 
		    break;
                case 3: 
		    bits = RGBBits32; 
		    break;
                case 4: 
		    bits = RGBABits32; 
		    break;
            }
        }

        if (type == DataBuffer.TYPE_FLOAT &&
                   bands >= 1 && bands <= 4) {
            cm = new FloatDoubleColorModel(cp, hasAlpha, false,
                                           transparency,
                                           DataBuffer.TYPE_FLOAT);
        } else {
            cm = new ComponentColorModel(cp, bits, hasAlpha,
                                         false, transparency, transferType);
        }

	return cm;
    }

    /**
     * Tests whether the color indices represent a gray-scale image.
     *
     * @param r The red channel color indices.
     * @param g The green channel color indices.
     * @param b The blue channel color indices.
     * @return If all the indices have 256 entries, and are identical mappings,
     *	       return <code>true</code>; otherwise, return <code>false</code>.
     */
    public static boolean isIndicesForGrayscale(byte[] r, byte[] g, byte[] b) {
	if (r.length != g.length || r.length != b.length)
	    return false;

	int size = r.length;

	if (size != 256)
	    return false;

	for (int i = 0; i < size; i++) {
	    byte temp = (byte) i;

	    if (r[i] != temp || g[i] != temp || b[i] != temp)
		return false;
	}

	return true;
    }
}
