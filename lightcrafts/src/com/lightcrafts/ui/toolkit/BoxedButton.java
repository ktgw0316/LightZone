/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2015 Masahiro Kitagawa */

package com.lightcrafts.ui.toolkit;

import com.lightcrafts.ui.layout.ToggleTitleBorder;

import javax.swing.*;

/**
 * Copyright (C) 2007 Light Crafts, Inc.
 * User: fabio
 * Date: May 18, 2007
 * Time: 4:20:44 PM
 */

public class BoxedButton {
    public final Box box;
    public final JComponent[] buttons;

    public BoxedButton(String title, JComponent... buttons) {
        this.buttons = buttons;
        box = Box.createHorizontalBox();

        int buttonWidth = 0;
        for (JComponent b : buttons)
            buttonWidth += b.getWidth();
        final int titleWidth = box.getFontMetrics(ToggleTitleBorder.font).stringWidth(title);
        final int sideMargin = Math.max(0, (titleWidth - buttonWidth) / 2);

        box.add(Box.createHorizontalStrut(sideMargin));
        for (JComponent b : buttons)
            box.add(b);
        box.add(Box.createHorizontalStrut(sideMargin));
        ToggleTitleBorder.setBorder(box, title);
    }
}

