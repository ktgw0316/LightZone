/*
 * Copyright (C) 2020-     Masahiro Kitagawa
 */

package com.lightcrafts.app.batch;

import com.lightcrafts.image.export.ImageFileExportOptions;
import com.lightcrafts.platform.Platform;
import com.lightcrafts.ui.export.ExportMultiControls;
import lombok.Setter;
import lombok.val;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;

import static com.lightcrafts.app.batch.Locale.LOCALE;

public class BatchConfiguratorView implements BatchConfiguratorContract.View {
    @Setter
    @Deprecated
    private BatchConfiguratorPresenter presenter;

    final Frame parent;
    private JDialog dialog;
    private JTextField dirLabel;

    final private DocumentListener batchLabelDocumentListener = new DocumentListener() {
        @Override
        public void insertUpdate(DocumentEvent e) {
            changedUpdate(e);
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            changedUpdate(e);
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            presenter.onDocumentUpdate();
        }
    };

    public BatchConfiguratorView(BatchConfiguratorPresenter presenter, Frame parent) {
        this.presenter = presenter;
        this.presenter.attachView(this);
        this.parent = parent;
    }

    @Override
    public void createAndShowGUI() {
        dialog = new JDialog(parent);
        final ActionListener disposeAction = e -> dialog.setVisible(false);

        dirLabel = new JTextField(presenter.getDirLabelText());
        dirLabel.setEditable(false);

        val dirButton = new JButton(LOCALE.get("BatchConfOutputChooserButton"));
        dirButton.addActionListener(e -> presenter.onDirButtonPressed());

        val batchLabel = new JTextField(presenter.getBatchLabelText());
        batchLabel.getDocument().addDocumentListener(batchLabelDocumentListener);
        batchLabel.setPreferredSize(new Dimension(160, batchLabel.getPreferredSize().height));
        batchLabel.setMaximumSize(batchLabel.getPreferredSize());

        val exportCtrls = new ExportMultiControls(presenter.getImageExportOptions(), dialog, false);

        val start = new JButton(LOCALE.get("BatchConfStartButton"));
        start.setAlignmentX(.5f);
        start.addActionListener(e -> presenter.setStarted());
        start.addActionListener(disposeAction);

        val cancel = new JButton(LOCALE.get("BatchConfCancelButton"));
        cancel.setAlignmentX(.5f);
        cancel.addActionListener(disposeAction);

        val buttons = Box.createHorizontalBox();
        buttons.add(Box.createHorizontalGlue());
        buttons.add(start);
        buttons.add(Box.createHorizontalStrut(8));
        buttons.add(cancel);
        buttons.setMaximumSize(buttons.getPreferredSize());

        val batchLabelBox = new JPanel();
        batchLabelBox.setLayout(new BoxLayout(batchLabelBox, BoxLayout.X_AXIS));
        batchLabelBox.add(batchLabel);
        batchLabelBox.setBorder(BorderFactory.createTitledBorder(LOCALE.get("BatchConfNameLabel")));

        val batchBox = Box.createHorizontalBox();
        batchBox.add(batchLabelBox);
        batchBox.add(Box.createHorizontalGlue());

        val dirBox = new JPanel();
        dirBox.setLayout(new BoxLayout(dirBox, BoxLayout.X_AXIS));
        dirBox.add(new JLabel(presenter.getDirBoxLabel()));
        dirBox.add(Box.createHorizontalStrut(8));
        dirBox.add(dirLabel);
        dirBox.add(Box.createHorizontalStrut(8));
        dirBox.add(dirButton);

        val dirBoxBox = Box.createHorizontalBox();
        dirBoxBox.add(dirBox);
        dirBoxBox.add(Box.createHorizontalGlue());

        exportCtrls.setBorder(BorderFactory.createTitledBorder(LOCALE.get("BatchConfFormatBorder")));

        val content = new JPanel();
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

        presenter.configFor((ImageFileExportOptions) exportCtrls.getSelectedExportOptions());
    }

    @Override
    public File chooseDirectory(File directory) {
        val chooser = Platform.getPlatform().getFileChooser();
        return chooser.chooseDirectory(
                LOCALE.get("BatchConfOutputChooserDialogTitle"),
                directory, dialog, false);
    }

    @Override
    public void setDirLabelText(String dirLabelText) {
        SwingUtilities.invokeLater(() -> dirLabel.setText(presenter.getDirLabelText()));
    }
}
