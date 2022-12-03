/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.browser.view;

import com.lightcrafts.ui.LightZoneSkin;
import com.lightcrafts.ui.browser.model.ImageDatum;
import static com.lightcrafts.ui.browser.view.Locale.LOCALE;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.List;

/**
 * A simple horizontal box of labels, each showing a different piece of
 * text information derived from an ImageBrowserEvent.
 */
public class ImageBrowserFooter extends Box implements ImageBrowserListener {

    private JLabel lead;
    private JLabel all;
    private JLabel error;

    public ImageBrowserFooter() {
        super(BoxLayout.X_AXIS);

        lead = new JLabel();
        all = new JLabel(LOCALE.get("NoImagesSelectedText")); // Init's preferred height
        error = new JLabel();

        Font font = lead.getFont();
        font = font.deriveFont(10f);
        lead.setFont(font);
        all.setFont(font);
        error.setFont(font);

        lead.setForeground(LightZoneSkin.Colors.ToolPanesForeground);
        all.setForeground(LightZoneSkin.Colors.ToolPanesForeground);
        error.setForeground(LightZoneSkin.Colors.ToolPanesForeground);

        lead.setAlignmentX(0f);
        all.setAlignmentX(.5f);
        error.setAlignmentX(1f);

        add(lead);
        add(Box.createHorizontalGlue());
        add(error);
        add(Box.createHorizontalGlue());
        add(all);

        setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
    }
    
    public void selectionChanged(ImageBrowserEvent event) {
        ImageDatum datum = event.getLead();
        File lead = (datum != null) ? datum.getFile() : null;
        List<File> files = event.getFiles();
        int count = event.getImageCount();
        setSelectedFiles(lead, files, count);
    }

    public void setSelectedFiles(File leadFile, List<File> files, int total) {
        if (leadFile != null) {
            String sizeText = getSizeText(leadFile.length());
            lead.setText(leadFile.getName() + " (" + sizeText + ")");
        }
        else {
            lead.setText("");
        }
        long bytes = 0;
        for (File file : files) {
            bytes += file.length();
        }
        String sizeText = getSizeText(bytes);

        int size = files.size();
        if (size == 0) {
            if (total > 0) {
                if (total == 1) {
                    all.setText(LOCALE.get("SingleImageText"));
                }
                else {
                    all.setText(total + " " + LOCALE.get("MultipleImagesText"));
                }
            }
            else {
                all.setText(LOCALE.get("NoImagesText"));
            }
        }
        else if (size == total) {
            if (size == 1) {
                all.setText(LOCALE.get("SingleImageSelectedText") +
                    " (" + sizeText + ")");
            }
            else {
                all.setText(
                    size + " " + LOCALE.get("MultipleImagesSelectedText") +
                        " (" + sizeText + ")"
		);
            }
        }
        else {
            all.setText(
                size + " / " + total + " " + LOCALE.get("MultipleImagesSelectedText") +
                    " (" + sizeText + ")"
            );
        }
        // On selection changes, warn if the lead selection is readonly,
        // and if it's not readonly, then warn if the folder is readonly.
        String errorText = null;
        if (leadFile != null) {
            if (! leadFile.canWrite()) {
                errorText = "\"" + leadFile.getName() + "\" " +
                    LOCALE.get("ReadOnlyFileText");
            }
        }
        if (errorText == null) {
            if (files.size() > 0) {
                File folder = files.get(0).getParentFile();
                if ((folder != null) && ! folder.canWrite()) {
                    errorText = "\"" + folder.getName() + "\" " +
                        LOCALE.get("ReadOnlyFolderText");
                }
            }
        }
        error.setText(errorText);
    }

    public void imageDoubleClicked(ImageBrowserEvent event) {
        error.setText("");
    }

    public void browserError(String message) {
        error.setText(message);
    }

    private static String getSizeText(long bytes) {
        if (bytes > 0x40000000) {
            return Long.toString(bytes >> 30) + "GB";
        }
        if (bytes > 0x00100000) {
            return Long.toString(bytes >> 20) + "MB";
        }
        if (bytes > 0x00000400) {
            return Long.toString(bytes >> 10) + "KB";
        }
        return Long.toString(bytes) + "B";
    }
}
