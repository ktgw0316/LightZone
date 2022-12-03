/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.region;

import javax.swing.event.MouseInputAdapter;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * An abstract base class to manage the various modes of CurveComponent mouse
 * interaction, such as NewCurveMode, MoveCurveMode, FollowMouseMode, etc.
 */

abstract class RegionMode extends MouseInputAdapter {

    // Hot point coordinates for all the cursors:
    private final static ResourceBundle HotPointResources =
        ResourceBundle.getBundle(
            "com/lightcrafts/ui/region/resources/HotPoints"
        );

    static Cursor NewCurveCursor = createCursor("new_region");
    static Cursor MoveCurveCursor = createCursor("move_curve");
    static Cursor MovingCurveCursor = createCursor("moving_curve");
    static Cursor MovePointCursor = createCursor("move_point");
    static Cursor MovingPointCursor = createCursor("moving_curve");
    static Cursor NewPointCursor = createCursor("new_point");
    static Cursor DefaultCursor = new Cursor(Cursor.DEFAULT_CURSOR);

    RegionModel model;
    CurveComponent comp;
    Cursor cursor;

    // RegionModes form a chain that never ends.
    RegionMode(RegionMode oldMode) {
        model = oldMode.model;
        comp = oldMode.comp;
    }

    // This is how the first one of the chain is constructed.
    RegionMode(RegionModel model, CurveComponent comp) {
        this.model = model;
        this.comp = comp;
    }

    // Derived classes that want autoscrolling should call this from
    // mouseMoved() and mouseDragged().
    void autoscroll(MouseEvent event) {
        // send the "autoscroll" message up the hierarchy:
        Rectangle r = new Rectangle(event.getX(), event.getY(), 1, 1);
        comp.scrollRectToVisible(r);
    }

    static boolean isModified(MouseEvent event) {
        return
            event.isShiftDown()   ||
            event.isAltDown()     ||
            event.isControlDown() ||
            event.isMetaDown()    ||
            event.isShiftDown();
    }

    private static Cursor createCursor(String name) {
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        String path = "resources/" + name + ".png";
        URL url = RegionMode.class.getResource(path);
        Image image = toolkit.createImage(url);
        int x = Integer.parseInt(HotPointResources.getString(name + "X"));
        int y = Integer.parseInt(HotPointResources.getString(name + "Y"));
        Point hot = new Point(x, y);
        Cursor cursor = toolkit.createCustomCursor(image, hot, name);
        return cursor;
    }
}
