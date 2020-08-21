/*
 * Copyright (C) 2020-     Masahiro Kitagawa
 */

package com.lightcrafts.app.batch;

import com.lightcrafts.image.export.ImageExportOptions;
import com.lightcrafts.image.export.ImageFileExportOptions;
import com.lightcrafts.ui.base.BasePresenter;
import lombok.Getter;
import lombok.val;

import static com.lightcrafts.app.batch.Locale.LOCALE;

class BatchConfiguratorPresenter extends BasePresenter<BatchConfiguratorContract.View>
        implements BatchConfiguratorContract.ViewActions {
    final private BatchConfiguratorModel model;
    final private BatchConfig config;

    @Getter
    final private String batchLabelText;

    @Getter
    final private ImageExportOptions imageExportOptions;

    @Getter
    final private String dirBoxLabel;

    @Getter
    private String dirLabelText;

    BatchConfiguratorPresenter(BatchConfiguratorModel model) {
        this.model = model;
        config = model.config;

        dirLabelText = config.directory.getName();
        batchLabelText = config.name;
        imageExportOptions = config.export;

        dirBoxLabel = LOCALE.get(model.isBatchExport
                ? "BatchConfExportOutputLabel"
                : "BatchConfSaveOutputLabel");
    }

    public void onDocumentUpdate() {
        config.name = batchLabelText;
    }

    @Override
    public void onDirButtonPressed() {
        val directory = mView.chooseDirectory(config.directory);

        if (directory != null) {
            config.directory = directory;
            dirLabelText = directory.getName();
            mView.setDirLabelText(dirLabelText);
        }
    }

    @Override
    public void setStarted() {
        model.started = true;
    }

    @Override
    public void configFor(ImageFileExportOptions options) {
        model.configFor(options);
    }
}
