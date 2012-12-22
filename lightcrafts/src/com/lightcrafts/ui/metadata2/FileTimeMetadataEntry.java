/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.metadata2;

import static com.lightcrafts.ui.metadata2.Locale.LOCALE;
import com.lightcrafts.image.metadata.CoreDirectory;
import com.lightcrafts.image.metadata.CoreTags;
import com.lightcrafts.image.metadata.ImageMetadata;
import com.lightcrafts.utils.TextUtil;

import java.util.Date;
import java.text.DateFormat;

class FileTimeMetadataEntry extends SimpleMetadataEntry {

    private final static DateFormat Format = DateFormat.getDateTimeInstance();

    FileTimeMetadataEntry() {
        super(CoreDirectory.class, CoreTags.CORE_FILE_DATE_TIME);
    }

    public String getLabel(ImageMetadata meta) {
        return LOCALE.get("FileTimeLabel");
    }

    public String getValue(ImageMetadata meta) {
        final CoreDirectory dir = (CoreDirectory) meta.getDirectoryFor(clazz);
        if (dir != null) {
            final Date date = dir.getFileDateTime();
            if (date != null) {
                return TextUtil.dateFormat( Format, date );
            }
        }
        return "";
    }
}
