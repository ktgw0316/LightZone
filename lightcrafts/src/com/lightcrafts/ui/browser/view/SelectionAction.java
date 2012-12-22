/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.browser.view;

import com.lightcrafts.ui.browser.model.ImageDatum;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

/**
 * An AbstractAction that listens to browser selection events and enables and
 * disables according to whether the selection is not empty.
 */
abstract class SelectionAction
    extends AbstractAction implements ImageBrowserListener
{
    private List<ImageDatum> selection;

    private KeyStroke keystroke;

    SelectionAction(
        String name,
        AbstractImageBrowser browser,
        KeyStroke key,
        boolean dynamic,
        boolean enabled
    ) {
        putValue(Action.NAME, name);
        selection = new ArrayList<ImageDatum>();
        keystroke = key;
        if (dynamic && enabled) {
            setEnabled(false);
            browser.addBrowserListener(this);
        }
        else if (! enabled) {   // dynamic or not dynamic
            setEnabled(false);
        }
    }

    KeyStroke getKeyStroke() {
        return keystroke;
    }

    public void selectionChanged(ImageBrowserEvent event) {
        selection = event.getDatums();
        update();
    }

    public void imageDoubleClicked(ImageBrowserEvent event) {
        // do nothing
    }

    public void browserError(String message) {
        // do nothing
    }

    List<ImageDatum> getSelection() {
        return selection;
    }

    void update() {
        boolean empty = selection.isEmpty();
        setEnabled(! empty);        
    }
}
