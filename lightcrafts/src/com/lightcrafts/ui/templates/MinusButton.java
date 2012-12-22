/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.templates;

import com.lightcrafts.templates.TemplateDatabase;
import com.lightcrafts.templates.TemplateKey;
import static com.lightcrafts.ui.templates.Locale.LOCALE;
import com.lightcrafts.ui.toolkit.CoolButton;
import com.lightcrafts.ui.toolkit.IconFactory;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

class MinusButton extends CoolButton {

    private static Icon Icon =
        IconFactory.createInvertedIcon(MinusButton.class, "minus.png");

    private final static String ToolTip = LOCALE.get("MinusToolTip");

    // In case of a TemplateException, the TemplateTree can not be instantiated,
    // so here's a disabled, functionless placeholder button.
    MinusButton() {
        setIcon(Icon);
        setToolTipText(ToolTip);
        setEnabled(false);
    }

    MinusButton(final TemplateTree tree) {
        this();

        // This button got broke when we made the Styles control respond to
        // clicks instead of tree selection changes.
        
//        final TemplateTreeSelectionModel selection =
//            tree.getTemplateSelectionModel();
//        selection.addTreeSelectionListener(
//            new TreeSelectionListener() {
//                public void valueChanged(TreeSelectionEvent e) {
//                    boolean empty = selection.isSelectionEmpty();
//                    setEnabled(tree.isEnabled() && ! empty);
//                }
//            }
//        );
        addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    TemplateKey key = tree.getSelectedTemplateKey();
                    if (key != null) {
                        try {
                            tree.clearSelection();
                            TemplateDatabase.removeTemplateDocument(key);
                        }
                        catch (TemplateDatabase.TemplateException e) {
                            // The user will see nothing happened.
                            e.printStackTrace();
                        }
                    }
                }
            }
        );
    }
}
