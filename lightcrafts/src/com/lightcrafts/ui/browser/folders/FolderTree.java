/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.browser.folders;

import com.lightcrafts.platform.Platform;
import com.lightcrafts.ui.LightZoneSkin;

import javax.swing.*;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;
import java.awt.dnd.DropTarget;
import java.io.File;
import java.util.TooManyListenersException;
import java.util.List;
import java.util.LinkedList;

/**
 * A tree component representing folders in all file systems, with specialized
 * tree structure mangling and fancy icons for special folders.  Folders
 * can be selected, selection can be directed programmatically, and the whole
 * model is updated dynamically when file system modifications are detected.
 */
class FolderTree extends JTree {

    private LinkedList<FolderTreeListener> listeners;

    FolderTree() {
        super(new FolderTreeModel());

        setBackground(LightZoneSkin.Colors.ToolPanesBackground);

        setFocusable(false);

        TreeCellRenderer renderer = FolderTreeCellRenderer.createRenderer();
        setCellRenderer(renderer);

        FolderTreeTransferHandler trans = new FolderTreeTransferHandler(this);
        setTransferHandler(trans);

        FolderTreeSelectionModel selection = new FolderTreeSelectionModel(this);
        setSelectionModel(selection);

        DropTarget target = getDropTarget();
        try {
            target.addDropTargetListener(selection);
        }
        catch (TooManyListenersException e) {
            // Doesn't seem to happen
            e.printStackTrace();
        }
        addTreeSelectionListener(
            new TreeSelectionListener() {
                public void valueChanged(TreeSelectionEvent e) {
                    notifyFolderSelected();
                }
            }
        );
        listeners = new LinkedList<FolderTreeListener>();
    }

    void addFolderTreeListener(FolderTreeListener listener) {
        listeners.add(listener);
    }

    void removeFolderTreeListener(FolderTreeListener listener) {
        listeners.remove(listener);
    }

    FolderTreeNode getRoot() {
        return (FolderTreeNode) getModel().getRoot();
    }

    boolean goToFolder(File folder) {
        final String[] components =
                Platform.getPlatform().getPathComponentsTo(folder.getAbsoluteFile());
        return goToFolder(components);
    }

    @Deprecated
    boolean goToPicturesFolder() {
        final String[] components =
                Platform.getPlatform().getPathComponentsToPicturesFolder();
        return goToFolder(components);
    }

    private boolean goToFolder(final String[] components) {
        if (components == null || components.length == 0)
            return false;
        FolderTreeNode node = getRoot();
        for ( String component : components ) {
            if (component.isEmpty())
                continue;
            boolean matchedComponent = false;
            for ( final FolderTreeNode child : node.getChildren() ) {
                if ( child.toString().equals( component ) ) {
                    node = child;
                    matchedComponent = true;
                    break;
                }
            }
            if ( !matchedComponent )
                return false;
        }
        setSelectionPath( node.getTreePath() );
        return true;
    }

    void notifyDropAccepted(List<File> files, File folder) {
        for (FolderTreeListener listener : listeners) {
            listener.folderDropAccepted(files, folder);
        }
    }

    void notifyFolderSelected() {
        FolderTreeNode node = getSelectedNode();
        if (node != null) {
            File file = node.getFile();
            for (FolderTreeListener listener : listeners) {
                listener.folderSelectionChanged(file);
            }
        }
    }

    FolderTreeNode getSelectedNode() {
        TreePath path = getSelectionPath();
        if (path != null) {
            return (FolderTreeNode) path.getLastPathComponent();
        }
        return null;
    }

    void dispose() {
        FolderTreeModel model = (FolderTreeModel) getModel();
        model.dispose();
    }

    @Override
    protected void finalize() throws Throwable {
        dispose();
        super.finalize();
    }
}
/* vim:set et sw=4 ts=4: */
