/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.app.menu;

import com.lightcrafts.app.Application;
import com.lightcrafts.app.ComboFrame;
import com.lightcrafts.ui.toolkit.journal.JournalDialog;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedList;
import java.util.List;

/**
 * The WindowMenu maintains a list of open AppFrames.  It is therefore a
 * repository of global data, and since it is a menu it is prone to leakage
 * on the Mac.
 * <p>
 * Any WindowMenu vended by createMenu() must be disposed by destroyMenu().
 */
public final class WindowMenu extends UpdatableDisposableMenu {

    // Keep a copy of all menus generated, so they can be updated:
    private static LinkedList<WindowMenu> Menus = new LinkedList<WindowMenu>();

    WindowMenu(ComboFrame frame) {
        super(frame, "Window");

        Menus.add(this);

        JMenuItem minItem = MenuFactory.createMenuItem("Minimize");
        minItem.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    ComboFrame frame = getComboFrame();
                    frame.setExtendedState(Frame.ICONIFIED);
                }
            }
        );
        minItem.setEnabled(frame != null);
        put("minItem", minItem);

        JMenuItem maxItem = MenuFactory.createMenuItem("Maximize");
        maxItem.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    ComboFrame frame = getComboFrame();
                    int state = frame.getExtendedState();
                    // If already maximized, then unmaximize:
                    if ((state & Frame.MAXIMIZED_BOTH) != 0) {
                        frame.setExtendedState(state - Frame.MAXIMIZED_BOTH);
                    }
                    else {
                        frame.setExtendedState(Frame.MAXIMIZED_BOTH);
                    }
                }
            }
        );
        maxItem.setEnabled(frame != null);
        put("maxItem", maxItem);

        JMenuItem allItem = MenuFactory.createMenuItem("AllToFront");
        allItem.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    for (WindowMenu windowMenu : Menus) {
                        Window window = windowMenu.getComboFrame();
                        if (window != null) {
                            window.toFront();
                        }
                    }
                }
            }
        );
        put("allItem", allItem);

        if ((System.getProperty("lightcrafts.debug") != null) ||
            (System.getProperty("lightcrafts.journal") != null)) {

            addSeparator();

            JMenuItem journalItem = new JMenuItem("Event Journal");
            journalItem.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        ComboFrame frame = getComboFrame();
                        JournalDialog.showJournalDialog(frame);
                    }
                }
            );
            put("journalItem", journalItem);
        }
        update();
    }

    public static void updateAll() {
        for (WindowMenu menu : Menus) {
            menu.update();
        }
    }

    void update() {
        JMenuItem minItem = (JMenuItem) get("minItem");
        JMenuItem maxItem = (JMenuItem) get("maxItem");
        JMenuItem allItem = (JMenuItem) get("allItem");

        removeAll();

        for (int n=0; containsKey("frame" + n); n++) {
            remove("frame" + n);
        }
        add(minItem);
        add(maxItem);
        addSeparator();
        add(allItem);
        addSeparator();

        List<ComboFrame> current = Application.getCurrentFrames();
        int index = 0;
        for (ComboFrame frame : current) {
            String title = frame.getTitle();
            JMenuItem menuItem = new JMenuItem(title);
            final String frameKey = "frame" + index++;
            put(frameKey, frame);
            menuItem.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        Frame frame = (Frame) get(frameKey);
                        frame.toFront();
                    }
                }
            );
            add(menuItem);
        }
        if (current.isEmpty()) {
            JMenuItem emptyItem = MenuFactory.createMenuItem("NoWindow");
            emptyItem.setEnabled(false);
            add(emptyItem);
        }
        JMenuItem journalItem = (JMenuItem) get("journalItem");
        if (journalItem != null) {
            addSeparator();
            add(journalItem);
        }
    }

    /**
     * The DisposableMenus vended by createMenu() are registered in a global
     * list, so they can be updated when windows open and close.  To unregister
     * such a DisposableMenu, call this method.
     */
    public static void destroyMenu(WindowMenu menu) {
        menu.dispose();
        Menus.remove(menu);
    }
}
