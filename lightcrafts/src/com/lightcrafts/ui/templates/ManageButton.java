/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.templates;

import static com.lightcrafts.ui.templates.Locale.LOCALE;
import com.lightcrafts.ui.toolkit.CoolButton;
import com.lightcrafts.ui.toolkit.IconFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

class ManageButton extends CoolButton {

    private static Icon Icon =
        IconFactory.createInvertedIcon(ManageButton.class, "elipsis.png");

    private final static String ToolTip = LOCALE.get("ManageToolTip");

    ManageButton() {
        setIcon(Icon);
        setToolTipText(ToolTip);

        addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    Frame frame = (Frame) SwingUtilities.getAncestorOfClass(
                        Frame.class, ManageButton.this
                    );
                    TemplateList.showDialog(frame);
                }
            }
        );
    }
}
