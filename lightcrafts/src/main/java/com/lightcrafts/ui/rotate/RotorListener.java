/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.rotate;

public interface RotorListener {

    /**
     * @param angle The new angle of rotation, in radians, with the sign
     * convention that a positive angle means a clockwise rotation on the
     * screen.
     * @param isChanging True if more changes are expected soon.
     * @param isNinetyDegrees True if this change came from one of the
     * ninety-degree rotation actions.
     */
    void angleChanged(
        double angle, boolean isChanging, boolean isNinetyDegrees
    );
    
    void angleReset();
}
