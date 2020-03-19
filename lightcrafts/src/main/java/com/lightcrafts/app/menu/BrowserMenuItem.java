/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.app.menu;

import com.lightcrafts.app.ComboFrame;
import com.lightcrafts.ui.browser.view.AbstractImageBrowser;

import java.io.File;
import java.util.List;

/**
 * An UpdatableMenuItem which simply enables and disables depending on
 * whether there is a current browser selection.
 */
abstract class BrowserMenuItem extends UpdatableMenuItem {

    protected BrowserMenuItem(ComboFrame frame, String key) {
        super(frame, key);
    }

    AbstractImageBrowser getBrowser() {
        ComboFrame frame = getComboFrame();
        return frame.getBrowser();
    }

    void update() {
        ComboFrame frame = getComboFrame();
        if (frame != null) {
            boolean visible = frame.isBrowserVisible();
            boolean empty = getSelection().isEmpty();
            setEnabled(visible && ! empty);
        }
    }

    List<File> getSelection() {
        return getBrowser().getSelectedFiles();
    }
}
