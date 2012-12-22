/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.app;

import static com.lightcrafts.app.Locale.LOCALE;
import com.lightcrafts.ui.toolkit.IconFactory;
import com.lightcrafts.ui.editor.EditorMode;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

final class DoneButton extends EditorButton {

    private final static Icon Icon =
        IconFactory.createInvertedIcon(UndoButton.class, "save.png");

    private final static String ToolTip = LOCALE.get("DoneButtonToolTip");

    DoneButton(final ComboFrame frame) {
        super(frame, Icon);
        setToolTipText(ToolTip);

        addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    frame.getEditor().setMode( EditorMode.ARROW );
                    frame.showBrowserPerspective();
                }
            }
        );
    }
}
