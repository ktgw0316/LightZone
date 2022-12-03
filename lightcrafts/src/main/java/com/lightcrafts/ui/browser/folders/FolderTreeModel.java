/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2017-     Masahiro Kitagawa */

package com.lightcrafts.ui.browser.folders;

import com.lightcrafts.utils.directory.DirectoryListener;
import com.lightcrafts.utils.directory.DirectoryMonitor;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import java.nio.file.Files;
import java.nio.file.Path;

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
    public void directoryChanged(final Path dir, final Path file, final String kind) {
        final FolderTreeNode dirNode = index.get(dir.toFile());
        if (dirNode == null) {
            return;
        }
        if (Files.exists(file) && ! Files.isDirectory(file)) {
            return;
        }
        synchronized (dirNode) {
            final FolderTreeNode childNode;
            final int nodeIndex;

            switch (kind) {
                case "ENTRY_CREATE":
                case "ENTRY_MODIFY":
                    dirNode.updateChildren();
                    childNode = index.get(file.toFile());
                    nodeIndex = dirNode.getIndex(childNode);
                    break;
                case "ENTRY_DELETE":
                    childNode = index.get(file.toFile());
                    nodeIndex = dirNode.getIndex(childNode);
                    dirNode.updateChildren();
                    break;
                default:
                    childNode = null;
                    nodeIndex = -1;
            }
            if (nodeIndex < 0) {
                return;
            }
            SwingUtilities.invokeLater(() -> {
                switch (kind) {
                    case "ENTRY_CREATE":
                        nodesWereInserted(dirNode, new int[]{nodeIndex});
                        break;
                    case "ENTRY_DELETE":
                        nodesWereRemoved(dirNode, new int[]{nodeIndex}, null);
                        break;
                    case "ENTRY_MODIFY":
                        nodeChanged(childNode);
                        break;
                    default:
                }
            });
        }
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
