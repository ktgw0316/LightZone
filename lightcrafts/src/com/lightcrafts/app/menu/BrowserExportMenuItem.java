/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.app.menu;

import com.lightcrafts.app.Application;
import com.lightcrafts.app.ComboFrame;
import com.lightcrafts.ui.browser.view.AbstractImageBrowser;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.List;

class BrowserExportMenuItem extends BrowserMenuItem {
    
    BrowserExportMenuItem(ComboFrame frame) {
        super(frame, "BrowserExport");
    }

    public void actionPerformed(ActionEvent event) {
        ComboFrame frame = getComboFrame();
        AbstractImageBrowser browser = getBrowser();
        List<File> list = browser.getSelectedFiles();
        File[] files = list.toArray(new File[0]);
        if (files.length > 1) {
            Application.export(frame, files);
        }
        else {
            Application.export(frame, files[0]);
        }
    }

}
