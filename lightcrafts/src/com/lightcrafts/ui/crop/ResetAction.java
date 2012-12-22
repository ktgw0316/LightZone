/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.crop;

import com.lightcrafts.model.CropBounds;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * This is the action performed from the ResetButton and the CropPopupMenu.
 */
class ResetAction extends AbstractAction {

    private CropOverlay overlay;
    private CropBounds resetValue;

    public void actionPerformed(ActionEvent event) {
        if (overlay != null) {
            overlay.setCropWithConstraints(resetValue);
        }
    }

    void setResetValue(CropBounds resetValue) {
        this.resetValue = resetValue;
    }

    CropBounds getResetValue() {
        return resetValue;
    }

    void setOverlay(CropOverlay overlay) {
        this.overlay = overlay;
        setEnabled(overlay != null);
    }
}
