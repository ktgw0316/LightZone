/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.app.menu;

import com.lightcrafts.app.ComboFrame;
import com.lightcrafts.app.Application;
import com.lightcrafts.ui.browser.view.AbstractImageBrowser;
import com.lightcrafts.prefs.ApplicationMode;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.List;

class SendMenuItem extends BrowserMenuItem {

    SendMenuItem(ComboFrame frame) {
        super(frame, "Send");
    }

    public void actionPerformed(ActionEvent event) {
        ComboFrame frame = getComboFrame();
        AbstractImageBrowser browser = frame.getBrowser();
        List<File> list = browser.getSelectedFiles();
        if (! list.isEmpty()) {
            File[] files = list.toArray(new File[0]);
            Application.send(frame, files);
        }
    }

    void update() {
        if (! ApplicationMode.isBasicMode()) {
            super.update();
        }
        else {
            setEnabled(false);
        }
    }
}
