/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2017-     Masahiro Kitagawa */

package com.lightcrafts.ui.browser.folders;

import com.lightcrafts.utils.directory.DirectoryListener;
import com.lightcrafts.utils.directory.DirectoryMonitor;

import javax.swing.SwingWorker;
import javax.swing.tree.DefaultTreeModel;
import java.io.File;

class FolderTreeModel extends DefaultTreeModel implements DirectoryListener {

    private NodeFileIndex index;

    private DirectoryMonitor monitor;

    private SwingWorker<Void, Void> worker;

    FolderTreeModel() {
        super(FolderTreeNode.createRoot());
        FolderTreeNode root = (FolderTreeNode) getRoot();
        index = root.getIndex();
        monitor = root.getDirectoryMonitor();
        monitor.addListener(this);
    }

    @Override
    public void directoryChanged(final File dir) {
        FolderTreeNode node = index.get(dir);
        if (node == null) {
            return;
        }

        // If the directory exists, then update its node's children.
        // If the directory does not exist, then update its parent's children.
        final FolderTreeNode dirNode = dir.isDirectory()
                ? node
                : (FolderTreeNode) node.getParent();
        worker = new SwingWorker<Void, Void>() {
            @Override
            public Void doInBackground() {
                dirNode.updateChildren();
                return null;
            }

            @Override
            protected void done() {
                nodeStructureChanged(dirNode);
            }
        };
        worker.execute();
    }

    public void dispose() {
        if (worker != null && !worker.isDone()) {
            worker.cancel(true);
        }
        worker = null;

        monitor.removeListener(this);
        monitor.dispose();
    }

    DirectoryMonitor getDirectoryMonitor() {
        return monitor;
    }
}
/* vim:set et sw=4 ts=4: */
