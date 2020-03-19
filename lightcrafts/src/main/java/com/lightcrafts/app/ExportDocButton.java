/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.app;

import static com.lightcrafts.app.Locale.LOCALE;
import com.lightcrafts.ui.toolkit.IconFactory;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

class ExportDocButton extends EditorButton {

    private final static Icon Icon =
        IconFactory.createInvertedIcon(ExportDocButton.class, "convert.png");

    private final static String ToolTip = LOCALE.get("ExportDocButtonToolTip");

    ExportDocButton(final ComboFrame frame) {
        super(frame, Icon);
        setToolTipText(ToolTip);

        addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    Application.export(frame);
                }
            }
        );
    }
}
