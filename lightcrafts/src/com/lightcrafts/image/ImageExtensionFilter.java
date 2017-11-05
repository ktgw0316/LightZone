/* Copyright (C) 2017-     Masahiro Kitagawa */

package com.lightcrafts.image;

import com.lightcrafts.image.types.ImageType;
import javafx.stage.FileChooser.ExtensionFilter;
import lombok.val;

import java.util.ArrayList;
import java.util.List;

public class ImageExtensionFilter {
    private final List<String> extensions = new ArrayList<String>();

    private ImageExtensionFilter() {
        for (val t : ImageType.getAllTypes()) {
            for (val ext : t.getExtensions()) {
                extensions.add("*." + ext);
            }
        }
    }

    // Initialization-on-demand holder idiom
    private static class LazyHolder {
        // TODO: l10n
        private static final ExtensionFilter FILTER =
                new ExtensionFilter("Image Files", new ImageExtensionFilter().extensions);
    }

    public static ExtensionFilter getFilter() {
        return LazyHolder.FILTER;
    }
}
/* vim:set et sw=4 ts=4: */
