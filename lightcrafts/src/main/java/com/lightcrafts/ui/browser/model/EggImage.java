/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.browser.model;

import com.lightcrafts.image.metadata.CoreDirectory;
import com.lightcrafts.image.metadata.ImageMetadata;
import com.lightcrafts.image.metadata.ImageMetadataDirectory;
import com.lightcrafts.image.metadata.values.DateMetaValue;
import com.lightcrafts.image.metadata.values.ImageMetaValue;
import com.lightcrafts.image.metadata.values.StringMetaValue;
import com.lightcrafts.image.metadata.values.UnsignedLongMetaValue;

import static com.lightcrafts.image.metadata.CoreTags.*;

import javax.imageio.ImageIO;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

/**
 * A global manager for the egg images that serve as placeholders during
 * ImageTasks.
 */
class EggImage {

    // The universal placeholder image
    private final static RenderedImage EggImage;

    // A map from Integer sizes to scaled copies of EggImage
    private final static HashMap<Integer, RenderedImage> EggImages;

    static {
        try {
            EggImage = ImageIO.read(
                ImageDatum.class.getResource("resources/EggFrame.jpg")
            );
        }
        catch (IOException e) {
            throw new RuntimeException("Couldn't initialize EggImage", e);
        }
        int size = Math.max(EggImage.getWidth(), EggImage.getHeight());
        EggImages = new HashMap<Integer, RenderedImage>();
        EggImages.put(size, EggImage);
    }

    // Return the egg image constrained to the given size.  Scaled egg
    // images are cached, keyed by size.
    static RenderedImage getEggImage(int size) {
        Integer key = size;
        if (EggImages.containsKey(key)) {
            return EggImages.get(key);
        }
        else {
            RenderedImage egg = Thumbnailer.constrainImage(EggImage, size);
            EggImages.put(key, egg);
            return egg;
        }
    }

    // Construct a lightweight placeholder ImageMetadata object from
    // information in the File.
    static ImageMetadata getEggMetadata(File file) {

        ImageMetadata meta = new ImageMetadata();

        ImageMetadataDirectory core =
            meta.getDirectoryFor(CoreDirectory.class, true);

        String path = file.getParent();
        if (path == null) {
            path = "";
        }
        ImageMetaValue pathValue = new StringMetaValue(path);
        core.putValue(CORE_DIR_NAME, pathValue);

        String name = file.getName();
        ImageMetaValue nameValue = new StringMetaValue(name);
        core.putValue(CORE_FILE_NAME, nameValue);

        long length = file.length();
        ImageMetaValue lengthValue = new UnsignedLongMetaValue(length);
        core.putValue(CORE_FILE_SIZE, lengthValue);

        long time = file.lastModified();
        ImageMetaValue timeValue = new DateMetaValue(time);
        core.putValue(CORE_FILE_DATE_TIME, timeValue);

        return meta;
    }
}
