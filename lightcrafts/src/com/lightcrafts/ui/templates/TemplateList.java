/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.templates;

import com.lightcrafts.platform.AlertDialog;
import com.lightcrafts.platform.FileChooser;
import com.lightcrafts.platform.Platform;
import com.lightcrafts.templates.TemplateDatabase;
import com.lightcrafts.templates.TemplateKey;
import com.lightcrafts.utils.xml.XmlDocument;

import static com.lightcrafts.ui.templates.Locale.LOCALE;
import com.lightcrafts.app.Application;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * A list component for showing, selecting, importing, exporting, and deleting
 * templates.  Also offers display in a modal dialog.
 */
public class TemplateList extends JPanel implements ListSelectionListener {

    // The import and export directory choices are sticky:
    private final static Preferences Prefs = Preferences.userRoot().node(
        "/com/lightcrafts/ui/templates"
    );
    private final static String RecentImportKey = "TemplateImportDir";
    private final static String RecentExportKey = "TemplateExportDir";

    private JList list;
    private JButton importButton;
    private JButton exportButton;
    private JButton deleteButton;

    private TemplateList() {
        setLayout(new BorderLayout());
        list = new JList(new DefaultListModel());
        list.setBorder(BorderFactory.createLineBorder(Color.gray));
        list.addListSelectionListener(this);
        add(new JScrollPane(list));

        updateFromTemplates();

        // Import and Export need file choosers.
        final FileChooser chooser = Platform.getPlatform().getFileChooser();

        importButton = new JButton(LOCALE.get("TemplateImportButton"));
        importButton.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    Frame frame = getAncestorFrame();
                    File dir = getRecentImportDir();
                    FilenameFilter lztFilter = new FilenameFilter() {
                        public boolean accept(File file, String name) {
                            return name.toLowerCase().endsWith(".lzt");
                        }
                    };
                    File file = chooser.openFile(
                        LOCALE.get("TemplateImportDialogTitle"),
                        dir, frame, lztFilter
                    );
                    if (file != null) {
                        importTemplate(file);
                        setRecentImportDir(file.getParentFile());
                    }
                }
            }
        );
        exportButton = new JButton(LOCALE.get("TemplateExportButton"));
        exportButton.setEnabled(false);
        exportButton.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    Frame frame = getAncestorFrame();
                    File dir = getRecentExportDir();
                    File file = chooser.chooseDirectory(
                        LOCALE.get("TemplateExportDialogTitle"),
                        dir, frame, false
                    );
                    if (file != null) {
                        exportTemplates(file);
                        setRecentExportDir(file);
                    }
                }
            }
        );
        deleteButton = new JButton(LOCALE.get("TemplateDeleteButton"));
        deleteButton.setEnabled(false);
        deleteButton.addActionListener(
            new ActionListener() {
                // Remove the selected templates from the template database
                // and also from the displayed list.
                public void actionPerformed(ActionEvent event) {
                    List<TemplateKey> keys = getSelectedTemplateKeys();
                    DefaultListModel model = (DefaultListModel) list.getModel();
                    for (TemplateKey key : keys) {
                        try {
                            TemplateDatabase.removeTemplateDocument(key);
                            model.removeElement(key);
                        }
                        catch (TemplateDatabase.TemplateException e) {
                            showError(LOCALE.get("TemplateDeleteError"), e);
                        }
                    }
                }
            }
        );
    }

    private void updateFromTemplates() {
        try {
            List<TemplateKey> keys = TemplateDatabase.getTemplateKeys();
            if (! keys.isEmpty()) {
                DefaultListModel model = new DefaultListModel();
                for (TemplateKey key : keys) {
                    model.addElement(key);
                }
                list.setModel(model);
                list.setEnabled(true);
            }
            else {
                DefaultListModel model = (DefaultListModel) list.getModel();
                model.removeAllElements();
                model.addElement(LOCALE.get("NoTemplatesMessage"));
                list.setEnabled(false);
            }
        }
        catch (TemplateDatabase.TemplateException e) {
            DefaultListModel model = (DefaultListModel) list.getModel();
            model.removeAllElements();
            model.addElement(LOCALE.get("TemplateStoreError"));
            list.setEnabled(false);
        }
    }

    private List<TemplateKey> getSelectedTemplateKeys() {
        Object[] rows = list.getSelectedValues();
        List<TemplateKey> keys = new ArrayList<TemplateKey>();
        for (Object row : rows) {
            TemplateKey key = (TemplateKey) row;
            keys.add(key);
        }
        return keys;
    }

    private void importTemplate(File file) {
        XmlDocument doc;
        try {
            InputStream in = new FileInputStream(file);
            doc = new XmlDocument(in);
        }
        catch (IOException e) {
            showError(
                LOCALE.get("TemplateImportError", file.getName()), e
            );
            return;
        }
        TemplateKey key = TemplateKey.importKey(file);
        try {
            TemplateDatabase.addTemplateDocument(doc, key, false);
        }
        catch (TemplateDatabase.TemplateException e) {
            showError(LOCALE.get("TemplateStoreError",  key.toString()), e);
            return;
        }
        updateFromTemplates();
    }

    private void exportTemplates(File dir) {
        if (dir.isFile()) {
            dir = dir.getParentFile();
        }
        List<TemplateKey> keys = getSelectedTemplateKeys();
        for (TemplateKey key : keys) {
            XmlDocument doc;
            try {
                doc = TemplateDatabase.getTemplateDocument(key);
            }
            catch (TemplateDatabase.TemplateException e) {
                showError(
                    LOCALE.get("TemplateAccessSpecificError", key.toString()), e
                );
                continue;
            }
            File file = new File(dir, key + ".lzt");
            try (OutputStream out = new FileOutputStream(file)) {
                doc.write(out);
            }
            catch (IOException e) {
                showError(
                    LOCALE.get(
                        "TemplateWriteError", key.toString(), file.getName()
                    ), e
                );
                return;
            }
            Platform.getPlatform().getAlertDialog().showAlert(
                getAncestorFrame(),
                LOCALE.get(
                    "TemplateWriteSuccess", key.toString(), file.getName()
                ),
                "", AlertDialog.WARNING_ALERT,
                LOCALE.get("TemplateWriteOkButton")
            );
        }
    }

    private void showError(String message, Throwable t) {
        Frame frame = getAncestorFrame();
        Application.showError(message, t, frame);
    }

    private Frame getAncestorFrame() {
        return (Frame) SwingUtilities.getAncestorOfClass(
            Frame.class, TemplateList.this
        );
    }

    private File getRecentImportDir() {
        String path = Prefs.get(RecentImportKey, new File(System.getProperty("user.home")).getAbsolutePath());
        return new File(path);
    }

    private void setRecentImportDir(File dir) {
        String path = dir.getAbsolutePath();
        Prefs.put(RecentImportKey, path);
    }

    private File getRecentExportDir() {
        String path = Prefs.get(RecentExportKey, new File(System.getProperty("user.home")).getAbsolutePath());
        return new File(path);
    }

    private void setRecentExportDir(File dir) {
        String path = dir.getAbsolutePath();
        Prefs.put(RecentExportKey, path);
    }

    public static void showDialog(Frame parent) {
        TemplateList templates = new TemplateList();
        JOptionPane.showOptionDialog(
            parent,
            templates,
            LOCALE.get("TemplateDialogTitle"),
            JOptionPane.OK_OPTION,
            JOptionPane.PLAIN_MESSAGE,
            null,
            new Object[] {
                LOCALE.get("TemplateDoneButton"),
                templates.deleteButton,
                templates.importButton,
                templates.exportButton
            },
            LOCALE.get("TemplateDoneButton")
        );
    }

    public void valueChanged(ListSelectionEvent e) {
        if (! e.getValueIsAdjusting()) {
            boolean empty = (list.getSelectedValue() == null);
            exportButton.setEnabled(! empty);
            deleteButton.setEnabled(! empty);
        }
    }

    public static void main(String[] args) {
        showDialog(null);
    }
}
