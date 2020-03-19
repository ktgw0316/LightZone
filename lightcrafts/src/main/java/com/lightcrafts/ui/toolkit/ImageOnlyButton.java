/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.toolkit;

import javax.swing.*;
import java.awt.*;

/**
 * This is just a JButton that is styled to suit a button that is pure icons:
 * no borders, no margins, no insets, not focusable, not opaque, and
 * generally undecorated by the PLAF.
 * <p>
 * Use it like a normal JButton.
 */

public class ImageOnlyButton extends JButton {

    public ImageOnlyButton() {
        setStyle(this);
    }

    public ImageOnlyButton(Action action) {
        super(action);
        setStyle(this);
    }

    public ImageOnlyButton(Icon icon) {
        super(icon);
        setStyle(this);
    }

    public ImageOnlyButton(Icon normalIcon, Icon pressedIcon) {
        this(normalIcon);
        setPressedIcon(pressedIcon);
        setStyle(this);
    }

    public ImageOnlyButton(
        Icon normalIcon, Icon pressedIcon, Icon disabledIcon
    ) {
        this(normalIcon);
        setPressedIcon(pressedIcon);
        setDisabledIcon(disabledIcon);
        setStyle(this);
    }

    public ImageOnlyButton(
        Icon normalIcon, Icon rolloverIcon, Icon pressedIcon, Icon disabledIcon
    ) {
        super(normalIcon);
        setPressedIcon(pressedIcon);
        setDisabledIcon(disabledIcon);
        setRolloverIcon(rolloverIcon);
        setRolloverEnabled(true);
        setStyle(this);
    }

    public ImageOnlyButton(
        Action action, Icon rolloverIcon, Icon pressedIcon, Icon disabledIcon
    ) {
        super(action);
        setPressedIcon(pressedIcon);
        setDisabledIcon(disabledIcon);
        setRolloverIcon(rolloverIcon);
        setRolloverEnabled(true);
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
