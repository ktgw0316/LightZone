/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.app.menu;

import com.lightcrafts.app.Application;
import com.lightcrafts.app.ComboFrame;
import com.lightcrafts.ui.toolkit.journal.JournalDialog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * The WindowMenu maintains a list of open AppFrames.  It is therefore a
 * repository of global data, and since it is a menu it is prone to leakage
 * on the Mac.
 * <p>
 * Any WindowMenu vended by createMenu() must be disposed by destroyMenu().
 */
public final class WindowMenu extends UpdatableDisposableMenu {

    private static final Logger logger = LoggerFactory.getLogger(WindowMenu.class);

    // Keep a copy of all menus generated, so they can be updated:
    private static LinkedList<WindowMenu> Menus = new LinkedList<WindowMenu>();

    WindowMenu(ComboFrame frame) {
        super(frame, "Window");

        Menus.add(this);

        JMenuItem minItem = MenuFactory.createMenuItem("Minimize");
        minItem.addActionListener(e -> {
            ComboFrame frame3 = getComboFrame();
            final int oldState = frame3.getExtendedState();
            frame3.setExtendedState(Frame.ICONIFIED);
            logFrameStateChange(frame3, oldState);
        });
        minItem.setEnabled(frame != null);
        put("minItem", minItem);

        JMenuItem maxItem = MenuFactory.createMenuItem("Maximize");
        maxItem.addActionListener(e -> {
            ComboFrame frame2 = getComboFrame();
            final int oldState = frame2.getExtendedState();
            // If already maximized, then unmaximize:
            if ((oldState & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH) {
                frame2.setExtendedState(Frame.NORMAL);
            }
            else {
                frame2.setExtendedState(Frame.MAXIMIZED_BOTH);
            }
            logFrameStateChange(frame2, oldState);
        });
        maxItem.setEnabled(frame != null);
        put("maxItem", maxItem);

        JMenuItem allItem = MenuFactory.createMenuItem("AllToFront");
        allItem.addActionListener(e -> {
            Menus.stream()
                    .map(UpdatableDisposableMenu::getComboFrame)
                    .filter(Objects::nonNull)
                    .forEach(window -> {
                        final var oldState = window.getExtendedState();
                        window.toFront();
                        logFrameStateChange(window, oldState);
                    });
        });
        put("allItem", allItem);

        if ((System.getProperty("lightcrafts.debug") != null) ||
            (System.getProperty("lightcrafts.journal") != null)) {

            addSeparator();

            JMenuItem journalItem = new JMenuItem("Event Journal");
            journalItem.addActionListener(e -> {
                ComboFrame frame1 = getComboFrame();
                JournalDialog.showJournalDialog(frame1);
            });
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
            menuItem.addActionListener(e -> {
                Frame frame1 = (Frame) get(frameKey);
                final var oldState = frame1.getExtendedState();
                frame1.toFront();
                logFrameStateChange(frame1, oldState);
            });
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

    private static void logFrameStateChange(Frame frame, int oldState) {
        final int newState = frame.getExtendedState();
        if (oldState == newState) return;
        logger.info( "Window state changed from {} ({}x{}) to {}",
                oldState, frame.getWidth(), frame.getHeight(), newState );
    }
}
