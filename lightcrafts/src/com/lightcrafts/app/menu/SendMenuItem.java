/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.app.menu;

import com.lightcrafts.app.ComboFrame;
import com.lightcrafts.app.Application;
import com.lightcrafts.ui.browser.view.AbstractImageBrowser;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.List;

class SendMenuItem extends BrowserMenuItem {

    SendMenuItem(ComboFrame frame) {
        super(frame, "Send");
    }

    public void actionPerformed(ActionEvent event) {
        ComboFrame frame = getComboFrame();
        if (frame == null) {
            return;
        }
        AbstractImageBrowser browser = frame.getBrowser();
        List<File> list = browser.getSelectedFiles();
        if (! list.isEmpty()) {
            File[] files = list.toArray(new File[0]);
            Application.send(frame, files);
        }
    }

}
