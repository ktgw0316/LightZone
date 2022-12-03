/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.app;

import com.lightcrafts.ui.browser.view.AbstractImageBrowser;
import com.lightcrafts.ui.browser.view.ImageBrowserEvent;
import com.lightcrafts.ui.browser.view.ImageBrowserListener;

import javax.swing.*;

/**
 * A FrameButton that listens to the current frame's browser and updates its
 * enabled state according to the current browser selection.
 */
abstract class BrowserButton
    extends FrameButton implements ImageBrowserListener
{
    private AbstractImageBrowser browser;

    protected BrowserButton(ComboFrame frame, String text) {
        super(frame, text);
    }

    protected BrowserButton(ComboFrame frame, Icon icon) {
        super(frame, icon);
    }

    void updateButton() {
        if (browser != null) {
            browser.removeBrowserListener(this);
        }
        ComboFrame frame = getComboFrame();
        browser = frame.getBrowser();
        browser.addBrowserListener(this);
        updateEnabled();
    }

    void updateEnabled() {
        boolean empty = browser.getSelectedFiles().isEmpty();
        setEnabled(! empty);
    }

    public void selectionChanged(ImageBrowserEvent event) {
        updateEnabled();
    }

    public void imageDoubleClicked(ImageBrowserEvent event) {
    }

    public void browserError(String message) {
    }
}
