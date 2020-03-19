/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.browser.ctrls;

import com.lightcrafts.ui.toolkit.CoolButton;
import com.lightcrafts.ui.toolkit.IconFactory;

import javax.swing.*;

class ButtonFactory {

    static JButton createButtonByName(String name) {
        return createButtonByName(name, CoolButton.ButtonStyle.NORMAL);
    }

    static JButton createButtonByName(
        String name, CoolButton.ButtonStyle style
    ) {
        JButton button = new CoolButton(style);
        Icon icon = getIconByName(name);
        if (icon != null) {
            button.setIcon(icon);
        }
        else {
            button.setText(name);
        }
        return button;
    }

    static JButton createButtonByText(String text) {
        JButton button = new CoolButton(CoolButton.ButtonStyle.NORMAL);
        button.setText(text);
        return button;
    }

    static Icon getIconByName(String name, int size) {
        return IconFactory.createInvertedIcon(
            ButtonFactory.class, name + ".png", size
        );
    }

    static Icon getIconByName(String name) {
        return IconFactory.createInvertedIcon(
            ButtonFactory.class, name + ".png"
        );
    }
}
