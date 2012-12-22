/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.app;

import com.lightcrafts.app.other.OtherApplication;
import com.lightcrafts.image.ImageInfo;
import com.lightcrafts.image.export.ImageExportOptions;
import com.lightcrafts.image.metadata.ImageMetadata;
import com.lightcrafts.image.types.JPEGImageType;
import com.lightcrafts.image.types.LZNImageType;
import com.lightcrafts.image.types.TIFFImageType;
import com.lightcrafts.model.Engine;
import com.lightcrafts.ui.editor.Document;
import com.lightcrafts.ui.export.SaveOptions;
import com.lightcrafts.utils.LightCraftsException;

import java.awt.*;
import java.io.File;
import java.io.IOException;

// Glue between Paul's OtherApplication.createExportOptions() and
// Application.save().

public class OtherApplicationShim {

    // Used in the save logic, and also by the SaveMenuItem to decide whether
    // to update its text.
    public static boolean shouldSaveDirectly(Document doc) {
        final OtherApplication app = (OtherApplication) doc.getSource();
        if (app != null) {
            final ImageInfo info = getImageInfo(doc);
            return app.shouldSaveDirectly(info);
        }
        return false;
    }

    static SaveOptions createExportOptions(Document doc) {
        final OtherApplication app = (OtherApplication) doc.getSource();
        final ImageInfo info = getImageInfo(doc);
        final Engine engine = doc.getEngine();
        final Dimension size = engine.getNaturalSize();
        try {
            final ImageExportOptions export =
                app.createExportOptions(info, size);
            if (export != null) {
                final SaveOptions options = getSaveOptions(export);
                options.setShouldSaveDirectly( true );
                return options;
            }
        }
        catch (IOException e) {
            // return null
        }
        catch (LightCraftsException e) {
            // return null
        }
        return null;
    }

    private static ImageInfo getImageInfo(Document doc) {
        final ImageMetadata meta = doc.getMetadata();
        final File file = meta.getFile();
        return ImageInfo.getInstanceFor(file);
    }

    private static SaveOptions getSaveOptions(ImageExportOptions export) {
        if (export instanceof LZNImageType.ExportOptions) {
            return SaveOptions.createLzn(export.getExportFile());
        }
        if (export instanceof JPEGImageType.ExportOptions) {
            return SaveOptions.createSidecarJpeg(export);
        }
        if (export instanceof TIFFImageType.ExportOptions) {
            final TIFFImageType.ExportOptions tiffOptions =
                (TIFFImageType.ExportOptions) export;
            if (tiffOptions.multilayer.getValue()) {
                return SaveOptions.createMultilayerTiff(export);
            }
            return SaveOptions.createSidecarTiff(export);
        }
        return null;
    }
}
