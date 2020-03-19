/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.templates;

import static com.lightcrafts.ui.templates.Locale.LOCALE;
import com.lightcrafts.ui.toolkit.CoolButton;
import com.lightcrafts.ui.toolkit.IconFactory;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

class PlusButton extends CoolButton {

    private static Icon Icon =
        IconFactory.createInvertedIcon(PlusButton.class, "plus.png");

    private final static String ToolTip = LOCALE.get("PlusToolTip");

    PlusButton(final TemplateControl control) {
        setIcon(Icon);
        setToolTipText(ToolTip);

        addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    control.plusButtonPressed();
                }
            }
        );
    }
}
