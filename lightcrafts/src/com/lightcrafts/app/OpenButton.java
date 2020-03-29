/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2017-     Masahiro Kitagawa */

package com.lightcrafts.app;

import static com.lightcrafts.app.Locale.LOCALE;
import com.lightcrafts.ui.toolkit.IconFontFactory;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

class OpenButton extends FrameButton {

    private final static Icon Icon = IconFontFactory.buildIcon("open");

    private final static String ToolTip = LOCALE.get("OpenButtonToolTip");

    OpenButton(final ComboFrame frame) {
        super(frame, Icon);
        setToolTipText(ToolTip);

        addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    Application.open(frame);
                }
            }
        );
    }

    @Override
    void updateButton() {
        // Do nothing
    }
}
