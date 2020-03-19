/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.browser.folders;

import javax.swing.*;
import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.io.File;

/**
 * This customized tree selection model serves to suppress
 * disruptive selection changes during drag gestures, and to control
 * drop target highlights on tree nodes.
 */
class FolderTreeSelectionModel
    extends DefaultTreeSelectionModel implements DropTargetListener
{
    private JTree tree;

    // The node currently receiving the drop target highlight
    private FolderTreeNode dropNode;

    // Don't allow selection changes during drag-overs
    private boolean isDragging;

    FolderTreeSelectionModel(JTree tree) {
        this.tree = tree;
        setSelectionMode(
            TreeSelectionModel.SINGLE_TREE_SELECTION
        );
    }

    public void setSelectionPath(TreePath path) {
        if (! isDragging) {
            super.setSelectionPath(path);
        }
    }

    public void dragEnter(DropTargetDragEvent dtde) {
        isDragging = true;
    }

    public void dragOver(DropTargetDragEvent dtde) {
        if (dropNode != null) {
            dropNode.setIsDropTarget(false);
            notifyChanged(dropNode);
        }
        Point p = dtde.getLocation();
        dropNode = getNodeAt(p);
        if (dropNode != null) {
            dropNode.setIsDropTarget(true);
            notifyChanged(dropNode);
        }
    }

    public void dropActionChanged(DropTargetDragEvent dtde) {
    }

    public void dragExit(DropTargetEvent dte) {
        isDragging = false;
        if (dropNode != null) {
            dropNode.setIsDropTarget(false);
            notifyChanged(dropNode);
        }
    }

    public void drop(DropTargetDropEvent dtde) {
        isDragging = false;
        if (dropNode != null) {
            dropNode.setIsDropTarget(false);
            notifyChanged(dropNode);
        }
    }

    File getDropFolder() {
        if (dropNode != null) {
            return dropNode.getFile();
        }
        return null;
    }

    private FolderTreeNode getNodeAt(Point p) {
        TreePath path = tree.getPathForLocation(p.x, p.y);
        if (path != null) {
            FolderTreeNode node = (FolderTreeNode) path.getLastPathComponent();
            return node;
        }
        return null;
    }

    private void notifyChanged(FolderTreeNode node) {
        FolderTreeModel model = (FolderTreeModel) tree.getModel();
        model.nodeChanged(node);
    }
}
