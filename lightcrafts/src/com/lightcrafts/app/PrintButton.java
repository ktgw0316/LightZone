/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2017-     Masahiro Kitagawa */

package com.lightcrafts.app;

import static com.lightcrafts.app.Locale.LOCALE;
import com.lightcrafts.ui.browser.view.AbstractImageBrowser;
import com.lightcrafts.ui.toolkit.IconFontFactory;
import com.lightcrafts.ui.editor.EditorMode;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

final class PrintButton extends BrowserButton {

    private final static Icon Icon = IconFontFactory.buildIcon("print");

    private final static String ToolTip = LOCALE.get("PrintButtonToolTip");

    PrintButton(ComboFrame frame) {
        super(frame, Icon);
        setToolTipText(ToolTip);

        addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    final ComboFrame frame = getComboFrame();
                    final AbstractImageBrowser browser = frame.getBrowser();
                    final File file = browser.getLeadSelectedFile();
                    if (file != null) {
                        frame.getEditor().setMode( EditorMode.ARROW );
                        Application.print(frame, file);
                    }
                }
            }
        );
    }
}
