/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2019-     Masahiro Kitagawa */

package com.lightcrafts.app.batch;

import com.lightcrafts.platform.Platform;
import lombok.val;

import javax.swing.*;
import java.awt.*;
import java.io.File;

/**
 * This conducts the dialog that must preceed TemplateApplicator.  It lets a
 * user confirm the list of files that will be processed, set a flag to
 * specify whether files should be created or overwritten, and if files will
 * be created, it also lets the user specify a name pattern for the new
 * files.
 */

public class BatchConfigurator {

    public static BatchConfig showDialog(
        File[] files, final Frame parent, boolean isBatchExport
    ) {
        val model = new BatchConfiguratorModel(files, isBatchExport);
        val presenter = new BatchConfiguratorPresenter(model);
        final BatchConfiguratorContract.View view =
                new BatchConfiguratorView(presenter, parent);
        view.createAndShowGUI();
        return model.started ? model.config : null;
    }

    public static void main(String[] args) throws Exception {
        val files = new File[100];
        for (int n = 0; n < 100; n++) {
            files[n] = File.createTempFile("test", ".lzn");
            files[n].deleteOnExit();
        }
        UIManager.setLookAndFeel(Platform.getPlatform().getLookAndFeel());
        EventQueue.invokeLater(() -> {
            showDialog(files, null, false);
            System.exit(0);
        });
    }
}
