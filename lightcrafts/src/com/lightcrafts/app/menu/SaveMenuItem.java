/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.app.menu;

import static com.lightcrafts.app.menu.Locale.LOCALE;
import com.lightcrafts.app.Application;
import com.lightcrafts.app.ComboFrame;
import com.lightcrafts.app.OtherApplicationShim;
import com.lightcrafts.app.other.OtherApplication;
import com.lightcrafts.ui.editor.Document;

import java.awt.event.ActionEvent;

final class SaveMenuItem extends DocumentMenuItem {

    // This menu item can have dynamic text, if there is an OtherApplication
    // that wants to do auto-save.
    private static String DefaultText;

    SaveMenuItem(ComboFrame frame) {
        super(frame, "Save");
        DefaultText = getText();
    }

    void update() {
        super.update();
        if (isEnabled()) {
            final Document doc = getDocument();
            if (OtherApplicationShim.shouldSaveDirectly(doc)) {
                final OtherApplication app = (OtherApplication) doc.getSource();
                final String appName = app.getName();
                final String appText = LOCALE.get("SaveToApp", appName);
                setText(appText);
            }
            else {
                setText(DefaultText);
            }
        }
        else {
            setText(DefaultText);
        }
    }

    public void actionPerformed(ActionEvent event) {
        performPreAction( event );
        final ComboFrame frame = getComboFrame();
        Application.save(frame);
    }
}
