/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.app.menu;

import com.lightcrafts.app.ComboFrame;
import com.lightcrafts.app.Application;
import com.lightcrafts.ui.editor.EditorMode;
import com.lightcrafts.ui.browser.view.AbstractImageBrowser;
import com.lightcrafts.ui.browser.view.ImageBrowserActions;
import com.lightcrafts.ui.editor.Editor;
import com.lightcrafts.templates.TemplateDatabase;
import com.lightcrafts.templates.TemplateKey;
import com.lightcrafts.platform.Platform;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.*;
import java.lang.ref.WeakReference;
import java.io.File;
import java.util.List;

class BrowserMenu extends UpdatableDisposableMenu {

    private WeakReference<AbstractImageBrowser> browserRef =
        new WeakReference<AbstractImageBrowser>(null);

    private JMenu templateItem; // Needs enable/disable with selection
    private boolean wasTemplatesInitialized;// Sometimes always disabled

    private boolean wasVisible;

    BrowserMenu(ComboFrame frame) {
        super(frame, "Browser");
        if (frame == null) {
            // This is the Mac placeholder frame.
            addNoFrameAction();
        }
    }

    void update() {
        ComboFrame frame = getComboFrame();
        if (frame == null) {
            // This is the Mac placeholder frame.
            return;
        }
        AbstractImageBrowser newBrowser = frame.getBrowser();
        AbstractImageBrowser oldBrowser = browserRef.get();

        boolean isVisible = frame.isBrowserVisible();

        if ((newBrowser != oldBrowser) || (isVisible != wasVisible)) {
            removeAll();
            if (isVisible) {
                addBrowserActions();
            }
            else {
                addNoBrowserAction();
            }
            browserRef = new WeakReference<AbstractImageBrowser>(newBrowser);
            wasVisible = isVisible;
        }
        if (isVisible) {
            List<File> selection = newBrowser.getSelectedFiles();
            // If the submenu was successfully initialized, then update enabled.
            if (wasTemplatesInitialized) {
                templateItem.setEnabled(! selection.isEmpty());
            }
        }
        super.update();
    }

    private void addBrowserActions() {
        ComboFrame frame = getComboFrame();
        AbstractImageBrowser browser = frame.getBrowser();
        ImageBrowserActions actions = browser.getActions();

        add(actions.getLeftAction());
        add(actions.getRightAction());
        addRatingMenu();
        addSeparator();
        add(actions.getSelectLatestAction());
        add(actions.getSelectAllAction());
        add(actions.getSelectNoneAction());
        addSeparator();
        add(actions.getEditAction());
        add(actions.getShowFileInFolderAction());
        add(actions.getRenameAction());
        add(actions.getTrashAction());
        addSeparator();
        add(actions.getCopyAction());
        add(actions.getPasteAction());
        addTemplatesMenu();
        addSeparator();
        add(new BrowserExportMenuItem(frame));
        add(new BrowserPrintMenuItem(frame));
        addSeparator();
        add(actions.getRefreshAction());
        add(new RescanMenuItem(frame));
        addSeparator();
        add(actions.getShowHideTypesAction());
    }

    private void addNoBrowserAction() {
        JMenuItem noBrowser = MenuFactory.createMenuItem("NoBrowser");
        final ComboFrame frame = getComboFrame();
        noBrowser.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent actionEvent) {
                    // Enqueue, because this update can itself be
                    // called from a perspective change.
                    EventQueue.invokeLater(
                        new Runnable() {
                            public void run() {
                                final Editor editor = frame.getEditor();
                                editor.setMode( EditorMode.ARROW );
                                frame.showBrowserPerspective();
                            }
                        }
                    );
                }
            }
        );
        add(noBrowser);
    }

    private void addNoFrameAction() {
        JMenuItem noFrame = MenuFactory.createMenuItem("NoFrame");
        noFrame.setEnabled(false);
        add(noFrame);
    }

    private void addTemplatesMenu() {
        templateItem = MenuFactory.createMenu("BrowserTemplate");
        try {
            List<TemplateKey> keys = TemplateDatabase.getTemplateKeys();
            for (final TemplateKey key : keys) {
                JMenuItem item = new JMenuItem(key.toString());
                item.addActionListener(
                    new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            ComboFrame frame = getComboFrame();
                            AbstractImageBrowser browser = frame.getBrowser();
                            List<File> list = browser.getSelectedFiles();
                            if (list.size() > 0) {
                                File[] files = list.toArray(new File[0]);
                                Application.applyTemplate(frame, files, key);
                            }
                        }
                    }
                );
                templateItem.add(item);
                wasTemplatesInitialized = true;
            }
        }
        catch (TemplateDatabase.TemplateException e) {
            System.out.println("Couldn't initialize browser templates menu");
            e.printStackTrace();
            templateItem.setEnabled(false);
            wasTemplatesInitialized = false;
        }
        templateItem.setEnabled(false);

        add(templateItem);
    }

    private void addRatingMenu() {
        ComboFrame frame = getComboFrame();
        AbstractImageBrowser browser = frame.getBrowser();
        ImageBrowserActions actions = browser.getActions();

        JMenu menu = MenuFactory.createMenu("Rate");
        for (Action action : actions.getRatingActions()) {
            JMenuItem item = menu.add(action);
            String name = (String) action.getValue(Action.NAME);
            // On Windogs only the core fonts seem to see stars
            if (Platform.isWindows()) {
                char star = '\u2605';
                if (name.length() > 0 && name.charAt(0) == star)
                    item.setFont(new Font("Serif", Font.PLAIN, 14));
            }
        }
//        menu.addSeparator();
//        for (Action action : actions.getRatingAdvanceActions()) {
//            menu.add(action);
//        }
        menu.addSeparator();
        menu.add(actions.getClearRatingAction());
        // menu.add(actions.getClearRatingAdvanceAction());
        add(menu);
    }
}

