/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.operation.zone;

import javax.swing.*;
import java.awt.*;

/** ZoneContainer is a JPanel with specialized layout for handling
 * ZoneWidgets in a ZoneControl.
 */

class ZoneContainer extends JPanel {

    private final static int Inset = 6;
    private final static int ReferenceWidth = 20;
    private final static int Gap = 6;

    private Component controls;
    private Component reference;

    ZoneContainer(ZoneWidget controls, ZoneWidget reference) {
        this.controls = controls;
        this.reference = reference;
        add(controls);
        add(reference);
    }

    public void doLayout() {
        Dimension size = getSize();

        // Insets are fixed, the reference Component has fixed width, there
        // is a fixed gap, and the controls Component gets the remaining
        // width.

        int controlsWidth = size.width - ReferenceWidth - Gap - 2 * Inset;
        int zoneHeight = size.height - 2 * Inset;

        reference.setLocation(Inset, Inset);
        reference.setSize(ReferenceWidth, zoneHeight);

        controls.setLocation(Inset + ReferenceWidth + Gap, Inset);
        controls.setSize(controlsWidth, zoneHeight);
    }
}
