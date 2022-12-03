/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.export;

import com.lightcrafts.image.export.ImageExportOptions;
import com.lightcrafts.image.types.ImageType;
import com.lightcrafts.image.types.JPEGImageType;
import com.lightcrafts.image.types.TIFFImageType;
import com.lightcrafts.platform.Platform;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ExportMultiControls extends JPanel implements ItemListener {

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
    // Keep track of the expanded/collapsed state of the controls toggle:
    private ExportCtrlToggle toggle;

    private JComboBox combo;

    private ExportComboItem oldFilter;

    private boolean textResize;

    // An enclosing window, to repack when the advanced options change:
    private Window window;

    public ExportMultiControls(
        ImageExportOptions options, Window window, boolean textResize
    ) {
        this.window = window;
        this.textResize = textResize;

        ExportComboItem defaultItem = null;

        // Set up all the image format options:
        List<ExportComboItem> filters = getAllFilters();
        ImageType defaultType = options.getImageType();

        combo = new JComboBox();
        combo.addItemListener(this);

        setLayout(new BorderLayout());

        // Initialize all the image type filters:
        for (ExportComboItem filter : filters) {
            ImageExportOptions filterOptions = filter.getExportOptions();
            ImageType filterType = filterOptions.getImageType();
            if (defaultType.equals(filterType)) {
                // For the default filter, use the default options:
                filter = new ExportComboItem(options);
                defaultItem = filter;
            }
        }
        // Add the default filter first, so that other filters will inherit
        // its settings according to ExportLogic.mergeExportOptions().
        if (defaultItem != null) {
            combo.addItem(defaultItem);
        }
        for (ExportComboItem filter : filters) {
            ImageExportOptions filterOptions = filter.getExportOptions();
            ImageType filterType = filterOptions.getImageType();
            if (! defaultType.equals(filterType)) {
                combo.addItem(filter);
            }
        }
        if (defaultItem != null) {
            combo.setSelectedItem(defaultItem);
        }
        combo.setMaximumSize(combo.getPreferredSize());
    }

    public ImageExportOptions getSelectedExportOptions() {
        ExportComboItem filter = (ExportComboItem) combo.getSelectedItem();
        ImageExportOptions options = filter.getExportOptions();
        return options;
    }

    private static List<ExportComboItem> getAllFilters() {
        ExportComboItem filter;
        ArrayList<ExportComboItem> filters = new ArrayList<ExportComboItem>();

        filter = new ExportComboItem(TIFFImageType.INSTANCE.newExportOptions());
        filters.add(filter);

        filter = new ExportComboItem(JPEGImageType.INSTANCE.newExportOptions());
        filters.add(filter);

        return filters;
    }

    // Respond to changes in the export image type combo box:
    public void itemStateChanged(ItemEvent event) {
        if (event.getStateChange() == ItemEvent.SELECTED) {
            ExportComboItem newFilter =
                (ExportComboItem) combo.getSelectedItem();

            // The oldFilter may be null, first time an ExportComboItem is set.
            ImageExportOptions newOptions = newFilter.getExportOptions();
            if (oldFilter != null) {
                ImageExportOptions oldOptions = oldFilter.getExportOptions();
                ExportLogic.mergeExportOptions(oldOptions, newOptions);
            }
            ExportControls ctrls = new ExportControls(newOptions, textResize);
            toggle = new ExportCtrlToggle(ctrls, window);

            removeAll();

            Box comboBox = Box.createHorizontalBox();
            comboBox.add(combo);
            comboBox.add(Box.createHorizontalGlue());

            add(comboBox, BorderLayout.NORTH);
            add(toggle);

            if (window != null) {
                window.pack();
            }
            repaint();

            oldFilter = newFilter;
        }
    }

    public static void main(String[] args) throws Exception {
        UIManager.setLookAndFeel(Platform.getPlatform().getLookAndFeel());

        TIFFImageType.ExportOptions options =
            TIFFImageType.INSTANCE.newExportOptions();
        File dir = new File(System.getProperty("user.home"));
        File file = new File(dir, "test.tif");
        options.setExportFile(file);

        JFrame frame = new JFrame();

        ExportMultiControls multi =
            new ExportMultiControls(options, frame, false);

        frame.setContentPane(multi);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}
