/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.browser.view;

import com.lightcrafts.ui.browser.model.ImageDatum;
import static com.lightcrafts.ui.browser.view.Locale.LOCALE;

import javax.swing.*;

/**
 * An AbstractAction that listens to browser selection events, enables and
 * disables according to whether the selection includes a lead selection, and
 * updates its name with the name of the lead selection.
 */
abstract class LeadSelectionAction
    extends AbstractAction implements ImageBrowserListener
{
    private ImageDatum lead;

    LeadSelectionAction(
        String key, AbstractImageBrowser browser, boolean enabled
    ) {
        putValue(Action.NAME, LOCALE.get(key));
        setEnabled(false);
        if (enabled) {
            browser.addBrowserListener(this);
        }
    }
    
    public void selectionChanged(ImageBrowserEvent event) {
        lead = event.getLead();
        update();
    }

    public void imageDoubleClicked(ImageBrowserEvent event) {
        // do nothing
    }

    public void browserError(String message) {
        // do nothing
    }

    ImageDatum getLeadDatum() {
        return lead;
    }

    void update() {
        setEnabled(lead != null);
    }
}
