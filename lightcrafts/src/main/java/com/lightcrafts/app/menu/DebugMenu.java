/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.app.menu;

import com.lightcrafts.app.Application;
import com.lightcrafts.app.ComboFrame;
import com.lightcrafts.model.Engine;
import com.lightcrafts.ui.editor.Document;

import javax.swing.*;
import java.awt.*;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class DebugMenu extends UpdatableDisposableMenu {

    private static final Logger logger = LoggerFactory.getLogger(DebugMenu.class);

    WeakReference<Document> docRef = new WeakReference<>(null);

    List<JMenuItem> fixedItems;

    DebugMenu(ComboFrame frame) {
        super(frame, "Debug");
        fixedItems = createStaticItems();
        for (JMenuItem item : fixedItems) {
            add(item);
        }
    }

    void update() {
        ComboFrame frame = getComboFrame();
        if (frame != null) {
            Document newDoc = frame.getDocument();
            Document oldDoc = docRef.get();
            if (newDoc != oldDoc) {
                removeAll();
                if (newDoc != null) {
                    Engine engine = newDoc.getEngine();
                    List<JMenuItem> items = engine.getDebugItems();
                    for (JMenuItem item : items) {
                        add(item);
                    }
                }
                for (JMenuItem item : fixedItems) {
                    add(item);
                }
                docRef = new WeakReference<>(newDoc);
            }
        }
    }

    private static LinkedList<JMenuItem> createStaticItems() {
        LinkedList<JMenuItem> items = new LinkedList<>();

        JMenuItem menuItem;

        boolean isDoubleBuffered =
            RepaintManager.currentManager(null).isDoubleBufferingEnabled();
        menuItem = new JCheckBoxMenuItem("Double Buffering", isDoubleBuffered);
        menuItem.addActionListener(event -> {
            AbstractButton button = (AbstractButton) event.getSource();
            boolean selected = button.isSelected();
            RepaintManager rm = RepaintManager.currentManager(null);
            rm.setDoubleBufferingEnabled(selected);
        });
        items.add(menuItem);

        JMenu lafMenu = new JMenu("Set Look And Feel");
        UIManager.LookAndFeelInfo[] lafs = UIManager.getInstalledLookAndFeels();
        for (final UIManager.LookAndFeelInfo info : lafs) {
            menuItem = new JMenuItem(info.getName());
            menuItem.addActionListener(e -> {
                String className = info.getClassName();
                Application.setLookAndFeel(className);
            });
            lafMenu.add(menuItem);
        }
        items.add(lafMenu);

        menuItem = new JMenuItem("Show TCTool");
        menuItem.addActionListener(event -> {
            // Invoke TCTool by reflection, since it's in test code:
            try {
                Class.forName("tilecachetool.TCTool").newInstance();
            } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
                logger.warn("Failed to open TCTool", e);
            }
        });
        items.add(menuItem);

        menuItem = new JMenuItem("Show Component Tree");
        menuItem.addActionListener(event -> {
            // Invoke ComponentTree by reflection, since it's in test code:
            try {
                Class<?> clazz = Class.forName("com.lightcrafts.app.test.ComponentTree");
                Method method = clazz.getDeclaredMethod("show", Component.class);
                List<ComboFrame> frames = Application.getCurrentFrames();
                for (ComboFrame frame : frames) {
                    method.invoke(null, frame);
                }
            } catch (IllegalAccessException | ClassNotFoundException | NoSuchMethodException
                     | InvocationTargetException e) {
                logger.warn("Failed to show component tree", e);
            }
        });
        items.add(menuItem);

        menuItem = new JMenuItem("Run GC");
        menuItem.addActionListener(event -> Runtime.getRuntime().gc());
        items.add(menuItem);

        return items;
    }
}
