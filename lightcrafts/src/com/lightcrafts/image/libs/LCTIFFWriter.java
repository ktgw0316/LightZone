/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2018-     Masahiro Kitagawa */

package com.lightcrafts.image.libs;

import com.lightcrafts.image.export.ResolutionOption;
import com.lightcrafts.image.export.ResolutionUnitOption;
import com.lightcrafts.image.metadata.EXIFDirectory;
import com.lightcrafts.image.metadata.EXIFEncoder;
import com.lightcrafts.image.metadata.IPTCDirectory;
import com.lightcrafts.image.metadata.ImageMetadata;
import com.lightcrafts.image.metadata.ImageMetadataDirectory;
import com.lightcrafts.image.metadata.TIFFDirectory;
import com.lightcrafts.image.metadata.TIFFMetadataReader;
import com.lightcrafts.image.metadata.values.ImageMetaValue;
import com.lightcrafts.image.types.TIFFConstants;
import com.lightcrafts.image.types.TIFFImageType;
import com.lightcrafts.utils.ProgressIndicator;
import com.lightcrafts.utils.UserCanceledException;
import com.lightcrafts.utils.Version;
import com.lightcrafts.utils.file.OrderableRandomAccessFile;
import com.lightcrafts.utils.thread.ProgressThread;
import com.lightcrafts.utils.xml.XMLUtil;
import java.awt.Point;
import java.awt.color.ICC_Profile;
import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferUShort;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;
import javax.media.jai.PlanarImage;
import org.w3c.dom.Document;

import static com.lightcrafts.image.metadata.EXIFConstants.EXIF_FIELD_SIZE;
import static com.lightcrafts.image.metadata.EXIFTags.EXIF_GPS_IFD_POINTER;
import static com.lightcrafts.image.metadata.EXIFTags.EXIF_IFD_POINTER;
import static com.lightcrafts.image.metadata.TIFFTags.TIFF_ARTIST;
import static com.lightcrafts.image.metadata.TIFFTags.TIFF_BITS_PER_SAMPLE;
import static com.lightcrafts.image.metadata.TIFFTags.TIFF_COPYRIGHT;
import static com.lightcrafts.image.metadata.TIFFTags.TIFF_DATE_TIME;
import static com.lightcrafts.image.metadata.TIFFTags.TIFF_DOCUMENT_NAME;
import static com.lightcrafts.image.metadata.TIFFTags.TIFF_EXIF_IFD_POINTER;
import static com.lightcrafts.image.metadata.TIFFTags.TIFF_HOST_COMPUTER;
import static com.lightcrafts.image.metadata.TIFFTags.TIFF_ICC_PROFILE;
import static com.lightcrafts.image.metadata.TIFFTags.TIFF_IMAGE_DESCRIPTION;
import static com.lightcrafts.image.metadata.TIFFTags.TIFF_IMAGE_LENGTH;
import static com.lightcrafts.image.metadata.TIFFTags.TIFF_IMAGE_WIDTH;
import static com.lightcrafts.image.metadata.TIFFTags.TIFF_INK_NAMES;
import static com.lightcrafts.image.metadata.TIFFTags.TIFF_MAKE;
import static com.lightcrafts.image.metadata.TIFFTags.TIFF_MODEL;
import static com.lightcrafts.image.metadata.TIFFTags.TIFF_MS_RATING;
import static com.lightcrafts.image.metadata.TIFFTags.TIFF_PAGE_NAME;
import static com.lightcrafts.image.metadata.TIFFTags.TIFF_PHOTOMETRIC_INTERPRETATION;
import static com.lightcrafts.image.metadata.TIFFTags.TIFF_PHOTOSHOP_IMAGE_RESOURCES;
import static com.lightcrafts.image.metadata.TIFFTags.TIFF_PLANAR_CONFIGURATION;
import static com.lightcrafts.image.metadata.TIFFTags.TIFF_RESOLUTION_UNIT;
import static com.lightcrafts.image.metadata.TIFFTags.TIFF_RICH_TIFF_IPTC;
import static com.lightcrafts.image.metadata.TIFFTags.TIFF_ROWS_PER_STRIP;
import static com.lightcrafts.image.metadata.TIFFTags.TIFF_SAMPLES_PER_PIXEL;
import static com.lightcrafts.image.metadata.TIFFTags.TIFF_SOFTWARE;
import static com.lightcrafts.image.metadata.TIFFTags.TIFF_TARGET_PRINTER;
import static com.lightcrafts.image.metadata.TIFFTags.TIFF_TILE_LENGTH;
import static com.lightcrafts.image.metadata.TIFFTags.TIFF_TILE_WIDTH;
import static com.lightcrafts.image.metadata.TIFFTags.TIFF_XMP_PACKET;
import static com.lightcrafts.image.metadata.TIFFTags.TIFF_X_RESOLUTION;
import static com.lightcrafts.image.metadata.TIFFTags.TIFF_Y_RESOLUTION;
import static com.lightcrafts.image.types.TIFFConstants.TIFF_FIELD_TYPE_ULONG;
import static com.lightcrafts.image.types.TIFFConstants.TIFF_HEADER_SIZE;
import static com.lightcrafts.image.types.TIFFConstants.TIFF_INLINE_VALUE_MAX_SIZE;
import static com.lightcrafts.image.types.TIFFConstants.TIFF_INT_SIZE;
import static com.lightcrafts.image.types.TIFFConstants.TIFF_LITTLE_ENDIAN;
import static com.lightcrafts.image.types.TIFFConstants.TIFF_PHOTOMETRIC_BLACK_IS_ZERO;
import static com.lightcrafts.image.types.TIFFConstants.TIFF_PHOTOMETRIC_RGB;
import static com.lightcrafts.image.types.TIFFConstants.TIFF_PHOTOMETRIC_SEPARATED;
import static com.lightcrafts.image.types.TIFFConstants.TIFF_PLANAR_CONFIGURATION_CHUNKY;
import static com.lightcrafts.image.types.TIFFConstants.TIFF_SHORT_SIZE;

/**
 * An <code>LCTIFFWriter</code> is a Java wrapper around the LibTIFF library.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 * @see <a href="http://www.remotesensing.org/libtiff/">LibTIFF</a>
 */
public final class LCTIFFWriter extends LCTIFFCommon {

    /**
     * Construct an <code>LCTIFFWriter</code> and open a TIFF file.
     *
     * @param fileName The name of the TIFF file to open.
     * @param width The width of the image in pixels.
     * @param height The height of the image in pixels.
     */
    public LCTIFFWriter(String fileName, int width, int height)
            throws LCImageLibException, UnsupportedEncodingException {
        this(fileName, null, width, height);
    }

    /**
     * Construct an <code>LCTIFFWriter</code> and open a TIFF file.
     *
     * @param fileName The name of the TIFF file to open.
     * @param width The width of the image in pixels.
     * @param height The height of the image in pixels.
     * @param resolution The resolution (in pixels per unit).
     * @param resolutionUnit The resolution unit; must be either {@link
     * TIFFConstants#TIFF_RESOLUTION_UNIT_CM} or {@link TIFFConstants#TIFF_RESOLUTION_UNIT_INCH}.
     */
    public LCTIFFWriter(String fileName, int width, int height,
            int resolution, int resolutionUnit)
            throws LCImageLibException, UnsupportedEncodingException {
        this(fileName, null, width, height, resolution, resolutionUnit);
    }

    /**
     * Construct an <code>LCTIFFWriter</code> and open a TIFF file.
     *
     * @param fileName The name of the TIFF file to open.
     * @param appendFileName The name of the TIFF file to append as the second page in a 2-page TIFF
     * file.
     * @param width The width of the image in pixels.
     * @param height The height of the image in pixels.
     */
    public LCTIFFWriter(String fileName, String appendFileName, int width,
            int height)
            throws LCImageLibException, UnsupportedEncodingException {
        this(
                fileName, appendFileName, width, height,
                ResolutionOption.DEFAULT_VALUE, ResolutionUnitOption.DEFAULT_VALUE
        );
    }

    /**
     * Construct an <code>LCTIFFWriter</code> and open a TIFF file.
     *
     * @param fileName The name of the TIFF file to open.
     * @param appendFileName The name of the TIFF file to append as the second page in a 2-page TIFF
     * file.
     * @param width The width of the image in pixels.
     * @param height The height of the image in pixels.
     * @param resolution The resolution (in pixels per unit).
     * @param resolutionUnit The resolution unit; must be either {@link
     * TIFFConstants#TIFF_RESOLUTION_UNIT_CM} or {@link TIFFConstants#TIFF_RESOLUTION_UNIT_INCH}.
     */
    public LCTIFFWriter(String fileName, String appendFileName, int width,
            int height, int resolution, int resolutionUnit)
            throws LCImageLibException, UnsupportedEncodingException {
        m_fileName = fileName;
        m_appendFileName = appendFileName;
        m_exportWidth = width;
        m_exportHeight = height;
        m_resolution = resolution;
        m_resolutionUnit = resolutionUnit;
        openForWriting(fileName);
        //
        // If openForWriting() fails, it will store 0 in the native pointer.
        //
        if (m_nativePtr == 0) {
            throw new LCImageLibException("Could not open " + fileName);
        }
    }

    /**
     * Puts a TIFF image as tiles.
     *
     * @param image The image to put.
     * @param thread The thread that's doing the putting.
     */
    public void putImageTiled(RenderedImage image, ProgressThread thread)
            throws IOException, LCImageLibException {
        try {
            writeImageTiled(image, thread);
            if (m_appendFileName != null) {
                append(m_appendFileName);
            }
            dispose();
            if (m_hasExifMetadata) {
                fixEXIFMetadata(m_fileName);
            }
        } finally {
            dispose();
        }
    }

    /**
     * Puts a TIFF image as strips.
     *
     * @param image The image to put.
     * @param thread The thread that's doing the putting.
     */
    public void putImageStriped(RenderedImage image, ProgressThread thread)
            throws IOException, LCImageLibException {
        try {
            writeImageStriped(image, thread);
            if (m_appendFileName != null) {
                append(m_appendFileName);
            }
            dispose();
            if (m_hasExifMetadata) {
                fixEXIFMetadata(m_fileName);
            }
        } finally {
            dispose();
        }
    }

    /**
     * Puts the given {@link ImageMetadata} into the TIFF file. This <i>must</i> be called only once
     * and prior to {@link #putImageStriped(RenderedImage, ProgressThread)}.
     *
     * @param metadata The {@link ImageMetadata} to put.
     */
    public void putMetadata(ImageMetadata metadata)
            throws LCImageLibException, UnsupportedEncodingException {
        metadata = metadata.prepForExport(
                TIFFImageType.INSTANCE, m_exportWidth, m_exportHeight,
                m_resolution, m_resolutionUnit, false
        );

        ////////// Put TIFF metadata //////////////////////////////////////////

        final ImageMetadataDirectory tiffDir =
                metadata.getDirectoryFor(TIFFDirectory.class);
        if (tiffDir != null) {
            for (final Map.Entry<Integer, ImageMetaValue> me : tiffDir) {
                final int tagID = me.getKey();
                final ImageMetaValue value = me.getValue();
                switch (tagID) {
                    case TIFF_ARTIST:
                    case TIFF_COPYRIGHT:
                    case TIFF_DATE_TIME:
                    case TIFF_DOCUMENT_NAME:
                    case TIFF_HOST_COMPUTER:
                    case TIFF_IMAGE_DESCRIPTION:
                    case TIFF_INK_NAMES:
                    case TIFF_MAKE:
                    case TIFF_MODEL:
                    case TIFF_PAGE_NAME:
                    case TIFF_SOFTWARE:
                    case TIFF_TARGET_PRINTER:
                        setStringField(tagID, value.getStringValue());
                        break;
                    case TIFF_MS_RATING:
                    case TIFF_RESOLUTION_UNIT:
                        setIntField(tagID, value.getIntValue());
                        break;
                    case TIFF_X_RESOLUTION:
                    case TIFF_Y_RESOLUTION:
                        setFloatField(tagID, value.getFloatValue());
                        break;
                }
            }
        }

        ////////// Put EXIF metadata //////////////////////////////////////////

        final ImageMetadataDirectory exifDir =
                metadata.getDirectoryFor(EXIFDirectory.class);
        if (exifDir != null) {
            final ByteBuffer exifBuf = EXIFEncoder.encode(metadata, false);
            //ByteBufferUtil.dumpToFile(exifBuf, "/tmp/tiff.exif");
            //
            // Libtiff doesn't support writing EXIF metadata so we have to do
            // an annoying work-around.  We temporarily store the encoded EXIF
            // metadata as belonging to the PHOTOSHOP tag.  Later, after the
            // TIFF file has been completely written, we go back and patch the
            // file in-place by changing the tag ID to EXIF_IFD_POINTER and
            // adjusting the EXIF metadata offsets.
            //
            // The reason the PHOTOSHOP tag is used is because: (1) we don't
            // use it for anything else, (2) its field type is unsigned byte
            // so we can set its value to the encoded binary EXIF metadata, and
            // (3) its tag ID (0x8649) is fairly close to that of the real tag
            // ID of EXIF_IFD_POINTER (0x8769).  Point #3 is important because
            // the tag IDs in a TIFF file must be in ascending sorted order so
            // even after the tag ID is changed, the set of tags is still in
            // ascending sorted order.
            //
            setByteField(TIFF_PHOTOSHOP_IMAGE_RESOURCES, exifBuf.array());
            m_hasExifMetadata = true;
        }

        ////////// Put IPTC metadata //////////////////////////////////////////

        final ImageMetadataDirectory iptcDir =
                metadata.getDirectoryFor(IPTCDirectory.class);
        if (iptcDir != null) {
            //
            // Write both the binary and XMP forms of IPTC metadata: the binary
            // form to enable non-XMP-aware applications to read it and the
            // XMP form to write all the metadata, i.e., the additional IPTC
            // tags present in XMP.
            //
            final byte[] iptcBuf = ((IPTCDirectory) iptcDir).encode(false);
            if (iptcBuf != null) {
                setByteField(TIFF_RICH_TIFF_IPTC, iptcBuf);
            }

            final Document xmpDoc = metadata.toXMP(false, true, IPTCDirectory.class);
            final byte[] xmpBuf = XMLUtil.encodeDocument(xmpDoc, false);
            setByteField(TIFF_XMP_PACKET, xmpBuf);
        }
    }

    /**
     * Sets the value of the given TIFF byte field.
     *
     * @param tagID The tag ID of the metadata field to set.  The ID should be that of a tag whose
     * value is a byte array ({@link TIFFConstants#TIFF_FIELD_TYPE_UBYTE}.
     * @param value The value for the given tag.
     * @return Returns <code>true</code> only if the value was set.
     * @throws IllegalArgumentException if <code>tagID</code> isn't that of an byte metadata field
     * or is otherwise unsupported.
     * @see #setFloatField(int, float)
     * @see #setIntField(int, int)
     * @see #setStringField(int, String)
     */
    public native boolean setByteField(int tagID, byte[] value)
            throws LCImageLibException;

    /**
     * Sets the value of the given TIFF integer metadata field.
     *
     * @param tagID The tag ID of the metadata field to set.  The ID should be that of a tag whose
     * value is an integer ({@link TIFFConstants#TIFF_FIELD_TYPE_USHORT} or {@link
     * TIFFConstants#TIFF_FIELD_TYPE_ULONG} and not a string.
     * @param value The value for the given tag.
     * @return Returns <code>true</code> only if the value was set.
     * @throws IllegalArgumentException if <code>tagID</code> isn't that of an integer metadata
     * field or is otherwise unsupported.
     * @see #setByteField(int, byte[])
     * @see #setIntField(int, int)
     * @see #setStringField(int, String)
     */
    private native boolean setFloatField(int tagID, float value)
            throws LCImageLibException;

    /**
     * Sets the ICC profile of the TIFF image.  This <i>must</i> be called only once and prior to
     * {@link #putImageStriped(RenderedImage, ProgressThread)} or {@link
     * #putImageTiled(RenderedImage, ProgressThread)}.
     *
     * @param iccProfile The {@link ICC_Profile} to set.
     */
    public void setICCProfile(ICC_Profile iccProfile)
            throws LCImageLibException {
        setByteField(TIFF_ICC_PROFILE, iccProfile.getData());
    }

    /**
     * Sets the value of the given TIFF integer metadata field.
     *
     * @param tagID The tag ID of the metadata field to set.  The ID should be that of a tag whose
     * value is an integer ({@link TIFFConstants#TIFF_FIELD_TYPE_USHORT} or {@link
     * TIFFConstants#TIFF_FIELD_TYPE_ULONG} and not a string.
     * @param value The value for the given tag.
     * @return Returns <code>true</code> only if the value was set.
     * @throws IllegalArgumentException if <code>tagID</code> isn't that of an integer metadata
     * field or is otherwise unsupported.
     * @see #setByteField(int, byte[])
     * @see #setFloatField(int, float)
     * @see #setStringField(int, String)
     */
    public native boolean setIntField(int tagID, int value)
            throws LCImageLibException;

    /**
     * Sets the value of the given TIFF string metadata field.
     *
     * @param tagID The tag ID of the metadata field to set.  The ID should be that of a tag whose
     * value is a string ({@link TIFFConstants#TIFF_FIELD_TYPE_ASCII}.
     * @param value The value for the given tag.
     * @return Returns <code>true</code> only if the value was set.
     * @throws IllegalArgumentException if <code>tagID</code> isn't that of an string metadata field
     * or is otherwise unsupported.
     * @see #setByteField(int, byte[])
     * @see #setFloatField(int, float)
     * @see #setIntField(int, int)
     */
    private boolean setStringField(int tagID, String value)
            throws LCImageLibException, UnsupportedEncodingException {
        byte[] valueUtf8 = (value + '\000').getBytes("UTF-8");
        return setStringField(tagID, valueUtf8);
    }

    private native boolean setStringField(int tagID, byte[] valueUtf8)
            throws LCImageLibException;

    /**
     * Append the TIFF image in the given file creating a multi-page TIFF file.
     *
     * @param fileName The name of the TIFF file to append.
     * @return Returns <code>true</code> only if the append succeeded.
     */
    private boolean append(String fileName)
            throws UnsupportedEncodingException {
        byte[] fileNameUtf8 = (fileName + '\000').getBytes("UTF-8");
        return append(fileNameUtf8);
    }

    private native boolean append(byte[] fileNameUtf8);

    /**
     * Computes which tile a given point is in.
     *
     * @param x The X coordinate.
     * @param y The Y coordinate.
     * @param z The Z coordinate.
     * @param sample TODO
     * @return Returns the tile index.
     */
    private native int computeTile(int x, int y, int z, int sample);

    /**
     * Fix the EXIF metadata in a TIFF file.
     *
     * @param fileName The full path of the TIFF file.
     */
    private static void fixEXIFMetadata(String fileName) throws IOException {
        //
        // This code is based on the code in TIFFMetadataReader but it's been
        // simplified and does much less error-checking because we just wrote
        // the TIFF file ourselves so we know it's valid.
        //
        try (OrderableRandomAccessFile file = new OrderableRandomAccessFile(fileName, "rw")) {
            if (file.readShort() == TIFF_LITTLE_ENDIAN) {
                file.order(ByteOrder.LITTLE_ENDIAN);
            }
            file.seek(TIFF_HEADER_SIZE - TIFF_INT_SIZE);
            int ifdOffset = file.readInt();
            while (ifdOffset > 0) {
                file.seek(ifdOffset);
                final int entryCount = file.readUnsignedShort();
                for (int entry = 0; entry < entryCount; ++entry) {
                    final int entryOffset =
                            TIFFMetadataReader.calcIFDEntryOffset(ifdOffset, entry);
                    file.seek(entryOffset);
                    final int tagID = file.readUnsignedShort();
                    if (tagID == TIFF_PHOTOSHOP_IMAGE_RESOURCES) {
                        file.seek(file.getFilePointer() - TIFF_SHORT_SIZE);
                        file.writeShort(TIFF_EXIF_IFD_POINTER);
                        file.writeShort(TIFF_FIELD_TYPE_ULONG);
                        file.writeInt(1);
                        final int subdirOffset = file.readInt();
                        fixEXIFDirectory(file, subdirOffset, 0);
                        return;
                    }
                }
                ifdOffset = file.readInt();
            }
        }
    }

    /**
     * Fix an EXIF directory in a TIFF file.  Specifically, this means to adjust all value offsets
     * so that they are relative to the beginning of the TIFF file (as is required by the TIFF
     * specification) rather than relative to the start of the EXIF directory (as is the case when
     * in a JPEG file).
     *
     * @param file The TIFF file containing an EXIF directory to fix.
     * @param dirOffset The offset to the start of the EXIF directory.
     * @param parentDirSize The size of the parent EXIF directory, if any.
     */
    private static void fixEXIFDirectory(OrderableRandomAccessFile file,
            long dirOffset, int parentDirSize)
            throws IOException {
        file.seek(dirOffset);
        final int entryCount = file.readUnsignedShort();
        for (int entry = 0; entry < entryCount; ++entry) {
            final int entryOffset =
                    TIFFMetadataReader.calcIFDEntryOffset((int) dirOffset, entry);
            file.seek(entryOffset);
            final int tagID = file.readUnsignedShort();
            final int fieldType = file.readUnsignedShort();
            final int numValues = file.readInt();
            final int byteCount = numValues * EXIF_FIELD_SIZE[fieldType];
            if (byteCount > TIFF_INLINE_VALUE_MAX_SIZE ||
                    tagID == EXIF_IFD_POINTER || tagID == EXIF_GPS_IFD_POINTER) {
                final int origValueOffset = file.readInt();
                final int valueOffset = origValueOffset + (int) dirOffset + parentDirSize;
                file.seek(file.getFilePointer() - TIFF_INT_SIZE);
                file.writeInt(valueOffset);
                switch (tagID) {
                    case EXIF_IFD_POINTER:
                    case EXIF_GPS_IFD_POINTER:
                        fixEXIFDirectory(file, valueOffset, -origValueOffset);
                        break;
                }
            }
        }
    }

    /**
     * Opens a TIFF file.
     *
     * @param fileName The name of the TIFF file to open.
     */
    private void openForWriting(String fileName)
            throws LCImageLibException, UnsupportedEncodingException {
        byte[] fileNameUtf8 = (fileName + '\000').getBytes("UTF-8");
        openForWriting(fileNameUtf8);
    }

    private native void openForWriting(byte[] fileNameUtf8)
            throws LCImageLibException;

    /**
     * Writes a TIFF image as strips.
     *
     * @param image The image to put.
     * @param thread The thread that's doing the writing.
     */
    private void writeImageStriped(RenderedImage image,
            ProgressThread thread)
            throws LCImageLibException {
        final int dataType = image.getSampleModel().getDataType();
        final int bands = image.getSampleModel().getNumBands();
        final int imageWidth = image.getWidth();
        final int imageHeight = image.getHeight();

        setIntField(TIFF_BITS_PER_SAMPLE, dataType == DataBuffer.TYPE_BYTE ? 8 : 16);
        setIntField(TIFF_IMAGE_WIDTH, imageWidth);
        setIntField(TIFF_IMAGE_LENGTH, imageHeight);
        setIntField(
                TIFF_PHOTOMETRIC_INTERPRETATION,
                bands == 4 ? TIFF_PHOTOMETRIC_SEPARATED :
                        bands == 3 ? TIFF_PHOTOMETRIC_RGB :
                                TIFF_PHOTOMETRIC_BLACK_IS_ZERO);
        setIntField(TIFF_PLANAR_CONFIGURATION, TIFF_PLANAR_CONFIGURATION_CHUNKY);
        final int stripHeight = 32;
        setIntField(TIFF_ROWS_PER_STRIP, stripHeight);
        setIntField(TIFF_SAMPLES_PER_PIXEL, bands);

        final ProgressIndicator indicator = ProgressIndicatorFactory.create(thread, imageHeight);

        // Allocate the output buffer only once
        final int type = (dataType == DataBuffer.TYPE_BYTE)
                ? DataBuffer.TYPE_BYTE
                : DataBuffer.TYPE_USHORT;
        final WritableRaster outBuffer = Raster.createInterleavedRaster(
                type, imageWidth, stripHeight, bands * imageWidth, bands,
                LCImageLibUtil.bandOffset(bands), new Point(0, 0));

        for (int y = 0, stripIndex = 0; y < imageHeight; y += stripHeight, stripIndex++) {
            if (thread != null && thread.isCanceled()) {
                return;
            }

            final int currentStripHeight = Math.min(stripHeight, imageHeight - y);

            // Create a child raster of the out buffer for the current strip
            final WritableRaster raster =
                    outBuffer.createWritableChild(0, 0, imageWidth, currentStripHeight, 0, y, null);

            // Prefetch tiles, uses all CPUs
            if (image instanceof PlanarImage) {
                final PlanarImage planarImage = (PlanarImage) image;
                planarImage.getTiles(planarImage.getTileIndices(raster.getBounds()));
            }
            image.copyData(raster);

            final ComponentSampleModel csm = (ComponentSampleModel) raster.getSampleModel();
            final int offset = LCImageLibUtil.min(csm.getBandOffsets());

            writeStrip(dataType, bands, imageWidth, stripIndex, currentStripHeight, raster, offset);
            indicator.incrementBy(currentStripHeight);
        }
        indicator.setIndeterminate(true);
    }

    /**
     * Writes a TIFF image as tiles.
     *
     * @param image The image to put.
     * @param thread The thread that's doing the writing.
     */
    private void writeImageTiled(RenderedImage image,
            ProgressThread thread)
            throws LCImageLibException {
        final int dataType = image.getSampleModel().getDataType();

        setIntField(TIFF_IMAGE_WIDTH, image.getWidth());
        setIntField(TIFF_IMAGE_LENGTH, image.getHeight());
        setIntField(TIFF_BITS_PER_SAMPLE, dataType == DataBuffer.TYPE_BYTE ? 8 : 16);
        setIntField(TIFF_SAMPLES_PER_PIXEL, image.getSampleModel().getNumBands());

        setIntField(TIFF_PLANAR_CONFIGURATION, TIFF_PLANAR_CONFIGURATION_CHUNKY);
        setIntField(TIFF_PHOTOMETRIC_INTERPRETATION, TIFF_PHOTOMETRIC_RGB);

        setIntField(TIFF_TILE_WIDTH, image.getTileWidth());
        setIntField(TIFF_TILE_LENGTH, image.getTileHeight());

        final ProgressIndicator indicator =
                ProgressIndicatorFactory
                        .create(thread, image.getNumXTiles() * image.getNumYTiles());

        for (int tileX = 0; tileX < image.getNumXTiles(); tileX++) {
            for (int tileY = 0; tileY < image.getNumYTiles(); tileY++) {
                if (thread != null && thread.isCanceled()) {
                    return;
                }

                final int tileIndex =
                        computeTile(tileX * image.getTileWidth(), tileY * image.getTileHeight(),
                                0, 0);
                final Raster tile = image.getTile(tileX, tileY);

                writeTile(dataType, tileIndex, tile);
                indicator.incrementBy(1);
            }
        }
        indicator.setIndeterminate(true);
    }

    private void writeStrip(int dataType, int bands, int imageWidth, int stripIndex,
            int currentStripHeight, WritableRaster raster, int offset)
            throws LCImageLibException {
        final DataBuffer db = raster.getDataBuffer();
        final int stripSize;
        final int written;

        if (dataType == DataBuffer.TYPE_BYTE) {
            stripSize = bands * imageWidth * currentStripHeight;
            written = writeStripByte(stripIndex, ((DataBufferByte) db).getData(), offset,
                    stripSize);
        } else {
            stripSize = 2 * bands * imageWidth * currentStripHeight;
            written = writeStripShort(stripIndex, ((DataBufferUShort) db).getData(), offset,
                    stripSize);
        }

        if (written != stripSize) {
            throw new LCImageLibException("something is wrong: " + written + " != " + stripSize);
        }
    }

    private void writeTile(int dataType, int tileIndex, Raster tile)
            throws LCImageLibException {
        final DataBuffer db = tile.getDataBuffer();
        final int tileSize;
        final int written;

        if (dataType == DataBuffer.TYPE_BYTE) {
            final byte[] buffer = ((DataBufferByte) db).getData();
            tileSize = buffer.length;
            written = writeTileByte(tileIndex, buffer, 0, tileSize);
        } else {
            final short[] buffer = ((DataBufferUShort) db).getData();
            tileSize = 2 * buffer.length;
            written = writeTileShort(tileIndex, buffer, 0, tileSize);
        }

        if (written != tileSize) {
            throw new LCImageLibException("something is wrong: " + written + " != " + tileSize);
        }
    }

    /**
     * Encodes and writes a strip to the TIFF image.
     *
     * @param stripIndex The index of the strip to write.
     * @param buf The buffer into which to write the image data.
     * @param offset The offset into the buffer where the image data will begin being placed.
     * @param stripSize The size of the strip.
     * @return Returns the number of bytes written or -1 if there was an error.
     */
    private native int writeStripByte(int stripIndex, byte[] buf, long offset,
            int stripSize)
            throws LCImageLibException;

    /**
     * Encodes and writes a strip to the TIFF image.
     *
     * @param stripIndex The index of the strip to write.
     * @param buf The buffer into which to write the image data.
     * @param offset The offset into the buffer where the image data will begin being placed.
     * @param stripSize The size of the strip.
     * @return Returns the number of bytes written or -1 if there was an error.
     */
    private native int writeStripShort(int stripIndex, short[] buf, long offset,
            int stripSize)
            throws LCImageLibException;

    /**
     * Encodes and writes a tile to the TIFF image.
     *
     * @param tileIndex The index of the tile to write.
     * @param buf The buffer into which to write the image data.
     * @param offset The offset into the buffer where the image data will begin being placed.
     * @param tileSize The size of the tile.
     * @return Returns the number of bytes written or -1 if there was an error.
     */
    private native int writeTileByte(int tileIndex, byte[] buf, long offset,
            int tileSize)
            throws LCImageLibException;

    /**
     * Encodes and writes a tile to the TIFF image.
     *
     * @param tileIndex The index of the tile to write.
     * @param buf The buffer into which to write the image data.
     * @param offset The offset into the buffer where the image data will begin being placed.
     * @param tileSize The size of the tile.
     * @return Returns the number of bytes written or -1 if there was an error.
     */
    private native int writeTileShort(int tileIndex, short[] buf, long offset,
            int tileSize)
            throws LCImageLibException;

    /**
     * The name of the TIFF file to append, if any.
     */
    private final String m_appendFileName;

    /**
     * The height of the image as exported.
     */
    private final int m_exportHeight;

    /**
     * The width of the image as exported.
     */
    private final int m_exportWidth;

    /**
     * The name of the TIFF file.
     */
    private final String m_fileName;

    /**
     * Flag used to remember whether the image has EXIF metadata.
     */
    private boolean m_hasExifMetadata;

    /**
     * The resolution (in pixels per unit) of the image as exported.
     */
    private final int m_resolution;

    /**
     * The resolution unit of the image as exported.
     */
    private final int m_resolutionUnit;

    public static void main(String[] args) {
        try {
            final LCTIFFReader tiff = new LCTIFFReader(args[0]);
            final PlanarImage image = tiff.getImage(null);
            if (image == null) {
                return;
            }

            final LCTIFFWriter writer = new LCTIFFWriter(
                    System.getProperty("java.io.tmpdir") + File.separator + "out.tiff",
                    args[1], image.getWidth(), image.getHeight());
            writer.setStringField(TIFF_SOFTWARE, Version.getApplicationName());
            writer.putImageStriped(image, null);
        } catch (IOException | LCImageLibException | UserCanceledException e) {
            e.printStackTrace();
        }
    }
}
/* vim:set et sw=4 ts=4: */
