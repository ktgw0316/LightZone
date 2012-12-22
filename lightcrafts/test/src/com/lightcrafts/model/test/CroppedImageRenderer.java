/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.model.test;

import java.awt.*;

/**
 * An interface to allow swapping between TestImageRenderer (always works)
 * and EngineImageRenderer (needs lots of fixing).
 */

interface CroppedImageRenderer extends CropBoundsControls.Listener {

    /**
     * Get a Component that renders the cropped image.
     */
    Component getComponent();
}
