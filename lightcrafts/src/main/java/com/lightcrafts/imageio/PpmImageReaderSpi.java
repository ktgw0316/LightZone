package com.lightcrafts.imageio;

import javax.imageio.ImageReader;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.util.Locale;

public class PpmImageReaderSpi extends ImageReaderSpi {

    static final String vendorName = "The LightZone Project";
    static final String version = "1.0";
    static final String[] names = { "ppm", "pnm" };
    static final String[] suffixes = { "ppm", "pnm" };
    static final String[] mimeTypes = { "image/x-portable-pixmap", "image/x-portable-anymap" };
    static final String readerClassName = "com.lightcrafts.imageio.PpmImageReaderSpi";
    static final String[] writerSpiNames = { "com.lightcrafts.imageio.PpmImageWriterSpi" };

    // Metadata formats, more information below
    static final boolean supportsStandardStreamMetadataFormat = false;
    static final String nativeStreamMetadataFormatName = null;
    static final String nativeStreamMetadataFormatClassName = null;
    static final String[] extraStreamMetadataFormatNames = null;
    static final String[] extraStreamMetadataFormatClassNames = null;
    static final boolean supportsStandardImageMetadataFormat = false;
    static final String nativeImageMetadataFormatName = null;
    static final String nativeImageMetadataFormatClassName = null;
    static final String[] extraImageMetadataFormatNames = null;
    static final String[] extraImageMetadataFormatClassNames = null;

    public PpmImageReaderSpi() {
        super(vendorName, version,
                names, suffixes, mimeTypes,
                readerClassName,
                new Class[] { ImageInputStream.class },
                writerSpiNames,
                supportsStandardStreamMetadataFormat,
                nativeStreamMetadataFormatName,
                nativeStreamMetadataFormatClassName,
                extraStreamMetadataFormatNames,
                extraStreamMetadataFormatClassNames,
                supportsStandardImageMetadataFormat,
                nativeImageMetadataFormatName,
                nativeImageMetadataFormatClassName,
                extraImageMetadataFormatNames,
                extraImageMetadataFormatClassNames);
    }

    @Override
    public String getDescription(Locale locale) {
        return "Portable Pix Map";
    }

    @Override
    public boolean canDecodeInput(Object input) throws IOException {
        if (!(input instanceof ImageInputStream stream)) {
            return false;
        }

        final byte[] b = new byte[2];
        stream.readFully(b);
        final var S1 = new String(b);

        return switch (S1) {
            case "P5", "P6", "P7" -> true;
            default -> false;
        };
    }

    @Override
    public ImageReader createReaderInstance(Object extension) {
        return new PpmImageReader(this);
    }
}
