/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.metadata2;

import com.lightcrafts.image.ImageInfo;
import com.lightcrafts.image.metadata.ImageMetadata;
import com.lightcrafts.image.metadata.ImageOrientation;
import com.lightcrafts.ui.editor.DocumentReader;
import static com.lightcrafts.ui.metadata2.Locale.LOCALE;

import java.io.File;

class EditSizeMetadataEntry extends MetadataEntry {

    public String getLabel(ImageMetadata meta) {
        return LOCALE.get("EditSizeLabel");
    }

    // If the file is a saved document, show the dimensions of the original
    // image.  Otherwise, show the image dimensions of the file itself. 
    public String getValue(ImageMetadata meta) {
        ImageMetadata origMeta = getOriginalImageMetadata(meta);
        if (origMeta != null) {
            return getDimensionText(origMeta);
        }
        else {
            return getDimensionText(meta);
        }
    }

    public boolean isEditable(ImageInfo info) {
        return false;
    }

    public boolean isValidValue(ImageMetadata meta, String value) {
        return true;
    }

    public void setValue(ImageMetadata meta, String value) {
        // readonly
    }

    private static String getDimensionText(ImageMetadata meta) {
        int width = meta.getImageWidth();
        int height = meta.getImageHeight();
        String value = "";
        if ((width > 0) && (height > 0)) {
            ImageOrientation orient = meta.getOrientation();
            switch (orient) {
                case ORIENTATION_90CCW:
                case ORIENTATION_90CCW_VFLIP:
                case ORIENTATION_90CW:
                case ORIENTATION_90CW_VFLIP:
                    value = LOCALE.get("EditSizeValue", height, width);
                    break;
                case ORIENTATION_180:
                case ORIENTATION_LANDSCAPE:
                case ORIENTATION_SEASCAPE:
                case ORIENTATION_VFLIP:
                case ORIENTATION_UNKNOWN:
                default:
                    value = LOCALE.get("EditSizeValue", width, height);
                    break;
            }
        }
        return value;
    }

    // Attempt to interpret the file corresponding to the given metadata
    // object as a saved document, identify the original image for the
    // document, extract metadata from there, and return it instead.
    private static ImageMetadata getOriginalImageMetadata(ImageMetadata meta) {
        try {
            File file = meta.getFile();
            DocumentReader.Interpretation interp = DocumentReader.read(file);
            if ((interp != null) && (interp.imageFile != null)) {
                file = interp.imageFile;
                ImageInfo info = ImageInfo.getInstanceFor(file);
                meta = info.getMetadata();
                return meta;
            }
        }
        catch (Throwable e) {
            // can't find LZN data, so return null
        }
        return null;
    }
}
