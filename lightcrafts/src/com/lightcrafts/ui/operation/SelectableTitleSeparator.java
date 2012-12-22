/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.operation;

import javax.swing.*;
import java.awt.*;

class SelectableTitleSeparator extends JComponent {

    static final int Height = 1;

    public Dimension getPreferredSize() {
        return new Dimension(Integer.MAX_VALUE, Height);
    }

    protected void paintComponent(Graphics g) {
        Color oldColor = g.getColor();
        g.setColor(Color.gray);
        Dimension size = getSize();
        g.drawLine(1, 0, size.width - 1, 0);
        g.setColor(oldColor);
    }
}
