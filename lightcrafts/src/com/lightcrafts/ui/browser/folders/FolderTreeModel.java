/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.browser.folders;

import com.lightcrafts.utils.directory.DirectoryListener;
import com.lightcrafts.utils.directory.DirectoryMonitor;

import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.io.File;

class FolderTreeModel extends DefaultTreeModel implements DirectoryListener {

    private NodeFileIndex index;

    private DirectoryMonitor monitor;

    FolderTreeModel() {
        super(FolderTreeNode.createRoot());
        FolderTreeNode root = (FolderTreeNode) getRoot();
        index = root.getIndex();
        monitor = root.getDirectoryMonitor();
        monitor.addListener(this);
    }

    // A folder's contents have changed or a folder has been deleted.
    public void directoryChanged(final File dir ) {
        if (! EventQueue.isDispatchThread()) {
            EventQueue.invokeLater(
                new Runnable() {
                    public void run() {
                        directoryChanged(dir);
                    }
                }
            );
            return;
        }
        FolderTreeNode node = index.get(dir);
        if (node == null) {
            return;
        }
        // If the directory exists, then update its node's children.
        // If the directory does not exist, then update its parent's children.
        if (dir.isDirectory()) {
            node.updateChildren();
            nodeStructureChanged(node);
        }
        else {
            FolderTreeNode parent = (FolderTreeNode) node.getParent();
            parent.updateChildren();
            nodeStructureChanged(parent);
        }
    }

    public void dispose() {
        monitor.removeListener( this );
        monitor.dispose();
    }

    DirectoryMonitor getDirectoryMonitor() {
        return monitor;
    }
}
/* vim:set et sw=4 ts=4: */
