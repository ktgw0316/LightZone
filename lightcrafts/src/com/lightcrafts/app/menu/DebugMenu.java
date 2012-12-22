/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.app.menu;

import com.lightcrafts.app.Application;
import com.lightcrafts.app.ComboFrame;
import com.lightcrafts.model.Engine;
import com.lightcrafts.ui.editor.Document;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.*;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedList;
import java.util.List;

class DebugMenu extends UpdatableDisposableMenu {

    WeakReference<Document> docRef = new WeakReference<Document>(null);

    List<JMenuItem> fixedItems = new LinkedList<JMenuItem>();

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
                    List items = engine.getDebugItems();
                    for (Object item : items) {
                        add((JMenuItem) item);
                    }
                }
                for (JMenuItem item : fixedItems) {
                    add(item);
                }
                docRef = new WeakReference<Document>(newDoc);
            }
        }
    }

    private static LinkedList<JMenuItem> createStaticItems() {
        LinkedList<JMenuItem> items = new LinkedList<JMenuItem>();

        JMenuItem menuItem;

        boolean isDoubleBuffered =
            RepaintManager.currentManager(null).isDoubleBufferingEnabled();
        menuItem = new JCheckBoxMenuItem("Double Buffering", isDoubleBuffered);
        menuItem.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    AbstractButton button = (AbstractButton) event.getSource();
                    boolean selected = button.isSelected();
                    RepaintManager rm = RepaintManager.currentManager(null);
                    rm.setDoubleBufferingEnabled(selected);
                }
            }
        );
        items.add(menuItem);

        JMenu lafMenu = new JMenu("Set Look And Feel");
        UIManager.LookAndFeelInfo[] lafs = UIManager.getInstalledLookAndFeels();
        for (final UIManager.LookAndFeelInfo info : lafs) {
            menuItem = new JMenuItem(info.getName());
            menuItem.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        String className = info.getClassName();
                        Application.setLookAndFeel(className);
                    }
                }
            );
            lafMenu.add(menuItem);
        }
        items.add(lafMenu);

        menuItem = new JMenuItem("Show TCTool");
        menuItem.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent event) {

                    // Invoke TCTool by reflection, since it's in test code:

                    try {
                        Class.forName("tilecachetool.TCTool").newInstance();
                    } catch (InstantiationException e) {
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }
        );
        items.add(menuItem);

        menuItem = new JMenuItem("Show Component Tree");
        menuItem.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent event) {

                    // Invoke ComponentTree by reflection, since it's in test code:

                    try {
                        Class clazz = Class.forName("com.lightcrafts.app.test.ComponentTree");
                        Method method = clazz.getDeclaredMethod("show", Component.class);
                        List<ComboFrame> frames = Application.getCurrentFrames();
                        for (ComboFrame frame : frames) {
                            method.invoke(null, frame);
                        }
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    } catch (NoSuchMethodException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    }
                }
            }
        );
        items.add(menuItem);

        menuItem = new JMenuItem("Run GC");
        menuItem.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    Runtime.getRuntime().gc();
                }
            }
        );
        items.add(menuItem);

        return items;
    }
}
