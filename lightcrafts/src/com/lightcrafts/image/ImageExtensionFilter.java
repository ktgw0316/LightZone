/* Copyright (C) 2017-     Masahiro Kitagawa */

package com.lightcrafts.image;

import com.lightcrafts.image.types.ImageType;
import javafx.stage.FileChooser.ExtensionFilter;
import lombok.val;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.TreeSet;

public class ImageExtensionFilter {
    private final ExtensionFilter[] filters;

    private ImageExtensionFilter() {
        val extensions = new TreeSet<String>();
        for (val t : ImageType.getAllTypes()) {
            for (val ext : t.getExtensions()) {
                extensions.add("*." + ext);
            }
        }

        val filterList = new LinkedList<ExtensionFilter>();
        for (val ext : extensions) {
            filterList.add(new ExtensionFilter(ext, ext, ext.toUpperCase()));
        }

        val allExtensions = new ArrayList<String>();
        for (val ext : extensions) {
            allExtensions.add(ext);
            allExtensions.add(ext.toUpperCase());
        }
        filterList.add(0, new ExtensionFilter("Image Files", allExtensions)); // TODO: l10n

        filters = filterList.toArray(new ExtensionFilter[filterList.size()]);
    }

    // Initialization-on-demand holder idiom
    private static class LazyHolder {
        // TODO: l10n
        private static final ExtensionFilter[] FILTERS = new ImageExtensionFilter().filters;
    }

    public static ExtensionFilter[] getFilter() {
        return LazyHolder.FILTERS;
    }
}
/* vim:set et sw=4 ts=4: */
