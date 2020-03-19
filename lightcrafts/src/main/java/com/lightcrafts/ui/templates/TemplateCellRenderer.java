/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.templates;

import com.lightcrafts.ui.LightZoneSkin;
import com.lightcrafts.ui.toolkit.IconFactory;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;
import java.awt.*;

/**
 * A TreeCellRenderer that builds on a backing TreeCellRenderer that's
 * provided by the Swing look-and-feel.
 */
class TemplateCellRenderer implements TreeCellRenderer {

    private final static Icon ExpandedIcon = IconFactory.createIcon(
        TemplateCellRenderer.class, "expanded.png"
    );
    private final static Icon CollapsedIcon = IconFactory.createIcon(
        TemplateCellRenderer.class, "collapsed.png"
    );

    private TreeCellRenderer backingRenderer;

    private final static Color NonSelectedForeground =
        LightZoneSkin.Colors.ToolPanesForeground;
    
    private final static Color SelectedForeground = Color.black;

    private final static Border PreviewedBorder =
        BorderFactory.createLineBorder(NonSelectedForeground);

    private final static Border NonPreviewedBorder =
        BorderFactory.createEmptyBorder(1, 1, 1, 1);
    
    TemplateCellRenderer() {
        JTree tree = new JTree();
        backingRenderer = tree.getCellRenderer();
    }

    public Component getTreeCellRendererComponent(
        JTree tree,
        Object value,
        boolean selected,
        boolean expanded,
        boolean leaf,
        int row,
        boolean hasFocus
    ) {
        JLabel label = (JLabel) backingRenderer.getTreeCellRendererComponent(
            tree, value, selected, expanded, leaf, row, hasFocus
        );
        if (leaf) {
            TreePath path = tree.getPathForRow(row);
            if (path != null) {
                Object last = path.getLastPathComponent();
                if (last instanceof TemplateTreeNode) {
                    TemplateTreeNode node = (TemplateTreeNode) last;
                    if (node.isPreviewed()) {
                        label.setBorder(PreviewedBorder);
                    }
                    else {
                        label.setBorder(NonPreviewedBorder);
                    }
                }
                else {
                    label.setBorder(NonPreviewedBorder);
                }
            }
            label.setBackground(LightZoneSkin.Colors.ToolPanesBackground);
            label.setOpaque(false);
            label.setIcon(null);
            label.setDisabledIcon(null);
        }
        else {
            label.setBorder(NonPreviewedBorder);
            label.setBackground(LightZoneSkin.Colors.FrameBackground);
            label.setOpaque(true);
            Icon icon = expanded ? ExpandedIcon : CollapsedIcon;
            label.setIcon(icon);
            label.setDisabledIcon(icon);
        }
        label.setForeground(
            selected ? SelectedForeground : NonSelectedForeground
        );
        Dimension size = label.getPreferredSize();
        label.setPreferredSize(new Dimension(200, size.height));
        
        return label;
    }
}

