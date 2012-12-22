/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.scroll;

import javax.swing.*;
import java.awt.*;

/** Just like ViewportLayout, except that in layoutContainer(), before
  * setViewSize() gets called (triggering state change events that lead to
  * all kinds of havoc with the visible view bounds), changes in view size
  * are detected and view bounds are first udpated to preserve the center of
  * the visible region.
  */

public class CenteringViewportLayout extends ViewportLayout {

    private Dimension oldSize;

    public void layoutContainer(Container parent) {
        JViewport viewport = (JViewport) parent;
        Component view = viewport.getView();
        Dimension newSize = view.getPreferredSize();

        if (oldSize != null && (! newSize.equals(oldSize))) {
            Rectangle oldVisible = viewport.getViewRect();
            Rectangle newVisible = getNewVisible(oldSize, oldVisible, newSize);
            Point p = getViewPosition(newSize, newVisible);
            super.layoutContainer(parent);
            viewport.setViewPosition(p);
            oldSize = newSize;
        }
        else {
            super.layoutContainer(parent);
        }
        oldSize = view.getSize();
    }

    private static Point getViewPosition(Dimension size, Rectangle visible) {
        int x = visible.x;
        int y = visible.y;
        if (x < 0) {
            x = 0;
        }
        else if (x > size.width - visible.width) {
            x = Math.max(0, size.width - visible.width);
        }
        if (y < 0) {
            y = 0;
        }
        else if (y > size.height - visible.height) {
            y = Math.max(0, size.height - visible.height);
        }
        return new Point(x, y);
    }

    private static Rectangle getNewVisible(
        Dimension oldSize, Rectangle oldVisible, Dimension newSize
    ) {
        // old center, in content coordinates:
        int oldCenterX = oldVisible.x + oldVisible.width / 2;
        int oldCenterY = oldVisible.y + oldVisible.height / 2;

        // old center, as ratios of the old dimensions:
        double x = oldCenterX / (double) oldSize.width;
        double y = oldCenterY / (double) oldSize.height;

        // new centers, in content coordinates:
        int newCenterX = (int) Math.round(x * newSize.width);
        int newCenterY = (int) Math.round(y * newSize.height);

        // upper left corner of translated visible rect, in content coordinates:
        int newLeft = newCenterX - oldVisible.width / 2;
        int newTop = newCenterY - oldVisible.height / 2;

        int newWidth = oldVisible.width;
        int newHeight = oldVisible.height;

        Rectangle newVisible =
            new Rectangle(newLeft, newTop, newWidth, newHeight);

        return newVisible;
    }
}
