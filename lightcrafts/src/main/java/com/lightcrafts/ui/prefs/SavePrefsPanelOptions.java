/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.prefs;

import com.lightcrafts.application.prefs.SaveFormatPreferenceWorkflow;
import com.lightcrafts.image.export.ImageExportOptions;

import java.util.List;

final class SavePrefsPanelOptions {

    private SavePrefsPanelOptions() {
    }

    static List<ImageExportOptions> getExportOptions(
        ImageExportOptions defaultOptions
    ) {
        return SaveFormatPreferenceWorkflow.getExportOptions(defaultOptions);
    }
}
