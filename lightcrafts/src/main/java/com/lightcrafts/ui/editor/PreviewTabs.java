/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.editor;

import static com.lightcrafts.ui.editor.Locale.LOCALE;
import com.lightcrafts.model.Preview;
import com.lightcrafts.ui.LightZoneSkin;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.util.prefs.Preferences;
import java.util.MissingResourceException;
import java.awt.*;

/**
 * A tabbed pane to show Engine Previews.  It's a little complex, because it
 * must retain its state in preferences, and because it must interact with
 * the variety of Previews and their other controls for show and hide.
 */
final class PreviewTabs extends JTabbedPane {

    // Retain the most recent Preview selection in Preferences:
    private final static Preferences Prefs =
        Preferences.userNodeForPackage(PreviewTabs.class);

    private final static String PreviewKey = "Preview";    // Preview to show

    private Preview selectedPreview = null;

    PreviewTabs(Preview[] previews) {
        setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);

        int index = 0;
        for (Preview preview : previews) {
            preview.setBackground(LightZoneSkin.Colors.ToolPanesBackground.brighter());
            addTab(preview.getName(), preview);
            String tip = getToolTip(preview);
            setToolTipTextAt(index++, tip);
        }
        // Initialize the selected tab from Preferences, if defined:
        final String initPreview = Prefs.get(PreviewKey, "ZoneFinder");
        for (Preview preview : previews) {
            String name = preview.getName();
            if (name.equals(initPreview)) {
                setSelectedComponent(preview);
            }
        }
        addChangeListener(
            new ChangeListener() {
                public void stateChanged(ChangeEvent e) {
                    if (selectedPreview != null)
                        selectedPreview.setSelected(false);
                    selectedPreview = (Preview) getSelectedComponent();
                    final String name = selectedPreview.getName();
                    Prefs.put(PreviewKey, name);
                }
            }
        );
    }

    // Create a disabled component, for the no-Document display mode.

    PreviewTabs() {
        addTab("Zones", new JPanel());
        addTab("Color Mask", new JPanel());
        addTab("Histogram", new JPanel());
        addTab("Sampler", new JPanel());
        setEnabled(false);
    }

    public void addTab(String title, Component component) {
        if (component instanceof Preview)
            selectedPreview = (Preview) component;
        super.addTab(title, component);
    }

    public void setSelectedComponent(Component c) {
        if (c instanceof Preview)
            selectedPreview = (Preview) c;
        super.setSelectedComponent(c);
    }

    void setPreview(Preview preview) {
        selectedPreview = preview;
        setSelectedComponent(preview);
    }

    Preview getPreview() {
        return (Preview) getSelectedComponent();
    }

    private static String getToolTip(Preview preview) {
        String name = preview.getName();
        name = name.replaceAll("\\s", "");
        try {
            return LOCALE.get("PreviewToolTip" + name);
        }
        catch (MissingResourceException e) {
            return null;
        }
    }
}
