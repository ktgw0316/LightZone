/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.browser.model;

import com.lightcrafts.image.BadImageFileException;
import com.lightcrafts.image.ImageInfo;
import com.lightcrafts.image.metadata.ImageMetadata;
import com.lightcrafts.image.types.*;

import java.io.File;
import java.io.IOException;

/**
 * ImageDatums have types that are something like ImageTypes.  The main
 * distinctions are whether LZN data are defined, and how the data are encoded
 * in the file.
 * <p>
 * These singleton instances of ImageDatumType are defined:
 *
 *     RAW
 *     LZN
 *     JPEG
 *     TIFF
 *     JPEG sidecar
 *     TIFF sidecar
 *     Multilayer TIFF
 *     Unknown
 */
public abstract class ImageDatumType {

    public final static ImageDatumType RawType = new ImageDatumType("RAW") {
        public boolean isInstance(ImageDatum datum) {
            ImageMetadata meta = datum.getMetadata(true);
            ImageType type = meta.getImageType();
            return (type instanceof RawImageType);
        }
        public boolean hasLznData() {
            return false;
        }
    };

    public final static ImageDatumType LznType = new ImageDatumType("LZN") {
        public boolean isInstance(ImageDatum datum) {
            ImageMetadata meta = datum.getMetadata(true);
            ImageType type = meta.getImageType();
            return (type instanceof LZNImageType);
        }
        public boolean hasLznData() {
            return true;
        }
    };

    public final static ImageDatumType JpegType = new ImageDatumType("JPEG") {
        public boolean isInstance(ImageDatum datum) {
            ImageMetadata meta = datum.getMetadata(true);
            ImageType type = meta.getImageType();
            if (type instanceof JPEGImageType) {
                LznEncoding encoding = getLznEncoding(datum);
                if (encoding == null) {
                    return true;
                }
            }
            return false;
        }
        public boolean hasLznData() {
            return false;
        }
    };

    public final static ImageDatumType TiffType = new ImageDatumType("TIFF") {
        public boolean isInstance(ImageDatum datum) {
            ImageMetadata meta = datum.getMetadata(true);
            ImageType type = meta.getImageType();
            if (type instanceof TIFFImageType) {
                LznEncoding encoding = getLznEncoding(datum);
                if (encoding == null) {
                    return true;
                }
            }
            return false;
        }
        public boolean hasLznData() {
            return false;
        }
    };

    public final static ImageDatumType JpegSidecarType =
        new ImageDatumType("JPEG-LZN") {
            public boolean isInstance(ImageDatum datum) {
                LznEncoding encoding = getLznEncoding(datum);
                return encoding == LznEncoding.JPEG;
            }
            public boolean hasLznData() {
                return true;
            }
        };

    public final static ImageDatumType TiffSidecarType =
        new ImageDatumType("TIFF-LZN") {
            public boolean isInstance(ImageDatum datum) {
                LznEncoding encoding = getLznEncoding(datum);
                return encoding == LznEncoding.TIFF_Sidecar;
            }
            public boolean hasLznData() {
                return true;
            }
        };

    public final static ImageDatumType TiffMultilayerType =
        new ImageDatumType("TIFF-MULTI") {
            public boolean isInstance(ImageDatum datum) {
                LznEncoding encoding = getLznEncoding(datum);
                return encoding == LznEncoding.TIFF_Multilayer;
            }
            public boolean hasLznData() {
                return true;
            }
        };

    public final static ImageDatumType UnknownType =
        new ImageDatumType("???") {
            public boolean isInstance(ImageDatum datum) {
                return true;
            }
            public boolean hasLznData() {
                return false;
            }
        };

    private String name;

    private ImageDatumType(String name) {
        this.name = name;
    }

    static ImageDatumType getTypeOf(ImageDatum datum) {
        // We could just iterate over all the instances looking for a match,
        // but instead we use special-case logic to minimize calls to
        // getLznEncoding().
        if (RawType.isInstance(datum)) {
            return RawType;
        }
        if (LznType.isInstance(datum)) {
            return LznType;
        }
        LznEncoding encoding = getLznEncoding(datum);
        if (encoding == null) {
            if (JpegType.isInstance(datum)) {
                return JpegType;
            }
            if (TiffType.isInstance(datum)) {
                return TiffType;
            }
            return UnknownType;
        }
        switch (encoding) {
            case LZN:
                return LznType;
            case JPEG:
                return JpegSidecarType;
            case TIFF_Sidecar:
                return TiffSidecarType;
            case TIFF_Multilayer:
                return TiffMultilayerType;
        }
        return UnknownType;
    }

    public abstract boolean isInstance(ImageDatum datum);

    public abstract boolean hasLznData();

    public String toString() {
        return name;
    }

    private enum LznEncoding { LZN, JPEG, TIFF_Sidecar, TIFF_Multilayer }

    // Figure out the type of LZN encoding used in the given ImageDatum, or
    // return null if this ImageDatum does not contain any readable LZN data.
    private static LznEncoding getLznEncoding(ImageDatum datum) {
        ImageMetadata meta = datum.getMetadata(true);
        ImageType type = meta.getImageType();
        if (type instanceof LZNImageType) {
            return LznEncoding.LZN;
        }
        if (type instanceof SidecarJPEGImageType) {
            return LznEncoding.JPEG;
        }
        if (type instanceof SidecarTIFFImageType) {
            return LznEncoding.TIFF_Sidecar;
        }
        if (type instanceof MultipageTIFFImageType) {
            return LznEncoding.TIFF_Multilayer;
        }
        return null;
    }
}
