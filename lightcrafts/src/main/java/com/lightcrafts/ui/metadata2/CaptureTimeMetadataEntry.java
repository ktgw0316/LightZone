/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2023-     Masahiro Kitagawa */

package com.lightcrafts.ui.metadata2;

import static com.lightcrafts.ui.metadata2.Locale.LOCALE;
import com.lightcrafts.image.metadata.ImageMetadata;
import com.lightcrafts.image.metadata.CoreDirectory;
import com.lightcrafts.image.metadata.CoreTags;

import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

class CaptureTimeMetadataEntry extends SimpleMetadataEntry {

    private final static DateTimeFormatter formatter =
            DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
                    .withLocale(java.util.Locale.getDefault());

    CaptureTimeMetadataEntry() {
        super(CoreDirectory.class, CoreTags.CORE_CAPTURE_DATE_TIME);
    }

    public String getLabel(ImageMetadata meta) {
        return LOCALE.get("CaptureTimeLabel");
    }

    public String getValue(ImageMetadata meta) {
        final CoreDirectory dir = (CoreDirectory) meta.getDirectoryFor(clazz);
        if (dir != null) {
            final var date = dir.getCaptureDateTime();
            if (date != null) {
                return date.format(formatter);
            }
        }
        return "";
    }
}
