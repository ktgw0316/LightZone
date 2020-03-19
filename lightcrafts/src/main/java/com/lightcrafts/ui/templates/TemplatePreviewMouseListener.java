/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.templates;

import com.lightcrafts.utils.xml.XmlNode;

import javax.swing.event.MouseInputAdapter;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.LinkedList;

/**
 * Follow mouse motion on the TemplateTree and update the TemplatePreview and the
 * TemplateTreeNode preview highlights.
 */
class TemplatePreviewMouseListener extends MouseInputAdapter {

    private TemplateTree tree;

    private TemplatePreview preview;

    private TemplateTreeNode previewNode;

    TemplatePreviewMouseListener(TemplateTree tree, TemplatePreview preview) {
        this.tree = tree;
        this.preview = preview;
    }

    public void mouseMoved(MouseEvent e) {
        Point p = e.getPoint();
        TreePath path = tree.getPathForLocation(p.x, p.y);
        if (path != null) {
            Object last = path.getLastPathComponent();
            if (last instanceof TemplateTreeNode) {
                TemplateTreeNode node = (TemplateTreeNode) last;
                XmlNode xml = node.node;
                preview.showTemplatePreview(xml);
                setPreviewNode((TemplateTreeNode) last);
            }
            else {
                preview.showNormalPreview();
                setPreviewNode(null);
            }
        }
        else {
            preview.showNormalPreview();
            setPreviewNode(null);
        }
    }

    public void mouseExited(MouseEvent e) {
        preview.showNormalPreview();
        setPreviewNode(null);
    }

    private void setPreviewNode(TemplateTreeNode node) {
        if ((previewNode != null) && (previewNode != node)) {
            previewNode.setPreviewed(false);
            repaintTreeNode(previewNode);
        }
        previewNode = node;
        if (previewNode != null) {
            previewNode.setPreviewed(true);
            repaintTreeNode(previewNode);
        }
    }

    private void repaintTreeNode(TreeNode node) {
        LinkedList<Object> objects = new LinkedList<Object>();
        while (node != null) {
            objects.addFirst(node);
            node = node.getParent();
        }
        TreePath path = new TreePath(objects.toArray());
        Rectangle rect = tree.getPathBounds(path);
        tree.repaint(rect);
    }
}
