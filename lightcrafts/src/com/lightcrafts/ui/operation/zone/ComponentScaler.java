/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.operation.zone;

import java.awt.*;

/** This is a static utility for converting between Component positions in
 * layout and floating point numbers between zero and one.  It's important
 * because we are using drag gestures on Components to control the ZoneModel,
 * and we'd like the changes to the model to be consistent with the dragged
 * positions.
 * <p>
 * The scale number is zero at the bottom of the parent Container and one at
 * the top.  Child Component heights are measured at their vertical midpoint.
 */
class ComponentScaler {

    /** Get the scale value correpsonding to the vertical midpoint of the
     * Component relative to its parent's height.
     */
    static double componentToScale(Component comp) {
        int top = comp.getLocation().y;
        int bottom = top + comp.getSize().height;

        Component parent = comp.getParent();
        int height = parent.getSize().height;

        return (height - (bottom + top) / 2) / (double) height;
    }

    /** Set the location of the given Component so that its vertical midpoint
     * is consistent with the given scale value.
     */
    static void scaleToComponent(Component comp, double scale) {
        Component parent = comp.getParent();
        int height = parent.getSize().height;

        Point p = comp.getLocation();
        Dimension size = comp.getSize();

        int y = (int) Math.round((1. - scale) * height - size.height / 2);

        comp.setLocation(p.x, y);
    }

    /** Just like componentToScale(Component) except the given inset is used
      * at the top and bottom of the parent Container.
      */
    static double componentToScale(Component comp, int inset) {
        int top = comp.getLocation().y;
        int bottom = top + comp.getSize().height;

        Component parent = comp.getParent();
        int height = parent.getSize().height;

        return (height - (bottom + top) / 2 - inset)
            / (double) (height - 2 * inset);
    }

    /** Just like scaleToComponent(Component, double) except the given inset is
      * used at the top and bottom of the parent Container.
      */
    static void scaleToComponent(Component comp, int inset, double scale) {
        Component parent = comp.getParent();
        int height = parent.getSize().height;

        Point p = comp.getLocation();
        Dimension size = comp.getSize();

        int y = (int) Math.round((1. - scale) * (height - 2 * inset)
            + inset - size.height / 2);

        comp.setLocation(p.x, y);
    }
}
