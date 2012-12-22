/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.app;

import static com.lightcrafts.app.Locale.LOCALE;
import com.lightcrafts.ui.toolkit.IconFactory;
import com.lightcrafts.ui.editor.EditorMode;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

final class PrintDocButton extends EditorButton {

    private final static Icon Icon =
        IconFactory.createInvertedIcon(PrintDocButton.class, "print.png");

    private final static String ToolTip = LOCALE.get("PrintDocButtonToolTip");

    PrintDocButton(final ComboFrame frame) {
        super(frame, Icon);
        setToolTipText(ToolTip);

        addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    frame.getEditor().setMode( EditorMode.ARROW );
                    Application.print(frame);
                }
            }
        );
    }
}
