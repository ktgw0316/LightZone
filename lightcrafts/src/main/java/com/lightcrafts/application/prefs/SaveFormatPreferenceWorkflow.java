/*
 * Copyright (c) 2026. Masahiro Kitagawa
 */

package com.lightcrafts.application.prefs;

import com.lightcrafts.domain.prefs.SaveFormat;
import com.lightcrafts.domain.prefs.SaveFormatPreference;
import com.lightcrafts.image.export.ImageExportOptions;
import com.lightcrafts.image.types.ImageType;
import com.lightcrafts.image.types.JPEGImageType;
import com.lightcrafts.image.types.TIFFImageType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Arrays;
import java.util.List;

public final class SaveFormatPreferenceWorkflow {

    private SaveFormatPreferenceWorkflow() {
    }

    public static @NotNull @Unmodifiable List<ImageExportOptions> getExportOptions(
            @NotNull ImageExportOptions defaultOptions) {
        final SaveFormat defaultFormat = saveFormatFor(defaultOptions.getImageType());
        return Arrays.stream(SaveFormat.values())
                .map(format -> {
                    if (SaveFormatPreference.usesDefaultOptions(defaultFormat, format)) {
                        return defaultOptions;
                    } else {
                        return imageTypeFor(format).newExportOptions();
                    }
                })
                .toList();
    }

    @NotNull
    private static SaveFormat saveFormatFor(ImageType imageType) {
        if (imageType == TIFFImageType.INSTANCE) {
            return SaveFormat.TIFF;
        }
        if (imageType == JPEGImageType.INSTANCE) {
            return SaveFormat.JPEG;
        }
        throw new IllegalArgumentException(
                "Unsupported save image type: " + imageType.getName());
    }

    @NotNull
    private static ImageType imageTypeFor(SaveFormat format) {
        return switch (format) {
            case TIFF -> TIFFImageType.INSTANCE;
            case JPEG -> JPEGImageType.INSTANCE;
        };
    }
}
