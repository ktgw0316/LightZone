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
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.LinkedList;

final class TemplatesMenu extends UpdatableDisposableMenu {

    private WeakReference<Document> docRef = new WeakReference<Document>(null);

    TemplatesMenu(ComboFrame frame) {
        super(frame, "Templates");
        JMenuItem noneItem = MenuFactory.createMenuItem("NoTemplateDoc");
        noneItem.setEnabled(false);
        add(noneItem);
    }

    void update() {
        ComboFrame frame = getComboFrame();
        Document newDoc = (frame != null) ? frame.getDocument() : null;
        Document oldDoc = docRef.get();

        if (newDoc != oldDoc) {
            try {
                removeAll();
                if (newDoc != null) {
                    List<TemplateKey> keys = TemplateDatabase.getTemplateKeys();
                    if (! keys.isEmpty()) {
                        LinkedHashMap<String, LinkedList<TemplateKey>> nsMap =
                            new LinkedHashMap<String, LinkedList<TemplateKey>>();
                        for (TemplateKey key : keys) {
                            String namespace = key.getNamespace();
                            if (! nsMap.containsKey(namespace)) {
                                nsMap.put(
                                    namespace, new LinkedList<TemplateKey>()
                                );
                            }
                            nsMap.get(namespace).add(key);
                        }
                        for (String namespace : nsMap.keySet()) {
                            JMenu nsItem = new JMenu(namespace);
                            List<TemplateKey> nsKeys = nsMap.get(namespace);
                            for (final TemplateKey key : nsKeys) {
                                JMenuItem templateItem =
                                    new JMenuItem(key.getName());
                                templateItem.addActionListener(
                                    new ActionListener() {
                                        public void actionPerformed(
                                            ActionEvent event
                                        ) {
                                            ComboFrame frame = getComboFrame();
                                            Application.applyTemplate(frame, key);
                                        }
                                    }
                                );
                                nsItem.add(templateItem);
                            }
                            add(nsItem);
                        }
                        setEnabled(true);
                    }
                    else {
                        JMenuItem noTemplatesItem =
                            MenuFactory.createMenuItem("NoTemplates");
                        noTemplatesItem.setEnabled(false);
                        add(noTemplatesItem);
                    }
                }
                else {
                    JMenuItem noDocItem =
                        MenuFactory.createMenuItem("NoTemplateDoc");
                    noDocItem.setEnabled(false);
                    add(noDocItem);
                }
            }
            catch (TemplateDatabase.TemplateException e) {
                setEnabled(false);
            }
            docRef = new WeakReference<Document>(newDoc);
        }
    }
}
