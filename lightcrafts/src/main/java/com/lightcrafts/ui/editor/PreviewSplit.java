/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.editor;

import com.lightcrafts.model.Preview;
import com.lightcrafts.ui.operation.OpStack;

import javax.swing.*;
import java.awt.*;

/**
 * This container holds either a split pane with a Preview on top and an
 * TemplateSplit on the bottom, or just a TemplateSplit.
 * <p>
 * The idea is to be able to show/and hide Previews, and have the split pane
 * divider come and go at the same time.
 */

class PreviewSplit extends JPanel {

    private final static Dimension PreviewSize = new Dimension(280, 190);

    private ToolsContainer tools;   // OpsToolbar and OpsScroll
    private JSplitPane split;       // added when the ZoneFinder is showing

    private PreviewTabs tabs;       // the tabbed pane holding the Previews
    private boolean showPreview;    // true if Previews are visible

    PreviewSplit(OpStack stack, Preview[] previews) {
        tools = new ToolsContainer(stack);
        setLayout(new BorderLayout());
        add(tools);

        split = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        split.setContinuousLayout(true);
        // This divider size matches values in TemplateSplit and SplitTreeNode.
        split.setDividerSize(4);

        tabs = new PreviewTabs(previews);

        split.add(tabs);

        setShowPreview(true);
    }

    // Create a disabled component, for the no-Document display mode:
    PreviewSplit() {
        tools = new ToolsContainer();
        setLayout(new BorderLayout());
        add(tools);

        split = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        split.setContinuousLayout(true);
        // This divider size matches values in TemplateSplit and SplitTreeNode.
        split.setDividerSize(4);

        tabs = new PreviewTabs();

        split.add(tabs);

        setShowPreview(true);
    }

    Preview getPreview() {
        return (tabs != null) ? tabs.getPreview() : null;
    }

    boolean isPreviewVisible() {
        return showPreview;
    }

    void setPreview(Preview preview) {
        tabs.setPreview(preview);
    }

    void setShowPreview(boolean show) {
        if (showPreview == show) {
            return;
        }
        showPreview = show;
        if (show) {
            remove(tools);
            split.add(tools);
            if (tabs.isValid()) {
                // Restore the divider to where it was before:
                split.setDividerLocation(tabs.getSize().height);
            }
            else {
                // Unless it's never been validated:
                split.setDividerLocation(PreviewSize.height);
            }
            add(split);
        }
        else {
            split.remove(tools);
            remove(split);
            add(tools);
        }
        revalidate();
    }

    void setDropper(Point p) {
        Preview preview = getPreview();
        if (preview != null) {
            preview.setDropper(p);
        }
    }
}
