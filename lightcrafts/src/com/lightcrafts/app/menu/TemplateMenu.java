/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.app.menu;

import com.lightcrafts.app.Application;
import com.lightcrafts.app.ComboFrame;
import com.lightcrafts.ui.editor.Document;
import com.lightcrafts.templates.TemplateDatabase;
import com.lightcrafts.templates.TemplateKey;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

// This is the "Apply Template" submenu under the File frame menu.  For the
// "Template" frame menu, see TemplatesMenu.

final class TemplateMenu extends UpdatableDisposableMenu {

    TemplateMenu(ComboFrame frame) {
        super(frame, "ApplyTemplate");
    }

    // Called from FileMenu when it is selected.
    void update() {
        ComboFrame frame = getComboFrame();
        Document doc = (frame != null) ? frame.getDocument() : null;
        if (doc == null) {
            setEnabled(false);
            return;
        }
        try {
            removeAll();
            List<TemplateKey> keys = TemplateDatabase.getTemplateKeys();
            if (! keys.isEmpty()) {
                for (final TemplateKey key : keys) {
                    JMenuItem templateItem = new JMenuItem(key.toString());
                    templateItem.addActionListener(
                        new ActionListener() {
                            public void actionPerformed(ActionEvent event) {
                                ComboFrame frame = getComboFrame();
                                Application.applyTemplate(frame, key);
                            }
                        }
                    );
                    add(templateItem);
                }
                setEnabled(true);
            }
            else {
                JMenuItem noneItem = MenuFactory.createMenuItem("NoTemplates");
                add(noneItem);
                setEnabled(false);
            }
        }
        catch (TemplateDatabase.TemplateException e) {
            setEnabled(false);
        }
    }
}
