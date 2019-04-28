/*
 * Copyright (C) 2020-     Masahiro Kitagawa
 */

package com.lightcrafts.app.batch;

import com.lightcrafts.image.export.ImageFileExportOptions;
import lombok.Getter;

import java.io.File;

class BatchConfiguratorModel {
    final boolean isBatchExport;

    // User clicked "start" (not "cancel", not disposed the dialog):
    boolean started = false;

    final String saveKey;

    @Getter
    final BatchConfig config;

    BatchConfiguratorModel(File[] files, boolean isBatchExport) {
        this.isBatchExport = isBatchExport;

        config = new BatchConfig();

        // Remember the last values:
        saveKey = isBatchExport ? "Export" : "Apply";
        config.restoreFromPrefs(saveKey);

        // For regular template application, override the restored output
        // directory with the directory of the first input file.  (Batch
        // export keeps its sticky output directory.)
        if (!isBatchExport || config.directory == null) {
            config.directory = files[0].getParentFile();
        }
    }

    void configFor(ImageFileExportOptions options) {
        if (!started) {
            return;
        }
        config.export = options;
        // Remember choices for next time:
        config.saveToPrefs(saveKey);
    }
}
