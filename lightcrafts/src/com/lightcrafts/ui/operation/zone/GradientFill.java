/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.operation.zone;

import javax.swing.*;
import java.awt.*;

/** A primitive Component that fills itself with a gray color determined from
 * a Zone.
 */

class GradientFill extends JComponent {

    private Zone zone;

    GradientFill(Zone zone) {
        this.zone = zone;
    }

    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Dimension size = getSize();
        Color oldColor = g.getColor();
        g.setColor(zone.getColor());
        g.fillRect(0, 0, size.width, size.height);
        g.setColor(oldColor);
    }

    public Dimension getPreferredSize() {
        return new Dimension(20, 20);
    }
}
