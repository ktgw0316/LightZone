/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.app;

import static com.lightcrafts.app.Locale.LOCALE;
import com.lightcrafts.ui.browser.view.AbstractImageBrowser;
import com.lightcrafts.ui.toolkit.IconFactory;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

final class EditButton extends BrowserButton {

    private final static Icon Icon=
        IconFactory.createInvertedIcon(EditButton.class, "edit.png");

    private final static String ToolTip = LOCALE.get("EditButtonToolTip");

    EditButton(ComboFrame frame) {
        super(frame, LOCALE.get("EditButtonText"));
        setIcon(Icon);
        setToolTipText(ToolTip);

        addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    ComboFrame frame = getComboFrame();
                    AbstractImageBrowser browser = frame.getBrowser();
                    File file = browser.getLeadSelectedFile();
                    if (file != null) {
                        Application.open(frame, file);
                    }
                }
            }
        );
    }
}
