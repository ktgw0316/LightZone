/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.operation;

import com.lightcrafts.ui.mode.Mode;

/**
 * OpControls sometimes want to trigger temporary Mode changes, like switching
 * to a dropper to sample from the image.  A callback to this interface
 * indicates a request from the OpControl that the given Mode be added or
 * removed, not an indication that the current Mode has changed.
 */
public interface OpControlModeListener {

    /**
     * An OpControl wants to push the given Mode onto the Mode stack.
     */
    void enterMode(Mode mode);

    /**
     * An OpControl wants to pop the given Mode, previously passed to
     * enterMode(), off of the Mode stack.
     */
    void exitMode(Mode mode);
}
