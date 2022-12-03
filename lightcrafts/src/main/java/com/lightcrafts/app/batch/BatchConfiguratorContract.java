/*
 * Copyright (C) 2020.     Masahiro Kitagawa
 */

package com.lightcrafts.app.batch;

import com.lightcrafts.image.export.ImageExportOptions;
import com.lightcrafts.image.export.ImageFileExportOptions;

import java.io.File;

interface BatchConfiguratorContract {
    interface ViewActions {
        String getBatchLabelText();

        ImageExportOptions getImageExportOptions();

        String getDirBoxLabel();

        String getDirLabelText();

        void onDirButtonPressed();

        void setStarted();

        void configFor(ImageFileExportOptions options);
    }

    interface View {
        void createAndShowGUI();

        File chooseDirectory(File directory);

        void setDirLabelText(String dirLabelText);
    }
}
