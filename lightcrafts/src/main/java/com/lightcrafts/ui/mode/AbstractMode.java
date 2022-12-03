/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.mode;

import java.awt.*;
import java.awt.geom.AffineTransform;

public abstract class AbstractMode implements Mode {

    public void dispose() {
        // do nothing
    }

    public void enter() {
        m_entered = true;
    }

    public void exit() {
        m_entered = false;
    }

    public boolean isIn() {
        return m_entered;
    }

    public void setTransform( AffineTransform xform ) {
        // do nothing
    }

    public void setUnderlayBounds( Rectangle bounds ) {
        // do nothing
    }

    public boolean wantsAutocroll() {
        return false;
    }

    private boolean m_entered;
}
/* vim:set et sw=4 ts=4: */
