/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.app;

import static com.lightcrafts.app.Locale.LOCALE;
import com.lightcrafts.ui.browser.view.AbstractImageBrowser;
import com.lightcrafts.ui.toolkit.IconFactory;
import com.lightcrafts.ui.editor.EditorMode;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.List;

class ExportButton extends BrowserButton {

    private final static Icon Icon =
        IconFactory.createInvertedIcon(UndoButton.class, "convert.png");

    private final static String ToolTip = LOCALE.get("ExportButtonToolTip");

    ExportButton(ComboFrame frame) {
        super(frame, Icon);
        setToolTipText(ToolTip);

        addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    ComboFrame frame = getComboFrame();
                    AbstractImageBrowser browser = frame.getBrowser();
                    List<File> list = browser.getSelectedFiles();
                    if (! list.isEmpty()) {
                        frame.getEditor().setMode( EditorMode.ARROW );
                        File[] files = list.toArray(new File[0]);
                        Application.export(frame, files);
                    }
                }
            }
        );
    }

}
