/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.app.menu;

import com.lightcrafts.app.ComboFrame;
import com.lightcrafts.platform.Platform;
import com.lightcrafts.ui.editor.Document;

import java.awt.event.ActionEvent;
import java.io.File;

class ShowInMenuItem extends DocumentMenuItem {

    ShowInMenuItem(ComboFrame frame) {
        super(frame, "ShowIn");
    }

    public void actionPerformed(ActionEvent event) {
        Document doc = getDocument();
        File file = doc.getFile();
        if (file == null) {
            file = doc.getMetadata().getFile();
        }
        Platform platform = Platform.getPlatform();
        String path = file.getAbsolutePath();
        platform.showFileInFolder(path);
    }
}
