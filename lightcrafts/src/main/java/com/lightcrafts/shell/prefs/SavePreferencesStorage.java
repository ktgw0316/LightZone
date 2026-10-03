/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.shell.prefs;

import com.lightcrafts.image.export.ImageExportOptions;
import com.lightcrafts.image.types.TIFFImageType;
import com.lightcrafts.ui.export.SaveOptions;

public final class SavePreferencesStorage {

    private SavePreferencesStorage() {
    }

    public static ImageExportOptions getDefaultExportOptions() {
        SaveOptions options = SaveOptions.getDefaultSaveOptions();
        return SaveOptions.getExportOptions(options);
    }

    public static void setDefaultExportOptions(ImageExportOptions exportOptions) {
        SaveOptions options = exportOptions.getImageType() == TIFFImageType.INSTANCE
            ? SaveOptions.createSidecarTiff(exportOptions)
            : SaveOptions.createSidecarJpeg(exportOptions);
        SaveOptions.setDefaultSaveOptions(options);
    }
}
