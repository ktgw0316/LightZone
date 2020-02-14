/*
 * Copyright (c) 2020. Masahiro Kitagawa
 */

package com.lightcrafts.image.metadata.providers;

public interface ColorLabelProvider extends ImageMetadataProvider {

    /**
     * Gets the color label number of an image: 1-8.
     *
     * @return Returns the number or 0 if it's unavailable.
     */
    int getColorLabel();

}
