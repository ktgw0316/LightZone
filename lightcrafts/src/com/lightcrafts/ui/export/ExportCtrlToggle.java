/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.export;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URL;
import java.util.prefs.Preferences;

import static com.lightcrafts.ui.export.Locale.LOCALE;

/**
 * A label with a collapse/expand icon, to show and hide export controls.
 * Its constructor takes an optional Window.  If this Window is not null,
 * then the ExportCtrlToggle will repack the Window when it collapses and
 * expands.
 */
class ExportCtrlToggle extends Box {

    private static Icon CollapsedIcon;
    private static Icon ExpandedIcon;
    private static Icon CollapsedHighlightIcon;
    private static Icon ExpandedHighlightIcon;

    static {
        URL url;
        Image image;
        Toolkit toolkit = Toolkit.getDefaultToolkit();

        url = ExportCtrlToggle.class.getResource(
            "resources/RightArrow.png"
        );
        image = toolkit.createImage(url);
        CollapsedIcon = new ImageIcon(image);

        url = ExportCtrlToggle.class.getResource(
            "resources/DownArrow.png"
        );
        image = toolkit.createImage(url);
        ExpandedIcon = new ImageIcon(image);

        url = ExportCtrlToggle.class.getResource(
            "resources/RightArrowHighlight.png"
        );
        image = toolkit.createImage(url);
        CollapsedHighlightIcon = new ImageIcon(image);

        url = ExportCtrlToggle.class.getResource(
            "resources/DownArrowHighlight.png"
        );
        image = toolkit.createImage(url);
        ExpandedHighlightIcon = new ImageIcon(image);
    }

    // Remember our expanded/collapsed state globally:
    private final static Preferences Prefs =
        Preferences.userNodeForPackage(ExportDialog.class);
    private final static String AdvancedOptionsKey = "AdvancedOptions";

    private ExportControls ctrls;
    private Window window;          // Pack the Window when toggling
    private JLabel label;
    private boolean isExpanded;

    ExportCtrlToggle(ExportControls ctrls, Window window) {
        super(BoxLayout.Y_AXIS);

        this.ctrls = ctrls;
        this.window = window;

        label = new JLabel(LOCALE.get("AdvancedOptionsLabel"));
        label.setAlignmentX(1f);
        Box labelBox = Box.createHorizontalBox();
        labelBox.add(label);
        labelBox.add(Box.createHorizontalGlue());
        labelBox.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
        add(labelBox);

        isExpanded = Prefs.getBoolean(AdvancedOptionsKey, true);

        // Manually initialize the expanded/collapsed state, and don't call
        // doExpand() or doCollapse() because these pack the window.
        if (isExpanded) {
            label.setIcon(ExpandedIcon);
            add(ctrls);
        }
        else {
            label.setIcon(CollapsedIcon);
        }
        label.addMouseListener(
            new MouseAdapter() {
                public void mousePressed(MouseEvent event) {
                    if (isExpanded) {
                        doCollapse();
                    }
                    else {
                        doExpand();
                    }
                }
                public void mouseEntered(MouseEvent event) {
                    if (isExpanded) {
                        label.setIcon(ExpandedHighlightIcon);
                    }
                    else {
                        label.setIcon(CollapsedHighlightIcon);
                    }
                }
                public void mouseExited(MouseEvent event) {
                    if (isExpanded) {
                        label.setIcon(ExpandedIcon);
                    }
                    else {
                        label.setIcon(CollapsedIcon);
                    }
                }
            }
        );
    }

    boolean isExpanded() {
        return isExpanded;
    }

    void doExpand() {
        add(ctrls);
        label.setIcon(ExpandedIcon);
        if (window != null) {
            window.pack();
        }
        else {
            revalidate();
            getParent().repaint();
        }
        isExpanded = true;
        Prefs.putBoolean(AdvancedOptionsKey, isExpanded);
    }

    void doCollapse() {
        remove(ctrls);
        label.setIcon(CollapsedIcon);
        if (window != null) {
            window.pack();
        }
        else {
            revalidate();
            getParent().repaint();
        }
        isExpanded = false;
        Prefs.putBoolean(AdvancedOptionsKey, isExpanded);
    }
}
