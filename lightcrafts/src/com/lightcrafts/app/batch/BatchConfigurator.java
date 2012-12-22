/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.app.batch;

import static com.lightcrafts.app.batch.Locale.LOCALE;
import com.lightcrafts.platform.FileChooser;
import com.lightcrafts.platform.Platform;
import com.lightcrafts.ui.export.ExportMultiControls;
import com.lightcrafts.image.export.ImageFileExportOptions;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;

/**
 * This conducts the dialog that must preceed TemplateApplicator.  It lets a
 * user confirm the list of files that will be processed, set a flag to
 * specify whether files should be created or overwritten, and if files will
 * be created, it also lets the user specify a name pattern for the new
 * files.
 */
public class BatchConfigurator {

    // User clicked "start" (not "cancel", not disposed the dialog):
    private static boolean started;

    public static BatchConfig showDialog(
        File[] files, final Frame parent, boolean isBatchExport
    ) {
        final JDialog dialog = new JDialog(parent);

        final BatchConfig config = new BatchConfig();

        // Remember the last values:
        String saveKey = isBatchExport ? "Export" : "Apply";
        config.restoreFromPrefs(saveKey);

        // For regular template application, override the restored output
        // directory with the directory of the first input file.  (Batch
        // export keeps its sticky output directory.)
        if (! isBatchExport || config.directory == null) {
            config.directory = files[0].getParentFile();
        }
        // Initialize components:

        final JTextField dirLabel = new JTextField(config.directory.getName());
        dirLabel.setEditable(false);

        JButton dirButton = new JButton(
            LOCALE.get("BatchConfOutputChooserButton")
        );
        dirButton.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    Platform platform = Platform.getPlatform();
                    FileChooser chooser = platform.getFileChooser();
                    File directory = chooser.chooseDirectory(
                        LOCALE.get("BatchConfOutputChooserDialogTitle"),
                        config.directory, dialog, false
                    );
                    if (directory != null) {
                        config.directory = directory;
                        String name = directory.getName();
                        dirLabel.setText(name);
                    }
                }
            }
        );
        final JTextField batchLabel = new JTextField(config.name);
        batchLabel.getDocument().addDocumentListener(
            new DocumentListener() {
                public void insertUpdate(DocumentEvent e) {
                    changedUpdate(e);
                }
                public void removeUpdate(DocumentEvent e) {
                    changedUpdate(e);
                }
                public void changedUpdate(DocumentEvent e) {
                    config.name = batchLabel.getText();
                }
            }
        );
        batchLabel.setPreferredSize(
            new Dimension(160, batchLabel.getPreferredSize().height)
        );
        batchLabel.setMaximumSize(batchLabel.getPreferredSize());

        ExportMultiControls exportCtrls =
            new ExportMultiControls(config.export, dialog, false);

        JButton start = new JButton(LOCALE.get("BatchConfStartButton"));
        start.setAlignmentX(.5f);

        JButton cancel = new JButton(LOCALE.get("BatchConfCancelButton"));
        cancel.setAlignmentX(.5f);

        started = false;

        ActionListener disposeAction = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dialog.setVisible(false);
            }
        };
        start.addActionListener(disposeAction);
        cancel.addActionListener(disposeAction);

        start.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    started = true;
                }
            }
        );
        // Define layout:

        Box buttons = Box.createHorizontalBox();
        buttons.add(Box.createHorizontalGlue());
        buttons.add(start);
        buttons.add(Box.createHorizontalStrut(8));
        buttons.add(cancel);
        buttons.setMaximumSize(buttons.getPreferredSize());

        JPanel batchLabelBox = new JPanel();
        batchLabelBox.setLayout(new BoxLayout(batchLabelBox, BoxLayout.X_AXIS));
        batchLabelBox.add(batchLabel);
        batchLabelBox.setBorder(
            BorderFactory.createTitledBorder(
                LOCALE.get("BatchConfNameLabel")
            )
        );
        Box batchBox = Box.createHorizontalBox();
        batchBox.add(batchLabelBox);
        batchBox.add(Box.createHorizontalGlue());

        JPanel dirBox = new JPanel();
        dirBox.setLayout(new BoxLayout(dirBox, BoxLayout.X_AXIS));
        if (isBatchExport) {
            dirBox.add(new JLabel(LOCALE.get("BatchConfExportOutputLabel")));
        }
        else {
            dirBox.add(new JLabel(LOCALE.get("BatchConfSaveOutputLabel")));
        }
        dirBox.add(Box.createHorizontalStrut(8));
        dirBox.add(dirLabel);
        dirBox.add(Box.createHorizontalStrut(8));
        dirBox.add(dirButton);

        Box dirBoxBox = Box.createHorizontalBox();
        dirBoxBox.add(dirBox);
        dirBoxBox.add(Box.createHorizontalGlue());

        exportCtrls.setBorder(
            BorderFactory.createTitledBorder(
                LOCALE.get("BatchConfFormatBorder")
            )
        );
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.add(dirBoxBox);
        content.add(Box.createVerticalStrut(8));
        content.add(batchBox);
        content.add(Box.createVerticalStrut(8));
        content.add(exportCtrls);
        content.add(Box.createVerticalStrut(8));
        content.add(buttons);
        content.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        // Hook up the escape key:
        content.registerKeyboardAction(
            disposeAction,
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW
        );
        // Set up the dialog:
        dialog.setContentPane(content);
        dialog.getRootPane().setDefaultButton(start);
        dialog.setModal(true);
        dialog.setTitle(LOCALE.get("BatchConfDialogTitle"));
        dialog.pack();
        dialog.setResizable(false);
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);

        if (started) {
            config.export =
                (ImageFileExportOptions) exportCtrls.getSelectedExportOptions();
            // Remember choices for next time:
            config.saveToPrefs(saveKey);
            return config;
        }
        else {
            return null;
        }
    }

    public static void main(String[] args) throws Exception {
        UIManager.setLookAndFeel(Platform.getPlatform().getLookAndFeel());
        EventQueue.invokeLater(
            new Runnable() {
                public void run() {
                    File[] files = new File[100];
                    for (int n=0; n<100; n++) {
                        files[n] = new File("/a/b/" + n + ".lzn");
                    }
                    showDialog(files, null, false);
                    System.exit(0);
                }
            }
        );
    }
}
