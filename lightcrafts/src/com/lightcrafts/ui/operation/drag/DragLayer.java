/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.operation.drag;

import javax.swing.*;
import java.awt.*;

class DragLayer extends JPanel {

    private Component comp;

    DragLayer() {
        setLayout(null);
        setOpaque(false);
    }

    void setDragComponent(Component comp) {
        this.comp = comp;
        removeAll();
        if (comp != null) {
            add(comp);
        }
        repaint();
    }

    void dragTo(int y) {
        Point loc = comp.getLocation();
        int x = loc.x;
        comp.setLocation(x, y);
    }

    public Dimension getPreferredSize() {
        if (comp != null) {
            return new Dimension(
                comp.getWidth(), comp.getY() + comp.getHeight()
            );
        }
        return new Dimension(0, 0);
    }
}
