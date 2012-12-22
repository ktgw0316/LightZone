/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.metadata2;

import static com.lightcrafts.ui.metadata2.Locale.LOCALE;
import com.lightcrafts.image.metadata.ImageMetadata;
import com.lightcrafts.image.types.ImageType;
import com.lightcrafts.image.types.LZNImageType;
import com.lightcrafts.image.ImageInfo;

class VersionSizeMetadataEntry extends MetadataEntry {

    public String getLabel(ImageMetadata meta) {
        return LOCALE.get("VersionSizeLabel");
    }

    // If the file is a saved document in an image file format (sidecar JPEG
    // or TIFF, or multilayer TIFF), then show the dimensions of the sidecar
    // image.  Otherwise, show nothing.
    public String getValue(ImageMetadata meta) {
        int width = meta.getOriginalImageWidth();
        int height = meta.getOriginalImageHeight();
        if ((width > 0) && (height > 0)) {
            // It's a saved document.  Is it an image?
            ImageType type = meta.getImageType();
            if (type != LZNImageType.INSTANCE) {
                // Yes, it's an image.
                width = meta.getImageWidth();
                height = meta.getImageHeight();
                String value = LOCALE.get("EditSizeValue", width, height);
                return value;
            }
        }
        return "";
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
}
