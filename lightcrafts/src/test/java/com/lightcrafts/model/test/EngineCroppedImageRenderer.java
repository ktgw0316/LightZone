/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.model.test;

import com.lightcrafts.image.ImageInfo;
import com.lightcrafts.image.metadata.ImageMetadata;
import com.lightcrafts.model.CropBounds;
import com.lightcrafts.model.Engine;
import com.lightcrafts.model.EngineFactory;
import com.lightcrafts.model.Scale;

import javax.swing.*;
import java.awt.*;
import java.io.File;

class EngineCroppedImageRenderer implements CroppedImageRenderer {

    private Engine engine;
    private JComponent image;

    EngineCroppedImageRenderer(String path) throws Exception {
        File file = new File(path);
        ImageInfo info = ImageInfo.getInstanceFor(file);
        ImageMetadata meta = info.getMetadata();
        engine = EngineFactory.createEngine(meta, null, null);
        image = (JComponent) engine.getComponent();
        image.setOpaque(false);
    }

    public Component getComponent() {
        return image;
    }

    public void cropBoundsChanged(
        CropBounds bounds, Scale scale, boolean isChanging
    ) {
        if (! isChanging) {
            engine.setCropBounds(bounds);
            engine.setScale(scale);
        }
    }
}
