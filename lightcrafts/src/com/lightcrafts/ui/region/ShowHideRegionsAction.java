/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.region;

import com.lightcrafts.ui.action.ToggleAction;
import static com.lightcrafts.ui.region.Locale.LOCALE;

import java.awt.event.ActionEvent;

final class ShowHideRegionsAction extends ToggleAction {

    private static String ShowRegionsText = LOCALE.get("ShowRegionsActionName");

    private static String HideRegionsText = LOCALE.get("HideRegionsActionName");

    private static String ShowRegionsTooltip = LOCALE.get("ShowRegionsToolTip");

    private static String HideRegionsTooltip = LOCALE.get("HideRegionsToolTip");

    private RegionOverlay regions;

    ShowHideRegionsAction(RegionOverlay regions) {
        this.regions = regions;
        setName(HideRegionsText, true);
        setName(ShowRegionsText, false);
        setDescription(HideRegionsTooltip, true);
        setDescription(ShowRegionsTooltip, false);
        setState(true);
    }

    boolean isRegionsVisible() {
        return getState();
    }

    protected void onActionPerformed(ActionEvent event) {
        regions.setRegionsVisible(true);
    }

    protected void offActionPerformed(ActionEvent event) {
        regions.setRegionsVisible(false);
    }

    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
    }
}
