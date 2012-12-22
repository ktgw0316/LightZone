/*
 * $RCSfile: TIFFEncodeParam.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.2 $
 * $Date: 2005/11/14 22:44:06 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.codec;

import java.util.Iterator;
import java.util.zip.Deflater;

/**
 * An instance of <code>ImageEncodeParam</code> for encoding images in 
 * the TIFF format.
 *
 * <p> This class allows for the specification of encoding parameters. By
 * default, the image is encoded without any compression, and is written
 * out consisting of strips, not tiles. The particular compression scheme 
 * to be used can be specified by using the <code>setCompression()</code>
 * method. The compression scheme specified will be honored only if it is 
 * compatible with the type of image being written out. For example, 
 * Group3 and Group4 compressions can only be used with Bilevel images.
 * Writing of tiled TIFF images can be enabled by calling the
 * <code>setWriteTiled()</code> method.
 *
 * <p><b> This class is not a committed part of the JAI API.  It may
 * be removed or changed in future releases of JAI.</b>
 *
 */
public class TIFFEncodeParam implements ImageEncodeParam {

    /** No compression. */
    public static final int COMPRESSION_NONE          = 1;

    /** Byte-oriented run-length encoding "PackBits" compression. */
    public static final int COMPRESSION_PACKBITS      = 32773;

    /**
     * Modified Huffman Compression (CCITT Run Length Encoding (RLE)).
     */
    public static final int COMPRESSION_GROUP3_1D     = 2;

    /**
     * CCITT T.4 bilevel compression (Group 3 facsimile compression).
     */
    public static final int COMPRESSION_GROUP3_2D      = 3;

    /**
     * CCITT T.6 bilevel compression (Group 4 facsimile compression).
     */
    public static final int COMPRESSION_GROUP4      = 4;

    /**
     * LZW compression.
     * <p><b>Not supported.</b>
     */
    public static final int COMPRESSION_LZW           = 5;

    /**
     * <a href="ftp://ftp.sgi.com/graphics/tiff/TTN2.draft.txt"> 
     * JPEG-in-TIFF</a> compression.
     */
    public static final int COMPRESSION_JPEG_TTN2     = 7;

    /**
     * <a href="http://info.internet.isi.edu:80/in-notes/rfc/files/rfc1951.txt"> 
     * DEFLATE</a> lossless compression (also known as "Zip-in-TIFF").
     */
    public static final int COMPRESSION_DEFLATE       = 32946;

    private int compression = COMPRESSION_NONE;

    private boolean reverseFillOrder = false;
    private boolean T4Encode2D = true;
    private boolean T4PadEOLs = false;

    private boolean writeTiled = false;
    private int tileWidth;
    private int tileHeight;

    private Iterator extraImages;

    private TIFFField[] extraFields;

    private boolean convertJPEGRGBToYCbCr = true;
    private JPEGEncodeParam jpegEncodeParam = null;

    private int deflateLevel = Deflater.DEFAULT_COMPRESSION;

    private boolean isLittleEndian = false;

    /** 
     * Constructs a TIFFEncodeParam object with default values for
     * all parameters.
     */
    public TIFFEncodeParam() {}

    /**
     * Returns the value of the compression parameter.
     */
    public int getCompression() {
	return compression;
    }

    /**
     * Specifies the type of compression to be used.  The compression type
     * specified will be honored only if it is compatible with the image
     * being written out.  Currently only PackBits, JPEG, and DEFLATE
     * compression schemes are supported.
     *
     * <p> If <code>compression</code> is set to any value but
     * <code>COMPRESSION_NONE</code> and the <code>OutputStream</code>
     * supplied to the <code>ImageEncoder</code> is not a
     * <code>SeekableOutputStream</code>, then the encoder will use either
     * a temporary file or a memory cache when compressing the data
     * depending on whether the file system is accessible.  Compression
     * will therefore be more efficient if a <code>SeekableOutputStream</code>
     * is supplied.
     *
     * @param compression    The compression type.
     * @throws IllegalArgumentException if <code>compression</code> is
     * not one of the defined <code>COMPRESSION_*</code> constants.
     */
    public void setCompression(int compression) {

        switch(compression) {
        case COMPRESSION_NONE:
        case COMPRESSION_GROUP3_1D:
        case COMPRESSION_GROUP3_2D:
        case COMPRESSION_GROUP4:
        case COMPRESSION_PACKBITS:
        case COMPRESSION_JPEG_TTN2:
        case COMPRESSION_DEFLATE:
            // Do nothing.
            break;
        default:
	    throw new IllegalArgumentException(JaiI18N.getString("TIFFEncodeParam0"));
	}

	this.compression = compression;
    }

    /**
     * Returns value of flag indicating whether CCITT-compressed bilevel
     * data should be filled in reverse order.
     *
     * @see #setReverseFillOrder
     */
    public boolean getReverseFillOrder() {
        return reverseFillOrder;
    }

    /**
     * Set value of flag indicating whether CCITT-compressed bilevel
     * data should be filled in reverse order.  If <code>true</code>,
     * pixels are arranged within a byte such that pixels with lower
     * column values are stored in the lower order bits of the byte.
     * Thus <code>true</code> corresponds to TIFF FillOrder value 2
     * and <code>false</code> to TIFF FillOrder 1.  The default
     * value is <code>false</code>.
     */
    public void setReverseFillOrder(boolean reverseFillOrder) {
        this.reverseFillOrder = reverseFillOrder;
    }

    /**
     * Returns value of flag indicating whether T4-compressed bilevel data
     * should be two-dimensionally encoded.
     *
     * @see #setT4Encode2D
     */
    public boolean getT4Encode2D() {
        return T4Encode2D;
    }

    /**
     * Set value of flag indicating whether T4-compressed bilevel data
     * should be two-dimensionally encoded.  If <code>true</code> the
     * data are two-dimensionally encoded; if <code>false</code> they
     * are one-dimensionally encoded.  The default value is <code>true</code>.
     */
    public void setT4Encode2D(boolean T4Encode2D) {
        this.T4Encode2D = T4Encode2D;
    }

    /**
     * Returns value of flag indicating whether T4-compressed bilevel data
     * should have the embedded EOL bit sequences padded to byte alignment.
     *
     * @see #setT4PadEOLs
     */
    public boolean getT4PadEOLs() {
        return T4PadEOLs;
    }

    /**
     * Sets value of flag indicating whether T4-compressed bilevel data
     * should have the embedded EOL bit sequences padded to byte alignment.
     * If <code>true</code>, zero-valued bits are prepended to each EOL
     * bit sequence <code>0x001</code> such that the EOL is right-aligned
     * on a byte boundary:
     *
     * <pre>
     * xxxx-0000 0000-0001
     * </pre>
     *
     * where "x" denotes a value which could be either data or a fill bit
     * depending on the alignment of the data before the EOL.  The default
     * value is <code>false</code>.
     */
    public void setT4PadEOLs(boolean T4PadEOLs) {
        this.T4PadEOLs = T4PadEOLs;
    }

    /**
     * Returns the value of the writeTiled parameter. 
     */
    public boolean getWriteTiled() {
	return writeTiled;
    }

    /**
     * If set, the data will be written out in tiled format, instead of
     * in strips.
     *
     * @param writeTiled     Specifies whether the image data should be 
     *                       wriiten out in tiled format.
     */
    public void setWriteTiled(boolean writeTiled) {
	this.writeTiled = writeTiled;
    }

    /**
     * Sets the dimensions of the tiles to be written.  If either
     * value is non-positive, the encoder will use a default value.
     *
     * <p> If the data are being written as tiles, i.e.,
     * <code>getWriteTiled()</code> returns <code>true</code>, then the
     * default tile dimensions used by the encoder are those of the tiles
     * of the image being encoded.
     *
     * <p> If the data are being written as strips, i.e.,
     * <code>getWriteTiled()</code> returns <code>false</code>, the width
     * of each strip is always the width of the image and the default
     * number of rows per strip is 8.
     *
     * <p> If JPEG compession is being used, the dimensions of the strips or
     * tiles may be modified to conform to the JPEG-in-TIFF specification.
     * 
     * @param tileWidth The tile width; ignored if strips are used.
     * @param tileHeight The tile height or number of rows per strip.
     */
    public void setTileSize(int tileWidth, int tileHeight) {
        this.tileWidth = tileWidth;
        this.tileHeight = tileHeight;
    }

    /**
     * Retrieves the tile width set via <code>setTileSize()</code>.
     */
    public int getTileWidth() {
        return tileWidth;
    }

    /**
     * Retrieves the tile height set via <code>setTileSize()</code>.
     */
    public int getTileHeight() {
        return tileHeight;
    }

    /**
     * Sets an <code>Iterator</code> of additional images to be written
     * after the image passed as an argument to the <code>ImageEncoder</code>.
     * The methods on the supplied <code>Iterator</code> must only be invoked
     * by the <code>ImageEncoder</code> which will exhaust the available
     * values unless an error occurs.
     *
     * <p> The value returned by an invocation of <code>next()</code> on the
     * <code>Iterator</code> must return either a <code>RenderedImage</code>
     * or an <code>Object[]</code> of length 2 wherein the element at index
     * zero is a <code>RenderedImage</code> amd the other element is a
     * <code>TIFFEncodeParam</code>.  If no <code>TIFFEncodeParam</code> is
     * supplied in this manner for an additional image, the parameters used
     * to create the <code>ImageEncoder</code> will be used.  The extra
     * image <code>Iterator</code> set on any <code>TIFFEncodeParam</code>
     * of an additional image will in all cases be ignored.
     */
    public synchronized void setExtraImages(Iterator extraImages) {
        this.extraImages = extraImages;
    }

    /**
     * Returns the additional image <code>Iterator</code> specified via
     * <code>setExtraImages()</code> or <code>null</code> if none was
     * supplied or if a <code>null</code> value was supplied.
     */
    public synchronized Iterator getExtraImages() {
        return extraImages;
    }

    /**
     * Sets the compression level for DEFLATE-compressed data which should
     * either be <code>java.util.Deflater.DEFAULT_COMPRESSION</code> or a
     * value in the range [1,9] where larger values indicate more compression.
     * The default setting is <code>Deflater.DEFAULT_COMPRESSION</code>.  This
     * setting is ignored if the compression type is not DEFLATE.
     *
     * @throws IllegalArgumentException if <code>deflateLevel</code> is
     * not in the range <code>[1,&nbsp;9]</code> and is not
     * {@link Deflater#DEFAULT_COMPRESSION}.
     */
    public void setDeflateLevel(int deflateLevel) {
        if(deflateLevel < 1 && deflateLevel > 9 &&
           deflateLevel != Deflater.DEFAULT_COMPRESSION) {
	    throw new IllegalArgumentException(JaiI18N.getString("TIFFEncodeParam1"));
        }

        this.deflateLevel = deflateLevel;
    }

    /**
     * Gets the compression level for DEFLATE compression.
     */
    public int getDeflateLevel() {
        return deflateLevel;
    }

    /**
     * Sets flag indicating whether to convert RGB data to YCbCr when the
     * compression type is JPEG.  The default value is <code>true</code>.
     * This flag is ignored if the compression type is not JPEG.
     */
    public void setJPEGCompressRGBToYCbCr(boolean convertJPEGRGBToYCbCr) {
        this.convertJPEGRGBToYCbCr = convertJPEGRGBToYCbCr;
    }

    /**
     * Whether RGB data will be converted to YCbCr when using JPEG compression.
     */
    public boolean getJPEGCompressRGBToYCbCr() {
        return convertJPEGRGBToYCbCr;
    }

    /**
     * Sets the JPEG compression parameters.  These parameters are ignored
     * if the compression type is not JPEG.  The argument may be
     * <code>null</code> to indicate that default compression parameters
     * are to be used.  For maximum conformance with the specification it
     * is recommended in most cases that only the quality compression
     * parameter be set.
     *
     * <p> The <code>writeTablesOnly</code> and <code>JFIFHeader</code>
     * flags of the <code>JPEGEncodeParam</code> are ignored.  The
     * <code>writeImageOnly</code> flag is used to determine whether the
     * JPEGTables field will be written to the TIFF stream: if
     * <code>writeImageOnly</code> is <code>true</code>, then the JPEGTables
     * field will be written and will contain a valid JPEG abbreviated
     * table specification datastream.  In this case the data in each data
     * segment (strip or tile) will contain an abbreviated JPEG image
     * datastream.  If the <code>writeImageOnly</code> flag is
     * <code>false</code>, then the JPEGTables field will not be written and
     * each data segment will contain a complete JPEG interchange datastream.
     */
    public void setJPEGEncodeParam(JPEGEncodeParam jpegEncodeParam) {
        if(jpegEncodeParam != null) {
            jpegEncodeParam = (JPEGEncodeParam)jpegEncodeParam.clone();
            jpegEncodeParam.setWriteTablesOnly(false);
            jpegEncodeParam.setWriteJFIFHeader(false);
        }
        this.jpegEncodeParam = jpegEncodeParam;
    }

    /**
     * Retrieves the JPEG compression parameters.
     */
    public JPEGEncodeParam getJPEGEncodeParam() {
        if(jpegEncodeParam == null) {
            jpegEncodeParam = new JPEGEncodeParam();
            jpegEncodeParam.setWriteTablesOnly(false);
            jpegEncodeParam.setWriteImageOnly(true);
            jpegEncodeParam.setWriteJFIFHeader(false);
        }
        return jpegEncodeParam;
    }

    /**
     * Sets an array of extra fields to be written to the TIFF Image File
     * Directory (IFD).  Fields with tags equal to the tag of any
     * automatically generated fields are ignored.  No error checking is
     * performed with respect to the validity of the field contents or
     * the appropriateness of the field for the image being encoded.
     *
     * @param extraFields An array of extra fields; the parameter is
     * copied by reference.
     */
    public void setExtraFields(TIFFField[] extraFields) {
        this.extraFields = extraFields;
    }

    /**
     * Returns the value set by <code>setExtraFields()</code>.
     */
    public TIFFField[] getExtraFields() {
        return extraFields;
    }

    /**
     * Sets a flag indicating whether the byte order used to write the
     * output stream is <i>little endian</i>.  If <code>true</code>
     * multi-byte data units such as 16-bit and 32-bit integers and 32-bit
     * floating point values are written from the least to the most
     * significant byte; if <code>false</code> the order is from most to
     * least significant byte.  The default value is <code>false</code>.
     */
    public void setLittleEndian(boolean isLittleEndian) {
        this.isLittleEndian = isLittleEndian;
    }

    /**
     * Returns the value of the flag indicating whether the output stream
     * byte order is little endian.
     */
    public boolean getLittleEndian() {
        return this.isLittleEndian;
    }
}
