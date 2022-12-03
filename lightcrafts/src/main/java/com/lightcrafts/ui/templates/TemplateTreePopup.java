/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.templates;

import com.lightcrafts.platform.Platform;
import com.lightcrafts.templates.TemplateKey;
import com.lightcrafts.templates.TemplateDatabase;
import static com.lightcrafts.ui.templates.Locale.LOCALE;
import com.lightcrafts.utils.file.FileUtil;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.*;
import java.io.File;

class TemplateTreePopup extends JPopupMenu {

    private TemplateTree tree;

    // The TemplateKey corresponding to the point on the TemplateTree where
    // the popup was triggered, as given in show().
    private TemplateKey key;
    
    TemplateTreePopup(TemplateTree tree) {
        this.tree = tree;

        JMenuItem item;

        item = new JMenuItem(LOCALE.get("ShowMenuItem"));
        item.addActionListener(
             new ActionListener() {
                 public void actionPerformed(ActionEvent event) {
                     if (key != null) {
                         Platform platform = Platform.getPlatform();
                         File file = key.getFile();
                         file = FileUtil.resolveAliasFile(file);
                         String path = file.getAbsolutePath();
                         platform.showFileInFolder(path);
                     }
                 }
             }
        );
        add(item);

        item = new JMenuItem(LOCALE.get("DeleteMenuItem"));
        item.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    if (key != null) {
                        try {
                            TemplateDatabase.removeTemplateDocument(key);
                        }
                        catch (TemplateDatabase.TemplateException e) {
                            // The TemplateList gives its own feedback.
                            System.out.println(
                                "Couldn't delete Template " + key
                            );
                            e.printStackTrace();
                        }
                    }
                }
            }
        );
        add(item);

        addSeparator();

        item = new JMenuItem(LOCALE.get("RestoreMenuItem"));
        item.addActionListener(
             new ActionListener() {
                 public void actionPerformed(ActionEvent event) {
                    TemplateDatabase.deployFactoryTemplates();
                 }
             }
        );
        add(item);
    }

    void show(Point p, TemplateKey key) {
        this.key = key;
        show(tree, p.x, p.y);
    }
}
