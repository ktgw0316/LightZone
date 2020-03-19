/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.model;

import com.lightcrafts.image.BadImageFileException;
import com.lightcrafts.image.ColorProfileException;
import com.lightcrafts.image.UnknownImageTypeException;
import com.lightcrafts.image.ImageInfo;
import com.lightcrafts.image.metadata.ImageMetadata;
import com.lightcrafts.model.ImageEditor.ImageEditorEngine;
import com.lightcrafts.utils.UserCanceledException;
import com.lightcrafts.utils.thread.ProgressThread;

import java.io.IOException;
import java.awt.image.RenderedImage;

/**
 * A factory of Engines for image processing.
 */
public class EngineFactory {

    /**
     * Create a new Engine from image file metadata.
     * @param meta A pointer to an image file.
     * @param exportInfo An ImageInfo telling what image should be used
     * to define output metadata, in case someone calls Engine.write().
     * @param thread A ProgressThread for reporting incremental progress.
     * Null is fine.
     * @return An Engine that can operate on and display the image data.
     */
    public static Engine createEngine(
        ImageMetadata meta, ImageInfo exportInfo, ProgressThread thread
    )
        throws BadImageFileException, ColorProfileException, IOException,
               UnknownImageTypeException, UserCanceledException
    {
        return new ImageEditorEngine(meta, exportInfo, thread);
    }

    public static Engine createEngine(RenderedImage image)  {
        return new ImageEditorEngine(image);
    }
}
