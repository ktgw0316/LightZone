/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.app;

import static com.lightcrafts.app.Locale.LOCALE;
import com.lightcrafts.ui.browser.view.AbstractImageBrowser;
import com.lightcrafts.ui.toolkit.IconFactory;
import com.lightcrafts.ui.editor.EditorMode;
import com.lightcrafts.prefs.ApplicationMode;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.List;

class SendButton extends BrowserButton {

    private final static Icon Icon =
        IconFactory.createInvertedIcon(UndoButton.class, "send.png");

    private final static String ToolTip = LOCALE.get("SendButtonToolTip");

    SendButton(ComboFrame frame) {
        super(frame, Icon);
        setToolTipText(ToolTip);

        addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    ComboFrame frame = getComboFrame();
                    AbstractImageBrowser browser = frame.getBrowser();
                    List<File> list = browser.getSelectedFiles();
                    if (! list.isEmpty()) {
                        File[] files = list.toArray(new File[0]);
                        frame.getEditor().setMode( EditorMode.ARROW );
                        Application.send(frame, files);
                    }
                }
            }
        );
        ApplicationMode.maybeSetToolTip(this);
    }

    void updateEnabled() {
        if (ApplicationMode.isBasicMode()) {
            setEnabled(false);
        }
        else {
            super.updateEnabled();
        }
    }
}
