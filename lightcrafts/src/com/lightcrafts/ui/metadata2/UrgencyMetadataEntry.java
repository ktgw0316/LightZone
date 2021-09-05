/*
 * Copyright (c) 2020. Masahiro Kitagawa
 */

package com.lightcrafts.ui.metadata2;

import com.lightcrafts.image.ImageInfo;
import com.lightcrafts.image.metadata.CoreDirectory;
import com.lightcrafts.image.metadata.CoreTags;
import com.lightcrafts.image.metadata.ImageMetadata;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jetbrains.annotations.NotNull;

public class UrgencyMetadataEntry extends SimpleMetadataEntry {

    @RequiredArgsConstructor
    static class UrgencyObject {
        @Getter
        final Integer value;

        @Override
        public String toString() {
            return value.toString();
        }
    }

    UrgencyMetadataEntry() {
        super(CoreDirectory.class, CoreTags.CORE_URGENCY);
    }

    @Override
    public String getLabel(ImageMetadata meta) {
        return "Color Label"; // TODO: LOCALE.get("ColorLabel");
    }

    @NotNull
    @Override
    public UrgencyObject getValue(ImageMetadata meta) {
        val dir = meta.getDirectoryFor(clazz);
        val metaValue = dir != null ? dir.getValue(tagID) : null;
        val value = metaValue != null ? metaValue.getIntValue() : 0;
        return new UrgencyObject(value);
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
        if (value == null) return;

        // Interpret as a decimal formatted number:
        try {
            final int i = Integer.parseInt(value);
            if (0 < i && i <= 8) {
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
