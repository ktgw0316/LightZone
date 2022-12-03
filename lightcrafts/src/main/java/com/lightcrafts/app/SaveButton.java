/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2017-     Masahiro Kitagawa */

package com.lightcrafts.app;

import static com.lightcrafts.app.Locale.LOCALE;
import com.lightcrafts.ui.editor.EditorMode;
import com.lightcrafts.ui.toolkit.IconFontFactory;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

final class SaveButton extends EditorButton {

    private final static Icon Icon = IconFontFactory.buildIcon("save");

    private final static String ToolTip = LOCALE.get("SaveButtonToolTip");

    SaveButton(final ComboFrame frame) {
        super(frame, Icon);
        setToolTipText(ToolTip);

        addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    frame.getEditor().setMode( EditorMode.ARROW );
                    Application.save(frame);
                }
            }
        );
    }
}
