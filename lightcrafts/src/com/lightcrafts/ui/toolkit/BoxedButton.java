/* Copyright (C) 2005-2011 Fabio Riccardi */

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
    public final AbstractButton[] buttons;

    public BoxedButton(String title, AbstractButton... buttons) {
        this.buttons = buttons;
        box = Box.createHorizontalBox();
        for (AbstractButton b : buttons)
            box.add(b);
        ToggleTitleBorder.setBorder(box, title);
    }
}

