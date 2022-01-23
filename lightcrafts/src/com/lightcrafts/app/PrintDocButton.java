/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.app;

import com.lightcrafts.ui.editor.EditorMode;
import com.lightcrafts.ui.toolkit.IconFontFactory;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import static com.lightcrafts.app.Locale.LOCALE;

final class PrintDocButton extends EditorButton {

    private final static Icon Icon = IconFontFactory.buildIcon("print");

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
