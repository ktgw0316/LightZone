/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.export;

import com.lightcrafts.image.export.ImageExportOptions;
import com.lightcrafts.image.types.ImageType;
import com.lightcrafts.image.types.JPEGImageType;
import com.lightcrafts.image.types.TIFFImageType;
import com.lightcrafts.utils.xml.XmlDocument;
import com.lightcrafts.platform.Platform;

import static com.lightcrafts.ui.export.Locale.LOCALE;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ExportDialog extends JFileChooser {

    static class ExportFilter extends FileFilter {

        private ImageExportOptions exportOptions;

        private ExportFilter(ImageExportOptions exportOptions) {
            this.exportOptions = exportOptions;
        }

        @Override
        public boolean accept(File f) {
            return true;
        }

        @Override
        public String getDescription() {
            return exportOptions.getImageType().getName();
        }

        private String getSuffix() {
            return exportOptions.getImageType().getExtensions()[0];
        }

        private ImageExportOptions getExportOptions() {
            return exportOptions;
        }
    }
    // When we're showing a dialog, update its controls with filter changes:
    private JDialog dialog;
    private JComponent controls;
    private JComponent buttons;

    // The default button for the dialog:
    private JButton exportButton;

    // This is the ComponentUI backdoor for validating and polishing the
    // chooser's text field input when the user clicks "Export":
    private Action approveAction;

    ExportDialog(ImageExportOptions options) {
        setAcceptAllFileFilterUsed(false);

        setApproveButtonText(LOCALE.get("ExportButton"));

        ExportFilter defaultFilter = null;

        // Set up all the image format options:
        List<ExportFilter> filters = getAllFilters();
        ImageType defaultType = options.getImageType();

        // Update the file's suffix and options when the filter changes:
        addPropertyChangeListener(
            new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent event) {
                    if (event.getPropertyName().equals(
                        JFileChooser.FILE_FILTER_CHANGED_PROPERTY
                    )) {
                        ExportFilter oldFilter =
                            (ExportFilter) event.getOldValue();
                        ExportFilter newFilter =
                            (ExportFilter) event.getNewValue();
                        fileFilterChanged(oldFilter, newFilter);
                    }
                }
            }
        );
        // Initialize all the image type filters:
        for (ExportFilter filter : filters) {
            ImageExportOptions filterOptions = filter.getExportOptions();
            ImageType filterType = filterOptions.getImageType();
            if (defaultType.equals(filterType)) {
                // For the default filter, use the default options:
                filter = new ExportFilter(options);
                defaultFilter = filter;
            }
            addChoosableFileFilter(filter);
        }
        File file = options.getExportFile();
        if (file != null) {
            setSelectedFile(file);
        }
        if (defaultFilter != null) {
            setFileFilter(defaultFilter);
        }
        setControlButtonsAreShown(false);

        // Remember the "approve-selection" action, so it can be invoked
        // when the user clicks "Export".  (Validates the text field value,
        // so it can be accessed from getSelectedFile().)

        ActionMap map = SwingUtilities.getUIActionMap(this);
        approveAction = map.get("approveSelection");
    }

    /**
     * Add ExportControls and control buttons to the JFileChooser dialog.
     */
    @Override
    protected JDialog createDialog(Component parent) {
        dialog = super.createDialog(parent);
        initButtons();
        initKeyboardActions();
        // This adds controls to the layout:
        fileFilterChanged(null, (ExportFilter) getFileFilter());
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        return dialog;
    }

    ImageExportOptions getSelectedExportOptions() {
        ExportFilter filter = (ExportFilter) getFileFilter();
        ImageExportOptions options = filter.getExportOptions();
        File file = getSelectedFile();
        final String extension = filter.getSuffix();
        file = ExportNameUtility.setFileExtension(file, extension);
        options.setExportFile(file);
        return options;
    }

    private static List<ExportFilter> getAllFilters() {
        ExportFilter filter;
        ArrayList<ExportFilter> filters = new ArrayList<ExportFilter>();

        filter = new ExportFilter(TIFFImageType.INSTANCE.newExportOptions());
        filters.add(filter);

        filter = new ExportFilter(JPEGImageType.INSTANCE.newExportOptions());
        filters.add(filter);

        return filters;
    }

    // Keep the selected file and the options control synchronized with the
    // current file filter.

    private void fileFilterChanged(
        ExportFilter oldFilter, ExportFilter newFilter
    ) {
        // The oldFilter may be null, the first time an ExportFilter is set.
        ImageExportOptions newOptions = newFilter.getExportOptions();
        if (dialog != null) {
            Container content = dialog.getContentPane();
            if (controls != null) {
                content.remove(controls);
                controls.remove(buttons);
            }
            if (oldFilter != null) {
                ImageExportOptions oldOptions = oldFilter.getExportOptions();
                ExportLogic.mergeExportOptions(oldOptions, newOptions);
            }
            ExportControls ctrls = new ExportControls(newOptions, true);

            // Keep track of the expanded/collapsed state of the controls toggle:
            ExportCtrlToggle toggle = new ExportCtrlToggle(ctrls, dialog);

            controls = Box.createVerticalBox();
            controls.add(toggle);
            controls.add(buttons);

            dialog.getRootPane().setDefaultButton(exportButton);

            content.add(controls, BorderLayout.SOUTH);

            dialog.pack();
        }
        // Update the selected file for suffix and uniqueness:
        File file = getSelectedFile();
        if ((file != null) && (! file.isDirectory())) {
            String ext = newFilter.getSuffix();
            file = ExportNameUtility.setFileExtension(file, ext);
            final File uniqFile =
                ExportNameUtility.ensureNotExists(file);

            // We have to enqueue, and we have to set to null
            // first, so we don't collide with setSelectedFile()
            // calls from filter change methods in the component
            // UI:
            EventQueue.invokeLater(
                new Runnable() {
                    public void run() {
                        setSelectedFile(null);
                        setSelectedFile(uniqFile);
                    }
                }
            );
        }
    }

    // Initialize the "Export" and "Cancel" buttons in the buttons member.

    private void initButtons() {
        exportButton = new JButton(LOCALE.get("ExportButton"));
        JButton cancelButton = new JButton(LOCALE.get("CancelButton"));

        cancelButton.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    ExportDialog.this.cancelSelection();
                }
            }
        );
        exportButton.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    ExportDialog.this.approveSelection();
                    approveAction.actionPerformed(null);
                }
            }
        );
        buttons = Box.createHorizontalBox();
        buttons.add(Box.createHorizontalGlue());
        buttons.add(cancelButton);
        buttons.add(Box.createHorizontalStrut(6));
        buttons.add(exportButton);

        Border border = BorderFactory.createEmptyBorder(6, 10, 10, 10);
        buttons.setBorder(border);
    }

    private void initKeyboardActions() {
        JComponent content = (JComponent) dialog.getContentPane();
        content.registerKeyboardAction(
            new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    ExportDialog.this.approveSelection();
                    approveAction.actionPerformed(null);
                }
            },
            KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW
        );
        content.registerKeyboardAction(
            new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    ExportDialog.this.cancelSelection();
                }
            },
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW
        );
    }

    public static ImageExportOptions showDialog(
        ImageExportOptions options,
        Frame parent
    ) {
        ExportDialog chooser = new ExportDialog(options);

        chooser.setDialogTitle(LOCALE.get("ExportTitle"));
        int result = chooser.showDialog(parent, LOCALE.get("ExportButton"));
        if (result != JFileChooser.APPROVE_OPTION) {
            return null;
        }
        options = chooser.getSelectedExportOptions();
        return options;
    }

    public static void main(String[] args) throws Exception {
        UIManager.setLookAndFeel(Platform.getPlatform().getLookAndFeel());

        final TIFFImageType.ExportOptions options =
            TIFFImageType.INSTANCE.newExportOptions();
        options.originalWidth.setValue(400);
        options.originalHeight.setValue(300);
        options.resizeWidth.setValue(400);
        options.resizeHeight.setValue(300);
        File dir = new File(System.getProperty("user.home"));
        File file = new File(dir, "test.tif");
        options.setExportFile(file);

        // Must enqueue the dialog because the file filter listener
        // enqueues some of its actions:
        final XmlDocument doc = new XmlDocument("Root");
        EventQueue.invokeAndWait(
            new Runnable() {
                public void run() {
                    ImageExportOptions o = showDialog(options, null);
                    if (o != null) {
                        o.write(doc.getRoot());
                    }
                    else {
                        System.out.println("cancelled");
                        System.exit(0);
                    }
                }
            }
        );
        doc.write(System.out);
        ImageExportOptions.read(doc.getRoot());
        System.exit(0);
    }
}
