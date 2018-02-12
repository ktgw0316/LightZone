/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.model;

import com.lightcrafts.image.color.ColorScience;

import javax.swing.*;
import java.awt.*;

/**
 * Any Operation may supply a Preview, which should be displayed to
 * represent state at a fixed point in an Engine pipeline.  Previews are
 * intended to support only graphics built from drawing primitives, not
 * Component hierarchies.
 * <p>
 * A Preview may not always be showing on the screen.  To save work when a
 * Preview is hidden, or if valid bounds are required to paint, test whether
 * the Preview is showing by calling <code>isShowing()</code>.
 */

public abstract class Preview extends JComponent {

    /**
     * A user-presentable String label for this Preview.
     */
    public abstract String getName();

    /**
     * Throws IllegalArgumentException.  It is forbidden to add Component
     * hierarchives to a Preview.
     */
    public void addImpl(Component comp, Object constraints, int index) {
        throw new IllegalArgumentException(
            "Preview JComponents can have no children"
        );
    }

    /**
     * Previews may update depending on a cursor location.
     * @param p A Point in the coordinates of an Engine's Component that is the
     * current location of the cursor.
     */
    public abstract void setDropper(Point p);

    /**
     * Preview may update depending on a Region.
     * @param region The currently selected Region.
     */
    public abstract void setRegion(Region region);

    /**
     * Sets the selection state of a preview
     * @param selected wether the preview component is selected or not
     */
    public abstract void setSelected(Boolean selected);

    /**
     * Derived classes must paint something.
     */
    protected abstract void paintComponent(Graphics graphics);

    static protected double calcZone(double lightness) {
        return 16 * Math.log1p(lightness) / (8 * Math.log(2));
    }

    static protected float calcLightness(int red, int green, int blue) {
        return ColorScience.Wr * red + ColorScience.Wg * green + ColorScience.Wb * blue;
    }
}
