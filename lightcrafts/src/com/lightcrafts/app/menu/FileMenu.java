/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.app.menu;

import com.lightcrafts.app.Application;
import com.lightcrafts.app.ComboFrame;
import com.lightcrafts.ui.editor.EditorMode;
import com.lightcrafts.platform.Platform;
import com.lightcrafts.ui.editor.ModeManager;
import com.lightcrafts.ui.editor.Editor;

import javax.swing.*;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.List;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;
import java.util.prefs.Preferences;

final class FileMenu
    extends UpdatableDisposableMenu implements PreferenceChangeListener
{
    FileMenu(ComboFrame frame) {
        super(frame, "File");

        add(new NewMenuItem(frame));
        add(new OpenMenuItem(frame));

        final JMenu recentFileMenu = MenuFactory.createMenu("RecentFile");
        // Remember this menu, for updateRecentFiles()
        put("recentFile", recentFileMenu);
        add(recentFileMenu);

        final JMenu recentFolderMenu = MenuFactory.createMenu("RecentFolder");
        // Remember this menu, for updateRecentFolders()
        put("recentFolder", recentFolderMenu);
        add(recentFolderMenu);

        addSeparator();

        add(new CloseDocMenuItem(frame));
//        add(new CloseMenuItem(frame));
        add(new SaveMenuItem(frame));
        add(new SaveAsMenuItem(frame));
        add(new RevertMenuItem(frame));

        addSeparator();

        add(new ExportMenuItem(frame));
        add(new SendMenuItem(frame));
        add(new ShowInMenuItem(frame));

        addSeparator();

        add(new SaveTemplateMenuItem(frame));

        final TemplateMenu templateMenu = new TemplateMenu(frame);
        add(templateMenu);

        // Update the template stuff whenever this menu is displayed:
        addMenuListener(
            new MenuListener() {
                public void menuSelected(MenuEvent e) {
                    templateMenu.update();
                }
                public void menuDeselected(MenuEvent e) {
                }
                public void menuCanceled(MenuEvent e) {
                }
            }
        );
        add(new ManageTemplatesMenuItem(frame));

        addSeparator();

        add(new PrintMenuItem(frame));

        if (Platform.getType() != Platform.MacOSX) {
            addSeparator();
            add(new ExitMenuItem(frame));
        }
        // Update the "recent files" submenu when preferences change:
        Preferences prefs;
        prefs = Application.getPreferences();
        prefs.addPreferenceChangeListener(this);
    }

    private void updateRecentFiles() {
        JMenu recentMenu = (JMenu) get("recentFile");
        if (recentMenu == null) {
            // This may be called after dispose(), because of rethreading
            // of the preference change event callback.
            return;
        }
        recentMenu.removeAll();
        List<File> recent = Application.getRecentFiles();
        for (final File file : recent) {
            String path = file.getAbsolutePath();
            JMenuItem menuItem = new JMenuItem(path);
            menuItem.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        final ComboFrame frame = getComboFrame();
                        if (frame != null) {
                            final Editor editor = frame.getEditor();
                            editor.setMode( EditorMode.ARROW );
                        }
                        else {
                            Application.openEmpty();
                        }
                        Application.open(frame, file);
                    }
                }
            );
            recentMenu.add(menuItem);
        }
        if (recent.isEmpty()) {
            JMenuItem emptyItem = MenuFactory.createMenuItem("NoRecentFile");
            emptyItem.setEnabled(false);
            recentMenu.add(emptyItem);
        }
        recentMenu.addSeparator();

        JMenuItem clearItem = MenuFactory.createMenuItem("ClearRecentFile");
        clearItem.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    Application.clearRecentFiles();
                }
            }
        );
        recentMenu.add(clearItem);
    }

    private void updateRecentFolders() {
        JMenu recentMenu = (JMenu) get("recentFolder");
        if (recentMenu == null) {
            // This may be called after dispose(), because of rethreading
            // of the preference change event callback.
            return;
        }
        recentMenu.removeAll();
        List<File> recent = Application.getRecentFolders();
        for (final File folder : recent) {
            String path = folder.getAbsolutePath();
            JMenuItem menuItem = new JMenuItem(path);
            menuItem.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        final ComboFrame frame = getComboFrame();
                        if (frame != null) {
                            final Editor editor = frame.getEditor();
                            editor.setMode( EditorMode.ARROW );
                        }
                        else {
                            Application.openEmpty();
                        }
                        Application.openRecentFolder(frame, folder);
                    }
                }
            );
            recentMenu.add(menuItem);
        }
        if (recent.isEmpty()) {
            JMenuItem emptyItem = MenuFactory.createMenuItem("NoRecentFolder");
            emptyItem.setEnabled(false);
            recentMenu.add(emptyItem);
        }
        recentMenu.addSeparator();

        JMenuItem clearItem = MenuFactory.createMenuItem("ClearRecentFolder");
        clearItem.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    Application.clearRecentFolders();
                }
            }
        );
        recentMenu.add(clearItem);
    }

    // Update the "Browse Recent" submenu
    void update() {
        super.update();
    }

    public void preferenceChange(PreferenceChangeEvent event) {
        EventQueue.invokeLater(
            new Runnable() {
                public void run() {
                    updateRecentFiles();
                    updateRecentFolders();
                }
            }
        );
    }

    public void dispose() {
        super.dispose();
        Preferences prefs = Application.getPreferences();
        prefs.removePreferenceChangeListener(this);
    }
}
