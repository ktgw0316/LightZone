/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.prefs;

import com.lightcrafts.application.prefs.PreferencesMessage;
import com.lightcrafts.domain.prefs.SaveFormat;
import com.lightcrafts.image.export.ImageExportOptions;
import com.lightcrafts.image.types.JPEGImageType;
import com.lightcrafts.image.types.TIFFImageType;
import com.lightcrafts.ui.export.ExportControls;
import com.lightcrafts.ui.export.ExportLogic;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.List;
import java.util.function.Consumer;

import static com.lightcrafts.ui.prefs.Locale.LOCALE;

class SavePrefsPanel extends JPanel implements ItemListener {
    private final JComboBox<ExportComboItem> typeCombo = new JComboBox<>();
    private final Box typeBox;
    private ExportControls controls;
    private final JLabel controlsTitle;
    private final Box controlsTitleBox;
    private final HelpArea help;
    private ExportComboItem otherComboItem;
    private boolean rendering;

    SavePrefsPanel(
        SaveFormat selectedFormat,
        ImageExportOptions defaultOptions,
        Consumer<PreferencesMessage> dispatch
    ) {
        List<ImageExportOptions> filters =
            SavePrefsPanelOptions.getExportOptions(defaultOptions);
        ExportComboItem selectedItem = null;
        for (ImageExportOptions options : filters) {
            ExportComboItem item = new ExportComboItem(options);
            typeCombo.addItem(item);
            if (formatFor(options) == selectedFormat) {
                selectedItem = item;
            }
        }
        if (selectedItem == null) {
            throw new IllegalStateException(
                "No save options for selected format: " + selectedFormat
            );
        }
        typeCombo.setSelectedItem(selectedItem);
        typeCombo.setMaximumSize(typeCombo.getPreferredSize());
        typeBox = Box.createHorizontalBox();
        typeBox.add(new JLabel(LOCALE.get("SaveTypeLabel")));
        typeBox.add(typeCombo);

        controls = new ExportControls(
            selectedItem.getExportOptions(), false
        );
        controls.setBorder(BorderFactory.createTitledBorder(""));
        controlsTitle = new JLabel(selectedItem + " Options");
        controlsTitleBox = Box.createHorizontalBox();
        controlsTitleBox.add(controlsTitle);
        controlsTitleBox.add(Box.createHorizontalGlue());

        help = new HelpArea();
        help.setText(LOCALE.get("SaveHelp"));
        Dimension size = new Dimension(help.getPreferredSize().width, 70);
        help.setPreferredSize(size);
        help.setMaximumSize(size);

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(Box.createVerticalGlue());
        add(Box.createVerticalStrut(8));
        add(typeBox);
        add(Box.createVerticalStrut(8));
        add(controlsTitleBox);
        add(Box.createVerticalStrut(8));
        add(controls);
        add(Box.createVerticalStrut(8));
        add(Box.createVerticalGlue());
        add(help);

        otherComboItem = selectedItem;
        typeCombo.addItemListener(this);
        this.dispatch = dispatch;
    }

    private final Consumer<PreferencesMessage> dispatch;

    ImageExportOptions getSelectedExportOptions() {
        ExportComboItem item =
            (ExportComboItem) typeCombo.getSelectedItem();
        if (item == null) {
            throw new IllegalStateException("No save format is selected");
        }
        return item.getExportOptions();
    }

    void render(SaveFormat format) {
        rendering = true;
        try {
            for (int i = 0; i < typeCombo.getItemCount(); i++) {
                ExportComboItem item = typeCombo.getItemAt(i);
                if (formatFor(item.getExportOptions()) == format) {
                    typeCombo.setSelectedItem(item);
                    return;
                }
            }
            throw new IllegalStateException(
                "No save options for selected format: " + format
            );
        } finally {
            rendering = false;
        }
    }

    @Override
    public void itemStateChanged(ItemEvent event) {
        if (event.getStateChange() != ItemEvent.SELECTED) {
            return;
        }
        ExportComboItem exportComboItem =
            (ExportComboItem) typeCombo.getSelectedItem();
        ImageExportOptions newOptions = exportComboItem.getExportOptions();
        if (otherComboItem != null) {
            ExportLogic.mergeExportOptions(
                otherComboItem.getExportOptions(), newOptions
            );
        }
        showOptions(exportComboItem);
        otherComboItem = exportComboItem;
        if (!rendering) {
            dispatch.accept(new PreferencesMessage.SaveFormatChanged(
                formatFor(newOptions)
            ));
        }
    }

    private void showOptions(ExportComboItem selected) {
        removeAll();
        controls = new ExportControls(selected.getExportOptions(), false);
        controls.setBorder(BorderFactory.createTitledBorder(""));
        controlsTitle.setText(selected + " Options");
        add(Box.createVerticalGlue());
        add(Box.createVerticalStrut(8));
        add(typeBox);
        add(Box.createVerticalStrut(8));
        add(controlsTitleBox);
        add(Box.createVerticalStrut(8));
        add(controls);
        add(Box.createVerticalStrut(8));
        add(Box.createVerticalGlue());
        add(help);
        revalidate();
        repaint();
    }

    private static SaveFormat formatFor(ImageExportOptions options) {
        if (options.getImageType() == TIFFImageType.INSTANCE) {
            return SaveFormat.TIFF;
        }
        if (options.getImageType() == JPEGImageType.INSTANCE) {
            return SaveFormat.JPEG;
        }
        throw new IllegalArgumentException(
            "Unsupported save image type: " +
                options.getImageType().getName()
        );
    }

    static class ExportComboItem {
        private final ImageExportOptions exportOptions;

        private ExportComboItem(ImageExportOptions exportOptions) {
            this.exportOptions = exportOptions;
        }

        @Override
        public String toString() {
            return exportOptions.getImageType().getName();
        }

        private ImageExportOptions getExportOptions() {
            return exportOptions;
        }
    }
}
