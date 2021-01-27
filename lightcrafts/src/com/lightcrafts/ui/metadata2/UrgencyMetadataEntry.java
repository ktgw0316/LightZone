/*
 * Copyright (c) 2020. Masahiro Kitagawa
 */

package com.lightcrafts.ui.metadata2;

import com.lightcrafts.image.ImageInfo;
import com.lightcrafts.image.metadata.CoreDirectory;
import com.lightcrafts.image.metadata.CoreTags;
import com.lightcrafts.image.metadata.ImageMetadata;
import com.lightcrafts.image.metadata.values.ImageMetaValue;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jetbrains.annotations.NotNull;

public class UrgencyMetadataEntry extends SimpleMetadataEntry {

    @RequiredArgsConstructor
    private static class UrgencyObject {
        final ImageMetaValue value;

        @Override
        public String toString() {
            // A zero value results from clearUrgency().
            return (value != null) && (value.getIntValue() > 0)
                    ? value.toString()
                    : "";
        }
    }

    UrgencyMetadataEntry() {
        super(CoreDirectory.class, CoreTags.CORE_URGENCY);
    }

    @Override
    public String getLabel(ImageMetadata meta) {
        return "Color Label"; // TODO: LOCALE.get("ColorLabel");
    }

    @Override
    public UrgencyObject getValue(ImageMetadata meta) {
        val dir = meta.getDirectoryFor(clazz);
        return dir != null
                ? new UrgencyObject(dir.getValue(tagID))
                : new UrgencyObject(null);
    }

    @Override
    public boolean isEditable(@NotNull ImageInfo info) {
        return info.canWriteMetadata();
    }

    @Override
    public boolean isValidValue(ImageMetadata meta, String value) {
        try {
            int i = Integer.parseInt(value);
            return 0 <= i && i <= 8;
        }
        catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    public void setValue(ImageMetadata meta, String value) {
        // Interpret as a decimal formatted number:
        try {
            final int i = Integer.parseInt(value);
            if (0 < i && i <= 5) {
                meta.setUrgency(i);
            } else if (i > 8) {
                meta.setUrgency(8);
            } else {
                meta.clearUrgency();
            }
        } catch (NumberFormatException e) {
            meta.clearUrgency();
        }
    }
}
