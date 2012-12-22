/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.prefs;

import com.lightcrafts.image.export.ImageExportOptions;
import com.lightcrafts.image.types.ImageType;
import com.lightcrafts.image.types.JPEGImageType;
import com.lightcrafts.image.types.TIFFImageType;
import static com.lightcrafts.prefs.Locale.LOCALE;
import com.lightcrafts.ui.export.ExportControls;
import com.lightcrafts.ui.export.ExportLogic;
import com.lightcrafts.ui.export.SaveOptions;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

class SavePrefsPanel extends JPanel implements ItemListener {

    // The combo box of ExportComboItems, one per output ImageType
    private JComboBox typeCombo;

    // A container holding the combo box and a label
    private Box typeBox;

    // The settings controls for the current output ImageType
    private ExportControls ctrls;

    // A header for the ExportControls
    private JLabel ctrlsTitle;

    // A container holding the export controls and a label
    private Box ctrlsTitleBox;

    // Remember hidden options for the unselected output ImageType
    private ExportComboItem otherComboItem;

    // Explanatory text
    private HelpArea help;

    SavePrefsPanel() {
        SaveOptions options = SaveOptions.getDefaultSaveOptions();
        ImageExportOptions export = SaveOptions.getExportOptions(options);

        List<ExportComboItem> filters = getExportAllComboItems();
        ImageType defaultType =
            SaveOptions.getExportOptions(options).getImageType();

        typeCombo = new JComboBox();
        ExportComboItem defaultItem = null;
        for (ExportComboItem filter : filters) {
            ImageExportOptions filterOptions = filter.getExportOptions();
            ImageType filterType = filterOptions.getImageType();
            if (defaultType.equals(filterType)) {
                // For the default filter, use the default options:
                filter = new ExportComboItem(export);
                defaultItem = filter;
            }
            typeCombo.addItem(filter);
        }
        if (defaultItem != null) {
            typeCombo.setSelectedItem(defaultItem);
        }
        typeCombo.setMaximumSize(typeCombo.getPreferredSize());

        typeCombo.addItemListener(this);

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        JLabel typeLabel = new JLabel(LOCALE.get("SaveTypeLabel"));

        typeBox = Box.createHorizontalBox();
        typeBox.add(typeLabel);
        typeBox.add(typeCombo);

        ctrls = new ExportControls(export, false);
        ctrls.setBorder(BorderFactory.createTitledBorder(""));

        ctrlsTitle = new JLabel(defaultItem.toString() + " Options");
        ctrlsTitleBox = Box.createHorizontalBox();
        ctrlsTitleBox.add(ctrlsTitle);
        ctrlsTitleBox.add(Box.createHorizontalGlue());

        String helpText = LOCALE.get("SaveHelp");
        help = new HelpArea();
        help.setText(helpText);

        Dimension helpSize = help.getPreferredSize();
        helpSize = new Dimension(helpSize.width, 70);
        help.setPreferredSize(helpSize);
        help.setMaximumSize(helpSize);

        add(Box.createVerticalGlue());
        add(Box.createVerticalStrut(8));
        add(typeBox);
        add(Box.createVerticalStrut(8));
        add(ctrlsTitleBox);
        add(Box.createVerticalStrut(8));
        add(ctrls);
        add(Box.createVerticalStrut(8));
        add(Box.createVerticalGlue());
        add(help);
    }

    void commit() {
        ExportComboItem item = (ExportComboItem) typeCombo.getSelectedItem();
        ImageExportOptions export = item.exportOptions;
        SaveOptions options;
        if (export.getImageType() == TIFFImageType.INSTANCE) {
            options = SaveOptions.createSidecarTiff(export);
        }
        else {
            options = SaveOptions.createSidecarJpeg(export);
        }
        SaveOptions.setDefaultSaveOptions(options);
    }

    // Respond to changes in the export image type combo box:
    public void itemStateChanged(ItemEvent event) {
        if (event.getStateChange() == ItemEvent.SELECTED) {
            ExportComboItem exportComboItem =
                (ExportComboItem) typeCombo.getSelectedItem();

            // The otherComboItem may be null, the first time this is called.
            ImageExportOptions newOptions = exportComboItem.getExportOptions();
            if (otherComboItem != null) {
                ImageExportOptions oldOptions = otherComboItem.getExportOptions();
                ExportLogic.mergeExportOptions(oldOptions, newOptions);
            }
            removeAll();

            ctrls = new ExportControls(newOptions, false);
            ctrls.setBorder(BorderFactory.createTitledBorder(""));

            ctrlsTitle.setText(exportComboItem.toString() + " Options");


            add(Box.createVerticalGlue());
            add(Box.createVerticalStrut(8));
            add(typeBox);
            add(Box.createVerticalStrut(8));
            add(ctrlsTitleBox);
            add(Box.createVerticalStrut(8));
            add(ctrls);
            add(Box.createVerticalStrut(8));
            add(Box.createVerticalGlue());
            add(help);
            
            repaint();

            otherComboItem = exportComboItem;
        }
    }

    private static List<ExportComboItem> getExportAllComboItems() {
        ExportComboItem filter;
        ArrayList<ExportComboItem> filters = new ArrayList<ExportComboItem>();

        filter = new ExportComboItem(TIFFImageType.INSTANCE.newExportOptions());
        filters.add(filter);

        filter = new ExportComboItem(JPEGImageType.INSTANCE.newExportOptions());
        filters.add(filter);

        return filters;
    }

    // Items for an export image type combo box:
    static class ExportComboItem {

        private ImageExportOptions exportOptions;

        private ExportComboItem(ImageExportOptions exportOptions) {
            this.exportOptions = exportOptions;
        }

        public String toString() {
            return exportOptions.getImageType().getName();
        }

        private ImageExportOptions getExportOptions() {
            return exportOptions;
        }
    }
}
