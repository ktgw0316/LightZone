/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.toolkit;

import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.JButton;
import java.awt.Insets;

/**
 * This is just a JButton that is styled to suit a button that is pure icons:
 * no borders, no margins, no insets, not focusable, not opaque, and
 * generally undecorated by the PLAF.
 * <p>
 * Use it like a normal JButton.
 */

public class ImageOnlyButton extends JButton {

    public ImageOnlyButton(Icon icon) {
        super(icon);
        setStyle(this);
    }

    public ImageOnlyButton(Icon normalIcon, Icon pressedIcon) {
        this(normalIcon);
        setPressedIcon(pressedIcon);
        setStyle(this);
    }

    public static void setStyle(AbstractButton button) {
        button.setMargin(new Insets(0, 0, 0, 0));
        button.setBorder(null);
        button.setContentAreaFilled(false);
        button.setFocusable(false);
        button.setFocusPainted(false);
    }
}
