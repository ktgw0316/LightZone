/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.app.menu;

import com.lightcrafts.app.Application;
import com.lightcrafts.app.ComboFrame;
import com.lightcrafts.ui.browser.view.AbstractImageBrowser;

import java.awt.event.ActionEvent;
import java.io.File;

class BrowserPrintMenuItem extends BrowserMenuItem {
    
    BrowserPrintMenuItem(ComboFrame frame) {
        super(frame, "BrowserPrint");
    }

    public void actionPerformed(ActionEvent event) {
        ComboFrame frame = getComboFrame();
        AbstractImageBrowser browser = getBrowser();
        File file = browser.getLeadSelectedFile();
        if (file != null) {
            Application.print(frame, file);
        }
    }
}
