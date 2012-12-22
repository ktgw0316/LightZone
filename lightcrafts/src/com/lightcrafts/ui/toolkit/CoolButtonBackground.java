/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.toolkit;

import javax.swing.*;
import java.awt.*;

/**
 * Every CoolButton has a CoolButtonBackground that paints under the button
 * itself.  There are four kinds of background: left, center, right, and
 * normal.
 * <p>
 * These backgrounds extend beyond the button's icon, and so they
 * require extra insets in the button layout.  CoolButton takes the Insets
 * declared here and uses them to inialize an empty border on itself.
 * CoolButtonBackground then paints over the empty border set up by
 * CoolButton when asked to do so by a call to paint().
 * <p>
 * These backgrounds are dynamic.  They depend on the button's pressed and
 * selected state, and they use the "percentage" updated through
 * CoolMechanics.
 */
abstract class CoolButtonBackground {

    static final Color COLOR1 = new Color(75, 75, 75);
    static final Color COLOR2 = new Color(45, 45, 45);

    AbstractButton button;

    CoolButtonBackground(AbstractButton button) {
        this.button = button;
    }

    abstract Insets getInsets();

    // Compute the Rectangle which has been reserved for background painting
    // by doing inset arithmetic on our button.
    Rectangle getBackgroundRect(AbstractButton button) {
        // Subtract out the full Insets of the button...
        Insets insets = button.getInsets();
        int x = insets.left;
        int y = insets.top;
        int w = button.getWidth() - insets.left - insets.right;
        int h = button.getHeight() - insets.top - insets.bottom;

        // then back off by the insets that came from our own getInsets().
        insets = getInsets();
        x -= insets.left;
        y -= insets.top;
        w += insets.left + insets.right;
        h += insets.top + insets.bottom;

        return new Rectangle(x, y, w, h);
    }
}
