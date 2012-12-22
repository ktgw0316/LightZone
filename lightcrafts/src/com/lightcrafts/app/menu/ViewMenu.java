/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.app.menu;

import com.lightcrafts.app.ComboFrame;
import com.lightcrafts.model.Preview;
import com.lightcrafts.ui.editor.Document;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.List;

final class ViewMenu extends UpdatableDisposableMenu {

    private List<JMenuItem> rotateItems;
    private List<JMenuItem> zoomItems;

    private WeakReference<Document> docRef = new WeakReference<Document>(null);

    ViewMenu(ComboFrame frame) {
        super(frame, "View");

        rotateItems = new LinkedList<JMenuItem>();
        zoomItems = new LinkedList<JMenuItem>();

        rotateItems.add(new RotateLeftMenuItem(frame));
        rotateItems.add(new RotateRightMenuItem(frame));

        zoomItems.add(new ZoomInMenuItem(frame));
        zoomItems.add(new ZoomOutMenuItem(frame));
        zoomItems.add(new ZoomFitMenuItem(frame));
        zoomItems.add(new Zoom1To1MenuItem(frame));

        addFixedItems();
        addSeparator();
        addLayoutItems();
        Document doc = (frame != null) ? frame.getDocument() : null;
        if (doc != null) {
            addSeparator();
            addPreviewItems();
        }
    }

    void update() {
        final ComboFrame frame = getComboFrame();
        final Document newDoc = (frame != null) ? frame.getDocument() : null;
        final Document oldDoc = docRef.get();

        if (newDoc != oldDoc) {
            removeAll();
            addFixedItems();
            addSeparator();
            addLayoutItems();
            if (newDoc != null) {
                addSeparator();
                addPreviewItems();
            }
            docRef = new WeakReference<Document>(newDoc);
        }
        super.update();
    }

    private void addFixedItems() {
        for (JMenuItem item : rotateItems) {
            add(item);
        }
        addSeparator();
        for (JMenuItem item : zoomItems) {
            add(item);
        }
    }

    private void addLayoutItems() {
        final ComboFrame frame = getComboFrame();
        add(new EditorLayoutMenuItem(frame));
        add(new BrowserLayoutMenuItem(frame));
    }

    private void addPreviewItems() {
        final ComboFrame frame = getComboFrame();
        final Document doc = frame.getDocument();
        final List<Preview> previews = doc.getEngine().getPreviews();
        int previewCount = 0;
        for ( final Preview preview : previews ) {
            if (previewCount == 0) {
                // The first preview is the "styles" preview, not for this menu.
                previewCount++;
                continue;
            }
            String name = preview.getName();
            JMenuItem previewItem = new JMenuItem(name);
            previewItem.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        doc.getEditor().setPreview( preview );
                    }
                }
            );
            final KeyStroke stroke = getKeyStroke( previewCount++ );
            previewItem.setAccelerator( stroke );
            add( previewItem );
        }
    }

    private static KeyStroke getKeyStroke(int i) {
        final int mask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
        return KeyStroke.getKeyStroke( KeyEvent.VK_0 + i, mask);
    }
}
/* vim:set et sw=4 ts=4: */
